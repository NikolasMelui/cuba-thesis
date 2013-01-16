/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.categories;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.CategorizedEntity;
import com.haulmont.cuba.core.entity.Category;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.components.AbstractEditor;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.data.DataSupplier;
import com.haulmont.cuba.gui.data.ValueListener;
import org.apache.commons.lang.BooleanUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class CategoryEditor extends AbstractEditor<Category> {

    private Category category;
    private CheckBox cb;
    private DataSupplier dataSupplier;

    @Inject
    protected MetadataTools metadataTools;

    @Inject
    protected MessageTools messageTools;

    public void init(Map<String, Object> params) {
        dataSupplier = getDsContext().getDataService();
        cb = getComponent("isDefault");

    }

    @Override
    protected void postInit() {
        category = getItem();
        generateEntityTypeField();
        initCb();
    }

    private void generateEntityTypeField(){

        boolean hasValue = (category.getEntityType() == null) ? (false) : (true);

        LookupField categoryEntityTypeField = getComponent("entityType");
        Map<String,Object> options = new HashMap<String,Object>();
        MetaClass entityType = null;
        for (MetaClass metaClass : metadataTools.getAllPersistentMetaClasses()) {
            if (CategorizedEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                options.put(messageTools.getEntityCaption(metaClass), metaClass);
                if (hasValue && metaClass.getName().equals(category.getEntityType())) {
                    entityType = metaClass;
                }
            }
        }
        categoryEntityTypeField.setOptionsMap(options);
        categoryEntityTypeField.setValue(entityType);
        categoryEntityTypeField.addListener(new ValueListener(){
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                category.setEntityType(((MetaClass)value).getName());
            }
        });
    }

    private void initCb() {
        cb.setValue(BooleanUtils.isTrue(category.getIsDefault()));
        cb.addListener(new ValueListener() {
            @Override
            public void valueChanged(Object source, String property, Object prevValue, Object value) {
                if (Boolean.TRUE.equals(value)) {
                    LoadContext categoriesContext = new LoadContext(category.getClass());
                    LoadContext.Query query = categoriesContext.setQueryString("select c from sys$Category c where c.entityType= :entityType and not c.id=:id");
                    categoriesContext.setView("category.defaultEdit");
                    query.addParameter("entityType", category.getEntityType());
                    query.addParameter("id", category.getId());
                    List<Category> categories = dataSupplier.loadList(categoriesContext);
                    for(Category cat : categories){
                        cat.setIsDefault(false);
                    }
                    CommitContext commitContext = new CommitContext(categories);
                    dataSupplier.commit(commitContext);
                    category.setIsDefault(true);
                }
                else if(Boolean.FALSE.equals(value)){
                    category.setIsDefault(false);
                }
            }
        });
    }

}
