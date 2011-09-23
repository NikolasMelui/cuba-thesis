/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.impl.DateTimeDatatype;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.UserSessionProvider;
import com.haulmont.cuba.core.sys.SetValueEntity;
import com.haulmont.cuba.gui.ServiceLocator;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.ValueListener;
import org.apache.commons.lang.ObjectUtils;
import org.dom4j.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public abstract class AbstractParam<T> {

    public enum Type {
        ENTITY,
        ENUM,
        RUNTIME_ENUM,
        DATATYPE,
        UNARY
    }

    public static final String NULL = "NULL";

    protected String name;
    protected Type type;
    protected Class javaClass;
    protected Object value;
    protected String entityWhere;
    protected String entityView;
    protected Datasource datasource;
    protected MetaProperty property;
    protected boolean inExpr;
    protected List<String> runtimeEnum;
    protected UUID categoryAttrId;

    private List<ValueListener> listeners = new ArrayList<ValueListener>();

    public AbstractParam(String name, Class javaClass, String entityWhere, String entityView,
                         Datasource datasource, boolean inExpr) {
        this(name, javaClass, entityWhere, entityView, datasource, null, inExpr);
    }

    public AbstractParam(String name, Class javaClass, String entityWhere, String entityView,
                         Datasource datasource, boolean inExpr, UUID categoryAttrId) {
        this(name, javaClass, entityWhere, entityView, datasource, null, inExpr);
        this.categoryAttrId = categoryAttrId;
    }

    public AbstractParam(String name, Class javaClass, String entityWhere, String entityView, Datasource datasource,
                         MetaProperty property, boolean inExpr) {
        this.name = name;
        if (javaClass != null) {
            this.javaClass = javaClass;
            if (SetValueEntity.class.isAssignableFrom(javaClass)) {
                type = Type.RUNTIME_ENUM;
            } else if (Entity.class.isAssignableFrom(javaClass)) {
                type = Type.ENTITY;
            } else if (Enum.class.isAssignableFrom(javaClass)) {
                type = Type.ENUM;
            } else {
                type = Type.DATATYPE;
            }
        } else {
            type = Type.UNARY;
            this.javaClass = Boolean.class;
        }
        this.entityWhere = entityWhere;
        this.entityView = entityView;
        this.datasource = datasource;
        this.property = property;
        this.inExpr = inExpr;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Class getJavaClass() {
        return javaClass;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (!ObjectUtils.equals(value, this.value)) {
            Object prevValue = this.value;
            this.value = value;
            for (ValueListener listener : listeners) {
                listener.valueChanged(this, "value", prevValue, value);
            }
        }
    }

    public void parseValue(String text) {
        if (NULL.equals(text)) {
            value = null;
            return;
        }

        if (inExpr) {
            String[] parts = text.split(",");
            List list = new ArrayList(parts.length);
            for (String part : parts) {
                list.add(parseSingleValue(part));
            }
            value = list;
        } else {
            value = parseSingleValue(text);
        }
    }

    private Object parseSingleValue(String text) {
        Object value;
        switch (type) {
            case ENTITY:
                value = loadEntity(text);
                break;

            case ENUM:
                value = Enum.valueOf(javaClass, text);
                break;

            case RUNTIME_ENUM:
                value = text;
                break;

            case DATATYPE:
            case UNARY:
                Datatype datatype = Datatypes.get(javaClass);
                if (datatype == null)
                    throw new UnsupportedOperationException("Unsupported parameter class: " + javaClass);
                //hardcode for compatibility with old datatypes
                if (datatype instanceof DateTimeDatatype) {
                    try {
                        value = datatype.parse(text);
                    } catch (ParseException e) {
                        try {
                            value = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(text);
                        } catch (ParseException exception) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    try {
                        value = datatype.parse(text);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;

            default:
                throw new IllegalStateException("Param type unknown");
        }
        return value;
    }

    private Object loadEntity(String id) {
        DataService service = ServiceLocator.getDataService();
        LoadContext ctx = new LoadContext(javaClass).setId(UUID.fromString(id));
        Entity entity = service.load(ctx);
        return entity;
    }

    public String formatValue() {
        if (value == null)
            return NULL;

        if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            for (Iterator iterator = ((Collection) value).iterator(); iterator.hasNext();) {
                Object v = iterator.next();
                sb.append(formatSingleValue(v));
                if (iterator.hasNext())
                    sb.append(",");
            }
            return sb.toString();
        } else {
            return formatSingleValue(value);
        }
    }

    private String formatSingleValue(Object v) {
        switch (type) {
            case ENTITY:
                if (v instanceof UUID)
                    return v.toString();
                else if (v instanceof Entity)
                    return ((Entity) v).getId().toString();

            case ENUM:
                return ((Enum) v).name();
            case RUNTIME_ENUM:
                return (String) v;

            case DATATYPE:
            case UNARY:
                Datatype datatype = Datatypes.getInstance().get(javaClass);
                if (datatype == null)
                    throw new UnsupportedOperationException("Unsupported parameter class: " + javaClass);

                return datatype.format(v);

            default:
                throw new IllegalStateException("Param type unknown");
        }
    }

    protected String getValueCaption(Object v) {
        if (v == null)
            return null;

        switch (type) {
            case ENTITY:
                if (v instanceof Instance)
                    return ((Instance) v).getInstanceName();
                else
                    v.toString();

            case ENUM:
                return MessageProvider.getMessage((Enum) v);

            case RUNTIME_ENUM:
                return (String) v;

            case DATATYPE:
            case UNARY:
                Datatype datatype = Datatypes.get(javaClass);
                if (datatype == null)
                    throw new UnsupportedOperationException("Unsupported parameter class: " + javaClass);

                return datatype.format(v, UserSessionProvider.getLocale());

            default:
                throw new IllegalStateException("Param type unknown");
        }
    }

    public abstract T createEditComponent();

    public void toXml(Element element) {
        Element paramElem = element.addElement("param");
        paramElem.addAttribute("name", getName());
        paramElem.addAttribute("javaClass", getJavaClass().getName());
        if (SetValueEntity.class.isAssignableFrom(javaClass) && runtimeEnum != null) {
            paramElem.addAttribute("categoryAttrId", categoryAttrId.toString());
        }
        paramElem.setText(formatValue());
    }

    public void addListener(ValueListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(ValueListener listener) {
        listeners.remove(listener);
    }

    public MetaProperty getProperty() {
        return property;
    }

}
