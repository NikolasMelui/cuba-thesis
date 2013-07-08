/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.app.ui.security.role;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.app.security.role.edit.tabs.SpecificPermissionsFrame;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.security.entity.BasicPermissionTarget;
import com.haulmont.cuba.gui.security.entity.PermissionVariant;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.vaadin.shared.ui.label.ContentMode;

/**
 * @author artamonov
 * @version $Id$
 */
public class SpecificPermissionsFrameCompanion implements SpecificPermissionsFrame.Companion{
    @Override
    public void initPermissionColoredColumns(TreeTable specificPermissionsTree) {
        specificPermissionsTree.addGeneratedColumn("permissionVariant", new Table.ColumnGenerator<BasicPermissionTarget>() {
            @Override
            public Component generateCell(BasicPermissionTarget entity) {
                Label label = AppConfig.getFactory().createComponent(Label.NAME);
                com.vaadin.ui.Label vLabel = (com.vaadin.ui.Label) WebComponentsHelper.unwrap(label);
                vLabel.setContentMode(ContentMode.HTML);

                StringBuilder builder = new StringBuilder();

                PermissionVariant permissionVariant = entity.getPermissionVariant();

                if (permissionVariant != PermissionVariant.NOTSET) {
                    builder.append("<span style=\"color:").append(permissionVariant.getColor()).append(";\">")
                            .append(AppBeans.get(Messages.class).getMessage(permissionVariant)).append("</span>");
                }

                vLabel.setValue(builder.toString());

                return label;
            }
        });
    }
}