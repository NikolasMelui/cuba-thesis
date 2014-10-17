/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Session;
import com.haulmont.chile.jpa.loader.JPAAnnotationsLoader;
import com.haulmont.cuba.core.entity.*;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
* @author krivopustov
* @version $Id$
*/
public class CubaAnnotationsLoader extends JPAAnnotationsLoader {

    private static final List<Class> SYSTEM_INTERFACES = Arrays.<Class>asList(
            Instance.class,
            Entity.class,
            BaseEntity.class,
            BaseGenericIdEntity.class,
            Versioned.class,
            Updatable.class,
            SoftDelete.class
    );

    public CubaAnnotationsLoader(Session session) {
        super(session);
    }

    @Override
    protected boolean isMetaPropertyField(Field field) {
        String name = field.getName();
        return super.isMetaPropertyField(field)
                && !name.equals("pcVersionInit")
                && !name.equals("pcStateManager")
                && !name.equals("pcDetachedState")
                && !name.equals("__valueListeners");
    }

    @Override
    protected void onPropertyLoaded(MetaProperty metaProperty, Field field) {
        super.onPropertyLoaded(metaProperty, field);
        boolean system = isPrimaryKey(field) || propertyBelongsTo(field, metaProperty, SYSTEM_INTERFACES);
        if (system)
            metaProperty.getAnnotations().put("system", true);
    }

    private boolean propertyBelongsTo(Field field, MetaProperty metaProperty, List<Class> systemInterfaces) {
        String getterName = "get" + StringUtils.capitalize(metaProperty.getName());

        Class<?> aClass = field.getDeclaringClass();
        //noinspection unchecked
        List<Class> allInterfaces = ClassUtils.getAllInterfaces(aClass);
        for (Class intf : allInterfaces) {
            Method[] methods = intf.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(getterName) && method.getParameterTypes().length == 0) {
                    if (systemInterfaces.contains(intf))
                        return true;
                }
            }
        }
        return false;
    }
}