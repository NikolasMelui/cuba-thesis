/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.IFrame;
import com.vaadin.terminal.*;
import com.vaadin.terminal.gwt.server.ChangeVariablesErrorEvent;
import com.vaadin.ui.AbstractComponent;
import com.haulmont.cuba.web.App;
import com.vaadin.ui.Window;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.net.SocketException;

/**
 * This exception handler comes into play if no other handler in the chain has handled the exception.
 *
 * @author krivopustov
 * @version $Id$
 */
public class DefaultExceptionHandler implements ExceptionHandler {

    protected Messages messages = AppBeans.get(Messages.class);

    @Override
    public boolean handle(Terminal.ErrorEvent event, App app) {
        // Copied from com.vaadin.Application.terminalError()

        Throwable t = event.getThrowable();
        if (t instanceof SocketException || !App.isBound()) {
            // Most likely client browser closed socket
            return true;
        }

        if (t != null) {
            if (app.getConnection().getSession() != null) {
                showDialog(t);
            } else {
                showNotification(app, t);
            }
        } else {
            // Finds the original source of the error/exception
            Object owner = null;
            if (event instanceof VariableOwner.ErrorEvent) {
                owner = ((VariableOwner.ErrorEvent) event).getVariableOwner();
            } else if (event instanceof URIHandler.ErrorEvent) {
                owner = ((URIHandler.ErrorEvent) event).getURIHandler();
            } else if (event instanceof ParameterHandler.ErrorEvent) {
                owner = ((ParameterHandler.ErrorEvent) event).getParameterHandler();
            } else if (event instanceof ChangeVariablesErrorEvent) {
                owner = ((ChangeVariablesErrorEvent) event).getComponent();
            }

            // Shows the error in AbstractComponent
            if (owner instanceof AbstractComponent) {
                final Throwable e = event.getThrowable();
                if (e instanceof ErrorMessage) {
                    ((AbstractComponent) owner).setComponentError((ErrorMessage) e);
                } else {
                    ((AbstractComponent) owner)
                            .setComponentError(new SystemError(e));
                }
            }
        }
        return true;
    }

    protected void showDialog(Throwable exception) {
        Throwable rootCause = ExceptionUtils.getRootCause(exception);
        if (rootCause == null) {
            rootCause = exception;
        }
        ExceptionDialog dialog = new ExceptionDialog(rootCause);
        for (Window window : App.getInstance().getWindows()) {
            if (window.isModal()) {
                dialog.setModal(true);
                break;
            }
        }
        App.getInstance().getAppWindow().addWindow(dialog);
        dialog.focus();
    }

    protected void showNotification(App app, Throwable exception) {
        Throwable rootCause = ExceptionUtils.getRootCause(exception);
        if (rootCause == null) {
            rootCause = exception;
        }

        App.getInstance().getWindowManager().showNotification(
                messages.getMessage(DefaultExceptionHandler.class, "exceptionDialog.caption", app.getLocale()),
                rootCause.getClass().getSimpleName() + (rootCause.getMessage() != null ? "\n" + rootCause.getMessage() : ""),
                IFrame.NotificationType.ERROR
        );
    }
}