/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.theme;

import com.haulmont.cuba.desktop.Resources;

import java.util.Set;

/**
 * <p>$Id$</p>
 *
 * @author Alexander Budarov
 */
public interface DesktopTheme {

    /**
     * @return name of theme
     */
    String getName();

    /**
     * Invoke this method before any UI components initialization.
     * Theme sets up look and feel, assigns UI defaults overrides.
     */
    void init();

    /**
     * Apply style to CUBA, swing or AWT component.
     *
     * @param component component
     * @param styleName space-separated list of styles to apply
     */
    void applyStyle(Object component, String styleName);

    /**
     * Apply style to CUBA, swing or AWT component.
     * This method is used by table style providers to reflect focus and selection states.
     *
     * @param component component
     * @param styleName space-separated list of styles to apply
     * @param state     set of strings describing internal swing component state
     */
    void applyStyle(Object component, String styleName, Set<String> state);

    /**
     * Return resources associated with theme.
     *
     * @return resources
     */
    Resources getResources();
}
