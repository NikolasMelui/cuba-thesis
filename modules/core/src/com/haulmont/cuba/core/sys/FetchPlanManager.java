/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.entity.Updatable;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewProperty;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.persistence.FetchPlan;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@link View} to OpenJPA's fetch plans.
 *
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("cuba_FetchPlanManager")
public class FetchPlanManager {

    private Map<View, Set<FetchPlanField>> fetchPlans = new ConcurrentHashMap<View, Set<FetchPlanField>>();

    public void setView(FetchPlan fetchPlan, @Nullable View view) {
        if (fetchPlan == null)
            throw new IllegalArgumentException("FetchPlan is null");

        fetchPlan.clearFetchGroups();

        if (view != null) {
            fetchPlan.removeFetchGroup(FetchPlan.GROUP_DEFAULT);
            fetchPlan.setExtendedPathLookup(true);

            addViewToFetchPlan(fetchPlan, view);
        } else {
            fetchPlan.addFetchGroup(FetchPlan.GROUP_DEFAULT);
        }
    }

    public void addView(FetchPlan fetchPlan, View view) {
        Objects.requireNonNull(fetchPlan, "FetchPlan is null");
        Objects.requireNonNull(view, "View is null");

        addViewToFetchPlan(fetchPlan, view);
    }

    private void addViewToFetchPlan(FetchPlan fetchPlan, View view) {
        Set<FetchPlanField> fetchPlanFields;
        if (!StringUtils.isEmpty(view.getName())) {
            fetchPlanFields = fetchPlans.get(view);
            if (fetchPlanFields == null) {
                fetchPlanFields = new HashSet<FetchPlanField>();
                processView(view, fetchPlanFields);
                fetchPlans.put(view, fetchPlanFields);
            }
        } else {
            // Don't cache unnamed views, because they are usually created programmatically and may be different
            // each time
            fetchPlanFields = new HashSet<FetchPlanField>();
            processView(view, fetchPlanFields);
        }
        for (FetchPlanField field : fetchPlanFields) {
            fetchPlan.addField(field.entityClass, field.property);
        }
    }

    private void processView(View view, Set<FetchPlanField> fetchPlanFields) {
        if (view.isIncludeSystemProperties()) {
            includeSystemProperties(view, fetchPlanFields);
        }
        // Always add SoftDelete properties to support EntityManager contract
        if (SoftDelete.class.isAssignableFrom(view.getEntityClass())) {
            for (String property : SoftDelete.PROPERTIES) {
                fetchPlanFields.add(createFetchPlanField(view.getEntityClass(), property));
            }
        }

        for (ViewProperty property : view.getProperties()) {
            if (property.isLazy())
                continue;

            FetchPlanField field = createFetchPlanField(view.getEntityClass(), property.getName());
            fetchPlanFields.add(field);
            if (property.getView() != null) {
                processView(property.getView(), fetchPlanFields);
            }
        }
    }

    private void includeSystemProperties(View view, Set<FetchPlanField> fetchPlanFields) {
        Class<? extends Entity> entityClass = view.getEntityClass();
        if (BaseEntity.class.isAssignableFrom(entityClass)) {
            for (String property : BaseEntity.PROPERTIES) {
                fetchPlanFields.add(createFetchPlanField(entityClass, property));
            }
        }
        if (Updatable.class.isAssignableFrom(entityClass)) {
            for (String property : Updatable.PROPERTIES) {
                fetchPlanFields.add(createFetchPlanField(entityClass, property));
            }
        }
    }

    private FetchPlanField createFetchPlanField(Class<? extends Entity> entityClass, String property) {
        return new FetchPlanField(getRealClass(entityClass, property), property);
    }

    /**
     * This is the workaround for OpenJPA's inability to create correct fetch plan for entities
     * inherited form other entities (not directly from a MappedSuperclass). Here we do the following:
     * <ul>
     * <li>If the property is declared in an entity, return this entity class</li>
     * <li>If the property is declared in a MappedSuperclass, return a recent entity class up to the hierarchy</li>
     * </ul>
     * @param entityClass   entity class for which a fetch plan is being created
     * @param property      entity property name to include in the fetch plan
     * @return              a class in the hierarchy (see conditions above)
     */
    @SuppressWarnings("unchecked")
    private Class getRealClass(Class<? extends Entity> entityClass, String property) {
        if (hasDeclaredField(entityClass, property))
            return entityClass;

        List<Class> superclasses = ClassUtils.getAllSuperclasses(entityClass);
        for (int i = 0; i < superclasses.size(); i++) {
            Class superclass = superclasses.get(i);
            if (hasDeclaredField(superclass, property)) {
                // If the class declaring the field is an entity, return it
                if (superclass.isAnnotationPresent(javax.persistence.Entity.class))
                    return superclass;
                // Else search for a recent entity up to the hierarchy
                for (int j = i - 1; j >= 0; j--) {
                    superclass = superclasses.get(j);
                    if (superclass.isAnnotationPresent(javax.persistence.Entity.class))
                        return superclass;
                }
            }
        }
        return entityClass;
    }

    private boolean hasDeclaredField(Class<? extends Entity> entityClass, String name) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(name))
                return true;
        }
        return false;
    }

    public void clearCache() {
        fetchPlans.clear();
    }

    private static class FetchPlanField {
        private final Class entityClass;
        private final String property;

        private FetchPlanField(Class entityClass, String property) {
            this.entityClass = entityClass;
            this.property = property;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FetchPlanField that = (FetchPlanField) o;

            if (!entityClass.equals(that.entityClass)) return false;
            if (!property.equals(that.property)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = entityClass.hashCode();
            result = 31 * result + property.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return entityClass.getName() + "." + property;
        }
    }
}

