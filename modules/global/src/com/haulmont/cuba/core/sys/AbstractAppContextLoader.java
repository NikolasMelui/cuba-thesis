/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.entity.BaseUuidEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * Base class for {@link AppContext} loaders.
 *
 * @author krivopustov
 * @version $Id$
 */
public class AbstractAppContextLoader {

    public static final String SPRING_CONTEXT_CONFIG = "cuba.springContextConfig";

    protected void afterInitAppProperties() {
        BaseUuidEntity.allowSetNotLoadedAttributes =
                Boolean.valueOf(AppContext.getProperty("cuba.allowSetNotLoadedAttributes"));
    }

    protected void beforeInitAppContext() {
    }

    protected void initAppContext() {
        String configProperty = AppContext.getProperty(SPRING_CONTEXT_CONFIG);
        if (StringUtils.isBlank(configProperty)) {
            throw new IllegalStateException("Missing " + SPRING_CONTEXT_CONFIG + " application property");
        }

        StrTokenizer tokenizer = new StrTokenizer(configProperty);
        String[] locations = tokenizer.getTokenArray();
        replaceLocationsFromConf(locations);

        CubaClassPathXmlApplicationContext appContext = new CubaClassPathXmlApplicationContext();

        appContext.setConfigLocations(locations);
        appContext.setValidating(false);
        appContext.refresh();

        AppContext.setApplicationContext(appContext);
    }

    protected void replaceLocationsFromConf(String[] locations) {
        String confDirProp = AppContext.getProperty("cuba.confDir");
        if (confDirProp == null)
            throw new IllegalStateException("cuba.confDir app property is not set");
        File confDir = new File(confDirProp);
        for (int i = 0; i < locations.length; i++) {
            String location = locations[i];
            if (ResourceUtils.isUrl(location))
                continue;
            if (location.startsWith("/"))
                location = location.substring(1);
            File file = new File(confDir, location);
            if (file.exists()) {
                locations[i] = file.toURI().toString();
            }
        }
    }

    protected void afterInitAppContext() {
    }
}