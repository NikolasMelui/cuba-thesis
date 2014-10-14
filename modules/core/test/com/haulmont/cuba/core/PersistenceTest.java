/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core;

import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.entity.Server;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PersistenceTest extends CubaTestCase {

    private UUID userId;

    protected void setUp() throws Exception {
        super.setUp();

        QueryRunner runner = new QueryRunner(persistence.getDataSource());
        runner.update("delete from SYS_SERVER");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            User user = new User();
            userId = user.getId();
            user.setName("testUser");
            user.setLogin("testLogin");
            user.setGroup(em.find(Group.class, UUID.fromString("0fa2b1a5-1d68-4d69-9fbd-dff348347f93")));
            em.persist(user);

            tx.commit();
        } finally {
            tx.end();
        }
    }

    protected void tearDown() throws Exception {
        deleteRecord("SEC_USER", userId);
        super.tearDown();
    }

    public void test() {
        UUID id;
        Server server;
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            assertNotNull(em);
            server = new Server();

            assertTrue(PersistenceHelper.isNew(server));
            assertFalse(PersistenceHelper.isManaged(server));
            assertFalse(PersistenceHelper.isDetached(server));

            id = server.getId();
            server.setName("localhost");
            server.setRunning(true);
            em.persist(server);
            assertTrue(PersistenceHelper.isNew(server));
            assertTrue(PersistenceHelper.isManaged(server));
            assertFalse(PersistenceHelper.isDetached(server));

            tx.commit();
        } finally {
            tx.end();
        }
        assertFalse(PersistenceHelper.isNew(server));
        assertFalse(PersistenceHelper.isManaged(server));
        assertTrue(PersistenceHelper.isDetached(server));


        tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            server = em.find(Server.class, id);
            assertNotNull(server);
            assertEquals(id, server.getId());

            assertFalse(PersistenceHelper.isNew(server));
            assertTrue(PersistenceHelper.isManaged(server));
            assertFalse(PersistenceHelper.isDetached(server));

            server.setRunning(false);

            tx.commit();
        } finally {
            tx.end();
        }
        assertFalse(PersistenceHelper.isNew(server));
        assertFalse(PersistenceHelper.isManaged(server));
        assertTrue(PersistenceHelper.isDetached(server));

        tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            server = em.merge(server);

            assertFalse(PersistenceHelper.isNew(server));
            assertTrue(PersistenceHelper.isManaged(server));
            assertFalse(PersistenceHelper.isDetached(server));

            tx.commit();
        } finally {
            tx.end();
        }


        tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            server = em.find(Server.class, id);
            assertNotNull(server);
            assertEquals(id, server.getId());

            em.remove(server);
            
            assertFalse(PersistenceHelper.isNew(server));
            assertTrue(PersistenceHelper.isManaged(server));  // is it correct?
            assertFalse(PersistenceHelper.isDetached(server));

            tx.commit();
        } finally {
            tx.end();
        }

        assertFalse(PersistenceHelper.isNew(server));
        assertFalse(PersistenceHelper.isManaged(server));
        assertTrue(PersistenceHelper.isDetached(server)); // is it correct?
    }

    private void raiseException() {
        throw new RuntimeException("test_ex");
    }

    public void testLoadReferencedEntity() throws Exception {
        User user = null;

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, false)
                            .addProperty("login")
            );
            Query q = em.createQuery("select u from sec$User u where u.id = ?1");
            q.setParameter(1, UUID.fromString("60885987-1b61-4247-94c7-dff348347f93"));
            List<User> list = q.getResultList();
            if (!list.isEmpty()) {
                user = list.get(0);

                UUID id = persistence.getTools().getReferenceId(user, "group");
                System.out.println(id);
            }

            tx.commit();
        } finally {
            tx.end();
        }

        try {
            persistence.getTools().getReferenceId(user, "group");
            fail();
        } catch (Exception e) {
            // ok
        }

        tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, false)
                            .addProperty("login")
                            .addProperty("group",
                                    new View(Group.class, false).addProperty("id")
                            )
            );
            Query q = em.createQuery("select u from sec$User u where u.id = ?1");
            q.setParameter(1, UUID.fromString("60885987-1b61-4247-94c7-dff348347f93"));
            List<User> list = q.getResultList();
            if (!list.isEmpty()) {
                user = list.get(0);

                UUID id = persistence.getTools().getReferenceId(user, "group");
                System.out.println(id);
            }

            tx.commit();
        } finally {
            tx.end();
        }

        /*
         * test Persistence.getReferenceId() when field value is NULL
         * (ticket XXX)
         */
        // create user without group
        User userWithoutGroup = new User();
        userWithoutGroup.setLogin("ForeverAlone");

        // save to DB
        tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.persist(userWithoutGroup);
            tx.commit();
        } finally {
            tx.end();
        }

        // test method
        try {
            tx = persistence.createTransaction();
            try {
                EntityManager em = persistence.getEntityManager();
                em.setView(new View(User.class).addProperty("login"));
                User reloadedUser = em.find(User.class, userWithoutGroup.getId());

                UUID groupId = persistence.getTools().getReferenceId(reloadedUser, "group");

                assertNull(groupId);

                tx.commit();
            } finally {
                tx.end();
            }
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
        }
    }

    public void testLoadByCombinedView() throws Exception {
        User user;
        Transaction tx = persistence.createTransaction();
        try {
            // load by single view

            EntityManager em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, false)
                            .addProperty("login")
            );
            user = em.find(User.class, UUID.fromString("60885987-1b61-4247-94c7-dff348347f93"));

            assertTrue(persistence.getTools().isLoaded(user, "login"));
            assertFalse(persistence.getTools().isLoaded(user, "name"));

            tx.commitRetaining();

            // load by combined view

            em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, false)
                            .addProperty("login")
            );
            em.addView(
                    new View(User.class, false)
                            .addProperty("name")
            );
            user = em.find(User.class, UUID.fromString("60885987-1b61-4247-94c7-dff348347f93"));

            assertTrue(persistence.getTools().isLoaded(user, "login"));
            assertTrue(persistence.getTools().isLoaded(user, "name"));

            tx.commitRetaining();

            // load by complex combined view

            em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, false)
                            .addProperty("login")
            );
            em.addView(
                    new View(User.class, false)
                            .addProperty("group", new View(Group.class).addProperty("name"))
            );
            user = em.find(User.class, UUID.fromString("60885987-1b61-4247-94c7-dff348347f93"));

            assertTrue(persistence.getTools().isLoaded(user, "login"));
            assertFalse(persistence.getTools().isLoaded(user, "name"));
            assertTrue(persistence.getTools().isLoaded(user, "group"));
            assertTrue(persistence.getTools().isLoaded(user.getGroup(), "name"));

            tx.commit();
        } finally {
            tx.end();
        }
    }

    public void testMergeNotLoaded() throws Exception {
        User user;
        Group group;

        Transaction tx = persistence.createTransaction();
        try {
            User transientUser = new User();
            transientUser.setId(userId);
            transientUser.setName("testUser1");

            EntityManager em = persistence.getEntityManager();
            em.merge(transientUser);

            tx.commitRetaining();

            em = persistence.getEntityManager();
            user = em.find(User.class, userId);
            assertNotNull(user);
            group = user.getGroup();
        } finally {
            tx.end();
        }

        assertEquals(userId, user.getId());
        assertEquals("testUser1", user.getName());
        assertEquals("testLogin", user.getLogin());
        assertNotNull(group);
    }

    public void testDirtyFields() throws Exception {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            assertNotNull(em);
            Server server = new Server();
            UUID id = server.getId();
            server.setName("localhost");
            server.setRunning(true);
            em.persist(server);

            tx.commitRetaining();

            em = persistence.getEntityManager();
            server = em.find(Server.class, id);
            assertNotNull(server);
            server.setData("testData");

            Set<String> dirtyFields = persistence.getTools().getDirtyFields(server);
            assertTrue(dirtyFields.contains("data"));

            tx.commit();
        } finally {
            tx.end();
        }
    }

    /**
     * OpenJPA silently ignores setting null in nullable=false attribute.
     */
    public void testNonNullAttribute() throws Exception {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            User user = em.find(User.class, userId);
            assertNotNull(user);
            user.setLogin(null);
            user.setName(null);
            tx.commitRetaining();

            em = persistence.getEntityManager();
            user = em.find(User.class, userId);
            assertNotNull(user);
            assertNotNull(user.getLogin()); // null was not saved
            assertNull(user.getName());     // null was saved

            tx.commit();
        } finally {
            tx.end();
        }

    }
}
