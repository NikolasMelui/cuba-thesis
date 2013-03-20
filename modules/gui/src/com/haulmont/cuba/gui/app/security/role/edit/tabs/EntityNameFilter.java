/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.security.role.edit.tabs;

import com.google.common.base.Predicate;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.security.entity.Permission;
import com.haulmont.cuba.gui.security.entity.AssignableTarget;
import com.haulmont.cuba.gui.security.entity.EntityPermissionTarget;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

/**
 * <p>$Id$</p>
 *
 * @author artamonov
 */
public class EntityNameFilter<T extends AssignableTarget> implements Predicate<T> {

    private Metadata metadata;
    private final CheckBox assignedOnlyCheckBox;

    private final CheckBox systemLevelCheckBox;

    private final TextField entityFilter;

    public EntityNameFilter(Metadata metadata, CheckBox assignedOnlyCheckBox, CheckBox systemLevelCheckBox,
                            TextField entityFilter) {
        this.metadata = metadata;
        this.assignedOnlyCheckBox = assignedOnlyCheckBox;
        this.systemLevelCheckBox = systemLevelCheckBox;
        this.entityFilter = entityFilter;
    }

    @Override
    public boolean apply(@Nullable T target) {
        if (target != null) {
            if (Boolean.TRUE.equals(assignedOnlyCheckBox.getValue()) && !target.isAssigned())
                return false;

            if (Boolean.FALSE.equals(systemLevelCheckBox.getValue()) && (target instanceof EntityPermissionTarget)) {
                MetaClass metaClass = metadata.getSession().getClassNN(
                        ((EntityPermissionTarget) target).getEntityClass());
                if (metadata.getTools().isSystemLevel(metaClass))
                    return false;
            }

            String filterValue = StringUtils.trimToEmpty(entityFilter.<String>getValue());
            if (StringUtils.isNotBlank(filterValue)) {
                String permissionValue = target.getPermissionValue();
                int delimeterIndex = permissionValue.indexOf(Permission.TARGET_PATH_DELIMETER);
                if (delimeterIndex >= 0)
                    permissionValue = permissionValue.substring(0, delimeterIndex);
                return StringUtils.containsIgnoreCase(permissionValue, filterValue);
            } else
                return true;
        }
        return false;
    }
}