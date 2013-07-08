/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.web.App;

import javax.annotation.Nullable;

/**
 * @author krivopustov
 * @version $Id$
 */
public class FileStorageExceptionHandler extends AbstractExceptionHandler {

    public FileStorageExceptionHandler() {
        super(FileStorageException.class.getName());
    }

    @Override
    protected void doHandle(App app, String className, String message, @Nullable Throwable throwable) {
        String msg = null;
        Messages messages = AppBeans.get(Messages.class);
        if (throwable != null) {
            FileStorageException storageException = (FileStorageException) throwable;
            String fileName = storageException.getFileName();
            if (storageException.getType().equals(FileStorageException.Type.FILE_NOT_FOUND))
                msg = messages.formatMessage(getClass(), "fileNotFound.message", fileName);
            else if (storageException.getType().equals(FileStorageException.Type.STORAGE_INACCESSIBLE))
                msg = messages.getMessage(getClass(), "fileStorageInaccessible.message");
        }
        if (msg == null) {
            msg = messages.getMessage(getClass(), "fileStorageException.message");
        }
        app.getWindowManager().showNotification(msg, IFrame.NotificationType.ERROR);
    }
}