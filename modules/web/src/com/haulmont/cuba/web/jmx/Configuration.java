/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.jmx;

import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.lang.text.StrBuilder;

import javax.annotation.ManagedBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@ManagedBean("cuba_web_Configuration")
public class Configuration implements ConfigurationMBean {

    @Override
    public String printAppProperties() {
        return printAppProperties(null);
    }

    @Override
    public String printAppProperties(String prefix) {
        List<String> list = new ArrayList<String>();
        for (String name : AppContext.getPropertyNames()) {
            if (prefix == null || name.startsWith(prefix)) {
                list.add(name + "=" + AppContext.getProperty(name));
            }
        }
        Collections.sort(list);
        return new StrBuilder().appendWithSeparators(list, "\n").toString();
    }

    @Override
    public String getAppProperty(String name) {
        return name + "=" + AppContext.getProperty(name);
    }

    @Override
    public String setAppProperty(String name, String value) {
        AppContext.setProperty(name, value);
        return "Property " + name + " set to " + value;
    }
}
