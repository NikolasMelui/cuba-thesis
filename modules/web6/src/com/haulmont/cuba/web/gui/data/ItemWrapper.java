/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.data;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;

import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class ItemWrapper implements Item, Item.PropertySetChangeNotifier {

    private static final long serialVersionUID = -7298696379571470141L;

    private Map<MetaPropertyPath, PropertyWrapper> properties = new HashMap<>();
    private List<PropertySetChangeListener> listeners = new ArrayList<>();

    protected Object item;

    public ItemWrapper(Object item, MetaClass metaClass) {
        this(item, AppBeans.get(MetadataTools.class).getPropertyPaths(metaClass));
    }

    public ItemWrapper(Object item, Collection<MetaPropertyPath> properties) {
        this.item = item;

        for (MetaPropertyPath property : properties) {
            this.properties.put(property, createPropertyWrapper(item, property));
        }

        if (item instanceof CollectionDatasource) {
            ((CollectionDatasource) item).addListener(new CollectionDsListenerAdapter<Entity>() {
                @Override
                public void itemChanged(Datasource<Entity> ds, Entity prevItem, Entity item) {
                    fireItemPropertySetChanged();
                }
            });
        }
    }

    protected void fireItemPropertySetChanged() {
        for (PropertySetChangeListener listener : listeners) {
            listener.itemPropertySetChange(new PropertySetChangeEvent());
        }
    }

    protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
        return new PropertyWrapper(item, propertyPath);
    }

    @Override
    public Property getItemProperty(Object id) {
        if (id instanceof MetaPropertyPath) {
            return properties.get(id);
        } else if (id instanceof MetaProperty) {
            final MetaProperty metaProperty = (MetaProperty) id;
            return properties.get(new MetaPropertyPath(metaProperty.getDomain(), metaProperty));
        } else {
            throw new UnsupportedOperationException("Unsupported item property: " + id);
        }
    }

    @Override
    public Collection getItemPropertyIds() {
        return properties.keySet();
    }

    @Override
    public boolean addItemProperty(Object id, Property property) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeItemProperty(Object id) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(PropertySetChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    @Override
    public void removeListener(PropertySetChangeListener listener) {
        listeners.remove(listener);
    }

    private class PropertySetChangeEvent implements Item.PropertySetChangeEvent {
        @Override
        public Item getItem() {
            return ItemWrapper.this;
        }
    }

    @Override
    public String toString() {
        final Entity entity = getItem();
        return entity == null ? "" : entity.getInstanceName();
    }

    public Entity getItem() {
        return item instanceof Datasource ? ((Datasource) item).getItem() : (Entity) item;
    }
}