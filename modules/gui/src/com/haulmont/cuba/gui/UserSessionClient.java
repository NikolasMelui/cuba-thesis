/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 01.04.2009 15:17:01
 *
 * $Id$
 */
package com.haulmont.cuba.gui;

import com.haulmont.cuba.security.global.UserSession;

import java.util.UUID;

/**
 * GenericUI class providing access to the current user session
 */
public abstract class UserSessionClient
{
    public static final String IMPL_PROP = "cuba.UserSessionClient.impl";
    private static final String DEFAULT_IMPL = "com.haulmont.cuba.web.sys.UserSessionClientImpl";

    private static UserSessionClient instance;

    private static UserSessionClient getInstance() {
        if (instance == null) {
            String implClassName = System.getProperty(IMPL_PROP);
            if (implClassName == null)
                implClassName = DEFAULT_IMPL;
            try {
                Class implClass = Thread.currentThread().getContextClassLoader().loadClass(implClassName);
                instance = (UserSessionClient) implClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    /**
     * Current user session
     */
    public static UserSession getUserSession() {
        return getInstance().__getUserSession();
    }

    public static UUID currentOrSubstitutedUserId() {
        UserSession us = getInstance().__getUserSession();
        return us.getSubstitutedUser() != null ? us.getSubstitutedUser().getId() : us.getUser().getId();
    }

    protected abstract UserSession __getUserSession();
}
