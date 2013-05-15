/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 17:20:39
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.web.toolkit.ui.CubaVerticalActionsLayout;
import com.vaadin.server.Sizeable;
//import com.vaadin.terminal.gwt.client.ui.VVerticalLayout;
//import com.vaadin.ui.ClientWidget;

@SuppressWarnings("serial")
//@ClientWidget(VVerticalLayout.class)
public class WebVBoxLayout extends WebAbstractBox  /*implements BoxLayout*/ {
    public WebVBoxLayout() {
        component = new CubaVerticalActionsLayout();
        component.setWidth(100, Sizeable.Unit.PERCENTAGE);
    }
}
