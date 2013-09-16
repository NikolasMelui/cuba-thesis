/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.web.toolkit.ui.CubaHorizontalActionsLayout;

/**
 * @author abramov
 * @version $Id$
 */
public class WebHBoxLayout extends WebAbstractBox {

    public WebHBoxLayout() {
        component = new CubaHorizontalActionsLayout();
    }
}