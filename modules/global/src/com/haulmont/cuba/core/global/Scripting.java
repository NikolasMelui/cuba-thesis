/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.global;

import groovy.lang.Binding;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;

/**
 * Central interface to provide scripting functionality.
 *
 * <p>Scripting includes ability to run dynamically compiled Java and Groovy classes, as well as loading resources
 * from classpath and conf directory.</p>
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public interface Scripting {

    String NAME = "cuba_Scripting";

    /**
     * Evaluates Groovy expression.
     * @param text      expression text
     * @param binding   Groovy binding
     * @param <T>       result type
     * @return          result of expression
     */
    <T> T evaluateGroovy(String text, Binding binding);

    /**
     * Evaluates Groovy expression.
     * @param text      expression text
     * @param context   map of parameters to pass to the expression, same as Binding
     * @param <T>       result type
     * @return          result of expression
     */
    <T> T evaluateGroovy(String text, Map<String, Object> context);

    /**
     * Runs Groovy script.
     * The script must be located as file under <em>conf</em> dir (see {@link com.haulmont.cuba.core.global.GlobalConfig#getConfDir()}).
     * @param name      path to the script relative to <em>conf</em> dir
     * @param binding   Groovy binding
     * @param <T>       result type
     * @return          result of the script execution
     */
    <T> T runGroovyScript(String name, Binding binding);

    /**
     * Runs Groovy script.
     * The script must be located as file under <strong>conf</strong> dir (see {@link com.haulmont.cuba.core.global.GlobalConfig#getConfDir()}).
     * @param name      path to the script relative to <em>conf</em> dir
     * @param context   map of parameters to pass to the script, same as Binding
     * @param <T>       result type
     * @return          result of the script execution
     */
    <T> T runGroovyScript(String name, Map<String, Object> context);

    /**
     * Returns the dynamic classloader.
     * <p>Actually it is the GroovyClassLoader which parent is {@link com.haulmont.cuba.core.sys.javacl.JavaClassLoader}.
     * For explanation on class loading sequence see {@link #loadClass(String)}
     * </p>
     * @return dynamic classloader
     */
    ClassLoader getClassLoader();

    /**
     * Loads class by name using the following sequence:
     * <ul>
     *     <li>Search for a Groovy source in the <em>conf</em> directory. If found, compile it and return</li>
     *     <li>Search for a Java source in the <em>conf</em> directory. If found, compile it and return</li>
     *     <li>Search for a class in classpath</li>
     * </ul>
     * It is possible to change sources in <em>conf</em> directory at run time, affecting the returning class,
     * with the following restrictions:
     * <ul>
     *     <li>You can not change source from Groovy to Java</li>
     *     <li>If you had Groovy source and than removed it, you'll still get the class compiled from those sources
     *     and not from classpath</li>
     * </ul>
     * You can bypass these restrictions if you invoke {@link #clearCache()} method, e.g. through JMX interface
     * CachingFacadeMBean.
     * @param name  fully qualified class name
     * @return      class or null if not found
     */
    @Nullable
    Class loadClass(String name);

    /**
     * DEPRECATED - use {@link Resources#getResourceAsStream(String)}
     */
    @Deprecated
    @Nullable
    InputStream getResourceAsStream(String name);

    /**
     * DEPRECATED - use {@link Resources#getResourceAsString(String)}
     */
    @Deprecated
    @Nullable
    String getResourceAsString(String name);

    /**
     * Clears compiled classes cache
     */
    void clearCache();
}
