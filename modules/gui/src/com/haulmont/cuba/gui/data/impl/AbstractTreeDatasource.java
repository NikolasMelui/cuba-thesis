/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.data.impl;

import com.haulmont.bali.datastruct.Node;
import com.haulmont.bali.datastruct.Tree;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.gui.logging.UIPerformanceLogger;
import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.perf4j.StopWatch;
import org.perf4j.log4j.Log4JStopWatch;

import java.util.*;

/**
 * @param <T> Entity
 * @param <K> Key
 *
 * @author abramov
 * @version $Id$
 */
public abstract class AbstractTreeDatasource<T extends Entity<K>, K>
        extends CollectionDatasourceImpl<T, K>
        implements HierarchicalDatasource<T, K> {

    protected Tree<T> tree;
    protected Map<K, Node<T>> nodes;

    @Override
    protected void loadData(Map<String, Object> params) {
        String tag = getLoggingTag("TDS");
        StopWatch sw = new Log4JStopWatch(tag, Logger.getLogger(UIPerformanceLogger.class));

        this.tree = loadTree(params);

        Map<K, Node<T>> targetNodes = new HashMap<>();

        clear();
        if (tree != null) {
            for (Node<T> node : tree.toList()) {
                final T entity = node.getData();
                final K id = entity.getId();

                data.put(id, entity);
                attachListener(entity);

                targetNodes.put(id, node);
            }
        }

        this.nodes = targetNodes;

        sw.stop();
    }

    protected abstract Tree<T> loadTree(Map<String, Object> params);

    @Override
    public String getHierarchyPropertyName() {
        return null;
    }

    @Override
    public void setHierarchyPropertyName(String parentPropertyName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<K> getRootItemIds() {
        if (State.NOT_INITIALIZED.equals(state)) {
            return Collections.emptyList();
        } else {
            if (tree == null) return Collections.emptyList();

            List ids = new ArrayList();
            for (Node<T> rootNode : tree.getRootNodes()) {
                ids.add(rootNode.getData().getId());
            }
            return (Collection<K>) Collections.unmodifiableCollection(ids);
        }
    }

    @Override
    public K getParent(K itemId) {
        final Node<T> node = nodes.get(itemId);
        return node == null ? null : node.getParent() == null ? null : node.getParent().getData().getId();
    }

    @Override
    public Collection<K> getChildren(K itemId) {
        final Node<T> node = nodes.get(itemId);
        if (node == null)
            return Collections.emptyList();
        else {
            final List<Node<T>> children = node.getChildren();

            final List<K> ids = new ArrayList<>();
            for (Node<T> targetNode : children) {
                ids.add(targetNode.getData().getId());
            }

            return ids;
        }
    }

    @Override
    public boolean isRoot(K itemId) {
        final Node<T> node = nodes.get(itemId);

        for (Node<T> tNode : tree.getRootNodes()) {
            if (ObjectUtils.equals(tNode, node)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasChildren(K itemId) {
        final Node<T> node = nodes.get(itemId);
        return node != null && !node.getChildren().isEmpty();
    }

    @Override
    public boolean canHasChildren(K itemId) {
        return true;
    }
}