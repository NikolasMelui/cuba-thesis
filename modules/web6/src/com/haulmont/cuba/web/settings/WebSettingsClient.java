/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.settings;

import com.google.common.base.Optional;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.gui.settings.SettingsClient;
import com.haulmont.cuba.security.app.UserSettingService;
import com.haulmont.cuba.web.auth.RequestContext;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User settings provider for web application. Caches settings in HTTP session.
 *
 * @author artamonov
 * @version $Id$
 */
@ManagedBean(SettingsClient.NAME)
public class WebSettingsClient implements SettingsClient {

    @Inject
    protected UserSettingService userSettingService;

    @Override
    public String getSetting(String name) {
        Map<String, Optional<String>> settings = getCache();
        Optional<String> cached = settings.get(name);
        if (cached != null) {
            return cached.orNull();
        }

        String setting = userSettingService.loadSetting(ClientType.WEB, name);
        settings.put(name, Optional.fromNullable(setting));

        return setting;
    }

    @Override
    public void setSetting(String name, @Nullable String value) {
        getCache().put(name, Optional.fromNullable(value));
        userSettingService.saveSetting(ClientType.WEB, name, value);
    }

    protected Map<String, Optional<String>> getCache() {
        HttpSession session = RequestContext.get().getSession();
        @SuppressWarnings("unchecked")
        Map<String, Optional<String>> settings = (Map<String, Optional<String>>) session.getAttribute(SettingsClient.NAME);
        if (settings == null) {
            settings = new ConcurrentHashMap<>();
            session.setAttribute(SettingsClient.NAME, settings);
        }
        return settings;
    }

    public void clearCache() {
        HttpSession session = RequestContext.get().getSession();
        session.setAttribute(SettingsClient.NAME, null);
    }
}