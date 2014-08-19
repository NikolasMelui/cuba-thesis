/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.app.script;

import com.haulmont.cuba.core.entity.Entity;

/**
 * @author degtyarjov
 * @version $Id$
 */
public interface ScriptGenerationService {
    String NAME = "cuba_ScriptGenerationService";

    /**
     * Generates an SQL-insert query for the entity
     */
    String generateInsertScript(Entity entity);

    /**
     * Generates an SQL-update query for the entity
     */
    String generateUpdateScript(Entity entity);

    String generateSelectScript(Entity entity);
}
