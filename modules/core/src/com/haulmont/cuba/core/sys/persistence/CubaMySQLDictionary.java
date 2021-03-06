/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.sys.persistence;

import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Join;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * @author krivopustov
 * @version $Id$
 */
public class CubaMySQLDictionary extends MySQLDictionary {
    @Override
    public SQLBuffer toTraditionalJoin(Join join) {
        return DBDictionaryUtils.toTraditionalJoin(this, join);
    }

    @Override
    protected SQLBuffer getWhere(Select sel, boolean forUpdate) {
        return DBDictionaryUtils.getWhere(this, sel, forUpdate, false);
    }
}