/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.datastruct.Node;
import com.haulmont.bali.datastruct.Tree;
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
import com.haulmont.cuba.web.toolkit.ui.CubaTreeTable;
import com.vaadin.data.Item;
import com.vaadin.server.Resource;

import java.util.*;

public class WebTreeTable extends WebAbstractTable<CubaTreeTable> implements TreeTable {

    protected String hierarchyProperty;

    public WebTreeTable() {
        component = createTreeTableComponent();
        initComponent(component);
    }

    protected CubaTreeTable createTreeTableComponent() {
        return new CubaTreeTableExt();
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
    public void setIconProvider(IconProvider iconProvider) {
        this.iconProvider = iconProvider;
        component.refreshRowCache();
    }

    @Override
    public void expandAll() {
        if (getDatasource() instanceof HierarchicalDatasource) {
            HierarchicalDatasource datasource = (HierarchicalDatasource) getDatasource();
            Object nullParentItemId = new Object();

            Map<Object, Object> parentsMapping = getParentsMapping(datasource, nullParentItemId);

            Tree<Object> itemIdsTree = toItemIdsTree(parentsMapping, nullParentItemId);

            List<Object> preOrder = toContainerPreOrder(itemIdsTree);
            List<Object> openItems = getItemIdsWithChildren(parentsMapping, nullParentItemId);
            List<Object> collapsedItemIds = getCollapsedItemIds();

            component.expandAllHierarchical(collapsedItemIds, preOrder, openItems);
        } else {
            component.expandAll();
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<Object, Object> getParentsMapping(HierarchicalDatasource ds, Object nullParentItemId) {
        Map<Object, Object> parentsMapping = new LinkedHashMap<>();

        Collection<Object> itemIds = ds.getItemIds();

        for (Object itemId : itemIds) {
            Object parentId = ds.getParent(itemId);

            if (itemIds.contains(parentId)) {
                parentsMapping.put(itemId, parentId);
            } else {
                parentsMapping.put(itemId, nullParentItemId);
            }
        }

        return parentsMapping;
    }

    protected List<Object> getItemIdsWithChildren(Map<Object, Object> parentsMapping, Object nullParentItemId) {
        Set<Object> parents = new LinkedHashSet<>(parentsMapping.values());
        parents.remove(nullParentItemId);
        return new ArrayList<>(parents);
    }

    protected Tree<Object> toItemIdsTree(Map<Object, Object> parentsMapping, Object nullParentItemId) {
        Map<Object, Node<Object>> nodeMapping = new LinkedHashMap<>();

        for (Object itemId : parentsMapping.keySet()) {
            Node<Object> node = new Node<>(itemId);
            nodeMapping.put(itemId, node);
        }

        List<Node<Object>> roots = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : parentsMapping.entrySet()) {
            Object itemId = entry.getKey();
            Object parentId = entry.getValue();

            Node<Object> itemNode = nodeMapping.get(itemId);

            if (parentId == nullParentItemId) {
                roots.add(itemNode);
            } else {
                Node<Object> parentNode = nodeMapping.get(parentId);
                parentNode.addChild(itemNode);
            }
        }

        return new Tree<>(roots);
    }

    protected List<Object> toContainerPreOrder(Tree<Object> itemIdsTree) {
        List<Node<Object>> nodes = itemIdsTree.toList();

        List<Object> nodesData = new ArrayList<>();
        for (Node<Object> node : nodes) {
            nodesData.add(node.data);
        }

        return nodesData;
    }

    protected List<Object> getCollapsedItemIds() {
        if (datasource == null) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        Collection<Object> itemIds = getDatasource().getItemIds();

        List<Object> collapsed = new ArrayList<>();
        for (Object itemId : itemIds) {
            if (component.isCollapsed(itemId)) {
                collapsed.add(itemId);
            }
        }

        return collapsed;
    }

    @Override
    public void expand(Object itemId) {
        if (component.containsId(itemId)) {
            component.expandItemWithParents(itemId);
        }
    }

    @Override
    public void collapseAll() {
        component.collapseAllHierarchical();
    }

    @Override
    public void collapse(Object itemId) {
        if (component.containsId(itemId)) {
            component.collapseItemRecursively(itemId);
        }
    }

    @Override
    public void expandUpTo(int level) {
        component.expandUpTo(level);
    }

    @Override
    public int getLevel(Object itemId) {
        return component.getLevel(itemId);
    }

    @Override
    public boolean isExpanded(Object itemId) {
        if (component.containsId(itemId)) {
            return !component.isCollapsed(itemId);
        }
        return false;
    }

    protected class TreeTableDsWrapper
            extends HierarchicalDsWrapper
            implements TreeTableContainer, com.vaadin.data.Container.Sortable, AggregationContainer {

        protected boolean treeTableDatasource;

        protected List<Object> aggregationProperties = null;

        public TreeTableDsWrapper(HierarchicalDatasource datasource) {
            super(datasource);
            treeTableDatasource = (datasource instanceof TreeTableDatasource);
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
            return new ItemWrapper(item, datasource.getMetaClass(), properties) {
                @Override
                protected PropertyWrapper createPropertyWrapper(Object item, MetaPropertyPath propertyPath) {
                    return new TablePropertyWrapper(item, propertyPath);
                }
            };
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean isCaption(Object itemId) {
            return treeTableDatasource && ((TreeTableDatasource) datasource).isCaption(itemId);
        }

        @SuppressWarnings("unchecked")
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
            List<CollectionDatasource.Sortable.SortInfo> infos = new ArrayList<>();
            for (int i = 0; i < propertyId.length; i++) {
                final MetaPropertyPath propertyPath = (MetaPropertyPath) propertyId[i];

                final CollectionDatasource.Sortable.SortInfo<MetaPropertyPath> info =
                        new CollectionDatasource.Sortable.SortInfo<>();
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

        @SuppressWarnings("unchecked")
        @Override
        public Object nextItemId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).nextItemId(itemId);
        }

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
        @Override
        public boolean isFirstId(Object itemId) {
            return ((CollectionDatasource.Sortable) datasource).isFirstId(itemId);
        }

        @SuppressWarnings("unchecked")
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
                aggregationProperties = new LinkedList<>();
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

        @Override
        public void resetSortOrder() {
            if (datasource instanceof CollectionDatasource.Sortable) {
                ((CollectionDatasource.Sortable) datasource).resetSortOrder();
            }
        }
    }

    protected class CubaTreeTableExt extends CubaTreeTable {
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
    }
}