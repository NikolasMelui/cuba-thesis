/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.sys.vcl;

/**
 * @author artamonov
 */
public interface FocusableTable {

    TableFocusManager getFocusManager();

    void setFocusManager(TableFocusManager focusManager);
}