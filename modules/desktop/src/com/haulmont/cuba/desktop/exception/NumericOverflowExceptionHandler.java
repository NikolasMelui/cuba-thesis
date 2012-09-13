/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.exception;

import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.gui.components.IFrame;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.jdbc.ReportingSQLException;

import javax.annotation.Nullable;

/**
 * Handles database "numeric overflow" exception.
 *
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class NumericOverflowExceptionHandler extends AbstractExceptionHandler {

    public NumericOverflowExceptionHandler() {
        super(ReportingSQLException.class.getName());
    }

    @Override
    protected void doHandle(Thread thread, String className, String message, @Nullable Throwable throwable) {
        if (StringUtils.containsIgnoreCase(message, "Numeric field overflow")) {
            String msg = MessageProvider.getMessage(getClass(), "numericFieldOverflow.message");
            App.getInstance().getMainFrame().showNotification(msg, IFrame.NotificationType.ERROR);
        }
    }
}
