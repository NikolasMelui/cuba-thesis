/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.data;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author tulupov
 * @version $Id$
 */
public class ObjectContainer implements com.vaadin.data.Container {

    private transient Log log = LogFactory.getLog(getClass());

    private static List<String> methodsName = new ArrayList<>();

    static {
        methodsName.add("getName");
        methodsName.add("getCaption");
    }

    private List values;

    public ObjectContainer(List values) {
        this.values = values;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LogFactory.getLog(getClass());
    }

    @Override
    public Item getItem(Object itemId) {
        return new ObjectItem(itemId);
    }

    @Override
    public Collection getContainerPropertyIds() {
        return Collections.emptyList();
    }

    @Override
    public Collection getItemIds() {
        return values;
    }

    @Override
    public Property getContainerProperty(Object itemId, Object propertyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class getType(Object propertyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean containsId(Object itemId) {
        return values.contains(itemId);
    }

    @Override
    public Item addItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object addItem() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addContainerProperty(Object propertyId, Class type, Object defaultValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeContainerProperty(Object propertyId) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAllItems() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    class ObjectItem implements Item {
        private Object item;
        private String name;

        ObjectItem(final Object item) {
            this.item = item;

            if (item instanceof Instance)
                this.name = InstanceUtils.getInstanceName((Instance) item);
            else
                this.name = getNameFromReflection(item);
        }

        private String getNameFromReflection(Object value) {
            final Method[] methods = value.getClass().getMethods();
            if (methods != null) {
                Method method = (Method) CollectionUtils.find(Arrays.asList(methods), new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        Method m = (Method) o;
                        return methodsName.contains(m.getName());
                    }
                });

                if (method != null) {
                    try {
                        final Object o = method.invoke(value);
                        if (o instanceof String)
                            return (String) o;
                        else
                            return String.valueOf(o);
                    } catch (Exception e) {
                        log.error("error invoking " + method.getName(), e);
                    }
                }
            }
            return String.valueOf(value);
        }

        @Override
        public Property getItemProperty(Object id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection getItemPropertyIds() {
            return Collections.emptyList();
        }

        @Override
        public boolean addItemProperty(Object id, Property property) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeItemProperty(Object id) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return name == null ? item.toString() : name;
        }
    }
}