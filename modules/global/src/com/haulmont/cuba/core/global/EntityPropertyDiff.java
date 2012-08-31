/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.AbstractNotPersistentEntity;

import java.io.Serializable;

/**
 * Diff between properties in entity snapshots
 * <p>$Id$</p>
 *
 * @author artamonov
 */
@MetaClass(name = "sys$EntityPropertyDiff")
public abstract class EntityPropertyDiff extends AbstractNotPersistentEntity implements Serializable {

    protected static final int CAPTION_CHAR_COUNT = 30;

    public enum ItemState {
        Normal,
        Modified,
        Added,
        Removed
    }

    private static final long serialVersionUID = -6467322033937742101L;

    private ViewProperty viewProperty;
    protected String propertyCaption;
    protected String label = "";
    protected String metaClassName = "";

    protected EntityPropertyDiff(ViewProperty viewProperty, com.haulmont.chile.core.model.MetaProperty metaProperty) {
        this.viewProperty = viewProperty;
        this.propertyCaption = AppBeans.get(MessageTools.class).getPropertyCaption(metaProperty);
        this.metaClassName = metaProperty.getDomain().getFullName();
    }

    public ViewProperty getViewProperty() {
        return viewProperty;
    }

    public String getMetaClassName() {
        return metaClassName;
    }

    @MetaProperty
    public String getName() {
        return propertyCaption;
    }

    @MetaProperty
    public void setName(String name) {
        propertyCaption = name;
    }

    @MetaProperty
    public String getLabel() {
        return label;
    }

    @MetaProperty
    public void setLabel(String label) {
        this.label = label;
    }

    public boolean hasStateValues() {
        return false;
    }

    public Object getBeforeValue() {
        return null;
    }

    public Object getAfterValue() {
        return null;
    }

    @MetaProperty
    public String getBeforeString() {
        return "";
    }

    @MetaProperty
    public String getAfterString() {
        return "";
    }

    @MetaProperty
    public String getBeforeCaption() {
        return getBeforeString();
    }

    @MetaProperty
    public String getAfterCaption() {
        return getAfterString();
    }

    @MetaProperty
    public ItemState getItemState() {
        return ItemState.Normal;
    }

    @MetaProperty
    public void setItemState(ItemState itemState) {

    }

    public boolean itemStateVisible() {
        return false;
    }
}