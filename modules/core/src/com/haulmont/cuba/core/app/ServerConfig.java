/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.Source;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.config.defaults.Default;
import com.haulmont.cuba.core.config.defaults.DefaultBoolean;
import com.haulmont.cuba.core.config.defaults.DefaultInt;
import com.haulmont.cuba.core.config.defaults.DefaultString;

/**
 * Configuration parameters interface used by the CORE layer.
 *
 * @author krivopustov
 * @version $Id$
 */
@Source(type = SourceType.APP)
public interface ServerConfig extends Config {

    /**
     * @return URL of user session provider - usually the main application core.
     * This URL is used by modules which don't login themselves but get existing sessions from main app.
     */
    @Property("cuba.userSessionProviderUrl")
    String getUserSessionProviderUrl();

    /**
     * @return Password used by LoginService.loginTrusted() method.
     * Trusted client may login without providing a user password. This is used by ActiveDirectory integration.
     *
     * <p>Must be equal to password set for the same property on the client.</p>
     */
    @Property("cuba.trustedClientPassword")
    @DefaultString("")
    String getTrustedClientPassword();

    @Property("cuba.security.resetPasswordTemplateBody")
    @Default("/com/haulmont/cuba/security/app/email/reset-password-body.gsp")
    String getResetPasswordEmailBodyTemplate();

    @Property("cuba.security.resetPasswordTemplateSubject")
    @Default("/com/haulmont/cuba/security/app/email/reset-password-subject.gsp")
    String getResetPasswordEmailSubjectTemplate();

    /**
     * @return User session expiration timeout in seconds.
     * Not the same as HTTP session timeout, but should have the same value.
     */
    @Property("cuba.userSessionExpirationTimeoutSec")
    @DefaultInt(1800)
    int getUserSessionExpirationTimeoutSec();
    void setUserSessionExpirationTimeoutSec(int timeout);

    /**
     * @return DB scripts directory.
     * Does not end with "/"
     */
    @Property("cuba.dbDir")
    String getDbDir();

    /**
     * @return Whether the server should try to init/update database on each startup.
     */
    @Property("cuba.automaticDatabaseUpdate")
    @DefaultBoolean(false)
    boolean getAutomaticDatabaseUpdate();

    /**
     * @return {@link FileStorageAPI} storage directory. If not set, <code>cuba.dataDir/filestorage</code> will be used.
     */
    @Property("cuba.fileStorageDir")
    String getFileStorageDir();

    /**
     * @return Whether to cut query text when logging it.
     */
    @Property("cuba.log.cutLoadListQueries")
    @DefaultBoolean(false)
    boolean getCutLoadListQueries();

    /**
     * @return Scheduled tasks execution control.
     */
    @Property("cuba.schedulingActive")
    @DefaultBoolean(false)
    boolean getSchedulingActive();
    void setSchedulingActive(boolean value);

    /**
     * @return Path to resource containing the release number
     */
    @Property("cuba.releaseNumberPath")
    @DefaultString("/com/haulmont/cuba/core/global/release.number")
    String getReleaseNumberPath();

    /**
     * @return Path to resource containing the release timestamp
     */
    @Property("cuba.releaseTimestampPath")
    @DefaultString("/com/haulmont/cuba/core/global/release.timestamp")
    String getReleaseTimestampPath();

    /**
     * @return Tells DataService to ensure distinct results by processing them in memory, instead of issue
     * 'select distinct' to the database.
     */
    @Property("cuba.inMemoryDistinct")
    @DefaultBoolean(false)
    boolean getInMemoryDistinct();
    void setInMemoryDistinct(boolean value);

    /**
     * @return Default database query timeout in seconds. If 0, middleware doesn't apply any timeout to queries.
     */
    @Property("cuba.defaultQueryTimeoutSec")
    int getDefaultQueryTimeoutSec();
    void setDefaultQueryTimeoutSec(int timeout);
}
