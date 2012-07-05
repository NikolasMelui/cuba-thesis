/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 18.02.2009 12:10:08
 * $Id$
 */
package com.haulmont.cuba.web.gui;

import com.haulmont.cuba.gui.components.*;
import com.vaadin.ui.Button;

import java.util.Collection;
import java.util.Collections;

class SelectAction implements Button.ClickListener {
    private Window.Lookup window;

    SelectAction(Window.Lookup window) {
        this.window = window;
    }

    public void buttonClick(Button.ClickEvent event) {
        Window.Lookup.Validator validator = window.getLookupValidator();
        if (validator != null && !validator.validate()) return;
        
        final Component lookupComponent = window.getLookupComponent();
        if (lookupComponent == null)
            throw new IllegalStateException("lookupComponent is not set");

        Collection selected;
        if (lookupComponent instanceof Table ) {
            selected = ((Table) lookupComponent).getSelected();
        } else if (lookupComponent instanceof Tree) {
            selected = ((Tree) lookupComponent).getSelected();
        } else if (lookupComponent instanceof LookupField) {
            selected = Collections.singleton(((LookupField) lookupComponent).getValue());
        } else if (lookupComponent instanceof PickerField) {
            selected = Collections.singleton(((PickerField) lookupComponent).getValue());
        } else if (lookupComponent instanceof OptionsGroup) {
            final OptionsGroup optionsGroup = (OptionsGroup) lookupComponent;
            selected = optionsGroup.getValue();
        } else {
            throw new UnsupportedOperationException("Unsupported lookupComponent type: " + lookupComponent.getClass());
        }

        final Window.Lookup.Handler lookupHandler = window.getLookupHandler();

        window.close("select");
        lookupHandler.handleLookup(selected);
    }
}
