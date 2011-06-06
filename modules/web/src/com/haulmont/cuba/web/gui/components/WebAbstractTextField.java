/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 22.12.2008 18:12:13
 * $Id$
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Formatter;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.web.gui.data.AbstractPropertyWrapper;
import com.haulmont.cuba.web.gui.data.DsManager;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.vaadin.data.Property;
import com.vaadin.data.util.PropertyFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;

public abstract class WebAbstractTextField<T extends com.haulmont.cuba.web.toolkit.ui.TextField>
    extends
        WebAbstractField<T>
    implements
        TextField, Component.Wrapper {

    private static Log log = LogFactory.getLog(WebAbstractTextField.class);

    private Datatype datatype;

    private Locale locale = UserSessionProvider.getLocale();

    protected Formatter formatter;

    public WebAbstractTextField() {
        this.component = createTextFieldImpl();

        final Property p = new AbstractPropertyWrapper() {
            public Class<?> getType() {
                return String.class;
            }
        };

        component.setPropertyDataSource(new PropertyFormatter(p) {
            @Override
            public String format(Object value) {
                if (datatype != null && value != null) {
                    return datatype.format(value,locale);
                } else if (value != null) {
                    return value.toString();
                } else {
                    return null;
                }
            }

            @Override
            public Object parse(String formattedValue) throws Exception {
                if (datatype != null) {
                    try {
                        return datatype.parse(formattedValue,locale);
                    } catch (ParseException e) {
                        log.warn("Unable to parse value of component " + getId() + "\n" + e.getMessage());
                        return null;
                    }
                } else {
                    return formattedValue;
                }
            }
        });
        attachListener(component);
        component.setImmediate(true);
        component.setNullRepresentation("");
    }

    protected abstract T createTextFieldImpl();

    public int getRows() {
        return component.getRows();
    }

    public void setRows(int rows) {
        component.setRows(rows);
    }

    public int getColumns() {
        return component.getColumns();
    }

    public void setColumns(int columns) {
        component.setColumns(columns);
    }

    public boolean isSecret() {
        return component.isSecret();
    }

    public void setSecret(boolean secret) {
        component.setSecret(secret);
    }

    public int getMaxLength() {
        return component.getMaxLength();
    }

    public void setMaxLength(int value) {
        component.setMaxLength(value);
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public void setDatatype(Datatype datatype) {
        this.datatype = datatype;
    }

    @Override
    public <T> T getValue() {
        Object value = super.getValue();
        if (datasource == null && datatype != null && value instanceof String) {
            try {
                return (T) datatype.parse((String) value, locale);
            } catch (ParseException e) {
                log.warn("Unable to parse value of component " + getId() + "\n" + e.getMessage());
                return null;
            }
        } else {
            return (T) value;
        }
    }

    @Override
    public void setValue(Object value) {
        if (datasource == null && datatype != null && value != null) {
            String str = datatype.format(value, locale);
            super.setValue(str);
        } else {
            super.setValue(value);
        }
    }

    @Override
    public void setDatasource(Datasource datasource, String property) {
        super.setDatasource(datasource, property);
        Integer len = (Integer) metaProperty.getAnnotations().get("length");
        if (len != null) {
            component.setMaxLength(len);
        }
    }

    @Override
    protected ItemWrapper createDatasourceWrapper(Datasource datasource, Collection<MetaPropertyPath> propertyPaths, DsManager dsManager) {
        return new ItemWrapper(datasource, propertyPaths, dsManager) {
            private static final long serialVersionUID = -5672549961402055473L;

            @Override
            protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath, DsManager dsManager) {
                return new PropertyWrapper(item, propertyPath, dsManager) {
                    private static final long serialVersionUID = -6484626348078235396L;

                    @Override
                    public String toString() {
                        if (formatter != null) {
                            Object value = getValue();
                            if (value instanceof Instance)
                                value = ((Instance) value).getInstanceName();
                            return formatter.format(value);
                        } else {
                            return super.toString();
                        }
                    }
                };
            }
        };
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

}
