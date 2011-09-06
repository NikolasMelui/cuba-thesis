/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.gui.components.filter;

import com.haulmont.cuba.gui.components.filter.AbstractConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.AbstractOperationEditor;
import com.haulmont.cuba.gui.components.filter.AbstractRuntimePropCondition;
import com.haulmont.cuba.gui.components.filter.ParamFactory;
import com.haulmont.cuba.gui.data.Datasource;
import org.dom4j.Element;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class RuntimePropCondition extends AbstractRuntimePropCondition<Param> {

    private OperationEditor operationEditor;

    public RuntimePropCondition(AbstractConditionDescriptor descriptor, String where, String join, String entityAlias) {
        super(descriptor, where, join, entityAlias);
        paramFactory = new ParamFactoryImpl();
    }

    public RuntimePropCondition(Element element, String messagesPack, String filterComponentName, Datasource datasource) {
        super(element, messagesPack, filterComponentName, datasource);

    }

    @Override
    public OperationEditor createOperationEditor() {
        operationEditor = new RuntimePropOperationEditor(this);
        return operationEditor;
    }

    @Override
    public AbstractOperationEditor getOperationEditor() {
        return operationEditor;
    }

    @Override
    protected ParamFactory<Param> getParamFactory() {
        return new ParamFactoryImpl();
    }
}
