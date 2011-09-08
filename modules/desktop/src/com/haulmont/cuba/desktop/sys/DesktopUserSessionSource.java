/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.sys;

import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AbstractUserSessionSource;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.security.global.UserSession;

import javax.annotation.ManagedBean;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@ManagedBean(UserSessionSource.NAME)
public class DesktopUserSessionSource extends AbstractUserSessionSource {

    @Override
    public boolean checkCurrentUserSession() {
        return App.getInstance().getConnection().isConnected() && App.getInstance().getConnection().getSession() != null;
    }

    @Override
    public UserSession getUserSession() {
        return App.getInstance().getConnection().getSession();
    }
}
