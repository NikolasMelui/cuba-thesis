/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.11.2008 14:05:10
 *
 * $Id$
 */
package com.haulmont.cuba.security.app;

import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

/**
 * Interface to {@link com.haulmont.cuba.security.app.LoginWorkerBean}
 */
public interface LoginWorker {

    String NAME = "cuba_LoginWorker";

    /**
     * @see LoginService#login(String, String, java.util.Locale)
     */
    UserSession login(String login, String password, Locale locale) throws LoginException;

    /**
     * @see LoginService#loginTrusted(String, String, java.util.Locale)
     */
    UserSession loginTrusted(String login, String password, Locale locale) throws LoginException;

    /**
     * @see LoginService#logout()
     */
    void logout();

    /**
     * @see LoginService#substituteUser(User)
     */
    UserSession substituteUser(User substitutedUser);

    /**
     * @see LoginService#getSession(UUID)
     */
    @Nullable
    UserSession getSession(UUID sessionId);

    /**
     * Log in from a middleware component. This method should not be exposed to any client tier.
     *
     * @param login    login of a system user
     * @return system user session that is not replicated in cluster
     * @throws LoginException in case of unsuccessful log in
     */
    UserSession loginSystem(String login) throws LoginException;
}
