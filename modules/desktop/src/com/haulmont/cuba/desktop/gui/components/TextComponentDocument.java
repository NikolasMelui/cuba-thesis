/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class TextComponentDocument extends PlainDocument {

    private static final long serialVersionUID = -3290292622981584624L;

    public static final int UNLIMITED_LENGTH = -1;

    private int maxLength = UNLIMITED_LENGTH;

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (str == null)
            return;

        if (maxLength == UNLIMITED_LENGTH || (getLength() + str.length()) <= maxLength) {
            super.insertString(offs, str, a);
        }
    }
}
