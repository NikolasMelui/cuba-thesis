/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.toolkit.gwt.client.ui;

import com.vaadin.terminal.gwt.client.UIDL;

/**
 * @author artamonov
 * @version $Id$
 */
public class VFieldGroupComposition extends VGroupBox {

    public static final String FIELDGROUP_CLASSNAME = "fieldgroup";

    @Override
    protected void constructDOM() {
        this.mainStyleName = FIELDGROUP_CLASSNAME;

        super.constructDOM();
    }

    @Override
    public void updateFromUIDL(UIDL uidl) {
        super.updateFromUIDL(uidl);

        boolean borderVisible = uidl.hasAttribute("borderVisible") && uidl.getBooleanAttribute("borderVisible");

        if (borderVisible) {
            addStyleDependentName("border");
        } else {
            removeStyleDependentName("border");
        }
    }
}