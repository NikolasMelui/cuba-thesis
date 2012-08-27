/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.chile.core.model;

import com.haulmont.chile.core.common.ValueListener;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Interface to be implemented by data model objects.
 *
 * @author abramov
 * @version $Id$
 */
public interface Instance {

    /**
     * @return unique identifier of this instance
     */
    UUID getUuid();

    /**
     * @return metaclass of this object, if any
     */
    @Nullable
    MetaClass getMetaClass();
    
    /**
     * @return Instance name as defined by {@link com.haulmont.chile.core.annotations.NamePattern}
     * or <code>toString()</code>.
     */
    String getInstanceName();

    /**
     * Get an attribute value.
     * @param name  attribute name according to JavaBeans notation
     * @return      attribute value
     */
    @Nullable
    <T> T getValue(String name);

    /**
     * Set an attribute value.
     * <p/> An implementor should first read a current value of the attribute, and then call an appropriate setter
     * method only if the new value differs. This ensures triggering of {@link ValueListener}s only if the attribute
     * was actually changed.
     * @param name  attribute name according to JavaBeans notation
     * @param value attribute value
     */
    void setValue(String name, Object value);

    /**
     * Get an attribute value. Locates the atribute by the given path in object graph starting from this instance.
     * <p/> The path must consist of attribute names according to JavaBeans notation, separated by dots, e.g.
     * <code>car.driver.name</code>.
     * @param propertyPath  path to an attibute
     * @return attribute value. If any traversing attribute value is null or is not an {@link Instance}, this method
     * stops here and returns this value.
     */
    @Nullable
    <T> T getValueEx(String propertyPath);

    /**
     * Set an attribute value. Locates the atribute by the given path in object graph starting from this instance.
     * <p/> The path must consist of attribute names according to JavaBeans notation, separated by dots, e.g.
     * <code>car.driver.name</code>.
     * <p/> In the example above this method first gets value of <code>car.driver</code> attribute, and if it is not
     * null and is an {@link Instance}, sets value of <code>name</code> attribute in it. If the value returned from
     * <code>getValueEx("car.driver")</code> is null or is not an {@link Instance}, this method throws
     * {@link IllegalStateException}.
     * <p/> An implementor should first read a current value of the attribute, and then call an appropriate setter
     * method only if the new value differs. This ensures triggering of {@link ValueListener}s only if the attribute
     * was actually changed.
     * @param propertyPath  path to an attibute
     * @param value         attribute value
     */
    void setValueEx(String propertyPath, Object value);

    /**
     * Add listener to track attributes changes.
     * @param listener  listener
     */
    void addListener(ValueListener listener);

    /**
     * Remove listener.
     * @param listener listener to remove
     */
    void removeListener(ValueListener listener);

    /**
     * Remove all {@link ValueListener}s.
     */
    void removeAllListeners();
}
