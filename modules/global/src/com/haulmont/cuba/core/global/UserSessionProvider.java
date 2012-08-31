/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.global;

import com.haulmont.cuba.security.global.UserSession;

import java.util.Locale;
import java.util.UUID;

/**
 * DEPRECATED - use {@link UserSessionSource} via DI or <code>AppBeans.get(UserSessionSource.class)</code>
 *
 * @author krivopustov
 * @version $Id$
 */
@Deprecated
public abstract class UserSessionProvider {

    private static UserSessionSource getSource() {
        return AppBeans.get(UserSessionSource.NAME, UserSessionSource.class);
    }

    /**
     * @return current user session
     */
    public static UserSession getUserSession() {
        return getSource().getUserSession();
    }

    /**
     * @return effective user ID. This is either the logged in user, or substituted user if a substitution was performed
     * in this user session.
     */
    public static UUID currentOrSubstitutedUserId() {
        return getSource().currentOrSubstitutedUserId();
    }

    /**
     * @return current user session locale
     */
    public static Locale getLocale() {
        return getSource().getLocale();
    }
}
