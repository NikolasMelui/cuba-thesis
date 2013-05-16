/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.ButtonsPanel;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.dom4j.Element;

import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * @author gorodnov
 * @version $Id$
 */
public class ButtonsPanelLoader extends ContainerLoader {
    public ButtonsPanelLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {
        final ButtonsPanel component = factory.createComponent("buttonsPanel");

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);
        loadAlign(component, element);

        loadWidth(component, element);
        loadHeight(component, element);

        if (!element.elements().isEmpty()) {
            loadSubComponents(component, element, "visible");
        } else {
            String className = element.attributeValue("providerClass");
            if (className != null) {
                final Class<ButtonsPanel.Provider> clazz = ReflectionHelper.getClass(className);

                try {
                    final Constructor<ButtonsPanel.Provider> constructor = clazz.getConstructor();
                    final ButtonsPanel.Provider instance = constructor.newInstance();
                    applyButtonsProvider(factory, component, instance);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }

            } else {
                throw new IllegalStateException(
                        "<buttonsPanel> element must contains \"class\" attribute or at least one <button> element");
            }
        }

        return component;
    }

    private void applyButtonsProvider(ComponentsFactory factory, ButtonsPanel panel, ButtonsPanel.Provider buttonsProvider)
            throws IllegalAccessException, InstantiationException {
        final Collection<Button> buttons = buttonsProvider.getButtons();
        for (final Button button : buttons) {
            panel.addButton(button);
        }
    }
}