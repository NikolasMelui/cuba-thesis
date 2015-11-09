/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.data;

import com.haulmont.chile.core.common.ValueListener;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.BaseGenericIdEntity;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.entity.CategoryAttributeValue;
import com.haulmont.cuba.core.global.UuidProvider;

import java.util.*;

/**
 * Specific entity, delegating all calls to internal BaseGenericIdEntity.
 *
 * Obsolete. Will be removed in future releases.
 *
 * @author devyatkin
 * @version $Id$
 * @version $Id$
 */
public class DynamicAttributesEntity implements BaseEntity {
    private static final long serialVersionUID = -8091230910619941201L;
    protected BaseGenericIdEntity mainItem;
    protected UUID id;
    protected Map<String, CategoryAttribute> attributesMap = new HashMap<>();

    public DynamicAttributesEntity(BaseGenericIdEntity mainItem, Collection<CategoryAttribute> attributes) {
        this.mainItem = mainItem;
        this.id = UuidProvider.createUuid();
        for (CategoryAttribute attribute : attributes) {
            attributesMap.put(attribute.getCode(), attribute);
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isDetached() {
        return false;
    }

    @Override
    public void setDetached(boolean detached) {
    }

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public Date getCreateTs() {
        return null;
    }

    @Override
    public void setCreateTs(Date date) {
    }

    @Override
    public String getCreatedBy() {
        return null;
    }

    @Override
    public void setCreatedBy(String createdBy) {
    }

    @Override
    public MetaClass getMetaClass() {
        return mainItem.getMetaClass();
    }

    @Override
    public String getInstanceName() {
        return null;
    }

    @Override
    public void addListener(com.haulmont.chile.core.common.ValueListener listener) {
        mainItem.addListener(listener);
    }

    @Override
    public void removeListener(ValueListener listener) {
        mainItem.removeListener(listener);
    }

    @Override
    public void removeAllListeners() {
        mainItem.removeAllListeners();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String name) {
        return (T) mainItem.getValue(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(String name, Object value) {
        mainItem.setValue(name, value);

        //if we set an attribute from another type of entity, we need to set reference to CategoryAttribute manually
        //this is workaround to make #PL-5770 logic works with modern RuntimePropertiesDatasource
        String attributeCode = DynamicAttributesUtils.decodeAttributeCode(name);
        Map<String, CategoryAttributeValue> dynamicAttributes = mainItem.getDynamicAttributes();
        if (dynamicAttributes != null) {
            CategoryAttributeValue categoryAttributeValue = dynamicAttributes.get(attributeCode);
            if (categoryAttributeValue != null && categoryAttributeValue.getCategoryAttribute() == null) {
                categoryAttributeValue.setCategoryAttribute(attributesMap.get(attributeCode));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueEx(String propertyPath) {
        return (T) mainItem.getValueEx(propertyPath);
    }

    @Override
    public void setValueEx(String propertyPath, Object value) {
        mainItem.setValueEx(propertyPath, value);
    }
}