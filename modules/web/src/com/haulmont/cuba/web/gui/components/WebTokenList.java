/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 20.07.2010 15:40:53
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.CollectionDatasourceListener;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.toolkit.ui.CustomField;
import com.haulmont.cuba.web.toolkit.ui.ScrollablePanel;
import com.haulmont.cuba.web.toolkit.ui.TokenListLabel;
import com.vaadin.data.Property;
import com.vaadin.terminal.KeyMapper;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebTokenList extends WebAbstractComponent<WebTokenList.TokenListImpl> implements TokenList {

    private static final long serialVersionUID = -6490244006772570832L;

    private CollectionDatasource datasource;

    private String captionProperty;

    private CaptionMode captionMode;

    private Position position = Position.TOP;

    private ItemChangeHandler itemChangeHandler;

    private boolean inline;

    private WebButton button;
    private WebActionsField actionsField;

    private ActionsFieldHelper actionsHelper;

    private MetaClass metaClass;
    private String lookupScreen;

    private boolean lookup;

    public WebTokenList() {
        button = new WebButton();
        button.setCaption("Add");
        actionsField = new WebActionsField();
        actionsField.enableButton(ActionsField.DROPDOWN, true);
        component = new TokenListImpl();
    }

    public CollectionDatasource getDatasource() {
        return datasource;
    }

    public void setDatasource(CollectionDatasource datasource) {
        this.datasource = datasource;

        datasource.addListener(new CollectionDatasourceListener() {
            public void collectionChanged(CollectionDatasource ds, Operation operation) {
                if (actionsHelper == null) {
                    actionsHelper = new ActionsFieldHelper(actionsField, metaClass);
                    if (isLookup()) {
                        if (getLookupScreen() != null) {
                            actionsHelper.createLookupAction(getLookupScreen());
                        } else {
                            actionsHelper.createLookupAction();
                        }
                    }
                }
                component.refreshComponent();
            }

            public void itemChanged(Datasource ds, Entity prevItem, Entity item) {
            }

            public void stateChanged(Datasource ds, Datasource.State prevState, Datasource.State state) {
            }

            public void valueChanged(Object source, String property, Object prevValue, Object value) {
            }
        });
    }

    @Override
    public void setFrame(IFrame frame) {
        super.setFrame(frame);
        actionsField.setFrame(frame);
    }

    public String getCaptionProperty() {
        return captionProperty;
    }

    public void setCaptionProperty(String captionProperty) {
        this.captionProperty = captionProperty;
    }

    public CaptionMode getCaptionMode() {
        return captionMode;
    }

    public void setCaptionMode(CaptionMode captionMode) {
        this.captionMode = captionMode;
    }

    public LookupField.FilterMode getFilterMode() {
        return actionsField.getFilterMode();
    }

    public void setFilterMode(LookupField.FilterMode mode) {
        actionsField.setFilterMode(mode);
    }

    public String getOptionsCaptionProperty() {
        return actionsField.getCaptionProperty();
    }

    public void setOptionsCaptionProperty(String captionProperty) {
        actionsField.setCaptionProperty(captionProperty);
    }

    public CaptionMode getOptionsCaptionMode() {
        return actionsField.getCaptionMode();
    }

    public void setOptionsCaptionMode(CaptionMode captionMode) {
        actionsField.setCaptionMode(captionMode);
    }

    public CollectionDatasource getOptionsDatasource() {
        return actionsField.getOptionsDatasource();
    }

    public void setOptionsDatasource(CollectionDatasource datasource) {
        actionsField.setOptionsDatasource(datasource);
        if (datasource != null) {
            metaClass = datasource.getMetaClass();
        }
    }

    public List getOptionsList() {
        return actionsField.getOptionsList();
    }

    public void setOptionsList(List optionsList) {
        actionsField.setOptionsList(optionsList);
    }

    public Map<String, Object> getOptionsMap() {
        return actionsField.getOptionsMap();
    }

    public void setOptionsMap(Map<String, Object> map) {
        actionsField.setOptionsMap(map);
    }

    public boolean isLookup() {
        return lookup;
    }

    public void setLookup(boolean lookup) {
        this.lookup = lookup;
        actionsField.enableButton(ActionsField.DROPDOWN, !lookup);
        actionsField.enableButton(ActionsField.LOOKUP, lookup);
        if (getOptionsDatasource() != null) {
            metaClass = getOptionsDatasource().getMetaClass();
        }
    }

    public String getLookupScreen() {
        return lookupScreen;
    }

    public void setLookupScreen(String lookupScreen) {
        this.lookupScreen = lookupScreen;
    }

    public String getAddButtonCaption() {
        return button.getCaption();
    }

    public void setAddButtonCaption(String caption) {
        button.setCaption(caption);
    }

    public String getAddButtonIcon() {
        return button.getIcon();
    }

    public void setAddButtonIcon(String icon) {
        button.setIcon(icon);
    }

    public ItemChangeHandler getItemChangeHandler() {
        return itemChangeHandler;
    }

    public void setItemChangeHandler(ItemChangeHandler handler) {
        this.itemChangeHandler = handler;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getCaption() {
        return component.getCaption();
    }

    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    public String getDescription() {
        return null;
    }

    public void setDescription(String description) {
    }

    protected String instanceCaption(Instance instance) {
        if (instance == null) { return ""; }
        if (instance.getMetaClass().getPropertyEx(captionProperty) != null) {
            return instance.getValueEx(captionProperty).toString();
        }
        throw new IllegalArgumentException(String.format("Couldn't find property with name '%s'",
                captionProperty));
    }

    public class TokenListImpl extends CustomField {

        private VerticalLayout root;

        private Panel container;

        private Component editor;

        private Map<Instance, Component> itemComponents = new HashMap<Instance, Component>();
        private Map<Component, Instance> componentItems = new HashMap<Component, Instance>();
        private KeyMapper componentsMapper = new KeyMapper();

        public TokenListImpl() {
            root = new VerticalLayout();
            root.setSpacing(true);
            root.setSizeFull();

            container = new ScrollablePanel();
            CssLayout layout = new CssLayout();
            container.setContent(layout);
            container.setSizeFull();

            root.addComponent(container);
            root.setExpandRatio(container, 1);

            setCompositionRoot(root);

            setStyleName("token-list");

            initField();
        }

        protected void initField() {
            final HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setWidth("100%");

            actionsField.setWidth("100%");
            Component lookupComponent = WebComponentsHelper.unwrap(actionsField);
            lookupComponent.setWidth("100%");

            layout.addComponent(lookupComponent);
            layout.setExpandRatio(lookupComponent, 1);

            button.setStyleName("add-btn");

            Button wrappedButton = (Button) WebComponentsHelper.unwrap(button);
            wrappedButton.addListener(new Button.ClickListener() {
                public void buttonClick(Button.ClickEvent event) {
                    final Entity newItem = actionsField.getValue();
                    if (newItem == null) return;
                    if (itemChangeHandler != null) {
                        itemChangeHandler.addItem(newItem);
                    } else {
                        datasource.addItem(newItem);
                    }
                    actionsField.setValue(null);
                }
            });
            layout.addComponent(wrappedButton);

            editor = layout;
        }

        public void refreshComponent() {
            if (inline) {
                addStyleName("inline");
            }
            if (editor != null) {
                root.removeComponent(editor);
            }
            if (editor == null) {
                throw new IllegalStateException();
            }
            if (position == Position.TOP) {
                root.addComponentAsFirst(editor);
            } else {
                root.addComponent(editor);
            }

            container.removeAllComponents();

            for (final Object itemId : datasource.getItemIds()) {
                final Instance item = (Instance) datasource.getItem(itemId);
                Component f = itemComponents.get(item);
                if (f == null) {
                    f = createToken();
                    itemComponents.put(item, f);
                    componentItems.put(f, item);
                }
                if (captionProperty != null) {
                    ((Property) f).setValue(instanceCaption(item));
                } else {
                    ((Property) f).setValue(item);
                }
                container.addComponent(f);
            }

            requestRepaint();
        }

        protected Component createToken() {
            final TokenListLabel label = new TokenListLabel();
            String key = componentsMapper.key(label);
            label.setKey(key);
            label.addListener(new TokenListLabel.RemoveTokenListener() {
                public void removeToken(TokenListLabel source) {
                    Instance item = componentItems.get(source);
                    if (item != null) {
                        datasource.removeItem((Entity) item);

                        itemComponents.remove(item);
                        componentItems.remove(source);
                    }
                }
            });
            return label;
        }

        @Override
        public Class<?> getType() {
            return List.class;
        }
    }

}
