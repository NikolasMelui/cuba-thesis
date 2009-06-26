/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 23.04.2009 15:26:55
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.web.toolkit.ui.ScrollablePanel;
import com.itmill.toolkit.ui.Layout;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.Collections;

public class ScrollBoxLayout extends ScrollablePanel implements com.haulmont.cuba.gui.components.ScrollBoxLayout {
    private String id;
    private Component component;
    private Alignment alignment = Alignment.TOP_LEFT;

    public void add(Component component) {
        final com.itmill.toolkit.ui.Component comp = ComponentsHelper.unwrap(component);
        if (comp instanceof Layout) {
            setLayout(((Layout) comp));
            this.component = component;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void remove(Component component) {
        if (getLayout() == ComponentsHelper.unwrap(component)) {
            setLayout(null);
            this.component = null;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void requestFocus() {
        if (getComponentIterator().hasNext()) {
            com.itmill.toolkit.ui.Component component = (com.itmill.toolkit.ui.Component) getComponentIterator().next();
            if (component instanceof Focusable) {
                ((Focusable) component).focus();
            }
        }
    }

    public <T extends Component> T getOwnComponent(String id) {
        return component != null && ObjectUtils.equals(component.getId(), id) ? (T) component : null;
    }

    public <T extends Component> T getComponent(String id) {
        final Layout layout = getLayout();
        if (layout instanceof Container) {
            final com.haulmont.cuba.gui.components.Component component = ((Container) layout).getOwnComponent(id);

            if (component == null) {
                return ComponentsHelper.<T>getComponentByIterate(layout, id);
            } else {
                return (T) component;
            }
        } else {
            return ComponentsHelper.<T>getComponentByIterate(layout, id);
        }
    }

    public Collection<Component> getOwnComponents() {
        return component == null ? Collections.<Component>emptyList() : Collections.singletonList(component);
    }

    public Collection<Component> getComponents() {
        return ComponentsHelper.getComponents(this);
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        final com.itmill.toolkit.ui.Component component = getParent();
        if (component instanceof Layout.AlignmentHandler) {
            ((Layout.AlignmentHandler) component).setComponentAlignment(this, ComponentsHelper.convertAlignment(alignment));
        }
    }

    public void expand(Component component, String height, String width) {
//        final com.itmill.toolkit.ui.Component expandedComponent = ComponentsHelper.unwrap(component);
//        if (getLayout() instanceof AbstractOrderedLayout) {
//            ComponentsHelper.expand((AbstractOrderedLayout) getLayout(), expandedComponent, height, width);
//        } else {
//            throw new UnsupportedOperationException();
//        }
    }
}
