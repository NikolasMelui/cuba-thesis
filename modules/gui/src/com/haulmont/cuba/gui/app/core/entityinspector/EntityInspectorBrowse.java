/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.core.entityinspector;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Session;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ExcelAction;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.actions.RefreshAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.CollectionDatasourceImpl;
import com.haulmont.cuba.gui.data.impl.GenericDataService;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.*;

/**
 * @author korotkov
 * @version $Id$
 */
public class EntityInspectorBrowse extends AbstractLookup {

    public static final String SCREEN_NAME = "entityInspector.browse";
    public static final WindowManager.OpenType WINDOW_OPEN_TYPE = WindowManager.OpenType.THIS_TAB;
    public static final String DEFAULT_FIELD_WIDTH = "300";

    @Inject
    private Metadata metadata;

    @Inject
    private Messages messages;

    @Inject
    protected BoxLayout lookupBox;

    @Inject
    protected BoxLayout tableBox;

    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected DsContext dsContext;

    @Inject
    protected Filter filter;

    protected LookupField entities;
    protected Button showButton;
    protected Table entitiesTable;

    /**
     * Buttons
     */
    protected Button createButton;
    protected Button editButton;
    protected Button removeButton;
    protected Button excelButton;
    protected Button refreshButton;

    protected CollectionDatasource entitiesDs;
    protected MetaClass selectedMeta;

    public EntityInspectorBrowse(IFrame frame) {
        super(frame);
    }

    @Override
    public void init(Map<String, Object> params) {
        String entityName = (String) params.get("entity");
        if (entityName != null) {
            Session session = metadata.getSession();
            selectedMeta = session.getClass(entityName);
            createEntitiesTable(selectedMeta);
            if (frame instanceof Lookup)
                setLookupComponent(entitiesTable);
        } else {
            entities = componentsFactory.createComponent(LookupField.NAME);
            entities.setWidth(DEFAULT_FIELD_WIDTH);
            entities.setOptionsMap(getEntitiesLookupFieldOptions());

            showButton = componentsFactory.createComponent(Button.NAME);
            showButton.setIcon("icons/refresh.png");
            showButton.setAction(new ShowButtonAction("show"));
            showButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "show"));

            lookupBox.add(entities);
            lookupBox.add(showButton);
        }
    }

    protected Map<String, Object> getEntitiesLookupFieldOptions() {
        Map<String, Object> options = new TreeMap<String, Object>();

        for (MetaClass metaClass : metadata.getTools().getAllPersistentMetaClasses()) {
            if (readPermitted(metaClass))
                options.put(messages.getTools().getEntityCaption(metaClass) + " (" + metaClass.getName() + ")", metaClass);
        }

        return options;
    }

    private class ShowButtonAction extends AbstractAction {
        protected ShowButtonAction(String id) {
            super(id);
        }

        @Override
        public void actionPerform(Component component) {
            selectedMeta = entities.getValue();
            if (selectedMeta != null) {
                createEntitiesTable(selectedMeta);

                //TODO: set tab caption
                //EntityInspectorBrowse.this.setCaption(selectedMeta.getName());
            }
        }
    }

    private void createEntitiesTable(MetaClass meta) {
        if (entitiesTable != null)
            tableBox.remove(entitiesTable);

        entitiesTable = componentsFactory.createComponent(Table.NAME);
        entitiesTable.setFrame(frame);

        //collect properties in order to add non-system columns first
        LinkedList<Table.Column> nonSystemPropertyColumns = new LinkedList<Table.Column>();
        LinkedList<Table.Column> systemPropertyColumns = new LinkedList<Table.Column>();
        for (MetaProperty metaProperty : meta.getProperties()) {
            //don't show embedded & multiple referred entities
            if (isEmbedded(metaProperty))
                continue;
            if (metaProperty.getRange().getCardinality().isMany())
                continue;

            Table.Column column = new Table.Column(meta.getPropertyPath(metaProperty.getName()));
            if (!metadata.getTools().isSystem(metaProperty)) {
                column.setCaption(getPropertyCaption(meta, metaProperty));
                nonSystemPropertyColumns.add(column);
            } else {
                column.setCaption(metaProperty.getName());
                systemPropertyColumns.add(column);
            }
        }
        for (Table.Column column : nonSystemPropertyColumns)
            entitiesTable.addColumn(column);

        for (Table.Column column : systemPropertyColumns)
            entitiesTable.addColumn(column);

        View view = createView(meta);
        entitiesDs = new CollectionDatasourceImpl<Entity<UUID>, UUID>(getDsContext(),
                new GenericDataService(), "entitiesDs", meta, view);
        entitiesDs.setQuery(String.format("select e from %s e", meta.getName()));
        entitiesDs.refresh();
        entitiesTable.setDatasource(entitiesDs);

        tableBox.add(entitiesTable);
        entitiesTable.setWidth("100%");
        tableBox.expand(entitiesTable);

        ButtonsPanel buttonsPanel = createButtonsPanel();
        entitiesTable.setButtonsPanel(buttonsPanel);

        RowsCount rowsCount = componentsFactory.createComponent(RowsCount.NAME);
        rowsCount.setDatasource(entitiesDs);
        entitiesTable.setRowsCount(rowsCount);

        Action editAction = new EditAction();
        entitiesTable.setEnterPressAction(editAction);
        entitiesTable.setItemClickAction(editAction);
        entitiesTable.setMultiSelect(true);
        filter.setDatasource(entitiesDs);
        filter.setVisible(true);
        filter.apply(true);
    }

    private boolean isEmbedded(MetaProperty metaProperty) {
        return metaProperty.getAnnotatedElement().isAnnotationPresent(javax.persistence.Embedded.class);
    }

    private ButtonsPanel createButtonsPanel() {
        ButtonsPanel buttonsPanel = componentsFactory.createComponent("buttonsPanel");

        createButton = componentsFactory.createComponent(Button.NAME);
        createButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "create"));
        createButton.setAction(new CreateAction());
        createButton.setIcon("icons/create.png");

        editButton = componentsFactory.createComponent(Button.NAME);
        editButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "edit"));
        EditAction editAction = new EditAction();
        editButton.setAction(editAction);
        entitiesDs.addListener(editAction);
        editButton.setIcon("icons/edit.png");

        removeButton = componentsFactory.createComponent(Button.NAME);
        removeButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "remove"));
        RemoveAction removeAction = new RemoveAction(entitiesTable);
        entitiesDs.addListener(removeAction);
        removeButton.setAction(removeAction);
        removeButton.setIcon("icons/remove.png");

        excelButton = componentsFactory.createComponent(Button.NAME);
        excelButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "excel"));
        excelButton.setAction(new ExcelAction(entitiesTable));
        excelButton.setIcon("icons/excel.png");

        refreshButton = componentsFactory.createComponent(Button.NAME);
        refreshButton.setCaption(MessageProvider.getMessage(EntityInspectorBrowse.class, "refresh"));
        refreshButton.setAction(new RefreshAction(entitiesTable));
        refreshButton.setIcon("icons/refresh.png");

        buttonsPanel.addButton(createButton);
        buttonsPanel.addButton(editButton);
        buttonsPanel.addButton(removeButton);
        buttonsPanel.addButton(excelButton);
        buttonsPanel.addButton(refreshButton);
        return buttonsPanel;
    }


    private View createView(MetaClass meta) {
        View view = new View(meta.getJavaClass(), false);
        for (MetaProperty metaProperty : meta.getProperties()) {
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    view.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case COMPOSITION:
                        View minimal = MetadataProvider.getViewRepository()
                                .getView(metaProperty.getRange().asClass(), View.MINIMAL);
                        View propView = new View(minimal, metaProperty.getName() + "Ds", false);
                        view.addProperty(metaProperty.getName(), propView);
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        return view;
    }

    protected class CreateAction extends AbstractAction {

        protected CreateAction() {
            super("create");
        }

        @Override
        public void actionPerform(Component component) {
            Map<String, Object> editorParams = new HashMap<String, Object>();
            editorParams.put("metaClass", selectedMeta.getName());
            Window window = openWindow("entityInspector.edit", WINDOW_OPEN_TYPE, editorParams);
            window.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    entitiesDs.refresh();
                }
            });
        }
    }

    protected class EditAction extends ItemTrackingAction {

        protected EditAction() {
            super("edit");
        }

        @Override
        public void actionPerform(Component component) {
            Set selected = entitiesTable.getSelected();
            if (selected.size() != 1)
                return;

            Entity item = (Entity) selected.toArray()[0];
            Map<String, Object> editorParams = new HashMap<String, Object>();
            editorParams.put("item", item);
            Window window = openWindow("entityInspector.edit", WINDOW_OPEN_TYPE, editorParams);
            window.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    entitiesDs.refresh();
                }
            });
        }
    }

    private String getPropertyCaption(MetaClass meta, MetaProperty metaProperty) {
        int idx = meta.getName().indexOf('$') + 1;
        return MessageProvider.getMessage(meta.getJavaClass(), meta.getName().substring(idx)
                + "." + metaProperty.getFullName());
    }

    private boolean readPermitted(MetaClass metaClass) {
        return entityOpPermitted(metaClass, EntityOp.READ);
    }

    private boolean entityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
        UserSession session = UserSessionProvider.getUserSession();
        return session.isEntityOpPermitted(metaClass, entityOp);
    }
}
