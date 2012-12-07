/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.security.sys;

import com.haulmont.cuba.security.app.Authentication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author krivopustov
 * @version $Id$
 */
public class AuthenticationInterceptor {

    private Log log = LogFactory.getLog(getClass());

    private Authentication authentication;

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    private Object aroundInvoke(ProceedingJoinPoint ctx) throws Throwable {
        if (log.isTraceEnabled())
            log.trace("Authenticating: " + ctx.getSignature());

        try {
            authentication.begin();
            Object res = ctx.proceed();
            return res;
        } finally {
            authentication.end();
        }
    }
}
