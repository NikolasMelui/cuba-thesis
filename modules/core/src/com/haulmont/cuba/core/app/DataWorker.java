/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.NotDetachedCommitContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides standard CRUD functionality for non-managed entities.
 * Always starts and commits a new transaction.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface DataWorker {

    String NAME = "cuba_DataWorker";

    /**
     * Commit a collection of new or detached entity instances to the database.
     * @param context   {@link com.haulmont.cuba.core.global.CommitContext} object, containing committing entities and other information
     * @return          set of committed instances
     */
    Set<Entity> commit(CommitContext<Entity> context);

    /**
     * Commit a collection of entity instances to the database. This method is used for clients, not supporting
     * transfer of detached state, e.g. REST API. In this case new entity identificators passed explicitly in
     * {@link com.haulmont.cuba.core.global.NotDetachedCommitContext} to differentiate what entities have to be persisted and what have to be merged.
     *
     * @param context   {@link com.haulmont.cuba.core.global.NotDetachedCommitContext} object, containing committing entities and other information
     * @return          map of passed instances to committed instances
     */
    Map<Entity, Entity> commitNotDetached(NotDetachedCommitContext<Entity> context);

    /**
     * Load a single entity instance.
     * <p>The depth of object graphs, starting from loaded instances, defined by {@link com.haulmont.cuba.core.global.View}
     * object passed in {@link com.haulmont.cuba.core.global.LoadContext}.</p>
     * @param context   {@link com.haulmont.cuba.core.global.LoadContext} object, defining what and how to load
     * @return          the loaded detached object, or null if not found
     */
    @Nullable
    <A extends Entity> A load(LoadContext context);

    /**
     * Load collection of entity instances.
     * <p>The depth of object graphs, starting from loaded instances, defined by {@link com.haulmont.cuba.core.global.View}
     * object passed in {@link LoadContext}.</p>
     * @param context   {@link LoadContext} object, defining what and how to load
     * @return          a list of detached instances, or empty list if nothing found
     */
    @Nonnull
    <A extends Entity> List<A> loadList(LoadContext context);
}
