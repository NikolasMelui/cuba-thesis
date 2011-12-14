/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.ListComponent;
import com.haulmont.cuba.gui.components.actions.*;
import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Utility class working with GenericUI components.
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class ComponentsHelper {
    public static final String[] UNIT_SYMBOLS = { "px", "pt", "pc", "em", "ex",
            "mm", "cm", "in", "%" };

    /**
     * Returns the collection of components within the specified container and all of its children.
     * @param container container to start from
     * @return          collection of components
     */
    public static Collection<Component> getComponents(Component.Container container) {
        final Collection<Component> ownComponents = container.getOwnComponents();
        Set<Component> res = new HashSet<Component>(ownComponents);

        for (Component component : ownComponents) {
            if (component instanceof Component.Container) {
                res.addAll(getComponents((Component.Container) component));
            }
        }

        return res;
    }

    /**
     * Searches for a component by identifier, down by the hierarchy of frames.
     * @param frame frame to start from
     * @param id    component identifier
     * @return      component instance or null if not found
     */
    @Nullable
    public static Component findComponent(IFrame frame, String id) {
        Component find = frame.getComponent(id);
        if (find != null) {
            return find;
        } else {
            for (Component c : frame.getComponents()) {
                if (c instanceof IFrame) {
                    Component comp = ((IFrame) c).getComponent(id);
                    if (comp != null) {
                        return comp;
                    } else {
                        findComponent((IFrame) c, id);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Visit all components below the specified container.
     * @param container container to start from
     * @param visitor   visitor instance
     */
    public static void walkComponents(
            com.haulmont.cuba.gui.components.Component.Container container,
            ComponentVisitor visitor
    ) {
        __walkComponents(container, visitor, "");
    }

    private static void __walkComponents(
            com.haulmont.cuba.gui.components.Component.Container container,
            ComponentVisitor visitor,
            String path
    ) {
        for (com.haulmont.cuba.gui.components.Component component : container.getOwnComponents()) {
            String id = component.getId();
            if (id == null && component instanceof Component.ActionOwner
                    && ((Component.ActionOwner) component).getAction() != null) {
                id = ((Component.ActionOwner) component).getAction().getId();
            }
            visitor.visit(component, path + id);

            if (component instanceof com.haulmont.cuba.gui.components.Component.Container) {
                String p = component instanceof IFrame ?
                        path + component.getId() + "." :
                        path;
                __walkComponents(((com.haulmont.cuba.gui.components.Component.Container) component), visitor, p);
            }
        }
    }

    /**
     * Get the topmost window for the specified component.
     * @param component component instance
     * @return          topmost window in the hierarchy of frames for this component. Can be null only if the component
     * was not properly initialized.
     */
    public static Window getWindow(Component.BelongToFrame component) {
        IFrame frame = component.getFrame();
        while (frame != null) {
            if (frame instanceof Window)
                return (Window) frame;
            frame = frame.getFrame();
        }
        return null;
    }

    /**
     * Searches for an action by name.
     * @param actionName    action name, can be a path to an action contained in some {@link Component.ActionsHolder}
     * @param frame         current frame
     * @return              action instance or null if there is no action with the specified name
     * @throws IllegalStateException    if the component denoted by the path doesn't exist or is not an ActionsHolder
     */
    @Nullable
    public static Action findAction(String actionName, IFrame frame) {
        String[] elements = ValuePathHelper.parse(actionName);
        if (elements.length > 1) {
            String id = elements[elements.length - 1];

            String[] subPath = (String[]) ArrayUtils.subarray(elements, 0, elements.length - 1);
            Component component = frame.getComponent(ValuePathHelper.format(subPath));
            if (component != null) {
                if (component instanceof Component.ActionsHolder) {
                    return ((Component.ActionsHolder) component).getAction(id);
                } else {
                    throw new IllegalArgumentException(
                            String.format("Component '%s' can't contain actions", Arrays.toString(subPath)));
                }
            } else {
                throw new IllegalArgumentException(
                        String.format("Can't find component '%s'", Arrays.toString(subPath)));
            }
        } else if (elements.length == 1) {
            String id = elements[0];
            return frame.getAction(id);
        } else {
            throw new IllegalArgumentException("Invalid action name: " + actionName);
        }
    }

    public static String getComponentPath(Component c) {
        StringBuilder sb = new StringBuilder(c.getId() == null ? "" : c.getId());
        if (c instanceof Component.BelongToFrame) {
            IFrame frame = ((Component.BelongToFrame) c).getFrame();
            while (frame != null) {
                sb.insert(0, ".");
                String s = frame.getId();
                if (s.contains("."))
                    s = "[" + s + "]";
                sb.insert(0, s);
                if (frame instanceof Window)
                    break;
                frame = frame.getFrame();
            }
        }
        return sb.toString();
    }

    public static String getComponentWidth(Component c) {
        float width = c.getWidth();
        int widthUnit = c.getWidthUnits();
        return width + UNIT_SYMBOLS[widthUnit];
    }

    public static String getComponentHeigth(Component c) {
        float height = c.getHeight();
        int heightUnit = c.getHeightUnits();
        return height + UNIT_SYMBOLS[heightUnit];
    }

    /**
     * Creates standard Create, Edit and Remove actions for the component
     * @param owner List, Table or Tree component
     */
    public static void createActions(ListComponent owner) {
        createActions(owner, EnumSet.of(ListActionType.CREATE, ListActionType.EDIT, ListActionType.REMOVE));
    }

    /**
     * Creates standard actions for the component
     * @param owner List, Table or Tree component
     * @param actions set of actions to create
     */
    public static void createActions(ListComponent owner, EnumSet<ListActionType> actions) {
        if (actions.contains(ListActionType.CREATE))
            owner.addAction(new CreateAction(owner));

        if (actions.contains(ListActionType.EDIT))
            owner.addAction(new EditAction(owner));

        if (actions.contains(ListActionType.REMOVE))
            owner.addAction(new RemoveAction(owner));

        if (actions.contains(ListActionType.REFRESH))
            owner.addAction(new RefreshAction(owner));
    }
}
