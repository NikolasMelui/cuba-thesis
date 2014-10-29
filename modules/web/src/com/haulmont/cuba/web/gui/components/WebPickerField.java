/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.TestIdManager;
import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.CaptionMode;
import com.haulmont.cuba.gui.components.PickerField;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.toolkit.ui.CubaPickerField;
import com.haulmont.cuba.web.toolkit.ui.converters.StringToEntityConverter;
import com.vaadin.data.Property;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class WebPickerField
        extends
            WebAbstractField<CubaPickerField>
        implements
            PickerField {

    protected CaptionMode captionMode = CaptionMode.ITEM;
    protected String captionProperty;

    protected MetaClass metaClass;

    protected List<Action> actions = new ArrayList<>();

    protected WebPickerFieldActionHandler actionHandler;

    protected Metadata metadata = AppBeans.get(Metadata.NAME);

    protected MetadataTools metadataTools = AppBeans.get(MetadataTools.NAME);

    public WebPickerField() {
        component = new Picker(this);
        component.setImmediate(true);
        component.setInvalidAllowed(false);
        component.setInvalidCommitted(true);
        component.setCaptionFormatter(new StringToEntityConverter() {
            @Override
            public String convertToPresentation(Entity value, Class<? extends String> targetType, Locale locale)
                    throws ConversionException {
                if (captionMode == CaptionMode.PROPERTY
                        && StringUtils.isNotEmpty(captionProperty)
                        && (value != null)) {

                    Object propertyValue = value.getValue(captionProperty);

                    MetaClass metaClass = metadata.getClass(value.getClass());
                    MetaProperty property = metaClass.getProperty(captionProperty);
                    return metadataTools.format(propertyValue, property);
                }

                return super.convertToPresentation(value, targetType, locale);
            }
        });

        attachListener(component);

        initActionHandler();
    }

    public WebPickerField(CubaPickerField component) {
        this.component = component;
        attachListener(component);
        initActionHandler();
    }

    @Override
    public MetaClass getMetaClass() {
        final Datasource ds = getDatasource();
        if (ds != null) {
            return metaProperty.getRange().asClass();
        } else {
            return metaClass;
        }
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        final Datasource ds = getDatasource();
        if (ds != null) {
            throw new IllegalStateException("Datasource is not null");
        }
        this.metaClass = metaClass;
    }

    @Override
    public void setValue(Object value) {
        if (component.isReadOnly()) {
            LogFactory.getLog(getClass()).debug("Set value for non editable field ignored");
            return;
        }

        if (value != null) {
            if (datasource == null && metaClass == null) {
                throw new IllegalStateException("Datasource or metaclass must be set for field");
            }

            Class fieldClass = getMetaClass().getJavaClass();
            Class<?> valueClass = value.getClass();
            if (!fieldClass.isAssignableFrom(valueClass)) {
                throw new IllegalArgumentException(
                        String.format("Could not set value with class %s to field with class %s",
                                fieldClass.getCanonicalName(),
                                valueClass.getCanonicalName())
                );
            }
        }

        super.setValue(value);
    }

    @Override
    public LookupAction addLookupAction() {
        LookupAction action = new LookupAction(this);
        addAction(action);
        return action;
    }

    @Override
    public ClearAction addClearAction() {
        ClearAction action = new ClearAction(this);
        addAction(action);
        return action;
    }

    @Override
    public PickerField.OpenAction addOpenAction() {
        OpenAction action = new OpenAction(this);
        addAction(action);
        return action;
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        this.datasource = datasource;

        final MetaClass metaClass = datasource.getMetaClass();
        metaPropertyPath = metaClass.getPropertyPath(property);
        if (metaPropertyPath == null) {
            throw new RuntimeException(String.format("Property '%s' not found in class %s", property, metaClass));
        }

        this.metaProperty = metaPropertyPath.getMetaProperty();
        this.metaClass = metaProperty.getRange().asClass();

        final ItemWrapper wrapper = createDatasourceWrapper(datasource, Collections.singleton(metaPropertyPath));
        final Property itemProperty = wrapper.getItemProperty(metaPropertyPath);

        component.setPropertyDataSource(itemProperty);

        datasource.addListener(
                new DsListenerAdapter() {
                    @Override
                    public void itemChanged(Datasource ds, Entity prevItem, Entity item) {
                        Object prevValue = getValue();
                        Object newValue = InstanceUtils.getValueEx(item, metaPropertyPath.getPath());
                        setValue(newValue);
                        fireValueChanged(prevValue, newValue);
                    }

                    @Override
                    public void valueChanged(Entity source, String property, Object prevValue, Object value) {

                        if (property.equals(metaPropertyPath.toString())) {
                            setValue(value);
                            fireValueChanged(prevValue, value);
                        }
                    }
                }
        );

        if (datasource.getState() == Datasource.State.VALID && datasource.getItem() != null) {
            if (property.equals(metaPropertyPath.toString())) {
                Object prevValue = getValue();
                Object newValue = InstanceUtils.getValueEx(datasource.getItem(), metaPropertyPath.getPath());
                setValue(newValue);
                fireValueChanged(prevValue, newValue);
            }
        }
        setRequired(metaProperty.isMandatory());
        if (StringUtils.isEmpty(getRequiredMessage())) {
            MessageTools messageTools = AppBeans.get(MessageTools.NAME);
            setRequiredMessage(messageTools.getDefaultRequiredMessage(metaProperty));
        }

        if (metaProperty.isReadOnly()) {
            setEditable(false);
        }
    }

    @Override
    public CaptionMode getCaptionMode() {
        return captionMode;
    }

    @Override
    public void setCaptionMode(CaptionMode captionMode) {
        this.captionMode = captionMode;
    }

    @Override
    public String getCaptionProperty() {
        return captionProperty;
    }

    @Override
    public void setCaptionProperty(String captionProperty) {
        this.captionProperty = captionProperty;
    }

    @Override
    public void addAction(Action action) {
        Action oldAction = getAction(action.getId());

        // get button for old action
        Button oldButton = null;
        if (oldAction != null && oldAction.getOwner() != null && oldAction.getOwner() instanceof WebButton) {
            WebButton oldActionButton = (WebButton) oldAction.getOwner();
            oldButton = oldActionButton.getComponent();
        }

        if (oldAction == null) {
            actions.add(action);
            actionHandler.addAction(action);
        } else {
            actions.add(actions.indexOf(oldAction), action);
            actions.remove(oldAction);
            actionHandler.removeAction(oldAction);
        }

        PickerButton pButton = new PickerButton();
        pButton.setAction(action);
        // no captions for picker buttons
        pButton.<Button>getComponent().setCaption("");

        if (oldButton == null) {
            component.addButton(pButton.<Button>getComponent());
        } else {
            component.replaceButton(oldButton, pButton.<Button>getComponent());
        }

        // apply Editable after action owner is set
        if (action instanceof StandardAction) {
            ((StandardAction) action).setEditable(isEditable());
        }

        if (StringUtils.isNotEmpty(getDebugId())) {
            pButton.setDebugId(AppUI.getCurrent().getTestIdManager().getTestId(getDebugId() + "_" + action.getId()));
        }
    }

    @Override
    public void setDebugId(String id) {
        super.setDebugId(id);

        if (id != null) {
            String debugId = getDebugId();

            TestIdManager testIdManager = AppUI.getCurrent().getTestIdManager();

            for (Action action : actions) {
                if (action.getOwner() != null && action.getOwner() instanceof WebButton) {
                    WebButton button = (WebButton) action.getOwner();
                    if (StringUtils.isEmpty(button.getDebugId())) {
                        button.setDebugId(testIdManager.getTestId(debugId + "_" + action.getId()));
                    }
                }
            }
        }
    }

    @Override
    public void removeAction(Action action) {
        actions.remove(action);
        actionHandler.removeAction(action);
        if (action.getOwner() != null && action.getOwner() instanceof WebButton) {
            Button button = ((WebButton) action.getOwner()).getComponent();
            component.removeButton(button);
        }
    }

    @Override
    public void removeAction(String id) {
        Action action = getAction(id);
        if (action != null) {
            removeAction(action);
        }
    }

    @Override
    public void removeAllActions() {
        for (Action action : new ArrayList<>(actions)) {
            removeAction(action);
        }
    }

    @Override
    public void addFieldListener(FieldListener listener) {
        component.addFieldListener(listener);
    }

    @Override
    public void setFieldEditable(boolean editable) {
        if (isEditable()) {
            component.getField().setReadOnly(!editable);
        }
    }

    @Override
    public Collection<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    @Override
    public Action getAction(String id) {
        for (Action action : actions) {
            if (ObjectUtils.equals(id, action.getId())) {
                return action;
            }
        }
        return null;
    }

    protected void initActionHandler() {
        actionHandler = new WebPickerFieldActionHandler(this);
        component.addActionHandler(actionHandler);
    }

    protected static class PickerButton extends WebButton {

        protected PickerButton() {
            component.setTabIndex(-1);
        }

        @Override
        public void setIcon(String icon) {
            if (StringUtils.isNotBlank(icon)) {
                component.setIcon(WebComponentsHelper.getIcon(icon));
            } else {
                component.setIcon(null);
            }
        }
    }

    public static class Picker extends CubaPickerField {

        private PickerField owner;

        public Picker(PickerField owner) {
            this.owner = owner;
        }

        public Picker(PickerField owner, AbstractField field) {
            super(field);
            this.owner = owner;
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            super.setReadOnly(readOnly);
            if (readOnly) {
                field.setReadOnly(true);
            }

            for (Action action : owner.getActions()) {
                if (action instanceof StandardAction) {
                    ((StandardAction) action).setEditable(!readOnly);
                }
            }
        }
    }
}