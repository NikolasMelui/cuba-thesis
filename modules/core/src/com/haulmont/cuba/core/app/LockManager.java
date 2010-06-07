/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 19.02.2010 10:36:50
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.LockDescriptor;
import com.haulmont.cuba.core.global.LockInfo;
import com.haulmont.cuba.core.global.LockNotSupported;
import com.haulmont.cuba.core.global.TimeProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ManagedBean(LockManagerAPI.NAME)
public class LockManager implements LockManagerAPI, LockManagerMBean, ClusterListener<LockInfo> {

    private static class LockKey {
        
        private final String name;
        private final String id;

        private LockKey(String name, String id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LockKey key = (LockKey) o;

            if (id != null ? !id.equals(key.id) : key.id != null) return false;
            if (!name.equals(key.name)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
    }

    private Log log = LogFactory.getLog(LockManager.class);

    private volatile Map<String, LockDescriptor> config;

    private Map<LockKey, LockInfo> locks = new ConcurrentHashMap<LockKey, LockInfo>();

    private ClusterManagerAPI clusterManager;

    @Inject
    public void setClusterManager(ClusterManagerAPI clusterManager) {
        this.clusterManager = clusterManager;
        this.clusterManager.addListener(LockInfo.class, this);
    }

    private Map<String, LockDescriptor> getConfig() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = new ConcurrentHashMap<String, LockDescriptor>();
                    Transaction tx = Locator.createTransaction();
                    try {
                        EntityManager em = PersistenceProvider.getEntityManager();
                        Query q = em.createQuery("select d from core$LockDescriptor d");
                        List<LockDescriptor> list = q.getResultList();
                        for (LockDescriptor ld : list) {
                            config.put(ld.getName(), ld);
                        }
                        tx.commit();
                    } finally {
                        tx.end();
                    }
                }
            }
        }
        return config;
    }

    public LockInfo lock(String name, String id) {
        LockKey key = new LockKey(name, id);

        LockInfo lockInfo = locks.get(key);
        if (lockInfo != null) {
            log.debug("Already locked: " + lockInfo);
            return lockInfo;
        }

        LockDescriptor ld = getConfig().get(name);
        if (ld == null) {
            return new LockNotSupported();
        }

        lockInfo = new LockInfo(SecurityProvider.currentUserSession().getCurrentOrSubstitutedUser(), name, id);
        locks.put(key, lockInfo);
        log.debug("Locked " + name + "/" + id);

        clusterManager.send(lockInfo);

        return null;
    }

    public void unlock(String name, String id) {
        LockInfo lockInfo = locks.remove(new LockKey(name, id));
        if (lockInfo != null) {
            log.debug("Unlocked " + name + "/" + id);

            clusterManager.send(new LockInfo(null, name, id));
        }
    }

    public LockInfo getLockInfo(String name, String id) {
        LockDescriptor ld = getConfig().get(name);
        if (ld == null) {
            return new LockNotSupported();
        }

        LockInfo lockInfo = locks.get(new LockKey(name, id));
        return lockInfo;
    }

    public List<LockInfo> getCurrentLocks() {
        return new ArrayList(locks.values());
    }

    public void expireLocks() {
        log.debug("Expiring locks");
        ArrayList<LockKey> list = new ArrayList(locks.keySet());
        for (LockKey key : list) {
            LockInfo lockInfo = locks.get(key);
            if (lockInfo != null) {
                LockDescriptor ld = getConfig().get(key.name);
                Integer timeoutSec = ld.getTimeoutSec();
                if (timeoutSec != null && timeoutSec > 0) {
                    Date since = lockInfo.getSince();
                    if (since.getTime() + timeoutSec * 1000 < TimeProvider.currentTimestamp().getTime()) {
                        log.debug("Lock " + key.name + "/" + key.id + " expired");
                        locks.remove(key);
                    }
                }
            }
        }
    }

    public int getLockCount() {
        return locks.size();
    }

    public String showLocks() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<LockKey, LockInfo> entry : locks.entrySet()) {
            sb.append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public void reloadConfiguration() {
        config = null;
    }

    public void receive(LockInfo message) {
        LockKey key = new LockKey(message.getEntityName(), message.getEntityId());
        if (message.getUser() != null) {
            LockInfo lockInfo = locks.get(key);
            if (lockInfo == null || lockInfo.getSince().before(message.getSince())) {
                locks.put(key, message);
            }
        } else {
            locks.remove(key);
        }
    }

    public byte[] getState() {
        List<LockInfo> list = new ArrayList<LockInfo>(locks.values());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(list);
        } catch (IOException e) {
            log.error("Error serializing LockInfo list", e);
            return new byte[0];
        }
        return bos.toByteArray();
    }

    public void setState(byte[] state) {
        if (state == null || state.length == 0)
            return;

        List<LockInfo> list;
        ByteArrayInputStream bis = new ByteArrayInputStream(state);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            list = (List<LockInfo>) ois.readObject();
        } catch (Exception e) {
            log.error("Error deserializing LockInfo list", e);
            return;
        }

        for (LockInfo lockInfo : list) {
            receive(lockInfo);
        }
    }
}
