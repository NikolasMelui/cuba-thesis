/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter.condition;

import com.google.common.base.Strings;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.annotation.SystemLevel;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.filter.ConditionParamBuilder;
import com.haulmont.cuba.gui.components.filter.Op;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.descriptor.AbstractConditionDescriptor;
import com.haulmont.cuba.gui.components.filter.operationedit.AbstractOperationEditor;
import com.haulmont.cuba.gui.components.filter.operationedit.DynamicAttributesOperationEditor;
import com.haulmont.cuba.gui.data.Datasource;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.dom4j.Element;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author devyatkin
 * @version $Id$
 */
@MetaClass(name = "sec$DynamicAttributesCondition")
@SystemLevel
public class DynamicAttributesCondition extends AbstractCondition {

    protected UUID categoryId;
    protected UUID categoryAttributeId;
    protected String propertyPath;
    protected String join;
    private static Pattern LIKE_PATTERN = Pattern.compile("(like \\S+)\\s+(?!ESCAPE)");

    public DynamicAttributesCondition(DynamicAttributesCondition condition) {
        super(condition);
        this.join = condition.getJoin();
        this.categoryId = condition.getCategoryId();
        this.categoryAttributeId = condition.getCategoryAttributeId();
    }

    public DynamicAttributesCondition(AbstractConditionDescriptor descriptor, String entityAlias, String propertyPath) {
        super(descriptor);
        this.entityAlias = entityAlias;
        this.name = RandomStringUtils.randomAlphabetic(10);
        Messages messages = AppBeans.get(Messages.class);
        this.locCaption = messages.getMessage(DynamicAttributesCondition.class, "newDynamicAttributeCondition");
        this.propertyPath = propertyPath;
    }

    public DynamicAttributesCondition(Element element, String messagesPack, String filterComponentName, Datasource datasource) {
        super(element, messagesPack, filterComponentName, datasource);

        propertyPath = element.attributeValue("propertyPath");

        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        locCaption = isBlank(caption)
                ? element.attributeValue("locCaption")
                : messageTools.loadString(messagesPack, caption);

        entityAlias = element.attributeValue("entityAlias");
        text = element.getText();
        join = element.attributeValue("join");
        categoryId = UUID.fromString(element.attributeValue("category"));
        String categoryAttributeValue = element.attributeValue("categoryAttribute");
        if (!Strings.isNullOrEmpty(categoryAttributeValue)) {
            categoryAttributeId = UUID.fromString(categoryAttributeValue);
        } else {
            //for backward compatibility
            List<Element> paramElements = Dom4j.elements(element, "param");
            for (Element paramElement : paramElements) {
                if (BooleanUtils.toBoolean(paramElement.attributeValue("hidden", "false"), "true", "false")) {
                    categoryAttributeId = UUID.fromString(paramElement.getText());
                    String paramName = paramElement.attributeValue("name");
                    text = text.replace(":" + paramName, "'" + categoryAttributeId + "'");
                }
            }
        }

        resolveParam(element);
    }

    @Override
    public void toXml(Element element, Param.ValueProperty valueProperty) {
        super.toXml(element, valueProperty);
        element.addAttribute("type", ConditionType.RUNTIME_PROPERTY.name());
        if (isBlank(caption)) {
            element.addAttribute("locCaption", locCaption);
        }
        element.addAttribute("category", categoryId.toString());
        element.addAttribute("categoryAttribute", categoryAttributeId.toString());
        element.addAttribute("entityAlias", entityAlias);
        if (!isBlank(propertyPath)) {
            element.addAttribute("propertyPath", propertyPath);
        }
        if (!isBlank(join)) {
            element.addAttribute("join", StringEscapeUtils.escapeXml(join));
        }
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID id) {
        categoryId = id;
    }

    public UUID getCategoryAttributeId() {
        return categoryAttributeId;
    }

    public void setCategoryAttributeId(UUID categoryAttributeId) {
        this.categoryAttributeId = categoryAttributeId;
    }

    @Override
    public void setOperator(Op operator) {
        if (!ObjectUtils.equals(this.operator, operator)) {
            this.operator = operator;
            String paramName = param.getName();
            ConditionParamBuilder paramBuilder = AppBeans.get(ConditionParamBuilder.class);
            if (operator.isUnary()) {
                unary = true;
                inExpr = false;
                Param param = Param.Builder.getInstance().setName(paramName)
                        .setJavaClass(Boolean.class)
                        .setInExpr(false)
                        .setRequired(required).build();
                setParam(param);
            } else {
                unary = false;
                inExpr = operator.equals(Op.IN) || operator.equals(Op.NOT_IN);
                Param param = paramBuilder.createParam(this);
                setParam(param);
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
        operationEditor = new DynamicAttributesOperationEditor(this);
        return operationEditor;
    }

//    @Override
//    protected Param createParam() {
//        if (categoryAttributeId != null) {
//            Class paramJavaClass = unary ? Boolean.class : javaClass;
//
//            MetaPropertyPath metaPropertyPath = DynamicAttributesUtils.getMetaPropertyPath(datasource.getMetaClass(), categoryAttributeId);
//            Param param = new Param(paramName, paramJavaClass, null, null, datasource,
//                    metaPropertyPath != null ? metaPropertyPath.getMetaProperty() : null,
//                    inExpr, required, categoryAttributeId);
//            return param;
//        } else {
//            return super.createParam();
//        }
//    }

    @Override
    protected void updateText() {
        if (operator == Op.NOT_EMPTY) {
            if (BooleanUtils.isTrue((Boolean) param.getValue())) {
                text = text.replace("not exists", "exists");
            } else if (BooleanUtils.isFalse((Boolean) param.getValue()) && !text.contains("not exists")) {
                text = text.replace("exists ", "not exists");
            }
        }

        if (operator == Op.ENDS_WITH || operator == Op.STARTS_WITH || operator == Op.CONTAINS || operator == Op.DOES_NOT_CONTAIN) {
            Matcher matcher = LIKE_PATTERN.matcher(text);
            if (matcher.find()) {
                String escapeCharacter = ("\\".equals(ESCAPE_CHARACTER) || "$".equals(ESCAPE_CHARACTER))
                        ? ESCAPE_CHARACTER + ESCAPE_CHARACTER
                        : ESCAPE_CHARACTER;
                text = matcher.replaceAll("$1 ESCAPE '" + escapeCharacter + "' ");
            }
        }
    }

    public String getJoin() {
        return join;
    }

    public void setJoin(String join) {
        this.join = join;
    }

    public String getWhere() {
        updateText();
        return text;
    }

    public void setWhere(String where) {
        this.text = where;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    @Override
    public AbstractCondition createCopy() {
        return new DynamicAttributesCondition(this);
    }

    @Override
    public String getLocCaption() {
        if (isBlank(caption) && !isBlank(propertyPath)) {
            MessageTools messageTools = AppBeans.get(MessageTools.class);
            String propertyCaption = messageTools.getPropertyCaption(datasource.getMetaClass(), propertyPath);
            if (!isBlank(propertyCaption)) {
                return propertyCaption + "." + locCaption;
            }
        }
        return super.getLocCaption();
    }

}