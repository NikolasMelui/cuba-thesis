package com.haulmont.cuba.web.gui.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.itmill.toolkit.event.Action;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class AbstractActionOwnerComponent<T extends com.itmill.toolkit.ui.Component> extends AbstractComponent<T> {
    protected List<com.haulmont.cuba.gui.components.Action> actionsOrder = new LinkedList<com.haulmont.cuba.gui.components.Action>();
    protected BiMap<com.haulmont.cuba.gui.components.Action, Action> actions = new HashBiMap<com.haulmont.cuba.gui.components.Action,Action>();

    public void addAction(final com.haulmont.cuba.gui.components.Action action) {
        actions.put(action, new ActionWrapper(action));
        actionsOrder.add(action);
    }

    public void removeAction(com.haulmont.cuba.gui.components.Action action) {
        actions.remove(action);
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

    protected class ActionsAdapter implements com.itmill.toolkit.event.Action.Handler {
        public com.itmill.toolkit.event.Action[] getActions(Object target, Object sender) {
            final List<Action> res = new ArrayList<Action>();
            for (com.haulmont.cuba.gui.components.Action action : actionsOrder) {
//                if (action.isEnabled()) {
                    res.add(actions.get(action));
//                }
            }
            return res.toArray(new Action[res.size()]);
        }

        public void handleAction(Action tableAction, Object sender, Object target) {
            final com.haulmont.cuba.gui.components.Action action = actions.inverse().get(tableAction);
            if (action != null) {
                action.actionPerform(AbstractActionOwnerComponent.this);
            }
        }
    }
}
