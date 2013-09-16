/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.testsupport;

import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Connection;

public class TestTransaction implements Transaction {

    private int status = Status.STATUS_ACTIVE;

    private List<Synchronization> syncs = new ArrayList<Synchronization>();

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        if (status != Status.STATUS_ACTIVE)
            throw new SystemException("Unable to commit: invalid tx status: " + status);

        Connection conn = null;
        for (Synchronization sync : syncs) {
            sync.beforeCompletion();
        }
        if (conn != null) {
            try {
                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        status = Status.STATUS_COMMITTED;
        for (Synchronization sync : syncs) {
            sync.afterCompletion(Status.STATUS_COMMITTED);
        }
    }

    public void rollback() throws IllegalStateException, SystemException {
        if (status != Status.STATUS_ACTIVE && status != Status.STATUS_MARKED_ROLLBACK)
            throw new SystemException("Unable to rollback: invalid tx status: " + status);

        status = Status.STATUS_ROLLEDBACK;
        for (Synchronization sync : syncs) {
            sync.afterCompletion(Status.STATUS_ROLLEDBACK);
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    public int getStatus() throws SystemException {
        return status;
    }

    public boolean enlistResource(XAResource xaResource) throws RollbackException, IllegalStateException, SystemException {
        return false;
    }

    public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException, SystemException {
        return false;
    }

    public void registerSynchronization(Synchronization synchronization) throws RollbackException, IllegalStateException, SystemException {
        syncs.add(synchronization);
    }

    @Override
    public String toString() {
        return super.toString() + ", status=" + status;
    }
}
