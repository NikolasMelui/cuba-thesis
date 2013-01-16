/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.data.GroupDatasource;
import com.haulmont.cuba.gui.data.GroupInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author gorodnov
 * @version $Id$
 */
public class CustomGroupDatasource<T extends Entity<K>, K> 
        extends CustomCollectionDatasource<T, K>
        implements GroupDatasource<T, K> {

    protected GroupDelegate<T,K> groupDelegate = new GroupDelegate<T, K>(this) {
        protected void doSort(SortInfo<MetaPropertyPath>[] sortInfo) {
            CustomGroupDatasource.super.doSort();
        }
    };

    public void groupBy(Object[] properties) {
        groupDelegate.groupBy(properties, sortInfos);
    }

    @Override
    protected void doSort() {
        if (hasGroups()) {
            groupDelegate.doGroupSort(sortInfos);
        } else {
            super.doSort();
        }
    }

    public List<GroupInfo> rootGroups() {
        return groupDelegate.rootGroups();
    }

    public boolean hasChildren(GroupInfo groupId) {
        return groupDelegate.hasChildren(groupId);
    }

    public List<GroupInfo> getChildren(GroupInfo groupId) {
        return groupDelegate.getChildren(groupId);
    }

    public Object getGroupProperty(GroupInfo groupId) {
        return groupDelegate.getGroupProperty(groupId);
    }

    public Object getGroupPropertyValue(GroupInfo groupId) {
        return groupDelegate.getGroupPropertyValue(groupId);
    }

    public Collection<K> getGroupItemIds(GroupInfo groupId) {
        return groupDelegate.getGroupItemIds(groupId);
    }

    public int getGroupItemsCount(GroupInfo groupId) {
        return groupDelegate.getGroupItemsCount(groupId);
    }

    public boolean hasGroups() {
        return groupDelegate.hasGroups();
    }

    public Collection<?> getGroupProperties() {
        return groupDelegate.getGroupProperties();
    }

    public boolean containsGroup(GroupInfo groupId) {
        return groupDelegate.containsGroup(groupId);
    }
}
