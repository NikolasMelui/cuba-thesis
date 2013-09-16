/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;

import javax.swing.*;
import java.awt.*;

/**
 * @author artamonov
 * @version $Id$
 */
public class DesktopPasswordField
        extends DesktopAbstractTextField<JPasswordField>
        implements PasswordField, Component.Wrapper {

    @Override
    protected JPasswordField createTextComponentImpl() {
        JPasswordField field = new JPasswordField();
        int height = (int) field.getPreferredSize().getHeight();
        field.setPreferredSize(new Dimension(150, height));
        return field;
    }

    @Override
    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void setMaxLength(int value) {
        maxLength = value;
        doc.setMaxLength(value);
    }
}