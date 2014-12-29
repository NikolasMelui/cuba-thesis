/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter.condition;

import com.google.common.base.Strings;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.filter.operationedit.AbstractOperationEditor;
import com.haulmont.cuba.gui.components.filter.Op;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.descriptor.AbstractConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.operationedit.PropertyOperationEditor;
import com.haulmont.cuba.gui.data.Datasource;
import org.apache.commons.lang.ObjectUtils;
import org.dom4j.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devyatkin
 * @version $Id$
 */
@MetaClass(name = "sec$PropertyCondition")
@SystemLevel
public class PropertyCondition extends AbstractCondition {

    private static Pattern PATTERN = Pattern.compile("\\s*(\\S+)\\s+((?:not\\s+)*\\S+)\\s+(\\S+)\\s*");
    private static Pattern PATTERN_NOT_IN = Pattern.compile("\\s*[(]\\s*[(]\\s*(\\S+)\\s+((:not\\s+)*\\S+)\\s+(\\S+)[\\S\\s]*");
    private static Pattern PATTERN_NULL = Pattern.compile("\\s*(\\S+)\\s+(is\\s+(?:not\\s+)?null)\\s*");

    public PropertyCondition(PropertyCondition condition) {
        super(condition);
        this.operator = condition.operator;
    }

    public PropertyCondition(Element element, String messagesPack, String filterComponentName, Datasource datasource) {
        super(element, messagesPack, filterComponentName, datasource);

        this.locCaption = FilterConditionUtils.getPropertyLocCaption(datasource.getMetaClass(), name);
        String text = element.getText();
        Matcher matcher = PATTERN_NULL.matcher(text);
        if (!matcher.matches()) {
            matcher = PATTERN_NOT_IN.matcher(text);
            if (!matcher.matches()) {
                matcher = PATTERN.matcher(text);
            }
            if (!matcher.matches()) {
                throw new IllegalStateException("Unable to build condition from: " + text);
            }
        }
        String operatorName = element.attributeValue("operatorType", null);
        if (operatorName != null) {
            operator = Op.valueOf(operatorName);
        } else {
            operator = Op.fromString(matcher.group(2));
        }

        String prop = matcher.group(1);
        entityAlias = prop.substring(0, prop.indexOf('.'));
    }

    @SuppressWarnings("unchecked")
    public PropertyCondition(AbstractConditionDescriptor descriptor, String entityAlias) {
        super(descriptor);
        this.entityAlias = entityAlias;
    }

    @Override
    protected Param createParam() {
        if (Strings.isNullOrEmpty(paramName)) {
            paramName = createParamName();
        }

        MetaProperty metaProperty = datasource.getMetaClass().getProperty(name);
        return new Param(paramName, javaClass, entityParamWhere, entityParamView,
                datasource, metaProperty, inExpr, required);
    }

    @Override
    protected void updateText() {
        StringBuilder sb = new StringBuilder();
        if (operator == Op.NOT_IN) {
            sb.append("((");
        }
        sb.append(entityAlias).append(".").append(name);

        if (Param.Type.ENTITY == param.getType()) {
            sb.append(".id");
        }

        sb.append(" ").append(operator.getText());

        if (!operator.isUnary()) {
            if (inExpr) {
                sb.append(" (");
            } else {
                sb.append(" ");
            }
            sb.append(":").append(param.getName());
            if (inExpr) {
                sb.append(")");
            }

            if (operator == Op.NOT_IN) {
                sb.append(") or (").append(entityAlias).append(".").append(name).append(" is null)) ");
            }
        }

        text = sb.toString();
    }

    public String getOperatorType() {
        return operator.name();
    }

    @Override
    public void toXml(Element element, Param.ValueProperty valueProperty) {
        super.toXml(element, valueProperty);
        element.addAttribute("type", ConditionType.PROPERTY.name());
        element.addAttribute("operatorType", getOperatorType());
    }

    @Override
    public void setOperator(Op operator) {
        if (!ObjectUtils.equals(this.operator, operator)) {
            this.operator = operator;
            String paramName = param.getName();

            if (operator.isUnary()) {
                unary = true;
                inExpr = false;
                setParam(new Param(paramName, null, null, null, null, false, required));
            } else {
                unary = false;
                inExpr = operator.equals(Op.IN) || operator.equals(Op.NOT_IN);
                setParam(new Param(
                        paramName, javaClass, entityParamWhere, entityParamView, datasource, param.getProperty(), inExpr, required));
            }
        }
    }

    @Override
    public String getOperationCaption() {
        Messages messages = AppBeans.get(Messages.NAME);
        return messages.getMessage(operator);
    }

    @Override
    public AbstractOperationEditor createOperationEditor() {
        operationEditor = new PropertyOperationEditor(this);
        return operationEditor;
    }

//    @Override
//    protected void copyFrom(AbstractCondition condition) {
//        super.copyFrom(condition);
//        if (condition instanceof PropertyCondition) {
//            this.operator = ((PropertyCondition) condition).operator;
//        }
//    }

    @Override
    public AbstractCondition createCopy() {
//        PropertyCondition propertyCondition = new PropertyCondition(this);
//        propertyCondition.copyFrom(this);
//        return propertyCondition;
        return new PropertyCondition(this);
    }
}