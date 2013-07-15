/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.Range;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.data.*;
import com.haulmont.cuba.web.gui.data.CollectionDsWrapper;
import com.haulmont.cuba.web.gui.data.ItemWrapper;
import com.haulmont.cuba.web.gui.data.PropertyWrapper;
import com.haulmont.cuba.web.gui.data.SortableCollectionDsWrapper;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.haulmont.cuba.web.toolkit.data.GroupTableContainer;
import com.haulmont.cuba.web.toolkit.ui.CubaGroupTable;
import com.vaadin.data.Item;
import com.vaadin.server.Resource;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.*;

/**
 * @author gorodnov
 * @version $Id$
 */
public class WebGroupTable extends WebAbstractTable<CubaGroupTable>
        implements GroupTable, Component.Wrapper {

    protected Map<Table.Column, GroupAggregationCells> groupAggregationCells = null;

    protected boolean rerender = true;

    public WebGroupTable() {
        component = new CubaGroupTable() {
            @Override
            @SuppressWarnings({"unchecked"})
            public Resource getItemIcon(Object itemId) {
                if (styleProvider != null) {
                    final Entity item = datasource.getItem(itemId);
                    final String resURL = styleProvider.getItemIcon(item);

                    return resURL == null ? null : WebComponentsHelper.getResource(resURL);
                } else {
                    return null;
                }
            }

            @Override
            protected String formatGroupPropertyValue(Object groupId, Object groupValue) {
                if (groupPropertyValueFormatter == null) {
                    String caption = null;
                    if (groupId != null) {
                        Datatype<Object> datatype = (Datatype<Object>) Datatypes.get(groupValue.getClass());
                        if (datatype != null)
                            caption = datatype.format(groupValue);
                    }
                    if (caption == null)
                        caption = groupValue == null ? "" : groupValue.toString();

                    return caption;
                } else {
                    return groupPropertyValueFormatter.format(groupId, groupValue);
                }
            }

            @Override
            public void groupBy(Object[] properties) {
                groupBy(properties, rerender);
            }
        };
        initComponent(component);

        component.setGroupPropertyValueFormatter(new AggregatableGroupPropertyValueFormatter());
    }

    @Override
    protected void setEditableColumns(List<MetaPropertyPath> editableColumns) {
        component.setEditableColumns(editableColumns.toArray());
    }

    @Override
    public boolean saveSettings(Element element) {
        super.saveSettings(element);

        Element groupPropertiesElement = element.element("groupProperties");
        if (groupPropertiesElement != null) {
            element.remove(groupPropertiesElement);
        }

        groupPropertiesElement = element.addElement("groupProperties");

        final Collection<?> groupProperties = component.getGroupProperties();
        for (Object groupProperty : groupProperties) {
            final Element groupPropertyElement = groupPropertiesElement.addElement("property");
            groupPropertyElement.addAttribute("id", groupProperty.toString());
        }

        return true;
    }

    @Override
    public void applySettings(Element element) {
        super.applySettings(element);

        final Element groupPropertiesElement = element.element("groupProperties");
        if (groupPropertiesElement != null) {
            final List elements = groupPropertiesElement.elements("property");
            final List<MetaPropertyPath> properties = new ArrayList<>(elements.size());
            for (final Object o : elements) {
                final MetaPropertyPath property = datasource.getMetaClass().getPropertyPath(((Element) o).attributeValue("id")
                );
                properties.add(property);
            }
            groupBy(properties.toArray());
        }
    }

    @Override
    protected CollectionDatasourceListener createAggregationDatasourceListener() {
        return new GroupAggregationDatasourceListener();
    }

    @Override
    protected CollectionDsWrapper createContainerDatasource(CollectionDatasource datasource, Collection<MetaPropertyPath> columns) {
        return new GroupTableDsWrapper(datasource, columns);
    }

    @Override
    protected Map<Object, Object> __handleAggregationResults(AggregationContainer.Context context, Map<Object, Object> results) {
//        vaadin7
//        if (context instanceof com.haulmont.cuba.web.toolkit.ui.GroupTable.GroupAggregationContext) {
//
//            com.haulmont.cuba.web.toolkit.ui.GroupTable.GroupAggregationContext groupContext =
//                    (com.haulmont.cuba.web.toolkit.ui.GroupTable.GroupAggregationContext) context;
//
//            for (final Map.Entry<Object, Object> entry : results.entrySet()) {
//                final Table.Column column = columns.get(entry.getKey());
//                GroupAggregationCells cells;
//                if ((cells = groupAggregationCells.get(column)) != null) {
//                    com.vaadin.ui.Label cell = cells.getCell(groupContext.getGroupId());
//                    if (cell != null) {
//                        WebComponentsHelper.setLabelText(cell, entry.getValue(), column.getFormatter());
//                        entry.setValue(cell);
//                    }
//                }
//            }
//
//            return results;
//
//        } else {
//            return super.__handleAggregationResults(context, results);
//        }
        return Collections.EMPTY_MAP;
    }

    @Override
    public void groupBy(Object[] properties) {
        component.groupBy(properties);
    }

    @Override
    public void expandAll() {
        component.expandAll();
    }

    @Override
    public void expand(GroupInfo groupId) {
        component.expand(groupId);
    }

    @Override
    public void collapseAll() {
        component.collapseAll();
    }

    @Override
    public void collapse(GroupInfo groupId) {
        component.collapse(groupId);
    }

    @Override
    public boolean isExpanded(GroupInfo groupId) {
        return component.isExpanded(groupId);
    }

    @Override
    public boolean isFixedGrouping() {
        return component.isFixedGrouping();
    }

    @Override
    public void setFixedGrouping(boolean fixedGrouping) {
        component.setFixedGrouping(fixedGrouping);
    }

    protected class GroupTableDsWrapper extends SortableCollectionDsWrapper
            implements GroupTableContainer,
                       AggregationContainer {

        private boolean groupDatasource;
        private List<Object> aggregationProperties = null;

        //Supports items expanding
        private final Set<GroupInfo> expanded = new HashSet<>();

        private Set<GroupInfo> expandState = new HashSet<>();

        //Items cache
        private LinkedList<Object> cachedItemIds;
        private Object first;
        private Object last;

        public GroupTableDsWrapper(CollectionDatasource datasource) {
            super(datasource, true);
            groupDatasource = datasource instanceof GroupDatasource;
        }

        public GroupTableDsWrapper(CollectionDatasource datasource, Collection<MetaPropertyPath> properties) {
            super(datasource, properties, true);
            groupDatasource = datasource instanceof GroupDatasource;
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
        public void groupBy(Object[] properties) {
            if (groupDatasource) {
                doGroup(properties);
            }
        }

        private void doGroup(Object[] properties) {
            saveState();
            ((GroupDatasource) datasource).groupBy(properties);
            restoreState();
            resetCachedItems();

            if (aggregationCells != null) {
                if (hasGroups()) {
                    if (groupAggregationCells == null) {
                        groupAggregationCells = new HashMap<>();
                    } else {
                        groupAggregationCells.clear();
                    }
                    fillGroupAggregationCells(groupAggregationCells);
                } else {
                    groupAggregationCells = null;
                }
            }
        }

        private void saveState() {
            //save expanding state
            expandState.clear();
            expandState.addAll(expanded);
        }

        private void restoreState() {
            collapseAll();
            //restore groups expanding
            if (hasGroups()) {
                for (final GroupInfo groupInfo : expandState) {
                    expand(groupInfo);
                }
            }
            expandState.clear();
        }

        private void fillGroupAggregationCells(Map<Table.Column, GroupAggregationCells> cells) {
            final Collection roots = rootGroups();
            for (final Object rootGroup : roots) {
                __fillGroupAggregationCells(rootGroup, cells);
            }
        }

        private void __fillGroupAggregationCells(Object groupId, Map<Table.Column, GroupAggregationCells> cells) {
            final Set<Table.Column> aggregatableColumns = aggregationCells.keySet();

            for (final Column column : aggregatableColumns) {
                if (!columns.get(getGroupProperty(groupId)).equals(column)) {
                    GroupAggregationCells groupCells = cells.get(column);
                    if (groupCells == null) {
                        groupCells = new GroupAggregationCells();
                        cells.put(column, groupCells);
                    }
                    groupCells.addCell(groupId, createAggregationCell());
                }
            }

            if (hasChildren(groupId)) {
                final Collection children = getChildren(groupId);
                for (final Object child : children) {
                    __fillGroupAggregationCells(child, cells);
                }
            }
        }

        @Override
        public Collection<?> rootGroups() {
            if (hasGroups()) {
                return ((GroupDatasource) datasource).rootGroups();
            }
            return Collections.emptyList();
        }

        @Override
        public boolean hasChildren(Object id) {
            return isGroup(id) && ((GroupDatasource) datasource).hasChildren((GroupInfo) id);
        }

        @Override
        public Collection<?> getChildren(Object id) {
            if (isGroup(id)) {
                return ((GroupDatasource) datasource).getChildren((GroupInfo) id);
            }
            return Collections.emptyList();
        }

        @Override
        public Collection<?> getGroupItemIds(Object id) {
            if (isGroup(id)) {
                return ((GroupDatasource) datasource).getGroupItemIds((GroupInfo) id);
            }
            return Collections.emptyList();
        }

        @Override
        public int getGroupItemsCount(Object id) {
            if (isGroup(id)) {
                return ((GroupDatasource) datasource).getGroupItemsCount((GroupInfo) id);
            }
            return 0;
        }

        @Override
        public boolean isGroup(Object id) {
            return (id instanceof GroupInfo) && ((GroupDatasource) datasource).containsGroup((GroupInfo) id);
        }

        @Override
        public Object getGroupProperty(Object id) {
            if (isGroup(id)) {
                return ((GroupDatasource) datasource).getGroupProperty((GroupInfo) id);
            }
            return null;
        }

        @Override
        public Object getGroupPropertyValue(Object id) {
            if (isGroup(id)) {
                return ((GroupDatasource) datasource).getGroupPropertyValue((GroupInfo) id);
            }
            return null;
        }

        @Override
        public boolean hasGroups() {
            return groupDatasource && ((GroupDatasource) datasource).hasGroups();
        }

        @Override
        public Collection<?> getGroupProperties() {
            if (hasGroups()) {
                return ((GroupDatasource) datasource).getGroupProperties();
            }
            return Collections.emptyList();
        }

        @Override
        public void expandAll() {
            if (hasGroups()) {
                this.expanded.clear();
                expand(rootGroups());
                resetCachedItems();
            }
        }

        private void expand(Collection groupIds) {
            for (final Object groupId : groupIds) {
                expanded.add((GroupInfo) groupId);
                if (hasChildren(groupId)) {
                    expand(getChildren(groupId));
                }
            }
        }

        @Override
        public void expand(Object id) {
            if (isGroup(id)) {
                expanded.add((GroupInfo) id);
                resetCachedItems();
            }
        }

        @Override
        public void collapseAll() {
            if (hasGroups()) {
                expanded.clear();
                resetCachedItems();
            }
        }

        @Override
        public void collapse(Object id) {
            if (isGroup(id)) {
                //noinspection RedundantCast
                expanded.remove((GroupInfo)id);
                resetCachedItems();
            }
        }

        @Override
        public boolean isExpanded(Object id) {
            //noinspection RedundantCast
            return isGroup(id) && expanded.contains((GroupInfo)id);
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

        @SuppressWarnings("unchecked")
        @Override
        public Map<Object, Object> aggregate(Context context) {
            return __aggregate(this, context);
        }

        @Override
        public Object firstItemId() {
            if (hasGroups()) {
                return first;
            }
            return super.firstItemId();
        }

        @Override
        public Object lastItemId() {
            if (hasGroups()) {
                return last;
            }
            return super.lastItemId();
        }

        @Override
        public Object nextItemId(Object itemId) {
            if (hasGroups()) {
                if (itemId == null) {
                    return null;
                }
                if (isLastId(itemId)) {
                    return null;
                }
                int index = getCachedItemIds().indexOf(itemId);
                return getCachedItemIds().get(index + 1);
            }
            return super.nextItemId(itemId);
        }

        @Override
        public Object prevItemId(Object itemId) {
            if (hasGroups()) {
                if (itemId == null) {
                    return null;
                }

                if (isFirstId(itemId)) {
                    return null;
                }
                int index = getCachedItemIds().indexOf(itemId);
                return getCachedItemIds().get(index - 1);
            }
            return super.prevItemId(itemId);
        }

        @Override
        public boolean isFirstId(Object itemId) {
            if (hasGroups()) {
                return itemId != null && itemId.equals(first);
            }
            return super.isFirstId(itemId);
        }

        @Override
        public boolean isLastId(Object itemId) {
            if (hasGroups()) {
                return itemId != null && itemId.equals(last);
            }
            return super.isLastId(itemId);
        }

        public Object addItemAfter(Object previousItemId) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public Item addItemAfter(Object previousItemId, Object newItemId) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection getItemIds() {
            if (hasGroups()) {
                return getCachedItemIds();
            } else {
                return super.getItemIds();
            }
        }

        @Override
        public void sort(Object[] propertyId, boolean[] ascending) {
            resetCachedItems();
            super.sort(propertyId, ascending);
        }

        protected synchronized LinkedList getCachedItemIds() {
            if (cachedItemIds == null) {
                final LinkedList<Object> result = new LinkedList<>();
                final List<GroupInfo> roots = ((GroupDatasource) datasource).rootGroups();
                for (final GroupInfo root : roots) {
                    result.add(root);
                    collectItemIds(root, result);
                }
                cachedItemIds = result;

                if (!cachedItemIds.isEmpty()) {
                    first = cachedItemIds.peekFirst();
                    last = cachedItemIds.peekLast();
                }
            }
            return cachedItemIds;
        }

        private void collectItemIds(GroupInfo groupId, final List<Object> itemIds) {
            if (expanded.contains(groupId)) {
                if (((GroupDatasource) datasource).hasChildren(groupId)) {
                    final List<GroupInfo> children = ((GroupDatasource) datasource).getChildren(groupId);
                    for (final GroupInfo child : children) {
                        itemIds.add(child);
                        collectItemIds(child, itemIds);
                    }
                } else {
                    itemIds.addAll(((GroupDatasource) datasource).getGroupItemIds(groupId));
                }
            }
        }

        protected void resetCachedItems() {
            cachedItemIds = null;
            first = null;
            last = null;
        }

        @Override
        public int size() {
            if (hasGroups()) {
                return getItemIds().size();
            }
            return super.size();
        }

        @Override
        protected DatasourceListener createDatasourceListener() {
            return new GroupDataSourceRefreshListener();
        }

        protected class GroupDataSourceRefreshListener extends DataSourceRefreshListener {
            @Override
            public void stateChanged(Datasource<Entity> ds, Datasource.State prevState, Datasource.State state) {
                rerender = false;
                Collection groupProperties = component.getGroupProperties();
                component.groupBy(groupProperties.toArray());
                super.stateChanged(ds, prevState, state);
                rerender = true;
            }

            @Override
            public void collectionChanged(CollectionDatasource ds, Operation operation, List<Entity> items) {
                Collection groupProperties = component.getGroupProperties();
                component.groupBy(groupProperties.toArray());
                super.collectionChanged(ds, operation, items);
            }
        }

    }

    protected class AggregatableGroupPropertyValueFormatter extends DefaultGroupPropertyValueFormatter {
        @Override
        public String format(Object groupId, Object value) {
            String formattedValue = super.format(groupId, value);
            int count = WebGroupTable.this.component.getGroupItemsCount(groupId);
            return String.format("%s (%d)", formattedValue == null ? "" : formattedValue, count);
        }
    }

    protected class DefaultGroupPropertyValueFormatter implements CubaGroupTable.GroupPropertyValueFormatter {

        @Override
        public String format(Object groupId, Object value) {
            if (value == null) {
                return "";
            }
            final MetaPropertyPath propertyPath = ((GroupInfo<MetaPropertyPath>) groupId).getProperty();
            final Table.Column column = columns.get(propertyPath);
            if (column != null && column.getXmlDescriptor() != null) {
                String captionProperty = column.getXmlDescriptor().attributeValue("captionProperty");
                if (column.getFormatter() != null) {
                    return column.getFormatter().format(value);
                } else if (!StringUtils.isEmpty(captionProperty)) {
                    return propertyPath.getRange().isDatatype() ?
                            propertyPath.getRange().asDatatype().format(value) :
                            String.valueOf(((Instance) value).getValue(captionProperty));
                }
            }
            final Range range = propertyPath.getRange();
            if (range.isDatatype()) {
                return range.asDatatype().format(value);
            } else if (range.isEnum()) {
                String nameKey = value.getClass().getSimpleName() + "." + value.toString();
                return MessageProvider.getMessage(value.getClass(), nameKey);
            } else {
                if (value instanceof Instance) {
                    return ((Instance) value).getInstanceName();
                } else {
                    return value.toString();
                }
            }
        }
    }

    @Override
    public List<Column> getColumns() {
        Object[] visibleColumns = component.getVisibleColumns();

        if (visibleColumns == null) {
            return Collections.emptyList();
        }

        List<Column> columns = new ArrayList<>(visibleColumns.length);
        for (final Object column : visibleColumns) {
            MetaPropertyPath path = (MetaPropertyPath) column;
            columns.add(getColumn(path.toString()));
        }

        return columns;
    }

    private class GroupAggregationCells {
        private Map<Object, com.vaadin.ui.Label> cells = new HashMap<>();

        public void addCell(Object groupId, com.vaadin.ui.Label cell) {
            cells.put(groupId, cell);
        }

        public com.vaadin.ui.Label getCell(Object groupId) {
            return cells.get(groupId);
        }
    }

    private class GroupAggregationDatasourceListener extends AggregationDatasourceListener {
        @Override
        public void valueChanged(Entity source, String property, Object prevValue, Object value) {
            super.valueChanged(source, property, prevValue, value);
            GroupDatasource ds = (GroupDatasource) WebGroupTable.this.getDatasource();
            Collection<GroupInfo> roots = ds.rootGroups();
            for (final GroupInfo root : roots) {
                recalcAggregation(root);
            }
        }

        protected void recalcAggregation(GroupInfo groupInfo) {
//            vaadin7
//            component.aggregate(new com.haulmont.cuba.web.toolkit.ui.GroupTable.GroupAggregationContext(
//                    component,
//                    groupInfo
//            ));
        }
    }
}