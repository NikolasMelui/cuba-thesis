/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(ServerInfoAPI.NAME)
public class ServerInfo implements ServerInfoAPI
{
    protected Log log = LogFactory.getLog(getClass());

    protected String releaseNumber = "?";
    protected String releaseTimestamp = "?";

    protected Configuration configuration;

    protected volatile String serverId;

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        ServerConfig config = configuration.getConfig(ServerConfig.class);

        InputStream stream = getClass().getResourceAsStream(config.getReleaseNumberPath());
        if (stream != null)
            try {
                releaseNumber = IOUtils.toString(stream);
            } catch (IOException e) {
                log.warn("Unable to read release number", e);
            }

        stream = getClass().getResourceAsStream(config.getReleaseTimestampPath());
        if (stream != null)
            try {
                releaseTimestamp = IOUtils.toString(stream);
            } catch (IOException e) {
                log.warn("Unable to read release timestamp", e);
            }
    }

    public String getReleaseNumber() {
        return releaseNumber;
    }

    public String getReleaseTimestamp() {
        return releaseTimestamp;
    }

    @Override
    public String getServerId() {
        if (serverId == null) {
            GlobalConfig globalConfig = configuration.getConfig(GlobalConfig.class);
            serverId = globalConfig.getWebHostName() + ":" + globalConfig.getWebPort() + "/" + globalConfig.getWebContextName();
        }
        return serverId;
    }
}
