/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.global;

import java.util.UUID;

/**
 * Global interface to create UUIDs.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface UuidSource {

    String NAME = "cuba_UuidSource";

    UUID createUuid();
}
