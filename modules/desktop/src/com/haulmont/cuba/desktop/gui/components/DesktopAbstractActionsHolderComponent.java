/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.gui.components.Action;
import org.apache.commons.lang.ObjectUtils;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopAbstractActionsHolderComponent<C extends JComponent> extends DesktopAbstractComponent<C> {

    protected java.util.List<Action> actionList = new LinkedList<com.haulmont.cuba.gui.components.Action>();

    public void addAction(Action action) {
        for (int i = 0; i < actionList.size(); i++) {
            Action a = actionList.get(i);
            if (ObjectUtils.equals(a.getId(), action.getId())) {
                actionList.set(i, action);
                return;
            }
        }
        actionList.add(action);
    }

    public void removeAction(Action action) {
        actionList.remove(action);
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
}
