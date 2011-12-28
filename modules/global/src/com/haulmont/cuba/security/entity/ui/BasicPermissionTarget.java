/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.security.entity.ui;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;

/**
 * Non-persistent entity to show permission targets in UI
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@com.haulmont.chile.core.annotations.MetaClass(name = "sec$Target")
@SystemLevel
public class BasicPermissionTarget
        extends AbstractPermissionTarget implements Cloneable, AssignableTarget {

    @MetaProperty(mandatory = true)
    private PermissionVariant permissionVariant = PermissionVariant.NOTSET;

    public BasicPermissionTarget(String id, String caption, String permissionValue) {
        this(id, caption, permissionValue, PermissionVariant.NOTSET);
    }

    public BasicPermissionTarget(String id, String caption, String permissionValue, PermissionVariant permissionVariant) {
        super(id, caption);
        this.permissionValue = permissionValue;
        this.permissionVariant = permissionVariant;
    }

    @Override
    public boolean isAssigned() {
        return permissionVariant != PermissionVariant.NOTSET;
    }

    public PermissionVariant getPermissionVariant() {
        return permissionVariant;
    }

    public void setPermissionVariant(PermissionVariant permissionVariant) {
        this.permissionVariant = permissionVariant;
    }

    @Override
    public BasicPermissionTarget clone() throws CloneNotSupportedException {
        BasicPermissionTarget targetClone = (BasicPermissionTarget) super.clone();
        targetClone.caption = caption;
        targetClone.id = id;
        targetClone.permissionValue = permissionValue;
        targetClone.permissionVariant = permissionVariant;

        return targetClone;
    }
}