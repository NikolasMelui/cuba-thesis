/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.web.App;
import com.vaadin.server.ErrorEvent;

/**
 * Interface to be implemented by exception handlers in Web-client.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface ExceptionHandler {
    /**
     * Handle an exception. Implementation class should either handle the exception and return true, or return false
     * to delegate execution to the next handler in the chain of responsibility.
     * @param event error event containing the exception, generated by Vaadin
     * @param app   current {@link App} instance
     * @return      true if the exception has been succesfully handled, false if not
     */
    boolean handle(ErrorEvent event, App app);
}
