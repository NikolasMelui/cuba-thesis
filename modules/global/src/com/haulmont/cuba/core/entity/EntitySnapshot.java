/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.impl.DateTimeDatatype;
import com.haulmont.cuba.core.global.UserSessionProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.persistence.Persistent;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

/**
 * Snapshot for system entity
 * <p>$Id$</p>
 *
 * @author artamonov
 */
@Entity(name = "core$EntitySnapshot")
@Table(name = "SYS_ENTITY_SNAPSHOT")
public class EntitySnapshot extends BaseUuidEntity {

    private static final long serialVersionUID = 4835363127711391591L;

    @Column(name = "VIEW_XML")
    private String viewXml;

    @Column(name = "SNAPSHOT_XML")
    private String snapshotXml;

    @Column(name = "ENTITY_META_CLASS")
    private String entityMetaClass;

    @Column(name = "ENTITY_ID")
    @Persistent
    private UUID entityId;

    public String getViewXml() {
        return viewXml;
    }

    public void setViewXml(String viewXml) {
        this.viewXml = viewXml;
    }

    public String getSnapshotXml() {
        return snapshotXml;
    }

    public void setSnapshotXml(String snapshotXml) {
        this.snapshotXml = snapshotXml;
    }

    public String getEntityMetaClass() {
        return entityMetaClass;
    }

    public void setEntityMetaClass(String entityMetaClass) {
        this.entityMetaClass = entityMetaClass;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    @MetaProperty
    public String getLabel() {
        String name = "";
        if (StringUtils.isNotEmpty(this.createdBy))
           name += this.createdBy + " ";

        Datatype datatype = Datatypes.get(DateTimeDatatype.NAME);
        name += datatype.format(createTs, UserSessionProvider.getLocale());

        return name;
    }

    @MetaProperty
    public String getAuthor() {
        return this.createdBy;
    }

    @MetaProperty
    public Date getChangeDate() {
        return this.createTs;
    }
}
