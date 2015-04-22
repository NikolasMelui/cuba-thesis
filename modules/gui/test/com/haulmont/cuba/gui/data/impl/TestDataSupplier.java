/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.data.impl;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.data.DataSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* @author krivopustov
* @version $Id$
*/
public class TestDataSupplier implements DataSupplier {

    public interface CommitValidator {
        void validate(CommitContext context);
    }

    int commitCount;

    CommitValidator commitValidator;

    @Override
    public Set<Entity> commit(CommitContext context) {
        commitCount++;

        if (commitValidator != null)
            commitValidator.validate(context);

        Set<Entity> result = new HashSet<>();
        for (Entity entity : context.getCommitInstances()) {
            result.add(entity);
        }
        return result;
    }

    @Override
    public <A extends Entity> A load(LoadContext context) {
        return null;
    }

    @Override
    @Nonnull
    public <A extends Entity> List<A> loadList(LoadContext context) {
        return Collections.emptyList();
    }

    @Override
    public <A extends Entity> A newInstance(MetaClass metaClass) {
        return null;
    }

    @Override
    public <A extends Entity> A reload(A entity, String viewName) {
        return null;
    }

    @Override
    public <A extends Entity> A reload(A entity, View view) {
        return null;
    }

    @Override
    public <A extends Entity> A reload(A entity, View view, MetaClass metaClass) {
        return null;
    }

    @Override
    public <A extends Entity> A reload(A entity, View view, MetaClass metaClass, boolean useSecurityConstraints) {
        return null;
    }

    @Override
    public <A extends Entity> A reload(A entity, View view, @Nullable MetaClass metaClass, boolean useSecurityConstraints, boolean loadRuntimeProperties) {
        return null;
    }

    @Override
    public <A extends Entity> A commit(A entity, View view) {
        return null;
    }

    @Override
    public <A extends Entity> A commit(A instance) {
        return commit(instance, null);
    }

    @Override
    public void remove(Entity entity) {
    }
}