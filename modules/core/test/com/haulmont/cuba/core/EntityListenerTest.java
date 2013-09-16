/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core;

import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.entity.Server;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.listener.AfterDeleteEntityListener;
import com.haulmont.cuba.core.listener.AfterInsertEntityListener;
import com.haulmont.cuba.core.listener.AfterUpdateEntityListener;
import com.haulmont.cuba.core.sys.listener.EntityListenerManager;

import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class EntityListenerTest extends CubaTestCase
{
    public static class TestListener implements
            AfterInsertEntityListener<Server>,
            AfterUpdateEntityListener<Server>,
            AfterDeleteEntityListener<Server>
    {
        protected Persistence persistence = AppBeans.get(Persistence.class);

        public void onAfterInsert(Server entity) {
            System.out.println("onAfterInsert " + entity);
        }

        public void onAfterUpdate(Server entity) {
            System.out.println("onAfterUpdate " + entity);

            Set<String> dirtyFields = persistence.getTools().getDirtyFields(entity);
            System.out.println(dirtyFields);

            EntityManager em = persistence.getEntityManager();
            Query q = em.createQuery("select max(s.createTs) from sys$Server s");
            Date maxDate = (Date) q.getSingleResult();
            System.out.println(maxDate);

            // JPA update queries don't work: reentrant flush error
//            Query q = em.createQuery("update sys$Server s set s.name = :name where s.id = :id");
//            Query q = em.createNativeQuery("update SYS_SERVER set NAME = ?1 where ID = ?2");
//            q.setParameter(1, "some other");
//            q.setParameter(2, entity.getId());
//            q.executeUpdate();

            QueryRunner runner = new QueryRunner(persistence.getDataSource());
            try {
                runner.update("update SYS_SERVER set NAME = ? where ID = ?", new Object[] {"some other", entity.getId()});
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void onAfterDelete(Server entity) {
            System.out.println("onAfterDelete " + entity);
        }
    }

    public void test() {
        AppBeans.get(EntityListenerManager.class).addListener(Server.class, TestListener.class);

        UUID id, id1;
        Transaction tx = persistence.createTransaction();
        try {
            EntityManager em = persistence.getEntityManager();
            assertNotNull(em);
            Server server = new Server();
            id = server.getId();
            server.setName("localhost");
            server.setRunning(true);
            em.persist(server);

            Server server1 = new Server();
            id1 = server1.getId();
            server1.setName("localhost");
            server1.setRunning(true);
            em.persist(server1);

            tx.commitRetaining();

            em = persistence.getEntityManager();
            server = em.find(Server.class, id);
            server.setName(server.getName() + " - " + new Date());

            tx.commitRetaining();

            em = persistence.getEntityManager();
            server = em.find(Server.class, id1);
            em.remove(server);

            tx.commit();
        } catch (Exception e) {
            tx.end();
        }


    }


}
