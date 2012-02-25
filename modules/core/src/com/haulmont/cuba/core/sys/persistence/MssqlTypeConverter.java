/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.persistence;

import java.sql.*;
import java.util.Date;
import java.util.UUID;

/**
 * @author artamonov
 * @version $Id$
 */
public class MssqlTypeConverter implements DbTypeConverter {
    @Override
    public Object getJavaObject(ResultSet resultSet, int columnIndex) throws SQLException {
        Object value;

        ResultSetMetaData metaData = resultSet.getMetaData();

        if ((columnIndex > metaData.getColumnCount()) || (columnIndex <= 0))
            throw new IndexOutOfBoundsException("Column index out of bound");

        int sqlType = metaData.getColumnType(columnIndex);
        String typeName = metaData.getColumnTypeName(columnIndex);

        switch (sqlType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
                if ("uniqueidentifier".equals(typeName))
                    value = UUID.fromString(resultSet.getString(columnIndex));
                else
                    value = resultSet.getObject(columnIndex);
                break;

            default:
                value = resultSet.getObject(columnIndex);
                break;
        }

        return value;
    }

    @Override
    public Object getSqlObject(Object value) {
        if (value instanceof Date)
            return new Timestamp(((Date) value).getTime());
        else if (value instanceof UUID)
            return value.toString();
        return value;
    }
}
