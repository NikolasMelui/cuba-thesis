/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FlowBoxLayout;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.dom4j.Element;

/**
 * @author gorodnov
 * @version $Id$
 */
public class FlowBoxLayoutLoader extends ContainerLoader {

    public FlowBoxLayoutLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) {
        final FlowBoxLayout component = factory.createComponent(FlowBoxLayout.NAME);

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadSpacing(component, element);
        loadMargin(component, element);

        loadSubComponents(component, element, "visible");

        loadHeight(component, element);
        loadWidth(component, element);

        assignFrame(component);

        return component;
    }
}