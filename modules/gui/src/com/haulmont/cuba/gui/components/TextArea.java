/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 16.01.2009 17:01:37
 * $Id$
 */
package com.haulmont.cuba.gui.components;

public interface TextArea extends Field {

    String NAME = "textArea";

    int getRows();
    void setRows(int rows);

    int getColumns();
    void setColumns(int columns);

    int getMaxLength();
    void setMaxLength(int value);
}
