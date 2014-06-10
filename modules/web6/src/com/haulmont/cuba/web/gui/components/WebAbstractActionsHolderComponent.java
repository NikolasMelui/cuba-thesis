/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.KeyCombination;
import com.haulmont.cuba.gui.components.ShortcutsDelegate;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.*;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

/**
 * @param <T>
 * @author abramov
 * @version $Id$
 */
public abstract class WebAbstractActionsHolderComponent<T extends com.vaadin.ui.Component & com.vaadin.event.Action.ShortcutNotifier>
        extends WebAbstractComponent<T> {

    protected VerticalLayout contextMenuPopup;
    protected final List<ContextMenuButton> contextMenuButtons = new LinkedList<>();

    protected List<Action> actionList = new LinkedList<>();

    protected final ShortcutsDelegate<ShortcutListener> shortcutsDelegate;

    public WebAbstractActionsHolderComponent() {
        contextMenuPopup = new VerticalLayout();
        contextMenuPopup.setCubaId("cubaContextMenu");
        contextMenuPopup.setStyleName("cuba-context-menu-container");
        contextMenuPopup.setSizeUndefined();

        shortcutsDelegate = new ShortcutsDelegate<ShortcutListener>() {
            @Override
            protected ShortcutListener attachShortcut(final String actionId, KeyCombination keyCombination) {
                ShortcutListener shortcut = new ShortcutListener(actionId, keyCombination.getKey().getCode(),
                        KeyCombination.Modifier.codes(keyCombination.getModifiers())) {

                    @Override
                    public void handleAction(Object sender, Object target) {
                        if (target == component) {
                            Action action = getAction(actionId);
                            if (action != null && action.isEnabled() && action.isVisible()) {
                                action.actionPerform(WebAbstractActionsHolderComponent.this);
                            }
                        }
                    }
                };
                component.addShortcutListener(shortcut);
                return shortcut;
            }

            @Override
            protected void detachShortcut(Action action, ShortcutListener shortcutDescriptor) {
                component.removeShortcutListener(shortcutDescriptor);
            }

            @Override
            protected Collection<Action> getActions() {
                return WebAbstractActionsHolderComponent.this.getActions();
            }
        };
    }

    public void addAction(final Action action) {
        checkNotNullArgument(action, "action must be non null");

        Action oldAction = getAction(action.getId());

        boolean added = false;
        for (int i = 0; i < actionList.size(); i++) {
            Action a = actionList.get(i);
            if (ObjectUtils.equals(a.getId(), action.getId())) {
                actionList.set(i, action);
                added = true;
                break;
            }
        }
        if (!added) {
            actionList.add(action);
        }
        action.refreshState();

        Component oldButton = null;

        if (oldAction != null) {
            ContextMenuButton oldActionButton = null;

            for (ContextMenuButton button : contextMenuButtons) {
                if (button.getAction() == oldAction) {
                    oldActionButton = button;
                    break;
                }
            }

            if (oldActionButton != null) {
                oldActionButton.setAction(null);
                contextMenuButtons.remove(oldActionButton);

                oldButton = WebComponentsHelper.unwrap(oldActionButton);
            }
        }

        if (StringUtils.isNotEmpty(action.getCaption())) {
            ContextMenuButton contextMenuButton = createContextMenuButton();
            contextMenuButton.setStyleName("cuba-context-menu-button");
            contextMenuButton.setAction(action);

            contextMenuButtons.add(contextMenuButton);

            Component newVButton = WebComponentsHelper.unwrap(contextMenuButton);
            if (oldButton == null) {
                contextMenuPopup.addComponent(newVButton);
            } else {
                contextMenuPopup.replaceComponent(oldButton, newVButton);
            }
        } else {
            if (oldButton != null) {
                contextMenuPopup.removeComponent(oldButton);
            }
        }

        shortcutsDelegate.addAction(oldAction, action);
    }

    protected abstract ContextMenuButton createContextMenuButton();

    public void removeAction(Action action) {
        if (action == null) {
            return;
        }

        actionList.remove(action);

        ContextMenuButton actionButton = null;
        for (ContextMenuButton button : contextMenuButtons) {
            if (button.getAction() == action) {
                actionButton = button;
                break;
            }
        }

        if (actionButton != null) {
            actionButton.setAction(null);
            contextMenuButtons.remove(actionButton);

            contextMenuPopup.removeComponent(WebComponentsHelper.unwrap(actionButton));
        }

        shortcutsDelegate.removeAction(action);
    }

    public void removeAction(String id) {
        Action action = getAction(id);
        if (action != null) {
            removeAction(action);
        }
    }

    public void removeAllActions() {
        for (Action action : new ArrayList<>(actionList)) {
            removeAction(action);
        }
    }

    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actionList);
    }

    @Nullable
    public Action getAction(String id) {
        for (Action action : getActions()) {
            if (ObjectUtils.equals(action.getId(), id)) {
                return action;
            }
        }
        return null;
    }
}