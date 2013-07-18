/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.core.entity.Entity;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.Collection;

/**
 * @author abramov
 * @version $Id$
 */
public interface ListComponent extends Component, Component.BelongToFrame, Component.ActionsHolder {
    boolean isMultiSelect();
    void setMultiSelect(boolean multiselect);

    @Nullable
    <T extends Entity> T getSingleSelected();
    <T extends Entity> Set<T> getSelected();

    void setSelected(@Nullable Entity item);
    void setSelected(Collection<Entity> items);

    <T extends CollectionDatasource> T getDatasource();

    void refresh();
}