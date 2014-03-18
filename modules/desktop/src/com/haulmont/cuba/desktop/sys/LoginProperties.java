/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.sys;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author devyatkin
 * @version $Id$
 */
public class LoginProperties {

    private Log log = LogFactory.getLog(LoginProperties.class);

    protected Properties properties = new Properties();

    protected String dataDir;

    protected static final String FILE_NAME = "login.properties";
    protected static final String LOGIN = "login";
    protected static final String LOCALE = "locale";

    public LoginProperties() {
        dataDir = AppBeans.get(Configuration.NAME, Configuration.class).getConfig(GlobalConfig.class).getDataDir();
        loadProperties();
    }

    public void save(String login, String locale) {
        properties.setProperty(LOGIN, login);
        properties.setProperty(LOCALE, locale);
        saveProperties();
    }

    public String loadLastLogin() {
        return properties.getProperty(LOGIN);
    }

    public String loadLastLocale() {
        return properties.getProperty(LOCALE);
    }

    protected void loadProperties() {
        File propertiesFile = new File(dataDir, FILE_NAME);
        try {
            if (propertiesFile.exists()) {
                FileInputStream stream = FileUtils.openInputStream(propertiesFile);
                try {
                    properties.load(stream);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
        } catch (IOException e) {
            log.error("Error loading login properties", e);
        }
    }

    protected void saveProperties() {
        File propertiesFile = new File(dataDir, "login.properties");
        try {
            FileOutputStream stream = FileUtils.openOutputStream(propertiesFile);
            try {
                properties.store(stream, "Login properties");
            } finally {
                IOUtils.closeQuietly(stream);
            }
        } catch (IOException e) {
            log.error("Error saving login properties", e);
        }
    }
}
