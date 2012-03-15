/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.persistence;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Join;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLServerDictionary;
import org.apache.openjpa.jdbc.sql.Select;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class CubaMssqlDictionary extends SQLServerDictionary {

    public SQLBuffer toTraditionalJoin(Join join) {
        return DBDictionaryUtils.toTraditionalJoin(this, join);
    }

    protected SQLBuffer getWhere(Select sel, boolean forUpdate) {
        return DBDictionaryUtils.getWhere(this, sel, forUpdate, true);
    }

    @Override
    public void setUnknown(PreparedStatement stmnt, int idx, Object val, Column col) throws SQLException {
        if (val instanceof UUID) {
            stmnt.setObject(idx, val.toString());
        } else {
            super.setUnknown(stmnt, idx, val, col);
        }
    }
}
