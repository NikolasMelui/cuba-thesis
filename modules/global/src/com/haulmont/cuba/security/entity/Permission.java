/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 24.12.2008 10:56:16
 *
 * $Id$
 */
package com.haulmont.cuba.security.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;

import javax.persistence.*;

/**
 * Security permission record.
 * <br>The {@link #value} may be 0,1,2 for {@link PermissionType#ENTITY_ATTR} and 0,1 for others
 */
@Entity(name = "sec$Permission")
@Table(name = "SEC_PERMISSION")
@SystemLevel
public class Permission extends StandardEntity
{
    private static final long serialVersionUID = 4188184934170706381L;

    /** @see com.haulmont.cuba.security.entity.PermissionType PermissionType.getId() */
    @Column(name = "TYPE")
    private Integer type;

    @Column(name = "TARGET", length = 100)
    private String target;

    /**
     * May be 0,1,2 for {@link PermissionType#ENTITY_ATTR} and 0,1 for others
     */
    @Column(name = "VALUE")
    private Integer value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID")
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public PermissionType getType() {
        return type == null ? null : PermissionType.fromId(type);
    }

    public void setType(PermissionType type) {
        this.type = type == null ? null : type.getId();
    }

    /** See {@link #value} */
    public Integer getValue() {
        return value;
    }

    /** See {@link #value} */
    public void setValue(Integer value) {
        this.value = value;
    }
}
