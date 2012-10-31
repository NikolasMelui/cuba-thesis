/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.sys.security;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.PasswordEncryption;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.portal.config.PortalConfig;
import com.haulmont.cuba.portal.sys.exceptions.NoMiddlewareConnectionException;
import com.haulmont.cuba.security.app.LoginService;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.NoUserSessionException;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * @author artamonov
 * @version $Id$
 */
@ManagedBean("cuba_PortalAnonymousSessionHolder")
public class AnonymousSessionHolder {

    private Log log = LogFactory.getLog(getClass());

    @Inject
    protected Configuration configuration;

    @Inject
    protected PasswordEncryption passwordEncryption;

    @Inject
    protected LoginService loginService;

    @Inject
    protected UserSessionService userSessionService;

    private volatile UserSession anonymousSession;

    public UserSession getSession() {
        boolean justLoggedIn = false;
        if (anonymousSession == null) {
            synchronized (this) {
                if (anonymousSession == null) {
                    anonymousSession = loginAsAnonymous();
                    justLoggedIn = true;
                }
            }
        }
        if (!justLoggedIn) {
            pingSession(anonymousSession);
        }
        return anonymousSession;
    }

    private UserSession loginAsAnonymous() {
        PortalConfig config = configuration.getConfig(PortalConfig.class);
        String login = config.getTrustedClientLogin();
        String password = config.getTrustedClientPassword();

        Locale defaulLocale = new Locale(config.getDefaultLocale());
        UserSession userSession;
        try {
            userSession = loginService.loginTrusted(login, password, defaulLocale);
            // Set client info on middleware
            AppContext.setSecurityContext(new SecurityContext(userSession));

            String portalLocationString = getPortalNetworkLocation();
            String portalClientInfo = "Portal Anonymous Session";
            if (StringUtils.isNotBlank(portalLocationString))
                portalClientInfo += " from : " + portalLocationString;

            userSessionService.setSessionClientInfo(userSession.getId(), portalClientInfo);
            AppContext.setSecurityContext(null);
        } catch (LoginException e) {
            throw new NoMiddlewareConnectionException("Unable to login as anonymous portal user", e);
        } catch (Exception e) {
            throw new NoMiddlewareConnectionException("Unable to connect to middleware services", e);
        }
        return userSession;
    }

    private String getPortalNetworkLocation() {
        String portalLocationString = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            // Get IP Address
            String ipAddr = addr.getHostAddress();
            // Get hostname
            String hostname = addr.getHostName();
            portalLocationString = hostname + " " + ipAddr;
        } catch (UnknownHostException ignored) {
        }
        return portalLocationString;
    }

    /**
     * Scheduled ping session
     */
    @SuppressWarnings("unused")
    public void pingSession() {
        // only if anonymous session initialized
        UserSession session = anonymousSession;
        if (session != null) {
            pingSession(session);
        }
    }

    private void pingSession(UserSession userSession) {
        AppContext.setSecurityContext(new SecurityContext(userSession));
        try {
            userSessionService.getMessages();
        } catch (NoUserSessionException e) {
            log.warn("Anonymous session has been lost, try restore");
            // auto restore anonymous session
            anonymousSession = null;
            getSession();
        }
        AppContext.setSecurityContext(null);
    }
}
