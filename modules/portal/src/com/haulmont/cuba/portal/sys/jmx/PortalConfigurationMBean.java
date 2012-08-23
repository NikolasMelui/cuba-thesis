/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.sys.jmx;

import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

/**
 * @author krivopustov
 * @version $Id$
 */
public interface PortalConfigurationMBean {

    String printAppProperties();

    @ManagedOperationParameters({@ManagedOperationParameter(name = "prefix", description = "")})
    String printAppProperties(String prefix);

    @ManagedOperationParameters({@ManagedOperationParameter(name = "name", description = "")})
    String getAppProperty(String name);

    @ManagedOperationParameters({
            @ManagedOperationParameter(name = "name", description = ""),
            @ManagedOperationParameter(name = "value", description = "")
    })
    String setAppProperty(String name, String value);
}
