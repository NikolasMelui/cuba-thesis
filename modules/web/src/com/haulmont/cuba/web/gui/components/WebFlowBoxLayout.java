/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FlowBoxLayout;
import com.haulmont.cuba.web.toolkit.ui.CubaFlowLayout;
import com.vaadin.shared.ui.MarginInfo;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author gorodnov
 * @version $Id$
 */
public class WebFlowBoxLayout extends WebAbstractComponent<CubaFlowLayout> implements FlowBoxLayout {

    protected Collection<Component> ownComponents = new HashSet<>();
    protected Map<String, Component> componentByIds = new HashMap<>();

    public WebFlowBoxLayout() {
        component = new CubaFlowLayout();
    }

    @Override
    public void add(Component childComponent) {
        final com.vaadin.ui.Component vComponent = WebComponentsHelper.getComposition(childComponent);

        if (childComponent.getId() != null) {
            component.addComponent(vComponent);
            componentByIds.put(childComponent.getId(), childComponent);
            if (frame != null) {
                frame.registerComponent(childComponent);
            }
        } else {
            component.addComponent(vComponent);
        }

        ownComponents.add(childComponent);
    }

    @Override
    public void remove(Component childComponent) {
        com.vaadin.ui.Component childComposition = WebComponentsHelper.getComposition(childComponent);

        if (childComponent.getId() != null) {
            component.removeComponent(childComposition);
            componentByIds.remove(childComponent.getId());
        } else {
            component.removeComponent(childComposition);
        }
        ownComponents.remove(childComponent);
    }

    @Nullable
    @Override
    public <T extends Component> T getOwnComponent(String id) {
        return (T) componentByIds.get(id);
    }

    @Nullable
    @Override
    public <T extends Component> T getComponent(String id) {
        return WebComponentsHelper.getComponent(this, id);
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
        component.setMargin(enable);
    }

    @Override
    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        component.setMargin(new MarginInfo(topEnable, rightEnable, bottomEnable, leftEnable));
    }

    @Override
    public void setSpacing(boolean enabled) {
        component.setSpacing(enabled);
    }
}