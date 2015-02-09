/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.chile.core.model.impl;

import com.haulmont.chile.core.model.*;

import java.io.InvalidObjectException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
@SuppressWarnings({"TransientFieldNotInitialized"})
public class MetaClassImpl extends MetadataObjectImpl implements MetaClass {

	private transient Map<String, MetaProperty> propertyByName = new HashMap<>();
    private transient Map<String, MetaProperty> ownPropertyByName = new HashMap<>();

	private transient final MetaModel model;
    private transient Class javaClass;

    protected transient List<MetaClass> ancestors = new ArrayList<>(3);
    protected transient Collection<MetaClass> descendants = new ArrayList<>(1);

    private static final long serialVersionUID = 7862691995170873154L;

    public MetaClassImpl(MetaModel model, String className) {
		super();

		this.model = model;
        this.name = className;

        ((MetaModelImpl) model).registerClass(this);
    }

    protected Object readResolve() throws InvalidObjectException {
        Session session = SessionImpl.serializationSupportSession;
        if (session == null) {
            return Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[] { MetaClass.class },
                    new MetaClassInvocationHandler(name)
            );
        } else {
            return session.getClass(name);
        }
    }

    @Override
    public MetaClass getAncestor() {
        if (ancestors.size() == 0) {
            return null;
        } else  {
            return ancestors.get(0);
        }
    }

    @Override
    public List<MetaClass> getAncestors() {
        return new ArrayList<>(ancestors);
    }

    @Override
    public Collection<MetaClass> getDescendants() {
        return new ArrayList<>(descendants);
    }

	@Override
    public <T> T createInstance() throws InstantiationException, IllegalAccessException {
        final Class aClass = getJavaClass();
        if (aClass == null) throw new IllegalStateException(String.format("Can't find java class for metaClass '%s'", this));

        return (T) aClass.newInstance();
	}

	public <T> T createInstance(Class<T> clazz) {
		throw new UnsupportedOperationException();
	}

	@Override
    public MetaModel getModel() {
		return model;
	}

    @Override
    public Class getJavaClass() {
        return javaClass;
    }

    @Override
    public Collection<MetaProperty> getProperties() {
		return propertyByName.values();
	}

	@Override
    public MetaProperty getProperty(String name) {
		return propertyByName.get(name);
	}

    @Override
    public MetaProperty getPropertyNN(String name) {
        MetaProperty property = getProperty(name);
        if (property == null)
            throw new IllegalArgumentException("Property '" + name + "' not found in " + getName());
        return property;
    }

    @Override
    public MetaPropertyPath getPropertyEx(String propertyPath) {
        String[] properties = propertyPath.split("[.]");
        List<MetaProperty> metaProperties = new ArrayList<>();

		MetaProperty currentProperty;
		MetaClass currentClass = this;

		for (String property : properties) {
			if (currentClass == null) break;
			currentProperty = currentClass.getProperty(property);
			if (currentProperty == null) break;
			
			final Range range = currentProperty.getRange();
			currentClass = range.isClass() ? range.asClass() : null;

            metaProperties.add(currentProperty);
		}
		
		return new MetaPropertyPath(this, metaProperties.toArray(new MetaProperty[metaProperties.size()]));
	}

    @Override
    public MetaPropertyPath getPropertyPath(String propertyPath) {
        String[] properties = propertyPath.split("[.]");
        List<MetaProperty> metaProperties = new ArrayList<>();

		MetaProperty currentProperty;
		MetaClass currentClass = this;

		for (String property : properties) {
			if (currentClass == null) return null;
			currentProperty = currentClass.getProperty(property);
			if (currentProperty == null) return null;

			final Range range = currentProperty.getRange();
			currentClass = range.isClass() ? range.asClass() : null;

            metaProperties.add(currentProperty);
		}

		return new MetaPropertyPath(this, metaProperties.toArray(new MetaProperty[metaProperties.size()]));
    }

    @Override
    public Collection<MetaProperty> getOwnProperties() {
        return ownPropertyByName.values();
    }

    public void setJavaClass(Class javaClass) {
        this.javaClass = javaClass;
        ((MetaModelImpl) model).registerClass(this);
    }

    public void addAncestor(MetaClass ancestorClass) {
        if (!ancestors.contains(ancestorClass)) {
            ancestors.add(ancestorClass);
            for (MetaProperty metaProperty : ancestorClass.getProperties()) {
                propertyByName.put(metaProperty.getName(), metaProperty);
            }
        }
        if (!((MetaClassImpl) ancestorClass).descendants.contains(this))
            ((MetaClassImpl) ancestorClass).descendants.add(this);
    }

    public void registerProperty(MetaProperty metaProperty) {
        propertyByName.put(metaProperty.getName(), metaProperty);
        ownPropertyByName.put(metaProperty.getName(), metaProperty);
        for (MetaClass descendant : descendants) {
            ((MetaClassImpl) descendant).registerAncestorProperty(metaProperty);
        }
    }

    public void registerAncestorProperty(MetaProperty metaProperty) {
        final MetaProperty prop = propertyByName.get(metaProperty.getName());
        if (prop == null) {
            propertyByName.put(metaProperty.getName(), metaProperty);
            for (MetaClass descendant : descendants) {
                ((MetaClassImpl) descendant).registerAncestorProperty(metaProperty);
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static class MetaClassInvocationHandler implements InvocationHandler {

        private String name;
        private volatile MetaClass metaClass;

        public MetaClassInvocationHandler(String name) {
            this.name = name;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName())) {
                return hashCode();
            }
            if (metaClass == null) {
                synchronized (this) {
                    if (metaClass == null) {
                        Session session = SessionImpl.serializationSupportSession;
                        if (session == null)
                            throw new IllegalStateException("SerializationSupportSession is not initialized");
                        metaClass = session.getClass(name);
                    }
                }
            }
            return method.invoke(metaClass, args);
        }
    }
}