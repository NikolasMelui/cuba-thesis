/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.sys;

import com.google.common.collect.Iterables;
import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.sys.persistence.DbmsType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;

import javax.annotation.Nullable;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.TemporalType;
import java.util.*;

/**
 * Implementation of {@link TypedQuery} interface based on OpenJPA.
 *
 * @author krivopustov
 * @version $Id$
 */
public class QueryImpl<T> implements TypedQuery<T> {

    private Log log = LogFactory.getLog(QueryImpl.class);

    private EntityManagerImpl em;
    private Metadata metadata;
    private OpenJPAEntityManager emDelegate;
    private OpenJPAQuery query;
    private boolean isNative;
    private String queryString;
    private Class resultClass;
    private FetchPlanManager fetchPlanMgr;

    private Collection<QueryMacroHandler> macroHandlers;

    public QueryImpl(EntityManagerImpl entityManager, boolean isNative, @Nullable Class resultClass,
                     Metadata metadata, FetchPlanManager fetchPlanMgr) {
        this.em = entityManager;
        this.metadata = metadata;
        this.emDelegate = entityManager.getDelegate();
        this.isNative = isNative;
        this.macroHandlers = AppBeans.getAll(QueryMacroHandler.class).values();
        this.resultClass = resultClass;
        this.fetchPlanMgr = fetchPlanMgr;
    }

    private OpenJPAQuery<T> getQuery() {
        if (query == null) {
            if (isNative) {
                log.trace("Creating SQL query: " + queryString);
                if (resultClass == null)
                    query = emDelegate.createNativeQuery(queryString);
                else
                    query = emDelegate.createNativeQuery(queryString, resultClass);
                query.setFlushMode(FlushModeType.COMMIT);
            } else {
                log.trace("Creating JPQL query: " + queryString);
                String s = transformQueryString();
                log.trace("Transformed JPQL query: " + s);
                if (resultClass != null) {
                    query = (OpenJPAQuery) emDelegate.createQuery(s, resultClass);
                } else {
                    query = emDelegate.createQuery(s);
                }
                query.setFlushMode(FlushModeType.COMMIT);
            }
        }
        return query;
    }

    private String transformQueryString() {
        String result = expandMacros(queryString);

        String entityName = QueryTransformerFactory.createParser(result).getEntityName();
        Class effectiveClass = metadata.getExtendedEntities().getEffectiveClass(entityName);
        String effectiveEntityName = metadata.getSession().getClassNN(effectiveClass).getName();
        if (!effectiveEntityName.equals(entityName)) {
            QueryTransformer transformer = QueryTransformerFactory.createTransformer(result, entityName);
            transformer.replaceEntityName(effectiveEntityName);
            result = transformer.getResult();
        }

        if (em.isSoftDeletion()
                && PersistenceHelper.isSoftDeleted(effectiveClass)) {
            QueryTransformer transformer = QueryTransformerFactory.createTransformer(result, effectiveEntityName);
            transformer.addWhere("e.deleteTs is null");
            result = transformer.getResult();
        }

        return result;
    }

    private String expandMacros(String queryStr) {
        String result = queryStr;
        if (macroHandlers != null) {
            for (QueryMacroHandler handler : macroHandlers) {
                result = handler.expandMacro(result);
            }
        }
        return result;
    }

    private void addMacroParams(OpenJPAQuery jpaQuery) {
        if (macroHandlers != null) {
            for (QueryMacroHandler handler : macroHandlers) {
                handler.setQueryParams(getQuery().getNamedParameters());
                for (Map.Entry<String, Object> entry : handler.getParams().entrySet()) {
                    jpaQuery.setParameter(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public List<T> getResultList() {
        if (!isNative && log.isTraceEnabled())
            log.trace("JPQL query result class: " + getQuery().getResultClass());
        OpenJPAQuery<T> jpaQuery = getQuery();
        addMacroParams(jpaQuery);
        return jpaQuery.getResultList();
    }

    @Override
    public T getSingleResult() {
        if (!isNative && log.isTraceEnabled())
            log.trace("JPQL query result class: " + getQuery().getResultClass());

        OpenJPAQuery<T> jpaQuery = getQuery();
        addMacroParams(jpaQuery);
        return jpaQuery.getSingleResult();
    }

    @Override
    @Nullable
    public T getFirstResult() {
        if (!isNative && log.isTraceEnabled())
            log.trace("JPQL query result class: " + getQuery().getResultClass());
        OpenJPAQuery<T> jpaQuery = getQuery();
        addMacroParams(jpaQuery);
        List<T> resultList = jpaQuery.getResultList();
        return Iterables.getFirst(resultList, null);
    }

    @Override
    public int executeUpdate() {
        OpenJPAQuery jpaQuery = getQuery();
        addMacroParams(jpaQuery);
        return jpaQuery.executeUpdate();
    }

    @Override
    public Query setMaxResults(int maxResult) {
        getQuery().setMaxResults(maxResult);
        return this;
    }

    @Override
    public Query setFirstResult(int startPosition) {
        getQuery().setFirstResult(startPosition);
        return this;
    }

    @Override
    public Query setParameter(String name, Object value) {
        return setParameter(name, value, true);
    }

    @Override
    public Query setParameter(String name, Object value, boolean implicitConversions) {
        if (implicitConversions && value instanceof Entity)
            value = ((BaseEntity) value).getId();
        getQuery().setParameter(name, value);
        return this;
    }

    @Override
    public Query setParameter(String name, Date value, TemporalType temporalType) {
        getQuery().setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public Query setParameter(int position, Object value) {
        return setParameter(position, value, true);
    }

    @Override
    public Query setParameter(int position, Object value, boolean implicitConversions) {
        if (isNative && value instanceof UUID && DbmsType.getCurrent() == DbmsType.POSTGRES) {
            Class c = ReflectionHelper.getClass("com.haulmont.cuba.core.sys.persistence.PostgresUUID");
            try {
                value = ReflectionHelper.newInstance(c, value);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else if (implicitConversions && value instanceof Entity)
            value = ((BaseEntity) value).getId();

        getQuery().setParameter(position, value);
        return this;
    }

    @Override
    public Query setParameter(int position, Date value, TemporalType temporalType) {
        getQuery().setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        getQuery().setLockMode(lockMode);
        return this;
    }

    @Override
    public Query setView(View view) {
        fetchPlanMgr.setView(getQuery().getFetchPlan(), view);
        return this;
    }

    @Override
    public Query setViewName(String viewName) {
        if (resultClass == null)
            throw new IllegalStateException("resultClass is null");

        setView(metadata.getViewRepository().getView(resultClass, viewName));
        return this;
    }

    @Override
    public Query setView(Class<? extends Entity> entityClass, String viewName) {
        setView(metadata.getViewRepository().getView(entityClass, viewName));
        return this;
    }

    @Override
    public Query addView(View view) {
        fetchPlanMgr.addView(getQuery().getFetchPlan(), view);
        return this;
    }

    @Override
    public Query addViewName(String viewName) {
        if (resultClass == null)
            throw new IllegalStateException("resultClass is null");

        addView(metadata.getViewRepository().getView(resultClass, viewName));
        return this;
    }

    @Override
    public Query addView(Class<? extends Entity> entityClass, String viewName) {
        addView(metadata.getViewRepository().getView(entityClass, viewName));
        return this;
    }

    @Override
    public javax.persistence.Query getDelegate() {
        return getQuery();
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public void setQueryString(String queryString) {
        if (query != null)
            throw new IllegalStateException("Unable to set query string: query is already created");
        this.queryString = queryString;
    }
}
