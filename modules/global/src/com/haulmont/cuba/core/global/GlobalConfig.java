/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.global;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultString;
import com.haulmont.cuba.core.config.type.Factory;
import com.haulmont.cuba.core.sys.AvailableLocalesFactory;

import java.util.Locale;
import java.util.Map;

/**
 * Configuration parameters interface used by all layers: CORE, WEB, DESKTOP.
 *
 * @author krivopustov
 * @version $Id$
 */
@Source(type = SourceType.APP)
public interface GlobalConfig extends Config {

    /**
     * @return This web application host name. Makes sense for CORE and WEB modules.
     */
    @Property("cuba.webHostName")
    @DefaultString("localhost")
    String getWebHostName();

    /**
     * @return This web application port. Makes sense for CORE and WEB modules.
     */
    @Property("cuba.webPort")
    @DefaultString("8080")
    String getWebPort();

    /**
     * @return This web application context name. Makes sense for CORE and WEB modules.
     */
    @Property("cuba.webContextName")
    @DefaultString("cuba")
    String getWebContextName();

    /**
     * @return Web-client connection URL. Used for making external links to the application screens and for other purposes.
     */
    @Property("cuba.webAppUrl")
    @DefaultString("http://localhost:8080/app")
    String getWebAppUrl();

    /**
     * @return Base url for dispatcher servlet of web application. Specific for core, web and portal blocks.
     */
    @Property("cuba.dispatcherBaseUrl")
    String getDispatcherBaseUrl();

    /**
     * @return REST API connection URL
     */
    @Property("cuba.restApiUrl")
    @DefaultString("http://localhost:8080/app-portal/api")
    String getRestApiUrl();

    /**
     * @return Configuration directory. {@link Scripting} searches for dynamic resources here.
     * Must not end with "/"
     */
    @Property("cuba.confDir")
    String getConfDir();

    /**
     * @return Logs directory. Place app-specific log files here.
     * Must not end with "/"
     */
    @Property("cuba.logDir")
    String getLogDir();

    /**
     * @return Temporary files directory. Place app-specific temp files here.
     * Must not end with "/"
     */
    @Property("cuba.tempDir")
    String getTempDir();

    /**
     * @return Data directory. Place persistent app-specific data files here.
     * Must not end with "/"
     */
    @Property("cuba.dataDir")
    String getDataDir();

    /**
     * Automatic testing mode indication.
     * @return true if in test mode
     */
    @Property("cuba.testMode")
    @DefaultBoolean(false)
    boolean getTestMode();

    /**
     * Supported locales. List of locales is shown on user login.
     * @return map of labels to locales
     */
    @Property("cuba.availableLocales")
    @Factory(factory = AvailableLocalesFactory.class)
    @Default("English|en;Russian|ru;French|fr")
    Map<String, Locale> getAvailableLocales();

    /**
     * Show locale select in LoginWindow.
     * @return true if show
     */
    @Property("cuba.localeSelectVisible")
    @DefaultBoolean(true)
    boolean getLocaleSelectVisible();

    /**
     * Whether to use {@link com.haulmont.cuba.core.sys.jpql.transform.QueryTransformerAstBased} instead of
     * {@link QueryTransformerRegex}
     * @return true or false
     */
    @Property("cuba.useAstBasedJpqlTransformer")
    @DefaultBoolean(false)
    boolean getUseAstBasedJpqlTransformer();

    /**
     * @return Overriden AppFolderEditWindow class name
     */
    @Property("cuba.web.appFolderEditWindow")
    String getAppFolderEditWindowClassName();

    /**
     * @return Allows to aplly a filter to previously selected rows
     */
    @Property("cuba.allowQueryFromSelected")
    @DefaultBoolean(true)
    boolean getAllowQueryFromSelected();

    /**
     * @return Classpath directories for dynamic class loader. Separated by ;
     */
    @Property("cuba.classpath.directories")
    String getCubaClasspathDirectories();
}

