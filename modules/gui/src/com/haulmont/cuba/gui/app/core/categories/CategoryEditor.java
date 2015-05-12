/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.app.core.categories;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Category;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.ValueListener;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author devyatkin
 * @version $Id$
 */
public class CategoryEditor extends AbstractEditor<Category> {

    protected Category category;
    protected CheckBox cb;
    protected DataSupplier dataSupplier;

    @Inject
    protected Metadata metadata;

    @Inject
    protected MessageTools messageTools;

    @Override
    public void init(Map<String, Object> params) {
        dataSupplier = getDsContext().getDataSupplier();
        cb = getComponent("isDefault");
    }

    @Override
    protected void postInit() {
        category = getItem();
        generateEntityTypeField();
        initCb();
    }

    protected void generateEntityTypeField() {
        boolean hasValue = category.getEntityType() != null;

        LookupField categoryEntityTypeField = getComponent("entityType");
        Map<String, Object> options = new TreeMap<>();//the map sorts meta classes by the string key
        MetaClass entityType = null;
        for (MetaClass metaClass : metadata.getTools().getAllPersistentMetaClasses()) {
            options.put(messageTools.getDetailedEntityCaption(metaClass), metaClass);
            if (hasValue && metaClass.getName().equals(category.getEntityType())) {
                entityType = metaClass;
            }
        }
        categoryEntityTypeField.setOptionsMap(options);
        categoryEntityTypeField.setValue(entityType);
        categoryEntityTypeField.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                MetaClass metaClass = (MetaClass) value;
                MetaClass originalClass = metadata.getExtendedEntities().getOriginalMetaClass(metaClass);
                category.setEntityType(originalClass == null ? metaClass.getName() : originalClass.getName());
            }
        });
    }

    protected void initCb() {
        cb.setValue(BooleanUtils.isTrue(category.getIsDefault()));
        cb.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (Boolean.TRUE.equals(value)) {
                    LoadContext categoriesContext = new LoadContext(category.getClass());
                    LoadContext.Query query = categoriesContext.setQueryString("select c from sys$Category c where c.entityType= :entityType and not c.id=:id");
                    categoriesContext.setView("category.defaultEdit");
                    query.setParameter("entityType", category.getEntityType());
                    query.setParameter("id", category.getId());
                    List<Category> categories = dataSupplier.loadList(categoriesContext);
                    for (Category cat : categories) {
                        cat.setIsDefault(false);
                    }
                    CommitContext commitContext = new CommitContext(categories);
                    dataSupplier.commit(commitContext);
                    category.setIsDefault(true);
                } else if (Boolean.FALSE.equals(value)) {
                    category.setIsDefault(false);
                }
            }
        });
    }
}