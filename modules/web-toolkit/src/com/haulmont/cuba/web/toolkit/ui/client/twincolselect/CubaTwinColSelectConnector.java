/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.twincolselect;

import com.haulmont.cuba.web.toolkit.ui.CubaTwinColSelect;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.twincolselect.TwinColSelectConnector;
import com.vaadin.shared.ui.Connect;

/**
 * @author devyatkin
 * @version $Id$
 */
@Connect(CubaTwinColSelect.class)
public class CubaTwinColSelectConnector extends TwinColSelectConnector {

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