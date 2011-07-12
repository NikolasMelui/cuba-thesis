/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components.filter;

import com.haulmont.cuba.gui.components.filter.AbstractCondition;
import com.haulmont.cuba.gui.components.filter.AbstractFilterParser;
import com.haulmont.cuba.gui.components.filter.ConditionType;
import com.haulmont.cuba.gui.data.Datasource;
import org.dom4j.Element;

import java.util.List;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class FilterParser extends AbstractFilterParser {
    public FilterParser(List<AbstractCondition> conditions, String messagesPack, String filterComponentName, Datasource datasource) {
        super(conditions, messagesPack, filterComponentName, datasource);
    }

    public FilterParser(String xml, String messagesPack, String filterComponentName, Datasource datasource) {
        super(xml, messagesPack, filterComponentName, datasource);
    }

    @Override
    protected AbstractCondition createCondition(ConditionType type, Element element) {
        switch (type) {
            case PROPERTY:
                return new PropertyCondition(element, messagesPack, filterComponentName, datasource);
            case CUSTOM:
                return new CustomCondition(element, messagesPack, filterComponentName, datasource);
            case RUNTIME_PROPERTY:
                return new RuntimePropCondition(element, messagesPack, filterComponentName, datasource);
            default:
                throw new IllegalStateException("Unknown condition type: " + type + " in " + xml);
        }
    }
}
