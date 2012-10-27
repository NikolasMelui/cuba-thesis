/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.SendingAttachment;
import com.haulmont.cuba.core.entity.SendingMessage;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.security.global.LoginException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.javamail.JavaMailSender;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Emailer MBean implementation.
 * <p/>
 * Provides email functionality, allows to set some emailing parameters through JMX-console.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(EmailerAPI.NAME)
public class Emailer extends ManagementBean implements EmailerMBean, EmailerAPI {

    private Log log = LogFactory.getLog(Emailer.class);

    private JavaMailSender mailSender;

    private EmailerConfig config;

    @Inject
    private EmailManagerAPI emailManager;

    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private TimeSource timeSource;

    @Inject
    private Persistence persistence;

    private static final String EMAIL_SMTP_HOST_PROPERTY_NAME = "cuba.email.smtpHost";
    private static final String EMAIL_DEFAULT_FROM_ADDRESS_PROPERTY_NAME = "cuba.email.fromAddress";
    private static final String SEND_ALL_TO_ADMIN_PROPERTY_NAME = "cuba.email.sendAllToAdmin";
    private static final String ADMIN_ADDRESS_PROPERTY_NAME = "cuba.email.adminAddress";

    @Inject
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Inject
    public void setConfig(Configuration configuration) {
        this.config = configuration.getConfig(EmailerConfig.class);
    }

    @Override
    public void sendEmail(EmailInfo info) throws EmailException {
        sendEmail(info, true);
    }

    @Override
    public void sendEmail(EmailInfo info, boolean sync) throws EmailException {
        if (sync) {
            sendEmailSync(info);
        } else {
            sendEmailAsync(info, null, null);
        }
    }

    private void sendEmailSync(EmailInfo info) throws EmailException {
        prepareMessageBody(info);
        sendEmail(info.getAddresses(), info.getCaption(), info.getBody(), info.getFrom() != null ? info.getFrom() : getFromAddress(), info.getAttachment());
    }

    private void prepareMessageBody(EmailInfo info) {
        if (info.getTemplatePath() != null) {
            Map map = info.getTemplateParameters() == null ? Collections.EMPTY_MAP : info.getTemplateParameters();
            info.setBody(TemplateHelper.processTemplateFromFile(info.getTemplatePath(), map));
        }
    }

    @Override
    public void sendEmailAsync(EmailInfo info, Integer attemptsCount, Date deadline) {
        prepareMessageBody(info);
        List<SendingMessage> sendingMessageList = splitEmail(info, attemptsCount, deadline);
        emailManager.addEmailsToQueue(sendingMessageList);
    }

    @Override
    public List<SendingMessage> sendMessagesAsync(EmailInfo info) {
        prepareMessageBody(info);
        List<SendingMessage> sendingMessageList = splitEmail(info);
        return emailManager.addEmailsToQueue(sendingMessageList);
    }

    @Override
    public void scheduledSendEmail(SendingMessage sendingMessage) throws LoginException, EmailException {
        loginOnce();
        sendEmail(sendingMessage);
    }

    private List<SendingMessage> splitEmail(EmailInfo info, Integer attemptsCount, Date deadline) {
        List<SendingMessage> sendingMessageList = new LinkedList<>();
        String[] addrArr = info.getAddresses().split("[,;]");
        for (String addr : addrArr) {
            try {
                addr = addr.trim();
                String fromEmail;
                if (info.getFrom() == null) {
                    fromEmail = getFromAddress();
                } else {
                    fromEmail = info.getFrom();
                }
                SendingMessage sendingMessage = createSendingMessage(addr, fromEmail, info.getCaption(), info.getBody(),
                        info.getAttachment(), attemptsCount, deadline);

                sendingMessageList.add(sendingMessage);
            } catch (Exception e) {
                log.error("Exception while creating SendingMessage:" + ExceptionUtils.getStackTrace(e));
            }
        }
        return sendingMessageList;
    }

    private List<SendingMessage> splitEmail(EmailInfo info) {
        return splitEmail(info, null, null);
    }

    @Override
    public void sendEmail(String addresses, String caption, String body, EmailAttachment... attachment)
            throws EmailException {
        sendEmail(addresses, caption, body, getFromAddress(), attachment);
    }

    public void sendEmail(SendingMessage sendingMessage) {
        try {
            String addr = sendingMessage.getAddress().trim();
            String fromEmail;
            if (sendingMessage.getFrom() == null) {
                fromEmail = getFromAddress();
            } else {
                fromEmail = sendingMessage.getFrom();
            }
//            updateSendingMessageStatus(sendingMessage, SendingStatus.SENDING);
            MimeMessage message = createMessage(addr, sendingMessage.getCaption(), sendingMessage.getContentText(), getEmailAttachments(sendingMessage), fromEmail);
            send(addr, message);

            updateSendingMessageStatus(sendingMessage, SendingStatus.SENT);
        } catch (Exception e) {
            log.warn("Unable to send email to '" + sendingMessage.getAddress() + "'", e);
            updateSendingMessageStatus(sendingMessage, SendingStatus.QUEUE);
        }
    }

    public void sendEmail(String addresses, String caption, String body, String from, EmailAttachment... attachment)
            throws EmailException {
        String[] addrArr = addresses.split("[,;]");

        List<String> failedAddresses = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (String addr : addrArr) {
            SendingMessage sendingMessage = null;
            try {
                addr = addr.trim();
                String fromEmail;
                if (from == null) {
                    fromEmail = getFromAddress();
                } else {
                    fromEmail = from;
                }
                sendingMessage = storeMessage(addr, fromEmail, caption, body, attachment, SendingStatus.SENDING);
                MimeMessage message = createMessage(addr, caption, body, attachment, fromEmail);
                send(addr, message);

                updateSendingMessageStatus(sendingMessage, SendingStatus.SENT);
            } catch (Exception e) {
                log.warn("Unable to send email to '" + addr + "'", e);
                failedAddresses.add(addr);
                errorMessages.add(e.getMessage());
                if (sendingMessage != null)
                    updateSendingMessageStatus(sendingMessage, SendingStatus.NOTSENT);
            }
        }
        if (!failedAddresses.isEmpty()) {
            throw new EmailException(
                    failedAddresses.toArray(new String[failedAddresses.size()]),
                    errorMessages.toArray(new String[errorMessages.size()])
            );
        }
    }

    private void updateSendingMessageStatus(SendingMessage sendingMessage, SendingStatus status) {
        if (sendingMessage != null) {
            boolean increaseAttemptsMade = !status.equals(SendingStatus.SENDING);
            Date currentTimestamp = timeSource.currentTimestamp();

            Transaction tx = persistence.createTransaction();
            try {
                EntityManager em = persistence.getEntityManager();
                StringBuilder queryStr = new StringBuilder("update sys$SendingMessage sm set sm.status = :status, sm.updateTs=:updateTs, sm.updatedBy = :updatedBy");
                if (increaseAttemptsMade)
                    queryStr.append(", sm.attemptsMade = sm.attemptsMade + 1 ");
                if (status.equals(SendingStatus.SENT))
                    queryStr.append(", sm.dateSent = :dateSent");
                queryStr.append("\n where sm.id=:id");
                Query query = em.createQuery(queryStr.toString())
                        .setParameter("status", status.getId())
                        .setParameter("id", sendingMessage.getId())
                        .setParameter("updateTs", currentTimestamp)
                        .setParameter("updatedBy", userSessionSource.getUserSession().getUser().getLogin());
                if (status.equals(SendingStatus.SENT))
                    query.setParameter("dateSent", currentTimestamp);
                query.executeUpdate();
                tx.commit();
            } finally {
                tx.end();
            }
        }
    }

    private void send(String addr, MimeMessage message) throws MessagingException {
        if (getSendAllToAdmin()) {
            addr = getAdminAddress();
        }
        InternetAddress internetAddress = new InternetAddress(addr);
        message.setRecipient(Message.RecipientType.TO, internetAddress);
        mailSender.send(message);
        log.info("Email '" + message.getSubject() + "' to '" + addr + "' sent succesfully");
    }

    private MimeMessage createMessage(
            String address,
            String caption,
            String text,
            EmailAttachment[] attachments, String from) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        msg.addRecipients(Message.RecipientType.TO, address);
        msg.setSubject(caption, "UTF-8");
        msg.setSentDate(new Date());
        msg.setFrom(new InternetAddress(from));

        MimeMultipart content = new MimeMultipart("mixed");
        MimeBodyPart textBodyPart = new MimeBodyPart();
        MimeMultipart textPart = new MimeMultipart("related");
        MimeBodyPart contentBodyPart = new MimeBodyPart();
        if (text.trim().startsWith("<html>")) {
            contentBodyPart.setContent(text, "text/html; charset=UTF-8");
        } else {
            contentBodyPart.setContent(text, "text/plain; charset=UTF-8");
        }
        textPart.addBodyPart(contentBodyPart);
        textBodyPart.setContent(textPart);
        content.addBodyPart(textBodyPart);

        if (attachments != null) {
            for (EmailAttachment attachment : attachments) {
                MimeBodyPart attachBodyPart = new MimeBodyPart();

                DataSource source = new ByteArrayDataSource(attachment.getData());
                attachBodyPart.setDataHandler(new DataHandler(source));

                String contentType = FileTypesHelper.getMIMEType(attachment.getName());

                String encodedFileName;
                try {
                    QCodec codec = new QCodec();
                    encodedFileName = codec.encode(attachment.getName());
                } catch (EncoderException e) {
                    encodedFileName = attachment.getName();
                }

                String contentId = attachment.getContentId();
                if (contentId == null) {
                    contentId = encodedFileName;
                    content.addBodyPart(attachBodyPart);
                } else
                    textPart.addBodyPart(attachBodyPart);

                attachBodyPart.setHeader("Content-ID", "<" + contentId + ">");
                attachBodyPart.setHeader("Content-Type", contentType + "; charset=utf-8; name=" + encodedFileName);
                attachBodyPart.setFileName(encodedFileName);
                attachBodyPart.setDisposition("inline");
            }
        }

        msg.setContent(content);
        msg.saveChanges();

        return msg;
    }

    @Override
    public String getFromAddress() {
        String fromAddress = AppContext.getProperty(EMAIL_DEFAULT_FROM_ADDRESS_PROPERTY_NAME);
        return fromAddress != null ? fromAddress : config.getFromAddress();
    }

    @Override
    public void setFromAddress(String address) {
        if (address != null) {
            try {
                login();
                config.setFromAddress(address);
            } catch (LoginException e) {
                throw new RuntimeException(e);
            } finally {
                logout();
            }
        }
    }

    @Override
    public String getSmtpHost() {
        String smtpHost = AppContext.getProperty(EMAIL_SMTP_HOST_PROPERTY_NAME);
        return smtpHost != null ? smtpHost : config.getSmtpHost();
    }
    
    private String getAdminAddress() {
        final String adminAddress = AppContext.getProperty(ADMIN_ADDRESS_PROPERTY_NAME);
        return adminAddress != null ? adminAddress : config.getAdminAddress();
    }

    private boolean getSendAllToAdmin() {
        final Boolean sendAllToAdmin = BooleanUtils.toBooleanObject(AppContext.getProperty(SEND_ALL_TO_ADMIN_PROPERTY_NAME));
        return sendAllToAdmin != null ? sendAllToAdmin : config.getSendAllToAdmin();
    }

    @Override
    public String sendTestEmail(String addresses) {
        try {
            String att = "<html><body><h1>Test attachment</h1></body></html>";
            EmailAttachment emailAtt = new EmailAttachment(att.getBytes(), "test attachment.html");
            sendEmail(addresses, "Test email", "<html><body><h1>Test email</h1></body></html>", emailAtt);
//            EmailInfo info = new EmailInfo(addresses, "Test email from mailer", "cuba@haulmont.com", "../server/default/conf/cuba/templates/testEmail.html", new HashMap<String, Serializable>(), null, emailAtt);
//            sendEmail(info);
            return "Email to '" + addresses + "' sent succesfully";
        } catch (Exception e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    private static class ByteArrayDataSource implements DataSource {
        private byte[] data;

        public ByteArrayDataSource(byte[] data) {
            this.data = data;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String getName() {
            return "ByteArray";
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }
    }

    private SendingMessage storeMessage(String addr, String from, String caption, String body, EmailAttachment[] attachment, SendingStatus status) {
        SendingMessage sendingMessage = createSendingMessage(addr, from, caption, body, attachment, null, null);
        if (status != null)
            sendingMessage.setStatus(status);
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.persist(sendingMessage);
            if (sendingMessage.getAttachments() != null && !sendingMessage.getAttachments().isEmpty()) {
                for (SendingAttachment attach : sendingMessage.getAttachments())
                    em.persist(attach);
            }
            tx.commit();
            return sendingMessage;
        } catch (Exception e) {
            log.error("Failed to store message: " + ExceptionUtils.getStackTrace(e));
            return null;
        } finally {
            tx.end();
        }
    }

    private SendingMessage createSendingMessage(String addr, String from, String caption, String body,
                                                EmailAttachment[] attachment, Integer attemptsCount, Date deadline) {

        SendingMessage sendingMessage = new SendingMessage();
        StringBuilder attachmentsName = new StringBuilder();
        List<SendingAttachment> sendingAttachments = null;
        if (attachment != null) {
            sendingAttachments = new ArrayList<>(attachment.length);
            for (EmailAttachment ea : attachment) {
                if (ea != null) {
                    attachmentsName.append(ea.getName()).append(";");
                    SendingAttachment sendingAttachment = new SendingAttachment();
                    sendingAttachment.setContent(ea.getData());
                    sendingAttachment.setMessage(sendingMessage);
                    sendingAttachment.setContentId(ea.getContentId());
                    sendingAttachment.setName(ea.getName());
                    sendingAttachments.add(sendingAttachment);
                }
            }
        }
        sendingMessage.setAttachments(sendingAttachments);
        if (getSendAllToAdmin())
            addr = getAdminAddress();
        sendingMessage.setAddress(addr);
        sendingMessage.setFrom(from);
        sendingMessage.setContentText(body);
        sendingMessage.setCaption(caption);
        sendingMessage.setStatus(SendingStatus.QUEUE);
        sendingMessage.setAttachmentsName(attachmentsName.toString());
        sendingMessage.setAttemptsCount(attemptsCount);
        sendingMessage.setDeadline(deadline);
        return sendingMessage;
    }

    @Override
    protected Credentials getCredentialsForLogin() {
        return new Credentials(AppContext.getProperty(EmailerAPI.NAME + ".login"), AppContext.getProperty(EmailerAPI.NAME + ".password"));
    }

    private EmailInfo getEmailInfo(SendingMessage sendingMessage) {
        EmailAttachment[] attachments = getEmailAttachments(sendingMessage);
        EmailInfo info = new EmailInfo(
                sendingMessage.getAddress(),
                sendingMessage.getCaption(),
                sendingMessage.getFrom(),
                null,
                null,
                sendingMessage.getContentText(),
                attachments
        );
        return info;
    }

    private EmailAttachment[] getEmailAttachments(SendingMessage sendingMessage) {
        EmailAttachment[] res;
        List<EmailAttachment> emailAttachmentList = new LinkedList<>();
        List<SendingAttachment> sendingAttachments = sendingMessage.getAttachments();
        if (sendingAttachments != null && sendingAttachments.size() > 0) {
            for (SendingAttachment attachment : sendingAttachments)
                emailAttachmentList.add(createEmailAttachment(attachment));
            res = new EmailAttachment[emailAttachmentList.size()];
            return emailAttachmentList.toArray(res);
        } else
            return null;
    }

    private EmailAttachment createEmailAttachment(SendingAttachment attachment) {
        return new EmailAttachment(attachment.getContent(), attachment.getName(), attachment.getContentId());
    }
}