/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web;

import com.haulmont.cuba.web.auth.RequestContext;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gorodnov
 * @version $Id$
 */
public class AppCookies implements Serializable {

    private static final long serialVersionUID = 5958656635050664762L;

    public static final int COOKIE_MAX_AGE = 31536000; //1 year (in seconds)

    private transient Map<String, Cookie> requestedCookies;

    private String cookiePath = "/";
    private boolean cookiesEnabled = true;

    private long lastRequestTimestamp = 0L;

    public AppCookies() {
        requestedCookies = new HashMap<>();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        requestedCookies = new HashMap<>();
    }

    public String getCookieValue(String name) {
        Cookie cookie = getCookieIfEnabled(name);
        return cookie == null ? null : cookie.getValue();
    }

    public int getCookieMaxAge(String name) {
        Cookie cookie = getCookieIfEnabled(name);
        return cookie == null ? 0 : cookie.getMaxAge();
    }

    public void addCookie(String name, String value) {
        addCookie(name, value, COOKIE_MAX_AGE);
    }

    public void addCookie(String name, String value, int maxAge) {
        if (isCookiesEnabled()) {
            if (StringUtils.isEmpty(value)) {
                removeCookie(name);
            } else {
                Cookie cookie = new Cookie(name, value);
                cookie.setPath(getCookiePath());
                cookie.setMaxAge(maxAge);
                addCookie(cookie);
            }
        }
    }

    public void removeCookie(String name) {
        if (isCookiesEnabled()) {
            Cookie cookie = getCookie(name);
            if (cookie != null) {
                cookie.setValue(null);
                cookie.setPath(getCookiePath());
                cookie.setMaxAge(0);
                addCookie(cookie);
            }
        }
    }

    protected Cookie getCookieIfEnabled(String name) {
        return isCookiesEnabled() ? getCookie(name) : null;
    }

    protected Cookie getCookie(String name) {
        if (RequestContext.get().getRequestTimestamp() != lastRequestTimestamp)
            updateCookies();
        return requestedCookies.get(name);
    }

    protected void addCookie(Cookie cookie) {
        RequestContext.get().getResponse().addCookie(cookie);
    }

    public void updateCookies() {
        if (isCookiesEnabled()) {
            requestedCookies.clear();
            Cookie[] cookies = RequestContext.get().getRequest().getCookies();
            if (cookies != null) {
                for (final Cookie cookie : cookies) {
                    requestedCookies.put(cookie.getName(), cookie);
                }
            }
            lastRequestTimestamp = RequestContext.get().getRequestTimestamp();
        }
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public boolean isCookiesEnabled() {
        return cookiesEnabled;
    }

    public void setCookiesEnabled(boolean cookiesEnabled) {
        this.cookiesEnabled = cookiesEnabled;
    }
}
