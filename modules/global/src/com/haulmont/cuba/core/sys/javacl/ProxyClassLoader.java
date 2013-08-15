/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: degtyarjov
 * Created: 19.03.13 18:20
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys.javacl;


import java.util.HashMap;
import java.util.Map;

public class ProxyClassLoader extends ClassLoader {
    Map<String, TimestampClass> compiled;
    ThreadLocal<Map<String, TimestampClass>> removedFromCompilation = new ThreadLocal<Map<String, TimestampClass>>();

    ProxyClassLoader(ClassLoader parent, Map<String, TimestampClass> compiled) {
        super(parent);
        this.compiled = compiled;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        TimestampClass tsClass = compiled.get(name);
        if (tsClass != null) {
            return tsClass.clazz;
        } else {
            return super.loadClass(name, resolve);
        }
    }

    public TimestampClass removeFromCache(String className) {
        Map<String, TimestampClass> removedFromCompilationMap = removedFromCompilation.get();
        if (removedFromCompilationMap == null) {
            removedFromCompilationMap = new HashMap<String, TimestampClass>();
            removedFromCompilation.set(removedFromCompilationMap);
        }

        TimestampClass timestampClass = compiled.get(className);
        if (timestampClass != null) {
            removedFromCompilationMap.put(className, timestampClass);
            compiled.remove(className);
            return timestampClass;
        }

        return null;
    }

    public void restoreRemoved() {
        Map<String, TimestampClass> map = removedFromCompilation.get();
        if (map != null) {
            compiled.putAll(map);
        }
        removedFromCompilation.remove();
    }

    public void cleanupRemoved() {
        removedFromCompilation.remove();
    }

    public boolean contains(String className) {
        return compiled.containsKey(className);
    }
}
