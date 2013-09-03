/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.impl.DateDatatype;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.DateField;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.persistence.TemporalType;

/**
 * @author abramov
 * @version $Id$
 */
public class DateFieldLoader extends AbstractFieldLoader {
    public DateFieldLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) {
        final DateField component = (DateField) super.loadComponent(factory, element, parent);

        TemporalType tt = null;
        if (component.getMetaProperty() != null) {
            if (component.getMetaProperty().getRange().asDatatype().equals(Datatypes.get(DateDatatype.NAME)))
                tt = TemporalType.DATE;
            else if (component.getMetaProperty().getAnnotations() != null)
                tt = (TemporalType) component.getMetaProperty().getAnnotations().get("temporal");
        }

        final String resolution = element.attributeValue("resolution");
        String dateFormat = element.attributeValue("dateFormat");
        if (!StringUtils.isEmpty(resolution)) {
            DateField.Resolution res = DateField.Resolution.valueOf(resolution);
            component.setResolution(res);
            if (dateFormat == null) {
                if (res == DateField.Resolution.DAY) {
                    dateFormat = "msg://dateFormat";
                }
                else if (res == DateField.Resolution.MIN) {
                    dateFormat = "msg://dateTimeFormat";                        
                }
            }
        } else if (tt == TemporalType.DATE) {
            component.setResolution(DateField.Resolution.DAY);
        }

        if (!StringUtils.isEmpty(dateFormat)) {
            //noinspection ConstantConditions
            if (dateFormat.startsWith("msg://")) {
                dateFormat = messages.getMainMessage(dateFormat.substring(6, dateFormat.length()));
            }
            component.setDateFormat(dateFormat);
        } else {
            String formatStr;
            if (tt == TemporalType.DATE) {
                formatStr = messages.getMainMessage("dateFormat");
            } else {
                formatStr = messages.getMainMessage("dateTimeFormat");
            }
            component.setDateFormat(formatStr);
        }

        return component;
    }
}
