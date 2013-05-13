/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.controllers;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.security.app.LoginService;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppUI;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;

/**
 * @author gorodnov
 * @version $Id$
 */
public abstract class ControllerUtils {
    private static final String DISPATCHER = "dispatch";

    public static String getLocationWithoutParams() {
        URI location = AppUI.getCurrent().getPage().getLocation();
        try {
            StringBuilder baseUrl = new StringBuilder(location.toURL().toExternalForm());
            if (location.getQuery() != null) {
                baseUrl.delete(baseUrl.indexOf(location.getQuery()) - 1, baseUrl.length());
            }
            return baseUrl.toString();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getWebControllerURL(String mapping) {
        if (mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
        String baseUrl = getLocationWithoutParams();

        StringBuilder url = new StringBuilder(baseUrl).append(getDispatcher());
        if (!mapping.startsWith("/")) {
            url.append("/");
        }
        url.append(mapping);
        return url.toString();
    }

    public static String getControllerURL(String mapping) {
        if (mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
        GlobalConfig globalConfig = AppBeans.get(Configuration.class).getConfig(GlobalConfig.class);

        StringBuilder sb = new StringBuilder(globalConfig.getWebAppUrl()).append(getContollerPrefix());
        if (!mapping.startsWith("/")) {
            sb.append("/");
        }
        sb.append(mapping);
        return sb.toString();
    }

    public static String getDispatcher() {
        return DISPATCHER;
    }

    public static String getContollerPrefix() {
        return "/" + DISPATCHER;
    }

    public static String getControllerPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.startsWith(getContollerPrefix())) {
            path = path.substring(getContollerPrefix().length());
        }
        return path;
    }

    public static UserSession getUserSession(HttpServletRequest req) {
        UserSession userSession = (UserSession) req.getSession().getAttribute(App.USER_SESSION_ATTR);
        if (userSession != null) {
            return userSession;
        } else {
            String s = req.getParameter("s");
            if (s != null) {
                try {
                    UUID id = UUID.fromString(s);
                    LoginService service = AppBeans.get(LoginService.class);
                    UserSession session = service.getSession(id);
                    if (session != null) {
                        req.getSession().setAttribute(App.USER_SESSION_ATTR, session);
                        return session;
                    }
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }
    }
}
