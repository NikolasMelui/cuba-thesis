/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components.filter;

import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.gui.components.filter.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.AbstractConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.ParamFactory;
import com.haulmont.cuba.gui.data.CollectionDatasource;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class ConditionCreator extends AbstractConditionDescriptor {

    public ConditionCreator(String filterComponentName, CollectionDatasource datasource) {
        super("creator", filterComponentName, datasource);
        locCaption = MessageProvider.getMessage(MESSAGES_PACK, "conditionCreator");
    }

    @Override
    public AbstractCondition createCondition() {
        return new NewCustomCondition(this, "", null, entityAlias);
    }

    @Override
    public Param createParam(AbstractCondition condition) {
        return null;
    }

    @Override
    protected ParamFactory getParamFactory() {
        return new ParamFactoryImpl();
    }

    @Override
    public Class getJavaClass() {
        return null;
    }

    @Override
    public String getEntityParamWhere() {
        return null;
    }

    @Override
    public String getEntityParamView() {
        return null;
    }
}
