/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.client.sys;

import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.core.sys.AbstractScripting;
import com.haulmont.cuba.core.sys.javacl.JavaClassLoader;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@ManagedBean(Scripting.NAME)
public class ScriptingClientImpl extends AbstractScripting {

    private String[] scriptEngineRoots;

    @Inject
    public ScriptingClientImpl(JavaClassLoader javaClassLoader, Configuration configuration) {
        super(javaClassLoader, configuration);
        scriptEngineRoots = new String[] {
                configuration.getConfig(GlobalConfig.class).getConfDir()
        };
    }

    @Override
    protected String[] getScriptEngineRootPath() {
        return scriptEngineRoots;
    }
}
