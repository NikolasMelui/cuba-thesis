/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Vasiliy Fontanenko
 * Created: 12.10.2010 19:21:08
 *
 * $Id$
 */
package com.haulmont.cuba.report.formatters.oo;

import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.uno.XComponentContext;
import ooo.connector.BootstrapSocketConnector;

public class OOOConnector {
    public static OOOConnection createConnection(String openOfficePath) throws BootstrapException {
        BootstrapSocketConnector bsc = new BootstrapSocketConnector(openOfficePath);
        XComponentContext xComponentContext = bsc.connect();
        OOOConnection oooConnection = new OOOConnection(xComponentContext, bsc);
        return oooConnection;
    }
}
