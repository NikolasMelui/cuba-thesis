/*
 * Copyright (c) 2010 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 02.06.2010 19:02:56
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

/**
 * Interface to be implemented by middleware cluster listeners. A cluster listener receives messages from other nodes
 * in the cluster.
 * @param <T>   type of message this listener receives
 */
public interface ClusterListener<T> {

    /**
     * Receive a message from other cluster nodes.
     * @param message   message instance
     */
    void receive(T message);

    /**
     * Get state of this cluster node to send it to other nodes.
     * "State" here means data that must be shared between cluster nodes.
     *
     * <p>This method is invoked by clustering implementation when a new node joins the cluster and wants to receive the
     * state from this active node.</p>
     *
     * @return  byte array containing the state
     */
    byte[] getState();

    /**
     * Set state of this cluster node receiving it from other active node.
     * "State" here means data that must be shared between cluster nodes.
     *
     * <p>This method is invoked by clustering implementation once when this node joins the cluster.
     *
     * @param state byte array containing the state
     */
    void setState(byte[] state);
}
