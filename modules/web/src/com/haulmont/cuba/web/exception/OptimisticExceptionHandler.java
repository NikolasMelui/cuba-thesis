/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.web.App;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles a JPA optimistic lock exception.
 *
 * @author krivopustov
 * @version $Id$
 */
public class OptimisticExceptionHandler extends AbstractExceptionHandler {

    public OptimisticExceptionHandler() {
        super("org.springframework.orm.jpa.JpaOptimisticLockingFailureException");
    }

    @Override
    protected void doHandle(App app, String className, String message, @Nullable Throwable throwable) {
        Pattern pattern = Pattern.compile("\\[([^-]*)-");
        Matcher matcher = pattern.matcher(message);
        String entityClassName = "";
        if (matcher.find()) {
            entityClassName = matcher.group(1);
        }

        String localizedEntityName = "";
        String entityName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1);
        String packageName = entityClassName.substring(0, entityClassName.lastIndexOf("."));
        localizedEntityName = MessageProvider.getMessage(packageName, entityName);

        String msg = MessageProvider.formatMessage(getClass(), "optimisticException.message", "\"" + localizedEntityName + "\"");
        app.getAppUI().showNotification(msg, Notification.TYPE_ERROR_MESSAGE);
    }
}