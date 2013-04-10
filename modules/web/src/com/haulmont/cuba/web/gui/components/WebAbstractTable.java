/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.impl.BooleanDatatype;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.client.ClientConfig;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.actions.ListActionType;
import com.haulmont.cuba.gui.data.*;
import com.haulmont.cuba.gui.data.impl.CollectionDsActionsNotifier;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.gui.presentations.PresentationsImpl;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.entity.Presentation;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.cuba.web.gui.AbstractFieldFactory;
import com.haulmont.cuba.web.gui.CompositionLayout;
import com.haulmont.cuba.web.gui.components.presentations.TablePresentations;
import com.haulmont.cuba.web.gui.data.CollectionDsWrapper;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.ui.CubaPlaceHolder;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @param <T>
 * @author abramov
 * @version $Id$
 */
public abstract class WebAbstractTable<T extends com.vaadin.ui.Table>
        extends WebAbstractList<T> implements Table {

    private static final String REQUIRED_TABLE_STYLE = "table";

    protected Map<Object, Column> columns = new HashMap<>();
    protected List<Table.Column> columnsOrder = new ArrayList<>();
    protected Map<MetaClass, CollectionDatasource> optionsDatasources = new HashMap<>();
    protected boolean editable;
    protected Action itemClickAction;
    protected Action enterPressAction;

    protected Table.StyleProvider styleProvider;

    protected Map<Table.Column, String> requiredColumns = new HashMap<>();

    protected Map<Table.Column, Set<Field.Validator>> validatorsMap = new HashMap<>();

    protected Set<com.haulmont.cuba.gui.components.Field.Validator> tableValidators = new LinkedHashSet<>();

    protected CompositionLayout componentComposition;

    protected HorizontalLayout topPanel;

    protected ButtonsPanel buttonsPanel;

    protected RowsCount rowsCount;

    protected Map<Table.Column, Object> aggregationCells = null;

    protected boolean usePresentations;

    protected Presentations presentations;
    protected TablePresentations tablePresentations;

    protected List<ColumnCollapseListener> columnCollapseListeners = new ArrayList<>();

    protected Map<String, Printable> printables = new HashMap<>();

    private String customStyle;

    @Override
    public java.util.List<Table.Column> getColumns() {
        return columnsOrder;
    }

    @Override
    public Table.Column getColumn(String id) {
        for (Table.Column column : columnsOrder) {
            if (column.getId().toString().equals(id))
                return column;
        }
        return null;
    }

    @Override
    public void addColumn(Table.Column column) {
        component.addContainerProperty(column.getId(), column.getType(), null);
        columns.put(column.getId(), column);
        columnsOrder.add(column);
        if (column.getWidth() != null) {
            component.setColumnWidth(column.getId(), column.getWidth());
        }
    }

    @Override
    public void removeColumn(Table.Column column) {
        component.removeContainerProperty(column.getId());
        //noinspection RedundantCast
        columns.remove((MetaPropertyPath) column.getId());
        columnsOrder.remove(column);
    }

    protected void addGeneratedColumn(Object id, Object generator) {
        component.addGeneratedColumn(id, (com.vaadin.ui.Table.ColumnGenerator) generator);
    }

    protected void removeGeneratedColumn(Object id) {
        component.removeGeneratedColumn(id);
    }

    @Override
    public void addPrintable(String columnId, Printable printable) {
        printables.put(columnId, printable);
    }

    @Override
    public void removePrintable(String columnId) {
        printables.remove(columnId);
    }

    @Override
    @Nullable
    public Printable getPrintable(Table.Column column) {
        com.vaadin.ui.Table.ColumnGenerator vColumnGenerator = component.getColumnGenerator(column.getId());
        if (vColumnGenerator instanceof CustomColumnGenerator) {
            ColumnGenerator columnGenerator = ((CustomColumnGenerator) vColumnGenerator).getColumnGenerator();
            if (columnGenerator instanceof Printable)
                return (Printable) columnGenerator;
        }
        return printables.get(column.getId().toString());
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    @Override
    public void setEditable(boolean editable) {
        this.editable = editable;
        if (datasource != null) {
            refreshColumns(component.getContainerDataSource());
        }
        component.setEditable(editable);
    }

    protected void setEditableColumns(List<MetaPropertyPath> editableColumns) {
//        vaadin7
//        component.setEditableColumns(editableColumns.toArray());
    }

    @Override
    public boolean isSortable() {
        return component.isSortEnabled();
    }

    @Override
    public void setSortable(boolean sortable) {
        component.setSortEnabled(sortable);
    }

    @Override
    public void setColumnReorderingAllowed(boolean columnReorderingAllowed) {
        component.setColumnReorderingAllowed(columnReorderingAllowed);
    }

    @Override
    public boolean getColumnReorderingAllowed() {
        return component.isColumnReorderingAllowed();
    }

    @Override
    public void setColumnControlVisible(boolean columnCollapsingAllowed) {
        component.setColumnCollapsingAllowed(columnCollapsingAllowed);
    }

    @Override
    public boolean getColumnControlVisible() {
        return component.isColumnCollapsingAllowed();
    }

    @Override
    public void sortBy(Object propertyId, boolean ascending) {
        if (isSortable()) {
            component.setSortAscending(ascending);
            component.setSortContainerPropertyId(propertyId);
            component.sort();
        }
    }

    @Override
    public RowsCount getRowsCount() {
        return rowsCount;
    }

    @Override
    public void setRowsCount(RowsCount rowsCount) {
        if (this.rowsCount != null && topPanel != null) {
            topPanel.removeComponent(WebComponentsHelper.unwrap(this.rowsCount));
        }
        this.rowsCount = rowsCount;
        if (rowsCount != null) {
            if (topPanel == null) {
                topPanel = new HorizontalLayout();
                topPanel.setWidth("100%");
                componentComposition.addComponentAsFirst(topPanel);
            }
            com.vaadin.ui.Component rc = WebComponentsHelper.unwrap(rowsCount);
            topPanel.addComponent(rc);
            topPanel.setComponentAlignment(rc, com.vaadin.ui.Alignment.BOTTOM_RIGHT);
        }
    }

    @Override
    public boolean isAllowMultiStringCells() {
        return false;
//        vaadin 7
//        return component.isAllowMultiStringCells();
    }

    @Override
    public void setAllowMultiStringCells(boolean value) {
//        vaadin7
//        component.setAllowMultiStringCells(value);
    }

    @Override
    public boolean isAggregatable() {
        return false;
//        vaadin7
//        return component.isAggregatable();
    }

    @Override
    public void setAggregatable(boolean aggregatable) {
//        vaadin7
//        component.setAggregatable(aggregatable);
    }

    @Override
    public void setShowTotalAggregation(boolean showAggregation) {
//        vaadin7
//        component.setShowTotalAggregation(showAggregation);
    }

    @Override
    public boolean isShowTotalAggregation() {
        return false;
//        vaadin7
//        return component.isShowTotalAggregation();
    }

    @Override
    public com.vaadin.ui.Component getComposition() {
        return componentComposition;
    }

    @Override
    public float getHeight() {
        return componentComposition.getHeight();
    }

    @Override
    public int getHeightUnits() {
        return UNIT_SYMBOLS.indexOf(componentComposition.getHeightUnits());
    }

    @Override
    public void setHeight(String height) {
        componentComposition.setHeight(height);
    }

    @Override
    public float getWidth() {
        return componentComposition.getWidth();
    }

    @Override
    public void setWidth(String width) {
        componentComposition.setWidth(width);
    }

    @Override
    public int getWidthUnits() {
        return UNIT_SYMBOLS.indexOf(componentComposition.getWidthUnits());
    }

    @Override
    public void setStyleName(String name) {
        this.customStyle = name;
        String style = REQUIRED_TABLE_STYLE;
        if (StringUtils.isNotEmpty(name))
            style += " " + name;
        super.setStyleName(style);
    }

    @Override
    public String getStyleName() {
        return customStyle;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected CollectionDatasource getOptionsDatasource(MetaClass metaClass, Table.Column column) {
        if (datasource == null)
            throw new IllegalStateException("Table datasource is null");

        final DsContext dsContext = datasource.getDsContext();

        String optDsName = column.getXmlDescriptor().attributeValue("optionsDatasource");
        if (StringUtils.isBlank(optDsName)) {
            CollectionDatasource ds = optionsDatasources.get(metaClass);
            if (ds != null) return ds;

            final DataSupplier dataSupplier = datasource.getDataSupplier();

            final String id = metaClass.getName();
            final String viewName = null; //metaClass.getName() + ".lookup";

            ds = new DsBuilder(dsContext)
                    .setDataSupplier(dataSupplier)
                    .setId(id)
                    .setMetaClass(metaClass)
                    .setViewName(viewName)
                    .buildCollectionDatasource();

            ds.refresh();

            optionsDatasources.put(metaClass, ds);

            return ds;
        } else {
            CollectionDatasource ds = dsContext.get(optDsName);
            if (ds == null)
                throw new IllegalStateException("Options datasource not found: " + optDsName);
            return ds;
        }
    }

    protected void initComponent(T component) {
        component.setMultiSelect(false);
        component.setNullSelectionAllowed(false);
        component.setImmediate(true);
        component.setValidationVisible(false);
//        vaadin7
//        component.setStoreColWidth(true);
        component.setStyleName(REQUIRED_TABLE_STYLE); //It helps us to manage a caption style
        component.setPageLength(15);

        component.addActionHandler(new ActionsAdapter());

        component.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void valueChange(Property.ValueChangeEvent event) {
                if (datasource == null) return;

                final Set<Entity> selected = getSelected();
                if (selected.isEmpty()) {
                    datasource.setItem(null);
                } else {
                    // reset selection and select new item
                    if (isMultiSelect())
                        datasource.setItem(null);
                    datasource.setItem(selected.iterator().next());
                }
            }
        });

        component.addShortcutListener(new ShortcutListener("tableEnter", com.vaadin.event.ShortcutAction.KeyCode.ENTER, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (enterPressAction != null) {
                    enterPressAction.actionPerform(WebAbstractTable.this);
                } else {
                    handleClickAction();
                }
            }
        });

        component.addItemClickListener(new ItemClickEvent.ItemClickListener() {
            @Override
            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick() && event.getItem() != null) {
                    handleClickAction();
                }
            }
        });

        // vaadin7
//        component.addColumnCollapseListener(new com.haulmont.cuba.web.toolkit.ui.Table.CollapseListener() {
//            @Override
//            public void columnCollapsed(Object columnId, boolean collapsed) {
//                final Column collapsedColumn = getColumn(columnId.toString());
//                for (ColumnCollapseListener listener : columnCollapseListeners) {
//                    listener.columnCollapsed(collapsedColumn, collapsed);
//                }
//            }
//        });

        component.setSelectable(true);
        component.setTableFieldFactory(new FieldFactory());
        component.setColumnCollapsingAllowed(true);
        component.setColumnReorderingAllowed(true);

        setEditable(false);

        componentComposition = new CompositionLayout(component);
        componentComposition.setSpacing(true);
        componentComposition.setMargin(false);
        componentComposition.setWidth("-1px");
        component.setSizeFull();
        componentComposition.setExpandRatio(component, 1);

//        vaadin7
//        component.setEnableCancelSorting(ConfigProvider.getConfig(WebConfig.class).getEnableCancelTableSorting());

        ClientConfig clientConfig = AppBeans.get(Configuration.class).getConfig(ClientConfig.class);

        addShortcutActionBridge(INSERT_SHORTCUT_ID, clientConfig.getTableInsertShortcut(), ListActionType.CREATE);
        addShortcutActionBridge(REMOVE_SHORTCUT_ID, clientConfig.getTableRemoveShortcut(), ListActionType.REMOVE);
    }

    /**
     * Connect shortcut action to default list action
     * @param shortcutActionId Shortcut action id
     * @param keyCombination Keys
     * @param defaultAction List action
     */
    protected void addShortcutActionBridge(String shortcutActionId, String keyCombination,
                                           final ListActionType defaultAction) {

        ShortcutAction.KeyCombination actionKeyCombination = ShortcutAction.KeyCombination.create(keyCombination);
        component.addShortcutListener(new ShortcutListener(shortcutActionId, actionKeyCombination.getKey().getCode(),
                ShortcutAction.Modifier.codes(actionKeyCombination.getModifiers())) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target == component) {
                    Action listAction = getAction(defaultAction.getId());
                    if (listAction != null && listAction.isEnabled())
                        listAction.actionPerform(WebAbstractTable.this);
                }
            }
        });
    }

    protected void handleClickAction() {
        Action action = getItemClickAction();
        if (action == null) {
            action = getAction("edit");
            if (action == null) {
                action = getAction("view");
            }
        }
        if (action != null && action.isEnabled()) {
            Window window = ComponentsHelper.getWindow(WebAbstractTable.this);

            if (!(window instanceof Window.Lookup)) {
                action.actionPerform(WebAbstractTable.this);
            } else {
                Window.Lookup lookup = (Window.Lookup) window;

                com.haulmont.cuba.gui.components.Component lookupComponent = lookup.getLookupComponent();
                if (lookupComponent != this)
                    action.actionPerform(WebAbstractTable.this);
                else if (action.getId().equals(WindowDelegate.LOOKUP_ITEM_CLICK_ACTION_ID)) {
                    action.actionPerform(WebAbstractTable.this);
                }
            }
        }
    }

    protected Collection<MetaPropertyPath> createColumns(com.vaadin.data.Container ds) {
        @SuppressWarnings({"unchecked"})
        final Collection<MetaPropertyPath> properties = (Collection<MetaPropertyPath>) ds.getContainerPropertyIds();

        Window window = ComponentsHelper.getWindow(this);
        boolean isLookup = window instanceof Window.Lookup;

        for (MetaPropertyPath propertyPath : properties) {
            final Table.Column column = columns.get(propertyPath);
            if (column != null && !(editable && BooleanUtils.isTrue(column.isEditable()))) {
                final String clickAction =
                        column.getXmlDescriptor() == null ?
                                null : column.getXmlDescriptor().attributeValue("clickAction");

                if (propertyPath.getRange().isClass()) {
                    if (!isLookup && !StringUtils.isEmpty(clickAction)) {
                        addGeneratedColumn(propertyPath, new ReadOnlyAssociationGenerator(column));
                    }
                } else if (propertyPath.getRange().isDatatype()) {
                    if (!isLookup && !StringUtils.isEmpty(clickAction)) {
                        addGeneratedColumn(propertyPath, new CodePropertyGenerator(column));
                    } else if (editable && BooleanUtils.isTrue(column.isCalculatable())) {
                        addGeneratedColumn(propertyPath, new CalculatableColumnGenerator());
                    } else {
                        final Datatype datatype = propertyPath.getRange().asDatatype();
                        if (BooleanDatatype.NAME.equals(datatype.getName()) && column.getFormatter() == null) {
                            addGeneratedColumn(propertyPath, new ReadOnlyBooleanDatatypeGenerator());
                        }
                    }
                } else if (propertyPath.getRange().isEnum()) {
                    // TODO (abramov)
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }

        return properties;
    }

    protected void refreshColumns(com.vaadin.data.Container ds) {
        @SuppressWarnings({"unchecked"})
        final Collection<MetaPropertyPath> propertyIds = (Collection<MetaPropertyPath>) ds.getContainerPropertyIds();
        for (final MetaPropertyPath id : propertyIds) {
            removeGeneratedColumn(id);
        }

        if (isEditable()) {
            final List<MetaPropertyPath> editableColumns = new ArrayList<>(propertyIds.size());
            for (final MetaPropertyPath propertyId : propertyIds) {
                final Table.Column column = getColumn(propertyId.toString());
                if (BooleanUtils.isTrue(column.isEditable())) {
                    editableColumns.add(propertyId);
                }
            }
            if (!editableColumns.isEmpty()) {
                setEditableColumns(editableColumns);
            }
        } else {
            setEditableColumns(Collections.<MetaPropertyPath>emptyList());
        }

        createColumns(ds);
    }

    @Override
    public void setDatasource(CollectionDatasource datasource) {
        UserSession userSession = AppBeans.get(UserSessionSource.class).getUserSession();
        MessageTools messageTools = AppBeans.get(MessageTools.class);
        MetadataTools metadataTools = AppBeans.get(MetadataTools.class);

        final Collection<Object> columns;
        if (this.columns.isEmpty()) {
            Collection<MetaPropertyPath> paths = metadataTools.getViewPropertyPaths(datasource.getView(), datasource.getMetaClass());
            for (MetaPropertyPath metaPropertyPath : paths) {
                MetaProperty property = metaPropertyPath.getMetaProperty();
                if (!property.getRange().getCardinality().isMany() && !metadataTools.isSystem(property)) {
                    Table.Column column = new Table.Column(metaPropertyPath);

                    column.setCaption(messageTools.getPropertyCaption(property));
                    column.setType(metaPropertyPath.getRangeJavaClass());

                    Element element = DocumentHelper.createElement("column");
                    column.setXmlDescriptor(element);

                    addColumn(column);
                }
            }
        }
        columns = this.columns.keySet();

        this.datasource = datasource;

        final CollectionDsWrapper containerDatasource = createContainerDatasource(datasource, getPropertyColumns());

        component.setContainerDataSource(containerDatasource);

        if (columns == null) {
            throw new NullPointerException("Columns cannot be null");
        }

        List<MetaPropertyPath> editableColumns = null;
        if (isEditable()) {
            editableColumns = new LinkedList<>();
        }

        for (final Object columnId : columns) {
            final Table.Column column = this.columns.get(columnId);

            final String caption;
            if (column != null) {
                caption = StringUtils.capitalize(column.getCaption() != null ? column.getCaption() : getColumnCaption(columnId));
            } else {
                caption = StringUtils.capitalize(getColumnCaption(columnId));
            }

            setColumnHeader(columnId, caption);

            if (column != null) {
                if (editableColumns != null && column.isEditable() && (columnId instanceof MetaPropertyPath)) {
                    MetaProperty colMetaProperty = ((MetaPropertyPath) columnId).getMetaProperty();
                    MetaClass colMetaClass = colMetaProperty.getDomain();
                    if (userSession.isEntityAttrPermitted(colMetaClass, colMetaProperty.getName(), EntityAttrAccess.MODIFY)) {
                        editableColumns.add((MetaPropertyPath) column.getId());
                    }
                }

                if (column.isCollapsed() && component.isColumnCollapsingAllowed()) {
                    component.setColumnCollapsed(column.getId(), true);
                }

//                vaadin7
//                if (column.getAggregation() != null && isAggregatable()) {
//                    component.addContainerPropertyAggregation(column.getId(),
//                            WebComponentsHelper.convertAggregationType(column.getAggregation().getType()));
//                }
            }
        }

        if (editableColumns != null && !editableColumns.isEmpty()) {
            setEditableColumns(editableColumns);
        }

        createColumns(containerDatasource);

        for (Table.Column column : this.columnsOrder) {
            if (editable && column.getAggregation() != null
                    && (BooleanUtils.isTrue(column.isEditable()) || BooleanUtils.isTrue(column.isCalculatable())))
            {
                addAggregationCell(column);
            }
        }

//        vaadin7
//        if (aggregationCells != null) {
//            getDatasource().addListener(createAggregationDatasourceListener());
//        }

        setVisibleColumns(getPropertyColumns());

        if (AppBeans.get(UserSessionSource.class).getUserSession().isSpecificPermitted(ShowInfoAction.ACTION_PERMISSION)) {
            ShowInfoAction action = (ShowInfoAction) getAction(ShowInfoAction.ACTION_ID);
            if (action == null) {
                action = new ShowInfoAction();
                addAction(action);
            }
            action.setDatasource(datasource);
        }

        if (rowsCount != null)
            rowsCount.setDatasource(datasource);

        datasource.addListener(new CollectionDsActionsNotifier(this));
    }

    private String getColumnCaption(Object columnId) {
        if (columnId instanceof MetaPropertyPath)
            return ((MetaPropertyPath) columnId).getMetaProperty().getName();
        else
            return columnId.toString();
    }

    private List<MetaPropertyPath> getPropertyColumns() {
        UserSession userSession = AppBeans.get(UserSessionSource.class).getUserSession();
        List<MetaPropertyPath> result = new ArrayList<>();
        for (Column column : columnsOrder) {
            if (column.getId() instanceof MetaPropertyPath) {
                MetaProperty colMetaProperty = ((MetaPropertyPath) column.getId()).getMetaProperty();
                MetaClass colMetaClass = colMetaProperty.getDomain();
                if (userSession.isEntityOpPermitted(colMetaClass, EntityOp.READ)
                        && userSession.isEntityAttrPermitted(
                        colMetaClass, colMetaProperty.getName(), EntityAttrAccess.VIEW)) {
                    result.add((MetaPropertyPath)column.getId());
                }
            }
        }
        return result;
    }

    protected abstract CollectionDsWrapper createContainerDatasource(CollectionDatasource datasource,
                                                                     Collection<MetaPropertyPath> columns);

    protected void setVisibleColumns(List<?> columnsOrder) {
        component.setVisibleColumns(columnsOrder.toArray());
    }

    protected void setColumnHeader(Object columnId, String caption) {
        component.setColumnHeader(columnId, caption);
    }

    @Override
    public void setRowHeaderMode(com.haulmont.cuba.gui.components.Table.RowHeaderMode rowHeaderMode) {
        switch (rowHeaderMode) {
            case NONE: {
                component.setRowHeaderMode(com.vaadin.ui.Table.RowHeaderMode.HIDDEN);
                break;
            }
            case ICON: {
                component.setRowHeaderMode(com.vaadin.ui.Table.RowHeaderMode.ICON_ONLY);
                break;
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public void setRequired(Table.Column column, boolean required, String message) {
        if (required)
            requiredColumns.put(column, message);
        else
            requiredColumns.remove(column);
    }

    @Override
    public void addValidator(Table.Column column, final com.haulmont.cuba.gui.components.Field.Validator validator) {
        Set<com.haulmont.cuba.gui.components.Field.Validator> validators = validatorsMap.get(column);
        if (validators == null) {
            validators = new HashSet<>();
            validatorsMap.put(column, validators);
        }
        validators.add(validator);
    }

    @Override
    public void addValidator(final com.haulmont.cuba.gui.components.Field.Validator validator) {
        tableValidators.add(validator);
    }

    public void validate() throws ValidationException {
        for (com.haulmont.cuba.gui.components.Field.Validator tableValidator : tableValidators) {
            tableValidator.validate(getSelected());
        }
    }

    @Override
    public void setStyleProvider(final Table.StyleProvider styleProvider) {
        this.styleProvider = styleProvider;
        if (styleProvider == null) {
            component.setCellStyleGenerator(null);
            return;
        }

        component.setCellStyleGenerator(new com.vaadin.ui.Table.CellStyleGenerator() {
            @Override
            public String getStyle(com.vaadin.ui.Table source, Object itemId, Object propertyId) {
                @SuppressWarnings({"unchecked"})
                final Entity item = datasource.getItem(itemId);
                return styleProvider.getStyleName(item, propertyId == null ? null : propertyId.toString());
            }
        });
    }

    @Override
    public void applySettings(Element element) {
        final Element columnsElem = element.element("columns");
        if (columnsElem != null) {
            Object[] oldColumns = component.getVisibleColumns();
            List<Object> newColumns = new ArrayList<>();
            // add columns from saved settings
            for (Element colElem : Dom4j.elements(columnsElem, "columns")) {
                for (Object column : oldColumns) {
                    if (column.toString().equals(colElem.attributeValue("id"))) {
                        newColumns.add(column);

                        String width = colElem.attributeValue("width");
                        if (width != null)
                            component.setColumnWidth(column, Integer.valueOf(width));

                        String visible = colElem.attributeValue("visible");
                        if (visible != null) {
                            if (component.isColumnCollapsingAllowed()) { // throws exception if not
                                component.setColumnCollapsed(column, !Boolean.valueOf(visible));
                            }
                        }
                        break;
                    }
                }
            }
            // add columns not saved in settings (perhaps new)
            for (Object column : oldColumns) {
                if (!newColumns.contains(column)) {
                    newColumns.add(column);
                }
            }
            // if the table contains only one column, always show it
            if (newColumns.size() == 1) {
                if (component.isColumnCollapsingAllowed()) { // throws exception if not
                    component.setColumnCollapsed(newColumns.get(0), false);
                }
            }

            component.setVisibleColumns(newColumns.toArray());

            if (isSortable()) {
                //apply sorting
                String sortProp = columnsElem.attributeValue("sortProperty");
                if (!StringUtils.isEmpty(sortProp)) {
                    MetaPropertyPath sortProperty = datasource.getMetaClass().getPropertyPath(sortProp);
                    if (newColumns.contains(sortProperty)) {
                        boolean sortAscending = BooleanUtils.toBoolean(columnsElem.attributeValue("sortAscending"));

                        component.setSortContainerPropertyId(null);
                        component.setSortAscending(sortAscending);
                        component.setSortContainerPropertyId(sortProperty);
                    }
                } else {
                    component.setSortContainerPropertyId(null);
                }
            }
        }
    }

    @Override
    public boolean saveSettings(Element element) {
        Element columnsElem = element.element("columns");
        if (columnsElem != null)
            element.remove(columnsElem);
        columnsElem = element.addElement("columns");

        Object[] visibleColumns = component.getVisibleColumns();
        for (Object column : visibleColumns) {
            Element colElem = columnsElem.addElement("columns");
            colElem.addAttribute("id", column.toString());

            int width = component.getColumnWidth(column);
            if (width > -1)
                colElem.addAttribute("width", String.valueOf(width));

            Boolean visible = !component.isColumnCollapsed(column);
            colElem.addAttribute("visible", visible.toString());
        }

        MetaPropertyPath sortProperty = (MetaPropertyPath) component.getSortContainerPropertyId();
        if (sortProperty != null) {
            Boolean sortAscending = component.isSortAscending();

            columnsElem.addAttribute("sortProperty", sortProperty.toString());
            columnsElem.addAttribute("sortAscending", sortAscending.toString());
        }

        return true;
    }

    @Override
    public void setEnterPressAction(Action action) {
        enterPressAction = action;
    }

    @Override
    public Action getEnterPressAction(){
        return enterPressAction;
    }

    @Override
    public void setItemClickAction(Action action) {
        if (itemClickAction != null) {
            removeAction(itemClickAction);
        }
        itemClickAction = action;
        if (!getActions().contains(action)) {
            addAction(action);
        }
    }

    @Override
    public Action getItemClickAction() {
        return itemClickAction;
    }

    public String getCaption() {
        return component.getCaption();
    }

    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    @Override
    public void setMultiSelect(boolean multiselect) {
        component.setNullSelectionAllowed(multiselect);
        super.setMultiSelect(multiselect);
    }

    @Override
    public ButtonsPanel getButtonsPanel() {
        return buttonsPanel;
    }

    @Override
    public void setButtonsPanel(ButtonsPanel panel) {
        if (buttonsPanel != null && topPanel != null) {
            topPanel.removeComponent(WebComponentsHelper.unwrap(buttonsPanel));
        }
        buttonsPanel = panel;
        if (panel != null) {
            if (topPanel == null) {
                topPanel = new HorizontalLayout();
                topPanel.setWidth("100%");
                componentComposition.addComponentAsFirst(topPanel);
            }
            topPanel.addComponent(WebComponentsHelper.unwrap(panel));
        }
    }

    @Override
    public void addGeneratedColumn(String columnId, ColumnGenerator generator) {
        if (columnId == null)
            throw new IllegalArgumentException("columnId is null");
        if (generator == null)
            throw new IllegalArgumentException("generator is null");

        MetaPropertyPath targetCol = getDatasource().getMetaClass().getPropertyPath(columnId);
        Object generatedColumnId = targetCol != null ? targetCol : columnId;

        // replace generator for column if exist
        if (component.getColumnGenerator(generatedColumnId) != null)
            component.removeGeneratedColumn(generatedColumnId);

        component.addGeneratedColumn(
                generatedColumnId,
                new CustomColumnGenerator(generator) {
                    @Override
                    public com.vaadin.ui.Component generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
                        Entity entity = getDatasource().getItem(itemId);
                        com.haulmont.cuba.gui.components.Component component = getColumnGenerator().generateCell(entity);
                        if (component == null)
                            return null;
                        else {
                            com.vaadin.ui.Component vComponent = WebComponentsHelper.unwrap(component);
                            // wrap field for show required asterisk
                            if ((vComponent instanceof com.vaadin.ui.Field)
                                && (((com.vaadin.ui.Field) vComponent).isRequired())) {
                                VerticalLayout layout = new VerticalLayout();
                                layout.addComponent(vComponent);
                                vComponent = layout;
                            }
                            return vComponent;
                        }
                    }
                }
        );
    }

    @Override
    public void addGeneratedColumn(String columnId, ColumnGenerator generator,
                                   Class<? extends com.haulmont.cuba.gui.components.Component> componentClass) {
        // web ui doesn't make any improvements with componentClass known
        addGeneratedColumn(columnId, generator);
    }

    @Override
    public void removeGeneratedColumn(String columnId) {
        MetaPropertyPath targetCol = getDatasource().getMetaClass().getPropertyPath(columnId);
        removeGeneratedColumn(targetCol == null ? columnId : targetCol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void repaint() {
        if (datasource != null) {
            com.vaadin.data.Container ds = component.getContainerDataSource();

            final Collection<MetaPropertyPath> propertyIds = (Collection<MetaPropertyPath>) ds.getContainerPropertyIds();
            // added generated columns
            final List<Pair<Object, com.vaadin.ui.Table.ColumnGenerator>> columnGenerators = new LinkedList<>();

            for (final MetaPropertyPath id : propertyIds) {
                com.vaadin.ui.Table.ColumnGenerator generator = component.getColumnGenerator(id);
                if (generator != null && !(generator instanceof WebAbstractTable.SystemTableColumnGenerator)) {
                    columnGenerators.add(new Pair<Object, com.vaadin.ui.Table.ColumnGenerator>(id, generator));
                }
            }

            refreshColumns(ds);

            // restore generated columns
            for (Pair<Object, com.vaadin.ui.Table.ColumnGenerator> generatorEntry : columnGenerators) {
                component.addGeneratedColumn(generatorEntry.getFirst(), generatorEntry.getSecond());
            }
        }
        component.markAsDirtyRecursive();
    }

    @Override
    public void selectAll() {
        if (isMultiSelect()) {
            component.setValue(component.getItemIds());
        }
    }

    protected Map<Object, Object> __aggregate(AggregationContainer container, AggregationContainer.Context context) {
        final List<AggregationInfo> aggregationInfos = new LinkedList<>();
        for (final Object o : container.getAggregationPropertyIds()) {
            final MetaPropertyPath propertyId = (MetaPropertyPath) o;
            final Table.Column column = columns.get(propertyId);
            if (column.getAggregation() != null) {
                aggregationInfos.add(column.getAggregation());
            }
        }
        Map<Object, Object> results = ((CollectionDatasource.Aggregatable) datasource).aggregate(
                aggregationInfos.toArray(new AggregationInfo[aggregationInfos.size()]),
                context.getItemIds()
        );
        if (aggregationCells != null) {
            results = __handleAggregationResults(context, results);
        }
        return results;
    }

    protected Map<Object, Object> __handleAggregationResults(AggregationContainer.Context context, Map<Object, Object> results) {
        for (final Map.Entry<Object, Object> entry : results.entrySet()) {
            final Table.Column column = columns.get(entry.getKey());
            com.vaadin.ui.Label cell;
            if ((cell = (com.vaadin.ui.Label) aggregationCells.get(column)) != null) {
                WebComponentsHelper.setLabelText(cell, entry.getValue(), column.getFormatter());
                entry.setValue(cell);
            }
        }
        return results;
    }

    protected class TablePropertyWrapper extends PropertyWrapper {

        private ValueChangeListener calcListener;
        private static final long serialVersionUID = -7942046867909695346L;

        public TablePropertyWrapper(Object item, MetaPropertyPath propertyPath) {
            super(item, propertyPath);
        }

        @Override
        public void addListener(ValueChangeListener listener) {
            super.addListener(listener);
            //A listener of a calculatable property must be only one
            if (listener instanceof CalculatablePropertyValueChangeListener) {
                if (this.calcListener != null) {
                    removeListener(calcListener);
                }
                calcListener = listener;
            }
        }

        @Override
        public void removeListener(ValueChangeListener listener) {
            super.removeListener(listener);
            if (calcListener == listener) {
                calcListener = null;
            }
        }

        @Override
        public boolean isReadOnly() {
            final Table.Column column = WebAbstractTable.this.columns.get(propertyPath);
            if (column != null) {
                return !editable || !(BooleanUtils.isTrue(column.isEditable()) || BooleanUtils.isTrue(column.isCalculatable()));
            } else {
                return super.isReadOnly();
            }
        }

        @Override
        public void setReadOnly(boolean newStatus) {
            super.setReadOnly(newStatus);
        }

        @Override
        public String getFormattedValue() {
            final Table.Column column = WebAbstractTable.this.columns.get(propertyPath);
            if (column != null) {
                if (column.getFormatter() != null) {
                    return column.getFormatter().format(getValue());
                } else if (column.getXmlDescriptor() != null) {
                    String captionProperty = column.getXmlDescriptor().attributeValue("captionProperty");
                    if (!StringUtils.isEmpty(captionProperty)) {
                        final Object value = getValue();
                        return this.propertyPath.getRange().isDatatype() ?
                                this.propertyPath.getRange().asDatatype().format(value) :
                                value != null
                                        ? String.valueOf(((Instance) value).getValue(captionProperty))
                                        : null;
                    }
                }
            }
            return super.getFormattedValue();
        }
    }

    private interface SystemTableColumnGenerator extends com.vaadin.ui.Table.ColumnGenerator {
    }

    protected static abstract class CustomColumnGenerator implements com.vaadin.ui.Table.ColumnGenerator {

        private ColumnGenerator columnGenerator;

        protected CustomColumnGenerator(ColumnGenerator columnGenerator) {
            this.columnGenerator = columnGenerator;
        }

        public ColumnGenerator getColumnGenerator() {
            return columnGenerator;
        }
    }

    protected abstract class LinkGenerator implements SystemTableColumnGenerator {
        protected Table.Column column;

        public LinkGenerator(Table.Column column) {
            this.column = column;
        }

        public com.vaadin.ui.Component generateCell(AbstractSelect source, final Object itemId, Object columnId) {
            final Item item = source.getItem(itemId);
            final Property property = item.getItemProperty(columnId);
            final Object value = property.getValue();

            final com.vaadin.ui.Button component = new com.vaadin.ui.Button();
            component.setData(value);
            component.setCaption(value == null ? "" : property.toString());
            component.setStyleName("link");

            component.addClickListener(new com.vaadin.ui.Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    final Element element = column.getXmlDescriptor();

                    final String clickAction = element.attributeValue("clickAction");
                    if (!StringUtils.isEmpty(clickAction)) {

                        if (clickAction.startsWith("open:")) {
                            final com.haulmont.cuba.gui.components.IFrame frame = WebAbstractTable.this.getFrame();
                            String screenName = clickAction.substring("open:".length()).trim();
                            final Window window = frame.openEditor(screenName, getItem(item, property), WindowManager.OpenType.THIS_TAB);

                            window.addListener(new Window.CloseListener() {
                                @Override
                                public void windowClosed(String actionId) {
                                    if (Window.COMMIT_ACTION_ID.equals(actionId) && window instanceof Window.Editor) {
                                        Object item = ((Window.Editor) window).getItem();
                                        if (item instanceof Entity) {
                                            datasource.updateItem((Entity) item);
                                        }
                                    }
                                }
                            });
                        } else if (clickAction.startsWith("invoke:")) {
                            final com.haulmont.cuba.gui.components.IFrame frame = WebAbstractTable.this.getFrame();
                            String methodName = clickAction.substring("invoke:".length()).trim();
                            try {
                                IFrame controllerFrame = WebComponentsHelper.getControllerFrame(frame);
                                Method method = controllerFrame.getClass().getMethod(methodName, Object.class);
                                method.invoke(controllerFrame, getItem(item, property));
                            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                throw new RuntimeException("Unable to invoke clickAction", e);
                            }

                        } else {
                            throw new UnsupportedOperationException("Unsupported clickAction format: " + clickAction);
                        }
                    }
                }
            });

            return component;
        }

        @Override
        public com.vaadin.ui.Component generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
            return generateCell(((AbstractSelect) source), itemId, columnId);
        }

        protected abstract Entity getItem(Item item, Property property);
    }

    protected class ReadOnlyAssociationGenerator extends LinkGenerator {
        public ReadOnlyAssociationGenerator(Table.Column column) {
            super(column);
        }

        @Override
        protected Entity getItem(Item item, Property property) {
            return (Entity) property.getValue();
        }
    }

    protected class CodePropertyGenerator extends LinkGenerator {
        public CodePropertyGenerator(Table.Column column) {
            super(column);
        }

        @Override
        protected Entity getItem(Item item, Property property) {
            return ((ItemWrapper) item).getItem();
        }
    }

    protected class ReadOnlyBooleanDatatypeGenerator implements SystemTableColumnGenerator {
        @Override
        public com.vaadin.ui.Component generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
            return generateCell((AbstractSelect) source, itemId, columnId);
        }

        protected com.vaadin.ui.Component generateCell(AbstractSelect source, Object itemId, Object columnId) {
            final Property property = source.getItem(itemId).getItemProperty(columnId);
            final Object value = property.getValue();

            CubaPlaceHolder checkPoxImage = new CubaPlaceHolder();
            checkPoxImage.setSizeUndefined();
            if (BooleanUtils.isTrue((Boolean) value))
                checkPoxImage.setStyleName("checkbox-checked");
            else
                checkPoxImage.setStyleName("checkbox-unchecked");

            return checkPoxImage;
        }
    }

    protected class CalculatableColumnGenerator implements SystemTableColumnGenerator {
        @Override
        public com.vaadin.ui.Component generateCell(com.vaadin.ui.Table source, Object itemId, Object columnId) {
            return generateCell((AbstractSelect) source, itemId, columnId);
        }

        protected com.vaadin.ui.Component generateCell(AbstractSelect source, Object itemId, Object columnId) {
            CollectionDatasource ds = WebAbstractTable.this.getDatasource();
            MetaPropertyPath propertyPath = ds.getMetaClass().getPropertyPath(columnId.toString());

            PropertyWrapper propertyWrapper = (PropertyWrapper) source.getContainerProperty(itemId, propertyPath);

            com.haulmont.cuba.gui.components.Formatter formatter = null;
            Table.Column column = WebAbstractTable.this.getColumn(columnId.toString());
            if (column != null) {
                formatter = column.getFormatter();
            }

            final com.vaadin.ui.Label label = new com.vaadin.ui.Label();
            WebComponentsHelper.setLabelText(label, propertyWrapper.getValue(), formatter);
            label.setWidth("-1px");

            //add property change listener that will update a label value
            propertyWrapper.addListener(new CalculatablePropertyValueChangeListener(label, formatter));

            return label;
        }
    }

    protected static class CalculatablePropertyValueChangeListener implements Property.ValueChangeListener {
        private Label component;
        private com.haulmont.cuba.gui.components.Formatter formatter;

        private static final long serialVersionUID = 8041384664735759397L;

        private CalculatablePropertyValueChangeListener(Label component, com.haulmont.cuba.gui.components.Formatter formatter) {
            this.component = component;
            this.formatter = formatter;
        }

        @Override
        public void valueChange(Property.ValueChangeEvent event) {
            WebComponentsHelper.setLabelText(component, event.getProperty().getValue(), formatter);
        }
    }

    protected void addAggregationCell(Table.Column column) {
        if (aggregationCells == null) {
            aggregationCells = new HashMap<>();
        }
        aggregationCells.put(column, createAggregationCell());
    }

    protected com.vaadin.ui.Label createAggregationCell() {
        com.vaadin.ui.Label label = new com.vaadin.ui.Label();
        label.setWidth("-1px");
        label.setParent(component);
        return label;
    }

    protected CollectionDatasourceListener createAggregationDatasourceListener() {
        return new AggregationDatasourceListener();
    }

    protected class AggregationDatasourceListener extends CollectionDsListenerAdapter<Entity> {

        @Override
        public void valueChanged(Entity source, String property, Object prevValue, Object value) {
//            vaadin7
//            final CollectionDatasource ds = WebAbstractTable.this.getDatasource();
//            component.aggregate(new AggregationContainer.Context(ds.getItemIds()));
        }
    }

    protected class FieldFactory extends AbstractFieldFactory {
        @Override
        protected Datasource getDatasource() {
            return datasource;
        }

        @Override
        protected CollectionDatasource getOptionsDatasource(MetaClass metaClass, MetaPropertyPath propertyPath) {
            return WebAbstractTable.this.getOptionsDatasource(metaClass, columns.get(propertyPath));
        }

        @Override
        protected Collection<Field.Validator> getValidators(MetaPropertyPath propertyPath) {
            return validatorsMap.get(columns.get(propertyPath));
        }

        @Override
        protected boolean required(MetaPropertyPath propertyPath) {
            return requiredColumns.containsKey(columns.get(propertyPath));
        }

        @Override
        protected String requiredMessage(MetaPropertyPath propertyPath) {
            return requiredColumns.get(columns.get(propertyPath));
        }

        @Override
        protected Formatter getFormatter(MetaPropertyPath propertyPath) {
            Table.Column column = columns.get(propertyPath);
            return column.getFormatter();
        }

        @Override
        protected String getFormat(MetaPropertyPath propertyPath) {
            Table.Column column = columns.get(propertyPath);
            Element formatterElement = column.getXmlDescriptor().element("formatter");
            return formatterElement.attributeValue("format");
        }

        @Override
        protected String fieldType(MetaPropertyPath propertyPath) {
            return null; //todo gorodnov: implement this method
        }

        @Override
        protected Element getXmlDescriptor(MetaPropertyPath propertyPath) {
            Table.Column column = columns.get(propertyPath);
            return column.getXmlDescriptor();
        }

        @Override
        protected void initCommon(com.vaadin.ui.Field field, Field cubaField, MetaPropertyPath propertyPath) {
            super.initCommon(field, cubaField, propertyPath);

            final Table.Column column = columns.get(propertyPath);
            final MetaProperty metaProperty;
            if (column.getId() != null) {
                metaProperty = ((MetaPropertyPath) column.getId()).getMetaProperty();
            } else {
                metaProperty = null;
            }

            if (field instanceof com.vaadin.ui.TextField) {
                initTextField((com.vaadin.ui.TextField) field, metaProperty, column.getXmlDescriptor());
            }

            if (cubaField instanceof WebDateField) {
                initDateField(field, metaProperty, column.getXmlDescriptor());
            }

            if (field instanceof CheckBox) {
//                vaadin7
//                ((CheckBox) field).setLayoutCaption(true);
            }
        }
    }

    protected boolean handleSpecificVariables(Map<String, Object> variables) {
        boolean needReload = false;

        if (isUsePresentations()) {

            final Presentations p = getPresentations();

            if (p.getCurrent() != null && p.isAutoSave(p.getCurrent()) && needUpdatePresentation(variables)) {
                Element e = p.getSettings(p.getCurrent());
                saveSettings(e);
                p.setSettings(p.getCurrent(), e);
            }
        }

        return needReload;
    }

    private boolean needUpdatePresentation(Map<String, Object> variables) {
        return variables.containsKey("colwidth") || variables.containsKey("sortcolumn")
                || variables.containsKey("sortascending") || variables.containsKey("columnorder")
                || variables.containsKey("collapsedcolumns") || variables.containsKey("groupedcolumns");
    }

    protected void paintSpecificContent(PaintTarget target) throws PaintException {
//        vaadin7
//        target.addVariable(component, "presentations", isUsePresentations());
//        if (isUsePresentations()) {
//            target.startTag("presentations");
//            tablePresentations.paint(target);
//            target.endTag("presentations");
//        }
    }

    @Override
    public List<Table.Column> getNotCollapsedColumns() {
        final List<Table.Column> visibleColumns = new ArrayList<>(component.getVisibleColumns().length);
        Object[] keys = component.getVisibleColumns();
        for (final Object key : keys) {
            if (!component.isColumnCollapsed(key)) {
                visibleColumns.add(columns.get(key));
            }
        }
        return visibleColumns;
    }

    @Override
    public void usePresentations(boolean use) {
        usePresentations = use;
    }

    @Override
    public boolean isUsePresentations() {
        return usePresentations;
    }

    @Override
    public void loadPresentations() {
        if (isUsePresentations()) {
            presentations = new PresentationsImpl(this);

//        vaadin7
//            tablePresentations = new TablePresentations(this);
        } else {
            throw new UnsupportedOperationException("Component doesn't use presentations");
        }
    }

    @Override
    public Presentations getPresentations() {
        if (isUsePresentations()) {
            return presentations;
        } else {
            throw new UnsupportedOperationException("Component doesn't use presentations");
        }
    }

    @Override
    public void applyPresentation(Object id) {
        if (isUsePresentations()) {
            Presentation p = presentations.getPresentation(id);
            applyPresentation(p);
        } else {
            throw new UnsupportedOperationException("Component doesn't use presentations");
        }
    }

    @Override
    public void applyPresentationAsDefault(Object id) {
        if (isUsePresentations()) {
            Presentation p = presentations.getPresentation(id);
            if (p != null) {
                presentations.setDefault(p);
                applyPresentation(p);
            }
        } else {
            throw new UnsupportedOperationException("Component doesn't use presentations");
        }
    }

    protected void applyPresentation(Presentation p) {
        presentations.setCurrent(p);
        Element settingsElement  = presentations.getSettings(p);
        applySettings(settingsElement);
        component.markAsDirty();
    }

    @Override
    public Object getDefaultPresentationId() {
//        vaadin7
//        Presentation def = presentations.getDefault();
        return null; // def == null ? null : def.getId();
    }

    @Override
    public void addColumnCollapsedListener(ColumnCollapseListener columnCollapsedListener) {
        columnCollapseListeners.add(columnCollapsedListener);
    }

    @Override
    public void removeColumnCollapseListener(ColumnCollapseListener columnCollapseListener) {
        columnCollapseListeners.remove(columnCollapseListener);
    }
}