/*
 * Copyright (c) 2008-2016 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.button;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.haulmont.cuba.web.toolkit.ui.CubaCopyButtonExtension;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.VButton;
import com.vaadin.shared.ui.Connect;

/**
 * @author gorelov
 */
@Connect(CubaCopyButtonExtension.class)
public class CubaCopyButtonExtensionConnector extends AbstractExtensionConnector {

    @Override
    public CubaCopyButtonExtensionState getState() {
        return (CubaCopyButtonExtensionState) super.getState();
    }

    @Override
    protected void extend(ServerConnector target) {
        final VButton button = (VButton) ((ComponentConnector) target).getWidget();

        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getState().copyTargetSelector != null) {
                    boolean success = copyToClipboard(getState().copyTargetSelector.startsWith(".")
                            ? getState().copyTargetSelector
                            : "." + getState().copyTargetSelector);
                    getRpcProxy(CubaCopyButtonExtensionServerRpc.class).copied(success);
                }
            }
        });

    }

    private native boolean copyToClipboard(String selector) /*-{
        var copyTextArea = $doc.querySelector(selector);
        copyTextArea.select();
        try {
            return $doc.execCommand('copy');
        } catch (e) {
            console.log(e.message);
            return false;
        }
    }-*/;
}
