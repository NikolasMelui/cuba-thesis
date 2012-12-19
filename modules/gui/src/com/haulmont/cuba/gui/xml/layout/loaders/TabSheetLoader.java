/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.List;

/**
 * @author abramov
 * @version $Id$
 */
public class TabSheetLoader extends ContainerLoader {

    public TabSheetLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {

        final TabSheet component = factory.createComponent(TabSheet.NAME);

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadHeight(component, element);
        loadWidth(component, element);

        final List<Element> tabElements = element.elements("tab");
        for (Element tabElement : tabElements) {
            final String name = tabElement.attributeValue("id");

            boolean lazy = Boolean.valueOf(tabElement.attributeValue("lazy"));

            final ComponentLoader loader = getLoader("vbox");
            final TabSheet.Tab tab;

            if (lazy) {
                tab = component.addLazyTab(name, tabElement, loader);
            } else {
                tab = component.addTab(name, loader.loadComponent(factory, tabElement, null));
            }

            final String detachable = tabElement.attributeValue("detachable");
            if (StringUtils.isNotEmpty(detachable)) {
                tab.setDetachable(Boolean.valueOf(detachable));
            }

            String caption = tabElement.attributeValue("caption");

            if (!StringUtils.isEmpty(caption)) {
                caption = loadResourceString(caption);
                tab.setCaption(caption);
            }

            String enable = tabElement.attributeValue("enable");
            if (enable == null) {
                final Element e = tabElement.element("enable");
                if (e != null) {
                    enable = e.getText();
                }
            }

            if (!StringUtils.isEmpty(enable)) {
                tab.setEnabled(Boolean.valueOf(enable));
            }
        }

        assignFrame(component);

        return component;
    }
}
