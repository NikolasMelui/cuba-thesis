/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.gui.data.Datasource;
import org.apache.commons.lang.ObjectUtils;
import org.dom4j.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public abstract class AbstractPropertyCondition<T extends AbstractParam> extends AbstractCondition<T> {

    private static Pattern PATTERN = Pattern.compile("\\s*(\\S+)\\s+((?:not\\s+)*\\S+)\\s+(\\S+)\\s*");
    private static Pattern PATTERN_NOT_IN = Pattern.compile("\\s*[(]\\s*[(]\\s*(\\S+)\\s+((:not\\s+)*\\S+)\\s+(\\S+)[\\S\\s]*");
    private static Pattern PATTERN_NULL = Pattern.compile("\\s*(\\S+)\\s+(is\\s+(?:not\\s+)?null)\\s*");

    private Op operator;

    public AbstractPropertyCondition(Element element, String messagesPack, String filterComponentName, Datasource datasource) {
        super(element, filterComponentName, datasource);

        String propertyPath = datasource.getMetaClass().getJavaClass().getSimpleName() + "." + name;
        this.locCaption = MessageProvider.getMessage(datasource.getMetaClass().getJavaClass(), propertyPath);
        if (locCaption == null || locCaption.equals(propertyPath))
            this.locCaption = AppBeans.get(MessageTools.class).loadString(messagesPack, caption);

        String text = element.getText();
        Matcher matcher = PATTERN_NULL.matcher(text);
        if (!matcher.matches()) {
            matcher = PATTERN_NOT_IN.matcher(text);
            if (!matcher.matches())
                matcher = PATTERN.matcher(text);
            if (!matcher.matches())
                throw new IllegalStateException("Unable to build condition from: " + text);
        }
        String operatorName = element.attributeValue("operatorType", null);
        if (operatorName != null)
            operator = Op.valueOf(operatorName);
        else
            operator = Op.fromString(matcher.group(2));

        String prop = matcher.group(1);
        entityAlias = prop.substring(0, prop.indexOf('.'));
    }

    public AbstractPropertyCondition(AbstractConditionDescriptor descriptor, String entityAlias) {
        super(descriptor);
        this.entityAlias = entityAlias;
    }

    @Override
    protected T createParam(String paramName) {
        MetaProperty metaProperty = datasource.getMetaClass().getProperty(name);
        return paramFactory.createParam(paramName, javaClass, entityParamWhere, entityParamView,
                datasource, metaProperty, inExpr, required);
    }

    @Override
    protected void updateText() {
        StringBuilder sb = new StringBuilder();
        if (operator.equals(Op.NOT_IN)) {
            sb.append("((");
        }
        sb.append(entityAlias).append(".").append(name);

        if (AbstractParam.Type.ENTITY.equals(param.getType()))
            sb.append(".id");

        sb.append(" ").append(operator.getText());

        if (!operator.isUnary()) {
            if (inExpr)
                sb.append(" (");
            else
                sb.append(" ");
            sb.append(":").append(param.getName());
            if (inExpr)
                sb.append(")");

            if (operator.equals(Op.NOT_IN)) {
                sb.append(") or (" + entityAlias + "." + name + " is null)) ");
            }
        }

        text = sb.toString();
    }

    public String getOperatorType() {
        return operator.name();
    }

    @Override
    public void toXml(Element element) {
        super.toXml(element);
        element.addAttribute("type", ConditionType.PROPERTY.name());
        element.addAttribute("operatorType", getOperatorType());

    }

    public Op getOperator() {
        return operator;
    }

    public void setOperator(Op operator) {
        if (!ObjectUtils.equals(this.operator, operator)) {
            this.operator = operator;
            String paramName = param.getName();

            if (operator.isUnary()) {
                unary = true;
                inExpr = false;
                setParam(paramFactory.createParam(paramName, null, null, null, null, false, required));
            } else {
                unary = false;
                inExpr = operator.equals(Op.IN) || operator.equals(Op.NOT_IN);
                setParam(paramFactory.createParam(
                        paramName, javaClass, entityParamWhere, entityParamView, datasource, param.getProperty(), inExpr, required));
            }
        }
    }

    @Override
    public String getError() {
        String res = super.getError();
        if (res != null)
            return res;

        if (operator == null)
            return locCaption + ": " + MessageProvider.getMessage(MESSAGES_PACK, "PropertyCondition.operatorNotDefined");
        else
            return null;
    }

    @Override
    public String getOperationCaption() {
        return MessageProvider.getMessage(operator);
    }

}
