/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 17.03.2009 12:42:08
 *
 * $Id$
 */
package com.haulmont.cuba.security;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.sys.TestUserSessionSource;
import com.haulmont.cuba.security.app.UserSettingService;

public class UserSettingServiceTest extends CubaTestCase
{
    private UserSettingService uss;

    protected void setUp() throws Exception {
        super.setUp();
        uss = Locator.lookup(UserSettingService.NAME);
    }

    protected void tearDown() throws Exception {
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query q = em.createNativeQuery("delete from SEC_USER_SETTING where USER_ID = ?");
            q.setParameter(1, TestUserSessionSource.USER_ID);
            q.executeUpdate();
            tx.commit();
        } finally {
            tx.end();
        }
        super.tearDown();
    }

    public void test() {
        String val = uss.loadSetting(ClientType.WEB, "test-setting");
        assertNull(val);

        uss.saveSetting(ClientType.WEB, "test-setting", "test-value");

        val = uss.loadSetting(ClientType.WEB, "test-setting");
        assertEquals("test-value", val);

        val = uss.loadSetting("test-setting");
        assertNull(val);

        uss.saveSetting("test-setting", "test-value-1");

        val = uss.loadSetting("test-setting");
        assertEquals("test-value-1", val);

        val = uss.loadSetting(ClientType.WEB, "test-setting");
        assertEquals("test-value", val);
    }
}
