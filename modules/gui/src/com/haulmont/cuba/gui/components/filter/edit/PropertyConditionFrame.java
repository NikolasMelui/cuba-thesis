/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter.edit;

import com.haulmont.cuba.gui.components.BoxLayout;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.filter.condition.PropertyCondition;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author gorbunkov
 * @version $Id$
 */
public class PropertyConditionFrame extends ConditionFrame<PropertyCondition> {

    @Inject
    protected BoxLayout operationLayout;

    protected Component operationComponent;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
    }

    public void setCondition(PropertyCondition condition) {
        super.setCondition(condition);
        if (operationComponent != null)
            operationLayout.remove(operationComponent);
        operationComponent = condition.createOperationEditor().getComponent();
        operationLayout.add(operationComponent);
    }

    @Override
    public boolean commit() {
        if (!super.commit())
            return false;
        return true;
    }
}
