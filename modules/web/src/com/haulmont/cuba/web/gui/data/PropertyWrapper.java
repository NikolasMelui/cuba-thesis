/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 29.12.2008 17:16:49
 * $Id$
 */
package com.haulmont.cuba.web.gui.data;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.Range;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DatasourceListener;
import com.vaadin.data.Property;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class PropertyWrapper implements Property, Property.ValueChangeNotifier {
    private boolean readOnly;
    private Object item;

    protected MetaPropertyPath propertyPath;

    private List<ValueChangeListener> listeners = new ArrayList<ValueChangeListener>();

    public PropertyWrapper(Object item, MetaPropertyPath propertyPath) {
        this.item = item;
        this.propertyPath = propertyPath;
        if (item instanceof Datasource) {
            ((Datasource) item).addListener(new DatasourceListener<Entity>() {
                public void itemChanged(Datasource<Entity> ds, Entity prevItem, Entity item) {
                    fireValueChangeEvent();
                }

                public void stateChanged(Datasource<Entity> ds, Datasource.State prevState, Datasource.State state) {}

                public void valueChanged(Entity source, String property, Object prevValue, Object value) {
                    fireValueChangeEvent();
                }
            });
        }
    }

    protected void fireValueChangeEvent() {
        final ValueChangeEvent changeEvent = new ValueChangeEvent();
        for (ValueChangeListener listener : new ArrayList<ValueChangeListener>(listeners)) {
            listener.valueChange(changeEvent);
        }
    }

    public Object getValue() {
        final Instance instance = getInstance();
        Object value = instance == null ? null : InstanceUtils.getValueEx(instance, propertyPath.getPath());
        if (value == null && propertyPath.getRange().isDatatype()
                && propertyPath.getRange().asDatatype().equals(Datatypes.getInstance().get(Boolean.class))) {
            value = Boolean.FALSE;
        }
        return value;
    }

    protected Instance getInstance() {
        if (item instanceof Datasource) {
            final Datasource ds = (Datasource) item;
            if (Datasource.State.VALID.equals(ds.getState())) {
                return (Instance) ds.getItem();
            } else {
                return null;
            }
        } else {
            return (Instance) item;
        }
    }

    public void setValue(Object newValue) throws ReadOnlyException, ConversionException {
        final Instance instance = getInstance();
        if (instance == null) throw new IllegalStateException("Instance is null");
        
        InstanceUtils.setValueEx(instance, propertyPath.getPath(), valueOf(newValue));
    }

    protected Object valueOf(Object newValue) throws Property.ConversionException{
        final Range range = propertyPath.getRange();
        if (range == null) {
            return newValue;
        } else {
            if (range.isDatatype() && newValue instanceof String) {
                try {
                    final Object value = range.asDatatype().parse((String) newValue);
                    return value;
                } catch (ParseException e) {
                    throw new Property.ConversionException(e);
                }
            } else {
                return newValue;
            }
        }
    }

    public Class getType() {
        return propertyPath.getRangeJavaClass();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean newStatus) {
        readOnly = newStatus;
    }

    @Override
    public String toString() {
        final Object value = getValue();
        if (value == null) return null;

        final Range range = propertyPath.getRange();
        if (range.isDatatype()) {
            return range.asDatatype().format(value);
        } else if (range.isEnum()){
            String nameKey = value.getClass().getSimpleName() + "." + value.toString();
            return MessageProvider.getMessage(value.getClass(), nameKey);
        } else {
            if (value instanceof Instance)
                return ((Instance) value).getInstanceName();
            else
                return value.toString();
        }
    }

    public void addListener(ValueChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(ValueChangeListener listener) {
        listeners.remove(listener);
    }

    private class ValueChangeEvent implements Property.ValueChangeEvent {
        public Property getProperty() {
            return PropertyWrapper.this;
        }
    }
}
