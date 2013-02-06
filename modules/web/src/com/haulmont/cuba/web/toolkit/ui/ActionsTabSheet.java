/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * Author: Nikolay Gorodnov
 * Created: 14.03.2011 16:07:19
 *
 * $Id$
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.vaadin.ui.Component;

import java.util.Stack;

// vaadin7 Actions support
public class ActionsTabSheet extends com.vaadin.ui.TabSheet /*implements Action.Container*/ {

    private Stack<Component> openedComponents = new Stack<>();

    /*private static final long serialVersionUID = -2956008661221108673L;

    protected TabSheetActionsManager actionManager;

    protected TabSheetActionsManager getActionManager() {
        if (actionManager == null) {
            actionManager = new TabSheetActionsManager(this);
        }
        return actionManager;
    }

    @Override
    public void addActionHandler(Action.Handler actionHandler) {
        getActionManager().addActionHandler(actionHandler);
    }

    @Override
    public void removeActionHandler(Action.Handler actionHandler) {
        if (actionManager != null) {
            getActionManager().removeActionHandler(actionHandler);
        }
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);
        if (actionManager != null) {
            getActionManager().paintActions(null, target);
        }
    }

    @Override
    public void changeVariables(Object source, Map variables) {
        if (variables.containsKey("close")) {
            final Component tab = keyMapper.get((String) variables.get("close"));
            if (tab != null) {
                closeTabAndSelectPrevious(tab);
            }
        } else {
            super.changeVariables(source, variables);
        }
        if (actionManager != null) {
            getActionManager().handleActions(variables, this);
        }
    }         */

    public void closeTabAndSelectPrevious(Component tab) {
        while (openedComponents.removeElement(tab))
            openedComponents.removeElement(tab);
        if ((!openedComponents.empty()) && (getSelectedTab().equals(tab))) {
            Component c = openedComponents.pop();
            while (!components.contains(c) && !openedComponents.isEmpty())
                c = openedComponents.pop();
            setSelectedTab(c);
        }
        closeHandler.onTabClose(this, tab);
        removeComponent(tab);
    }

    public void silentCloseTabAndSelectPrevious(Component tab) {
        while (openedComponents.removeElement(tab))
            openedComponents.removeElement(tab);
        if ((!openedComponents.empty()) && (selected.equals(tab))) {
            Component c = openedComponents.pop();
            while (!components.contains(c) && !openedComponents.isEmpty())
                c = openedComponents.pop();
            setSelectedTab(c);
        }
    }

    @Override
    public void setSelectedTab(Component c) {
        if (c != null && components.contains(c) && !c.equals(selected)) {
            setSelected(c);
            openedComponents.push(c);
            updateSelection();
            fireSelectedTabChange();
            markAsDirty();
        }
    }

    protected void closeTab(Component tab) {
        while (openedComponents.removeElement(tab))
                    openedComponents.removeElement(tab);
        if (closeHandler != null) {
            closeHandler.onTabClose(this, tab);
        }
    }

    /*
    @Override
    public void attach() {
        super.attach();
        if (actionManager != null) {
            getActionManager().setViewer(this);
        }
    }           */
    /*
    @Override
    protected void paintTab(PaintTarget target, Component component, Tab tab) throws PaintException {
        target.startTag("tab");
        if (!tab.isEnabled() && tab.isVisible()) {
            target.addAttribute("disabled", true);
        }

        if (!tab.isVisible()) {
            target.addAttribute("hidden", true);
        }

        if (tab.isClosable()) {
            target.addAttribute("closable", true);
        }

        final Resource icon = tab.getIcon();
        if (icon != null) {
            target.addAttribute("icon", icon);
        }
        final String caption = tab.getCaption();
        if (caption != null && caption.length() > 0) {
            target.addAttribute("caption", caption);
        }

        final String description = tab.getDescription();
        if (description != null) {
            target.addAttribute("description", description);
        }

        final ErrorMessage componentError = tab.getComponentError();
        if (componentError != null) {
            componentError.paint(target);
        }

        target.addAttribute("key", keyMapper.key(component));
        if (component.equals(getSelectedTab())) {
            target.addAttribute("selected", true);
            component.paint(target);
            paintedTabs.add(component);
        } else if (paintedTabs.contains(component)) {
            component.paint(target);
        } else {
            component.requestRepaintRequests();
        }
        paintTabActions(target, component, tab);
        target.endTag("tab");
    }

    protected void paintTabActions(PaintTarget target, Component component, Tab tab) throws PaintException {
        if (actionManager != null) {
            target.addAttribute("al", getActionManager().getActionsKeys(component, this));
        }
    }

    private class TabSheetActionsManager extends ActionManager {
        private static final long serialVersionUID = -8215673657372064982L;

        private <T extends Component & Action.Container> TabSheetActionsManager(T viewer) {
            super(viewer);
            actionMapper = new KeyMapper(); //fixes an issue when actionMapper are reset to null during repainting
        }

        @Override
        public void paintActions(Object actionTarget, PaintTarget paintTarget) throws PaintException {
            List<Action> actions = new ArrayList<Action>();
            if (actionHandlers != null) {
                for (Action.Handler handler : actionHandlers) {
                    Action[] as = handler.getActions(actionTarget, this.viewer);
                    if (as != null) {
                        actions.addAll(Arrays.asList(as));
                    }
                }
            }
            if (ownActions != null) {
                actions.addAll(ownActions);
            }
            if (!actions.isEmpty() || clientHasActions) {
                paintActions(paintTarget, actions);
            }
            clientHasActions = !actions.isEmpty();
        }

        public String[] getActionsKeys(Object target, Object sender) {
            final Action[] actions = getActions(target, sender);
            if (actions.length == 0) {
                return new String[0];
            }
            final List<String> actionKeys = new ArrayList<String>(actions.length);
            for (final Action action : actions) {
                actionKeys.add(actionMapper.key(action));
            }
            return actionKeys.toArray(new String[actionKeys.size()]);
        }

        @Override
        public void handleAction(Action action, Object sender, Object target) {
            String tabKey = (String) target;
            Component tab = (Component) keyMapper.get(tabKey);
            super.handleAction(action, sender, tab);
        }
    }*/
}