/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ValuePathHelper;

import javax.swing.*;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopComponentsHelper {

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

    public static <T extends Component> T getComponent(Component.Container comp, String id) {
        final JComponent container = unwrap(comp);

        final String[] elements = ValuePathHelper.parse(id);
        if (elements.length == 1) {
            final Component component = comp.getOwnComponent(id);

            if (component == null) {
                return (T) getComponentByIteration(container, id);
            } else {
                return (T) component;
            }
        } else {
            Component component = comp.getOwnComponent(elements[0]);
            if (component == null) {
                return (T) getComponentByIteration(container, id);
            } else {
                final List<String> subpath = Arrays.asList(elements).subList(1, elements.length);
                if (component instanceof Component.Container) {
                    return ((Component.Container) component).<T>getComponent(
                            ValuePathHelper.format(subpath.toArray(new String[subpath.size()])));
                } else {
                    return null;
                }
            }
        }
    }

    private static <T extends Component> T getComponentByIteration(JComponent container, String id) {
        throw new UnsupportedOperationException();
    }

}
