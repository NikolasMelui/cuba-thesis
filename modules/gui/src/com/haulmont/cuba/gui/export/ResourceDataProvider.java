/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.export;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.core.global.ScriptingProvider;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

/**
 * DataProvider for application resources
 *
 * @author artamonov
 * @version $Id$
 */
public class ResourceDataProvider implements ExportDataProvider {

    private String resourcePath;

    public ResourceDataProvider(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public InputStream provide() throws ResourceException {
        return AppBeans.get(Resources.class).getResourceAsStream(resourcePath);
    }

    @Override
    public void close() {
        // do nothing
    }
}