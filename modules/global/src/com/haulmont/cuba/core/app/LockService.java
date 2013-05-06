/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.global.LockInfo;

import java.util.List;

/**
 * Service interface for pessimistic locking.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface LockService {

    String NAME = "cuba_LockService";

    /**
     * Try to lock object
     * @param name locking object name
     * @param id locking object ID
     * @return <li>null in case of successful lock,
     * <li>{@link com.haulmont.cuba.core.global.LockNotSupported} instance in case of locking is not configured for this object,
     * <li>{@link LockInfo} instance in case of this object is already locked by someone
     */
    LockInfo lock(String name, String id);

    /**
     * Unlock object
     * @param name locking object name
     * @param id locking object ID
     */
    void unlock(String name, String id);

    /**
     * Get locking status for particular object
     * @param name locking object name
     * @param id locking object ID
     * @return <li>null in case of no lock,
     * <li>{@link com.haulmont.cuba.core.global.LockNotSupported} instance in case of locking is not configured for this object,
     * <li>{@link LockInfo} instance in case of this object is locked by someone
     */
    LockInfo getLockInfo(String name, String id);

    /**
     * List of current locks
     */
    List<LockInfo> getCurrentLocks();

    void reloadConfiguration();
}
