/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.data;

import com.haulmont.chile.core.common.ValueListener;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.CategoryAttributeValue;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.global.UuidProvider;
import com.haulmont.cuba.core.sys.SetValueEntity;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * The entity, that contains a set of runtime properties.
 *
 * <p>$Id$</p>
 *
 * @author devyatkin
 */

public class RuntimePropertiesEntity implements Entity, Instance, BaseEntity {

    private static final long serialVersionUID = -8091230910619941201L;

    private MetaClass metaClass;
    private UUID id;
    private Map<String,Object> values;
    private Map<String, Object> changed = new HashMap<String, Object>();
    private Set<ValueListener> listeners = new LinkedHashSet<ValueListener>();
    private Map<String,CategoryAttributeValue> categoryValues;

    public RuntimePropertiesEntity(MetaClass metaClass, Map<String, Object> variables,
                                   Map<String, CategoryAttributeValue> categoryValues) {
        this.metaClass = metaClass;
        this.id = UuidProvider.createUuid();
        this.values = variables;
        this.categoryValues = categoryValues;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUuid() {
        return id;
    }

    public Date getCreateTs() {
        return null;
    }

    public void setCreateTs(Date date) {

    }

    public String getCreatedBy() {
        return null;
    }

    public void setCreatedBy(String createdBy) {

    }

    public MetaClass getMetaClass() {
        return metaClass;
    }

    public String getInstanceName() {
        return null;
    }

    public void addListener(com.haulmont.chile.core.common.ValueListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    public <T> T getValue(String name) {
        return (T) values.get(name);
    }

    @Override
    public void setValue(String name, Object value) {
        Object oldValue = values.get(name);
        if (!ObjectUtils.equals(oldValue, value)) {
            values.put(name, value);
            changed.put(name, value);
            CategoryAttributeValue categoryValue = categoryValues.get(name);
            if (value != null) {
                if (BaseEntity.class.isAssignableFrom(value.getClass())) {
                    categoryValue.setEntityValue(((Entity) value).getUuid());
                } else {
                    setValue(categoryValue, value);
                }
            } else
                setValue(categoryValue, value);

            for (ValueListener listener : listeners) {
                listener.propertyChanged(this, name, oldValue, value);
            }
        }
    }

    public <T> T getValueEx(String propertyPath) {
        return (T) values.get(propertyPath);
    }

    @Override
    public void setValueEx(String propertyPath, Object value) {
        Object oldValue = values.get(propertyPath);
        if (!ObjectUtils.equals(oldValue, value)) {
            values.put(propertyPath, value);
            changed.put(propertyPath, value);
            CategoryAttributeValue attrValue = categoryValues.get(propertyPath);
            if (value != null) {
                if (Entity.class.isAssignableFrom(value.getClass())) {
                    attrValue.setEntityValue(((Entity) value).getUuid());
                } else {
                    setValue(attrValue, value);
                }
            } else {
                setValue(attrValue, value);
            }
            for (ValueListener listener : listeners) {
                listener.propertyChanged(this, propertyPath, oldValue, value);
            }
        }
    }

    public CategoryAttributeValue getCategoryValue(String name){
        return categoryValues.get(name);
    }

    private void setValue(CategoryAttributeValue attrValue, Object value) {
        if (attrValue.getCategoryAttribute().getIsEntity()) {
            attrValue.setEntityValue((UUID) value);
        } else {
            String dataType = attrValue.getCategoryAttribute().getDataType();
            switch (RuntimePropsDatasource.PropertyType.valueOf(dataType)) {
                case INTEGER:
                    attrValue.setIntValue((Integer) value);
                    break;
                case DOUBLE:
                    attrValue.setDoubleValue((Double) value);
                    break;
                case BOOLEAN:
                    attrValue.setBooleanValue((Boolean) value);
                    break;
                case DATE:
                    attrValue.setDateValue((Date) value);
                    break;
                case STRING:
                    attrValue.setStringValue(StringUtils.trimToNull((String) value));
                    break;
                case ENUMERATION:
                    if (value != null)
                        attrValue.setStringValue(((SetValueEntity) value).getValue());
                    else attrValue.setStringValue(null);
                    break;
                case ENTITY:
                    attrValue.setEntityValue((UUID) value);
                    break;
            }
        }
    }
}
