/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Gennady Pavlov
 * Created: 08.04.2010 16:09:20
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.ValueListener;
import com.haulmont.cuba.web.toolkit.ui.ActionsField;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Deprecated
public class WebActionsField
        extends WebAbstractField<ActionsField>
        implements com.haulmont.cuba.gui.components.ActionsField {
    private WebLookupField lookupField;
    private WebButton lookupButton;
    private WebButton openButton;

    private List<Action> actionsOrder = new LinkedList<>();

    public WebActionsField() {
        lookupField = new WebLookupField();
        this.component = new ActionsField((AbstractSelect) lookupField.getComponent());
        component.setImmediate(true);
        component.setInvalidAllowed(false);
        component.setInvalidCommitted(true);
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        lookupField.setDatasource(datasource, property);
        setRequired(getMetaProperty().isMandatory());
    }

    @Override
    public MetaProperty getMetaProperty() {
        return lookupField.getMetaProperty();
    }

    @Override
    public Datasource getDatasource() {
        return lookupField.getDatasource();
    }

    public Object getNullOption() {
        return lookupField.getNullOption();
    }

    public void setNullOption(Object nullOption) {
        lookupField.setNullOption(nullOption);
    }

    public FilterMode getFilterMode() {
        return lookupField.getFilterMode();
    }

    public void setFilterMode(FilterMode mode) {
        lookupField.setFilterMode(mode);
    }

    public boolean isNewOptionAllowed() {
        return lookupField.isNewOptionAllowed();
    }

    public void setNewOptionAllowed(boolean newOptionAllowed) {
        lookupField.setNewOptionAllowed(newOptionAllowed);
    }

    public NewOptionHandler getNewOptionHandler() {
        return lookupField.getNewOptionHandler();
    }

    public void setNewOptionHandler(NewOptionHandler newOptionHandler) {
        lookupField.setNewOptionHandler(newOptionHandler);
    }

    public boolean isMultiSelect() {
        return lookupField.isMultiSelect();
    }

    public void setMultiSelect(boolean multiselect) {
        lookupField.setMultiSelect(multiselect);
    }

    public CaptionMode getCaptionMode() {
        return lookupField.getCaptionMode();
    }

    public void setCaptionMode(CaptionMode captionMode) {
        lookupField.setCaptionMode(captionMode);
    }

    public String getCaptionProperty() {
        return lookupField.getCaptionProperty();
    }

    public void setCaptionProperty(String captionProperty) {
        lookupField.setCaptionProperty(captionProperty);
    }

    public CollectionDatasource getOptionsDatasource() {
        return lookupField.getOptionsDatasource();
    }

    public void setOptionsDatasource(CollectionDatasource datasource) {
        lookupField.setOptionsDatasource(datasource);
    }

    public List getOptionsList() {
        return lookupField.getOptionsList();
    }

    public void setOptionsList(List optionsList) {
        lookupField.setOptionsList(optionsList);
    }

    public Map<String, Object> getOptionsMap() {
        return lookupField.getOptionsMap();
    }

    public void setOptionsMap(Map<String, Object> map) {
        lookupField.setOptionsMap(map);
    }

    public String getDescriptionProperty() {
        return lookupField.getDescriptionProperty();
    }

    public void setDescriptionProperty(String descProperty) {
        lookupField.setDescriptionProperty(descProperty);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);

        lookupField.setEditable(editable);
        if (lookupButton != null) {
            lookupButton.setVisible(editable);
            if (editable) {
                lookupButton.setIcon("select/img/bg-right-lookup.png");
            } else {
                lookupButton.setIcon("select/img/bg-right-lookup-readonly.png");
            }
        }
        if (openButton != null) {
            if (getValue() == null) {
                openButton.setVisible(editable);
            }
            if (editable) {
                openButton.setIcon("select/img/bg-right-open.png");
            } else {
                openButton.setIcon("select/img/bg-right-open-readonly.png");
            }
        }
    }

    @Override
    public void setValue(Object value) {
        if (!isEditable())
            return;

        CollectionDatasource optionsDatasource = lookupField.getOptionsDatasource();
        if (value != null && value instanceof Entity && optionsDatasource != null &&
                !optionsDatasource.containsItem(((Entity) value).getId())) {
            optionsDatasource.addItem((Entity) value);
        }
        lookupField.setValue(value);
    }

    @Override
    public <T> T getValue() {
        Datasource ds = getDatasource();
        if (ds != null && Datasource.State.VALID.equals(ds.getState())) {
            Instance instance = ds.getItem();
            return instance == null ? null : (T) instance.getValue(getMetaProperty().getName());
        }
        return lookupField.getValue();
    }

    public void addButton(com.haulmont.cuba.gui.components.Button button) {
        component.addButton((Button) WebComponentsHelper.unwrap(button));
    }

    public void enableButton(String buttonId, boolean enable) {
        if (DROPDOWN.equals(buttonId)) {
            if (enable) {
                component.addStyleName("dropdown");
            } else {
                component.removeStyleName("dropdown");
            }
        } else if (LOOKUP.equals(buttonId)) {
            if (lookupButton == null) {
                lookupButton = new WebButton();
                if (lookupField.isEditable()) {
                    lookupButton.setIcon("select/img/bg-right-lookup.png");
                } else {
                    lookupButton.setIcon("select/img/bg-right-lookup-readonly.png");
                }
                lookupButton.setEnabled(lookupField.isEditable());
                lookupButton.setStyleName(BaseTheme.BUTTON_LINK);
                component.addButton((Button) lookupButton.getComponent());
            }
            lookupButton.setVisible(enable);
        } else if (OPEN.equals(buttonId)) {
            if (openButton == null) {
                openButton = new WebButton();
                if (lookupField.isEditable()) {
                    openButton.setIcon("select/img/bg-right-open.png");
                } else {
                    openButton.setIcon("select/img/bg-right-open-readonly.png");
                }
                openButton.setStyleName(BaseTheme.BUTTON_LINK);
                component.addButton((Button) openButton.getComponent());
            }
            openButton.setVisible(enable);
        }
    }

    public void addAction(Action action) {
        if (action == null || action.getId() == null) {
            return;
        }

        if (action.getId().equals(LOOKUP) && lookupButton != null) {
            lookupButton.setAction(action);
            lookupButton.setEnabled(lookupField.isEditable());
        } else if (action.getId().equals(OPEN) && openButton != null) {
            openButton.setAction(action);
        }
        actionsOrder.add(action);
    }

    public void removeAction(com.haulmont.cuba.gui.components.Action action) {
        actionsOrder.remove(action);
    }

    public Collection<com.haulmont.cuba.gui.components.Action> getActions() {
        return actionsOrder;
    }

    public com.haulmont.cuba.gui.components.Action getAction(String id) {
        for (com.haulmont.cuba.gui.components.Action action : getActions()) {
            if (ObjectUtils.equals(action.getId(), id)) {
                return action;
            }
        }

        return null;
    }

    @Override
    public void addListener(ValueListener listener) {
        lookupField.addListener(listener);
    }

    @Override
    public void removeListener(ValueListener listener) {
        lookupField.removeListener(listener);
    }

    @Override
    public void addValidator(Validator validator) {
        lookupField.addValidator(validator);
    }

    @Override
    public void removeValidator(Validator validator) {
        lookupField.removeValidator(validator);
    }

    @Override
    public boolean isValid() {
        return lookupField.isValid();
    }

    @Override
    public void validate() throws ValidationException {
        if (!isVisible() || !isEditable() || !isEnabled())
            return;

        Object value = getValue();
        if (isEmpty(value)) {
            if (isRequired())
                throw new RequiredValueMissingException(component.getRequiredError(), this);
            else
                return;
        }

        for (Field.Validator validator : validators) {
            validator.validate(value);
        }
    }

    public WebLookupField getLookupField() {
        return lookupField;
    }

    @Override
    public void disablePaging() {
    }
}