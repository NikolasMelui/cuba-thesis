/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.EmbeddableEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import com.haulmont.cuba.gui.data.*;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;

/**
 * @author artamonov
 * @version $Id$
 */
public class EmbeddedDatasourceImpl<T extends EmbeddableEntity>
        extends AbstractDatasource<T>
        implements Datasource<T>, DatasourceImplementation<T>, EmbeddedDatasource<T> {

    protected Datasource masterDs;
    protected MetaProperty metaProperty;

    @Override
    public void setup(String id, Datasource masterDs, String property) {
        this.id = id;
        this.masterDs = masterDs;
        metaProperty = masterDs.getMetaClass().getProperty(property);
        initParentDsListeners();
    }

    protected void initParentDsListeners() {
        masterDs.addListener(new DatasourceListener<Entity>() {
            @Override
            public void itemChanged(Datasource ds, Entity prevItem, Entity item) {
                Entity prevValue = getItem(prevItem);
                Entity newValue = getItem(item);
                reattachListeners(prevValue, newValue);
                fireItemChanged(prevValue);
            }

            @Override
            public void stateChanged(Datasource ds, State prevState, State state) {
                for (DatasourceListener dsListener : new ArrayList<>(dsListeners)) {
                    dsListener.stateChanged(EmbeddedDatasourceImpl.this, prevState, state);
                }
            }

            @Override
            public void valueChanged(Entity source, String property, Object prevValue, Object value) {
                if (property.equals(metaProperty.getName()) && !ObjectUtils.equals(prevValue, value)) {
                    reattachListeners((Entity) prevValue, (Entity) value);
                    fireItemChanged(prevValue);
                }
            }

            protected void reattachListeners(Entity prevItem, Entity item) {
                if (prevItem != item) {
                    detachListener(prevItem);
                    attachListener(item);
                }
            }
        });
    }

    @Override
    public DsContext getDsContext() {
        return masterDs.getDsContext();
    }

    @Override
    public DataSupplier getDataSupplier() {
        return masterDs.getDataSupplier();
    }

    @Override
    public void commit() {
        if (!allowCommit)
            return;

        clearCommitLists();
        modified = false;
    }

    @Override
    public State getState() {
        return masterDs.getState();
    }

    @Override
    public T getItem() {
        final Instance item = masterDs.getItem();
        return getItem(item);
    }

    protected T getItem(Instance item) {
        return item == null ? null : (T) item.getValue(metaProperty.getName());
    }

    @Override
    public void setItem(T item) {
        if (getItem() != null) {
            InstanceUtils.copy(item, getItem());
            itemToUpdate.add(item);
        } else {
            final Instance parentItem = masterDs.getItem();
            parentItem.setValue(metaProperty.getName(), item);
        }
        setModified(true);
        ((DatasourceImplementation) masterDs).modified(masterDs.getItem());
    }

    @Override
    public MetaClass getMetaClass() {
        MetaClass metaClass = metaProperty.getRange().asClass();
        return metadata.getExtendedEntities().getEffectiveMetaClass(metaClass);
    }

    @Override
    public View getView() {
        final ViewProperty property = masterDs.getView().getProperty(metaProperty.getName());
        return property == null ? null : metadata.getViewRepository().getView(getMetaClass(), property.getView().getName());
    }

    @Override
    public void committed(Set<Entity> entities) {
        Entity item = masterDs.getItem();

        Entity newItem = null;
        Entity previousItem = null;

        if (item != null) {
            Iterator<Entity> commitIter = entities.iterator();
            while (commitIter.hasNext() && (previousItem == null) && (newItem == null)) {
                Entity commitItem = commitIter.next();
                if (commitItem.equals(item)) {
                    previousItem = item;
                    newItem = commitItem;
                }
            }
            if (previousItem != null) {
                detachListener(getItem(previousItem));
            }
            if (newItem != null) {
                attachListener(getItem(newItem));
            }
        }

        modified = false;
        clearCommitLists();
    }

    @Override
    public Datasource getMaster() {
        return masterDs;
    }

    @Override
    public MetaProperty getProperty() {
        return metaProperty;
    }

    @Override
    public void invalidate() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void initialized() {
    }

    @Override
    public void valid() {
    }

    @Override
    public void modified(T item) {
        super.modified(item);
        ((DatasourceImplementation) masterDs).modified(masterDs.getItem());
    }

    @Override
    public Collection<T> getItemsToCreate() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<T> getItemsToUpdate() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<T> getItemsToDelete() {
        return Collections.EMPTY_LIST;
    }
}