/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.app;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * @author krivopustov
 * @version $Id$
 */
@Service(DataService.NAME)
public class DataServiceBean implements DataService {

    @Inject
    protected DataManager dataManager;

    @Override
    public Set<Entity> commit(CommitContext context) {
        return dataManager.commit(context);
    }

    @Override
    @Nullable
    public <A extends Entity> A load(LoadContext context) {
        return dataManager.load(context);
    }

    @Override
    public <A extends Entity> List<A> loadList(LoadContext context) {
        return dataManager.loadList(context);
    }
}