/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.desktop.gui.data.TreeTableModelAdapter;
import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import org.jdesktop.swingx.JXTreeTable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopTreeTable
    extends DesktopAbstractTable<JXTreeTable>
    implements TreeTable
{
    private String hierarchyProperty;

    protected Map<Integer,CellRenderer> cellRenderers = new HashMap<Integer, CellRenderer>();

    public DesktopTreeTable() {
        impl = new JXTreeTable() {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                CellRenderer cellRenderer = cellRenderers.get(column);
                if (cellRenderer != null)
                    return cellRenderer;
                else
                    return super.getCellRenderer(row, column);
            }

            @Override
            public TreeCellRenderer getTreeCellRenderer() {
                return super.getTreeCellRenderer();
            }
        };
        impl.setRootVisible(false);
        impl.setColumnControlVisible(true);
        impl.setEditable(false);
        DesktopComponentsHelper.correctTableFocusTraversal(impl);

        initComponent();

        tableSettings = new SwingXTableSettings(impl, columnsOrder);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        impl.setEditable(editable);
    }

    @Override
    protected void initTableModel(CollectionDatasource datasource) {
        tableModel = new TreeTableModelAdapter(
                impl,
                ((HierarchicalDatasource) datasource),
                columnsOrder,
                true
        );
        impl.setTreeTableModel(((TreeTableModelAdapter) tableModel));
    }

    @Override
    protected void initSelectionListener(final CollectionDatasource datasource) {
        impl.getTreeSelectionModel().addTreeSelectionListener(
                new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent e) {
                        Entity entity = getSingleSelected();
                        datasource.setItem(entity);
                    }
                }
        );
    }

    @Override
    public String getHierarchyProperty() {
        return hierarchyProperty;
    }

    @Override
    public void setDatasource(HierarchicalDatasource datasource) {
        setDatasource((CollectionDatasource) datasource);
        this.hierarchyProperty = datasource.getHierarchyPropertyName();
    }

    @Override
    public void expandAll() {
        TreeTableModelAdapter model = (TreeTableModelAdapter) tableModel;
        if (!model.isLeaf(model.getRoot())) {
            recursiveExpand(model.getRoot());
        }
        readjustColumns();
    }

    private void recursiveExpand(Object node) {
        TreeTableModelAdapter model = (TreeTableModelAdapter) tableModel;
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

        impl.expandPath(((TreeTableModelAdapter) tableModel).getTreePath(item));
    }

    @Override
    public void collapseAll() {
        if (tableModel == null)
            return;

        impl.collapsePath(new TreePath(((TreeTableModelAdapter) tableModel).getRoot()));
        readjustColumns();
    }

    @Override
    public void collapse(Object itemId) {
        if (datasource == null)
            return;
        Entity<Object> item = datasource.getItem(itemId);
        if (item == null)
            return;

        impl.collapsePath(((TreeTableModelAdapter) tableModel).getTreePath(item));
    }

    @Override
    public int getLevel(Object itemId) {
        Object parentId;
        if ((parentId = ((HierarchicalDatasource) datasource).getParent(itemId)) == null) {
            return 0;
        }
        return getLevel(parentId) + 1;
    }

    @Override
    public boolean isExpanded(Object itemId) {
        if (datasource == null)
            return false;
        Entity<Object> item = datasource.getItem(itemId);
        if (item == null)
            return false;

        return impl.isExpanded(((TreeTableModelAdapter) tableModel).getTreePath(item));
    }

    @Override
    public Set getSelected() {
        Set selected = new HashSet();
        TreePath[] selectionPaths = impl.getTreeSelectionModel().getSelectionPaths();
        if (selectionPaths != null) {
            for (TreePath path : selectionPaths) {
                Entity entity = ((TreeTableModelAdapter) tableModel).getEntity(path.getLastPathComponent());
                if (entity != null)
                    selected.add(entity);
            }
        }
        return selected;
    }

    @Override
    public void setSelected(Entity item) {
        TreePath treePath = ((TreeTableModelAdapter) tableModel).getTreePath(item);
        impl.getTreeSelectionModel().setSelectionPath(treePath);
    }

    @Override
    public void setSelected(Collection<Entity> items) {
        for (Entity item : items) {
            TreePath treePath = ((TreeTableModelAdapter) tableModel).getTreePath(item);
            impl.getTreeSelectionModel().addSelectionPath(treePath);
        }
    }

    @Override
    public void packRows() {
        // not supported on JXTreeTable
    }

    @Override
    public void setSortable(boolean sortable) {
        super.setSortable(sortable);
        impl.setSortable(sortable);
    }

    @Override
    public void addGeneratedColumn(String columnId, ColumnGenerator generator) {
        if (columnId == null)
            throw new IllegalArgumentException("columnId is null");
        if (generator == null)
            throw new IllegalArgumentException("generator is null");

        Column col = getColumn(columnId);
        tableModel.addGeneratedColumn(col);
        TableColumnModel columnModel = impl.getColumnModel();
        int columnIndex = columnModel.getColumnIndex(col);
        if (columnIndex == 0)
            throw new UnsupportedOperationException("Unable to add cell renderer for hierarchical column in TreeTable");
        cellRenderers.put(columnIndex, new CellRenderer(generator));
    }

    protected class CellRenderer implements TableCellRenderer {

        private ColumnGenerator columnGenerator;

        public CellRenderer(ColumnGenerator generator) {
            this.columnGenerator = generator;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Entity item = tableModel.getItem(row);
            com.haulmont.cuba.gui.components.Component component = columnGenerator.generateCell(DesktopTreeTable.this, item.getId());
            Component comp;
            if (component == null)
                comp = new ComponentWrapper(new JLabel(""));
            else
                comp = new ComponentWrapper(DesktopComponentsHelper.getComposition(component));
            return comp;
        }
    }
}
