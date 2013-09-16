/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components;

/**
 * @author krivopustov
 * @version $Id$
 */
public class RequiredValueMissingException extends ValidationException {

    private Component component;

    public RequiredValueMissingException() {
    }

    public RequiredValueMissingException(String message) {
        super(message);
    }

    public RequiredValueMissingException(String message, Component component) {
        super(message);
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}