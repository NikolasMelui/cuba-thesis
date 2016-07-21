/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter.edit;

import com.google.common.base.Strings;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributes;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.Category;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.filter.ConditionParamBuilder;
import com.haulmont.cuba.gui.components.filter.Op;
import com.haulmont.cuba.gui.components.filter.OpManager;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.condition.DynamicAttributesCondition;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author devyatkin
 * @version $Id$
 */
public class DynamicAttributesConditionFrame extends ConditionFrame<DynamicAttributesCondition> {

    @Inject
    protected ThemeConstants themeConstants;

    @Inject
    protected DataService dataService;

    @Inject
    protected Metadata metadata;

    @Inject
    protected LookupField categoryLookup;

    @Inject
    protected LookupField attributeLookup;

    @Inject
    protected LookupField operationLookup;

    @Inject
    protected Label categoryLabel;

    @Inject
    protected MessageTools messageTools;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        categoryLookup.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (value != null)
                    fillAttributeSelect((Category) value);
            }
        });

        attributeLookup.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (value != null)
                    fillOperationSelect(DynamicAttributesUtils.getAttributeClass((CategoryAttribute) value));
            }
        });
    }

    @Override
    public void setCondition(DynamicAttributesCondition condition) {
        super.setCondition(condition);
        fillCategorySelect();
    }

    protected String checkCondition() {
        if (categoryLookup.getValue() == null) {
            return "DynamicAttributesConditionFrame.selectCategory";
        }
        if (attributeLookup.getValue() == null) {
            return "DynamicAttributesConditionFrame.selectAttribute";
        }
        if (operationLookup.getValue() == null) {
            return "DynamicAttributesConditionFrame.selectOperator";
        }
        return null;
    }

    @Override
    public boolean commit() {
        if (!super.commit())
            return false;

        String error = checkCondition();
        if (error != null) {
            showNotification(getMessage(error), IFrame.NotificationType.TRAY);
            return false;
        }

        CategoryAttribute attribute = attributeLookup.getValue();

        String alias = condition.getEntityAlias();
        String cavAlias = "cav" + RandomStringUtils.randomNumeric(5);

        String paramName;
        String operation = operationLookup.<Op>getValue().getText();
        Op op = operationLookup.getValue();

        Class javaClass = DynamicAttributesUtils.getAttributeClass(attribute);
        String propertyPath = Strings.isNullOrEmpty(condition.getPropertyPath()) ? "" : "." + condition.getPropertyPath();
        ConditionParamBuilder paramBuilder = AppBeans.get(ConditionParamBuilder.class);
        paramName = paramBuilder.createParamName(condition);

        String where;
        if (op == Op.NOT_EMPTY) {
            where = "(exists (select " + cavAlias + " from sys$CategoryAttributeValue " + cavAlias +
                    " where " + cavAlias + ".entityId=" +
                    alias +
                    propertyPath +
                    ".id and " + cavAlias + ".categoryAttribute.id='" +
                    attributeLookup.<CategoryAttribute>getValue().getId() + "'))";
        } else {
            condition.setJoin(", sys$CategoryAttributeValue " + cavAlias + " ");
            String valueFieldName = "stringValue";
            if (Entity.class.isAssignableFrom(javaClass))
                valueFieldName = "entityValue";
            else if (String.class.isAssignableFrom(javaClass))
                valueFieldName = "stringValue";
            else if (Integer.class.isAssignableFrom(javaClass))
                valueFieldName = "intValue";
            else if (Double.class.isAssignableFrom(javaClass))
                valueFieldName = "doubleValue";
            else if (Boolean.class.isAssignableFrom(javaClass))
                valueFieldName = "booleanValue";
            else if (Date.class.isAssignableFrom(javaClass))
                valueFieldName = "dateValue";

            String paramStr = " ? ";
            if (!op.isUnary())
                if (Op.IN.equals(op) || Op.NOT_IN.equals(op))
                    paramStr = " ( ? ) ";

            where = cavAlias + ".entityId=" +
                    alias +
                    propertyPath +
                    ".id and " + cavAlias + "." +
                    valueFieldName +
                    " " +
                    operation +
                    (op.isUnary() ? " " : paramStr) + "and " + cavAlias + ".categoryAttribute.id='" +
                    attributeLookup.<CategoryAttribute>getValue().getId() + "'";
            where = where.replace("?", ":" + paramName);
        }

        condition.setWhere(where);
        condition.setUnary(op.isUnary());
        condition.setEntityParamView(null);
        condition.setEntityParamWhere(null);
        condition.setInExpr(Op.IN.equals(op) || Op.NOT_IN.equals(op));
        condition.setOperator(operationLookup.<Op>getValue());
        condition.setJavaClass(javaClass);
        condition.setCategoryId(categoryLookup.<Category>getValue().getId());
        condition.setCategoryAttributeId(attributeLookup.<CategoryAttribute>getValue().getId());
        condition.setLocCaption(attribute.getName());

        Class paramJavaClass = op.isUnary() ? Boolean.class : javaClass;
        Param param = Param.Builder.getInstance()
                .setName(paramName)
                .setJavaClass(paramJavaClass)
                .setDataSource(condition.getDatasource())
                .setProperty(DynamicAttributesUtils.getMetaPropertyPath(null, attribute).getMetaProperty())
                .setInExpr(condition.getInExpr())
                .setRequired(condition.getRequired())
                .setCategoryAttrId(attribute.getId())
                .build();

        Object defaultValue = condition.getParam().getDefaultValue();
        param.setDefaultValue(defaultValue);

        condition.setParam(param);

        return true;
    }

    protected void fillCategorySelect() {
        DynamicAttributes dynamicAttributes = AppBeans.get(DynamicAttributes.NAME);
        MetaClass metaClass = condition.getDatasource().getMetaClass();
        if (!Strings.isNullOrEmpty(condition.getPropertyPath())) {
            MetaPropertyPath propertyPath = metaClass.getPropertyPath(condition.getPropertyPath());
            if (propertyPath == null) {
                throw new RuntimeException("Property path " + condition.getPropertyPath() + " doesn't exist");
            }
            metaClass = propertyPath.getRange().asClass();

        }
        Collection<Category> categories = dynamicAttributes.getCategoriesForMetaClass(metaClass);
        UUID catId = condition.getCategoryId();
        Category selectedCategory = null;
        Map<String, Object> categoriesMap = new TreeMap<>();
        if (categories.size() == 1 && (catId == null || ObjectUtils.equals(catId, categories.iterator().next().getId()))) {
            Category category = categories.iterator().next();
            categoryLookup.setVisible(false);
            categoryLabel.setVisible(false);
            attributeLookup.requestFocus();
            categoriesMap.put(category.getName(), category);
            categoryLookup.setOptionsMap(categoriesMap);
            categoryLookup.setValue(category);
            fillAttributeSelect(category);
        } else {
            categoryLookup.setVisible(true);
            categoryLabel.setVisible(true);
            for (Category category : categories) {
                categoriesMap.put(category.getName(), category);
                if (category.getId().equals(catId)) {
                    selectedCategory = category;
                }
            }
            categoryLookup.setOptionsMap(categoriesMap);
            categoryLookup.setValue(selectedCategory);
        }
    }

    protected void fillAttributeSelect(Category category) {
        UUID attrId = condition.getCategoryAttributeId();
        CategoryAttribute selectedAttribute = null;
        Map<String, Object> attributesMap = new TreeMap<>();
        for (CategoryAttribute attribute : category.getCategoryAttrs()) {
            attributesMap.put(attribute.getName(), attribute);
            if (attribute.getId().equals(attrId)) {
                selectedAttribute = attribute;
            }
        }
        attributeLookup.setOptionsMap(attributesMap);
        attributeLookup.setValue(selectedAttribute);
    }

    protected void fillOperationSelect(Class clazz) {
        OpManager opManager = AppBeans.get(OpManager.class);
        List<Op> ops = new LinkedList<>(opManager.availableOps(clazz));
        operationLookup.setOptionsList(ops);
        Op operator = condition.getOperator();
        if (operator != null) {
            operationLookup.setValue(operator);
        }
    }
}
