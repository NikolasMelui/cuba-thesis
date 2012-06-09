/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.client.sys.config;

import com.haulmont.cuba.core.app.ConfigStorageService;
import com.haulmont.cuba.core.config.ConfigPersister;
import com.haulmont.cuba.core.config.SourceType;
import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class ConfigPersisterClientImpl implements ConfigPersister {

    private Map<String, String> cache = new ConcurrentHashMap<String, String>();

    private volatile boolean cacheLoaded;

    private final Log log = LogFactory.getLog(ConfigPersisterClientImpl.class);

    public String getProperty(SourceType sourceType, String name) {
        log.trace("Getting property '" + name + "', source=" + sourceType.name());
        String value;
        switch (sourceType) {
            case SYSTEM:
                value = System.getProperty(name);
                break;
            case APP:
                value = AppContext.getProperty(name);
                break;
            case DATABASE:
                loadCache();
                value = cache.get(name);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported config source type: " + sourceType);
        }
        return value;
    }

    private void loadCache() {
        if (!cacheLoaded) {
            synchronized (this) {
                if (!cacheLoaded) {
                    Map<String, String> properties = getConfigStorage().getDbProperties();
                    cache.clear();
                    cache.putAll(properties);
                    cacheLoaded = true;
                }
            }
        }
    }

    public void setProperty(SourceType sourceType, String name, String value) {
        log.debug("Setting property '" + name + "' to '" + value + "', source=" + sourceType.name());
        switch (sourceType) {
            case SYSTEM:
                System.setProperty(name, value);
                break;
            case APP:
                AppContext.setProperty(name, value);
                break;
            case DATABASE:
                cache.put(name, value);
                getConfigStorage().setDbProperty(name, value);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported config source type: " + sourceType);
        }
    }

    private ConfigStorageService getConfigStorage() {
        return (ConfigStorageService) AppContext.getBean(ConfigStorageService.NAME);
    }
}
