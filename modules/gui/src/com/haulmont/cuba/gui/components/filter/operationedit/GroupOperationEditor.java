/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter.operationedit;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.gui.components.BoxLayout;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.filter.condition.AbstractCondition;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;

/**
 * Grouping condition operation editor. Actually does nothing.
 *
 * @author krivopustov
 * @version $Id$
 */
public class GroupOperationEditor extends AbstractOperationEditor {

    protected ComponentsFactory componentsFactory = AppBeans.get(ComponentsFactory.class);

    public GroupOperationEditor(AbstractCondition condition) {
        super(condition);
    }

    @Override
    protected Component createComponent() {
        return componentsFactory.createComponent(BoxLayout.VBOX);
    }
}
