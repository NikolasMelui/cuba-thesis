/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui.client.twincolselect;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.web.toolkit.ui.CubaTwinColSelect;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.twincolselect.TwinColSelectConnector;
import com.vaadin.shared.ui.Connect;

/**
 * @author devyatkin
 * @version $Id$
 */
@Connect(value = CubaTwinColSelect.class, loadStyle = Connect.LoadStyle.LAZY)
public class CubaTwinColSelectConnector extends TwinColSelectConnector {

    @Override
    protected Widget createWidget() {
        return GWT.create(CubaTwinColSelectWidget.class);
    }

    @Override
    public CubaTwinColSelectWidget getWidget() {
        return (CubaTwinColSelectWidget) super.getWidget();
    }

    @Override
    public CubaTwinColSelectState getState() {
        return (CubaTwinColSelectState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        getWidget().setAddAllBtnEnabled(getState().addAllBtnEnabled);
    }
}
