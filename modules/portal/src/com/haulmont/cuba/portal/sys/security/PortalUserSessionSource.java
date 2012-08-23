/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.sys.security;

import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AbstractUserSessionSource;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.portal.App;
import com.haulmont.cuba.portal.Connection;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.UserSession;

import javax.annotation.ManagedBean;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(UserSessionSource.NAME)
public class PortalUserSessionSource extends AbstractUserSessionSource {

    protected UserSessionService userSessionService;

    protected PortalSessionFactory portalSessionFactory;

    public void setUserSessionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public void setPortalSessionFactory(PortalSessionFactory portalSessionFactory) {
        this.portalSessionFactory = portalSessionFactory;
    }

    @Override
    public boolean checkCurrentUserSession() {
        if (App.isBound()) {
            Connection connection = App.getInstance().getConnection();
            return connection.isConnected() && connection.getSession() != null;
        } else {
            SecurityContext securityContext = AppContext.getSecurityContext();
            if (securityContext == null)
                return false;

            if (securityContext.getSession() != null)
                return true;
            else {
                try {
                    userSessionService.getUserSession(securityContext.getSessionId());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
    }

    @Override
    public UserSession getUserSession() {
        if (App.isBound())
            return App.getInstance().getConnection().getSession();
        else {
            SecurityContext securityContext = AppContext.getSecurityContext();
            if (securityContext == null)
                throw new IllegalStateException("No security context bound to the current thread");

            if (securityContext.getSession() != null)
                return securityContext.getSession();
            else {
                UserSession userSession = userSessionService.getUserSession(securityContext.getSessionId());
                return portalSessionFactory.createPortalSession(userSession, null);
            }
        }
    }
}
