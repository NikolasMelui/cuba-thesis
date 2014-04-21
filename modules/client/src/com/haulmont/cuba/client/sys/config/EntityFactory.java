/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.client.sys.config;

import com.haulmont.cuba.core.app.DataService;
import com.haulmont.cuba.core.config.type.TypeFactory;
import com.haulmont.cuba.core.global.EntityLoadInfo;
import com.haulmont.cuba.core.global.LoadContext;
import org.apache.commons.lang.StringUtils;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean(TypeFactory.ENTITY_FACTORY_BEAN_NAME)
public class EntityFactory extends TypeFactory {

    @Inject
    private DataService ds;

    @Override
    public Object build(String string) {
        if (StringUtils.isBlank(string))
            return null;
        EntityLoadInfo info = EntityLoadInfo.parse(string);
        if (info == null)
            throw new IllegalArgumentException("Invalid entity info: " + string);

        LoadContext ctx = new LoadContext(info.getMetaClass()).setId(info.getId());
        if (info.getViewName() != null)
            ctx.setView(info.getViewName());
        return ds.load(ctx);
    }
}
