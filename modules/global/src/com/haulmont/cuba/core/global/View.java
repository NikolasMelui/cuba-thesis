/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.global;

import com.haulmont.cuba.core.entity.Entity;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to declare a graph of objects that must be retrieved from the database.
 * <p>
 * A view can be constructed in Java code or defined in XML and deployed
 * to the {@link com.haulmont.cuba.core.global.ViewRepository} for recurring usage.
 * </p>
 * There are the following predefined view types:
 * <ul>
 * <li>{@link #LOCAL}</li>
 * <li>{@link #MINIMAL}</li>
 * </ul>
 *
 * @author krivopustov
 * @version $Id$
 */
public class View implements Serializable {

    /**
     * Includes all local non-system properties.
     */
    public static final String LOCAL = "_local";

    /**
     * Includes only properties contained in {@link com.haulmont.chile.core.annotations.NamePattern}.
     */
    public static final String MINIMAL = "_minimal";

    private static final long serialVersionUID = 4313784222934349594L;

    private Class<? extends Entity> entityClass;

    private String name;

    private Map<String, ViewProperty> properties = new HashMap<>();

    private boolean includeSystemProperties;

    public View(Class<? extends Entity> entityClass) {
        this(entityClass, "", true);
    }

    public View(Class<? extends Entity> entityClass, boolean includeSystemProperties) {
        this(entityClass, "", includeSystemProperties);
    }

    public View(Class<? extends Entity> entityClass, String name) {
        this(entityClass, name, true);
    }

    public View(Class<? extends Entity> entityClass, String name, boolean includeSystemProperties) {
        this.entityClass = entityClass;
        this.name = name != null ? name : "";
        this.includeSystemProperties = includeSystemProperties;
    }

    public View(View src, String name, boolean includeSystemProperties) {
        this(src, null, name, includeSystemProperties);
    }

    public View(View src, @Nullable Class<? extends Entity> entityClass, String name, boolean includeSystemProperties) {
        this.entityClass = entityClass == null ? src.entityClass : entityClass;
        this.name = name != null ? name : "";
        this.includeSystemProperties = includeSystemProperties;
        this.properties.putAll(src.properties);
    }

    /**
     * @return entity class this view belongs to
     */
    public Class<? extends Entity> getEntityClass() {
        return entityClass;
    }

    /**
     * @return view name, unique within an entity
     */
    public String getName() {
        return name;
    }

    /**
     * @return collection of properties
     */
    public Collection<ViewProperty> getProperties() {
        return properties.values();
    }

    /**
     * @return whether this view includes system properties implicitly
     */
    public boolean isIncludeSystemProperties() {
        return includeSystemProperties;
    }

    /**
     * Add a property to this view.
     * @param name  property name
     * @param view  a view for a reference attribute, or null
     * @param lazy  see {@link com.haulmont.cuba.core.global.ViewProperty#isLazy()}
     * @return      this view instance for chaining
     */
    public View addProperty(String name, @Nullable View view, boolean lazy) {
        properties.put(name, new ViewProperty(name, view, lazy));
        return this;
    }

    /**
     * Add a property to this view.
     * @param name  property name
     * @param view  a view for a reference attribute, or null
     * @return      this view instance for chaining
     */
    public View addProperty(String name, View view) {
        properties.put(name, new ViewProperty(name, view));
        return this;
    }

    /**
     * Add a property to this view.
     * @param name  property name
     * @return      this view instance for chaining
     */
    public View addProperty(String name) {
        properties.put(name, new ViewProperty(name, null));
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        View view = (View) o;

        return entityClass.equals(view.entityClass) && name.equals(view.name);
    }

    @Override
    public int hashCode() {
        int result = entityClass.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return entityClass.getName() + "/" + name;
    }

    /**
     * Get directly owned view property by name.
     * @param name  property name
     * @return      view property instance or null if it is not found
     */
    @Nullable
    public ViewProperty getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Check if a directly owned property with the given name exists in the view.
     * @param name  property name
     * @return      true if such property found
     */
    public boolean containsProperty(String name) {
        return properties.containsKey(name);
    }

    /**
     * Determine if the whole view graph contains a lazy property.
     * @see com.haulmont.cuba.core.global.ViewProperty#isLazy()
     */
    public boolean hasLazyProperties() {
        for (ViewProperty property : getProperties()) {
            if (property.isLazy())
                return true;
            if (property.getView() != null && property.getView().hasLazyProperties())
                return true;
        }
        return false;
    }
}