/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui.client.datefield;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.web.toolkit.ui.CubaDateField;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.datefield.PopupDateFieldConnector;
import com.vaadin.shared.ui.Connect;

/**
 * @author artamonov
 * @version $Id$
 */
@Connect(CubaDateField.class)
public class CubaDateFieldConnector extends PopupDateFieldConnector {

    @Override
    public CubaDateFieldWidget getWidget() {
        return (CubaDateFieldWidget) super.getWidget();
    }

    @Override
    protected Widget createWidget() {
        return GWT.create(CubaDateFieldWidget.class);
    }

    @Override
    public CubaDateFieldState getState() {
        return (CubaDateFieldState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);

        getWidget().setMask(getState().dateMask);
    }
}