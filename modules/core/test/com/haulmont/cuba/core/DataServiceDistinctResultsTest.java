/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core;

import com.google.common.collect.Iterables;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.security.entity.Group;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.lang.StringUtils;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * @author krivopustov
 * @version $Id$
 */
public class DataServiceDistinctResultsTest extends CubaTestCase {

    public static final int QTY = 17;

    public static final String QUERY =
            "select u from sec$User u left join u.userRoles r where u.group.id = :groupId order by u.loginLowerCase";

    public static final String DISTINCT_QUERY =
            "select distinct u from sec$User u left join u.userRoles r where u.group.id = :groupId order by u.loginLowerCase";

    private UUID groupId;
    private UUID role1Id;
    private UUID role2Id;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Transaction tx = PersistenceProvider.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();

            Group group = new Group();
            groupId = group.getId();
            group.setName("testGroup");
            em.persist(group);

            Role role1 = new Role();
            role1Id = role1.getId();
            role1.setName("role1");
            em.persist(role1);

            Role role2 = new Role();
            role2Id = role2.getId();
            role2.setName("role2");
            em.persist(role2);

            for (int i = 0; i < QTY; i++) {
                User user = new User();
                user.setName("user" + StringUtils.leftPad(String.valueOf(i), 2, '0'));
                user.setLogin(user.getName());
                user.setGroup(group);
                em.persist(user);

                UserRole userRole1 = new UserRole();
                userRole1.setUser(user);
                userRole1.setRole(role1);
                em.persist(userRole1);

                UserRole userRole2 = new UserRole();
                userRole2.setUser(user);
                userRole2.setRole(role2);
                em.persist(userRole2);
            }

            tx.commit();
        } finally {
            tx.end();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        Transaction tx = PersistenceProvider.createTransaction();
        try {
            QueryRunner runner = new QueryRunner(Locator.getDataSource());
            try {
                String sql = "delete from SEC_USER_ROLE where ROLE_ID = '" + role1Id.toString() + "'";
                runner.update(sql);

                sql = "delete from SEC_USER_ROLE where ROLE_ID = '" + role2Id.toString() + "'";
                runner.update(sql);

                sql = "delete from SEC_ROLE where ID = '" + role1Id.toString() + "'";
                runner.update(sql);

                sql = "delete from SEC_ROLE where ID = '" + role2Id.toString() + "'";
                runner.update(sql);

                sql = "delete from SEC_USER where GROUP_ID = '" + groupId.toString() + "'";
                runner.update(sql);

                sql = "delete from SEC_GROUP where ID = '" + groupId.toString() + "'";
                runner.update(sql);

            } catch (SQLException e) {
                e.printStackTrace();
            }
            tx.commit();
        } finally {
            tx.end();
        }
        super.tearDown();
    }

    public void testDistinctResults() {
        checkSetup();

        LinkedHashSet<User> set;

        ConfigProvider.getConfig(ServerConfig.class).setInMemoryDistinct(false);

        set = load(0, 10, QUERY);
        assertEquals(5, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user04", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(0, 10, DISTINCT_QUERY);
        assertEquals(10, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user09", Iterables.getLast(set, null).getLoginLowerCase());

        ConfigProvider.getConfig(ServerConfig.class).setInMemoryDistinct(true);

        set = load(0, 10, QUERY);
        assertEquals(5, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user04", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(0, 10, DISTINCT_QUERY);
        assertEquals(10, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user09", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(0, 20, DISTINCT_QUERY);
        assertEquals(17, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user16", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(0, 17, DISTINCT_QUERY);
        assertEquals(17, set.size());
        assertEquals("user00", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user16", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(5, 5, DISTINCT_QUERY);
        assertEquals(5, set.size());
        assertEquals("user05", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user09", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(10, 5, DISTINCT_QUERY);
        assertEquals(5, set.size());
        assertEquals("user10", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user14", Iterables.getLast(set, null).getLoginLowerCase());

        set = load(15, 5, DISTINCT_QUERY);
        assertEquals(2, set.size());
        assertEquals("user15", Iterables.getFirst(set, null).getLoginLowerCase());
        assertEquals("user16", Iterables.getLast(set, null).getLoginLowerCase());

    }

    private LinkedHashSet<User> load(int firstResult, int maxResults, String queryString) {
        DataService ds = Locator.lookup(DataService.NAME);
        LoadContext lc = new LoadContext(User.class);
        LoadContext.Query q = lc.setQueryString(queryString);
        q.addParameter("groupId", groupId);
        q.setFirstResult(firstResult);
        q.setMaxResults(maxResults);
        List list = ds.loadList(lc);
        return new LinkedHashSet(list);
    }

    private void checkSetup() {
        Transaction tx = PersistenceProvider.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query query = em.createQuery("select u from sec$User u left join u.userRoles r where u.group.id = ?1");
            query.setParameter(1, groupId);
            List list = query.getResultList();
            tx.commit();
            assertEquals(QTY * 2, list.size());
        } finally {
            tx.end();
        }
    }
}
