/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.annotation.ExtendedBy;
import com.haulmont.cuba.core.entity.annotation.Extends;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Encapsulates functionality for working with extended entities.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("cuba_ExtendedEntities")
public class ExtendedEntities {

    @Inject
    private Metadata metadata;

    /**
     * Default constructor used by container at runtime and in server-side integration tests.
     */
    public ExtendedEntities() {
    }

    /**
     * Constructor used in client-side tests.
     */
    public ExtendedEntities(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Searches for an extended entity and returns it if found, otherwise returns the original entity.
     * @param originalMetaClass original entity
     * @return                  extended or original entity
     */
    public Class getEffectiveClass(MetaClass originalMetaClass) {
        Class extClass = getExtendedClass(originalMetaClass);
        return extClass == null ? originalMetaClass.getJavaClass() : extClass;
    }

    /**
     * Searches for an extended entity and returns it if found, otherwise returns the original entity.
     * @param originalClass original entity
     * @return              extended or original entity
     */
    public Class getEffectiveClass(Class originalClass) {
        return getEffectiveClass(metadata.getSession().getClassNN(originalClass));
    }

    /**
     * Searches for an extended entity and returns it if found, otherwise returns the original entity.
     * @param entityName    original entity
     * @return              extended or original entity
     */
    public Class getEffectiveClass(String entityName) {
        return getEffectiveClass(metadata.getSession().getClassNN(entityName));
    }

    /**
     * Searches for an extended entity and returns it if found, otherwise returns the original entity.
     * @param originalMetaClass original entity
     * @return                  extended or original entity
     */
    public MetaClass getEffectiveMetaClass(MetaClass originalMetaClass) {
        return metadata.getSession().getClassNN(getEffectiveClass(originalMetaClass));
    }

    /**
     * Searches for an extended entity and returns it if found, otherwise returns the original entity.
     * @param originalClass original entity
     * @return              extended or original entity
     */
    public MetaClass getEffectiveMetaClass(Class originalClass) {
        return metadata.getSession().getClassNN(getEffectiveClass(originalClass));
    }

    /**
     * Searches for an extended entity and returns it if found.
     * @param originalMetaClass original entity
     * @return                  extended entity or null if the provided entity has no extension
     */
    @Nullable
    public Class getExtendedClass(MetaClass originalMetaClass) {
        return (Class) originalMetaClass.getAnnotations().get(ExtendedBy.class.getName());
    }

    /**
     * Searches for an original entity for the provided extended entity.
     * @param extendedMetaClass extended entity
     * @return                  original entity or null if the provided entity is not an extension
     */
    @Nullable
    public Class getOriginalClass(MetaClass extendedMetaClass) {
        return (Class) extendedMetaClass.getAnnotations().get(Extends.class.getName());
    }

    /**
     * Searches for an original entity for the provided extended entity.
     * @param extendedMetaClass extended entity
     * @return                  original entity or null if the provided entity is not an extension
     */
    @Nullable
    public MetaClass getOriginalMetaClass(MetaClass extendedMetaClass) {
        Class originalClass = getOriginalClass(extendedMetaClass);
        return originalClass == null ? null : metadata.getSession().getClassNN(originalClass);
    }

    /**
     * Searches for an original entity for the provided extended entity.
     * @param extendedEntityName extended entity
     * @return                   original entity or null if the provided entity is not an extension
     */
    @Nullable
    public MetaClass getOriginalMetaClass(String extendedEntityName) {
        return getOriginalMetaClass(metadata.getSession().getClassNN(extendedEntityName));
    }
}
