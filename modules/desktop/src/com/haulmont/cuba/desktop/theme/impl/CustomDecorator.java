/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.theme.impl;

import com.haulmont.cuba.core.global.ScriptingProvider;
import com.haulmont.cuba.desktop.theme.ComponentDecorator;

import java.util.Set;

/**
 * <p>$Id$</p>
 *
 * @author Alexander Budarov
 */
public class CustomDecorator implements ComponentDecorator {

    private String className;

    public CustomDecorator(String className) {
        this.className = className;
    }

    @Override
    public void decorate(Object component, Set<String> state) {
        Class decoratorClass = ScriptingProvider.loadClass(className);
        try {
            ComponentDecorator delegate = (ComponentDecorator) decoratorClass.newInstance();
            delegate.decorate(component, state);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
