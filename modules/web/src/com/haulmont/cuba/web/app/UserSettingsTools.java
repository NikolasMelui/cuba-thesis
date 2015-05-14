/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.app;

import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.security.app.UserSettingService;
import com.haulmont.cuba.web.WebConfig;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * Utility bean for work with user settings on web client tier.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(UserSettingsTools.NAME)
public class UserSettingsTools {

    public static final String NAME = "cuba_UserSettingsTools";

    public static class FoldersState {

        public final boolean visible;
        public final int horizontalSplit;
        public final int verticalSplit;

        public FoldersState(boolean visible, int horizontalSplit, int verticalSplit) {
            this.horizontalSplit = horizontalSplit;
            this.verticalSplit = verticalSplit;
            this.visible = visible;
        }
    }

    @Inject
    protected UserSettingService userSettingService;

    @Inject
    protected WebConfig webConfig;

    public AppWorkArea.Mode loadAppWindowMode() {
        String s = userSettingService.loadSetting(ClientType.WEB, "appWindowMode");
        if (s != null) {
            if (AppWorkArea.Mode.SINGLE.name().equals(s)) {
                return AppWorkArea.Mode.SINGLE;
            } else if (AppWorkArea.Mode.TABBED.name().equals(s)) {
                return AppWorkArea.Mode.TABBED;
            }
        }
        return AppWorkArea.Mode.valueOf(webConfig.getAppWindowMode().toUpperCase());
    }

    public void saveAppWindowMode(AppWorkArea.Mode mode) {
        userSettingService.saveSetting(ClientType.WEB, "appWindowMode", mode.name());
    }

    public String loadAppWindowTheme() {
        String s = userSettingService.loadSetting(ClientType.WEB, "appWindowTheme");
        if (s != null) {
            return s;
        }
        return webConfig.getAppWindowTheme();
    }

    public void saveAppWindowTheme(String theme) {
        userSettingService.saveSetting(ClientType.WEB, "appWindowTheme", theme);
    }

    public FoldersState loadFoldersState() {
        String s = userSettingService.loadSetting(ClientType.WEB, "foldersState");
        if (s == null)
            return null;

        String[] parts = s.split(",");
        if (parts.length != 3)
            return null;

        try {
            return new FoldersState(Boolean.parseBoolean(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    public void saveFoldersState(boolean visible, int horizontalSplit, int verticalSplit) {
        userSettingService.saveSetting(ClientType.WEB, "foldersState",
                String.valueOf(visible) + ","
                + String.valueOf(horizontalSplit) + ","
                + String.valueOf(verticalSplit)
        );
    }
}