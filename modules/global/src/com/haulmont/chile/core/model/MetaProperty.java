/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.chile.core.model;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;

/**
 * Metadata object representing an entity attribute.
 *
 * @author abramov
 * @version $Id$
 */
public interface MetaProperty extends MetadataObject<MetaProperty> {

    /**
     * Property type
     */
    enum Type {

        /** Simple value type, e.g. String, Number */
        DATATYPE,

        /** Enumeration */
        ENUM,

        /** Reference type. Attribute of this type contains a related entity. */
        ASSOCIATION,

        /**
         * Reference type. Attribute of this type contains a related entity.
         * Composition implies ownership, that is the referenced object exists only as part of the owning entity.
         */
        COMPOSITION
    }

    /**
     * @return containing MetaModel instance
     */
    MetaModel getModel();

    /**
     * @return MetaClass, containing this MetaProperty
     */
    MetaClass getDomain();

    /**
     * @return Range of this property.
     */
    Range getRange();

    /**
     * @return property type
     */
    Type getType();

    /**
     * @return true if the corresponding entity attribute must contain a value
     */
    boolean isMandatory();

    /**
     * @return true if the corresponding entity attribute is read-only
     */
    boolean isReadOnly();

    /**
     * @return a MetaProperty from the opposite side of relation, or null if this is not a reference property
     */
    MetaProperty getInverse();

    /**
     * @return corresponding Java field or method as AnnotatedElement object
     */
    AnnotatedElement getAnnotatedElement();

    /**
     * @return Java class of the corresponding field or method's return type
     */
    Class<?> getJavaType();

    /**
     * @return Java class which declares the corresponding field or method
     */
    @Nullable
    Class<?> getDeclaringClass();
}
