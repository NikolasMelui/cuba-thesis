/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.theme.impl;

import java.awt.*;

/**
 * <p>$Id$</p>
 *
 * @author Alexander Budarov
 */
public class FontDecorator extends PropertyPathDecorator {

    private String family;
    private Integer style;
    private Integer size;

    public FontDecorator(String propertyPath, String family, Integer style, Integer size) {
        super(propertyPath);
        this.family = family;
        this.style = style;
        this.size = size;
    }

    @Override
    protected void apply(Object object, String property) {
        java.awt.Component component = (java.awt.Component) object;
        Font oldFont = component.getFont();

        String newFamily = oldFont.getFamily();
        int newStyle = oldFont.getStyle();
        int newSize = oldFont.getSize();
        if (family != null) {
            newFamily = family;
        }

        if (style != null) {
            newStyle = style;
        }

        if (size != null) {
            newSize = size;
        }

        Font newFont = new Font(newFamily, newStyle, newSize);
        setProperty(object, property, newFont);
    }
}
