/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.log;

import com.haulmont.cuba.core.global.Logging;
import com.haulmont.cuba.core.global.SilentException;
import com.vaadin.data.Validator;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorEvent;
import com.vaadin.ui.AbstractComponent;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public class AppLog {

    private transient LinkedList<LogItem> items = new LinkedList<>();

    private static final int CAPACITY = 100;

    private static Log log = LogFactory.getLog(AppLog.class);

    public void log(LogItem item) {
        String msg = item.getMessage() + "\n" + item.getStacktrace();
        if (item.getLevel().equals(LogLevel.ERROR))
            log.error(msg);
        else
            log.debug(item.getLevel() + ": " + msg);
        
        if (items.size() >= CAPACITY) {
            items.removeLast();
        }
        items.addFirst(item);
    }

    public void log(LogLevel level, String message, Throwable throwable) {
        log(new LogItem(level, message, throwable));
    }

    public void debug(String message) {
        log(new LogItem(LogLevel.DEBUG, message, null));
    }

    public void info(String message) {
        log(new LogItem(LogLevel.INFO, message, null));
    }

    public void warning(String message) {
        log(new LogItem(LogLevel.WARNING, message, null));
    }

    public void error(String message) {
        log(new LogItem(LogLevel.ERROR, message, null));
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void log(ErrorEvent event) {
        Throwable t = event.getThrowable();

        if (t instanceof SilentException)
            return;

        if (t instanceof Validator.InvalidValueException)
            return;

        if (t instanceof SocketException
                || ExceptionUtils.getRootCause(t) instanceof SocketException) {
            // Most likely client browser closed socket
            LogItem item = new LogItem(LogLevel.WARNING,
                    "SocketException in CommunicationManager. Most likely client (browser) closed socket.", null);
            log(item);
            return;
        }

        Throwable rootCause = ExceptionUtils.getRootCause(t);
        if (rootCause == null)
            rootCause = t;
        Logging annotation = rootCause.getClass().getAnnotation(Logging.class);
        Logging.Type loggingType = annotation == null ? Logging.Type.FULL : annotation.value();
        if (loggingType == Logging.Type.NONE)
            return;

        // Finds the original source of the error/exception
        AbstractComponent component = DefaultErrorHandler.findAbstractComponent(event);

        StringBuilder msg = new StringBuilder();
        msg.append("Exception");
        if (component != null)
            msg.append(" in ").append(component.getClass().getName());
        msg.append(": ");

        if (loggingType == Logging.Type.BRIEF) {
            error(msg + rootCause.toString());
        } else {
            LogItem item = new LogItem(LogLevel.ERROR, msg.toString(), t);
            log(item);
        }
    }

    public List<LogItem> getItems() {
        return new ArrayList<>(items);
    }
}