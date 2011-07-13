/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.gui.data.DesktopContainerHelper;
import com.haulmont.cuba.desktop.sys.layout.BoxLayoutAdapter;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;

import javax.swing.*;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public abstract class DesktopAbstractBox
        extends DesktopAbstractComponent<JPanel>
        implements com.haulmont.cuba.gui.components.BoxLayout, DesktopContainer
{
    protected BoxLayoutAdapter layoutAdapter;

    protected Collection<Component> ownComponents = new HashSet<Component>();
    protected Map<String, Component> componentByIds = new HashMap<String, Component>();

    protected Component expandedComponent;

    public DesktopAbstractBox() {
        impl = new JPanel();
        layoutAdapter = BoxLayoutAdapter.create(impl);
    }

    public void add(Component component) {
        // add caption first
        if (DesktopContainerHelper.hasExternalCaption(component)) {
            String caption = ((HasCaption) component).getCaption();
            impl.add(new JLabel(caption), layoutAdapter.getCaptionConstraints());
        }

        JComponent composition = DesktopComponentsHelper.getComposition(component);
        impl.add(composition, layoutAdapter.getConstraints(component));

        if (component.getId() != null) {
            componentByIds.put(component.getId(), component);
            if (frame != null) {
                frame.registerComponent(component);
            }
        }
        ownComponents.add(component);

        DesktopContainerHelper.assignContainer(component, this);
    }

    public void remove(Component component) {
        JComponent composition = DesktopComponentsHelper.getComposition(component);
        impl.remove(composition);
        impl.revalidate();
        impl.repaint();

        if (component.getId() != null) {
            componentByIds.remove(component.getId());
        }
        ownComponents.remove(component);

        DesktopContainerHelper.assignContainer(component, null);
    }

    @Override
    public void updateComponent(Component child) {
        JComponent composition = DesktopComponentsHelper.getComposition(child);
        layoutAdapter.updateConstraints(composition, layoutAdapter.getConstraints(child));
    }

    public <T extends Component> T getOwnComponent(String id) {
        return (T) componentByIds.get(id);
    }

    public <T extends Component> T getComponent(String id) {
        return DesktopComponentsHelper.<T>getComponent(this, id);
    }

    public Collection<Component> getOwnComponents() {
        return Collections.unmodifiableCollection(ownComponents);
    }

    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    public void expand(Component component, String height, String width) {
        if (expandedComponent != null
                && expandedComponent instanceof DesktopComponent) {
            ((DesktopComponent) expandedComponent).setExpanded(false);
        }

        JComponent composition = DesktopComponentsHelper.getComposition(component);
        layoutAdapter.expand(composition, height, width);

        if (component instanceof DesktopComponent) {
            ((DesktopComponent) component).setExpanded(true);
        }

        expandedComponent = component;
    }

    public void expand(Component component) {
        expand(component, "", "");
    }

    public void setMargin(boolean enable) {
        layoutAdapter.setMargin(enable);
    }

    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        layoutAdapter.setMargin(topEnable, rightEnable, bottomEnable, leftEnable);
    }

    public void setSpacing(boolean enabled) {
        layoutAdapter.setSpacing(enabled);
    }

    @Override
    public void setExpanded(boolean expanded) {
        layoutAdapter.setExpandLayout(expanded);
    }
}
