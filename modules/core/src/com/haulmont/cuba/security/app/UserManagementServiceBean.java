/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.security.app;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.app.EmailerAPI;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.Constraint;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.SessionAttribute;
import com.haulmont.cuba.security.entity.User;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author artamonov
 * @version $Id$
 */
@Service(UserManagementService.NAME)
public class UserManagementServiceBean implements UserManagementService {

    private static final String GROUP_COPY_VIEW = "group.copy";

    private static final String MOVE_USER_TO_GROUP_VIEW = "user.moveToGroup";

    private static final String RESET_PASSWORD_VIEW = "user.resetPassword";

    private Log log = LogFactory.getLog(getClass());

    @Inject
    private Persistence persistence;

    @Inject
    private Metadata metadata;

    @Inject
    private Encryption encryption;

    @Inject
    private EmailerAPI emailerAPI;

    @Inject
    private Resources resources;

    @Inject
    private Configuration configuration;

    @Override
    public Group copyAccessGroup(UUID accessGroupId) {
        checkNotNull(accessGroupId, "Null access group id");

        Group clone = null;

        Transaction tx = persistence.getTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.setView(metadata.getViewRepository().getView(Group.class, GROUP_COPY_VIEW));

            Group accessGroup = em.find(Group.class, accessGroupId);
            if (accessGroup == null)
                throw new IllegalStateException("Unable to find specified access group with id: " + accessGroupId);

            clone = cloneGroup(accessGroup, accessGroup.getParent(), em);

            tx.commit();
        } finally {
            tx.end();
        }

        return clone;
    }

    @Override
    public Integer moveUsersToGroup(List<UUID> userIds, @Nullable UUID targetAccessGroupId) {
        checkNotNull(userIds, "Null users list");

        if (userIds.isEmpty())
            return 0;

        Transaction tx = persistence.getTransaction();

        int modifiedUsers = 0;
        try {
            EntityManager em = persistence.getEntityManager();

            Group targetAccessGroup = null;
            if (targetAccessGroupId != null) {
                targetAccessGroup = em.find(Group.class, targetAccessGroupId);
                if (targetAccessGroup == null)
                    throw new IllegalStateException("Could not found target access group with id: " + targetAccessGroupId);
            }

            em.setView(metadata.getViewRepository().getView(User.class, MOVE_USER_TO_GROUP_VIEW));

            TypedQuery<User> query = em.createQuery("select u from sec$User u where u.id in (:userIds)", User.class);
            query.setParameter("userIds", userIds);

            List<User> users = query.getResultList();
            if (users == null || users.size() != userIds.size())
                throw new IllegalStateException("Not all users found in database");

            for (User user : users) {
                if (!ObjectUtils.equals(user.getGroup(), targetAccessGroup)) {
                    user.setGroup(targetAccessGroup);
                    em.merge(user);
                    modifiedUsers++;
                }
            }

            tx.commit();
        } finally {
            tx.end();
        }
        return modifiedUsers;
    }

    @Override
    public Integer changePasswordsAtLogonAndSendEmails(List<UUID> userIds) {
        checkNotNull(userIds, "Null users list");

        if (userIds.isEmpty())
            return 0;

        Map<User, String> modifiedUsers = updateUserPasswords(userIds, true);

        // email templates
        ServerConfig serverConfig = configuration.getConfig(ServerConfig.class);
        String resetPasswordBodyTemplate = serverConfig.getResetPasswordEmailBodyTemplate();
        String resetPasswordSubjectTemplate = serverConfig.getResetPasswordEmailSubjectTemplate();

        SimpleTemplateEngine templateEngine = new SimpleTemplateEngine();

        Map<String, Template> localizedBodyTemplates = new HashMap<>();
        Map<String, Template> localizedSubjectTemplates = new HashMap<>();

        // load default
        Template bodyDefaultTemplate = loadDefaultTemplate(resetPasswordBodyTemplate, templateEngine);
        Template subjectDefaultTemplate = loadDefaultTemplate(resetPasswordSubjectTemplate, templateEngine);

        // send emails
        for (User user : modifiedUsers.keySet()) {
            if (StringUtils.isNotEmpty(user.getEmail())) {

                EmailTemplate template = getResetPasswordTemplate(user, templateEngine,
                        resetPasswordSubjectTemplate, resetPasswordBodyTemplate,
                        subjectDefaultTemplate, bodyDefaultTemplate,
                        localizedSubjectTemplates, localizedBodyTemplates);

                sendResetPasswordEmail(user, modifiedUsers.get(user), template.getSubjectTemplate(), template.getBodyTemplate());
            }
        }

        return modifiedUsers.size();
    }

    @Override
    public Map<UUID, String> changePasswordsAtLogon(List<UUID> userIds, boolean generatePassword) {
        checkNotNull(userIds, "Null users list");

        if (userIds.isEmpty())
            return Collections.emptyMap();

        Map<User, String> modifiedUsers = updateUserPasswords(userIds, generatePassword);
        Map<UUID, String> userPasswords = new LinkedHashMap<>();
        for (Map.Entry<User, String> entry : modifiedUsers.entrySet())
            userPasswords.put(entry.getKey().getId(), entry.getValue());

        return userPasswords;
    }

    private EmailTemplate getResetPasswordTemplate(User user,
                                                   SimpleTemplateEngine templateEngine,
                                                   String resetPasswordSubjectTemplate,
                                                   String resetPasswordBodyTemplate,
                                                   Template subjectDefaultTemplate,
                                                   Template bodyDefaultTemplate,
                                                   Map<String, Template> localizedSubjectTemplates,
                                                   Map<String, Template> localizedBodyTemplates) {

        String locale = StringUtils.isEmpty(user.getLanguage()) ? Locale.getDefault().getLanguage() : user.getLanguage();

        Template bodyTemplate;
        if (Locale.getDefault().getLanguage().equals(locale))
            bodyTemplate = bodyDefaultTemplate;
        else {
            if (localizedBodyTemplates.containsKey(locale))
                bodyTemplate = localizedBodyTemplates.get(locale);
            else {
                String baseName = FilenameUtils.getBaseName(resetPasswordBodyTemplate);
                String localizedTemplate = baseName + "_" + locale;
                String templateString = resources.getResourceAsString(localizedTemplate);
                if (templateString == null) {
                    log.warn("Reset passwords: Not found email body template for locale: '" + locale + "'");
                    bodyTemplate = bodyDefaultTemplate;
                } else {
                    try {
                        bodyTemplate = templateEngine.createTemplate(templateString);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                localizedBodyTemplates.put(locale, bodyTemplate);
            }
        }

        Template subjectTemplate;
        if (Locale.getDefault().getLanguage().equals(locale))
            subjectTemplate = subjectDefaultTemplate;
        else {
            if (localizedSubjectTemplates.containsKey(locale))
                subjectTemplate = localizedSubjectTemplates.get(locale);
            else {
                String baseName = FilenameUtils.getBaseName(resetPasswordSubjectTemplate);
                String localizedTemplate = baseName + "_" + locale;
                String templateString = resources.getResourceAsString(localizedTemplate);
                if (templateString == null) {
                    log.warn("Reset passwords: Not found email subject template for locale '" + locale + "'");
                    subjectTemplate = subjectDefaultTemplate;
                    localizedSubjectTemplates.put(locale, subjectDefaultTemplate);
                } else {
                    try {
                        subjectTemplate = templateEngine.createTemplate(templateString);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    localizedSubjectTemplates.put(locale, subjectTemplate);
                }
            }
        }

        return new EmailTemplate(subjectTemplate, bodyTemplate);
    }

    private Template loadDefaultTemplate(String templatePath, SimpleTemplateEngine templateEngine) {
        Template template;
        String defaultTemplateContent = resources.getResourceAsString(templatePath);
        if (defaultTemplateContent == null)
            throw new IllegalStateException("Not found default email template for reset passwords operation");

        try {
            template = templateEngine.createTemplate(defaultTemplateContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return template;
    }

    private void sendResetPasswordEmail(User user, String password, Template subjectTemplate, Template bodyTemplate) {
        Transaction tx = persistence.getTransaction();
        String emailBody;
        String emailSubject;
        try {
            Map<String, Object> binding = new HashMap<>();
            binding.put("user", user);
            binding.put("password", password);
            binding.put("persistence", persistence);

            emailBody = bodyTemplate.make(binding).writeTo(new StringWriter(0)).toString();
            emailSubject = subjectTemplate.make(binding).writeTo(new StringWriter(0)).toString();

            tx.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            tx.end();
        }

        EmailInfo emailInfo = new EmailInfo(user.getEmail(), emailSubject, null, null, null, emailBody);
        emailerAPI.sendMessagesAsync(emailInfo);
    }

    private Map<User, String> updateUserPasswords(List<UUID> userIds, boolean generatePassword) {
        Map<User, String> modifiedUsers = new LinkedHashMap<>();

        Transaction tx = persistence.getTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            em.setView(metadata.getViewRepository().getView(User.class, RESET_PASSWORD_VIEW));

            TypedQuery<User> query = em.createQuery("select u from sec$User u where u.id in (:userIds)", User.class);
            query.setParameter("userIds", userIds);

            List<User> users = query.getResultList();

            if (users == null || users.size() != userIds.size())
                throw new IllegalStateException("Not all users found in database");

            for (User user : users) {
                String password = null;
                if (generatePassword) {
                    password = encryption.generateRandomPassword();

                    HashDescriptor pwd = encryption.getPasswordHash(password);
                    user.setPassword(pwd.getHash());
                    user.setSalt(pwd.getSalt());
                }
                user.setChangePasswordAtNextLogon(true);

                modifiedUsers.put(user, password);
            }

            tx.commit();
        } finally {
            tx.end();
        }
        return modifiedUsers;
    }

    private Group cloneGroup(Group group, Group parent, EntityManager em) {
        Group groupClone = new Group();

        groupClone.setName(group.getName());
        groupClone.setParent(parent);

        em.persist(groupClone);
        // fire hierarchy listeners
        em.flush();

        if (group.getConstraints() != null) {
            for (Constraint constraint : group.getConstraints()) {
                Constraint constraintClone = cloneConstraint(constraint, groupClone);
                em.persist(constraintClone);
            }
        }

        if (group.getSessionAttributes() != null) {
            for (SessionAttribute attribute : group.getSessionAttributes()) {
                SessionAttribute attributeClone = cloneSessionAttribute(attribute, groupClone);
                em.persist(attributeClone);
            }
        }

        Query query = em.createQuery("select g from sec$Group g where g.parent.id = :group");
        query.setParameter("group", group);

        List subGroups = query.getResultList();
        if (subGroups != null && subGroups.size() > 0) {
            for (Object subGroupObject : subGroups) {
                Group subGroup = (Group) subGroupObject;
                cloneGroup(subGroup, groupClone, em);
            }
        }

        return groupClone;
    }

    private SessionAttribute cloneSessionAttribute(SessionAttribute attribute, Group group) {
        SessionAttribute resultAttribute = new SessionAttribute();
        resultAttribute.setName(attribute.getName());
        resultAttribute.setDatatype(attribute.getDatatype());
        resultAttribute.setStringValue(attribute.getStringValue());
        resultAttribute.setGroup(group);
        return resultAttribute;
    }

    private Constraint cloneConstraint(Constraint constraint, Group group) {
        Constraint resultConstraint = new Constraint();
        resultConstraint.setEntityName(constraint.getEntityName());
        resultConstraint.setJoinClause(constraint.getJoinClause());
        resultConstraint.setWhereClause(constraint.getWhereClause());
        resultConstraint.setGroup(group);
        return resultConstraint;
    }

    /**
     * Template pair : subject + body
     */
    private class EmailTemplate {

        private Template subjectTemplate;
        private Template bodyTemplate;

        private EmailTemplate(Template subjectTemplate, Template bodyTemplate) {
            this.subjectTemplate = subjectTemplate;
            this.bodyTemplate = bodyTemplate;
        }

        public Template getSubjectTemplate() {
            return subjectTemplate;
        }

        public Template getBodyTemplate() {
            return bodyTemplate;
        }
    }
}