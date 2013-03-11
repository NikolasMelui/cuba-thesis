/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.persistence;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.SQLBuffer;

/**
 * @author degtyarjov
 * @version $Id$
 */
public class CubaOracleDictionary extends OracleDictionary {

    @Override
    public String getTypeName(Column col) {
        return super.getTypeName(col);
    }

    @Override
    public void appendCast(SQLBuffer buf, Object val, int type) {
        // Convert the cast function: "CAST({0} AS {1})"
        int firstParam = castFunction.indexOf("{0}");
        String pre = castFunction.substring(0, firstParam); // "CAST("
        String mid = castFunction.substring(firstParam + 3);
        int secondParam = mid.indexOf("{1}");
        String post;
        if (secondParam > -1) {
            post = mid.substring(secondParam + 3); // ")"
            mid = mid.substring(0, secondParam); // " AS "
        } else
            post = "";

        buf.append(pre);
        if (val instanceof FilterValue)
            ((FilterValue) val).appendTo(buf);
        else if (val instanceof SQLBuffer)
            buf.append(((SQLBuffer) val));
        else
            buf.append(val.toString());
        buf.append(mid);
        buf.append(getTypeName(type).replaceAll("\\{0\\}", ""));//just a workaround because getTypeName returns NUMBER{0} for some types which is probably a bug
        appendLength(buf, type);
        buf.append(post);
    }
}
