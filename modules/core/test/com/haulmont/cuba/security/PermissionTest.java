/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.security;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.HashDescriptor;
import com.haulmont.cuba.security.app.LoginWorker;
import com.haulmont.cuba.security.entity.*;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.security.global.UserSession;

import java.util.Locale;
import java.util.UUID;

/**
 * @author krivopustov
 * @version $Id$
 */
public class PermissionTest extends CubaTestCase {

    private static final String USER_NAME = "testUser";
    private static final String USER_PASSW = "testUser";
    private static final String PROFILE_NAME = "testProfile";
    private static final String PERM_TARGET_SCREEN = "w:sys$Server.browse";
    private static final String PERM_TARGET_ATTR = "sys$Server:address";

    private UUID role1Id, permission1Id, role2Id, permission2Id, userId, groupId,
            userRole1Id, userRole2Id;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Role role1 = new Role();
            role1Id = role1.getId();
            role1.setName("testRole1");
            em.persist(role1);

            Role role2 = new Role();
            role2Id = role2.getId();
            role2.setName("testRole2");
            em.persist(role2);

            Permission permission1 = new Permission();
            permission1Id = permission1.getId();
            permission1.setRole(role1);
            permission1.setType(PermissionType.SCREEN);
            permission1.setTarget(PERM_TARGET_SCREEN);
            permission1.setValue(0);
            em.persist(permission1);

            Permission permission2 = new Permission();
            permission2Id = permission2.getId();
            permission2.setRole(role2);
            permission2.setType(PermissionType.ENTITY_ATTR);
            permission2.setTarget(PERM_TARGET_ATTR);
            permission2.setValue(1);
            em.persist(permission2);

            Group group = new Group();
            groupId = group.getId();
            group.setName("testGroup");
            em.persist(group);

            User user = new User();
            userId = user.getId();
            user.setName(USER_NAME);
            user.setLogin(USER_NAME);

            HashDescriptor pwd = passwordEncryption.getPasswordHash(USER_PASSW);
            user.setPassword(pwd.toCredentialsString());

            user.setGroup(group);
            em.persist(user);

            UserRole userRole1 = new UserRole();
            userRole1Id = userRole1.getId();
            userRole1.setUser(user);
            userRole1.setRole(role1);
            em.persist(userRole1);

            UserRole userRole2 = new UserRole();
            userRole2Id = userRole2.getId();
            userRole2.setUser(user);
            userRole2.setRole(role2);
            em.persist(userRole2);

            tx.commit();
        } finally {
            tx.end();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Query q;

            q = em.createNativeQuery("delete from SEC_USER_ROLE where ID = ? or ID = ?");
            q.setParameter(1, userRole1Id.toString());
            q.setParameter(2, userRole2Id.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_USER where ID = ?");
            q.setParameter(1, userId.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_GROUP where ID = ?");
            q.setParameter(1, groupId.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_PERMISSION where ID = ? or ID = ?");
            q.setParameter(1, permission1Id.toString());
            q.setParameter(2, permission2Id.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_ROLE where ID = ? or ID = ?");
            q.setParameter(1, role1Id.toString());
            q.setParameter(2, role2Id.toString());
            q.executeUpdate();

            tx.commit();
        } finally {
            tx.end();
        }
        super.tearDown();
    }

    public void test() throws LoginException {
        LoginWorker lw = AppBeans.get(LoginWorker.NAME);

        UserSession userSession = lw.login(USER_NAME, passwordEncryption.getPlainHash(USER_PASSW), Locale.getDefault());
        assertNotNull(userSession);

        boolean permitted = userSession.isPermitted(PermissionType.SCREEN, PERM_TARGET_SCREEN);
        assertFalse(permitted);

        permitted = userSession.isPermitted(PermissionType.SCREEN, "some action");
        assertTrue(permitted); // permitted all if not explicitly denied

        permitted = userSession.isPermitted(PermissionType.ENTITY_ATTR, PERM_TARGET_ATTR);
        assertTrue(permitted); // READ access permitted

        permitted = userSession.isPermitted(PermissionType.ENTITY_ATTR, PERM_TARGET_ATTR, 2);
        assertFalse(permitted); // READ/WRITE access denied
    }
}
