/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.core.bulk;

import com.haulmont.chile.core.datatypes.impl.ByteArrayDatatype;
import com.haulmont.chile.core.datatypes.impl.UUIDDatatype;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.NestedDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImpl;
import com.haulmont.cuba.gui.data.impl.DsContextImpl;
import com.haulmont.cuba.gui.data.impl.EmbeddedDatasourceImpl;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Pattern;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

/**
 * @author artamonov
 * @version $Id$
 */
public class BulkEditorWindow extends AbstractWindow {

    @Inject
    protected ViewRepository viewRepository;

    @Inject
    protected MetadataTools metadataTools;

    @Inject
    protected MessageTools messageTools;

    @Inject
    protected Metadata metadata;

    @Inject
    protected DataSupplier dataSupplier;

    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected Configuration configuration;

    @Inject
    protected Security security;

    @Inject
    protected BoxLayout contentPane;

    @Inject
    protected Label infoLabel;

    @Inject
    protected Button applyButton;

    @Inject
    protected ThemeConstants themeConstants;

    @WindowParam(required = true)
    protected MetaClass metaClass;

    @WindowParam(required = true)
    protected Set<Entity> selected;

    @WindowParam
    protected String exclude;

    protected Pattern excludeRegex;

    protected DsContextImpl dsContext;
    protected Datasource<Entity> datasource;
    protected Map<String, Datasource<Entity>> datasources = new HashMap<>();

    protected Map<String, ManagedField> managedFields = new LinkedHashMap<>();
    protected Map<String, Field> dataFields = new LinkedHashMap<>();
    protected BulkEditorFieldFactory fieldFactory = new BulkEditorFieldFactory();
    protected List<Entity> items;

    protected List<String> managedEmbeddedProperties = new ArrayList<>();

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        checkNotNullArgument(metaClass);
        checkNotNullArgument(selected);

        if (StringUtils.isNotBlank(exclude)) {
            excludeRegex = Pattern.compile(exclude);
        }

        for (ManagedField managedField : getManagedFields(metaClass)) {
            managedFields.put(managedField.getFqn(), managedField);
        }

        View view = createView(metaClass);

        items = loadItems(view);

        dsContext = new DsContextImpl(dataSupplier);
        dsContext.setFrameContext(getDsContext().getFrameContext());
        setDsContext(dsContext);

        datasource = new DatasourceImpl<>();
        datasource.setup(dsContext, dataSupplier, metaClass.getName() + "Ds", metaClass, view);
        ((DatasourceImpl) datasource).valid();

        dsContext.register(datasource);
        createNestedEmbeddedDatasources(datasource, metaClass, "");

        Entity instance = metadata.create(metaClass);
        createEmbeddedFields(metaClass, instance, "");

        datasource.setItem(instance);
        datasource.setAllowCommit(false);

        createDataComponents();

        datasource.refresh();
    }

    protected void createDataComponents() {
        if (managedFields.isEmpty()) {
            infoLabel.setValue(getMessage("bulk.noEditableProperties"));
            applyButton.setVisible(false);
            return;
        }

        GridLayout grid = componentsFactory.createComponent(GridLayout.NAME);
        grid.setSpacing(true);
        grid.setColumns(4);
        grid.setRows((managedFields.size() + 1) / 2);

        contentPane.add(grid);
        grid.setFrame(frame);

        ArrayList<ManagedField> editFields = new ArrayList<>(managedFields.values());
        Collections.sort(editFields, new Comparator<ManagedField>() {
            @Override
            public int compare(ManagedField o1, ManagedField o2) {
                return o1.getLocalizedName().compareTo(o2.getLocalizedName());
            }
        });

        String fieldWidth = themeConstants.get("cuba.gui.BulkEditorWindow.field.width");

        for (ManagedField field : editFields) {
            Label label = componentsFactory.createComponent(Label.NAME);
            label.setFrame(getFrame());
            label.setValue(field.getLocalizedName());

            grid.add(label);

            Datasource<Entity> fieldDs = datasource;
            if (metadataTools.isEmbeddable(fieldDs.getMetaClass())) {
                fieldDs = datasources.get(field.getParentFqn());
            }

            final Field editField = fieldFactory.createField(fieldDs, field.getMetaProperty());
            if (editField != null) {
                editField.setFrame(getFrame());
                editField.setWidth(fieldWidth);

                boolean required = editField.isRequired();

                BoxLayout boxLayout = componentsFactory.createComponent(BoxLayout.HBOX);
                boxLayout.setFrame(getFrame());
                boxLayout.setSpacing(true);

                boxLayout.add(editField);

                if (!required) {
                    final Button clearButton = componentsFactory.createComponent(Button.NAME);
                    clearButton.setFrame(getFrame());
                    Action action = new AbstractAction("actions.BulkClear") {
                        @Override
                        public void actionPerform(Component component) {
                            editField.setEnabled(!editField.isEnabled());
                            if (!editField.isEnabled()) {
                                editField.setValue(null);
                                setIcon("icons/edit.png");
                                clearButton.setDescription(getMessage("bulk.editAttribute"));
                            } else {
                                setIcon("icons/erase.png");
                                clearButton.setDescription(getMessage("bulk.clearAttribute"));
                            }
                        }
                    };
                    action.setCaption("");
                    action.setIcon("icons/erase.png");

                    clearButton.setAction(action);
                    clearButton.setDescription(getMessage("bulk.clearAttribute"));

                    boxLayout.add(clearButton);
                }

                editField.setRequired(false);
                editField.setValue(null);

                grid.add(boxLayout);

                dataFields.put(field.getFqn(), editField);
            } else {
                Label unknownLabel = componentsFactory.createComponent(Label.NAME);
                unknownLabel.setFrame(getFrame());
                grid.add(unknownLabel);
            }
        }

        if (!dataFields.isEmpty()) {
            dataFields.values().iterator().next().requestFocus();
        }
    }

    protected boolean isByteArray(MetaProperty metaProperty) {
        return ByteArrayDatatype.NAME.equals(metaProperty.getRange().asDatatype().getName());
    }

    protected boolean isUuid(MetaProperty metaProperty) {
        return UUIDDatatype.NAME.equals(metaProperty.getRange().asDatatype().getName());
    }

    /**
     * Recursively instantiates the embedded properties.
     * E.g. embedded properties of the embedded property will also be instantiated.
     *
     * @param metaClass meta class of the entity
     * @param item      entity instance
     */
    protected void createEmbeddedFields(MetaClass metaClass, Entity item, String fqnPrefix) {
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            String fqn = metaProperty.getName();
            if (StringUtils.isNotEmpty(fqnPrefix)) {
                fqn = fqnPrefix + "." + fqn;
            }

            if (managedEmbeddedProperties.contains(fqn) && metadataTools.isEmbedded(metaProperty)) {
                MetaClass embeddedMetaClass = metaProperty.getRange().asClass();
                Entity embedded = item.getValue(metaProperty.getName());
                if (embedded == null) {
                    embedded = metadata.create(embeddedMetaClass);
                    item.setValue(metaProperty.getName(), embedded);
                }
                createEmbeddedFields(embeddedMetaClass, embedded, fqn);
            }
        }
    }

    protected void createNestedEmbeddedDatasources(Datasource masterDs, MetaClass metaClass, String fqnPrefix) {
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (MetaProperty.Type.ASSOCIATION == metaProperty.getType()
                    || MetaProperty.Type.COMPOSITION == metaProperty.getType()) {
                String fqn = metaProperty.getName();
                if (StringUtils.isNotEmpty(fqnPrefix)) {
                    fqn = fqnPrefix + "." + fqn;
                }

                if (managedEmbeddedProperties.contains(fqn) && metadataTools.isEmbedded(metaProperty)) {
                    MetaClass propertyMetaClass = metaProperty.getRange().asClass();
                    @SuppressWarnings("unchecked")
                    NestedDatasource<Entity> propertyDs = new EmbeddedDatasourceImpl();
                    propertyDs.setup(fqn + "Ds", masterDs, metaProperty.getName());
                    propertyDs.setAllowCommit(false);
                    createNestedEmbeddedDatasources(propertyDs, propertyMetaClass, fqn);
                    datasources.put(fqn, propertyDs);
                    dsContext.register(propertyDs);
                }
            }
        }
    }

    /**
     * Creates a view, loading only neccessary properties.
     * Referenced entities will be loaded with a MINIMAL view.
     *
     * @param meta meta class
     * @return View instance
     */
    protected View createView(MetaClass meta) {
        @SuppressWarnings("unchecked")
        View view = new View(meta.getJavaClass(), false);
        for (MetaProperty metaProperty : meta.getProperties()) {
            if (!managedFields.containsKey(metaProperty.getName())) {
                continue;
            }

            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    view.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case COMPOSITION:
                    View propView;
                    if (!metadataTools.isEmbedded(metaProperty)) {
                        propView = viewRepository.getView(metaProperty.getRange().asClass(), View.MINIMAL);
                    } else {
                        // build view for embedded property
                        propView = createEmbeddedView(metaProperty.getRange().asClass(), metaProperty.getName());
                    }
                    //in some cases JPA loads extended entities as instance of base class which leads to ClassCastException
                    //loading property lazy prevents this from happening
                    view.addProperty(metaProperty.getName(), propView, true);
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        return view;
    }

    protected View createEmbeddedView(MetaClass meta, String fqnPrefix) {
        @SuppressWarnings("unchecked")
        View view = new View(meta.getJavaClass(), false);
        for (MetaProperty metaProperty : meta.getProperties()) {
            String fqn = fqnPrefix + "." + metaProperty.getName();

            if (!managedFields.containsKey(fqn)) {
                continue;
            }

            switch (metaProperty.getType()) {
                case DATATYPE:
                case ENUM:
                    view.addProperty(metaProperty.getName());
                    break;
                case ASSOCIATION:
                case COMPOSITION:
                    View propView;
                    if (!metadataTools.isEmbedded(metaProperty)) {
                        propView = viewRepository.getView(metaProperty.getRange().asClass(), View.MINIMAL);
                    } else {
                        // build view for embedded property
                        propView = createEmbeddedView(metaProperty.getRange().asClass(), fqn);
                    }
                    //in some cases JPA loads extended entities as instance of base class which leads to ClassCastException
                    //loading property lazy prevents this from happening
                    view.addProperty(metaProperty.getName(), propView, true);
                    break;
                default:
                    throw new IllegalStateException("unknown property type");
            }
        }
        return view;
    }

    protected boolean isPermitted(MetaClass metaClass, MetaProperty metaProperty) {
        return security.isEntityAttrPermitted(metaClass, metaProperty.getName(), EntityAttrAccess.MODIFY);
    }

    protected boolean isManagedAttribute(MetaClass metaClass, MetaProperty metaProperty) {
        if (metadataTools.isSystem(metaProperty)
                || metadataTools.isTransient(metaProperty)
                || metadataTools.isSystemLevel(metaProperty)
                || metaProperty.getRange().getCardinality().isMany()
                || !isPermitted(metaClass, metaProperty)) {
            return false;
        }

        if (metaProperty.getRange().isDatatype() && (isByteArray(metaProperty) || isUuid(metaProperty))) {
            return false;
        }

        if (metaProperty.getRange().isClass()) {
            MetaClass propertyMetaClass = metaProperty.getRange().asClass();
            if (metadataTools.isSystemLevel(propertyMetaClass)) {
                return false;
            }

            if (!security.isEntityOpPermitted(propertyMetaClass, EntityOp.READ)) {
                return false;
            }
        }

        return !(excludeRegex != null && excludeRegex.matcher(metaProperty.getName()).matches());
    }

    protected List<ManagedField> getManagedFields(MetaClass metaClass) {
        List<ManagedField> managedFields = new ArrayList<>();
        // sort Fields
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (isManagedAttribute(metaClass, metaProperty)) {
                String propertyCaption = messageTools.getPropertyCaption(metaProperty);
                if (!metadataTools.isEmbedded(metaProperty)) {
                    managedFields.add(new ManagedField(metaProperty.getName(), metaProperty,
                            propertyCaption, null));
                } else {
                    List<ManagedField> nestedFields = getManagedFields(metaProperty, metaProperty.getName(), propertyCaption);
                    if (nestedFields.size() > 0) {
                        managedEmbeddedProperties.add(metaProperty.getName());
                    }

                    managedFields.addAll(nestedFields);
                }
            }
        }
        return managedFields;
    }

    protected List<ManagedField> getManagedFields(MetaProperty embeddedProperty, String fqnPrefix, String localePrefix) {
        List<ManagedField> managedFields = new ArrayList<>();
        MetaClass metaClass = embeddedProperty.getRange().asClass();
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            if (isManagedAttribute(metaClass, metaProperty)) {
                String fqn = fqnPrefix + "." + metaProperty.getName();
                String localeName = localePrefix + " " + messageTools.getPropertyCaption(metaProperty);

                if (!metadataTools.isEmbedded(metaProperty)) {
                    managedFields.add(new ManagedField(fqn, metaProperty, localeName, fqnPrefix));
                } else {
                    List<ManagedField> nestedFields = getManagedFields(metaProperty, fqn, localeName);
                    if (nestedFields.size() > 0) {
                        managedEmbeddedProperties.add(fqn);
                    }
                    managedFields.addAll(nestedFields);
                }
            }
        }
        return managedFields;
    }

    public void cancelChanges() {
        close(CLOSE_ACTION_ID);
    }

    public void applyChanges() {
        List<String> fields = new ArrayList<>();
        for (Map.Entry<String, Field> fieldEntry : dataFields.entrySet()) {
            Field field = fieldEntry.getValue();
            if (field.getValue() != null || !field.isEnabled()) {
                String localizedName = managedFields.get(fieldEntry.getKey()).getLocalizedName();
                fields.add("- " + localizedName);
            }
        }

        if (!fields.isEmpty()) {
            showOptionDialog(getMessage("bulk.confirmation"),
                    formatMessage("bulk.applyConfirmation", items.size(), StringUtils.join(fields, "\n")),
                    MessageType.CONFIRMATION, new Action[]{
                            new AbstractAction("actions.Apply") {
                                {
                                    setIcon("icons/ok.png");
                                }

                                @Override
                                public void actionPerform(Component component) {
                                    commitChanges();
                                }
                            },
                            new AbstractAction("actions.Cancel") {
                                {
                                    setIcon("icons/cancel.png");
                                }

                                @Override
                                public void actionPerform(Component component) {

                                }
                            }
                    });
        } else {
            showNotification(getMessage("bulk.noChanges"), NotificationType.HUMANIZED);
        }
    }

    protected List<Entity> loadItems(View view) {
        LoadContext lc = new LoadContext(metaClass);
        lc.setSoftDeletion(false);

        List<UUID> ids = new ArrayList<>();
        for (Entity item : selected) {
            ids.add(((BaseUuidEntity) item).getId());
        }
        LoadContext.Query query = new LoadContext.Query(String.format("select e from %s e where e.id in :ids", metaClass));
        query.setParameter("ids", ids);
        lc.setQuery(query);
        lc.setView(view);

        return dataSupplier.loadList(lc);
    }

    protected void commitChanges() {
        List<String> fields = new ArrayList<>();
        for (Map.Entry<String, Field> fieldEntry : dataFields.entrySet()) {
            Field field = fieldEntry.getValue();
            if (field.getValue() != null || !field.isEnabled()) {
                fields.add(managedFields.get(fieldEntry.getKey()).getFqn());
            }
        }

        for (Map.Entry<String, Field> fieldEntry : dataFields.entrySet()) {
            Field field = fieldEntry.getValue();
            if (!field.isEnabled()) {
                for (Entity item : items) {
                    ensureEmbeddedPropertyCreated(item, fieldEntry.getKey());

                    item.setValueEx(fieldEntry.getKey(), null);
                }
            } else if (field.getValue() != null) {
                for (Entity item : items) {
                    ensureEmbeddedPropertyCreated(item, fieldEntry.getKey());

                    item.setValueEx(fieldEntry.getKey(), field.getValue());
                }
            }
        }

        CommitContext cc = new CommitContext();
        cc.setCommitInstances(new ArrayList<>(items));

        Set<Entity> commited = dataSupplier.commit(cc);

        LogFactory.getLog(getClass()).info(String.format(
                "Applied bulk editing for %s entries of %s. Changed properties: %s",
                commited.size(), metaClass, StringUtils.join(fields, ", ")));

        showNotification(formatMessage("bulk.successMessage", commited.size()), NotificationType.HUMANIZED);
        close(COMMIT_ACTION_ID);
    }

    protected void ensureEmbeddedPropertyCreated(Entity item, String propertyPath) {
        if (!StringUtils.contains(propertyPath, ".")) {
            return;
        }

        MetaPropertyPath path = metaClass.getPropertyPath(propertyPath);

        if (path != null) {
            Entity currentItem = item;
            for (MetaProperty property : path.getMetaProperties()) {
                if (metadataTools.isEmbedded(property)) {
                    Object currentItemValue = currentItem.getValue(property.getName());
                    if (currentItemValue == null) {
                        Entity newItem = metadata.create(property.getRange().asClass());
                        currentItem.setValue(property.getName(), newItem);
                        currentItem = newItem;
                    } else {
                        currentItem = (Entity) currentItemValue;
                    }
                } else {
                    break;
                }
            }
        }
    }

    protected static class ManagedField {

        protected final String fqn;
        protected final String parentFqn;

        protected final String localizedName;

        protected final MetaProperty metaProperty;

        public ManagedField(String fqn, MetaProperty metaProperty, String localizedName, String parentFqn) {
            this.fqn = fqn;
            this.metaProperty = metaProperty;
            this.localizedName = localizedName;
            this.parentFqn = parentFqn;
        }

        public String getFqn() {
            return fqn;
        }

        public String getParentFqn() {
            return parentFqn;
        }

        public String getLocalizedName() {
            return localizedName;
        }

        public MetaProperty getMetaProperty() {
            return metaProperty;
        }
    }
}