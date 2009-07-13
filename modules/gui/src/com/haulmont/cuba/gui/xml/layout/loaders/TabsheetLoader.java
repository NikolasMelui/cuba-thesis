/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 02.02.2009 16:46:18
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Tabsheet;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.List;

public class TabsheetLoader extends ContainerLoader {
    public TabsheetLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) throws InstantiationException, IllegalAccessException {
        final Tabsheet component = factory.createComponent("tabsheet");

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadHeight(component, element);
        loadWidth(component, element);

        loadExpandable(component, element);

        final List<Element> tabElements = element.elements("tab");
        for (Element tabElement : tabElements) {
            final String name = tabElement.attributeValue("id");

            final ComponentLoader loader = getLoader("vbox");

            final Tabsheet.Tab tab = component.addTab(name, loader.loadComponent(factory, tabElement, null));
            String caption = tabElement.attributeValue("caption");

            if (!StringUtils.isEmpty(caption)) {
                caption = loadResourceString(caption);
                tab.setCaption(caption);
            }
        }

        addAssignWindowTask(component);

        return component;
    }
}
