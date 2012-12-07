/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.jmx;

import com.haulmont.cuba.core.app.cache.ObjectsCacheController;
import com.haulmont.cuba.core.app.cache.ObjectsCacheInstance;
import com.haulmont.cuba.core.app.cache.ObjectsCacheManagerAPI;
import org.apache.commons.lang.StringUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("cuba_ObjectsCacheManagerMBean")
public class ObjectsCacheManager implements ObjectsCacheManagerMBean {

    @Inject
    protected ObjectsCacheManagerAPI manager;

    @Override
    public int getCacheCount() {
        return manager.getActiveInstances().size();
    }

    @Override
    public String printActiveCaches() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ObjectsCacheInstance cache : manager.getActiveInstances()) {
            int size = cache.getStatistics().getObjectsCount();
            stringBuilder.append(cache.getName()).append(String.format(" [%s]\n", size));
        }
        return stringBuilder.toString();
    }

    @Override
    public String printStatsByName(String cacheName) {
        if (StringUtils.isNotEmpty(cacheName)) {
            ObjectsCacheInstance cache = manager.getCache(cacheName);
            if (cache != null) {
                return cache.getStatistics().toString();
            } else
                return String.format("Couldn't find cache %s", cacheName);
        } else
            return "Empty name is not permitted";
    }

    @Override
    public String reloadByName(String cacheName) {
        if (StringUtils.isNotEmpty(cacheName)) {
            ObjectsCacheController controller = manager.getController(cacheName);
            if (controller != null) {
                controller.reloadCache();
                return String.format("Updated %s", cacheName);
            } else
                return String.format("Couldn't find controller for cache %s", cacheName);
        } else
            return "Empty name is not permitted";
    }}
