/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.cuba.desktop.gui.data.DesktopContainerHelper;
import com.haulmont.cuba.desktop.sys.layout.BoxLayoutAdapter;
import com.haulmont.cuba.desktop.sys.layout.GridLayoutAdapter;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GridLayout;
import net.miginfocom.layout.CC;

import javax.swing.*;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class DesktopGridLayout
        extends DesktopAbstractComponent<JPanel>
        implements GridLayout, DesktopContainer
{
    protected GridLayoutAdapter layoutAdapter;

    protected Collection<Component> ownComponents = new HashSet<Component>();
    protected Map<String, Component> componentByIds = new HashMap<String, Component>();
    protected Map<Component, ComponentCaption> captions = new HashMap<Component, ComponentCaption>();
    protected Map<Component, JPanel> wrappers = new HashMap<Component, JPanel>();

    public DesktopGridLayout() {
        impl = new JPanel();
        layoutAdapter = GridLayoutAdapter.create(impl);
    }

    public float getColumnExpandRatio(int col) {
        return layoutAdapter.getColumnExpandRatio(col);
    }

    public void setColumnExpandRatio(int col, float ratio) {
        layoutAdapter.setColumnExpandRatio(col, ratio);
    }

    public float getRowExpandRatio(int col) {
        return layoutAdapter.getRowExpandRatio(col);
    }

    public void setRowExpandRatio(int col, float ratio) {
        layoutAdapter.setRowExpandRatio(col, ratio);
    }

    public void add(Component component, int col, int row) {
        add(component, col, row, col, row);
    }

    public void add(Component component, int col, int row, int col2, int row2) {
        final JComponent composition = DesktopComponentsHelper.getComposition(component);

        // add caption first
        ComponentCaption caption = null;
        boolean haveDescription = false;
        if (DesktopContainerHelper.hasExternalCaption(component)) {
            caption = new ComponentCaption(component);
            captions.put(component, caption);
            impl.add(caption, layoutAdapter.getCaptionConstraints(col, row, col2, row2));
        } else if (DesktopContainerHelper.hasExternalDescription(component)) {
            caption = new ComponentCaption(component);
            captions.put(component, caption);
            haveDescription = true;
        }
         //if component have description without caption, we need to wrap
        // component to view Description button horizontally after component
        if (haveDescription) {
            JPanel wrapper = new JPanel();
            BoxLayoutAdapter adapter = BoxLayoutAdapter.create(wrapper);
            adapter.setExpandLayout(true);
            adapter.setSpacing(false);
            adapter.setMargin(false);
            wrapper.add(composition);
            wrapper.add(caption, new CC().alignY("top"));
            impl.add(wrapper, layoutAdapter.getConstraints(component, col, row, col2, row2));
            wrappers.put(component, wrapper);
        } else {
            impl.add(composition, layoutAdapter.getConstraints(component, col, row, col2, row2));
        }

        if (component.getId() != null) {
            componentByIds.put(component.getId(), component);
            if (frame != null) {
                frame.registerComponent(component);
            }
        }
        ownComponents.add(component);

        DesktopContainerHelper.assignContainer(component, this);
    }

    public int getRows() {
        return layoutAdapter.getRows();
    }

    public void setRows(int rows) {
        layoutAdapter.setRows(rows);
    }

    public int getColumns() {
        return layoutAdapter.getColumns();
    }

    public void setColumns(int columns) {
        layoutAdapter.setColumns(columns);
    }

    public void add(Component component) {
        // captions not added here
        final JComponent composition = DesktopComponentsHelper.getComposition(component);
        impl.add(composition, layoutAdapter.getConstraints(component));
        //setComponentAlignment(itmillComponent, WebComponentsHelper.convertAlignment(component.getAlignment()));

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
        if (wrappers.containsKey(component)) {
            impl.remove(wrappers.get(component));
            wrappers.remove(component);
        } else {
            impl.remove(DesktopComponentsHelper.getComposition(component));
        }
        if (captions.containsKey(component)) {
            impl.remove(captions.get(component));
            captions.remove(component);

        }
        if (component.getId() != null) {
            componentByIds.remove(component.getId());
        }
        ownComponents.remove(component);

        DesktopContainerHelper.assignContainer(component, null);
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
    public void updateComponent(Component child) {
        JComponent composition = DesktopComponentsHelper.getComposition(child);
        layoutAdapter.updateConstraints(composition, layoutAdapter.getConstraints(child));
        if (captions.containsKey(child)) {
            captions.get(child).update();
        }
    }
}
