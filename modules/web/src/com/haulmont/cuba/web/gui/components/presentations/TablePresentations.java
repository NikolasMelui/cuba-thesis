/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components.presentations;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.gui.presentations.PresentationsChangeListener;
import com.haulmont.cuba.security.entity.Presentation;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.gui.components.WebPopupButton;
import com.haulmont.cuba.web.toolkit.ui.CubaEnhancedTable;
import com.haulmont.cuba.web.toolkit.ui.CubaMenuBar;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractProperty;
import com.vaadin.ui.*;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gorodnov
 * @version $Id$
 */
public class TablePresentations extends VerticalLayout {

    protected CubaMenuBar menuBar;
    protected WebPopupButton button;
    protected CheckBox textSelectionCheckBox;

    protected Table table;
    protected CubaEnhancedTable tableImpl;

    protected Map<Object, com.vaadin.ui.MenuBar.MenuItem> presentationsMenuMap;

    protected Messages messages;

    public TablePresentations(Table component) {
        this.table = component;
        this.messages = AppBeans.get(Messages.class);

        this.tableImpl = (CubaEnhancedTable) WebComponentsHelper.unwrap(table);

        setSizeUndefined();
        setStyleName("cuba-table-presentations");
        setParent((HasComponents) WebComponentsHelper.unwrap(component));

        initLayout();

        table.getPresentations().addListener(new PresentationsChangeListener() {
            @Override
            public void currentPresentationChanged(Presentations presentations, Object oldPresentationId) {
                table.getPresentations().commit();
                if (presentationsMenuMap != null) {
                    // simple change current item
                    if (oldPresentationId != null) {
                        if (oldPresentationId instanceof Presentation)
                            oldPresentationId = ((Presentation) oldPresentationId).getId();

                        com.vaadin.ui.MenuBar.MenuItem lastMenuItem = presentationsMenuMap.get(oldPresentationId);
                        if (lastMenuItem != null)
                            removeCurrentItemStyle(lastMenuItem);
                    }

                    Presentation current = presentations.getCurrent();
                    if (current != null) {
                        com.vaadin.ui.MenuBar.MenuItem menuItem = presentationsMenuMap.get(current.getId());
                        if (menuItem != null)
                            setCurrentItemStyle(menuItem);
                    }

                    buildActions();
                }
            }

            @Override
            public void presentationsSetChanged(Presentations presentations) {
                build();
            }
        });

        build();
    }

    protected void removeCurrentItemStyle(com.vaadin.ui.MenuBar.MenuItem item) {
        item.setStyleName("");
    }

    protected void setCurrentItemStyle(com.vaadin.ui.MenuBar.MenuItem item) {
        item.setStyleName("current");
    }

    protected void initLayout() {
        setSpacing(true);

        Label titleLabel = new Label(getMessage("PresentationsPopup.title"));
        titleLabel.setStyleName("cuba-table-presentations-title");
        titleLabel.setWidth("-1px");
        addComponent(titleLabel);
        setComponentAlignment(titleLabel, Alignment.MIDDLE_CENTER);

        menuBar = new CubaMenuBar();
        menuBar.setStyleName("cuba-table-presentations-list");
        menuBar.setWidth("100%");
        menuBar.setHeight("-1px");
        menuBar.setVertical(true);
        addComponent(menuBar);

        button = new WebPopupButton();
        button.setCaption(getMessage("PresentationsPopup.actions"));
        addComponent(button.<Component>getComponent());
        setComponentAlignment(button.<Component>getComponent(), Alignment.MIDDLE_CENTER);

        textSelectionCheckBox = new CheckBox();
        textSelectionCheckBox.setImmediate(true);
        textSelectionCheckBox.setInvalidCommitted(true);
        textSelectionCheckBox.setCaption(getMessage("PresentationsPopup.textSelection"));
        addComponent(textSelectionCheckBox);
        textSelectionCheckBox.setPropertyDataSource(new AbstractProperty() {
            @Override
            public Object getValue() {
                return tableImpl.isTextSelectionEnabled();
            }

            @Override
            public void setValue(Object newValue) throws Property.ReadOnlyException {
                if (newValue instanceof Boolean) {
                    tableImpl.setTextSelectionEnabled((Boolean) newValue);
                }
            }

            @Override
            public Class getType() {
                return Boolean.class;
            }
        });
    }

    public void build() {
        button.setPopupVisible(false);
        buildPresentationsList();
        buildActions();
    }

    public void updateTextSelection() {
        textSelectionCheckBox.setValue(tableImpl.isTextSelectionEnabled());
    }

    protected void buildPresentationsList() {
        menuBar.removeItems();
        presentationsMenuMap = new HashMap<>();

        final Presentations p = table.getPresentations();

        for (final Object presId : p.getPresentationIds()) {
            final MenuBar.MenuItem item = menuBar.addItem(
                    buildItemCaption(p.getCaption(presId)),
                    new com.vaadin.ui.MenuBar.Command() {
                        @Override
                        public void menuSelected(com.vaadin.ui.MenuBar.MenuItem selectedItem) {
                            table.applyPresentation(presId);
                        }
                    }
            );
            final Presentation current = p.getCurrent();
            if (current != null && presId.equals(current.getId())) {
                setCurrentItemStyle(item);
            }
            presentationsMenuMap.put(presId, item);
        }
    }

    protected void buildActions() {
        final Collection<Action> actions = new ArrayList<>(button.getActions());
        for (final Action action : actions) {
            button.removeAction(action);
        }

        final Presentations p = table.getPresentations();
        final Presentation current = p.getCurrent();

        button.addAction(new AbstractAction(getMessage("PresentationsPopup.saveAs")) {
            @Override
            public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                tableImpl.hidePresentationsPopup();

                Presentation presentation = new Presentation();
                presentation.setComponentId(ComponentsHelper.getComponentPath(table));

                openEditor(presentation);
            }
        });
        button.addAction(new AbstractAction(getMessage("PresentationsPopup.reset")) {
            @Override
            public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                table.resetPresentation();
            }
        });

        final boolean allowGlobalPresentations = AppBeans.get(UserSessionSource.class).getUserSession()
                .isSpecificPermitted("cuba.gui.presentations.global");
        if (current != null && (!p.isGlobal(current) || allowGlobalPresentations)) {
            button.addAction(new AbstractAction(getMessage("PresentationsPopup.save")) {
                @Override
                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                    tableImpl.hidePresentationsPopup();

                    Element e = p.getSettings(current);
                    table.saveSettings(e);
                    p.setSettings(current, e);
                    p.commit();
                }
            });
            button.addAction(new AbstractAction(getMessage("PresentationsPopup.edit")) {
                @Override
                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                    tableImpl.hidePresentationsPopup();

                    openEditor(current);
                }
            });
            button.addAction(new AbstractAction(getMessage("PresentationsPopup.delete")) {
                @Override
                public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
                    tableImpl.hidePresentationsPopup();

                    p.remove(current);
                    p.commit();
                }
            });
        }
    }

    protected void openEditor(Presentation presentation) {
        PresentationEditor window = new PresentationEditor(presentation, table);
        AppUI.getCurrent().addWindow(window);
        window.center();
    }

    protected static String buildItemCaption(String caption) {
        if (caption == null) return "";
        return caption;
    }

    protected String getMessage(String key) {
        return messages.getMessage(getClass(), key);
    }
}