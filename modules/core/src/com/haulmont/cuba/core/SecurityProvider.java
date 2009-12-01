/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 11.11.2008 18:27:17
 *
 * $Id$
 */
package com.haulmont.cuba.core;

import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.core.global.QueryTransformer;
import com.haulmont.cuba.core.global.QueryTransformerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 * Provides access to the current user session for middleware
 */
public abstract class SecurityProvider
{
    public static final String IMPL_PROP = "cuba.SecurityProvider.impl";

    private static final String DEFAULT_IMPL = "com.haulmont.cuba.core.sys.SecurityProviderImpl";

    public static final String CONSTRAINT_PARAM_SESSION_ATTR = "session$";
    public static final String CONSTRAINT_PARAM_USER_LOGIN = "userLogin";
    public static final String CONSTRAINT_PARAM_USER_ID = "userId";
    public static final String CONSTRAINT_PARAM_USER_GROUP_ID = "userGroupId";

    private static SecurityProvider instance;

    private static SecurityProvider getInstance() {
        if (instance == null) {
            String implClassName = System.getProperty(IMPL_PROP);
            if (implClassName == null)
                implClassName = DEFAULT_IMPL;
            try {
                Class implClass = Thread.currentThread().getContextClassLoader().loadClass(implClassName);
                instance = (SecurityProvider) implClass.newInstance();
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
     * Current (logged in) user identifier
     */
    public static UUID currentUserId() {
        return getInstance().__currentUserSession().getUser().getId();
    }

    /**
     * Returns substituted user ID if there is one, otherwise returns logged in user ID
     */
    public static UUID currentOrSubstitutedUserId() {
        UserSession us = getInstance().__currentUserSession();
        return us.getSubstitutedUser() != null ? us.getSubstitutedUser().getId() : us.getUser().getId();
    }

    /**
     * Current user session
     */
    public static UserSession currentUserSession() {
        return getInstance().__currentUserSession();
    }

    /**
     * Checks if the current user belongs to role
     * @param role role name
     */
    public static boolean currentUserInRole(String role) {
        UserSession session = getInstance().__currentUserSession();
        return (Arrays.binarySearch(session.getRoles(), role) >= 0);
    }

    /**
     * Modifies the query depending on current user's security constraints
     * @param query query to modify
     * @param entityName name of entity which is quering
     */
    public static void applyConstraints(Query query, String entityName) {
        getInstance().__applyConstraints(query, entityName);
    }

    protected abstract UserSession __currentUserSession();

    protected void __applyConstraints(Query query, String entityName) {
        List<String[]> constraints = __currentUserSession().getConstraints(entityName);
        if (constraints.isEmpty())
            return;

        QueryTransformer transformer = QueryTransformerFactory.createTransformer(
                query.getQueryString(), entityName);

        for (String[] constraint : constraints) {
            String join = constraint[0];
            String where = constraint[1];
            if (StringUtils.isBlank(join))
                transformer.addWhere(where);
            else
                transformer.addJoinAndWhere(join, where);
        }
        query.setQueryString(transformer.getResult());
        for (String paramName : transformer.getAddedParams()) {
            setQueryParam(query, paramName);
        }
    }

    protected void setQueryParam(Query query, String paramName) {
        if (paramName.startsWith(CONSTRAINT_PARAM_SESSION_ATTR)) {
            UserSession userSession = __currentUserSession();

            String attrName = paramName.substring(CONSTRAINT_PARAM_SESSION_ATTR.length());
            if (CONSTRAINT_PARAM_USER_LOGIN.equals(attrName)) {
                query.setParameter(paramName, userSession.getUser().getLogin());
            } else if (CONSTRAINT_PARAM_USER_ID.equals(attrName)) {
                UUID userId = userSession.getSubstitutedUser() != null ? 
                        userSession.getSubstitutedUser().getId() :
                        userSession.getUser().getId();
                query.setParameter(paramName, userId);
            } else if (CONSTRAINT_PARAM_USER_GROUP_ID.equals(attrName)) {
                Object groupId = userSession.getSubstitutedUser() == null ?
                        userSession.getUser().getGroup().getId() :
                        userSession.getSubstitutedUser().getGroup().getId();
                query.setParameter(paramName, groupId);
            } else {
                Serializable value = userSession.getAttribute(attrName);
                query.setParameter(paramName, value);
            }
        }
    }
}
