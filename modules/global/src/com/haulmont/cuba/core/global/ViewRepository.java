/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;

import javax.annotation.Nullable;

/**
 * Represents a repository of {@link View} objects, accessible by names.
 * <p/> Repository contains all views defined in XML and deployed at runtime.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface ViewRepository {

    String NAME = "cuba_ViewRepository";

    /**
     * Get View for an entity.
     *
     * @param entityClass   entity class
     * @param name          view name
     * @return              view instance. Throws {@link com.haulmont.cuba.core.global.ViewNotFoundException} if not found.
     */
    View getView(Class<? extends Entity> entityClass, String name);

    /**
     * Get View for an entity.
     *
     * @param metaClass     entity class
     * @param name          view name
     * @return              view instance. Throws {@link com.haulmont.cuba.core.global.ViewNotFoundException} if not found.
     */
    View getView(MetaClass metaClass, String name);

    /**
     * Searches for a View for an entity.
     *
     * @param metaClass     entity class
     * @param name          view name
     * @return              view instance or null if no view found
     */
    @Nullable
    View findView(MetaClass metaClass, String name);
}
