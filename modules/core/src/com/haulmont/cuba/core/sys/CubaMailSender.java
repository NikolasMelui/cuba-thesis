/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.app.EmailerConfig;
import com.haulmont.cuba.core.global.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.mail.Session;
import java.util.Properties;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(CubaMailSender.NAME)
public class CubaMailSender extends JavaMailSenderImpl {

    public static final String NAME = "cuba_MailSender";

    protected EmailerConfig config;

    private boolean propertiesInitialized;

    @Inject
    public void setConfiguration(Configuration configuration) {
        config = configuration.getConfig(EmailerConfig.class);
    }

    @Override
    public String getHost() {
        return config.getSmtpHost();
    }

    @Override
    public void setHost(String host) {
        throw new UnsupportedOperationException("Use cuba.email.* properties");
    }

    @Override
    public int getPort() {
        return config.getSmtpPort();
    }

    @Override
    public void setPort(int port) {
        throw new UnsupportedOperationException("Use cuba.email.* properties");
    }

    @Override
    public String getUsername() {
        return config.getSmtpAuthRequired() && !StringUtils.isBlank(config.getSmtpUser()) ?
                config.getSmtpUser() : null;
    }

    @Override
    public void setUsername(String username) {
        throw new UnsupportedOperationException("Use cuba.email.* properties");
    }

    @Override
    public String getPassword() {
        return config.getSmtpAuthRequired() && !StringUtils.isBlank(config.getSmtpPassword()) ?
                config.getSmtpPassword() : null;
    }

    @Override
    public void setPassword(String password) {
        throw new UnsupportedOperationException("Use cuba.email.* properties");
    }

    @Override
    public synchronized Session getSession() {
        if (!propertiesInitialized) {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.auth", String.valueOf(config.getSmtpAuthRequired()));
            properties.setProperty("mail.smtp.starttls.enable", String.valueOf(config.getSmtpStarttlsEnable()));
            setJavaMailProperties(properties);
            propertiesInitialized = true;
        }
        return super.getSession();
    }
}
