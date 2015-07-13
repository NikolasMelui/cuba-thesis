/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.google.common.collect.Iterables;
import com.haulmont.cuba.web.gui.components.presentations.TablePresentations;
import com.haulmont.cuba.web.gui.data.PropertyValueStringify;
import com.haulmont.cuba.web.toolkit.ShortcutActionManager;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.data.TableContainer;
import com.haulmont.cuba.web.toolkit.data.TreeTableContainer;
import com.haulmont.cuba.web.toolkit.data.util.TreeTableContainerWrapper;
import com.haulmont.cuba.web.toolkit.ui.client.table.CubaTableClientRpc;
import com.haulmont.cuba.web.toolkit.ui.client.table.CubaTableServerRpc;
import com.haulmont.cuba.web.toolkit.ui.client.treetable.CubaTreeTableState;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.Action;
import com.vaadin.event.ActionManager;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Layout;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author artamonov
 * @version $Id$
 */
public class CubaTreeTable extends com.vaadin.ui.TreeTable implements TreeTableContainer, CubaEnhancedTable {

    protected LinkedList<Object> editableColumns = null;

    /**
     * Keeps track of the ShortcutListeners added to this component, and manages the painting and handling as well.
     */
    protected ActionManager shortcutActionManager;

    protected boolean autowirePropertyDsForFields = false;

    protected boolean showTotalAggregation = true;

    protected boolean aggregatable = false;

    protected List<ColumnCollapseListener> columnCollapseListeners; // lazily initialized List

    protected Map<Object, CellClickListener> cellClickListeners; // lazily initialized map

    protected Map<Object, String> columnDescriptions; // lazily initialized map

    public CubaTreeTable() {
        registerRpc(new CubaTableServerRpc() {
            @Override
            public void onClick(String columnKey, String rowKey) {
                Object columnId = columnIdMap.get(columnKey);
                Object itemId = itemIdMapper.get(rowKey);

                if (cellClickListeners != null) {
                    CellClickListener cellClickListener = cellClickListeners.get(columnId);
                    if (cellClickListener != null) {
                        cellClickListener.onClick(itemId, columnId);
                    }
                }
            }
        });
    }

    @Override
    protected CubaTreeTableState getState() {
        return (CubaTreeTableState) super.getState();
    }

    @Override
    protected CubaTreeTableState getState(boolean markAsDirty) {
        return (CubaTreeTableState) super.getState(markAsDirty);
    }

    @Override
    public TablePresentations getPresentations() {
        return (TablePresentations) getState(false).presentations;
    }

    @Override
    public void setContextMenuPopup(Layout contextMenu) {
        getState().contextMenu = contextMenu;
    }

    @Override
    public void hideContextMenuPopup() {
        getRpcProxy(CubaTableClientRpc.class).hideContextMenuPopup();
    }

    @Override
    public void setPresentations(TablePresentations presentations) {
        getState().presentations = presentations;
    }

    @Override
    public void hidePresentationsPopup() {
        getRpcProxy(CubaTableClientRpc.class).hidePresentationsPopup();
    }

    @Override
    public boolean isTextSelectionEnabled() {
        return getState(false).textSelectionEnabled;
    }

    @Override
    public void setTextSelectionEnabled(boolean textSelectionEnabled) {
        if (isTextSelectionEnabled() != textSelectionEnabled) {
            getState(true).textSelectionEnabled = textSelectionEnabled;
        }
    }

    @Override
    public void setMultiLineCells(boolean multiLineCells) {
        if (isMultiLineCells() != multiLineCells) {
            getState(true).multiLineCells = multiLineCells;
        }
    }

    @Override
    public boolean isMultiLineCells() {
        return getState(false).multiLineCells;
    }

    @Override
    public boolean disableContentBufferRefreshing() {
        return disableContentRefreshing();
    }

    @Override
    public void enableContentBufferRefreshing(boolean refreshContent) {
        enableContentRefreshing(refreshContent);
    }

    @Override
    protected Object getPropertyValue(Object rowId, Object colId,
                                      Property property) {
        if (isColumnEditable(colId, isEditable()) && fieldFactory != null) {
            final Field<?> f = fieldFactory.createField(
                    getContainerDataSource(), rowId, colId, this);
            if (f != null) {
                // Remember that we have made this association so we can remove
                // it when the component is removed
                associatedProperties.put(f, property);
                if (autowirePropertyDsForFields) {
                    bindPropertyToField(rowId, colId, property, f);
                }
                return f;
            }
        }

        return formatPropertyValue(rowId, colId, property);
    }

    @Override
    public boolean isAutowirePropertyDsForFields() {
        return autowirePropertyDsForFields;
    }

    @Override
    public void setAutowirePropertyDsForFields(boolean autowirePropertyDsForFields) {
        this.autowirePropertyDsForFields = autowirePropertyDsForFields;
    }

    @Override
    public boolean isContextMenuEnabled() {
        return getState(false).contextMenuEnabled;
    }

    @Override
    public void setContextMenuEnabled(boolean contextMenuEnabled) {
        if (isContextMenuEnabled() != contextMenuEnabled) {
            getState(true).contextMenuEnabled = contextMenuEnabled;
        }
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

    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        super.changeVariables(source, variables);

        if (shortcutActionManager != null) {
            shortcutActionManager.handleActions(variables, this);
        }
    }

    @Override
    public void addShortcutListener(ShortcutListener shortcut) {
        if (shortcutActionManager == null) {
            shortcutActionManager = new ShortcutActionManager(this);
        }

        shortcutActionManager.addAction(shortcut);
    }

    @Override
    public void removeShortcutListener(ShortcutListener shortcut) {
        if (shortcutActionManager != null) {
            shortcutActionManager.removeAction(shortcut);
        }
    }

    @Override
    public void addColumnCollapseListener(ColumnCollapseListener listener) {
        if (columnCollapseListeners == null) {
            columnCollapseListeners = new LinkedList<>();
        }

        columnCollapseListeners.add(listener);
    }

    @Override
    public void removeColumnCollapseListener(ColumnCollapseListener listener) {
        if (columnCollapseListeners != null) {
            columnCollapseListeners.remove(listener);
        }
    }

    @Override
    public void setColumnCollapsed(Object propertyId, boolean collapsed) throws IllegalStateException {
        if (isColumnCollapsed(propertyId) != collapsed
                && columnCollapseListeners != null) {
            for (ColumnCollapseListener listener : columnCollapseListeners) {
                listener.columnCollapsed(propertyId, collapsed);
            }
        }
        super.setColumnCollapsed(propertyId, collapsed);
    }

    @Override
    protected void paintActions(PaintTarget target, Set<Action> actionSet) throws PaintException {
        super.paintActions(target, actionSet);

        if (shortcutActionManager != null) {
            shortcutActionManager.paintActions(null, target);
        }
    }

    @Override
    public Object[] getEditableColumns() {
        if (editableColumns == null) {
            return null;
        }
        return editableColumns.toArray();
    }

    @Override
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
                throw new IllegalStateException("Ids must be non-nulls");
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
    public boolean isColumnEditable(Object columnId) {
        return isColumnEditable(columnId, isEditable());
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
    protected boolean changeVariables(Map<String, Object> variables) {
        boolean clientNeedsContentRefresh = super.changeVariables(variables);

        if (variables.containsKey("resetsortorder")) {
            resetSortOrder();

            markAsDirty();
        }

        return clientNeedsContentRefresh;
    }

    @Override
    protected String formatPropertyValue(Object rowId, Object colId, Property<?> property) {
        if (property instanceof PropertyValueStringify) {
            return ((PropertyValueStringify) property).getFormattedValue();
        }

        return super.formatPropertyValue(rowId, colId, property);
    }

    @Override
    public void setContainerDataSource(Container newDataSource) {
        disableContentRefreshing();
        if (newDataSource == null) {
            newDataSource = new HierarchicalContainer();
        }

        super.setContainerDataSource(new TreeTableContainerWrapper(newDataSource));
    }

    public void expandAll() {
        for (Object id : getItemIds()) {
            expandItemRecursively(id);
        }
    }

    public void expandItemRecursively(Object id) {
        setCollapsed(id, false);
        if (hasChildren(id)) {
            for (Object childId: getChildren(id)) {
                expandItemRecursively(childId);
            }
        }
    }

    public void collapseAll() {
        for (Object id : getItemIds()) {
            setCollapsed(id, true);
        }
    }

    public void setExpanded(Object itemId) {
        setCollapsed(itemId, false);
    }

    public boolean isExpanded(Object itemId) {
        return !isCollapsed(itemId);
    }

    @Override
    public void resetSortOrder() {
        sortContainerPropertyId = null;
        sortAscending = true;

        if (items instanceof TableContainer) {
            ((TableContainer) items).resetSortOrder();
        }
    }

    @Override
    public Iterator<Component> iterator() {
        List<Component> additionalConnectors = null;

        CubaTreeTableState tableState = getState(false);
        if (tableState.presentations != null) {
            additionalConnectors = new LinkedList<>();
            additionalConnectors.add((Component) tableState.presentations);
        }
        if (tableState.contextMenu != null) {
            if (additionalConnectors == null) {
                additionalConnectors = new LinkedList<>();
            }
            additionalConnectors.add((Component) tableState.contextMenu);
        }
        if (tableState.customPopup != null) {
            if (additionalConnectors == null) {
                additionalConnectors = new LinkedList<>();
            }
            additionalConnectors.add((Component) tableState.customPopup);
        }

        if (additionalConnectors == null) {
            return super.iterator();
        } else if (visibleComponents != null) {
            return Iterables.concat(visibleComponents, additionalConnectors).iterator();
        } else {
            return additionalConnectors.iterator();
        }
    }

    @Override
    public void refreshCellStyles() {
        super.refreshRenderedCells();
    }

    @Override
    public boolean removeContainerProperty(Object propertyId) throws UnsupportedOperationException {
        if (editableColumns != null) {
            editableColumns.remove(propertyId);
        }

        if (isAggregatable() && items instanceof AggregationContainer) {
            removeContainerPropertyAggregation(propertyId);
        }

        boolean removed = super.removeContainerProperty(propertyId);

        if (removed) {
            resetPageBuffer();
        }

        return removed;
    }

    @Override
    public boolean isAggregatable() {
        return this.aggregatable;
    }

    @Override
    public void setAggregatable(boolean aggregatable) {
        if (this.aggregatable != aggregatable) {
            this.aggregatable = aggregatable;
            markAsDirty();
        }
    }

    @Override
    public boolean isShowTotalAggregation() {
        return showTotalAggregation;
    }

    @Override
    public void setShowTotalAggregation(boolean showTotalAggregation) {
        if (this.showTotalAggregation != showTotalAggregation) {
            this.showTotalAggregation = showTotalAggregation;
            markAsDirty();
        }
    }

    @Override
    public Collection getAggregationPropertyIds() {
        if (items instanceof AggregationContainer) {
            return ((AggregationContainer) items).getAggregationPropertyIds();
        }
        throw new IllegalStateException("Table container is not AggregationContainer: " + items.getClass());
    }

    @Override
    public Type getContainerPropertyAggregation(Object propertyId) {
        if (items instanceof AggregationContainer) {
            return ((AggregationContainer) items).getContainerPropertyAggregation(propertyId);
        }
        throw new IllegalStateException("Table container is not AggregationContainer: " + items.getClass());
    }

    @Override
    public void addContainerPropertyAggregation(Object propertyId, Type type) {
        if (items instanceof AggregationContainer) {
            ((AggregationContainer) items).addContainerPropertyAggregation(propertyId, type);
        } else {
            throw new IllegalStateException("Table container is not AggregationContainer: " + items.getClass());
        }
    }

    @Override
    public void removeContainerPropertyAggregation(Object propertyId) {
        if (items instanceof AggregationContainer) {
            ((AggregationContainer) items).removeContainerPropertyAggregation(propertyId);
        } else {
            throw new IllegalStateException("Table container is not AggregationContainer: " + items.getClass());
        }
    }

    @Override
    public Map<Object, Object> aggregate(Context context) {
        if (items instanceof AggregationContainer && isAggregatable()) {
            return ((AggregationContainer) items).aggregate(context);
        }
        throw new IllegalStateException("Table container is not AggregationContainer: " + items.getClass());
    }

    @Override
    protected void paintAdditionalData(PaintTarget target) throws PaintException {
        if (reqFirstRowToPaint == -1 && items instanceof AggregationContainer && isAggregatable()
                && !((AggregationContainer) items).getAggregationPropertyIds().isEmpty() && isShowTotalAggregation()) {
            paintAggregationRow(target, ((AggregationContainer) items).aggregate(new Context(items.getItemIds())));
        }
    }

    protected void paintAggregationRow(PaintTarget target, Map<Object, Object> aggregations) throws PaintException {
        target.startTag("arow");
        for (final Object columnId : visibleColumns) {
            if (columnId == null || isColumnCollapsed(columnId)) {
                continue;
            }

            if (getCellStyleGenerator() != null) {
                String cellStyle = getCellStyleGenerator().getStyle(this, null, columnId);
                if (cellStyle != null && !cellStyle.equals("")) {
                    target.addAttribute("style-"
                            + columnIdMap.key(columnId), cellStyle + "-ag");
                }
            }

            String value = (String) aggregations.get(columnId);
            target.addText(value);
        }
        target.endTag("arow");
    }

    @Override
    public void setClickListener(Object propertyId, CellClickListener clickListener) {
        if (cellClickListeners == null) {
            cellClickListeners = new HashMap<>();
        }
        cellClickListeners.put(propertyId, clickListener);
    }

    @Override
    public void removeClickListener(Object propertyId) {
        if (cellClickListeners != null) {
            cellClickListeners.remove(propertyId);
        }
    }

    @Override
    public void beforeClientResponse(boolean initial) {
        super.beforeClientResponse(initial);

        updateClickableColumnKeys();
        updateColumnDescriptions();
    }

    protected void updateClickableColumnKeys() {
        if (cellClickListeners != null) {
            String[] clickableColumnKeys = new String[cellClickListeners.size()];
            int i = 0;
            for (Object columnId : cellClickListeners.keySet()) {
                clickableColumnKeys[i] = columnIdMap.key(columnId);
                i++;
            }

            getState().clickableColumnKeys = clickableColumnKeys;
        }
    }

    @Override
    public void showCustomPopup(Component popupComponent) {
        if (getState().customPopup != null) {
            ((AbstractComponent) getState().customPopup).setParent(null);
        }

        getState().customPopup = popupComponent;
        getRpcProxy(CubaTableClientRpc.class).showCustomPopup();

        popupComponent.setParent(this);
    }

    @Override
    public boolean getCustomPopupAutoClose() {
        return getState(false).customPopupAutoClose;
    }

    @Override
    public void setCustomPopupAutoClose(boolean popupAutoClose) {
        if (getState(false).customPopupAutoClose != popupAutoClose) {
            getState().customPopupAutoClose = popupAutoClose;
        }
    }

    @Override
    public void setColumnDescription(Object columnId, String description) {
        if (description != null) {
            if (columnDescriptions == null) {
                columnDescriptions = new HashMap<>();
            }
            columnDescriptions.put(columnId, description);
        } else if (columnDescriptions != null) {
            columnDescriptions.remove(columnId);
        }
    }

    @Override
    public String getColumnDescription(Object columnId) {
        if (columnDescriptions != null) {
            return columnDescriptions.get(columnId);
        }
        return null;
    }

    protected void updateColumnDescriptions() {
        if (columnDescriptions != null) {
            Map<String, String> columnDescriptionsByKey = new HashMap<>();
            for (Map.Entry<Object, String> columnEntry : columnDescriptions.entrySet()) {
                columnDescriptionsByKey.put(columnIdMap.key(columnEntry.getKey()), columnEntry.getValue());
            }
            getState().columnDescriptions = columnDescriptionsByKey;
        }
    }
}