/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.security;

import com.haulmont.cuba.client.ClientUserSession;
import com.haulmont.cuba.security.global.UserSession;

/**
 * User session that holds middleware session.
 *
 * @author artamonov
 * @version $Id$
 */
public class PortalSession extends ClientUserSession {
    private static final long serialVersionUID = 64089583666599524L;

    private volatile boolean authenticated; // indicates whether user passed authentication

    public PortalSession(UserSession src) {
        super(src);
        authenticated = false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
