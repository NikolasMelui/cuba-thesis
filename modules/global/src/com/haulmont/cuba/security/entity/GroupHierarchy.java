/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 17.12.2008 15:12:01
 *
 * $Id$
 */
package com.haulmont.cuba.security.entity;

import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;

import javax.persistence.*;

/**
 * User group hierarchy
 */
@Entity(name = "sec$GroupHierarchy")
@Table(name = "SEC_GROUP_HIERARCHY")
@SystemLevel
public class GroupHierarchy extends BaseUuidEntity
{
    private static final long serialVersionUID = 8106113488822530560L;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "GROUP_ID")
    private Group group;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private Group parent;

    @Column(name = "HIERARCHY_LEVEL")
    private Integer level;

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }
}
