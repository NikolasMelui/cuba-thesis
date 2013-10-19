/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.app.ui.security.user;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.PasswordField;
import com.haulmont.cuba.web.auth.WebAuthConfig;

import javax.inject.Inject;

/**
 * @author krivopustov
 * @version $Id$
 */
public class UserEditorCompanion implements UserEditor.Companion {

    @Inject
    protected Configuration configuration;

    public void initPasswordField(PasswordField passwordField) {
        passwordField.setRequired(!configuration.getConfig(WebAuthConfig.class).getUseActiveDirectory());
    }
}