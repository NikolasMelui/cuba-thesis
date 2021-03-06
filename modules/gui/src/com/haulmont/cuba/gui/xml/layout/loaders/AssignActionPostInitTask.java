/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.GuiDevelopmentException;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

/**
 * @author abramov
 * @version $Id$
 */
public class AssignActionPostInitTask implements ComponentLoader.PostInitTask {

    protected Component.ActionOwner component;
    protected String actionName;
    private IFrame frame;

    public AssignActionPostInitTask(Component.ActionOwner component, String actionName, IFrame frame) {
        this.component = component;
        this.actionName = actionName;
        this.frame = frame;
    }

    @Override
    public void execute(ComponentLoader.Context context, IFrame window) {
        final String[] elements = ValuePathHelper.parse(actionName);
        if (elements.length > 1) {
            final String id = elements[elements.length - 1];
            String[] subPath = (String[]) ArrayUtils.subarray(elements, 0, elements.length - 1);

            // using this.frame to look up the component inside the actual frame
            final Component holder = this.frame.getComponent(ValuePathHelper.format(subPath));
            if (holder != null) {
                if (holder instanceof Component.ActionsHolder) {
                    final Action action = ((Component.ActionsHolder) holder).getAction(id);
                    if (action != null) {
                        this.component.setAction(action);
                    } else {
                        throw new GuiDevelopmentException(String.format(
                                "Can't find action '%s' in '%s'", id, holder.getId()), context.getFullFrameId(),
                                "Holder ID", holder.getId());
                    }
                } else {
                    throw new GuiDevelopmentException(String.format(
                            "Component '%s' can't contain actions", holder.getId()), context.getFullFrameId(),
                            "Holder ID", holder.getId());
                }
            } else {
                throw new GuiDevelopmentException(
                        "Can't find component: " + Arrays.toString(subPath) + " for action: " + actionName,
                        context.getFullFrameId(), "Component ID", Arrays.toString(subPath));
            }
        } else if (elements.length == 1) {
            final String id = elements[0];
            final Action action = window.getAction(id);

            if (action != null) {
                this.component.setAction(action);
            } else {
                String message = "Can't find action " + id;
                if (Window.Editor.WINDOW_COMMIT.equals(id) || Window.Editor.WINDOW_COMMIT_AND_CLOSE.equals(id))
                    message += ". This may happen if you are opening an AbstractEditor-based screen by openWindow() method, " +
                            "for example from the main menu. Use openEditor() method or give the screen a name ended " +
                            "with '.edit' to open it as editor from the main menu.";
                throw new GuiDevelopmentException(message, context.getFullFrameId());
            }
        } else {
            throw new GuiDevelopmentException("Empty action name", context.getFullFrameId());
        }
    }
}