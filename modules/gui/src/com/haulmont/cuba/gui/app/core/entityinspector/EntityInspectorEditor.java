/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.app.core.entityinspector;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.CategorizedEntity;
import com.haulmont.cuba.core.entity.Category;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.AddAction;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.RuntimePropsDatasource;
import com.haulmont.cuba.gui.data.impl.*;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.openjpa.persistence.jdbc.EmbeddedMapping;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.util.*;

/**
 * @author korotkov
 * @version $Id$
 */
public class EntityInspectorEditor extends AbstractEditor {

    public static final String SCREEN_NAME = "entityInspector.edit";
    public static final String DEFAULT_FIELD_WIDTH = "200";
    public static final int CAPTION_MAX_LENGTH = 100;


    public static final WindowManager.OpenType OPEN_TYPE = WindowManager.OpenType.THIS_TAB;

    @Inject
    protected Metadata metadata;

    @Inject
    protected DataService dataService;

    @Inject
    protected BoxLayout buttonsBox;

    @Inject
    protected BoxLayout contentPane;

    @Inject
    protected BoxLayout tablesBox;

    @Inject
    protected ComponentsFactory componentsFactory;
    protected MetaClass meta;
    protected String parentProperty;

    protected Entity item;
    protected Entity parent;

    protected DsContextImpl dsContext;
    protected Datasource datasource;
    protected Map<String, Datasource> datasources;

    protected Boolean isNew;
    protected Boolean autocommit;
    protected Boolean showSystemFields;
    protected Collection<Table> tables;

    protected RuntimePropsDatasource rDS;
    protected CollectionDatasource categories;
    protected FieldGroup fieldGroup;

    protected LinkedList<FieldGroup.Field> customFields;
    protected ButtonsPanel buttonsPanel;
    protected Button commitButton;
    protected Button cancelButton;

    private boolean categorizedEntity;
    private boolean createRequest;
    private final String TABLE_MAX_HEIGHT;
    private final int TABLE_MAX_ROW;

    public EntityInspectorEditor() {
        super();
        customFields = new LinkedList<FieldGroup.Field>();
        datasources = new HashMap<String, Datasource>();
        tables = new LinkedList<Table>();
        isNew = true;
        TABLE_MAX_HEIGHT = "200px";
        autocommit = true;
        showSystemFields = false;
        TABLE_MAX_ROW = 5;
    }

    @Override
    public void init(Map<String, Object> params) {
        item = (Entity) params.get("item");
        parent = (Entity) params.get("parent");
        parentProperty = (String) params.get("parentProperty");
        datasource = (Datasource) params.get("datasource");
        isNew = item == null || PersistenceHelper.isNew(item);
        meta = item != null ? item.getMetaClass() : metadata.getSession().getClass((String) params.get("metaClass"));
        autocommit = params.get("autocommit") != null ? (Boolean) params.get("autocommit") : true;
        showSystemFields = params.get("showSystemFields") != null ? (Boolean) params.get("showSystemFields") : false;

        if (meta == null)
            throw new IllegalStateException("Entity or entity's MetaClass must be specified");

        View view = createView(meta);

        createRequest = item == null || item.getId() == null;
        if (createRequest) {
            item = MetadataProvider.create(meta);
            createEmbeddedFields(meta, item);
            setParentField(item, parentProperty, parent);
        } else {
            //edit request
            if (!isNew)
                item = loadSingleItem(meta, item.getId(), view);
        }

        categorizedEntity = item instanceof CategorizedEntity;


        dsContext = new DsContextImpl(dataService);
        if (datasource == null) {
            datasource = new DatasourceImpl<Entity>
                    (dsContext, dataService, meta.getName() + "Ds", item.getMetaClass(), view);
            ((DatasourceImpl) datasource).valid();
        }

        dsContext.register(datasource);
        createPropertyDatasources(datasource);
        if (categorizedEntity) {
            initRuntimePropertiesDatasources(view);
        }

        datasource.refresh();

        createDataComponents(meta);
        if (categorizedEntity) {
            createRuntimeDataComponents();
        }

        datasource.setItem(item);
        if (datasource instanceof CollectionDatasource && createRequest) {
            ((CollectionDatasource) datasource).addItem(item);
        }

        if (categorizedEntity) {
            rDS.refresh();
        }

        createCommitButtons();
        setCaption(meta.getName());
        layout();
    }

    private void setParentField(Entity item, String parentProperty, Entity parent) {
        if (parentProperty != null && parent != null && item != null)
            item.setValue(parentProperty, parent);
    }

    private void createRuntimeDataComponents() {
        if (rDS != null && categories != null) {
            RuntimePropertiesFrame runtimePropertiesFrame = new RuntimePropertiesFrame(frame);
            runtimePropertiesFrame.setDsContext(dsContext);
            runtimePropertiesFrame.setMessagesPack("com.haulmont.cuba.gui.app.core.entityinspector");
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("runtimeDs", rDS.getId());
            params.put("categoriesDs", categories.getId());
            params.put("fieldWidth", DEFAULT_FIELD_WIDTH);
            params.put("borderVisible", "true");
            runtimePropertiesFrame.init(params);
            runtimePropertiesFrame.setCategoryFieldVisible(false);
        }
    }

    private void initRuntimePropertiesDatasources(View view) {
        rDS = new RuntimePropsDatasourceImpl(dsContext, dataService, "rDS",
                view.getName(), datasource.getId());
        MetaClass categoriesMeta = metadata.getSession().getClass(Category.class);
        categories = new CollectionDatasourceImpl(dsContext, dataService,
                "categories", categoriesMeta, View.LOCAL);
        categories.setQuery(String.format("select c from sys$Category c where c.entityType='%s'", meta.getName()));
        categories.refresh();
        dsContext.register(rDS);
        dsContext.register(categories);
    }

    /**
     * Recursively instantiates the embedded properties.
     * E.g. embedded properties of the embedded property will also be instantiated.
     *
     * @param metaClass meta class of the entity
     * @param item      entity instance
     */
    private void createEmbeddedFields(MetaClass metaClass, Entity item) {
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (isEmbedded(metaProperty)) {
                Entity embedded;
                MetaClass embeddedMetaClass = metaProperty.getRange().asClass();
                if (item.getValue(metaProperty.getName()) != null)
                    continue;
                try {
                    embedded = embeddedMetaClass.createInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException("cannot create instance of the embedded property", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                createEmbeddedFields(embeddedMetaClass, embedded);
                item.setValue(metaProperty.getName(), embedded);
            }
        }
    }

    /**
     * Returns metaProperty of the referred entity annotated with either nullIndicatorAttributeName or
     * nullIndicatorColumnName property.
     *
     * @param embeddedMetaProperty embedded property of the current entity
     * @return property of the referred entity
     */
    private MetaProperty getNullIndicatorProperty(MetaProperty embeddedMetaProperty) {
        EmbeddedMapping embeddedMapping = embeddedMetaProperty.getAnnotatedElement().getAnnotation(EmbeddedMapping.class);

        if (embeddedMapping == null)
            return null;

        MetaClass meta = embeddedMetaProperty.getRange().asClass();

        String attributeName = embeddedMapping.nullIndicatorAttributeName();
        String columnName = embeddedMapping.nullIndicatorColumnName();

        if (!isEmpty(attributeName))
            return meta.getProperty(attributeName);
        else if (!isEmpty(columnName))
            return findPropertyByMappedColumn(meta, columnName);
        else
            return null;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private MetaProperty findPropertyByMappedColumn(MetaClass meta, String columnName) {
        for (MetaProperty metaProperty : meta.getProperties()) {
            Column columnAnn = metaProperty.getAnnotatedElement().getAnnotation(Column.class);
            if (columnAnn == null)
                continue;
            String name = columnAnn.name();
            if (name == null)
                continue;
            if (name.equals(columnName))
                return metaProperty;
        }
        return null;
    }

    /**
     * Checks if the property is embedded
     *
     * @param metaProperty meta property
     * @return true if embedded, false otherwise
     */
    private boolean isEmbedded(MetaProperty metaProperty) {
        return metaProperty.getAnnotatedElement().isAnnotationPresent(javax.persistence.Embedded.class);
    }

    /**
     * Loads single item by id
     *
     * @param meta item's meta class
     * @param id   item's id
     * @param view view
     * @return loaded item if found, null otherwise
     */
    private Entity loadSingleItem(MetaClass meta, Object id, View view) {
        LoadContext ctx = new LoadContext(meta);
        ctx.setView(view);
        String query = String.format("select e from %s e where e.id = :id", meta.getName());
        LoadContext.Query q = ctx.setQueryString(query);
        q.addParameter("id", id);
        return dataService.load(ctx);
    }

    /**
     * Creates components representing item data
     * (fieldGroup, fieldGroups for embedded properties, tables for the referred entities)
     *
     * @param metaClass item meta class
     */
    private void createDataComponents(MetaClass metaClass) {
        fieldGroup = componentsFactory.createComponent(FieldGroup.NAME);
        contentPane.add(fieldGroup);
        fieldGroup.setFrame(frame);
        boolean custom;
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            boolean isRequired = isRequired(metaProperty);
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    //skip system properties
                    if (MetadataHelper.isSystem(metaProperty) && !showSystemFields) {
                        continue;
                    }
                    custom = false;
                    addField(metaClass, metaProperty, fieldGroup, isRequired, custom, customFields);
                    break;
                case AGGREGATION:
                case ASSOCIATION:
                    if (metaProperty.getRange().getCardinality().isMany()) {
                        addTable(metaProperty);
                    } else {
                        if (isEmbedded(metaProperty))
                            addEmbeddedFieldGroup(metaProperty);
                        else {
                            custom = true;
                            addField(metaClass, metaProperty, fieldGroup, isRequired, custom, customFields);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        fieldGroup.setDatasource(datasource);
        createCustomFields(fieldGroup, customFields);
        fieldGroup.setBorderVisible(true);
    }

    /**
     * Creates field group for the embedded property
     *
     * @param embeddedMetaProperty meta property of the embedded property
     */
    private void addEmbeddedFieldGroup(MetaProperty embeddedMetaProperty) {
        MetaProperty nullIndicatorProperty = getNullIndicatorProperty(embeddedMetaProperty);
        Datasource embedDs = datasources.get(embeddedMetaProperty.getName());
        FieldGroup fieldGroup = componentsFactory.createComponent(FieldGroup.NAME);
        contentPane.add(fieldGroup);
        fieldGroup.setFrame(frame);
        fieldGroup.setCaption(getPropertyCaption(meta, embeddedMetaProperty));
        MetaClass embeddableMetaClass = embeddedMetaProperty.getRange().asClass();
        Collection<FieldGroup.Field> customFields = new LinkedList<FieldGroup.Field>();
        boolean custom;
        for (MetaProperty metaProperty : embeddableMetaClass.getProperties()) {
            boolean isRequired = isRequired(metaProperty) || metaProperty.equals(nullIndicatorProperty);
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    //skip system properties
                    if (MetadataHelper.isSystem(metaProperty) && !showSystemFields) {
                        continue;
                    }
                    custom = false;
                    addField(embeddableMetaClass, metaProperty, fieldGroup, isRequired, custom, customFields);
                    break;
                case AGGREGATION:
                case ASSOCIATION:
                    if (metaProperty.getRange().getCardinality().isMany()) {
                        throw new IllegalStateException("tables for the embeddable entities are not supported");
                    } else {
                        if (isEmbedded(metaProperty)) {
                            addEmbeddedFieldGroup(metaProperty);
                        } else {
                            custom = true;
                            addField(embeddableMetaClass, metaProperty, fieldGroup, isRequired, custom, customFields);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        fieldGroup.setDatasource(embedDs);
        fieldGroup.setBorderVisible(true);
    }

    private boolean isRequired(MetaProperty metaProperty) {
        if (metaProperty.isMandatory())
            return true;

        ManyToOne many2One = metaProperty.getAnnotatedElement().getAnnotation(ManyToOne.class);
        if (many2One != null && !many2One.optional())
            return true;

        OneToOne one2one = metaProperty.getAnnotatedElement().getAnnotation(OneToOne.class);
        if (one2one != null && !one2one.optional())
            return true;

        return false;
    }

    /**
     * Creates and registers in dsContext property datasource for each of the entity non-datatype
     * and non-enum property
     *
     * @param masterDs master datasource
     */
    private void createPropertyDatasources(Datasource masterDs) {
        for (MetaProperty metaProperty : meta.getProperties()) {
            switch (metaProperty.getType()) {
                case AGGREGATION:
                case ASSOCIATION:
                    Datasource propertyDs;
                    if (metaProperty.getRange().getCardinality().isMany()) {
                        propertyDs = new CollectionPropertyDatasourceImpl(metaProperty.getName() + "Ds",
                                masterDs, metaProperty.getName());
                    } else {
                        if (isEmbedded(metaProperty)) {
                            propertyDs = new EmbeddedDatasourceImpl(metaProperty.getName() + "Ds",
                                    masterDs, metaProperty.getName());
                        } else {
                            propertyDs = new PropertyDatasourceImpl(metaProperty.getName() + "Ds",
                                    masterDs, metaProperty.getName());
                        }
                    }
                    datasources.put(metaProperty.getName(), propertyDs);
                    dsContext.register(propertyDs);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Tunes up layout analysing created components
     */
    private void layout() {
        //TODO: web & desktop layouts conflict: vbox("-1") + table("-1")
//        for (Table table : tables)
//            if (table.getDatasource().size() <= TABLE_MAX_ROW)
//                table.setHeight("-1");
//            else
//                table.setHeight(TABLE_MAX_HEIGHT);
    }

    private void createCommitButtons() {
        buttonsPanel = componentsFactory.createComponent(ButtonsPanel.NAME);
        commitButton = componentsFactory.createComponent(Button.NAME);
        commitButton.setIcon("icons/ok.png");
        commitButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "commit"));
        commitButton.setAction(new CommitAction());
        cancelButton = componentsFactory.createComponent(Button.NAME);
        cancelButton.setIcon("icons/cancel.png");
        cancelButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "cancel"));
        cancelButton.setAction(new CancelAction());
        buttonsPanel.add(commitButton);
        buttonsPanel.add(cancelButton);
        buttonsBox.add(buttonsPanel);
    }

    /**
     * Adds field to the specified field group.
     * If the field should be custom, adds it to the specified customFields collection
     * which can be used later to create fieldGenerators
     *
     * @param meta         meta class of item
     * @param metaProperty meta property of the item's property which field is creating
     * @param fieldGroup   field group to which created field will be added
     * @param customFields if the field is custom it will be added to this collection
     * @param required     true if the field is required
     * @param custom       true if the field is custom
     */
    private void addField(MetaClass meta, MetaProperty metaProperty,
                          FieldGroup fieldGroup, boolean required, boolean custom,
                          Collection<FieldGroup.Field> customFields) {
        if (!attrViewPermitted(metaProperty))
            return;

        if ((metaProperty.getType() == MetaProperty.Type.AGGREGATION
                || metaProperty.getType() == MetaProperty.Type.ASSOCIATION)
                && !entityOpPermitted(metaProperty.getRange().asClass(), EntityOp.READ))
            return;

        FieldGroup.Field field = new FieldGroup.Field(metaProperty.getName());
        String caption = getPropertyCaption(meta, metaProperty);
        field.setCaption(caption);
        field.setType(metaProperty.getJavaType());
        field.setWidth(DEFAULT_FIELD_WIDTH);
        field.setCustom(custom);
        field.setRequired(required);
        if (required)
            field.setRequiredError("Field " + caption + " is required");
        fieldGroup.addField(field);
        if (custom)
            customFields.add(field);
    }

    /**
     * Checks if specified property is a reference to entity's parent entity.
     * Parent entity can be specified during creating of this screen.
     *
     * @param metaProperty meta property
     * @return true if property references to a parent entity
     */
    private boolean isParentProperty(MetaProperty metaProperty) {
        return parentProperty != null && metaProperty.getName().equals(parentProperty);
    }

    /**
     * Creates custom fields and adds them to the fieldGroup
     */
    private void createCustomFields(FieldGroup fieldGroup, Collection<FieldGroup.Field> customFields) {
        for (FieldGroup.Field field : customFields) {
            //custom field generator creates an pickerField
            fieldGroup.addCustomField(field, new FieldGroup.CustomFieldGenerator() {
                @Override
                public Component generateField(Datasource datasource, Object propertyId) {
                    MetaProperty metaProperty = datasource.getMetaClass().getProperty(propertyId.toString());
                    MetaClass propertyMeta = metaProperty.getRange().asClass();
                    PickerField field = componentsFactory.createComponent(PickerField.NAME);
                    String caption = getPropertyCaption(metaProperty.getDomain(), metaProperty);
                    field.setCaption(caption);

                    PickerField.LookupAction lookupAction = field.addLookupAction();
                    //forwards lookup to the EntityInspectorBrowse window
                    lookupAction.setLookupScreen(EntityInspectorBrowse.SCREEN_NAME);
                    lookupAction.setLookupScreenOpenType(OPEN_TYPE);
                    lookupAction.setLookupScreenParams(Collections.singletonMap("entity", (Object) propertyMeta.getName()));

                    field.addClearAction();
                    //don't lets user to change parent
                    if (isParentProperty(metaProperty)) {
                        //set parent item if it has been retrieved
                        if (parent != null) {
                            if (parent.toString() == null) {
                                initNamePatternFields(parent);
                            }
                            field.setValue(parent);
                        }
                        field.setEditable(false);
                    }
                    field.setDatasource(datasource, propertyId.toString());
                    return field;
                }
            });
        }
    }

    /**
     * Tries to initialize entity fields included in entity name pattern by default values
     *
     * @param entity instance
     */
    private void initNamePatternFields(Entity entity) {
        Collection<MetaProperty> properties = MetadataHelper.getNamePatternProperties(entity.getMetaClass());
        for (MetaProperty property : properties) {
            if (entity.getValue(property.getName()) == null) {
                if (property.getType() == MetaProperty.Type.DATATYPE)
                    try {
                        entity.setValue(property.getName(), property.getJavaType().newInstance());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

    private String getPropertyCaption(MetaClass meta, MetaProperty metaProperty) {
        int idx = meta.getName().indexOf('$') + 1;
        String caption = MessageProvider.getMessage(meta.getJavaClass(), meta.getName().substring(idx)
                + "." + metaProperty.getFullName());
        if (caption.length() < CAPTION_MAX_LENGTH)
            return caption;
        else
            return caption.substring(0, CAPTION_MAX_LENGTH);
    }

    /**
     * Creates a table for the entities in ONE_TO_MANY or MANY_TO_MANY relation with the current one
     */
    private void addTable(MetaProperty childMeta) {
        MetaClass meta = childMeta.getRange().asClass();

        //don't show empty table if the user don't have permissions on the attribute or the entity
        if (!attrViewPermitted(childMeta.getDomain(), childMeta.getName()) ||
                !entityOpPermitted(meta, EntityOp.READ)) {
            return;
        }

        //vertical box for the table and its label
        BoxLayout vbox = componentsFactory.createComponent(BoxLayout.VBOX);
        vbox.setWidth("100%");
        CollectionDatasource propertyDs = (CollectionDatasource) datasources.get(childMeta.getName());

        Label label = componentsFactory.createComponent(Label.NAME);
        label.setValue(getPropertyCaption(childMeta.getDomain(), childMeta));
        label.setStyleName("h2");

        Table table = componentsFactory.createComponent(Table.NAME);
        table.setMultiSelect(true);
        table.setFrame(frame);
        //place non-system properties columns first
        LinkedList<Table.Column> nonSystemPropertyColumns = new LinkedList<Table.Column>();
        LinkedList<Table.Column> systemPropertyColumns = new LinkedList<Table.Column>();
        for (MetaProperty metaProperty : meta.getProperties()) {
            Table.Column column = new Table.Column(meta.getPropertyPath(metaProperty.getName()));
            if (!MetadataHelper.isSystem(metaProperty)) {
                column.setCaption(getPropertyCaption(meta, metaProperty));
                nonSystemPropertyColumns.add(column);
            } else {
                column.setCaption(metaProperty.getName());
                systemPropertyColumns.add(column);
            }
        }
        for (Table.Column column : nonSystemPropertyColumns)
            table.addColumn(column);

        for (Table.Column column : systemPropertyColumns)
            table.addColumn(column);

        //set datasource so we could create a buttons panel
        table.setDatasource(propertyDs);

        //refresh ds to read ds size
        propertyDs.refresh();
        ButtonsPanel propertyButtonsPanel = createButtonsPanel(childMeta, propertyDs, table);
        table.setButtonsPanel(propertyButtonsPanel);

        RowsCount rowsCount = componentsFactory.createComponent(RowsCount.NAME);
        rowsCount.setDatasource(propertyDs);
        table.setRowsCount(rowsCount);
        table.setWidth("100%");
        vbox.setHeight(TABLE_MAX_HEIGHT);
        vbox.add(label);
        vbox.add(table);
        vbox.expand(table);
        tablesBox.add(vbox);
        tables.add(table);
    }

    /**
     * Creates a buttons panel managing table's content.
     *
     * @param metaProperty property representing table's data
     * @param propertyDs   property's Datasource (CollectionPropertyDatasource usually)
     * @param table        table
     * @return buttons panel
     */
    private ButtonsPanel createButtonsPanel(final MetaProperty metaProperty,
                                            final CollectionDatasource propertyDs, Table table) {
        MetaClass propertyMetaClass = metaProperty.getRange().asClass();
        ButtonsPanel propertyButtonsPanel = componentsFactory.createComponent(ButtonsPanel.NAME);

        Button createButton = componentsFactory.createComponent(Button.NAME);
        createButton.setAction(new CreateAction(metaProperty, propertyDs, propertyMetaClass));
        createButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "create"));
        createButton.setIcon("icons/create.png");

        Button addButton = componentsFactory.createComponent(Button.NAME);
        AddAction addAction = createAddAction(metaProperty, propertyDs, table, propertyMetaClass);
        addButton.setAction(addAction);
        addButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "add"));
        addButton.setIcon("icons/add.png");

        Button editButton = componentsFactory.createComponent(Button.NAME);
        EditAction editAction = new EditAction(metaProperty, table, propertyDs);
        propertyDs.addListener(editAction);
        editButton.setAction(editAction);
        editButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "edit"));
        editButton.setIcon("icons/edit.png");
        table.setItemClickAction(editAction);
        table.setEnterPressAction(editAction);

        RemoveAction removeAction = createRemoveAction(metaProperty, table);
        propertyDs.addListener(removeAction);
        Button removeButton = componentsFactory.createComponent(Button.NAME);
        removeButton.setAction(removeAction);
        removeButton.setCaption(MessageProvider.getMessage(EntityInspectorEditor.class, "remove"));
        removeButton.setIcon("icons/remove.png");

        propertyButtonsPanel.addButton(createButton);
        propertyButtonsPanel.addButton(addButton);
        propertyButtonsPanel.addButton(editButton);
        propertyButtonsPanel.addButton(removeButton);
        return propertyButtonsPanel;
    }

    private AddAction createAddAction(MetaProperty metaProperty, CollectionDatasource propertyDs,
                                      Table table, MetaClass propertyMetaClass) {
        Lookup.Handler addHandler = createAddHandler(metaProperty, propertyDs);
        AddAction addAction = new AddAction(table, addHandler, OPEN_TYPE);
        addAction.setWindowId(EntityInspectorBrowse.SCREEN_NAME);
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("entity", propertyMetaClass.getName());
        MetaProperty inverseProperty = metaProperty.getInverse();
        if (inverseProperty != null)
            params.put("parentProperty", inverseProperty.getName());
        addAction.setWindowParams(params);
        addAction.setOpenType(OPEN_TYPE);
        return addAction;
    }

    private Lookup.Handler createAddHandler(final MetaProperty metaProperty, final CollectionDatasource propertyDs) {
        Lookup.Handler result = new Lookup.Handler() {
            @Override
            public void handleLookup(Collection items) {
                for (Object item : items) {
                    Entity entity = (Entity) item;
                    Iterable existingPropertyEntities = Iterables.transform(
                            propertyDs.getItemIds(),
                            new Function<UUID, Entity>() {
                                @Override
                                public Entity apply(@Nullable UUID id) {
                                    return propertyDs.getItem(id);
                                }
                            }
                    );

                    //set currently editing item to the child's parent property
                    if (!Iterables.contains(existingPropertyEntities, entity)) {
                        MetaProperty inverseProperty = metaProperty.getInverse();
                        entity.setValue(inverseProperty.getName(), datasource.getItem());
                        propertyDs.addItem(entity);
                    }
                }
            }
        };
        propertyDs.refresh();
        return result;
    }

    /**
     * Creates either Remove or Exclude action depending on property type
     */
    private RemoveAction createRemoveAction(MetaProperty metaProperty, Table table) {
        RemoveAction result;
        switch (metaProperty.getType()) {
            case AGGREGATION:
                result = new com.haulmont.cuba.gui.components.actions.RemoveAction(table);
                break;
            case ASSOCIATION:
                result = new com.haulmont.cuba.gui.components.actions.ExcludeAction(table);
                break;
            default:
                throw new IllegalArgumentException("property must contain an entity");
        }
        result.setAutocommit(false);
        return result;
    }

    /**
     * Creates a view, loading all the properties.
     * Referenced entities will be loaded with a LOCAL view.
     *
     * @param meta meta class
     * @return View instance
     */
    private View createView(MetaClass meta) {
        View view = new View(meta.getJavaClass(), false);
        for (MetaProperty metaProperty : meta.getProperties()) {
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    view.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case AGGREGATION:
                    View propView = createReferredPropertyView(metaProperty);
                    view.addProperty(metaProperty.getName(), propView);
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        return view;
    }

    /**
     * Creates a view that includes all of the properties. Related entities will be loaded with a local view.
     *
     * @param property
     * @return
     */
    private View createReferredPropertyView(MetaProperty property) {
        if (property.getType() != MetaProperty.Type.AGGREGATION &&
                property.getType() != MetaProperty.Type.ASSOCIATION)
            throw new RuntimeException("cannot create view for basic type property");

        MetaClass propertyMeta = property.getRange().asClass();
        View view = new View(propertyMeta.getJavaClass(), true);
        for (MetaProperty metaProperty : propertyMeta.getProperties()) {
            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    view.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case AGGREGATION:
                    View propView = MetadataProvider.getViewRepository()
                            .getView(metaProperty.getRange().asClass(), View.MINIMAL);
                    view.addProperty(metaProperty.getName(), propView);
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        MetadataProvider.getViewRepository().storeView(propertyMeta, view);
        return view;
    }

    protected class CommitAction extends AbstractAction {

        protected CommitAction() {
            super("commit");
        }

        @Override
        public void actionPerform(Component component) {
            try {
                validate();
                if (autocommit)
                    dsContext.commit();
                close(Window.COMMIT_ACTION_ID);
            } catch (ValidationException e) {
                showNotification("Validation error", e.getMessage(), NotificationType.TRAY);
            }
        }
    }

    protected class CancelAction extends AbstractAction {

        protected CancelAction() {
            super("cancel");
        }

        @Override
        public void actionPerform(Component component) {
            if (createRequest) {
                //remove added item
                datasource.setItem(null);
                if (datasource instanceof CollectionDatasource)
                    ((CollectionDatasource) datasource).removeItem(item);
            }
            close(Window.COMMIT_ACTION_ID);
        }
    }

    /**
     * Opens entity inspector's editor to create entity
     */
    protected class CreateAction extends AbstractAction {

        private CollectionDatasource entitiesDs;
        private MetaClass entityMeta;
        protected MetaProperty metaProperty;

        protected CreateAction(MetaProperty metaProperty, CollectionDatasource entitiesDs, MetaClass entityMeta) {
            super("create");
            this.entitiesDs = entitiesDs;
            this.entityMeta = entityMeta;
            this.metaProperty = metaProperty;
        }

        @Override
        public void actionPerform(Component component) {
            Map<String, Object> editorParams = new HashMap<String, Object>();
            editorParams.put("metaClass", entityMeta.getName());
            editorParams.put("datasource", entitiesDs);
            editorParams.put("autocommit", Boolean.FALSE);
            MetaProperty inverseProperty = metaProperty.getInverse();
            if (inverseProperty != null)
                editorParams.put("parentProperty", inverseProperty.getName());
            editorParams.put("parent", item);

            Window window = openWindow("entityInspector.edit", OPEN_TYPE, editorParams);
            window.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    entitiesDs.refresh();
                }
            });
        }
    }

    protected class EditAction extends ItemTrackingAction {

        private Table entitiesTable;
        private CollectionDatasource entitiesDs;
        private MetaProperty metaProperty;

        protected EditAction(MetaProperty metaProperty, Table entitiesTable, CollectionDatasource entitiesDs) {
            super("edit");
            this.entitiesTable = entitiesTable;
            this.entitiesDs = entitiesDs;
            this.metaProperty = metaProperty;
        }

        @Override
        public void actionPerform(Component component) {
            Set selected = entitiesTable.getSelected();

            if (selected.size() != 1)
                return;

            Entity editItem = (Entity) selected.toArray()[0];
            Map<String, Object> editorParams = new HashMap<String, Object>();
            editorParams.put("metaClass", editItem.getMetaClass());
            editorParams.put("item", editItem);
            editorParams.put("parent", item);
            editorParams.put("datasource", entitiesDs);
            editorParams.put("autocommit", Boolean.FALSE);
            MetaProperty inverseProperty = metaProperty.getInverse();
            if (inverseProperty != null)
                editorParams.put("parentProperty", inverseProperty.getName());

            Window window = openWindow("entityInspector.edit", OPEN_TYPE, editorParams);
            window.addListener(new CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    entitiesDs.updateItem(entitiesDs.getItem());
                }
            });
        }
    }

    private boolean attrViewPermitted(MetaClass metaClass, String property) {
        return attrPermitted(metaClass, property, EntityAttrAccess.VIEW);
    }

    private boolean attrViewPermitted(MetaProperty metaProperty) {
        return attrPermitted(metaProperty.getDomain(), metaProperty.getName(), EntityAttrAccess.VIEW);
    }

    private boolean attrPermitted(MetaClass metaClass, String property, EntityAttrAccess entityAttrAccess) {
        UserSession session = UserSessionProvider.getUserSession();
        return session.isEntityAttrPermitted(metaClass, property, entityAttrAccess);
    }

    private boolean entityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
        UserSession session = UserSessionProvider.getUserSession();
        return session.isEntityOpPermitted(metaClass, entityOp);
    }
}
