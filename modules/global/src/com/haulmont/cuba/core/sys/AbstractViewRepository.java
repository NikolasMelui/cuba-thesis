/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.sys;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.Range;
import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base implementation of the {@link ViewRepository}. Contains methods to store {@link View} objects and deploy
 * them from XML.
 *
 * <p/> Don't replace this class completely, because the framework uses it directly.
 *
 * @author krivopustov
 * @version $Id$
 */
public class AbstractViewRepository implements ViewRepository {

    protected Log log = LogFactory.getLog(getClass());

    protected List<String> readFileNames = new LinkedList<>();

    protected Map<MetaClass, Map<String, View>> storage = new ConcurrentHashMap<>();

    @Inject
    protected Metadata metadata;

    @Inject
    protected Resources resources;

    private volatile boolean initialized;

    protected void checkInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    log.info("Initializing views");
                    init();
                    initialized = true;
                }
            }
        }
    }

    protected void init() {
        String configName = AppContext.getProperty("cuba.viewsConfig");
        if (!StringUtils.isBlank(configName)) {
            StrTokenizer tokenizer = new StrTokenizer(configName);
            for (String fileName : tokenizer.getTokenArray()) {
                deployViews(fileName);
            }
        }
    }

    /**
     * Get View for an entity.
     * @param entityClass   entity class
     * @param name          view name
     * @return              view instance. Throws {@link com.haulmont.cuba.core.global.ViewNotFoundException} if not found.
     */
    @Override
    public View getView(Class<? extends Entity> entityClass, String name) {
        return getView(metadata.getSession().getClassNN(entityClass), name);
    }

    /**
     * Get View for an entity.
     * @param metaClass     entity class
     * @param name          view name
     * @return              view instance. Throws {@link com.haulmont.cuba.core.global.ViewNotFoundException} if not found.
     */
    @Override
    public View getView(MetaClass metaClass, String name) {
        Objects.requireNonNull(metaClass, "MetaClass is null");

        View view = findView(metaClass, name);

        if (view == null)
            throw new ViewNotFoundException(String.format("View %s/%s not found", metaClass.getName(), name));
        return view;
    }

    /**
     * Searches for a View for an entity
     * @param metaClass     entity class
     * @param name          view name
     * @return              view instance or null if no view found
     */
    @Override
    @Nullable
    public View findView(MetaClass metaClass, String name) {
        if (metaClass == null || name == null)
            return null;

        checkInitialized();

        // Replace with extended entity if such one exists
        metaClass = metadata.getExtendedEntities().getEffectiveMetaClass(metaClass);

        View view = retrieveView(metaClass, name, false);
        if (view == null) {
            MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
            if (originalMetaClass != null) {
                view = retrieveView(originalMetaClass, name, false);
            }
        }
        return view;
    }

    protected View deployDefaultView(MetaClass metaClass, String name) {
        metaClass = metadata.getExtendedEntities().getEffectiveMetaClass(metaClass);

        Class<? extends BaseEntity> javaClass = metaClass.getJavaClass();
        View view = new View(javaClass, name, false);
        if (View.LOCAL.equals(name)) {
            for (MetaProperty property : metaClass.getProperties()) {
                if (!property.getRange().isClass()
                        && !metadata.getTools().isSystem(property)
                        && metadata.getTools().isPersistent(property)) {
                    view.addProperty(property.getName());
                }
            }
        } else if (View.MINIMAL.equals(name)) {
            Collection<MetaProperty> metaProperties = metadata.getTools().getNamePatternProperties(metaClass, true);
            for (MetaProperty metaProperty : metaProperties) {
                view.addProperty(metaProperty.getName());
            }
        } else
            throw new UnsupportedOperationException("Unsupported default view: " + name);

        storeView(metaClass, view);
        return view;
    }

    public void deployViews(String resourceUrl) {
        if (!readFileNames.contains(resourceUrl)) {
            log.debug("Deploying views config: " + resourceUrl);

            InputStream stream = null;
            try {
                stream = resources.getResourceAsStream(resourceUrl);
                if (stream == null)
                    throw new IllegalStateException("Resource is not found: " + resourceUrl);
                deployViews(stream);
                readFileNames.add(resourceUrl);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
    }

    public void deployViews(InputStream xml) {
        deployViews(new InputStreamReader(xml));
    }

    public void deployViews(Reader xml) {
        SAXReader reader = new SAXReader();
        Document doc;
        try {
            doc = reader.read(xml);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        Element rootElem = doc.getRootElement();

        for (Element includeElem : (List<Element>) rootElem.elements("include")) {
            String file = includeElem.attributeValue("file");
            if (!StringUtils.isBlank(file))
                deployViews(file);
        }

        for (Element viewElem : (List<Element>) rootElem.elements("view")) {
            deployView(rootElem, viewElem);
        }
    }

    protected View retrieveView(MetaClass metaClass, String name, boolean deploying) {
        Map<String, View> views = storage.get(metaClass);
        View view = (views == null ? null : views.get(name));
        if (view == null && (name.equals(View.LOCAL) || name.equals(View.MINIMAL))) {
            view = deployDefaultView(metaClass, name);
        }
        return view;
    }

    public View deployView(Element rootElem, Element viewElem) {
        String viewName = viewElem.attributeValue("name");
        if (StringUtils.isBlank(viewName))
            throw new DevelopmentException("Invalid view definition: no 'name' attribute present");

        MetaClass metaClass;

        String entity = viewElem.attributeValue("entity");
        if (StringUtils.isBlank(entity)) {
            String className = viewElem.attributeValue("class");
            if (StringUtils.isBlank(className))
                throw new DevelopmentException("Invalid view definition: no 'entity' or 'class' attribute present");
            Class entityClass = ReflectionHelper.getClass(className);
            metaClass = metadata.getSession().getClassNN(entityClass);
        } else {
            metaClass = metadata.getSession().getClassNN(entity);
        }

        View v = retrieveView(metaClass, viewName, true);
        boolean overwrite = BooleanUtils.toBoolean(viewElem.attributeValue("overwrite"));
        if (v != null && !overwrite)
            return v;

        String systemProperties = viewElem.attributeValue("systemProperties");

        View view;
        String ancestor = viewElem.attributeValue("extends");
        if (ancestor != null) {
            View ancestorView = getAncestorView(metaClass, ancestor);

            boolean includeSystemProperties = systemProperties == null ?
                    ancestorView.isIncludeSystemProperties() : Boolean.valueOf(systemProperties);
            view = new View(ancestorView, metaClass.getJavaClass(), viewName, includeSystemProperties);
        } else {
            view = new View(metaClass.getJavaClass(), viewName, Boolean.valueOf(systemProperties));
        }
        loadView(rootElem, viewElem, view);
        storeView(metaClass, view);

        return view;
    }

    private View getAncestorView(MetaClass metaClass, String ancestor) {
        View ancestorView = retrieveView(metaClass, ancestor, false);
        if (ancestorView == null) {
            MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
            if (originalMetaClass != null)
                ancestorView = retrieveView(originalMetaClass, ancestor, false);
            if (ancestorView == null)
                throw new DevelopmentException("No ancestor view found: " + ancestor);
        }
        return ancestorView;
    }

    protected void loadView(Element rootElem, Element viewElem, View view) {
        final MetaClass metaClass = metadata.getSession().getClassNN(view.getEntityClass());
        final String viewName = view.getName();

        for (Element propElem : (List<Element>) viewElem.elements("property")) {
            String propertyName = propElem.attributeValue("name");

            MetaProperty metaProperty = metaClass.getProperty(propertyName);
            if (metaProperty == null)
                throw new DevelopmentException(
                        String.format("View %s/%s definition error: property %s doesn't exists", metaClass.getName(), viewName, propertyName)
                );

            View refView = null;
            String refViewName = propElem.attributeValue("view");

            MetaClass refMetaClass;
            Range range = metaProperty.getRange();
            if (range == null) {
                throw new RuntimeException("cannot find range for meta property: " + metaProperty);
            }

            final List<Element> propertyElements = propElem.elements("property");
            boolean inlineView = !propertyElements.isEmpty();

            if (refViewName != null && !inlineView) {

                if (!range.isClass())
                    throw new DevelopmentException(
                            String.format("View %s/%s definition error: property %s is not an entity", metaClass.getName(), viewName, propertyName)
                    );

                refMetaClass = getMetaClass(propElem, range);

                refView = retrieveView(refMetaClass, refViewName, false);
                if (refView == null) {
                    for (Element e : (List<Element>) rootElem.elements("view")) {
                        if (refMetaClass.equals(getMetaClass(e.attributeValue("entity"), e.attributeValue("class")))
                                && refViewName.equals(e.attributeValue("name"))) {
                            refView = deployView(rootElem, e);
                            break;
                        }
                    }

                    if (refView == null) {
                        MetaClass originalMetaClass = metadata.getExtendedEntities().getOriginalMetaClass(refMetaClass);
                        if (originalMetaClass != null)
                            refView = retrieveView(originalMetaClass, refViewName, false);
                    }

                    if (refView == null)
                        throw new DevelopmentException(
                                String.format(
                                        "View %s/%s definition error: unable to find/deploy referenced view %s/%s",
                                        metaClass.getName(), viewName, range.asClass().getName(), refViewName)
                        );
                }
            }
            if (range.isClass() && refView == null && inlineView) {
                // try to import anonymous views
                String ancestorViewName = propElem.attributeValue("view");
                if (ancestorViewName == null) {
                    refView = new View(range.asClass().getJavaClass());
                } else {
                    refMetaClass = getMetaClass(propElem, range);
                    View ancestorView = getAncestorView(refMetaClass, ancestorViewName);
                    refView = new View(ancestorView, range.asClass().getJavaClass(), "", true);
                }
                loadView(rootElem, propElem, refView);
            }
            boolean lazy = Boolean.valueOf(propElem.attributeValue("lazy"));
            view.addProperty(propertyName, refView, lazy);
        }
    }

    private MetaClass getMetaClass(String entityName, String entityClass) {
        if (entityName != null) {
            return metadata.getExtendedEntities().getEffectiveMetaClass(metadata.getClassNN(entityName));
        } else {
            return metadata.getExtendedEntities().getEffectiveMetaClass(ReflectionHelper.getClass(entityClass));
        }
    }

    private MetaClass getMetaClass(Element propElem, Range range) {
        MetaClass refMetaClass;
        String refEntityName = propElem.attributeValue("entity"); // this attribute is deprecated
        if (refEntityName == null) {
            refMetaClass = metadata.getExtendedEntities().getEffectiveMetaClass(range.asClass());
        } else {
            refMetaClass = metadata.getSession().getClass(refEntityName);
        }
        return refMetaClass;
    }

    public void storeView(MetaClass metaClass, View view) {
        Map<String, View> views = storage.get(metaClass);
        if (views == null) {
            views = new ConcurrentHashMap<>();
        }

        views.put(view.getName(), view);
        storage.put(metaClass, views);
    }

    public List<View> getAll() {
        checkInitialized();
        List<View> list = new ArrayList<>();
        for (Map<String, View> viewMap : storage.values()) {
            list.addAll(viewMap.values());
        }
        return list;
    }
}
