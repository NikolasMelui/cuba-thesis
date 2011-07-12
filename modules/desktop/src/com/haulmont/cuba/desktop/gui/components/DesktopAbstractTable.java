/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.MessageUtils;
import com.haulmont.cuba.core.global.MetadataHelper;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.desktop.App;
import com.haulmont.cuba.desktop.gui.data.AnyTableModelAdapter;
import com.haulmont.cuba.desktop.gui.data.RowSorterImpl;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class DesktopAbstractTable<C extends JTable>
        extends DesktopAbstractActionOwnerComponent<C>
        implements Table
{
    protected MigLayout layout;
    protected JPanel panel;
    protected JPanel topPanel;
    protected AnyTableModelAdapter tableModel;
    protected CollectionDatasource datasource;
    protected ButtonsPanel buttonsPanel;
    protected RowsCount rowsCount;
    protected Map<MetaPropertyPath, Column> columns = new HashMap<MetaPropertyPath, Column>();
    protected List<Table.Column> columnsOrder = new ArrayList<Table.Column>();
    protected boolean sortable = true;
    protected TableSettings tableSettings;

    protected void initComponent() {
        layout = new MigLayout("flowy, fill, insets 0", "", "[min!][fill]");
        panel = new JPanel(layout);

        topPanel = new JPanel(new BorderLayout());
        topPanel.setVisible(false);
        panel.add(topPanel, "growx");

        JScrollPane scrollPane = new JScrollPane(impl);
        impl.setFillsViewportHeight(true);
        panel.add(scrollPane, "grow");

        impl.setShowGrid(true);
        impl.setGridColor(Color.lightGray);

        impl.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            Action action = getAction(EditAction.ACTION_ID);
                            if (action != null)
                                action.actionPerform(DesktopAbstractTable.this);
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        showPopup(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        showPopup(e);
                    }

                    private void showPopup(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            // select row
                            Point p = e.getPoint();
                            int rowNumber = impl.convertRowIndexToModel(impl.rowAtPoint(p));
                            ListSelectionModel model = impl.getSelectionModel();
                            model.setSelectionInterval(rowNumber, rowNumber);
                            // show popup menu
                            createPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
        );
    }

    protected abstract void initTableModel(CollectionDatasource datasource);

    @Override
    public JComponent getComposition() {
        return panel;
    }

    public List<Column> getColumns() {
        return columnsOrder;
    }

    public Column getColumn(String id) {
        for (Table.Column column : columnsOrder) {
            if (column.getId().toString().equals(id))
                return column;
        }
        return null;
    }

    public void addColumn(Column column) {
        columns.put((MetaPropertyPath) column.getId(), column);
        columnsOrder.add(column);
    }

    public void removeColumn(Column column) {
        columns.remove((MetaPropertyPath) column.getId());
        columnsOrder.remove(column);
    }

    public void setDatasource(final CollectionDatasource datasource) {
        UserSession userSession = UserSessionProvider.getUserSession();
        if (!userSession.isEntityOpPermitted(datasource.getMetaClass(), EntityOp.READ)) {
            impl.setVisible(false);
            return;
        }

        final Collection<MetaPropertyPath> properties;
        if (this.columns.isEmpty()) {
            Collection<MetaPropertyPath> paths = MetadataHelper.getViewPropertyPaths(datasource.getView(), datasource.getMetaClass());
            for (MetaPropertyPath metaPropertyPath : paths) {
                MetaProperty property = metaPropertyPath.getMetaProperty();
                if (!property.getRange().getCardinality().isMany() && !MetadataHelper.isSystem(property)) {
                    Table.Column column = new Table.Column(metaPropertyPath);

                    column.setCaption(MessageUtils.getPropertyCaption(property));
                    column.setType(metaPropertyPath.getRangeJavaClass());

                    Element element = DocumentHelper.createElement("column");
                    column.setXmlDescriptor(element);

                    addColumn(column);
                }
            }
        }
        properties = this.columns.keySet();

        this.datasource = datasource;

        initTableModel(datasource);

        Enumeration<TableColumn> columnEnumeration = impl.getColumnModel().getColumns();
        int i = 0;
        while (columnEnumeration.hasMoreElements()) {
            TableColumn tableColumn = columnEnumeration.nextElement();
            Column column = columnsOrder.get(i++);
            tableColumn.setIdentifier(column);
        }

        impl.setRowSorter(new RowSorterImpl(tableModel));

        initSelectionListener(datasource);

        List<MetaPropertyPath> editableColumns = null;
        if (isEditable()) {
            editableColumns = new LinkedList<MetaPropertyPath>();
        }

        for (final MetaPropertyPath propertyPath : properties) {
            final Table.Column column = this.columns.get(propertyPath);

            final String caption;
            if (column != null) {
                caption = StringUtils.capitalize(column.getCaption() != null ? column.getCaption() : propertyPath.getMetaProperty().getName());
            } else {
                caption = StringUtils.capitalize(propertyPath.getMetaProperty().getName());
            }

            setColumnHeader(propertyPath, caption);

            if (column != null) {
                if (editableColumns != null && column.isEditable()) {
                    MetaProperty colMetaProperty = propertyPath.getMetaProperty();
                    MetaClass colMetaClass = colMetaProperty.getDomain();
                    if (userSession.isEntityAttrPermitted(colMetaClass, colMetaProperty.getName(), EntityAttrAccess.MODIFY)) {
                        editableColumns.add((MetaPropertyPath) column.getId());
                    }
                }

//                if (column.isCollapsed() && component.isColumnCollapsingAllowed()) {
//                    try {
//                        component.setColumnCollapsed(column.getId(), true);
//                    } catch (IllegalAccessException e) {
//                        // do nothing
//                    }
//                }

//                if (column.getAggregation() != null && isAggregatable()) {
//                    component.addContainerPropertyAggregation(column.getId(),
//                            WebComponentsHelper.convertAggregationType(column.getAggregation().getType()));
//                }
            }
        }

        if (editableColumns != null && !editableColumns.isEmpty()) {
            setEditableColumns(editableColumns);
        }

        List<MetaPropertyPath> columnsOrder = new ArrayList<MetaPropertyPath>();
        for (Table.Column column : this.columnsOrder) {
            MetaProperty colMetaProperty = ((MetaPropertyPath) column.getId()).getMetaProperty();
            MetaClass colMetaClass = colMetaProperty.getDomain();
            if (userSession.isEntityOpPermitted(colMetaClass, EntityOp.READ)
                    && userSession.isEntityAttrPermitted(
                    colMetaClass, colMetaProperty.getName(), EntityAttrAccess.VIEW)) {
                columnsOrder.add((MetaPropertyPath) column.getId());
            }
//            if (editable && column.getAggregation() != null
//                    && (BooleanUtils.isTrue(column.isEditable()) || BooleanUtils.isTrue(column.isCalculatable())))
//            {
//                addAggregationCell(column);
//            }
        }

//        if (aggregationCells != null) {
//            dsManager.addListener(createAggregationDatasourceListener());
//        }

        setVisibleColumns(columnsOrder);

//        if (UserSessionProvider.getUserSession().isSpecificPermitted(ShowInfoAction.ACTION_PERMISSION)) {
//            ShowInfoAction action = (ShowInfoAction) getAction(ShowInfoAction.ACTION_ID);
//            if (action == null) {
//                action = new ShowInfoAction();
//                addAction(action);
//            }
//            action.setDatasource(datasource);
//        }
//
        if (rowsCount != null)
            rowsCount.setDatasource(datasource);
    }

    protected void initSelectionListener(final CollectionDatasource datasource) {
        impl.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting())
                            return;

                        Entity entity = getSingleSelected();
                        datasource.setItem(entity);
                    }
                }
        );
    }

    protected void setVisibleColumns(List<MetaPropertyPath> columnsOrder) {
    }

    protected void setEditableColumns(List<MetaPropertyPath> editableColumns) {
    }

    protected void setColumnHeader(MetaPropertyPath propertyPath, String caption) {
    }

    public void setRequired(Column column, boolean required, String message) {
    }

    public void addValidator(Column column, Field.Validator validator) {
    }

    public void addValidator(Field.Validator validator) {
    }

    public void setItemClickAction(com.haulmont.cuba.gui.components.Action action) {
    }

    public Action getItemClickAction() {
        return null;
    }

    public List<Column> getNotCollapsedColumns() {
        return null;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setAggregatable(boolean aggregatable) {
    }

    public boolean isAggregatable() {
        return false;
    }

    public void setShowTotalAggregation(boolean showAggregation) {
    }

    public boolean isShowTotalAggregation() {
        return false;
    }

    public void sortBy(Object propertyId, boolean ascending) {
    }

    public RowsCount getRowsCount() {
        return null;
    }

    public void setRowsCount(RowsCount rowsCount) {
        if (this.rowsCount != null) {
            topPanel.remove(DesktopComponentsHelper.getComposition(this.rowsCount));
        }
        this.rowsCount = rowsCount;
        if (rowsCount != null) {
            topPanel.add(DesktopComponentsHelper.getComposition(rowsCount), BorderLayout.EAST);
            topPanel.setVisible(true);
        }
    }

    public boolean isAllowMultiStringCells() {
        return false;
    }

    public void setAllowMultiStringCells(boolean value) {
    }

    public void setRowHeaderMode(RowHeaderMode mode) {
    }

    public void setStyleProvider(StyleProvider styleProvider) {
    }

    public void setPagingMode(PagingMode mode) {
    }

    public void setPagingProvider(PagingProvider pagingProvider) {
    }

    public void addGeneratedColumn(String columnId, ColumnGenerator generator) {
    }

    public void removeGeneratedColumn(Object id){
    }

    public boolean isEditable() {
        return false;
    }

    public void setEditable(boolean editable) {
    }

    public ButtonsPanel getButtonsPanel() {
        return buttonsPanel;
    }

    public void setButtonsPanel(ButtonsPanel panel) {
        if (buttonsPanel != null) {
            topPanel.remove(DesktopComponentsHelper.unwrap(buttonsPanel));
        }
        buttonsPanel = panel;
        if (panel != null) {
            topPanel.add(DesktopComponentsHelper.unwrap(panel), BorderLayout.WEST);
            topPanel.setVisible(true);
        }
    }

    public void usePresentations(boolean b) {
    }

    public boolean isUsePresentations() {
        return false;
    }

    public void loadPresentations() {
    }

    public Presentations getPresentations() {
        return null;
    }

    public void applyPresentation(Object id) {
    }

    public void applyPresentationAsDefault(Object id) {
    }

    public Object getDefaultPresentationId() {
        return null;
    }

    public void applySettings(Element element) {
        tableSettings.apply(element, isSortable());
    }

    public boolean saveSettings(Element element) {
        return tableSettings.saveSettings(element);
    }

    public boolean isMultiSelect() {
        return impl.getSelectionModel().getSelectionMode() != ListSelectionModel.SINGLE_SELECTION;
    }

    public void setMultiSelect(boolean multiselect) {
        if (multiselect)
            impl.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        else
            impl.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public <T extends Entity> T getSingleSelected() {
        Set selected = getSelected();
        return selected.isEmpty() ? null : (T) selected.iterator().next();
    }

    public Set getSelected() {
        Set set = new HashSet();
        int[] rows = impl.getSelectedRows();
        for (int row : rows) {
            int modelRow = impl.convertRowIndexToModel(row);
            Object item = tableModel.getItem(modelRow);
            set.add(item);
        }
        return set;
    }

    public void setSelected(Entity item) {
        int rowIndex = impl.convertRowIndexToView(tableModel.getRowIndex(item));
        impl.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }

    public void setSelected(Collection<Entity> items) {
        for (Entity item : items) {
            int rowIndex = impl.convertRowIndexToView(tableModel.getRowIndex(item));
            impl.getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
        }
    }

    public CollectionDatasource getDatasource() {
        return datasource;
    }

    public void refresh() {
        datasource.refresh();
    }

    protected JPopupMenu createPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        for (final Action action : actionsOrder) {
            menuItem = new JMenuItem(action.getCaption());
            if (action.getIcon() != null) {
                menuItem.setIcon(App.getInstance().getResources().getIcon(action.getIcon()));
            }
            menuItem.setEnabled(action.isEnabled());
            menuItem.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            action.actionPerform(DesktopAbstractTable.this);
                        }
                    }
            );
            popup.add(menuItem);
        }
        return popup;
    }
}
