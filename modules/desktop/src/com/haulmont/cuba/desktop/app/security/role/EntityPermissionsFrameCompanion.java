/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.app.security.role;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.desktop.gui.components.DesktopComponentsHelper;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.app.security.role.edit.tabs.EntityPermissionsFrame;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.app.security.entity.OperationPermissionTarget;
import com.haulmont.cuba.gui.app.security.entity.PermissionVariant;
import com.haulmont.cuba.gui.components.TextField;

import javax.swing.*;

/**
 * @author artamonov
 * @version $id$
 */
public class EntityPermissionsFrameCompanion implements EntityPermissionsFrame.Companion {

    protected Messages messages = AppBeans.get(Messages.NAME);

    @Override
    public void initPermissionColoredColumns(Table entityPermissionsTable) {
        entityPermissionsTable.addGeneratedColumn("createPermissionVariant", new Table.ColumnGenerator<OperationPermissionTarget>() {
            @Override
            public Component generateCell(OperationPermissionTarget target) {
                return generateLabelByPermissionVariant(target.getCreatePermissionVariant());
            }
        }, Label.class);

        entityPermissionsTable.addGeneratedColumn("readPermissionVariant", new Table.ColumnGenerator<OperationPermissionTarget>() {
            @Override
            public Component generateCell(OperationPermissionTarget target) {
                return generateLabelByPermissionVariant(target.getReadPermissionVariant());
            }
        }, Label.class);

        entityPermissionsTable.addGeneratedColumn("updatePermissionVariant", new Table.ColumnGenerator<OperationPermissionTarget>() {
            @Override
            public Component generateCell(OperationPermissionTarget target) {
                return generateLabelByPermissionVariant(target.getUpdatePermissionVariant());
            }
        }, Label.class);

        entityPermissionsTable.addGeneratedColumn("deletePermissionVariant", new Table.ColumnGenerator<OperationPermissionTarget>() {
            @Override
            public Component generateCell(OperationPermissionTarget target) {
                return generateLabelByPermissionVariant(target.getDeletePermissionVariant());
            }
        }, Label.class);
    }

    @Override
    public void initTextFieldFilter(TextField entityFilter, Runnable runnable) {
        DesktopComponentsHelper.addEnterShortcut(entityFilter, runnable);
    }

    private Label generateLabelByPermissionVariant(PermissionVariant permissionVariant) {
        Label label = AppConfig.getFactory().createComponent(Label.NAME);
        JLabel jLabel = DesktopComponentsHelper.unwrap(label);

        StringBuilder builder = new StringBuilder();

        if (permissionVariant != PermissionVariant.NOTSET) {
            builder.append("<html>");
            builder.append("<font color=\"").append(permissionVariant.getColor()).append("\">")
                    .append(messages.getMessage(permissionVariant)).append("</font>");

            builder.append("</html>");
        }

        jLabel.setText(builder.toString());

        return label;
    }
}