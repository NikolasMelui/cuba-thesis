/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.remoting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class LocalServiceDirectory {

    private static Map<String, LocalServiceInvoker> invokers = new ConcurrentHashMap<String, LocalServiceInvoker>();

    private static Log log = LogFactory.getLog(LocalServiceDirectory.class);

    public static void registerInvoker(String name, LocalServiceInvoker invoker) {
        log.debug("Registering service " + name);
        invokers.put(name, invoker);
    }

    public static void unregisterInvoker(String name) {
        log.debug("Unregistering service " + name);
        invokers.remove(name);
    }

    public static LocalServiceInvoker getInvoker(String name) {
        return invokers.get(name);
    }
}
