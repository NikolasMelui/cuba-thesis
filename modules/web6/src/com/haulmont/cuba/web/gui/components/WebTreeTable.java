/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.TreeTable;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.gui.data.TreeTableDatasource;
import com.haulmont.cuba.web.gui.data.CollectionDsWrapper;
import com.haulmont.cuba.web.gui.data.HierarchicalDsWrapper;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.data.TreeTableContainer;
import com.vaadin.data.Item;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.terminal.Resource;

import java.util.*;

/**
 * @author abramov
 * @version $Id$
 */
public class WebTreeTable
    extends
        WebAbstractTable<com.haulmont.cuba.web.toolkit.ui.TreeTable>
    implements
        TreeTable {

    protected String hierarchyProperty;
    
    public WebTreeTable() {
        component = createTreeTableComponent();
        initComponent(component);
    }

    protected com.haulmont.cuba.web.toolkit.ui.TreeTable createTreeTableComponent() {
        return new MyTreeTable();
    }

    @Override
    public void setRowHeaderMode(RowHeaderMode rowHeaderMode) {
        // Row Header mode for TreeTable ignored
    }

    @Override
    public void setDatasource(CollectionDatasource datasource) {
        super.setDatasource(datasource);
        this.hierarchyProperty = ((HierarchicalDatasource) datasource).getHierarchyPropertyName();

        // if showProperty is null, the Tree will use itemId.toString
        MetaProperty metaProperty = hierarchyProperty == null ? null : datasource.getMetaClass().getProperty(hierarchyProperty);
        component.setItemCaptionPropertyId(metaProperty);
    }

    @Override
    public String getHierarchyProperty() {
        return hierarchyProperty;
    }

    @Override
    public void setDatasource(HierarchicalDatasource datasource) {
        setDatasource((CollectionDatasource) datasource);
    }

    @Override
    protected CollectionDsWrapper createContainerDatasource(
            CollectionDatasource datasource, Collection<MetaPropertyPath> columns) {
        return new TreeTableDsWrapper((HierarchicalDatasource) datasource);
    }

    @Override
    public void expandAll() {
        component.expandAll();
    }

    @Override
    public void expand(Object itemId) {
        component.setExpanded(itemId);
    }

    @Override
    public void collapseAll() {
        component.collapseAll();
    }

    @Override
    public void collapse(Object itemId) {
        component.setCollapsed(itemId);
    }

    @Override
    public int getLevel(Object itemId) {
        return component.getLevel(itemId);
    }

    @Override
    public boolean isExpanded(Object itemId) {
        return component.isExpanded(itemId);
    }

    protected class TreeTableDsWrapper
            extends HierarchicalDsWrapper
            implements TreeTableContainer, com.vaadin.data.Container.Sortable, AggregationContainer
    {
        protected boolean treeTableDatasource;

        private List<Object> aggregationProperties = null;

        public TreeTableDsWrapper(HierarchicalDatasource datasource) {
            super(datasource);
            treeTableDatasource  = (datasource instanceof TreeTableDatasource);
        }

        @Override
        protected void createProperties(View view, MetaClass metaClass) {
            if (columns.isEmpty()) {
                super.createProperties(view, metaClass);
            } else {
                for (Map.Entry<Object, Column> entry : columns.entrySet()) {
                    if (entry.getKey() instanceof MetaPropertyPath) {
                        properties.add((MetaPropertyPath) entry.getKey());
                    }
                }
            }
        }

        @Override
        protected ItemWrapper createItemWrapper(Object item) {
            return new ItemWrapper(item, properties) {
                @Override
                protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
                    return new TablePropertyWrapper(item, propertyPath);
                }
            };
        }

        @Override
        public boolean isCaption(Object itemId) {
            return treeTableDatasource && ((TreeTableDatasource) datasource).isCaption(itemId);
        }

        @Override
        public String getCaption(Object itemId) {
            if (treeTableDatasource) {
                return ((TreeTableDatasource) datasource).getCaption(itemId);
            }
            return null;
        }

        @Override
        public boolean setCaption(Object itemId, String caption) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLevel(Object itemId) {
            return getItemLevel(itemId);
        }

        protected int getItemLevel(Object itemId) {
            Object parentId;
            if ((parentId = getParent(itemId)) == null) {
                return 0;
            }
            return getItemLevel(parentId) + 1;
        }

        @Override
        public void sort(Object[] propertyId, boolean[] ascending) {
            List<CollectionDatasource.Sortable.SortInfo> infos = new ArrayList<CollectionDatasource.Sortable.SortInfo>();
            for (int i = 0; i < propertyId.length; i++) {
                final MetaPropertyPath propertyPath = (MetaPropertyPath) propertyId[i];

                final CollectionDatasource.Sortable.SortInfo<MetaPropertyPath> info =
                        new CollectionDatasource.Sortable.SortInfo<MetaPropertyPath>();
                info.setPropertyPath(propertyPath);
                info.setOrder(ascending[i] ? CollectionDatasource.Sortable.Order.ASC : CollectionDatasource.Sortable.Order.DESC);

                infos.add(info);
            }
            ((CollectionDatasource.Sortable) datasource).sort(infos.toArray(new CollectionDatasource.Sortable.SortInfo[infos.size()]));
        }

        @Override
        public Collection getSortableContainerPropertyIds() {
            return properties;
        }

        @Override
        public Object nextItemId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).nextItemId(itemId);
        }

        @Override
        public Object prevItemId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).prevItemId(itemId);
        }

        @Override
        public Object firstItemId() {
            return ((CollectionDatasource.Sortable) datasource).firstItemId();
        }

        @Override
        public Object lastItemId() {
            return ((CollectionDatasource.Sortable) datasource).lastItemId();
        }

        @Override
        public boolean isFirstId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).isFirstId(itemId);
        }

        @Override
        public boolean isLastId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).isLastId(itemId);
        }

        @Override
        public Object addItemAfter(Object previousItemId) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Item addItemAfter(Object previousItemId, Object newItemId) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection getAggregationPropertyIds() {
            if (aggregationProperties != null) {
                return Collections.unmodifiableList(aggregationProperties);
            }
            return Collections.emptyList();
        }

        @Override
        public Type getContainerPropertyAggregation(Object propertyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addContainerPropertyAggregation(Object propertyId, Type type) {
            if (aggregationProperties == null) {
                aggregationProperties = new LinkedList<Object>();
            } else if (aggregationProperties.contains(propertyId)) {
                throw new IllegalStateException("Such aggregation property is already exists");
            }
            aggregationProperties.add(propertyId);
        }

        @Override
        public void removeContainerPropertyAggregation(Object propertyId) {
            if (aggregationProperties != null) {
                aggregationProperties.remove(propertyId);
                if (aggregationProperties.isEmpty()) {
                    aggregationProperties = null;
                }
            }
        }

        @Override
        public Map<Object, Object> aggregate(Context context) {
            return __aggregate(this, context);
        }
    }

    protected class MyTreeTable extends com.haulmont.cuba.web.toolkit.ui.TreeTable {
        @Override
        public Resource getItemIcon(Object itemId) {
            return WebTreeTable.this.getItemIcon(itemId);
        }

        @Override
        protected boolean changeVariables(Map<String, Object> variables) {
            boolean b = super.changeVariables(variables);
            b = handleSpecificVariables(variables) || b;
            return b;
        }

        @Override
        public void paintContent(PaintTarget target) throws PaintException {
            super.paintContent(target);
            paintSpecificContent(target);
        }
    }
}