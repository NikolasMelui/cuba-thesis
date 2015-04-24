/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.security.ds;

import com.haulmont.bali.datastruct.Tree;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.config.PermissionConfig;
import com.haulmont.cuba.gui.app.security.entity.BasicPermissionTarget;

/**
 * @author abramov
 * @version $Id$
 */
public class SpecificPermissionTreeDatasource extends BasicPermissionTreeDatasource {

    protected PermissionConfig permissionConfig = AppBeans.get(PermissionConfig.class);
    protected UserSessionSource userSessionSource = AppBeans.get(UserSessionSource.NAME);

    @Override
    public Tree<BasicPermissionTarget> getPermissions() {
        return permissionConfig.getSpecific(userSessionSource.getLocale());
    }
}