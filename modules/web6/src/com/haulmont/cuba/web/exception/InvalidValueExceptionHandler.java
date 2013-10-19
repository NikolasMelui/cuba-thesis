/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.web.App;
import com.vaadin.data.Validator;
import com.vaadin.ui.Window;

import javax.annotation.Nullable;

/**
 * @author krivopustov
 * @version $Id$
 */
public class InvalidValueExceptionHandler extends AbstractExceptionHandler {

    public InvalidValueExceptionHandler() {
        super(Validator.InvalidValueException.class.getName());
    }

    @Override
    protected void doHandle(App app, String className, String message, @Nullable Throwable throwable) {
        app.getAppWindow().showNotification(
                MessageProvider.getMessage(getClass(), "validationFail.caption"),
                MessageProvider.getMessage(getClass(), "validationFail"),
                Window.Notification.TYPE_TRAY_NOTIFICATION
        );
    }
}
