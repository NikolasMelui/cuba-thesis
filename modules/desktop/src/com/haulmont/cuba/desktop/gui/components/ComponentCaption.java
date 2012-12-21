/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.sys.DesktopToolTipManager;
import com.haulmont.cuba.desktop.sys.layout.BoxLayoutAdapter;
import com.haulmont.cuba.desktop.sys.vcl.ToolTipButton;
import com.haulmont.cuba.gui.components.Component;

import javax.swing.*;

/**
 * @author budarov
 * @version $Id$
 */
public class ComponentCaption extends JPanel {

    private Component owner;
    private JLabel label;

    public ComponentCaption(Component owner) {
        BoxLayoutAdapter.create(this);
        this.owner = owner;
        takeOwnerProperties();
    }

    private void takeOwnerProperties() {
        if (label == null) {
            label = new JLabel();
            add(label);
        }

        label.setText(((Component.HasCaption) owner).getCaption());
        if (((Component.HasCaption) owner).getDescription() != null) {
            ToolTipButton btn = new ToolTipButton();
            btn.setFocusable(false);
            btn.setToolTipText(((Component.HasCaption) owner).getDescription());
            DesktopToolTipManager.getInstance().registerTooltip(btn);
            add(btn);
        }

        setVisible(owner.isVisible());
        setEnabled(owner.isEnabled());
    }

    public void update() {
        takeOwnerProperties();
    }
}