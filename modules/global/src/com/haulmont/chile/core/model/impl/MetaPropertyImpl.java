/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.chile.core.model.impl;

import com.haulmont.chile.core.model.*;

import java.io.InvalidObjectException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author abramov
 * @version $Id$
 */
@SuppressWarnings({"TransientFieldNotInitialized"})
public class MetaPropertyImpl extends MetadataObjectImpl<MetaProperty> implements MetaProperty {

	private final MetaClass domain;
	private transient final MetaModel model;
	
	private transient boolean mandatory;
    private transient boolean readOnly;
	private transient Type type;
	private transient Range range;
	
	private transient MetaProperty inverse;

    private transient AnnotatedElement annotatedElement;
    private transient Class<?> javaType;
    private transient Class<?> declaringClass;

    private static final long serialVersionUID = -2827471157045502206L;

    public MetaPropertyImpl(MetaClass domain, String name) {
		super();
		this.domain = domain;
		this.model = domain.getModel();
        this.name = name;

        ((MetaClassImpl) domain).registerProperty(this);
    }

    protected Object readResolve() throws InvalidObjectException {
        Session session = SessionImpl.serializationSupportSession;
        if (session == null) {
            return Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[] { MetaProperty.class },
                    new MetaPropertyInvocationHandler(domain, name)
            );
        } else {
            return domain.getProperty(name);
        }
    }

	public MetaClass getDomain() {
		return domain;
	}

	public MetaProperty getInverse() {
		return inverse;
	}

	public void setInverse(MetaProperty inverse) {
		this.inverse = inverse;
	}

	public MetaModel getModel() {
		return model;
	}

	public Range getRange() {
		return range;
	}

    public AnnotatedElement getAnnotatedElement() {
        return annotatedElement;
    }


    public void setAnnotatedElement(AnnotatedElement annotatedElement) {
        this.annotatedElement = annotatedElement;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(Class<?> declaringClass) {
        this.declaringClass = declaringClass;
    }

    public void setRange(Range range) {
		this.range = range;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isMandatory() {
		return mandatory;
	}

    public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String toString() {
        return domain.getName() + "." + name;
    }

    private class MetaPropertyInvocationHandler implements InvocationHandler {

        private MetaClass domain;
        private String name;
        private volatile MetaProperty metaProperty;

        public MetaPropertyInvocationHandler(MetaClass domain, String name) {
            this.domain = domain;
            this.name = name;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName())) {
                return hashCode();
            }
            if (metaProperty == null) {
                synchronized (this) {
                    if (metaProperty == null) {
                        metaProperty = domain.getProperty(name);
                    }
                }
            }
            return method.invoke(metaProperty, args);
        }
    }
}
