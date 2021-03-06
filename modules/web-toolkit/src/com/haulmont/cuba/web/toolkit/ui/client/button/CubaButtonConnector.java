/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.button;

import com.google.gwt.event.dom.client.ClickEvent;
import com.haulmont.cuba.web.toolkit.ui.CubaButton;
import com.haulmont.cuba.web.toolkit.ui.client.appui.ValidationErrorHolder;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.communication.StateChangeEvent;

import com.vaadin.client.ui.button.ButtonConnector;
import com.vaadin.shared.ui.Connect;

/**
 * @author artamonov
 * @version $Id$
 */
@Connect(value = CubaButton.class, loadStyle = Connect.LoadStyle.EAGER)
public class CubaButtonConnector extends ButtonConnector {

    protected boolean pendingResponse = false;

    public CubaButtonConnector() {
        registerRpc(CubaButtonClientRpc.class, new CubaButtonClientRpc() {
            @Override
            public void onClickHandled() {
                stopResponsePending();
            }
        });
    }

    @Override
    public CubaButtonState getState() {
        return (CubaButtonState) super.getState();
    }

    @Override
    public CubaButtonWidget getWidget() {
        return (CubaButtonWidget) super.getWidget();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        stopResponsePending();

        super.onStateChanged(stateChangeEvent);

        if (stateChangeEvent.hasPropertyChanged("caption")) {
            String text = getState().caption;
            if (text == null || "".equals(text)) {
                getWidget().addStyleDependentName("empty-caption");
            } else {
                getWidget().removeStyleDependentName("empty-caption");
            }
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        if (ValidationErrorHolder.hasValidationErrors()) {
            return;
        }

        if (pendingResponse) {
            return;
        }

        if (getState().useResponsePending) {
            startResponsePending();
        }

        super.onClick(event);
    }

    public void stopResponsePending() {
        pendingResponse = false;
        getWidget().removeStyleDependentName("wait");
    }

    protected void startResponsePending() {
        pendingResponse = true;
        getWidget().addStyleDependentName("wait");
    }
}