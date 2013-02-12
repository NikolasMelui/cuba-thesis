/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.ui.FieldGroupLayout;
import com.haulmont.cuba.web.toolkit.ui.HorizontalActionsLayout;
import com.haulmont.cuba.web.toolkit.ui.VerticalActionsLayout;
import com.vaadin.event.Action;
import com.vaadin.server.ClassResource;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.*;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class WebComponentsHelper {

    public static Resource getResource(String resURL) {
        if (StringUtils.isEmpty(resURL)) return null;

        if (resURL.startsWith("file:")) {
            return new FileResource(new File(resURL.substring("file:".length())));
        } else if (resURL.startsWith("jar:")) {
            return new ClassResource(resURL.substring("jar:".length()));
        } else if (resURL.startsWith("theme:")) {
            return new ThemeResource(resURL.substring("theme:".length()));
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /*
    public static class ComponentPath {
        String[] elements;
        com.haulmont.cuba.gui.components.Component[] components;

        public ComponentPath(String[] elements, com.haulmont.cuba.gui.components.Component[] components) {
            this.elements = elements;
            this.components = components;
        }

        public String[] getElements() {
            return elements;
        }

        public com.haulmont.cuba.gui.components.Component[] getComponents() {
            return components;
        }
    }*/

    public static <T extends Component> Collection<T> getComponents(ComponentContainer container, Class<T> aClass) {
        List<T> res = new ArrayList<>();
        for (Object aContainer : container) {
            Component component = (Component) aContainer;
            if (aClass.isAssignableFrom(component.getClass())) {
                res.add((T) component);
            } else if (ComponentContainer.class.isAssignableFrom(component.getClass())) {
                res.addAll(getComponents((ComponentContainer) component, aClass));
            }

        }

        return res;
    }

    /**
     * Returns underlying Vaadin component implementation.
     *
     * @param component GUI component
     * @return          Vaadin component
     * @see #getComposition(com.haulmont.cuba.gui.components.Component)
     */
    public static Component unwrap(com.haulmont.cuba.gui.components.Component component) {
        Object comp = component;
        while (comp instanceof com.haulmont.cuba.gui.components.Component.Wrapper) {
            comp = ((com.haulmont.cuba.gui.components.Component.Wrapper) comp).getComponent();
        }

        return (com.vaadin.ui.Component) comp;
    }
          /*
    public static com.haulmont.cuba.web.toolkit.Timer unwrap(com.haulmont.cuba.gui.components.Timer timer) {
        return (com.haulmont.cuba.web.toolkit.Timer) timer;
    }   */

    /**
     * Returns underlying Vaadin component, which serves as the outermost container for the supplied GUI component.
     * For simple components like {@link com.haulmont.cuba.gui.components.Button} this method returns the same
     * result as {@link #unwrap(com.haulmont.cuba.gui.components.Component)}.
     *
     * @param component GUI component
     * @return          Vaadin component
     */
    public static Component getComposition(com.haulmont.cuba.gui.components.Component component) {
        Object comp = component;
        while (comp instanceof com.haulmont.cuba.gui.components.Component.Wrapper) {
            comp = ((com.haulmont.cuba.gui.components.Component.Wrapper) comp).getComposition();
        }

        return (com.vaadin.ui.Component) comp;
    }
    /**
     * @deprecated Use ComponentsHelper.getComponents() instead
     */
    @Deprecated
    public static Collection<com.haulmont.cuba.gui.components.Component> getComponents(
            com.haulmont.cuba.gui.components.Component.Container container) {
        final Collection<com.haulmont.cuba.gui.components.Component> ownComponents = container.getOwnComponents();
        Set<com.haulmont.cuba.gui.components.Component> res = new HashSet<>(ownComponents);

        for (com.haulmont.cuba.gui.components.Component component : ownComponents) {
            if (component instanceof com.haulmont.cuba.gui.components.Component.Container) {
                res.addAll(getComponents((com.haulmont.cuba.gui.components.Component.Container) component));
            }
        }

        return res;
    }

    public static <T extends com.haulmont.cuba.gui.components.Component> T getComponent(
            com.haulmont.cuba.gui.components.Component.Container comp, String id) {
        final Component unwrapedComponent = unwrap(comp);
        final ComponentContainer container =
                unwrapedComponent instanceof Form ?
                        ((Form) unwrapedComponent).getLayout() :
                        (ComponentContainer) unwrapedComponent;

        final String[] elements = ValuePathHelper.parse(id);
        if (elements.length == 1) {
            final com.haulmont.cuba.gui.components.Component component = comp.getOwnComponent(id);

            if (component == null) {
                return (T) getComponentByIterate(container, id);
            } else {
                return (T) component;
            }
        } else {
            com.haulmont.cuba.gui.components.Component component = comp.getOwnComponent(elements[0]);
            if (component == null) {
                return (T) getComponentByIterate(container, id);
            } else {
                final List<String> subpath = Arrays.asList(elements).subList(1, elements.length);
                if (component instanceof com.haulmont.cuba.gui.components.Component.Container) {
                    return ((com.haulmont.cuba.gui.components.Component.Container) component).<T>getComponent(
                            ValuePathHelper.format(subpath.toArray(new String[subpath.size()])));
                } else {
                    return null;
                }
            }
        }
    }

    protected static <T extends com.haulmont.cuba.gui.components.Component> T getComponentByIterate(ComponentContainer container, String id) {
        com.haulmont.cuba.gui.components.Component component;
        final Iterator iterator = container.getComponentIterator();
        while (iterator.hasNext()) {
            Component c = (Component) iterator.next();

            if (c instanceof com.haulmont.cuba.gui.components.Component.Container) {
                component = ((com.haulmont.cuba.gui.components.Component.Container) c).getComponent(id);
                if (component != null) return (T) component;
            } else if (c instanceof WebComponentEx) {
                component = ((WebComponentEx) c).asComponent();
                if (component instanceof com.haulmont.cuba.gui.components.Component.Container) {
                    component = ((com.haulmont.cuba.gui.components.Component.Container) component).getComponent(id);
                    if (component != null) return (T) component;
                }
            } else if (c instanceof ComponentContainer) {
                component = getComponentByIterate(((ComponentContainer) c), id);
                if (component != null) return (T) component;
            } else if (c instanceof Form) {
                component = getComponentByIterate(((Form) c).getLayout(), id);
                if (component != null) return (T) component;
            }
        }

        return null;
    }

    public static void expand(AbstractOrderedLayout layout, Component component, String height, String width) {
        if (!isHorizontalLayout(layout)
                && (StringUtils.isEmpty(height) || "-1px".equals(height) || height.endsWith("%"))) {
            component.setHeight("100%");
        }

        if (!isVerticalLayout(layout)
                && (StringUtils.isEmpty(width) || "-1px".equals(width) || width.endsWith("%"))) {
            component.setWidth("100%");
        }

        layout.setExpandRatio(component, 1);
    }

    public static boolean isVerticalLayout(AbstractOrderedLayout layout) {
        return (layout instanceof VerticalLayout)
                || (layout instanceof VerticalActionsLayout);
    }

    public static boolean isHorizontalLayout(AbstractOrderedLayout layout) {
        return (layout instanceof HorizontalLayout)
                || (layout instanceof HorizontalActionsLayout);
    }

    public static Alignment convertAlignment(com.haulmont.cuba.gui.components.Component.Alignment alignment) {
        if (alignment == null) return null;

        switch (alignment) {
            case TOP_LEFT: {return Alignment.TOP_LEFT;}
            case TOP_CENTER: {return Alignment.TOP_CENTER;}
            case TOP_RIGHT: {return Alignment.TOP_RIGHT;}
            case MIDDLE_LEFT: {return Alignment.MIDDLE_LEFT;}
            case MIDDLE_CENTER: {return Alignment.MIDDLE_CENTER;}
            case MIDDLE_RIGHT: {return Alignment.MIDDLE_RIGHT;}
            case BOTTOM_LEFT: {return Alignment.BOTTOM_LEFT;}
            case BOTTOM_CENTER: {return Alignment.BOTTOM_CENTER;}
            case BOTTOM_RIGHT: {return Alignment.BOTTOM_RIGHT;}
            default: {throw new UnsupportedOperationException();}
        }
    }

    public static Notification.Type convertNotificationType(IFrame.NotificationType type) {
        switch (type) {
            case TRAY:
                return Notification.Type.TRAY_NOTIFICATION;
            case HUMANIZED:
                return Notification.Type.HUMANIZED_MESSAGE;
            case WARNING:
                return Notification.Type.WARNING_MESSAGE;
            case ERROR:
                return Notification.Type.ERROR_MESSAGE;
            default:
                return Notification.Type.WARNING_MESSAGE;
        }
    }

    public static FilteringMode convertFilterMode(com.haulmont.cuba.gui.components.LookupField.FilterMode filterMode) {
        switch (filterMode) {
            case NO:
                return FilteringMode.OFF;
            case STARTS_WITH:
                return FilteringMode.STARTSWITH;
            case CONTAINS:
                return FilteringMode.CONTAINS;
            default:
                return FilteringMode.OFF;
        }
    }

    public static AggregationContainer.Type convertAggregationType(AggregationInfo.Type function) {
        switch (function) {
            case COUNT:
                return AggregationContainer.Type.COUNT;
            case AVG:
                return AggregationContainer.Type.AVG;
            case MAX:
                return AggregationContainer.Type.MAX;
            case MIN:
                return AggregationContainer.Type.MIN;
            case SUM:
                return AggregationContainer.Type.SUM;
            default:
                throw new IllegalArgumentException("Unknown function: " + function);
        }
    }

    public static Button createButton() {
        return createButton(null);
    }

    public static Button createButton(String icon) {
        WebButton webButton = new WebButton();
        webButton.setIcon(icon);
        return (Button) unwrap(webButton);
    }

    public static IFrame getControllerFrame(IFrame frame) {
        if (frame instanceof AbstractFrame) {
            return frame;
        } else if (frame instanceof WrappedWindow) {
            IFrame wrapper = ((WrappedWindow) frame).getWrapper();
            if (wrapper != null) {
                return wrapper;
            }
        } else if (frame instanceof WrappedFrame) {
            IFrame wrapper = ((WrappedFrame) frame).getWrapper();
            if (wrapper != null) {
                return wrapper;
            }
        }
        return getControllerFrame(frame.getFrame());
    }

    public static void setLabelText(com.vaadin.ui.Label label, Object value, Formatter formatter) {
        label.setValue(value == null
                ? "" : String.class.isInstance(value)
                        ? (String) value : formatter != null
                                ? formatter.format(value) : value.toString()
        );
    }

    public static com.vaadin.event.ShortcutAction createShortcutAction(ShortcutAction action) {
        ShortcutAction.KeyCombination keyCombination = action.getKeyCombination();
        return new com.vaadin.event.ShortcutAction(
                action.getCaption(),
                keyCombination.getKey().getCode(),
                ShortcutAction.Modifier.codes(keyCombination.getModifiers())
        );
    }

    /**
     * Add actions to vaadin action container.
     *
     * @param container any {@link Action.Container}
     * @param actions map of actions
     */
    public static void setActions(final Action.Container container,
                                  final Map<Action, Runnable> actions) {
        container.addActionHandler(new Action.Handler() {
            @Override
            public Action[] getActions(Object target, Object sender) {
                Set<Action> shortcuts = actions.keySet();
                return shortcuts.toArray(new Action[shortcuts.size()]);
            }

            @Override
            public void handleAction(Action action, Object sender, Object target) {
                Runnable runnable = actions.get(action);
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    public static int convertFieldGroupCaptionAlignment(FieldGroup.FieldCaptionAlignment captionAlignment) {
        switch (captionAlignment) {
            case TOP:
                return FieldGroupLayout.CAPTION_ALIGN_TOP;
            case LEFT:
            default:
                return FieldGroupLayout.CAPTION_ALIGN_LEFT;
        }
    }

    public static Resolution convertDateFieldResolution(com.haulmont.cuba.gui.components.DateField.Resolution resolution) {
        switch (resolution) {
            case SEC:
                return Resolution.SECOND;

            case HOUR:
                return Resolution.HOUR;

            case DAY:
                return Resolution.DAY;

            case MONTH:
                return Resolution.MONTH;

            case YEAR:
                return Resolution.YEAR;

            case MIN:
            default:
                return Resolution.MINUTE;
        }
    }
}