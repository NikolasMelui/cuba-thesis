/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.sys.layout;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class BoxLayoutAdapter extends LayoutAdapter {

    public enum FlowDirection { X, Y }

    protected FlowDirection direction = FlowDirection.X;

    protected Component expandedComponent;

    // if true, should let children components gain more size than minimal. If false, then shrink them to minimal
    protected boolean expandLayout = false;

    public static BoxLayoutAdapter create(JComponent container) {
        MigBoxLayoutAdapter layoutAdapter = new MigBoxLayoutAdapter(container);
        container.setLayout(layoutAdapter.getLayout());
        return layoutAdapter;
    }

    public static BoxLayoutAdapter create(LayoutManager layout, JComponent container) {
        if (layout instanceof MigLayout) {
            MigBoxLayoutAdapter layoutAdapter = new MigBoxLayoutAdapter((MigLayout) layout, container);
            container.setLayout(layoutAdapter.getLayout());
            return layoutAdapter;
        }
        else
            throw new UnsupportedOperationException("Unsupported layout manager: " + layout);
    }

    public void expand(Component component) {
        expand(component, null, null);
    }

    public void expand(Component component, String height, String width) {
        expandedComponent = component;
        update();
    }

    public void setFlowDirection(BoxLayoutAdapter.FlowDirection direction) {
        this.direction = direction;
        update();
    }

    public abstract void updateConstraints(JComponent component, Object constraints);

    public boolean isExpandLayout() {
        return expandLayout;
    }

    public void setExpandLayout(boolean expandLayout) {
        this.expandLayout = expandLayout;
        update();
    }
}
