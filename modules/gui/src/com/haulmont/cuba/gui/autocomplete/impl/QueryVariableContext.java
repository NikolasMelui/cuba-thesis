package com.haulmont.cuba.jpql.impl;

import com.haulmont.cuba.jpql.impl.model.Entity;
import com.haulmont.cuba.jpql.impl.model.VirtualEntity;
import com.haulmont.cuba.jpql.impl.tree.QueryNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Alexander Chevelev
 * Date: 20.10.2010
 * Time: 19:29:29
 */
public class QueryVariableContext {
    private Map<String, Entity> entityVariableName2entity = new HashMap<String, Entity>();
    private QueryNode node;
    private List<QueryVariableContext> children = new ArrayList<QueryVariableContext>();
    private Entity entity;
    private boolean propagateVariablesUpstairs = true;
    private QueryVariableContext parent = null;

    QueryVariableContext(DomainModel model, QueryNode node) {
        if (model == null)
            throw new NullPointerException("No model passed");
        if (node == null)
            throw new NullPointerException("No node passed");

        this.node = node;
        this.entity = new VirtualEntity();
        model.add(entity);
    }

    public boolean isPropagateVariablesUpstairs() {
        return propagateVariablesUpstairs;
    }

    public void setPropateVariablesUp(boolean propateVariablesUpdatairs) {
        this.propagateVariablesUpstairs = propateVariablesUpdatairs;
    }

    public Entity getEntityByVariableName(String entityVariableName) {
        Entity result = entityVariableName2entity.get(entityVariableName);
        if (result != null) {
            return result;
        }

        return parent == null ? null : parent.getEntityByVariableName(entityVariableName);
    }

    /**
     * Internal method to register entity variables found in query
     *
     * @param variableName - found entity variable name
     * @param entity
     */
    public void addEntityVariable(String variableName, Entity entity) {
        if (variableName == null) {
            throw new NullPointerException("No entity variable name passed");
        }
        if (entity == null) {
            throw new NullPointerException("No entity passed");
        }
        entityVariableName2entity.put(variableName, entity);
    }

    public QueryVariableContext getContextByCaretPosition(int caretPosition) {
        if (!node.contains(caretPosition)) {
            return null;
        }

        for (QueryVariableContext child : children) {
            QueryVariableContext childResult = child.getContextByCaretPosition(caretPosition);
            if (childResult != null)
                return childResult;
        }
        return this;

    }

    public void addChild(QueryVariableContext child) {
        if (child == null) {
            throw new NullPointerException("No child passed");
        }
        if (child.getParent() != null) {
            throw new IllegalArgumentException("Child has parent already");
        }
        child.setParent(this);
        children.add(child);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    private QueryVariableContext getParent() {
        return parent;
    }

    private void setParent(QueryVariableContext parent) {
        this.parent = parent;
    }
}
