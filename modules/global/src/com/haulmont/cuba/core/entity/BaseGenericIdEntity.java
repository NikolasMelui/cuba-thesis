/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.entity;

import com.google.common.base.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.impl.AbstractInstance;
import com.haulmont.cuba.core.app.runtimeproperties.RuntimePropertiesUtils;
import com.haulmont.cuba.core.global.*;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.DetachedStateManager;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for persistent entities.
 * <p>When choosing a base class for your entity, consider more specific base classes defining the primary key type:
 * <ul>
 * <li>{@link BaseUuidEntity}</li>
 * <li>{@link BaseLongIdEntity}</li>
 * <li>{@link BaseIntegerIdEntity}</li>
 * <li>{@link BaseStringIdEntity}</li>
 * </ul>
 * or most commonly used {@link StandardEntity}.
 * </p>
 *
 * @author krivopustov
 * @version $Id$
 */
@MappedSuperclass
public abstract class BaseGenericIdEntity<T> extends AbstractInstance implements BaseEntity<T> {

    private static final long serialVersionUID = -8400641366148656528L;

    @Transient
    protected boolean detached;

    @Column(name = "CREATE_TS")
    protected Date createTs;

    @Column(name = "CREATED_BY", length = LOGIN_FIELD_LEN)
    protected String createdBy;

    protected Map<String, CategoryAttributeValue> runtimeProperties = null;

    public abstract void setId(T id);

    @Override
    public boolean isDetached() {
        return detached;
    }

    @Override
    public void setDetached(boolean detached) {
        this.detached = detached;
    }

    @Override
    public MetaClass getMetaClass() {
        Metadata metadata = AppBeans.get(Metadata.NAME);
        return metadata.getSession().getClassNN(getClass());
    }

    @Override
    public Date getCreateTs() {
        return createTs;
    }

    @Override
    public void setCreateTs(Date createTs) {
        this.createTs = createTs;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Called from enhanced setters before property is changed.
     *
     * @param name       property name
     * @param fieldIndex corresponding OpenJPA field index
     * @param value      new value
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void propertyChanging(String name, int fieldIndex, Object value) {
        if (!allowSetNotLoadedAttributes
                && fieldIndex > -1
                && this instanceof PersistenceCapable
                && ((PersistenceCapable) this).pcGetStateManager() instanceof DetachedStateManager) {
            BitSet loaded = ((DetachedStateManager) ((PersistenceCapable) this).pcGetStateManager()).getLoaded();
            if (!loaded.get(fieldIndex)) {
                throw new IllegalEntityStateException("Property '" +
                        getClass().getCanonicalName() + "." + name + "' is not loaded");
            }
        }
    }

    /**
     * For internal use only.
     */
    public static boolean allowSetNotLoadedAttributes;

    @Override
    public void setValue(String property, Object obj, boolean checkEquals) {
        if (RuntimePropertiesUtils.isRuntimeProperty(property)) {
            Preconditions.checkState(runtimeProperties != null, "Runtime properties should be loaded explicitly");
            String attributeCode = RuntimePropertiesUtils.decodeAttributeCode(property);
            CategoryAttributeValue categoryAttributeValue = runtimeProperties.get(attributeCode);
            if (categoryAttributeValue != null) {
                if (obj != null) {
                    categoryAttributeValue.setValue(obj);
                } else {
                    categoryAttributeValue.setDeleteTs(AppBeans.get(TimeSource.class).currentTimestamp());
                }
            } else if (obj != null) {
                categoryAttributeValue = new CategoryAttributeValue();
                categoryAttributeValue.setValue(obj);
                categoryAttributeValue.setEntityId(getUuid());
                categoryAttributeValue.setCode(attributeCode);
                runtimeProperties.put(attributeCode, categoryAttributeValue);
            }

            propertyChanged(property, null, obj);
        } else {
            super.setValue(property, obj, checkEquals);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValue(String property) {
        if (RuntimePropertiesUtils.isRuntimeProperty(property)) {
            if (PersistenceHelper.isNew(this) && runtimeProperties == null) {
                    runtimeProperties = new HashMap<>();
            }

            Preconditions.checkState(runtimeProperties != null, "Runtime properties should be loaded explicitly");
            CategoryAttributeValue categoryAttributeValue = runtimeProperties.get(RuntimePropertiesUtils.decodeAttributeCode(property));
            if (categoryAttributeValue != null) {
                return (T) categoryAttributeValue.getValue();
            } else {
                return null;
            }
        } else {
            return super.getValue(property);
        }
    }

    public void setRuntimeProperties(Map<String, CategoryAttributeValue> runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Nullable
    public Map<String, CategoryAttributeValue> getRuntimeProperties() {
        return runtimeProperties;
    }
}