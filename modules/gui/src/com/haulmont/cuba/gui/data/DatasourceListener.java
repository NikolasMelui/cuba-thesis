/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.data;

import com.haulmont.cuba.core.entity.Entity;

import javax.annotation.Nullable;

/**
 * Listener to basic datasource events
 *
 * @param <T> type of entity the datasource contains
 * @author abramov
 * @version $Id$
 */
public interface DatasourceListener<T extends Entity> extends ValueListener<T> {

    /**
     * Current item changed
     *
     * @param ds       datasource
     * @param prevItem previous selected item
     * @param item     current item
     */
    void itemChanged(Datasource<T> ds, @Nullable T prevItem, @Nullable T item);

    /**
     * Datasource state changed
     *
     * @param ds        datasource
     * @param prevState previous state
     * @param state     current state
     */
    void stateChanged(Datasource<T> ds, Datasource.State prevState, Datasource.State state);
}