/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.KeyCombination;
import com.haulmont.cuba.web.toolkit.VersionedThemeResource;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;
import org.apache.commons.lang.StringUtils;
import org.vaadin.hene.popupbutton.PopupButton;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author pavlov
 * @version $Id$
 */
public class WebPopupButton
        extends
            WebAbstractComponent<org.vaadin.hene.popupbutton.PopupButton>
        implements
            com.haulmont.cuba.gui.components.PopupButton {

    private Component popupComponent;
    private com.vaadin.ui.Component vPopupComponent;
    private String icon;

    private List<Action> actionOrder = new LinkedList<>();

    public WebPopupButton() {
        component = new PopupButton();
        component.setImmediate(true);

        vPopupComponent = new VerticalLayout();
        vPopupComponent.addStyleName("cuba-popupmenu");
        ((VerticalLayout) vPopupComponent).setMargin(false);
        vPopupComponent.setSizeUndefined();
        component.setContent(vPopupComponent);
    }

    @Override
    public String getCaption() {
        return component.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        component.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return component.getDescription();
    }

    @Override
    public void setDescription(String description) {
        component.setDescription(description);
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
        if (!StringUtils.isEmpty(icon)) {
            component.setIcon(new VersionedThemeResource(icon));
            component.addStyleName(WebButton.ICON_STYLE);
        } else {
            component.setIcon(null);
            component.removeStyleName(WebButton.ICON_STYLE);
        }
    }

    public void setPopupComponent(Component component) {
        this.popupComponent = component;
        vPopupComponent = WebComponentsHelper.unwrap(popupComponent);
        this.component.setContent(vPopupComponent);
    }

    public void removePopupComponent() {
        popupComponent = null;
        this.component.setContent(null);
        vPopupComponent = null;
    }

    public Component getPopupComponent() {
        return popupComponent;
    }

    @Override
    public boolean isPopupVisible() {
        return component.isPopupVisible();
    }

    @Override
    public void setPopupVisible(boolean popupVisible) {
        component.setPopupVisible(popupVisible);
    }

    @Override
    public void setMenuWidth(String width) {
        if (vPopupComponent != null && width != null) {
            vPopupComponent.setWidth(width);    
        }
    }

    @Override
    public boolean isAutoClose() {
        return false;
//        vaadin7
//        return component.isAutoClose();
    }

    @Override
    public void setAutoClose(boolean autoClose) {
//        vaadin7
//        component.setAutoClose(autoClose);
    }

    @Override
    public void addAction(final Action action) {
        if (action != null && vPopupComponent instanceof com.vaadin.ui.Layout) {
            WebButton button = new WebButton();
            button.setAction(new PopupActionWrapper(action));
            button.setIcon(null); // don't show icons to look the same as Table actions

            com.vaadin.ui.Button vButton = (com.vaadin.ui.Button) button.getComposition();
            vButton.setImmediate(true);            
            vButton.setSizeFull();
            vButton.setStyleName(BaseTheme.BUTTON_LINK);

            ((com.vaadin.ui.Layout) vPopupComponent).addComponent(vButton);
            component.markAsDirty();
            actionOrder.add(action);
        }
    }

    @Override
    public void removeAction(Action action) {
        if (vPopupComponent instanceof com.vaadin.ui.Layout && actionOrder.remove(action)) {
            ((com.vaadin.ui.Layout) vPopupComponent).removeComponent(WebComponentsHelper.unwrap((Component) action.getOwner()));
        }
    }

    @Override
    public Action getAction(String id) {
        if (vPopupComponent instanceof com.vaadin.ui.Layout && id != null) {
            for (Action action : actionOrder) {
                if (id.equals(action.getId())) {
                    return action;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actionOrder);
    }

    private class PopupActionWrapper implements Action {

        private Action action;

        private PopupActionWrapper(Action action) {
            this.action = action;
        }

        @Override
        public void actionPerform(Component component) {
            WebPopupButton.this.component.setPopupVisible(false);

            action.actionPerform(component);
        }

        @Override
        public void addOwner(ActionOwner actionOwner) {
            action.addOwner(actionOwner);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            action.addPropertyChangeListener(listener);
        }

        @Override
        public String getCaption() {
            return action.getCaption();
        }

        @Override
        public String getIcon() {
            return action.getIcon();
        }

        @Override
        public String getId() {
            return action.getId();
        }

        @Override
        public ActionOwner getOwner() {
            return action.getOwner();
        }

        @Override
        public Collection<ActionOwner> getOwners() {
            return action.getOwners();
        }

        @Override
        public boolean isEnabled() {
            return action.isEnabled();
        }

        @Override
        public boolean isVisible() {
            return action.isVisible();
        }

        @Override
        public void removeOwner(ActionOwner actionOwner) {
            action.removeOwner(actionOwner);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            action.removePropertyChangeListener(listener);
        }

        @Override
        public void setCaption(String caption) {
            action.setCaption(caption);
        }

        @Override
        public KeyCombination getShortcut() {
            return null;
        }

        @Override
        public void setShortcut(KeyCombination shortcut) {
        }

        @Override
        public void setShortcut(String shortcut) {
        }

        @Override
        public void setEnabled(boolean enabled) {
            action.setEnabled(enabled);
        }

        @Override
        public void setIcon(String icon) {
            action.setIcon(icon);
        }

        @Override
        public void setVisible(boolean visible) {
            action.setVisible(visible);
        }

        @Override
        public void refreshState() {
            action.refreshState();
        }
    }
}