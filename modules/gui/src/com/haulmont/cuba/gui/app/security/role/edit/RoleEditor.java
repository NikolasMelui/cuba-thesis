/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.security.role.edit;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.app.security.role.edit.tabs.ScreenPermissionsFrame;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.security.entity.Role;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author abramov
 * @version $Id$
 */
public class RoleEditor extends AbstractEditor<Role> {

    @Inject
    private ScreenPermissionsFrame screensTabFrame;

    @Named("name")
    private TextField nameField;

    @Override
    protected void postInit() {
        screensTabFrame.loadPermissions();

        if (!PersistenceHelper.isNew(getItem())) {
            nameField.setEnabled(false);
        }
    }
}