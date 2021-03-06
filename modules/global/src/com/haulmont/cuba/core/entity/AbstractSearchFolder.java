/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * @author krivopustov
 * @version $Id$
 */
@MappedSuperclass
@SystemLevel
public abstract class AbstractSearchFolder extends Folder {

    private static final long serialVersionUID = -2234453892776090930L;

    @Column(name = "FILTER_COMPONENT")
    protected String filterComponentId;

    @Column(name = "FILTER_XML")
    protected String filterXml;

    @Column(name = "APPLY_DEFAULT")
    protected Boolean applyDefault = true;

    public void copyFrom(AbstractSearchFolder srcFolder) {
        setCreatedBy(srcFolder.getCreatedBy());
        setCreateTs(srcFolder.getCreateTs());
        setDeletedBy(srcFolder.getDeletedBy());
        setDeleteTs(srcFolder.getDeleteTs());
        setFilterComponentId(srcFolder.getFilterComponentId());
        setFilterXml(srcFolder.getFilterXml());
        setName(srcFolder.getCaption());
        setTabName(srcFolder.getTabName());
        setParent(srcFolder.getParent());
        setItemStyle(srcFolder.getItemStyle());
        setSortOrder(srcFolder.getSortOrder());
    }

    public String getFilterComponentId() {
        return filterComponentId;
    }

    public void setFilterComponentId(String filterComponentId) {
        this.filterComponentId = filterComponentId;
    }

    public String getFilterXml() {
        return filterXml;
    }

    public void setFilterXml(String filterXml) {
        this.filterXml = filterXml;
    }

    public Boolean getApplyDefault() {
        return applyDefault;
    }

    public void setApplyDefault(Boolean applyDefault) {
        this.applyDefault = applyDefault;
    }

    @MetaProperty
    public String getLocName() {
        if (StringUtils.isNotEmpty(name)) {
            Messages messages = AppBeans.get(Messages.NAME);
            return messages.getMainMessage(name);
        }
        return null;
    }
}