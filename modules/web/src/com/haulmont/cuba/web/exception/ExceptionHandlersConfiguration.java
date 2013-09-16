/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.exception;

import com.haulmont.bali.util.ReflectionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that is used to configure {@link ExceptionHandlers} via spring.xml.
 *
 * <p>If a project needs specific exception handlers, it should define a bean of this type with its own
 * <strong>id</strong>, e.g. <code>refapp_ExceptionHandlersConfiguration</code>, and set the list of handler class
 * names in <code>handlerClasses</code> property.</p>
 *
 * @author krivopustov
 * @version $Id$
 */
public class ExceptionHandlersConfiguration {

    private List<Class> handlerClasses = new ArrayList<Class>();

    /**
     * Set the list of exception handler class names, usually from spring.xml.
     * @param list  list of class names
     */
    public void setHandlerClasses(List<String> list) {
        for (String className : list) {
            handlerClasses.add(ReflectionHelper.getClass(className));
        }
    }

    /**
     * Get the list of exception handler class names.
     * @return  list of class names
     */
    public List<Class> getHandlerClasses() {
        return handlerClasses;
    }
}
