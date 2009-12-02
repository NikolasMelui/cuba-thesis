/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 10.12.2008 15:44:32
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

import javax.ejb.Local;

/**
 * Local interface to {@link com.haulmont.cuba.core.app.ResourceRepositoryServiceBean}
 */
@Local
public interface ResourceRepositoryService
{
    String JNDI_NAME = "cuba/core/ResourceRepositoryService";

    /**
     * Loads resource into cache as String and returns it
     * @param name resource file name relative to resources root (jboss/server/default/conf)
     * @return String resource
     */
    String getResAsString(String name);
}
