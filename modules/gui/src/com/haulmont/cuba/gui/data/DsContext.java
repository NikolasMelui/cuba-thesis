/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.data;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.gui.WindowContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface providing access to datasources defined in a screen.
 * <p/>
 * Implementation of this interface serves also for automatic coordination between datasources on load/commit time.
 *
 * @author Abramov
 * @version $Id$
 */
public interface DsContext {

    /**
     * @return context of a window that owns this DsContext
     */
    WindowContext getWindowContext();
    void setWindowContext(WindowContext context);

    /**
     * @return a reference to DataSupplier
     */
    DataSupplier getDataSupplier();

    /**
     * Get datasource by name.
     *
     * @param name  datasource name
     * @return      datasource instance or null if not found
     */
    @Nullable
    <T extends Datasource> T get(String name);

    /**
     * @return all datasources contained in this context
     */
    Collection<Datasource> getAll();

    /**
     * @return true if any contained datasource is modified
     */
    boolean isModified();

    /**
     * Refresh all datasources.
     */
    void refresh();

    /**
     * Commit all changed datasources.
     * @return true if there were changes and commit has been done
     */
    boolean commit();

    /**
     * Register dependency between datasources.
     * <br>Dependent datasource is refreshed if one of the following events occurs on master datasource:
     * <ul>
     * <li>itemChanged
     * <li>collectionChanged with Operation.REFRESH
     * <li>valueChanged with the specified property
     * </ul>
     */
    void registerDependency(Datasource ds, Datasource dependFrom, String property);

    void addListener(CommitListener listener);
    void removeListener(CommitListener listener);

    /**
     * @return a parent DsContext if this DsContext is defined in a frame
     */
    @Nullable
    DsContext getParent();

    /**
     * @return list of DsContext's of frames included in the current screen, if any
     */
    List<DsContext> getChildren();

    /**
     * This listener allows to intercept commit events.
     * <br>Can be used to augment CommitContext with entities which must be committed in the
     * same transaction as datasources content.
     */
    public interface CommitListener {
        /**
         * Called before sending data to the middleware.
         * @param context   commit context
         */
        void beforeCommit(CommitContext context);

        /**
         * Called after a succesfull commit to the middleware.
         * @param context   commit context
         * @param result    set of committed entities returning from the middleware service
         */
        void afterCommit(CommitContext context, Set<Entity> result);
    }
}

