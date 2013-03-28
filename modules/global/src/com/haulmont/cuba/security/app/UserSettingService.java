/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.security.app;

import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.security.entity.User;

/**
 * Service providing current user settings functionality:
 * an application can save/load some "setting" (plain or XML string) for current user.
 * <p/>It is ususally used by UI forms and components.
 *
 * @author krivopustov
 * @version $Id$
 */
public interface UserSettingService {

    String NAME = "cuba_UserSettingService";

    /** Load settings for the current user and null client type. Returns null if no such setting found. */
    String loadSetting(String name);

    /** Load settings for the current user. Returns null if no such setting found. */
    String loadSetting(ClientType clientType, String name);

    /** Save settings for the current user and null client type */
    void saveSetting(String name, String value);

    /** Save settings for the current user */
    void saveSetting(ClientType clientType, String name, String value);

    /** Copy user settings to another user */
    void copySettings(User fromUser, User toUser);
}
