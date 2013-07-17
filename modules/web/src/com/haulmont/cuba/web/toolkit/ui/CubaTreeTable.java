/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.gui.data.PropertyValueStringify;
import com.haulmont.cuba.web.toolkit.data.TreeTableContainer;
import com.haulmont.cuba.web.toolkit.data.util.TreeTableContainerWrapper;
import com.haulmont.cuba.web.toolkit.ui.client.treetable.CubaTreeTableState;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;

import java.util.Collection;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaTreeTable extends com.vaadin.ui.TreeTable implements TreeTableContainer {

    protected LinkedList<Object> editableColumns = null;

    @Override
    protected CubaTreeTableState getState() {
        return (CubaTreeTableState) super.getState();
    }

    @Override
    protected CubaTreeTableState getState(boolean markAsDirty) {
        return (CubaTreeTableState) super.getState(markAsDirty);
    }

    public boolean isTextSelectionEnabled() {
        return getState(false).textSelectionEnabled;
    }

    public void setTextSelectionEnabled(boolean textSelectionEnabled) {
        if (isTextSelectionEnabled() != textSelectionEnabled) {
            getState(true).textSelectionEnabled = textSelectionEnabled;
        }
    }

    public boolean isAllowPopupMenu(){
        return getState(false).allowPopupMenu;
    }

    public void setAllowPopupMenu(boolean allowPopupMenu){
        if (isAllowPopupMenu() != allowPopupMenu)
            getState(true).allowPopupMenu = allowPopupMenu;
    }

    @Override
    public boolean isCaption(Object itemId) {
        return items instanceof TreeTableContainer
                && ((TreeTableContainer) items).isCaption(itemId);
    }

    @Override
    public String getCaption(Object itemId) {
        return ((TreeTableContainer) items).getCaption(itemId);
    }

    @Override
    public boolean setCaption(Object itemId, String caption) {
        return ((TreeTableContainer) items).setCaption(itemId, caption);
    }

    @Override
    public int getLevel(Object itemId) {
        return ((TreeTableContainer) items).getLevel(itemId);
    }

    public Object[] getEditableColumns() {
        if (editableColumns == null) {
            return null;
        }
        return editableColumns.toArray();
    }

    public void setEditableColumns(Object[] editableColumns) {
        checkNotNull(editableColumns, "You cannot set null as editable columns");

        if (this.editableColumns == null) {
            this.editableColumns = new LinkedList<>();
        } else {
            this.editableColumns.clear();
        }

        final Collection properties = getContainerPropertyIds();
        for (final Object editableColumn : editableColumns) {
            if (editableColumn == null) {
                throw new NullPointerException("Ids must be non-nulls");
            } else if (!properties.contains(editableColumn)
                    || columnGenerators.containsKey(editableColumn)) {
                throw new IllegalArgumentException(
                        "Ids must exist in the Container and it must be not a generated column, incorrect id: "
                                + editableColumn);
            }
            this.editableColumns.add(editableColumn);
        }

        refreshRowCache();
    }

    @Override
    protected boolean isColumnEditable(Object columnId, boolean editable) {
        return editable &&
                editableColumns != null && editableColumns.contains(columnId);
    }

    @Override
    public void addGeneratedColumn(Object id, ColumnGenerator generatedColumn) {
        if (generatedColumn == null) {
            throw new IllegalArgumentException(
                    "Can not add null as a GeneratedColumn");
        }
        if (columnGenerators.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Can not add the same GeneratedColumn twice, id:" + id);
        } else {
            columnGenerators.put(id, generatedColumn);
            /*
             * add to visible column list unless already there (overriding
             * column from DS)
             */
            if (!visibleColumns.contains(id)) {
                visibleColumns.add(id);
            }

            if (editableColumns != null) {
                editableColumns.remove(id);
            }

            refreshRowCache();
        }
    }

    @Override
    protected String formatPropertyValue(Object rowId, Object colId, Property<?> property) {
        if (property instanceof PropertyValueStringify)
            return ((PropertyValueStringify) property).getFormattedValue();

        return super.formatPropertyValue(rowId, colId, property);
    }

    @Override
    public void setContainerDataSource(Container newDataSource) {disableContentRefreshing();
        if (newDataSource == null) {
            newDataSource = new HierarchicalContainer();
        }

        super.setContainerDataSource(new TreeTableContainerWrapper(newDataSource));
    }

    public void expandAll() {
        for (Object id : getItemIds())
            setCollapsed(id, false);
    }

    public void collapseAll() {
        for (Object id : getItemIds())
            setCollapsed(id, true);
    }

    public void setExpanded(Object itemId) {
        setCollapsed(itemId, false);
    }

    public boolean isExpanded(Object itemId) {
        return !isCollapsed(itemId);
    }
}