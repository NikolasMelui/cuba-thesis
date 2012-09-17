/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.categories;

import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.Category;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.RefreshAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.RuntimePropsDatasource;
import com.haulmont.cuba.gui.data.impl.CollectionPropertyDatasourceImpl;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.lang.BooleanUtils;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <p>$Id$</p>
 *
 * @author gorbunkov
 */
public class CategoryAttrsFrame extends AbstractFrame {

    @Inject
    protected Metadata metadata;

    @Inject
    protected MessageTools messageTools;

    @Inject
    protected ComponentsFactory factory;

    @Inject
    protected DataService dataService;

    @Inject
    private Table categoryAttrsTable;

    @Inject
    protected Datasource categoryDs;

    @Inject
    protected CollectionPropertyDatasourceImpl<CategoryAttribute, UUID> categoryAttrsDs;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        categoryAttrsTable.addAction(new CategoryAttributeCreateAction());
        categoryAttrsTable.addAction(new CategoryAttributeEditAction());
        categoryAttrsTable.addAction(new RemoveAction(categoryAttrsTable,false));
        categoryAttrsTable.addAction(new RefreshAction(categoryAttrsTable));

        categoryAttrsDs.addListener(new DsListenerAdapter() {
            @Override
            public void stateChanged(Datasource ds, Datasource.State prevState, Datasource.State state) {
                if (state != Datasource.State.VALID) return;
                initDataTypeColumn();
                initDefaultValueColumn();
            }
        });

        initMoveButtons();
    }

    private void initMoveButtons() {
        ((Button)getComponent("moveUp")).setAction(new AbstractAction("moveUp") {
            @Override
            public void actionPerform(Component component) {
                Set<CategoryAttribute> selected = categoryAttrsTable.getSelected();
                if (selected.isEmpty())
                    return;

                CategoryAttribute currentAttr = selected.iterator().next();
                UUID prevId = categoryAttrsDs.prevItemId(currentAttr.getId());
                if (prevId == null)
                    return;

                Integer tmp = currentAttr.getOrderNo();
                CategoryAttribute prevAttr = categoryAttrsDs.getItem(prevId);
                currentAttr.setOrderNo(prevAttr.getOrderNo());
                prevAttr.setOrderNo(tmp);

                sortTableByOrderNo();
            }

            @Override
            public String getCaption() {
                return "";
            }
        });

        AbstractAction action = new AbstractAction("moveDown") {
            @Override
            public void actionPerform(Component component) {
                Set<CategoryAttribute> selected = categoryAttrsTable.getSelected();
                if (selected.isEmpty())
                    return;

                CategoryAttribute currentAttr = selected.iterator().next();
                UUID nextId = categoryAttrsDs.nextItemId(currentAttr.getId());
                if (nextId == null)
                    return;

                Integer tmp = currentAttr.getOrderNo();
                CategoryAttribute nextAttr = categoryAttrsDs.getItem(nextId);
                currentAttr.setOrderNo(nextAttr.getOrderNo());
                nextAttr.setOrderNo(tmp);

                sortTableByOrderNo();
            }

            @Override
            public String getCaption() {
                return "";
            }

        };
        ((Button)getComponent("moveDown")).setAction(action);
    }

    private void sortTableByOrderNo() {
        categoryAttrsTable.sortBy(categoryAttrsDs.getMetaClass().getPropertyPath("orderNo"), true);
    }

    private void initDataTypeColumn() {
        categoryAttrsTable.removeGeneratedColumn("dataType");
        categoryAttrsTable.addGeneratedColumn("dataType", new Table.ColumnGenerator<CategoryAttribute>() {
            public Component generateCell(CategoryAttribute attribute) {
                Label dataTypeLabel = factory.createComponent(Label.NAME);
                String labelContent;
                if (BooleanUtils.isTrue(attribute.getIsEntity())) {
                    try {
                        Class clazz = Class.forName(attribute.getDataType());
                        MetaClass metaClass = metadata.getSession().getClass(clazz);
                        labelContent = messageTools.getEntityCaption(metaClass);
                    } catch (ClassNotFoundException ex) {
                        labelContent = "classNotFound";
                    }
                } else {
                    labelContent = getMessage(attribute.getDataType());
                }

                dataTypeLabel.setValue(labelContent);
                return dataTypeLabel;
            }
        });
    }

    private void initDefaultValueColumn() {
        categoryAttrsTable.addGeneratedColumn("defaultValue", new Table.ColumnGenerator<CategoryAttribute>() {
            @Override
            public Component generateCell(CategoryAttribute attribute) {
                String defaultValue = "";

                if (BooleanUtils.isNotTrue(attribute.getIsEntity())) {
                    RuntimePropsDatasource.PropertyType dataType = RuntimePropsDatasource.PropertyType.valueOf(attribute.getDataType());
                    switch (dataType) {
                        case DATE:
                            Date date = attribute.getDefaultDate();
                            if (date != null) {
                                String dateTimeFormat = Datatypes.getFormatStrings(UserSessionProvider.getLocale()).getDateTimeFormat();
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateTimeFormat);
                                defaultValue = simpleDateFormat.format(date);
                            } else if (BooleanUtils.isTrue(attribute.getDefaultDateIsCurrent())) {
                                defaultValue = getMessage("currentDate");
                            }
                            break;
                        case BOOLEAN:
                            Boolean b = attribute.getDefaultBoolean();
                            if (b != null)
                                defaultValue = BooleanUtils.isTrue(b) ? getMessage("msgTrue") : getMessage("msgFalse");
                            break;
                        default:
                            if (attribute.getDefaultValue() != null)
                                defaultValue = attribute.getDefaultValue().toString();
                    }
                } else {
                    try {
                        Class clazz = Class.forName(attribute.getDataType());
                        LoadContext entitiesContext = new LoadContext(clazz);
                        String entityClassName = MetadataProvider.getSession().getClass(clazz).getName();
                        if (attribute.getDefaultEntityId() != null) {
                            LoadContext.Query query = entitiesContext.setQueryString("select a from " + entityClassName + " a where a.id =:e");
                            query.addParameter("e", attribute.getDefaultEntityId());
                            entitiesContext.setView("_local");
                            Entity entity = dataService.load(entitiesContext);
                            defaultValue = InstanceUtils.getInstanceName(entity);
                        } else defaultValue = "";
                    } catch (ClassNotFoundException ex) {
                        defaultValue = getMessage("entityNotFound");
                    }
                }

                Label defaultValueLabel = factory.createComponent(Label.NAME);
                defaultValueLabel.setValue(defaultValue);
                return defaultValueLabel;
            }
        });
   }

    private void assignNextOrderNo(CategoryAttribute attr) {
        UUID lastId = categoryAttrsDs.lastItemId();
        if (lastId == null)
            attr.setOrderNo(1);
        else {
            CategoryAttribute lastItem = categoryAttrsDs.getItem(lastId);
            if (lastItem.getOrderNo() == null)
                attr.setOrderNo(1);
            else
                attr.setOrderNo(categoryAttrsDs.getItem(lastId).getOrderNo() + 1);

        }
    }

    protected class CategoryAttributeEditAction extends AbstractAction {

        protected CategoryAttributeEditAction() {
            super("edit");
        }

        public String getCaption() {
            return getMessage("categoryAttrsTable.edit");
        }

        @Override
        public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
            Set<CategoryAttribute> selected = categoryAttrsTable.getSelected();
            if (!selected.isEmpty()) {
                AttributeEditor editor = openEditor(
                        "sys$CategoryAttribute.edit",
                        selected.iterator().next(),
                        WindowManager.OpenType.DIALOG,
                        categoryAttrsTable.getDatasource());
                editor.addListener(new Window.CloseListener() {
                    @Override
                    public void windowClosed(String actionId) {
                        categoryAttrsTable.getDatasource().refresh();
                    }
                });
            }
        }
    }

    protected class CategoryAttributeCreateAction extends AbstractAction {

        protected CategoryAttributeCreateAction() {
            super("create");
        }

        public String getCaption() {
            return getMessage("categoryAttrsTable.create");
        }

        @Override
        public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
            final CategoryAttribute attribute = metadata.create(CategoryAttribute.class);
            attribute.setCategory((Category) categoryDs.getItem());
            assignNextOrderNo(attribute);
            AttributeEditor editor = openEditor(
                    "sys$CategoryAttribute.edit",
                    attribute,
                    WindowManager.OpenType.DIALOG,
                    categoryAttrsTable.getDatasource());
            editor.addListener(new Window.CloseListener() {
                @Override
                public void windowClosed(String actionId) {
                    categoryAttrsTable.getDatasource().refresh();
                }
            });
        }
    }


}
