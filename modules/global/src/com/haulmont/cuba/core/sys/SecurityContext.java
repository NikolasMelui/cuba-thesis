/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.security.global.UserSession;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Holds information about the current user session.
 *
 * <p/>Instances of this class are normally set in {@link AppContext} by the framework, but also have to be
 * passed to it in case of manually running new threads. Here is the sample code for an asynchronous execution:
 * <pre>
 *     final SecurityContext securityContext = AppContext.getSecurityContext();
 *     executor.submit(new Runnable() {
 *         public void run() {
 *             AppContext.setSecurityContext(securityContext);
 *             // business logic here
 *         }
 *     });
 * </pre>
 *
 * @author krivopustov
 * @version $Id$
 */
public class SecurityContext {

    private final UUID sessionId;
    private UserSession session;
    private String user;

    public SecurityContext(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId is null");
        this.sessionId = sessionId;
    }

    public SecurityContext(UUID sessionId, String user) {
        this(sessionId);
        this.user = user;
    }

    public SecurityContext(UserSession session) {
        Objects.requireNonNull(session, "session is null");
        this.session = session;
        this.sessionId = session.getId();
        this.user = session.getUser().getLogin();
    }

    /**
     * @return Current {@link UserSession} ID. This is the only required value for the {@link SecurityContext}.
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * @return current user session. Can be null, so don't rely on this method in application code - use
     * {@link com.haulmont.cuba.core.global.UserSessionSource}
     */
    @Nullable
    public UserSession getSession() {
        return session;
    }

    /**
     * @return current user login. Can be null, so don't rely on this method in application code - use
     * {@link com.haulmont.cuba.core.global.UserSessionSource}
     */
    @Nullable
    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "SecurityContext{" +
                "sessionId=" + sessionId +
                '}';
    }
}
