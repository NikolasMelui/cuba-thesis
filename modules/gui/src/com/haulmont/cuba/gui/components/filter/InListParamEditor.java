/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;
import com.haulmont.cuba.gui.theme.ThemeConstants;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;

import javax.inject.Inject;
import java.util.*;

/**
 * Window is used for selecting multiple values for generic filter 'In list' condition
 *
 * @author gorbunkov
 * @version $Id$
 */
public class InListParamEditor extends AbstractWindow {

    @Inject
    protected Messages messages;

    @Inject
    protected ThemeConstants theme;

    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected BoxLayout componentLayout;

    @Inject
    protected BoxLayout valuesLayout;

    protected Map<Object, String> values;

    protected CollectionDatasource collectionDatasource;

    protected MetaClass metaClass;

    protected List<String> runtimeEnum;

    protected Class itemClass;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);
        getDialogParams()
            .setWidth(theme.getInt("cuba.gui.filter.FilterListParamEditor.dialog.width"));

        values = new HashMap<>((Map<Object, String>) params.get("values"));
        collectionDatasource = (CollectionDatasource) params.get("collectionDatasource");
        metaClass = (MetaClass) params.get("metaClass");
        runtimeEnum = (List<String>) params.get("runtimeEnum");
        itemClass = (Class) params.get("itemClass");

        for (Map.Entry<Object, String> entry : values.entrySet()) {
            addItemLayout(entry.getKey(), entry.getValue());
        }

        String pickerWidth = theme.get("cuba.gui.filter.FilterListParamEditor.picker.width");

        if (collectionDatasource != null) {
            final LookupField lookup = componentsFactory.createComponent(LookupField.NAME);
            lookup.setWidth(pickerWidth);
            lookup.setOptionsDatasource(collectionDatasource);

            collectionDatasource.addListener(
                    new CollectionDsListenerAdapter<Entity>() {
                        @Override
                        public void collectionChanged(CollectionDatasource ds, Operation operation, List<Entity> items) {
                            lookup.setValue(null);
                        }
                    }
            );

            lookup.addListener(new ValueListener() {
                @Override
                public void valueChanged(Object source, String property, Object prevValue, Object value) {
                    if (value != null && !containsValue((Instance) value)) {
                        String str = addEntityInstance((Instance) value);
                        addItemLayout(value, str);
                    }
                    lookup.setValue(null);
                }
            });

            componentLayout.add(lookup);

        } else if (metaClass != null) {
            final PickerField picker = componentsFactory.createComponent(PickerField.NAME);
            picker.setWidth(pickerWidth);
            picker.setMetaClass(metaClass);

            PickerField.LookupAction action = new PickerField.LookupAction(picker) {
                @Override
                public void afterSelect(Collection items) {
                    if (!items.isEmpty()) {
                        for (Object value : items) {
                            if (!containsValue((Instance) value)) {
                                String str = addEntityInstance((Instance) value);
                                addItemLayout(value, str);
                            }
                        }
                    }
                    picker.setValue(null);
                }
            };

            picker.addAction(action);
            action.setLookupScreenOpenType(WindowManager.OpenType.DIALOG);
            picker.addClearAction();

            componentLayout.add(picker);

        } else if (runtimeEnum != null) {
            final LookupField lookup = componentsFactory.createComponent(LookupField.NAME);
            lookup.setWidth(pickerWidth);
            lookup.setOptionsList(runtimeEnum);

            lookup.addListener(new ValueListener() {
                @Override
                public void valueChanged(Object source, String property, Object prevValue, Object value) {
                    if (value != null && !containsValue((String) value)) {
                        String str = addRuntimeEnumValue((String) value);
                        addItemLayout(value, str);
                    }
                    lookup.setValue(null);
                }
            });

            componentLayout.add(lookup);
        } else if (itemClass.isEnum()) {
            Map<String, Object> options = new HashMap<>();
            for (Object obj : itemClass.getEnumConstants()) {
                options.put(messages.getMessage((Enum) obj), obj);
            }

            final LookupField lookup = componentsFactory.createComponent(LookupField.NAME);
            lookup.setWidth(pickerWidth);
            lookup.setOptionsMap(options);

            lookup.addListener(new ValueListener() {
                @Override
                public void valueChanged(Object source, String property, Object prevValue, Object value) {
                    if (value != null && !containsValue((Enum) value)) {
                        String str = addEnumValue((Enum) value);
                        addItemLayout(value, str);
                    }
                    lookup.setValue(null);
                }
            });

            componentLayout.add(lookup);

        } else if (Date.class.isAssignableFrom(itemClass)) {
            final DateField dateField = componentsFactory.createComponent(DateField.NAME);

            Button addButton = componentsFactory.createComponent(Button.NAME);
            addButton.setAction(new AbstractAction("") {
                @Override
                public void actionPerform(Component component) {
                    Date date = dateField.getValue();
                    if (date != null) {
                        String str = addDate(date);
                        addItemLayout(date, str);
                        dateField.setValue(null);
                    }
                }

                @Override
                public String getCaption() {
                    return messages.getMainMessage("actions.Add");
                }
            });

            DateField.Resolution resolution;
            String dateFormat;
            if (itemClass.equals(java.sql.Date.class)) {
                resolution = DateField.Resolution.DAY;
                dateFormat = messages.getMessage(AppConfig.getMessagesPack(), "dateFormat");
            } else {
                resolution = DateField.Resolution.MIN;
                dateFormat = messages.getMessage(AppConfig.getMessagesPack(), "dateTimeFormat");
            }
            dateField.setResolution(resolution);
            dateField.setDateFormat(dateFormat);

            componentLayout.add(dateField);
            componentLayout.add(addButton);
        } else
            throw new UnsupportedOperationException();
    }

    protected boolean containsValue(String value) {
        return values.containsValue(value);
    }

    protected boolean containsValue(Instance value) {
        return this.values.containsValue(value.getInstanceName());
    }

    protected boolean containsValue(Enum value) {
        return this.values.containsValue(messages.getMessage(value));
    }

    protected String addRuntimeEnumValue(String value) {
        values.put(value, value);
        return value;
    }

    protected String addEnumValue(Enum en) {
        String str = messages.getMessage(en);
        values.put(en, str);
        return str;
    }

    protected String addEntityInstance(Instance value) {
        String str = value.getInstanceName();
        values.put(value, str);
        return str;
    }

    protected void addItemLayout(final Object value, String str) {
        final BoxLayout itemLayout = componentsFactory.createComponent(BoxLayout.HBOX);
        itemLayout.setSpacing(true);

        Label itemLab = componentsFactory.createComponent(Label.NAME);
        itemLab.setValue(str);
        itemLayout.add(itemLab);
        itemLab.setAlignment(Alignment.MIDDLE_LEFT);

        LinkButton delItemBtn = componentsFactory.createComponent(LinkButton.NAME);
        delItemBtn.setIcon("icons/item-remove.png");
        delItemBtn.setAction(new AbstractAction("") {
            @Override
            public void actionPerform(Component component) {
                values.remove(value);
                valuesLayout.remove(itemLayout);
            }
        });
        itemLayout.add(delItemBtn);
        delItemBtn.setAlignment(Alignment.MIDDLE_LEFT);

        valuesLayout.add(itemLayout);
    }

    protected String addDate(Date date) {
        UserSessionSource sessionSource = AppBeans.get(UserSessionSource.NAME);
        Locale locale = sessionSource.getUserSession().getLocale();
        Datatype datatype = Datatypes.get(itemClass);

        String str = datatype.format(date, locale);

        values.put(date, str);
        return str;
    }

    public Map<Object, String> getValues() {
        return values;
    }

    public void commit() {
        close(COMMIT_ACTION_ID);
    }

    public void cancel() {
        close(CLOSE_ACTION_ID);
    }

}
