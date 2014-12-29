/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core;

import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;

import java.util.List;
import java.util.UUID;

public class SoftDeleteTest extends CubaTestCase
{
    private UUID groupId;
    private UUID userId;
    private UUID role2Id;
    private UUID userRole1Id;
    private UUID userRole2Id;

    protected void setUp() throws Exception {
        super.setUp();

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Group group = new Group();
            groupId = group.getId();
            group.setName("testGroup");
            em.persist(group);

            User user = new User();
            userId = user.getId();
            user.setName("testUser");
            user.setLogin("testLogin");
            user.setGroup(group);
            em.persist(user);

            Role role1 = em.find(Role.class, UUID.fromString("0c018061-b26f-4de2-a5be-dff348347f93"));

            UserRole userRole1 = new UserRole();
            userRole1Id = userRole1.getId();
            userRole1.setUser(user);
            userRole1.setRole(role1);
            em.persist(userRole1);

            Role role2 = new Role();
            role2Id = role2.getId();
            role2.setName("roleToBeDeleted");
            em.persist(role2);

            UserRole userRole2 = new UserRole();
            userRole2Id = userRole2.getId();
            userRole2.setUser(user);
            userRole2.setRole(role2);
            em.persist(userRole2);
            
            tx.commitRetaining();

            em = persistence.getEntityManager();

            UserRole ur = em.find(UserRole.class, userRole2Id);
            em.remove(ur);

            Role r = em.find(Role.class, role2Id);
            em.remove(r);

            tx.commit();
        } finally {
            tx.end();
        }
    }

    protected void tearDown() throws Exception {
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Query q;

            q = em.createNativeQuery("delete from SEC_USER_ROLE where ID = ? or ID = ?");
            q.setParameter(1, userRole1Id.toString());
            q.setParameter(2, userRole2Id.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_ROLE where ID = ?");
            q.setParameter(1, role2Id.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_USER where ID = ?");
            q.setParameter(1, userId.toString());
            q.executeUpdate();

            q = em.createNativeQuery("delete from SEC_GROUP where ID = ?");
            q.setParameter(1, groupId.toString());
            q.executeUpdate();

            tx.commit();
        } finally {
            tx.end();
        }
        super.tearDown();
    }

    public void testNormalMode() {
        System.out.println("===================== BEGIN testNormalMode =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Role role = em.find(Role.class, role2Id);
            assertNull(role);

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testNormalMode =====================");
    }

    public void testCleanupMode() {
        System.out.println("===================== BEGIN testCleanupMode =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.setSoftDeletion(false);

            Role role = em.find(Role.class, role2Id);
            assertNotNull(role);
            assertTrue(role.isDeleted());

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testCleanupMode =====================");
    }

    public void testOneToMany() {
        System.out.println("===================== BEGIN testOneToMany =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            em.setView(
                    new View(User.class, "testView")
                            .addProperty("name")
                            .addProperty("login")
                            .addProperty("userRoles",
                                    new View(UserRole.class, "testView")
                                            .addProperty("role",
                                                    new View(Role.class, "testView")
                                                            .addProperty("name")))
            );
            User user = em.find(User.class, userId);

            List<UserRole> userRoles = user.getUserRoles();
            assertEquals(1, userRoles.size());
            for (UserRole ur : userRoles) {
                assertNotNull(ur.getRole());
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testOneToMany =====================");
    }

    public void testOneToManyLazy() {
        System.out.println("===================== BEGIN testOneToManyLazy =====================");
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            User user = em.find(User.class, userId);

            List<UserRole> userRoles = user.getUserRoles();
            assertEquals(1, userRoles.size());
            for (UserRole ur : userRoles) {
                assertNotNull(ur.getRole());
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testOneToManyLazy =====================");
    }

    public void testOneToMany_CleanupMode() {
        System.out.println("===================== BEGIN testOneToMany_CleanupMode =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.setSoftDeletion(false);

            em.setView(
                    new View(User.class, "testView")
                            .addProperty("name")
                            .addProperty("login")
                            .addProperty("userRoles",
                                new View(UserRole.class, "testView")
                                    .addProperty("role",
                                        new View(Role.class, "testView")
                                            .addProperty("name")))
            );
            User user = em.find(User.class, userId);

            List<UserRole> userRoles = user.getUserRoles();
            assertEquals(2, userRoles.size());
            for (UserRole ur : userRoles) {
                assertNotNull(ur.getRole());
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testOneToMany_CleanupMode =====================");
    }

    public void testOneToMany_Query() {
        System.out.println("===================== BEGIN testOneToMany_Query =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Query q = em.createQuery("select u from sec$User u where u.id = ?1");
            q.setParameter(1, userId);
            User user = (User) q.getSingleResult();

            List<UserRole> userRoles = user.getUserRoles();
            assertEquals(1, userRoles.size());
            for (UserRole ur : userRoles) {
                assertNotNull(ur.getRole());
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testOneToMany_Query =====================");
    }

    public void testOneToMany_JoinFetchQuery() {
        System.out.println("===================== BEGIN testOneToMany_JoinFetchQuery =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();

            Query q = em.createQuery("select u from sec$User u join fetch u.userRoles where u.id = ?1");
            q.setParameter(1, userId);
            User user = (User) q.getSingleResult();

            List<UserRole> userRoles = user.getUserRoles();
            assertEquals(1, userRoles.size());
            for (UserRole ur : userRoles) {
                assertNotNull(ur.getRole());
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testOneToMany_JoinFetchQuery =====================");
    }

    public void testQuery() {
        System.out.println("===================== BEGIN testQuery =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            Query query = em.createQuery("select r from sec$Role r where r.name = ?1");
            query.setParameter(1, "roleToBeDeleted");

            List<Role> list = query.getResultList();
            assertTrue(list.isEmpty());

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testQuery =====================");
    }

    public void testQuery_CleanupMode() {
        System.out.println("===================== BEGIN testQuery_CleanupMode =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            em.setSoftDeletion(false);
            Query query = em.createQuery("select r from sec$Role r where r.name = ?1");
            query.setParameter(1, "roleToBeDeleted");

            List<Role> list = query.getResultList();
            assertTrue(!list.isEmpty());

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testQuery_CleanupMode =====================");
    }

    public void testQueryWithoutConditions() {
        System.out.println("===================== BEGIN testQueryWithoutConditions =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            Query query = em.createQuery("select r from sec$Role r");

            List<Role> list = query.getResultList();
            for (Role role : list) {
                if (role.getId().equals(role2Id)) {
                    fail();
                }
            }

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testQueryWithoutConditions =====================");
    }

    public void testRemoveNotManaged() {
        System.out.println("===================== BEGIN testRemoveNotManaged =====================");

        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            UserRole userRole = em.find(UserRole.class, userRole1Id);

            tx.commitRetaining();

            em = persistence.getEntityManager();
            em.remove(userRole);

            tx.commitRetaining();

            em = persistence.getEntityManager();
            UserRole deletedUserRole = em.find(UserRole.class, userRole1Id);
            assertNull(deletedUserRole);

            tx.commit();
        } finally {
            tx.end();
        }
        System.out.println("===================== END testRemoveNotManaged =====================");
    }

    public void testGlobalSoftDeleteSwitch() throws Exception {
        EntityManager em;
        Transaction tx;
        tx = persistence.createTransaction();
        try {
            em = persistence.getEntityManager();
            assertTrue(em.isSoftDeletion());
            tx.commit();
        } finally {
            tx.end();
        }

        persistence.setSoftDeletion(false);
        try {
            tx = persistence.createTransaction();
            try {
                em = persistence.getEntityManager();
                assertFalse(em.isSoftDeletion());

                persistence.setSoftDeletion(true);
                em = persistence.getEntityManager();
                assertFalse(em.isSoftDeletion()); // persistence.setSoftDeletion affects only EMs created in a new tx

                tx.commit();
            } finally {
                tx.end();
            }
        } finally {
            persistence.setSoftDeletion(true);
        }

        tx = persistence.createTransaction();
        try {
            em = persistence.getEntityManager();
            assertTrue(em.isSoftDeletion());
            tx.commit();
        } finally {
            tx.end();
        }
    }
}
