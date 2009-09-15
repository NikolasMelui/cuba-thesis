/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: DEGTYARJOV EUGENIY
 * Created: 11.09.2009 15:13:58
 *
 * $Id$
 */

package com.haulmont.cuba.security.entity;

import com.haulmont.cuba.core.entity.AbstractNotPersistentEntity;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;

import java.util.UUID;
import java.util.Date;

@MetaClass(name = "sec$UserSessionEntity")
public class UserSessionEntity extends AbstractNotPersistentEntity {
    private static final long serialVersionUID = 7730031482721158275L;
    @MetaProperty
    private UUID id;
    @MetaProperty
    private String login;
    @MetaProperty
    private String userName;
    @MetaProperty
    private Date since;
    @MetaProperty
    private Date lastUsedTs;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }

    public Date getLastUsedTs() {
        return lastUsedTs;
    }

    public void setLastUsedTs(Date lastUsedTs) {
        this.lastUsedTs = lastUsedTs;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String toString() {
        return "User session: id-"+id+" login-"+login+" user-"+userName+" since-"+since+" last-"+lastUsedTs;
    }
}
