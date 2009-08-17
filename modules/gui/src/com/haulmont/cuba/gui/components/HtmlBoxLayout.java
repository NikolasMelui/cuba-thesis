/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 17.08.2009 17:09:25
 *
 * $Id$
 */
package com.haulmont.cuba.gui.components;

public interface HtmlBoxLayout extends Layout, Component.Expandable {
    String getTemplateName();
    void setTemplateName(String templateName);
}
