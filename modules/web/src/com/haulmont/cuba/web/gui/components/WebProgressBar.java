/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.ProgressBar;
import com.vaadin.ui.ProgressIndicator;

/**
 * Web realization of progress bar depending on vaadin {@link ProgressIndicator} component.
 * <p/>
 * Note that indeterminate bar implemented here just like as determinate, but with fixed 0.0 value
 * <p/>
 * <p>$Id$</p>
 *
 * @author Alexander Budarov
 */
public class WebProgressBar extends WebAbstractField<ProgressIndicator> implements ProgressBar {
    private static final long serialVersionUID = 6339983078728788950L;

    protected boolean indeterminate;

    public WebProgressBar() {
        component = new ProgressIndicator();
        attachListener(component);
        component.setImmediate(true);
        component.setInvalidCommitted(true);
        component.setIndeterminate(false);
    }

    @Override
    public boolean isIndeterminate() {
        return indeterminate;
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (indeterminate) {
            component.setValue(0.0f);
        }
    }
}
