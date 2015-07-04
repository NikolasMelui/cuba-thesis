/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.exception;

import com.haulmont.cuba.core.global.ExceptionHandlersConfig;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.IFrame;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.Ordered;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles database unique constraint violations. Determines the exception type by searching a special marker string
 * in the messages of all exceptions in the chain.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("cuba_UniqueConstraintViolationHandler")
public class UniqueConstraintViolationHandler implements GenericExceptionHandler, Ordered {

    @Inject
    protected Messages messages;

    @Inject
    protected ExceptionHandlersConfig exceptionHandlersConfig;

    @Override
    public boolean handle(Throwable exception, WindowManager windowManager) {
        Throwable t = exception;
        try {
            while (t != null) {
                if (t.toString().contains("org.springframework.dao.DataIntegrityViolationException")
                        || t.toString().contains("org.springframework.orm.jpa.JpaSystemException")) {
                    return doHandle(t, windowManager);
                }
                t = t.getCause();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean doHandle(Throwable throwable, WindowManager windowManager) {
        Pattern pattern = exceptionHandlersConfig.getUniqueConstraintViolationPattern();
        String constraintName = "";

        Matcher matcher = pattern.matcher(throwable.toString());
        if (matcher.find()) {
            if (matcher.groupCount() > 1) {
                constraintName = matcher.group(2);
            } else if (matcher.groupCount() == 1) {
                constraintName = matcher.group(1);
            }

            String msg = "";
            if (StringUtils.isNotBlank(constraintName)) {
                msg = messages.getMainMessage(constraintName.toUpperCase());
            }

            if (msg.equalsIgnoreCase(constraintName)) {
                msg = messages.getMainMessage("uniqueConstraintViolation.message");
                if (StringUtils.isNotBlank(constraintName)) {
                    msg = msg + " (" + constraintName + ")";
                }
            }

            windowManager.showNotification(msg, IFrame.NotificationType.ERROR);
            return true;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PLATFORM_PRECEDENCE + 60;
    }
}
