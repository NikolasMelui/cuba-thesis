/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.desktop.gui.data.TreeModelAdapter;
import com.haulmont.cuba.gui.components.CaptionMode;
import com.haulmont.cuba.gui.components.ShowInfoAction;
import com.haulmont.cuba.gui.components.Tree;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsActionsNotifier;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopTree
    extends DesktopAbstractActionsHolderComponent<JTree>
    implements Tree
{
    protected String hierarchyProperty;
    protected HierarchicalDatasource<Entity<Object>, Object> datasource;
    private JScrollPane treeView;
    private CaptionMode captionMode = CaptionMode.ITEM;
    private String captionProperty;
    protected TreeModelAdapter model;

    public DesktopTree() {
        impl = new JTree();
        treeView = new JScrollPane(impl);

        impl.setRootVisible(false);
        impl.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        impl.setExpandsSelectedPaths(true);
    }

    @Override
    public JComponent getComposition() {
        return treeView;
    }

    @Override
    public void expandTree() {
        if (model == null)
            return;

        if (!model.isLeaf(model.getRoot())) {
            recursiveExpand(model.getRoot());
        }
    }

    private void recursiveExpand(Object node) {
        impl.expandPath(model.getTreePath(node));
        for (int i = 0; i < model.getChildCount(node); i++) {
            Object child = model.getChild(node, i);
            if (!model.isLeaf(child)) {
                impl.expandPath(model.getTreePath(child));
                recursiveExpand(child);
            }
        }
    }

    @Override
    public void expand(Object itemId) {
        if (datasource == null)
            return;
        Entity<Object> item = datasource.getItem(itemId);
        if (item == null)
            return;

        impl.expandPath(model.getTreePath(item));
    }

    @Override
    public void collapseTree() {
        if (model == null)
            return;

        impl.collapsePath(new TreePath(model.getRoot()));
    }

    @Override
    public void collapse(Object itemId) {
        if (datasource == null)
            return;
        Entity<Object> item = datasource.getItem(itemId);
        if (item == null)
            return;

        impl.collapsePath(model.getTreePath(item));
    }

    @Override
    public boolean isExpanded(Object itemId) {
        if (datasource == null)
            return false;
        Entity<Object> item = datasource.getItem(itemId);
        if (item == null)
            return false;

        return impl.isExpanded(model.getTreePath(item));
    }

    @Override
    public CaptionMode getCaptionMode() {
        return captionMode;
    }

    @Override
    public void setCaptionMode(CaptionMode captionMode) {
        this.captionMode = captionMode;
        if (model != null)
            model.setCaptionMode(captionMode);
    }

    @Override
    public String getCaptionProperty() {
        return captionProperty;
    }

    @Override
    public void setCaptionProperty(String captionProperty) {
        this.captionProperty = captionProperty;
        if (model != null)
            model.setCaptionProperty(captionProperty);
    }

    @Override
    public String getHierarchyProperty() {
        return hierarchyProperty;
    }

    @Override
    public void setDatasource(HierarchicalDatasource datasource) {
        this.datasource = datasource;
        hierarchyProperty = datasource.getHierarchyPropertyName();

        model = new TreeModelAdapter(datasource, captionMode, captionProperty, true);
        impl.setModel(model);

        impl.addTreeSelectionListener(new SelectionListener());

        if (UserSessionProvider.getUserSession().isSpecificPermitted(ShowInfoAction.ACTION_PERMISSION)) {
            ShowInfoAction action = (ShowInfoAction) getAction(ShowInfoAction.ACTION_ID);
            if (action == null) {
                action = new ShowInfoAction();
                addAction(action);
            }
            action.setDatasource(datasource);
        }

        datasource.addListener(new CollectionDsActionsNotifier(this));
    }

    @Override
    public boolean isMultiSelect() {
        return impl.getSelectionModel().getSelectionMode() != TreeSelectionModel.SINGLE_TREE_SELECTION;
    }

    @Override
    public void setMultiSelect(boolean multiselect) {
        impl.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    @Override
    public <T extends Entity> T getSingleSelected() {
        Set selected = getSelected();
        return selected.isEmpty() ? null : (T) selected.iterator().next();
    }

    @Override
    public <T extends Entity> Set<T> getSelected() {
        Set<T> selected = new HashSet<>();
        TreePath[] selectionPaths = impl.getSelectionPaths();
        if (selectionPaths != null) {
            for (TreePath selectionPath : selectionPaths) {
                Entity entity = model.getEntity(selectionPath.getLastPathComponent());
                if (entity != null)
                    selected.add((T) entity);
            }
        }
        return selected;
    }

    @Override
    public void setSelected(Entity item) {
        TreePath path = model.getTreePath(item);
        impl.setSelectionPath(path);
    }

    @Override
    public void setSelected(Collection<Entity> items) {
        TreePath[] paths = new TreePath[items.size()];
        int i = 0;
        for (Entity item : items) {
            paths[i] = model.getTreePath(item);
        }
        impl.setSelectionPaths(paths);
    }

    @Override
    public CollectionDatasource getDatasource() {
        return datasource;
    }

    @Override
    public void refresh() {
        datasource.refresh();
    }

    private class SelectionListener implements TreeSelectionListener {

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            Set selected = getSelected();
            if (selected.isEmpty()) {
                datasource.setItem(null);
            } else {
                Object item = selected.iterator().next();
                if (item instanceof Entity) {
                    datasource.setItem((Entity) item);
                } else {
                    datasource.setItem(null);
                }
            }
        }
    }
}
