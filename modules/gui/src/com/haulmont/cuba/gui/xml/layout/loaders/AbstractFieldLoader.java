/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 22.12.2008 18:20:37
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.MessageUtils;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Field;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.List;

public class AbstractFieldLoader extends AbstractDatasourceComponentLoader {
    protected LayoutLoaderConfig config;
    protected ComponentsFactory factory;

    private static final long serialVersionUID = 4810695977418474135L;

    public AbstractFieldLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context);
        this.config = config;
        this.factory = factory;
    }

    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) throws InstantiationException, IllegalAccessException {
        final Field component = factory.createComponent(element.getName());

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadDatasource(component, element);

        loadVisible(component, element);
        loadEditable(component, element);
        loadEnable(component, element);

        loadStyleName(component, element);

        loadCaption(component, element);
        loadDescription(component, element);

        loadValidators(component, element);
        loadRequired(component, element);

        loadHeight(component, element);
        loadWidth(component, element);

        loadExpandable(component, element);

        assignFrame(component);

        return component;
    }

    protected void loadRequired(Field component, Element element) {
        final String required = element.attributeValue("required");
        if (!StringUtils.isEmpty(required)) {
            component.setRequired(BooleanUtils.toBoolean(required));
        }
        String msg = element.attributeValue("requiredMessage");
        if (msg != null) {
            component.setRequiredMessage(loadResourceString(msg));
        } else if (component.isRequired()) {
            component.setRequiredMessage(
                    MessageProvider.formatMessage(
                            messagesPack,
                            "validation.required.defaultMsg",
                            MessageUtils.getPropertyCaption(component.getMetaProperty())
                    )
            );
        }
    }

    protected void loadValidators(Field component, Element element) {
        @SuppressWarnings({"unchecked"})
        final List<Element> validatorElements = element.elements("validator");

        if (!validatorElements.isEmpty()) {
            for (Element validatorElement : validatorElements) {
                final Field.Validator validator = loadValidator(validatorElement);
                if (validator != null) {
                    component.addValidator(validator);
                }
            }

        } else if (component.getDatasource() != null) {
            MetaProperty property = component.getMetaProperty();
            Field.Validator validator = getDefaultValidator(property);
            if (validator != null) {
                component.addValidator(validator);
            }
        }
    }

}