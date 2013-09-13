/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.security.app;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.remoting.RemoteClientInfo;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.NoUserSessionException;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.security.sys.UserSessionManager;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Class that encapsulates the middleware login/logout functionality.
 *
 * @author krivopustov
 * @version $Id$
 * @see com.haulmont.cuba.security.app.LoginServiceBean
 */
@ManagedBean(LoginWorker.NAME)
public class LoginWorkerBean implements LoginWorker {
    private Log log = LogFactory.getLog(LoginWorkerBean.class);

    @Inject
    private Persistence persistence;

    @Inject
    private Messages messages;

    private Configuration configuration;

    @Inject
    private PasswordEncryption passwordEncryption;

    @Inject
    private UserSessionManager userSessionManager;

    @Inject
    private UserSessionSource userSessionSource;

    private Pattern permittedIpMaskPattern;

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;

        String permittedIpMask = configuration.getConfig(ServerConfig.class).getTrustedClientPermittedIpMask();
        permittedIpMaskPattern = Pattern.compile(permittedIpMask);
    }

    @Nullable
    private User loadUser(String login) throws LoginException {
        if (login == null)
            throw new IllegalArgumentException("Login is null");

        EntityManager em = persistence.getEntityManager();
        String queryStr = "select u from sec$User u where u.loginLowerCase = ?1 and (u.active = true or u.active is null)";

        Query q = em.createQuery(queryStr);
        q.setParameter(1, login.toLowerCase());

        List list = q.getResultList();
        if (list.isEmpty()) {
            log.warn("Failed to authenticate: " + login);
            return null;
        } else {
            User user = (User) list.get(0);
            return user;
        }
    }

    @Override
    public UserSession login(String login, String password, Locale locale) throws LoginException {
        if (password == null)
            throw new LoginException(getInvalidCredentialsMessage(login, locale));

        Transaction tx = persistence.createTransaction();
        try {
            User user = loadUser(login);
            if (user == null)
                throw new LoginException(getInvalidCredentialsMessage(login, locale));

            if (!passwordEncryption.checkPassword(user, password))
                throw new LoginException(getInvalidCredentialsMessage(login, locale));

            if (user.getLanguage() != null &&
                    BooleanUtils.isFalse(configuration.getConfig(GlobalConfig.class).getLocaleSelectVisible())) {
                locale = new Locale(user.getLanguage());
            }

            UserSession session = userSessionManager.createSession(user, locale, false);
            if (user.getDefaultSubstitutedUser() != null) {
                session = userSessionManager.createSession(session, user.getDefaultSubstitutedUser());
            }
            log.info("Logged in: " + session);

            tx.commit();

            userSessionManager.storeSession(session);

            return session;
        } finally {
            tx.end();
        }
    }

    @Override
    public UserSession login(String login, String password, Locale locale, Map<String, Object> params) throws LoginException {
        return login(login, password, locale);
    }

    protected String getInvalidCredentialsMessage(String login, Locale locale) {
        return messages.formatMessage(getClass(), "LoginException.InvalidLoginOrPassword", locale, login);
    }

    @Override
    public UserSession loginSystem(String login) throws LoginException {
        Transaction tx = persistence.createTransaction();
        try {
            User user = loadUser(login);
            if (user == null)
                throw new LoginException(getInvalidCredentialsMessage(login, Locale.getDefault()));

            UserSession session = userSessionManager.createSession(user, Locale.getDefault(), true);
            if (user.getDefaultSubstitutedUser() != null) {
                session = userSessionManager.createSession(session, user.getDefaultSubstitutedUser());
            }
            log.info("Logged in: " + session);

            tx.commit();

            userSessionManager.storeSession(session);

            return session;
        } finally {
            tx.end();
        }
    }

    @Override
    public UserSession loginTrusted(String login, String password, Locale locale) throws LoginException {
        RemoteClientInfo remoteClientInfo = RemoteClientInfo.get();
        if (remoteClientInfo != null) {
            // reject request from not permitted client ip
            if (!permittedIpMaskPattern.matcher(remoteClientInfo.getAddress()).find()) {
                log.warn("Attempt of trusted login from not permitted IP address: " + remoteClientInfo.getAddress());
                throw new LoginException(getInvalidCredentialsMessage(login, locale));
            }
        }

        String trustedClientPassword = configuration.getConfig(ServerConfig.class).getTrustedClientPassword();
        if (StringUtils.isBlank(trustedClientPassword) || !trustedClientPassword.equals(password))
            throw new LoginException(getInvalidCredentialsMessage(login, locale));

        Transaction tx = persistence.createTransaction();
        try {
            User user = loadUser(login);

            if (user == null)
                throw new LoginException(
                        messages.formatMessage(getClass(), "LoginException.InvalidActiveDirectoryUser", locale, login));

            Locale userLocale = locale;
            if (!StringUtils.isBlank(user.getLanguage())) {
                userLocale = new Locale(user.getLanguage());
            }
            UserSession session = userSessionManager.createSession(user, userLocale, false);
            if (user.getDefaultSubstitutedUser() != null) {
                session = userSessionManager.createSession(session, user.getDefaultSubstitutedUser());
            }
            log.info("Logged in: " + session);

            tx.commit();

            userSessionManager.storeSession(session);

            return session;
        } finally {
            tx.end();
        }
    }

    @Override
    public UserSession loginTrusted(String login, String password, Locale locale, Map<String, Object> params) throws LoginException {
        return loginTrusted(login, password, locale);
    }

    @Override
    public void logout() {
        try {
            UserSession session = userSessionSource.getUserSession();
            userSessionManager.removeSession(session);
            log.info("Logged out: " + session);
        } catch (SecurityException e) {
            log.warn("Couldn't logout: " + e);
        } catch (NoUserSessionException e) {
            log.warn("NoUserSessionException thrown on logout");
        }
    }

    @Override
    public UserSession substituteUser(User substitutedUser) {
        UserSession currentSession = userSessionSource.getUserSession();

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            User user;
            if (currentSession.getUser().equals(substitutedUser)) {
                user = em.find(User.class, substitutedUser.getId());
                if (user == null)
                    throw new javax.persistence.NoResultException("User not found");
            } else {
                TypedQuery<User> query = em.createQuery(
                        "select s.substitutedUser from sec$User u join u.substitutions s " +
                        "where u.id = ?1 and s.substitutedUser.id = ?2",
                        User.class
                );
                query.setParameter(1, currentSession.getUser());
                query.setParameter(2, substitutedUser);
                List<User> list = query.getResultList();
                if (list.isEmpty())
                    throw new javax.persistence.NoResultException("User not found");
                else
                    user = list.get(0);
            }

            UserSession session = userSessionManager.createSession(currentSession, user);

            tx.commit();

            userSessionManager.removeSession(currentSession);
            userSessionManager.storeSession(session);

            return session;
        } finally {
            tx.end();
        }
    }

    @Override
    public UserSession getSession(UUID sessionId) {
        try {
            UserSession session = userSessionManager.getSession(sessionId);
            return session;
        } catch (RuntimeException e) {
            if (e instanceof NoUserSessionException)
                return null;
            else
                throw e;
        }
    }
}
