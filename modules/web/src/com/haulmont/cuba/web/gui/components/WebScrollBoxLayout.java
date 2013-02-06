/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import org.apache.commons.lang.ObjectUtils;

import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class WebScrollBoxLayout extends WebAbstractComponent<Panel> implements ScrollBoxLayout, Component.Wrapper {

    private String id;
    protected List<Component> components = new ArrayList<>();
    private Alignment alignment = Alignment.TOP_LEFT;
    private Orientation orientation = Orientation.VERTICAL;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.VERTICAL;

    private IFrame frame;

    public WebScrollBoxLayout() {
        component = new Panel();
        component.setContent(new VerticalLayout());

        getContent().setMargin(false);
//        component.setScrollable(true);
    }

    private AbstractOrderedLayout getContent() {
        return (AbstractOrderedLayout) component.getContent();
    }

    @Override
    public void add(Component childComponent) {
        AbstractOrderedLayout newContent = null;
        if (orientation == Orientation.VERTICAL && !(getContent() instanceof VerticalLayout))
            newContent = new VerticalLayout();
        else if (orientation == Orientation.HORIZONTAL && !(getContent() instanceof HorizontalLayout))
            newContent = new HorizontalLayout();

        if (newContent != null) {
            newContent.setMargin((getContent()).getMargin());
            newContent.setSpacing((getContent()).isSpacing());
            component.setContent(newContent);

            applyScrollBarsPolicy(scrollBarPolicy);
        }

        getContent().addComponent(WebComponentsHelper.getComposition(childComponent));
        components.add(childComponent);
    }

    @Override
    public void remove(Component component) {
        getContent().removeComponent(WebComponentsHelper.getComposition(component));
        components.remove(component);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void requestFocus() {
        Iterator<com.vaadin.ui.Component> componentIterator = getContent().iterator();
        if (componentIterator.hasNext()) {
            com.vaadin.ui.Component childComponent = componentIterator.next();
            if (childComponent instanceof com.vaadin.ui.Component.Focusable) {
                ((com.vaadin.ui.Component.Focusable) childComponent).focus();
            }
        }
    }

    @Override
    public <T extends Component> T getOwnComponent(String id) {
        for (Component component : components) {
            if (ObjectUtils.equals(component.getId(), id))
                return (T) component;
        }
        return null;
    }

    @Override
    public <T extends Component> T getComponent(String id) {
        for (Component component : getComponents()) {
            if (ObjectUtils.equals(component.getId(), id))
                return (T) component;
        }
        return null;
    }

    @Override
    public Collection<Component> getOwnComponents() {
        return Collections.unmodifiableCollection(components);
    }

    @Override
    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    @Override
    public Alignment getAlignment() {
        return alignment;
    }

    @Override
    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        final com.vaadin.ui.Component parentComponent = component.getParent();
        if (parentComponent instanceof Layout.AlignmentHandler) {
            ((Layout.AlignmentHandler) parentComponent).setComponentAlignment(component, WebComponentsHelper.convertAlignment(alignment));
        }
    }

    @Override
    public <A extends IFrame> A getFrame() {
        return (A) frame;
    }

    @Override
    public void setFrame(IFrame frame) {
        this.frame = frame;
        frame.registerComponent(this);
    }

    @Override
    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(Orientation orientation) {
        if (!ObjectUtils.equals(orientation, this.orientation)) {
            if (!components.isEmpty())
                throw new IllegalStateException("Unable to change scrollBox orientation after adding components to it");

            this.orientation = orientation;
        }
    }

    @Override
    public ScrollBarPolicy getScrollBarPolicy() {
        return scrollBarPolicy;
    }

    @Override
    public void setScrollBarPolicy(ScrollBarPolicy scrollBarPolicy) {
        if (this.scrollBarPolicy != scrollBarPolicy) {
            applyScrollBarsPolicy(scrollBarPolicy);
        }
        this.scrollBarPolicy = scrollBarPolicy;
    }

    private void applyScrollBarsPolicy(ScrollBarPolicy scrollBarPolicy) {
        switch (scrollBarPolicy) {
            case VERTICAL:
                getContent().setHeight(Sizeable.SIZE_UNDEFINED, Sizeable.Unit.PIXELS);
                getContent().setWidth(100, Sizeable.Unit.PERCENTAGE);
                break;

            case HORIZONTAL:
                getContent().setHeight(100, Sizeable.Unit.PERCENTAGE);
                getContent().setWidth(Sizeable.SIZE_UNDEFINED, Sizeable.Unit.PIXELS);
                break;

            case BOTH:
                getContent().setSizeUndefined();
                break;

            case NONE:
                getContent().setSizeFull();
                break;
        }
    }

    @Override
    public void setMargin(boolean enable) {
        getContent().setMargin(enable);
    }

    @Override
    public void setMargin(boolean topEnable, boolean rightEnable, boolean bottomEnable, boolean leftEnable) {
        getContent().setMargin(new MarginInfo(topEnable, rightEnable, bottomEnable, leftEnable));
    }

    @Override
    public void setSpacing(boolean enabled) {
        getContent().setSpacing(enabled);
    }
}