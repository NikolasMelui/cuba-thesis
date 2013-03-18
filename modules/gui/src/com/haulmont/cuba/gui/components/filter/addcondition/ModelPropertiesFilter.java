/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.components.filter.addcondition;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.global.UserSession;

/**
 * @author artamonov
 * @version $Id$
 */
public class ModelPropertiesFilter {

    private UserSession userSession;
    private final MessageTools messageTools;
    private final MetadataTools metadataTools;

    public ModelPropertiesFilter() {
        userSession = AppBeans.get(UserSessionSource.class).getUserSession();
        messageTools = AppBeans.get(MessageTools.class);
        metadataTools = AppBeans.get(MetadataTools.class);
    }

    public boolean isPropertyFilterAllowed(MetaProperty property) {
        if (userSession.isEntityAttrPermitted(property.getDomain(), property.getName(), EntityAttrAccess.VIEW)) {
            // exclude system level attributes
            Boolean systemLevel = metadataTools.isSystemLevel(property);
            if (!systemLevel) {
                // exclude not localized properties (they are usually not for end user) and ToMany
                if (messageTools.hasPropertyCaption(property) && !property.getRange().getCardinality().isMany()) {
                    return true;
                }
            }
        }
        return false;
    }
}