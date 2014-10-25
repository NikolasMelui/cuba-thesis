/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.dom4j.Element;

/**
 * @author abramov
 * @version $Id$
 */
public class FileUploadFieldLoader extends ComponentLoader {

    public FileUploadFieldLoader(Context context) {
        super(context);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) {
        Component component = factory.createComponent(element.getName());

        initComponent(element, component, parent);

        return component;
    }

    protected void initComponent(Element element, Component component, Component parent) {
        loadId(parent, element);
        loadEnable(parent, element);
        loadVisible(parent, element);

        loadStyleName(parent, element);
        loadAlign(parent, element);

        loadHeight(parent, element);
        loadWidth(parent, element);

        loadCaption((Component.HasCaption) parent, element);
        loadDescription((Component.HasCaption) parent, element);

        assignFrame((Component.BelongToFrame) parent);
    }
}