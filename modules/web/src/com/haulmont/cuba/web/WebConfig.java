/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.*;
import com.haulmont.cuba.core.config.type.Factory;
import com.haulmont.cuba.core.config.type.StringListTypeFactory;

import java.util.List;

/**
 * Web Client configuration parameters interface.
 *
 * @author krivopustov
 * @version $Id$
 */
@Source(type = SourceType.APP)
public interface WebConfig extends Config {

    /**
     * @return Whether to use local invocations instead of HTTPInvoker. Makes sense for improving performance,
     * if the WEB and CORE applications started on the same JVM (same Tomcat instance).
     */
    @Property("cuba.useLocalServiceInvocation")
    @DefaultBoolean(true)
    boolean getUseLocalServiceInvocation();

    /**
     * @return Password used by LoginService.loginTrusted() method.
     * Trusted client may login without providing a user password. This is used by ActiveDirectory integration.
     *
     * <p>Must be equal to password set for the same property on the CORE.</p>
     */
    @Property("cuba.trustedClientPassword")
    @DefaultString("")
    String getTrustedClientPassword();

    /**
     * @return Default user login to set in the login dialog.
     */
    @Property("cuba.web.loginDialogDefaultUser")
    String getLoginDialogDefaultUser();

    /**
     * @return Default user password to set in the login dialog.
     */
    @Property("cuba.web.loginDialogDefaultPassword")
    String getLoginDialogDefaultPassword();

    /**
     * @return HTTP session expiration timeout in seconds.<br/>
     * Should be equals or less than middleware user session timeout <code>cuba.userSessionExpirationTimeoutSec</code>
     */
    @Property("cuba.httpSessionExpirationTimeoutSec")
    @DefaultInt(1800)
    int getHttpSessionExpirationTimeoutSec();

    /**
     * @return Whether to use the ActiveDirectory authentication
     */
    @Property("cuba.web.useActiveDirectory")
    @DefaultBoolean(false)
    boolean getUseActiveDirectory();

    @Property("cuba.web.activeDirectoryDebug")
    @DefaultBoolean(false)
    boolean getActiveDirectoryDebug();

    /**
     * @return ActiveDirectory domains configuration info
     */
    @Property("cuba.web.activeDirectoryDomains")
    String getActiveDirectoryDomains();

    /**
     * @return Short/User-friendly domain aliases for login window form
     */
    @Property("cuba.web.activeDirectoryAliases")
    String getActiveDirectoryAliases();

    /**
     * @return ActiveDirectory authentification provider
     */
    @Property("cuba.web.activeDirectoryAuthClass")
    @DefaultString("com.haulmont.cuba.web.sys.auth.KerberosAuthProvider")
    String getActiveDirectoryAuthClass();

    /**
     * @return Kerberos domain and realms config (krb5.ini)
     */
    @Property("cuba.web.kerberosConf")
    String getKerberosConf();

    /**
     * @return Kerberos login module config (jaas.conf)
     */
    @Property("cuba.web.kerberosJaasConf")
    String getKerberosJaasConf();

    /**
     * @return Kerberos single-sign-on module in JaasConf
     */
    @Property("cuba.web.kerberosLoginModule")
    String getKerberosLoginModule();

    /**
     * @return Kerberos auth module in JaasConf
     */
    @Property("cuba.web.kerberosAuthModule")
    String getKerberosAuthModule();

    /**
     *  @return Comma-separated list of URLs for CubaHttpFilter to bypass.
     */
    @Property("cuba.web.cubaHttpFilterBypassUrls")
    @Default("/ws/")
    String getCubaHttpFilterBypassUrls();

    /**
     * @return Default main window mode.
     * Takes place until the user did not change its own preference through user settings.
     */
    @Property("cuba.web.appWindowMode")
    @Default("TABBED")
    String getAppWindowMode();

    /**
     * @return Maximum number of open tabs. 0 for unlimited.
     */
    @Property("cuba.web.maxTabCount")
    @DefaultInt(0)
    int getMaxTabCount();

    /**
     * @return Whether to use native HTML buttons
     */
    @Property("cuba.web.useNativeButtons")
    @DefaultBoolean(false)
    boolean getUseNativeButtons();

    /**
     * @return If true and if IE browser is used, we suggest to install Chrome frame IE plugin.
     */
    @Property("cuba.web.useChromeFramePlugin")
    @DefaultBoolean(false)
    boolean getUseChromeFramePlugin();

    /**
     * @return Whether to close DateField calendar popup right after date is selected.
     * Default behaviour is the following: if the DateField contains time, the calendar popup allows to select time and
     * closes only when user clicks somewhere outside the popup.
     */
    @Property("cuba.web.closeCalendarWhenDateSelected")
    @DefaultBoolean(false)
    boolean getCloseCalendarWhenDateSelected();

    /**
     * @return Request execution time in seconds, after which a message log will be logged.
     */
    @Property("cuba.web.logLongRequestsThresholdSec")
    @DefaultInt(5)
    int getLogLongRequestsThresholdSec();

    /**
     * @return Whether to enable the Folders Pane functionality.
     */
    @Property("cuba.web.foldersPaneEnabled")
    @DefaultBoolean(true)
    boolean getFoldersPaneEnabled();

    /**
     * @return AppFolders refresh period in seconds.
     */
    @Property("cuba.web.appFoldersRefreshPeriodSec")
    @DefaultInt(180)
    int getAppFoldersRefreshPeriodSec();

    /**
     * @return Whether to use icons for folders.
     */
    @Property("cuba.web.showFolderIcons")
    @DefaultBoolean(false)
    boolean getShowFolderIcons();

    /**
     * @return Whether to show {@link com.haulmont.cuba.web.app.folders.FoldersPane} on first login.
     */
    @Property("cuba.web.foldersPaneVisibleByDefault")
    @DefaultBoolean(false)
    boolean getFoldersPaneVisibleByDefault();

    /**
     * @return Default {@link com.haulmont.cuba.web.app.folders.FoldersPane} width.
     */
    @Property("cuba.web.foldersPaneDefaultWidth")
    @DefaultInt(200)
    int getFoldersPaneDefaultWidth();

    /**
     * @return Maximum number of symbols in main tabs captions.
     */
    @Property("cuba.web.mainTabCaptionLength")
    @DefaultInt(25)
    int getMainTabCaptionLength();

    /**
     * @return Whether to use disabling and dimming of browser window on long requests.
     */
    @Property("cuba.web.useUiBlocking")
    @DefaultBoolean(true)
    boolean getUseUiBlocking();

    /**
     * @return Theme
     */
    @Default("iceland")
    @Property("cuba.web.theme")
    String getAppWindowTheme();

    /**
     * @return Supported web themes
     */
    @Property("cuba.web.availableAppThemes")
    @Factory(factory = StringListTypeFactory.class)
    @Default("peyto|havana|iceland")
    List<String> getAvailableAppThemes();

    /**
     * @return Whether to use lightweight header (app icon, menu, app settings in the same row)
     */
    @DefaultBoolean(true)
    @Property("cuba.web.useLightHeader")
    boolean getUseLightHeader();

    /**
     * @return Path to an image for use as wallpaper. Not used in the platform.
     */
    @Property("cuba.web.appWindowWallpaper")
    @Default("default")
    String getAppWindowWallpaper();

    /**
     * @return List of file extensions which should be shown in the browser instead of downloading as attachments.
     */
    @Property("cuba.web.viewFileExtensions")
    @Factory(factory = StringListTypeFactory.class)
    @Default("htm|html|jpg|png|jpeg|pdf")
    List<String> getViewFileExtensions();

    @Property("cuba.web.resourcesRoot")
    String getResourcesRoot();

    /**
     * @return Whether to generate unique suffixes for Ids in testing mode
     */
    @Property("cuba.web.allowIdSuffix")
    @DefaultBoolean(false)
    boolean getAllowIdSuffix();

    /**
     * @return Whether to enable cancel sorting of table columns. If true, each third click on the column will cancel
     * sorting instead of reversing it.
     */
    @Property("cuba.web.enableCancelTableSorting")
    @DefaultBoolean(true)
    boolean getEnableCancelTableSorting();

    /**
     * Supports automatic testing.
     * @return a name of request parameter that marks a request from an automatic testing tool, for example jMeter.
     */
    @Property("cuba.web.testModeParamName")
    @Default("jmeter")
    String getTestModeParamName();

    /**
     * List of entity ids which can be restored by means of the restore screen.
     * <p>Obsolete. Recommended way to specify this information is entity annotations
     * in <code>*-metadata.xml</code></p>
     * @return comma-separated list of entity ids
     */
    @Property("cuba.restoreScreenEntityIds")
    @Deprecated
    String getRestoreEntityId();

    /**
     * Support e-mail. All feedback mails will be sent on this address.
     */
    @Property("cuba.supportEmail")
    @DefaultString("cubasupport@haulmont.com")
    String getSupportEmail();

    /**
     * Timeout for check changes from browser <br/>
     * Used by BackgroundWorker for timers
     *
     * @return Timeout in ms
     */
    @Property("cuba.backgroundWorker.uiCheckInterval")
    @DefaultInteger(2000)
    Integer getUiCheckInterval();

    /**
     * @return Maximum number of active background tasks
     */
    @Property("cuba.backgroundWorker.maxActiveTasksCount")
    @DefaultInteger(100)
    Integer getMaxActiveBackgroundTasksCount();

    @Property("cuba.backgroundWorker.maxClientLatencySeconds")
    @DefaultInt(60)
    int getClientBackgroundTasksLatencySeconds();

    /**
     * @return System ID. Use for identification (support emails).
     */
    @Property("cuba.systemId")
    @DefaultString("CUBA")
    String getSystemID();
}
