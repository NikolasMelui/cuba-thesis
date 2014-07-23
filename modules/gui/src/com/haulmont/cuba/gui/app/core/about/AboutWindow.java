/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.core.about;

import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.theme.ThemeConstants;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author krivopustov
 * @version $Id$
 */
public class AboutWindow extends AbstractWindow {

    @Inject
    protected ThemeConstants themeConstants;

    @Override
    public void init(Map<String, Object> params) {
        getDialogParams()
                .setWidth(themeConstants.getInt("cuba.gui.AboutWindow.width"))
                .setHeight(themeConstants.getInt("cuba.gui.AboutWindow.height"))
                .setResizable(false);
    }
}