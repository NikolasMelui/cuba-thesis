/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui.client.window;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.haulmont.cuba.web.toolkit.ui.client.appui.ValidationErrorHolder;
import com.vaadin.client.ui.VWindow;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaWindowWidget extends VWindow {

    public static final String MODAL_WINDOW_CLASSNAME = "v-window-modal";
    public static final String NONMODAL_WINDOW_CLASSNAME = "v-window-nonmodal";

    public interface ContextMenuHandler {
        void onContextMenu(Event event);
    }

    protected ContextMenuHandler contextMenuHandler;

    public CubaWindowWidget() {
        needFocusTopmostModalWindow = false;
        DOM.sinkEvents(header, DOM.getEventsSunk(header) | Event.ONCONTEXTMENU);
        addStyleName(NONMODAL_WINDOW_CLASSNAME);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (contextMenuHandler != null && event.getTypeInt() == Event.ONCONTEXTMENU) {
            contextMenuHandler.onContextMenu(event);
            return;
        }

        if ((event.getTypeInt() == Event.ONCLICK
                || event.getTypeInt() == Event.ONMOUSEDOWN)
                && event.getButton() != NativeEvent.BUTTON_LEFT) {
            event.preventDefault();
            event.stopPropagation();
            return;
        }

        super.onBrowserEvent(event);
    }

    @Override
    public void onKeyUp(KeyUpEvent event) {
        // disabled Vaadin close by ESCAPE #PL-4355
    }

    @Override
    protected void constructDOM() {
        super.constructDOM();

        DOM.sinkEvents(closeBox, Event.FOCUSEVENTS);
    }

    @Override
    protected void onCloseClick() {
        if (ValidationErrorHolder.hasValidationErrors()) {
            return;
        }

        super.onCloseClick();
    }



    @Override
    public void setVaadinModality(boolean modality) {
        super.setVaadinModality(modality);
        if (modality) {
            removeStyleName(NONMODAL_WINDOW_CLASSNAME);
            if (!getStyleName().contains(MODAL_WINDOW_CLASSNAME)) {
                addStyleName(MODAL_WINDOW_CLASSNAME);
            }
        } else {
            removeStyleName(MODAL_WINDOW_CLASSNAME);
            if (!getStyleName().contains(NONMODAL_WINDOW_CLASSNAME)) {
                addStyleName(NONMODAL_WINDOW_CLASSNAME);
            }
        }
    }
}