/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.auth.ActiveDirectoryConnection;
import com.haulmont.cuba.web.auth.ActiveDirectoryHelper;
import com.haulmont.cuba.web.auth.WebAuthConfig;

import java.util.Locale;

/**
 * Default {@link Connection} implementation for web-client.
 *
 * @author gorodnov
 * @version $Id$
 */
public class DefaultConnection extends AbstractConnection implements ActiveDirectoryConnection {

    protected Configuration configuration = AppBeans.get(Configuration.NAME);

    @Override
    public void login(String login, String password, Locale locale) throws LoginException {
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null");
        }

        update(loginService.login(login, password, locale,
                ParamsMap.of(ClientType.class.getSimpleName(), ClientType.WEB.name())));
    }

    @Override
    public void loginByRememberMe(String login, String rememberMeToken, Locale locale) throws LoginException {
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null");
        }

        update(loginService.loginByRememberMe(login, rememberMeToken, locale,
                ParamsMap.of(ClientType.class.getSimpleName(), ClientType.WEB.name())));
    }

    @Override
    public void loginActiveDirectory(String login, Locale locale) throws LoginException {
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null");
        }

        String password = configuration.getConfig(WebAuthConfig.class).getTrustedClientPassword();
        update(loginService.loginTrusted(login, password, locale,
                ParamsMap.of(ClientType.class.getSimpleName(), ClientType.WEB.name())));

        UserSession session = getSession();
        if (session == null) {
            throw new IllegalStateException("Null session after login");
        }
        session.setAttribute(ACTIVE_DIRECTORY_USER_SESSION_ATTRIBUTE, true);
    }

    @Override
    public String logout() {
        super.logout();
        return ActiveDirectoryHelper.useActiveDirectory() ? "login" : "";
    }

    @Override
    public boolean checkRememberMe(String login, String rememberMeToken) {
        return loginService.checkRememberMe(login, rememberMeToken);
    }
}