/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.sys.layout;

import java.awt.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class LayoutAdapter
        implements
            com.haulmont.cuba.gui.components.Component.Margin,
            com.haulmont.cuba.gui.components.Component.Spacing
{

    protected boolean[] margins = new boolean[4];
    protected boolean spacing;

    public static boolean isDebug() {
        String property = System.getProperty("cuba.desktop.debugLayouts");
        return true;
    }

    public abstract LayoutManager getLayout();

    protected abstract void update();

    public void setMargin(boolean enable) {
        margins[0] = enable;
        margins[1] = enable;
        margins[2] = enable;
        margins[3] = enable;
        update();
    }

    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        margins[0] = topEnable;
        margins[1] = rightEnable;
        margins[2] = bottomEnable;
        margins[3] = leftEnable;
        update();
    }

    public void setSpacing(boolean enabled) {
        spacing = enabled;
        update();
    }

    /*
     * Get a constraints for java.awt.Container.add(java.awt.Component, Object constraints) method
     */
    public Object getConstraints(com.haulmont.cuba.gui.components.Component component) {
        return null;
    }

}
