/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.ui.client.resizabletextarea.CubaResizableTextAreaState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaResizableTextArea extends CubaTextArea {

    protected boolean resizable = false;
    protected List<ResizeListener> listeners = new ArrayList<>();

    public static interface ResizeListener {
        public void onResize(String oldWidth, String oldHeight, String width, String height);
    }

    @Override
    protected CubaResizableTextAreaState getState() {
        return (CubaResizableTextAreaState) super.getState();
    }

    @Override
    protected CubaResizableTextAreaState getState(boolean markAsDirty) {
        return (CubaResizableTextAreaState) super.getState(markAsDirty);
    }

    public boolean isResizable() {
        return getState(false).resizable;
    }

    public void setResizable(boolean resizable) {
        getState().resizable = resizable;
    }


    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);

        if (getState(false).rows > 0 && getState(false).columns > 0) {
            // TextArea with fixed rows or cols can not be resizable
            getState().resizable = false;
        }
    }

    public void addResizeListener(ResizeListener resizeListener) {
        if (!listeners.contains(resizeListener))
            listeners.add(resizeListener);
    }

    public void removeResizeListener(ResizeListener resizeListener) {
        listeners.remove(resizeListener);
    }
}