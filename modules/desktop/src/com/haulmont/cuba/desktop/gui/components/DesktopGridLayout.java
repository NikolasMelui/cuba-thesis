/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.desktop.gui.components;

import com.haulmont.bali.datastruct.Pair;
import com.haulmont.cuba.desktop.gui.data.DesktopContainerHelper;
import com.haulmont.cuba.desktop.sys.layout.BoxLayoutAdapter;
import com.haulmont.cuba.desktop.sys.layout.GridLayoutAdapter;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GridLayout;
import com.haulmont.cuba.gui.components.IFrame;
import net.miginfocom.layout.CC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DesktopGridLayout extends DesktopAbstractComponent<JPanel> implements GridLayout, DesktopContainer {

    protected GridLayoutAdapter layoutAdapter;

    protected Collection<Component> ownComponents = new LinkedHashSet<>();
    protected Map<String, Component> componentByIds = new HashMap<>();
    protected Map<Component, ComponentCaption> captions = new HashMap<>();
    protected Map<Component, Pair<JPanel, BoxLayoutAdapter>> wrappers = new HashMap<>();

    protected boolean scheduledRepaint = false;

    public DesktopGridLayout() {
        impl = new JPanel();
        assignClassDebugProperty(impl);
        layoutAdapter = GridLayoutAdapter.create(impl);
    }

    @Override
    public float getColumnExpandRatio(int col) {
        return layoutAdapter.getColumnExpandRatio(col);
    }

    @Override
    public void setColumnExpandRatio(int col, float ratio) {
        layoutAdapter.setColumnExpandRatio(col, ratio);
    }

    @Override
    public float getRowExpandRatio(int row) {
        return layoutAdapter.getRowExpandRatio(row);
    }

    @Override
    public void setRowExpandRatio(int row, float ratio) {
        layoutAdapter.setRowExpandRatio(row, ratio);
    }

    @Override
    public void add(Component component, int col, int row) {
        add(component, col, row, col, row);
    }

    @Override
    public void add(Component component, int col, int row, int col2, int row2) {
        final JComponent composition = DesktopComponentsHelper.getComposition(component);

        // add caption first
        ComponentCaption caption = null;
        boolean haveDescription = false;
        if (DesktopContainerHelper.hasExternalCaption(component)) {
            caption = new ComponentCaption(component);
            captions.put(component, caption);
            impl.add(caption, layoutAdapter.getCaptionConstraints(component, col, row, col2, row2));
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
            wrappers.put(component, new Pair<>(wrapper, adapter));
        } else {
            impl.add(composition, layoutAdapter.getConstraints(component, col, row, col2, row2));
        }

        if (component.getId() != null) {
            componentByIds.put(component.getId(), component);
        }

        if (frame != null) {
            if (component instanceof BelongToFrame
                    && ((BelongToFrame) component).getFrame() == null) {
                ((BelongToFrame) component).setFrame(frame);
            } else {
                frame.registerComponent(component);
            }
        }

        ownComponents.add(component);

        DesktopContainerHelper.assignContainer(component, this);

        requestRepaint();
    }

    @Override
    public void setFrame(IFrame frame) {
        super.setFrame(frame);

        if (frame != null) {
            for (Component childComponent : ownComponents) {
                if (childComponent instanceof BelongToFrame
                        && ((BelongToFrame) childComponent).getFrame() == null) {
                    ((BelongToFrame) childComponent).setFrame(frame);
                }
            }
        }
    }

    @Override
    public int getRows() {
        return layoutAdapter.getRows();
    }

    @Override
    public void setRows(int rows) {
        layoutAdapter.setRows(rows);
    }

    @Override
    public int getColumns() {
        return layoutAdapter.getColumns();
    }

    @Override
    public void setColumns(int columns) {
        layoutAdapter.setColumns(columns);
    }

    @Override
    public void add(Component component) {
        // captions not added here
        final JComponent composition = DesktopComponentsHelper.getComposition(component);
        impl.add(composition, layoutAdapter.getConstraints(component));
        //setComponentAlignment(itmillComponent, WebComponentsHelper.convertAlignment(component.getAlignment()));

        if (component.getId() != null) {
            componentByIds.put(component.getId(), component);
        }

        if (frame != null) {
            if (component instanceof BelongToFrame
                    && ((BelongToFrame) component).getFrame() == null) {
                ((BelongToFrame) component).setFrame(frame);
            } else {
                frame.registerComponent(component);
            }
        }

        ownComponents.add(component);

        DesktopContainerHelper.assignContainer(component, this);

        requestRepaint();
    }

    protected void requestRepaint() {
        if (!scheduledRepaint) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    impl.revalidate();
                    impl.repaint();

                    scheduledRepaint = false;
                }
            });

            scheduledRepaint = true;
        }
    }

    @Override
    public void remove(Component component) {
        if (wrappers.containsKey(component)) {
            impl.remove(wrappers.get(component).getFirst());
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

        requestRepaint();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Component> T getOwnComponent(String id) {
        return (T) componentByIds.get(id);
    }

    @Nullable
    @Override
    public <T extends Component> T getComponent(String id) {
        return ComponentsHelper.getComponent(this, id);
    }

    @Nonnull
    @Override
    public <T extends Component> T getComponentNN(String id) {
        T component = getComponent(id);
        if (component == null) {
            throw new IllegalArgumentException(String.format("Not found component with id '%s'", id));
        }
        return component;
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return Collections.unmodifiableCollection(ownComponents);
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    @Override
    public void setMargin(boolean enable) {
        layoutAdapter.setMargin(enable);
    }

    @Override
    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        layoutAdapter.setMargin(topEnable, rightEnable, bottomEnable, leftEnable);
    }

    @Override
    public void setSpacing(boolean enabled) {
        layoutAdapter.setSpacing(enabled);
    }

    @Override
    public void updateComponent(Component child) {
        JComponent composition;
        if (wrappers.containsKey(child)) {
            composition = wrappers.get(child).getFirst();
        } else {
            composition = DesktopComponentsHelper.getComposition(child);
        }
        layoutAdapter.updateConstraints(composition, layoutAdapter.getConstraints(child));
        if (captions.containsKey(child)) {
            ComponentCaption caption = captions.get(child);
            caption.update();
            if (!wrappers.containsKey(child)) {
                CC c = (CC) layoutAdapter.getConstraints(child);
                layoutAdapter.updateConstraints(caption, layoutAdapter.getCaptionConstraints(child,
                        c.getCellX(), c.getCellY(), c.getCellX(), c.getCellY()));
            }
        }

        requestRepaint();

        requestContainerUpdate();
    }
}