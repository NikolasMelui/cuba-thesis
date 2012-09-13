/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.exception;

import com.haulmont.cuba.core.global.EntityAccessException;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.gui.components.IFrame;

import javax.annotation.Nullable;

/**
 * Handles {@link com.haulmont.cuba.core.global.EntityAccessException}.
 *
 * <p>$Id$</p>
 *
 * @author pavlov
 */
public class EntityAccessExceptionHandler extends AbstractExceptionHandler {

    public EntityAccessExceptionHandler() {
        super(EntityAccessException.class.getName());
    }

    @Override
    protected void doHandle(Thread thread, String className, String message, @Nullable Throwable throwable) {
        String msg = MessageProvider.formatMessage(getClass(), "entityAccessException.message");
        App.getInstance().getMainFrame().showNotification(msg, IFrame.NotificationType.WARNING);
    }
}
