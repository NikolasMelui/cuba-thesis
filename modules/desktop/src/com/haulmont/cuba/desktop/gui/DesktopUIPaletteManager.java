/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui;

import com.haulmont.cuba.gui.ComponentPalette;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;

/**
 * <p>$Id$</p>
 *
 * @author artamonov
 */
public class DesktopUIPaletteManager {

    public static void registerPalettes(ComponentPalette... palettes) {
        LayoutLoaderConfig.registerLoaders(palettes);
        DesktopComponentsFactory.registerComponents(palettes);
    }
}