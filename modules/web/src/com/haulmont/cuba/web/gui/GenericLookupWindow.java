/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 18.02.2009 12:06:42
 * $Id$
 */
package com.haulmont.cuba.web.gui;

import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.actions.ListActionType;
import com.haulmont.cuba.web.gui.components.*;
import com.haulmont.chile.core.model.MetaClass;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;

import java.util.EnumSet;
import java.util.Map;

public class GenericLookupWindow extends GenericBrowserWindow implements com.haulmont.cuba.gui.components.Window.Lookup {
    private Handler handler;
    private Validator validator;

    @Override
    protected com.vaadin.ui.Component createLayout() {
        final VerticalLayout layout = (VerticalLayout) super.createLayout();

        HorizontalLayout okbar = new HorizontalLayout();
        okbar.setHeight("25px");

        final Button selectButton = new Button("Select");
        selectButton.addListener(new SelectAction(this));

        final Button cancelButton = new Button("Cancel", new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                close("cancel");
            }
        });

        okbar.addComponent(selectButton);
        okbar.addComponent(cancelButton);
        
        layout.addComponent(okbar);
        layout.setComponentAlignment(okbar, com.vaadin.ui.Alignment.BOTTOM_LEFT);

        return layout;
    }

    protected WebTable createTable() {
        final WebTable table = new WebTable();
        table.setMultiSelect(true);

        ComponentsHelper.createActions(table, EnumSet.of(ListActionType.EDIT, ListActionType.REFRESH));

        return table;
    }

    @Override
    protected void init(Map<String, Object> params) {
        super.init(params);   
    }

    protected void initEditable(Map<String, Object> params) {
        // Do nothing
    }

    protected void initCaption(MetaClass metaClass) {
        setCaption("Lookup " + metaClass.getName());
    }

    public Component getLookupComponent() {
        return table;
    }

    public void setLookupComponent(Component lookupComponent) {
        throw new UnsupportedOperationException();
    }

    public Handler getLookupHandler() {
        return handler;
    }

    public void setLookupHandler(Handler handler) {
        this.handler = handler;
    }

    public Validator getLookupValidator() {
        return validator;
    }

    public void setLookupValidator(Validator validator) {
        this.validator = validator;
    }
}
