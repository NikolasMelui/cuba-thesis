/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.cuba.gui.data.CollectionDatasource;

/**
 * Base GUI class for grouping conditions (AND & OR) descriptors.
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class AbstractGroupConditionDescriptor<T extends AbstractParam> extends AbstractConditionDescriptor<T> {

    protected GroupType groupType;

    public AbstractGroupConditionDescriptor(GroupType groupType, String name, String filterComponentName, CollectionDatasource datasource) {
        super(name, filterComponentName, datasource);
        this.groupType = groupType;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    @Override
    public Class getJavaClass() {
        return null;
    }

    @Override
    public String getEntityParamWhere() {
        return null;
    }

    @Override
    public String getEntityParamView() {
        return null;
    }
}
