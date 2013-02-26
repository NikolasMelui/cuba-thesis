/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.web.App;
import com.vaadin.terminal.Terminal;
import com.vaadin.ui.Window;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles database unique constraint violations. Determines the exception type by searching a special marker string
 * in the messages of all exceptions in the chain.
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class UniqueConstraintViolationHandler implements ExceptionHandler {

    private Pattern pattern;

    private Messages messages = AppBeans.get(Messages.class);
    private DataService dataService = AppBeans.get(DataService.class);

    private Pattern getPattern() {
        if (pattern == null) {
            String s = dataService.getDbDialect().getUniqueConstraintViolationPattern();
            pattern = Pattern.compile(s);
        }
        return pattern;
    }

    @Override
    public boolean handle(Terminal.ErrorEvent event, App app) {
        Throwable t = event.getThrowable();
        try {
            while (t != null) {
                if (t.toString().contains("org.apache.openjpa.persistence.EntityExistsException")) {
                    doHandle(t, app);
                    return true;
                }
                t = t.getCause();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void doHandle(Throwable throwable, App app) {
        String constraintName = "";
        Matcher matcher = getPattern().matcher(throwable.toString());
        if (matcher.find()) {
            if (matcher.groupCount() > 1)
                constraintName = matcher.group(2);
            else
                constraintName = matcher.group(1);
        }

        String msg = "";
        if (StringUtils.isNotBlank(constraintName)) {
            msg = messages.getMainMessage(constraintName.toUpperCase());
        }

        if (msg.equalsIgnoreCase(constraintName)) {
            msg = messages.getMainMessage("uniqueConstraintViolation.message");
            if (StringUtils.isNotBlank(constraintName))
                msg = msg + " (" + constraintName + ")";
        }

        app.getAppWindow().showNotification(msg, Window.Notification.TYPE_ERROR_MESSAGE);
    }
}
