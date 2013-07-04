/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.vaadin.event.Action;
import com.vaadin.event.ActionManager;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.LegacyComponent;

import java.util.Map;

/**
 * @author gorodnov
 * @version $Id$
 */
public class CubaOrderedActionsLayout extends AbstractOrderedLayout implements Action.Container, LegacyComponent {

    private ActionManager actionManager;

    @Override
    public void addActionHandler(Action.Handler actionHandler) {
        getActionManager().addActionHandler(actionHandler);
        markAsDirty();
    }

    @Override
    public void removeActionHandler(Action.Handler actionHandler) {
        if (actionManager != null) {
            actionManager.removeActionHandler(actionHandler);
            markAsDirty();
        }
    }

    @Override
    protected ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = new ActionManager(this);
        }
        return actionManager;
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        if (actionManager != null) {
            actionManager.paintActions(null, target);
        }
    }

    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        if (actionManager != null) {
            actionManager.handleActions(variables, this);
        }
    }
}