/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.haulmont.cuba.gui.components.Action;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.haulmont.cuba.gui.ComponentsHelper.findActionById;

/**
 * Encapsulates {@link com.haulmont.cuba.gui.components.Component.ActionsHolder} functionality for web frames and
 * windows.
 *
 * @author krivopustov
 */
public class WebFrameActionsHolder {

    protected List<Action> actionList = new LinkedList<>();
    protected BiMap<com.vaadin.event.Action, Action> actions = HashBiMap.create();

    public void addAction(Action action) {
        int index = findActionById(actionList, action.getId());
        if (index < 0) {
            index = actionList.size();
        }

        addAction(action, index);
    }

    public void addAction(Action action, int index) {
        int oldIndex = findActionById(actionList, action.getId());
        if (oldIndex >= 0) {
            removeAction(actionList.get(oldIndex));
            if (index > oldIndex) {
                index--;
            }
        }

        if (action.getShortcut() != null) {
            actions.put(WebComponentsHelper.createShortcutAction(action), action);
        }

        actionList.add(index, action);
    }

    public void removeAction(Action action) {
        if (actionList.remove(action)) {
            actions.inverse().remove(action);
        }
    }

    public void removeAction(String id) {
        Action action = getAction(id);
        if (action != null) {
            removeAction(action);
        }
    }

    public void removeAllActions() {
        actionList.clear();
        actions.clear();
    }

    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actionList);
    }

    public Action getAction(String id) {
        for (Action action : getActions()) {
            if (ObjectUtils.equals(action.getId(), id)) {
                return action;
            }
        }
        return null;
    }

    public com.vaadin.event.Action[] getActionImplementations() {
        List<com.vaadin.event.Action> orderedActions = new LinkedList<>();
        for (Action action : actionList) {
            com.vaadin.event.Action e = actions.inverse().get(action);
            if (e != null) {
                orderedActions.add(e);
            }
        }
        return orderedActions.toArray(new com.vaadin.event.Action[orderedActions.size()]);
    }

    public Action getAction(com.vaadin.event.Action actionImpl) {
        return actions.get(actionImpl);
    }
}