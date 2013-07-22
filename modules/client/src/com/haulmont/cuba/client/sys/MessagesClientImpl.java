/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.client.sys;

import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.app.LocalizedMessageService;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AbstractMessages;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(Messages.NAME)
public class MessagesClientImpl extends AbstractMessages {

    @Inject
    protected LocalizedMessageService localizedMessageService;

    @Inject
    protected UserSessionSource userSessionSource;

    protected volatile boolean remoteSearch = true;

    protected ClientConfig clientConfig;

    @Inject
    @Override
    public void setConfiguration(Configuration configuration) {
        super.setConfiguration(configuration);
        clientConfig = configuration.getConfig(ClientConfig.class);
    }

    @Override
    protected Locale getUserLocale() {
        UserSession userSession = userSessionSource.getUserSession();
        return userSession != null ? userSession.getLocale() : Locale.getDefault();
    }

    @Override
    protected String searchRemotely(String pack, String key, Locale locale) {
        if (!remoteSearch || !AppContext.isStarted())
            return null;

        if (log.isTraceEnabled())
            log.trace("searchRemotely: " + pack + "/" + locale + "/" + key);

        try {
            String message = localizedMessageService.getMessage(pack, key, locale);
            if (key.equals(message))
                return null;
            else
                return message;
        } catch (Exception e) {
            List list = ExceptionUtils.getThrowableList(e);
            for (Object throwable : list) {
                if (throwable instanceof SocketException) {
                    log.trace("searchRemotely: " + throwable);
                    return null; // silently ignore network errors
                }
            }
            throw (RuntimeException) e;
        }
    }

    public boolean isRemoteSearch() {
        return remoteSearch;
    }

    public void setRemoteSearch(boolean remoteSearch) {
        this.remoteSearch = remoteSearch && clientConfig.getRemoteMessagesSearchEnabled();
    }
}
