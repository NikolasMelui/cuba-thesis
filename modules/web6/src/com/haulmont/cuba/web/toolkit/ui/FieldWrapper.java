/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;

/**
 * Simple wrapper for Cuba components which does not contain Vaadin Field
 *
 * @author artamonov
 * @version $Id$
 */
public class FieldWrapper extends CustomField {

    protected Component component;

    public FieldWrapper(Component component) {
        this.component = component;
        this.setCaption(" "); // use space in caption for proper layout

        setCompositionRoot(WebComponentsHelper.getComposition(component));
    }

    @Override
    public Class getType() {
        return Object.class;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        if (component instanceof Component.Editable) {
            ((Component.Editable) component).setEditable(!readOnly);
        } else {
            super.setReadOnly(readOnly);
        }
    }

    @Override
    public boolean isReadOnly() {
        if (component instanceof Component.Editable) {
            return !((Component.Editable) component).isEditable();
        }
        return super.isReadOnly();
    }
}