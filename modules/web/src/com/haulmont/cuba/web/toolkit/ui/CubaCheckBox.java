/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.ui.client.checkbox.CubaCheckBoxState;
import com.vaadin.ui.CheckBox;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaCheckBox extends CheckBox {

    @Override
    protected CubaCheckBoxState getState() {
        return (CubaCheckBoxState) super.getState();
    }

    @Override
    protected CubaCheckBoxState getState(boolean markAsDirty) {
        return (CubaCheckBoxState) super.getState(markAsDirty);
    }

    public boolean isCaptionManagedByLayout() {
        return getState(false).captionManagedByLayout;
    }

    public void setCaptionManagedByLayout(boolean captionManagedByLayout) {
        if (isCaptionManagedByLayout() != captionManagedByLayout) {
            getState().captionManagedByLayout = captionManagedByLayout;
        }
    }
}