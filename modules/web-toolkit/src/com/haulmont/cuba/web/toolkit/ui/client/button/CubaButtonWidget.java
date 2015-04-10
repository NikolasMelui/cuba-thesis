/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.button;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VButton;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaButtonWidget extends VButton {

    public CubaButtonWidget() {
        addStyleDependentName("empty-caption");
    }

    @Override
    public void onClick(ClickEvent event) {
        if (BrowserInfo.get().isIE() && BrowserInfo.get().getIEVersion() >= 11) {
            // fix focusing of button-wrap in IE11
            setFocus(true);
        }

        super.onClick(event);
    }

    @Override
    public void setText(String text) {
        if (text == null || "".equals(text)) {
            addStyleDependentName("empty-caption");
        } else {
            removeStyleDependentName("empty-caption");
        }

        super.setText(text);
    }

    @Override
    protected boolean handleKeyboardEvents(Event event) {
        int type = DOM.eventGetType(event);
        // Synthesize clicks based on keyboard events AFTER the normal key
        // handling.
        if ((event.getTypeInt() & Event.KEYEVENTS) != 0) {
            switch (type) {
                case Event.ONKEYDOWN:
                    // Stop propagation when the user starts pressing a button that
                    // we are handling to prevent actions from getting triggered
                    if (event.getKeyCode() == 32 /* space */) {
                        isFocusing = true;
                        event.preventDefault();
                        event.stopPropagation();
                        return true;
                    } else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                        isFocusing = true;
                        event.stopPropagation();
                        return true;
                    }
                    break;

                // CAUTION IE sometimes does not generate ONKEYPRESS for ENTER, so we override default Vaadin behavior
                case Event.ONKEYUP:
                    if (isFocusing) {
                        if (event.getKeyCode() == 32 /* space */) {
                            isFocusing = false;
                            onClick();
                            event.stopPropagation();
                            event.preventDefault();
                            return true;
                        } else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                            isFocusing = false;
                            onClick();
                            event.stopPropagation();
                            event.preventDefault();
                            return true;
                        }
                    }
                    break;
            }
        }

        return false;
    }
}