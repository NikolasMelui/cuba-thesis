/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.DetachedFrame;
import com.haulmont.cuba.desktop.TopLevelFrame;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;
import org.apache.commons.lang.ArrayUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.Action;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Collections;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopComponentsHelper {

    public static final int BUTTON_HEIGHT = 30;
    public static final int FIELD_HEIGHT = 28;

    public static Color requiredBgColor = (Color) UIManager.get("cubaRequiredBackground");
    public static Color defaultBgColor = (Color) UIManager.get("nimbusLightBackground");

    public static JComponent unwrap(Component component) {
        Object comp = component;
        while (comp instanceof Component.Wrapper) {
            comp = ((Component.Wrapper) comp).getComponent();
        }
        return (JComponent) comp;
    }

    public static JComponent getComposition(Component component) {
        Object comp = component;
        while (comp instanceof Component.Wrapper) {
            comp = ((Component.Wrapper) comp).getComposition();
        }
        return (JComponent) comp;
    }

    @Nullable
    public static <T extends Component> T getComponent(Component.Container comp, String id) {
        final String[] elements = ValuePathHelper.parse(id);
        if (elements.length == 1) {
            final Component component = comp.getOwnComponent(id);

            if (component == null) {
                Component c = getComponentByIteration(comp, id);
                if (c != null)
                    return (T) c;
            } else {
                return (T) component;
            }
        } else {
            Component component = comp.getOwnComponent(elements[0]);
            if (component == null) {
                Component c = getComponentByIteration(comp, id);
                if (c != null)
                    return (T) c;
            } else {
                String[] subpath = (String[]) ArrayUtils.subarray(elements, 1, elements.length);
                if (component instanceof Component.Container) {
                    return ((Component.Container) component).<T>getComponent(ValuePathHelper.format(subpath));
                }
            }
        }
        return null;
    }

    @Nullable
    private static <T extends Component> T getComponentByIteration(Component.Container container, String id) {
        for (Component component : container.getOwnComponents()) {
            if (id.equals(component.getId()))
                return (T) component;
            else {
                if (component instanceof Component.Container) {
                    return (T) getComponentByIteration((Component.Container) component, id);
                }
            }
        }
        return null;
    }

    public static int convertMessageType(IFrame.MessageType messageType) {
        switch (messageType) {
            case CONFIRMATION:
            case CONFIRMATION_HTML:
                return JOptionPane.QUESTION_MESSAGE;
            case WARNING:
            case WARNING_HTML:
                return JOptionPane.WARNING_MESSAGE;
            default:
                return JOptionPane.INFORMATION_MESSAGE;
        }
    }

    public static int convertNotificationType(IFrame.NotificationType type) {
        switch (type) {
            case WARNING:
            case WARNING_HTML:
                return JOptionPane.WARNING_MESSAGE;
            case ERROR:
            case ERROR_HTML:
                return JOptionPane.ERROR_MESSAGE;
            case HUMANIZED:
            case HUMANIZED_HTML:
                return JOptionPane.INFORMATION_MESSAGE;
            case TRAY:
            case TRAY_HTML:
                return JOptionPane.WARNING_MESSAGE;
            default:
                return JOptionPane.PLAIN_MESSAGE;
        }
    }

    public static void adjustSize(JButton button) {
        button.setPreferredSize(new Dimension(0, BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
    }

    public static void adjustSize(JComboBox comboBox) {
        comboBox.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
    }

    public static void adjustDateFieldSize(JPanel dateFieldComposition) {
        dateFieldComposition.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
    }

    /**
     * Convert {@link KeyCombination} to {@link KeyStroke}.
     *
     * @param combination Key combination to convert
     * @return KeyStroke
     */
    public static KeyStroke convertKeyCombination(KeyCombination combination) {
        KeyCombination.Modifier[] modifiers = combination.getModifiers();
        int modifiersMask = 0;
        if (modifiers != null && modifiers.length > 0) {
            for (KeyCombination.Modifier modifier : modifiers) {
                modifiersMask = modifiersMask | convertModifier(modifier);
            }
        }
        return KeyStroke.getKeyStroke(combination.getKey().getVirtualKey(), modifiersMask, false);
    }

    /**
     * Convert {@link ShortcutAction.Modifier} to {@link InputEvent} modifier constraint.
     *
     * @param modifier modifier to convert
     * @return {@link InputEvent} modifier constraint
     */
    public static int convertModifier(KeyCombination.Modifier modifier) {
        switch (modifier) {
            case CTRL:
                return InputEvent.CTRL_DOWN_MASK;
            case ALT:
                return InputEvent.ALT_DOWN_MASK;
            case SHIFT:
                return InputEvent.SHIFT_DOWN_MASK;
            default:
                throw new IllegalArgumentException("Modifier " + modifier.name() + " not recognized");
        }
    }

    /**
     * Make JTable handle TAB key as all other components - move focus to next/previous components.
     * <p>Default Swing behaviour for table is to move focus to next/previous cell inside the table.</p>
     * @param table table instance
     */
    public static void correctTableFocusTraversal(JTable table) {
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Collections.singleton(AWTKeyStroke.getAWTKeyStroke("TAB")));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Collections.singleton(AWTKeyStroke.getAWTKeyStroke("shift TAB")));
    }

    /**
     * Add shortcut action to any JComponent.
     *
     * @param name name of action that used as action key in {@link InputMap} and {@link ActionMap}.
     * @param component
     * @param key
     * @param action
     */
    public static void addShortcutAction(String name, JComponent component, KeyStroke key, Action action) {
        ActionMap actionMap = component.getActionMap();
        InputMap inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(key, name);
        actionMap.put(name, action);
    }

    public static void decorateMissingValue(JComponent jComponent, boolean missingValueState) {
        jComponent.setBackground(missingValueState ? requiredBgColor : defaultBgColor);
    }

    /**
     * Returns {@link TopLevelFrame} of container.
     *
     * @param container
     * @return {@link TopLevelFrame} of container
     */
    public static TopLevelFrame getTopLevelFrame(Container container) {
        Container prevContainer;
        Container parent = container;
        do {
            prevContainer = parent;
            if (parent instanceof DetachedFrame) {
                parent = ((DetachedFrame) parent).getParentContainer();
            } if (parent instanceof JPopupMenu) {
                parent = ((JPopupMenu) parent).getInvoker().getParent();
            } else {
                parent = parent.getParent();
            }
        } while (parent != null);


        return (TopLevelFrame) prevContainer;
    }

    public static TopLevelFrame getTopLevelFrame(java.awt.Component component) {
        Container prevContainer;
        Container parent = component instanceof Container ? (Container) component : component.getParent();
        do {
            prevContainer = parent;
            if (parent instanceof DetachedFrame) {
                parent = ((DetachedFrame) parent).getParentContainer();
            } if (parent instanceof JPopupMenu) {
                parent = ((JPopupMenu) parent).getInvoker().getParent();
            } else {
                parent = parent.getParent();
            }
        } while (parent != null);

        return (TopLevelFrame) prevContainer;
    }

    /**
     * Returns {@link TopLevelFrame} of component.
     *
     * @param component
     * @return {@link TopLevelFrame} of component
     */
    public static TopLevelFrame getTopLevelFrame(DesktopAbstractComponent component) {
        return getTopLevelFrame(component.getComposition());
    }

    /**
     * Returns {@link TopLevelFrame} of frame.
     *
     * @param frame
     * @return {@link TopLevelFrame} of component
     */
    public static TopLevelFrame getTopLevelFrame(IFrame frame) {
        if (frame instanceof DesktopWindow) {
            return ((DesktopWindow) frame).getWindowManager().getFrame();
        } else if (frame instanceof DesktopFrame) {
            return getTopLevelFrame((frame).getFrame());
        } else if (frame instanceof AbstractFrame) {
            Component.Wrapper wrapper = (Component.Wrapper) ((AbstractFrame) frame).getComposition();
            if (wrapper instanceof DesktopWindow) {
                return ((DesktopWindow) wrapper).getWindowManager().getFrame();
            } else if (wrapper instanceof DesktopFrame) {
                return getTopLevelFrame(((DesktopFrame) wrapper).getFrame());
            } else {
                return getTopLevelFrame((Container) wrapper.getComposition());
            }
        } else {
            throw new IllegalArgumentException("Can not get top level frame for " + frame);
        }
    }

    /**
     * Determines whether component will be displayed on the screen.
     *
     * @param component
     * @return true if the component and all of its ancestors are visible
     */
    public static boolean isRecursivelyVisible(java.awt.Component component) {
        return component.isVisible() && (component.getParent() == null || isRecursivelyVisible(component.getParent()));
    }
}