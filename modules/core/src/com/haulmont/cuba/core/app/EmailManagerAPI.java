/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.entity.SendingMessage;

import java.util.List;

/**
 * Provides asynchronous email sending facility for {@link EmailerAPI}.
 *
 * @author ovchinnikov
 * @version $Id$
 */
public interface EmailManagerAPI {

    String NAME = "cuba_EmailManager";

    /**
     * Add emails to a queue by persisting the {@link SendingMessage} instances.
     *
     * @param sendingMessageList just created SendingMessage instances
     * @return  the same instances persisted and detached
     */
    List<SendingMessage> addEmailsToQueue(List<SendingMessage> sendingMessageList);

    /**
     * Send emails added to the queue.
     * <p/> This method should be called periodically from a scheduled task.
     *
     * @return short message describing how many emails were sent, or error message
     */
    String queueEmailsToSend();
}
