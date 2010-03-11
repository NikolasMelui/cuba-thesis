/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.09.2009 12:09:22
 *
 * $Id$
 */
package com.haulmont.cuba.gui.filter;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.cuba.core.global.QueryTransformer;
import com.haulmont.cuba.core.global.QueryTransformerFactory;
import com.haulmont.cuba.gui.xml.ParameterInfo;
import com.haulmont.cuba.gui.xml.ParametersHelper;
import org.dom4j.Element;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

import java.util.*;

public class QueryFilter {

    private final Condition root;
    private final String targetEntity;

    public QueryFilter(Element element, String targetEntity) {
        this.targetEntity = targetEntity;
        if (element.elements().isEmpty())
            throw new IllegalArgumentException("filter element is empty");
        Element rootElem = (Element) element.elements().get(0);
        root = createCondition(rootElem);
        parse(rootElem, root.getConditions());
    }

    public QueryFilter(QueryFilter src1, QueryFilter src2) {
        if (src1 == null || src2 == null)
            throw new IllegalArgumentException("Source query filter is null");

        if (!src1.targetEntity.equals(src2.targetEntity))
            throw new IllegalArgumentException("Target entities do not match");

        targetEntity = src1.targetEntity;

        root = new LogicalCondition(LogicalOp.AND);
        root.getConditions().add(src1.root);
        root.getConditions().add(src2.root);
    }

    private Condition createCondition(Element element) {
        Condition condition;

        if ("c".equals(element.getName())) {
            condition = new Clause(element.getText(), element.attributeValue("join"));
            // support unary conditions without parameters in text (e.g. "is null")
            for (Element paramElem : Dom4j.elements(element, "param")) {
                Set<ParameterInfo> params = ParametersHelper.parseQuery(":" + paramElem.attributeValue("name"));
                condition.getParameters().addAll(params);
            }
        } else {
            condition = new LogicalCondition(LogicalOp.fromString(element.getName()));
        }

        return condition;
    }

    private void parse(Element parentElem, List<Condition> conditions) {
        for (Element element : Dom4j.elements(parentElem)) {
            if ("param".equals(element.getName())) 
                continue;

            Condition condition = createCondition(element);
            conditions.add(condition);
            parse(element, condition.getConditions());
        }
    }

    public Collection<ParameterInfo> getParameters() {
        return root.getParameters();
    }

    public String processQuery(String query, Map<String, Object> paramValues) {
        Set<String> params = new HashSet<String>();
        for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
            if (paramValueIsOk(entry.getValue()))
                params.add(entry.getKey());
        }

        QueryTransformer transformer = QueryTransformerFactory.createTransformer(query, targetEntity);

        if (isActual(root, params)) {
            Condition refined = refine(root, params);
            String where = refined.getContent();

            if (!StringUtils.isBlank(where)) {
                Set<String> joins = refined.getJoins();
                if (!joins.isEmpty()) {
                    String joinsStr = new StrBuilder().appendAll(joins).toString();
                    transformer.addJoinAsIs(joinsStr);
                }
                transformer.addWhereAsIs(where);
            }
        }
        return transformer.getResult();
    }

    private boolean paramValueIsOk(Object value) {
        if (value instanceof String)
            return !StringUtils.isBlank((String) value);
        else if (value instanceof Number)
            return ((Number) value).intValue() != 0;
        else
            return value != null;
    }

    private Condition refine(Condition src, Set<String> params) {
        Condition copy = src.copy();
        List<Condition> list = new ArrayList<Condition>();
        for (Condition condition : src.getConditions()) {
            if (isActual(condition, params)) {
                list.add(refine(condition, params));
            }
        }
        copy.setConditions(list.isEmpty() ? Collections.EMPTY_LIST : list);
        return copy;
    }

    private boolean isActual(Condition condition, Set<String> params) {
        Set<ParameterInfo> declaredParams = condition.getParameters();

        if (declaredParams.isEmpty())
            return true;

        for (ParameterInfo paramInfo : declaredParams) {
            if (params.contains(paramInfo.getName()))
                return true;
        }

        return false;
    }
}
