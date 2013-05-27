/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.gui.data.GroupInfo;
import com.haulmont.cuba.web.gui.data.PropertyValueStringify;
import com.haulmont.cuba.web.toolkit.data.GroupTableContainer;
import com.haulmont.cuba.web.toolkit.data.util.GroupTableContainerWrapper;
import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.KeyMapper;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;

import java.util.*;

/**
 * @author gorodnov
 * @version $Id$
 */
public class CubaGroupTable extends CubaTable implements GroupTableContainer {

    protected KeyMapper groupIdMap = new KeyMapper();

    protected GroupPropertyValueFormatter groupPropertyValueFormatter;

    protected boolean fixedGrouping = false;

    @Override
    public void setContainerDataSource(Container newDataSource) {
        if (newDataSource == null) {
            newDataSource = new IndexedContainer();
        }

        super.setContainerDataSource(new GroupTableContainerWrapper(newDataSource));
    }

    @Override
    protected String formatPropertyValue(Object rowId, Object colId, Property<?> property) {
        if (property instanceof PropertyValueStringify)
            return ((PropertyValueStringify) property).getFormattedValue();

        return super.formatPropertyValue(rowId, colId, property);
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);

        if (hasGroups()) {
            final Collection groupProperties = getGroupProperties();
            final String[] groupColumns = new String[groupProperties.size()];

            int index = 0;
            for (final Object groupColumnId : groupProperties) {
                groupColumns[index++] = columnIdMap.key(groupColumnId);
            }
            target.addVariable(this, "groupColumns", groupColumns);
        }
    }

    @Override
    protected boolean changeVariables(Map<String, Object> variables) {
        boolean clientNeedsContentRefresh = super.changeVariables(variables);

        boolean needsResetPageBuffer = false;
        Object[] newGroupProperties = null;

        if (!fixedGrouping) {
            if (variables.containsKey("columnorder") && !variables.containsKey("groupedcolumns")) {
                newGroupProperties = new Object[0];
            } else if (variables.containsKey("groupedcolumns")) {
                final Object[] ids = (Object[]) variables.get("groupedcolumns");
                final Object[] groupProperties = new Object[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    groupProperties[i] = columnIdMap.get(ids[i].toString());
                }
                newGroupProperties = groupProperties;
                // Deny group by generated columns
                if (!columnGenerators.isEmpty()) {
                    List<Object> notGeneratedProperties = new ArrayList<>();
                    for (Object id : newGroupProperties) {
                        if (!columnGenerators.containsKey(id) || (id instanceof MetaPropertyPath)) {
                            notGeneratedProperties.add(id);
                        }
                    }
                    newGroupProperties = notGeneratedProperties.toArray();
                }
            }
        } else {
            if (variables.containsKey("columnorder") || variables.containsKey("groupedcolumns")) {
                markAsDirty();
            }
        }

        if (variables.containsKey("collapsedcolumns")) {
            boolean needToRegroup = false;
            final List<Object> groupProperties = new ArrayList<>(getGroupProperties());
            for (int index = 0; index < groupProperties.size(); index++) {
                final Object propertyId = groupProperties.get(index);
                if (isColumnCollapsed(propertyId)) {
                    groupProperties.subList(index, groupProperties.size())
                            .clear();
                    needToRegroup = true;
                    break;
                }
            }
            if (needToRegroup) {
                newGroupProperties = groupProperties.toArray();
            }
        }

        if (variables.containsKey("expand")) {
            Object groupId = groupIdMap.get((String) variables.get("expand"));
            expand(groupId, false);
            clientNeedsContentRefresh = true;
            needsResetPageBuffer = true;
        }

        if (variables.containsKey("collapse")) {
            Object groupId = groupIdMap.get((String) variables.get("collapse"));
            collapse(groupId, false);
            clientNeedsContentRefresh = true;
            needsResetPageBuffer = true;
        }

        if (newGroupProperties != null) {
            groupBy(newGroupProperties, false);
        }

        if (needsResetPageBuffer) {
            resetPageBuffer();
        }

        return clientNeedsContentRefresh;
    }

    @Override
    protected boolean isCellPaintingNeeded(Object itemId, Object columnId) {
        return !isGroup(itemId);
    }

    @Override
    protected void paintRowAttributes(PaintTarget target, Object itemId) throws PaintException {
        super.paintRowAttributes(target, itemId);

        boolean hasGroups = hasGroups();
        if (hasGroups) {
            if (isGroup(itemId)) {
                target.addAttribute("colKey", columnIdMap.key(getGroupProperty(itemId)));
                target.addAttribute("groupKey", groupIdMap.key(itemId));
                if (isExpanded(itemId))
                    target.addAttribute("expanded", true);

                final Object propertyValue = getGroupPropertyValue(itemId);
                target.addAttribute("groupCaption", formatGroupPropertyValue(itemId, propertyValue));

                // todo aggregation
            }
        }
    }

    @Override
    protected LinkedHashSet<Object> getItemIdsInRange(Object startItemId, final int length) {
        Set<Object> rootIds = super.getItemIdsInRange(startItemId, length);
        LinkedHashSet<Object> ids = new LinkedHashSet<>();
        for (Object itemId: rootIds) {
            if (itemId instanceof GroupInfo) {
                if (!isExpanded(itemId)) {
                    Collection<?> itemIds = getGroupItemIds(itemId);
                    ids.addAll(itemIds);
                    expand(itemId, true);
                }

                List<GroupInfo> children = (List<GroupInfo>) getChildren(itemId);
                for (GroupInfo groupInfo : children) {
                    if (!isExpanded(groupInfo)) {
                        expand(groupInfo, true);
                    }
                }
            } else {
                ids.add(itemId);
            }
        }
        return ids;
    }

    @Override
    protected boolean isColumnNeedsToRefreshRendered(Object colId) {
        final GroupTableContainer items = (GroupTableContainer) this.items;
        final boolean groupped = items.hasGroups();

        return !groupped || !getGroupProperties().contains(colId);
    }

    @Override
    protected boolean isItemNeedsToRefreshRendered(Object itemId) {
        final GroupTableContainer items = (GroupTableContainer) this.items;
        final boolean groupped = items.hasGroups();

        return !groupped || !items.isGroup(itemId);
    }

    protected String formatGroupPropertyValue(Object groupId, Object groupValue) {
        return groupPropertyValueFormatter != null
                ? groupPropertyValueFormatter.format(groupId, groupValue)
                : (groupValue == null ? "" : groupValue.toString());
    }

    protected void expand(Object id, boolean rerender) {
        final int pageIndex = getCurrentPageFirstItemIndex();
        ((GroupTableContainer) items).expand(id);
        setCurrentPageFirstItemIndex(pageIndex, false);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            markAsDirty();
        }
    }

    protected void collapse(Object id, boolean rerender) {
        final int pageIndex = getCurrentPageFirstItemIndex();
        ((GroupTableContainer) items).collapse(id);
        setCurrentPageFirstItemIndex(pageIndex, false);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            markAsDirty();
        }
    }

    protected void groupBy(Object[] properties, boolean rerender) {
        ((GroupTableContainer) items).groupBy(properties);
        if (rerender) {
            resetPageBuffer();
            refreshRenderedCells();
            markAsDirty();
        }
    }

    @Override
    protected void setColumnOrder(Object[] columnOrder) {
        Collection<?> groupProperties = getGroupProperties();
        if (!groupProperties.isEmpty()) {
            // check order of grouped and not grouped columns
            int i = 1;
            while (i < columnOrder.length && isValidOrderPosition(groupProperties, columnOrder, i)) {
                i++;
            }
            if (i < columnOrder.length) {
                // found not grouped column on left side of grouped
                markAsDirty();
                return;
            }
        }

        super.setColumnOrder(columnOrder);
    }

    protected boolean isValidOrderPosition(Collection<?> groupProperties, Object[] columnOrder, int index) {
        if (!groupProperties.contains(columnOrder[index]))
            return true;

        return groupProperties.contains(columnOrder[index]) &&
                groupProperties.contains(columnOrder[index - 1]);
    }

    @Override
    public Collection<?> getGroupProperties() {
        Collection<?> groupProperties = ((GroupTableContainer) items).getGroupProperties();
        // Deny group by generated columns
        if (!columnGenerators.isEmpty()) {
            List<Object> notGeneratedGroupProps = new ArrayList<>();
            for (Object id : groupProperties) {
                if (!columnGenerators.containsKey(id) || (id instanceof MetaPropertyPath))
                    notGeneratedGroupProps.add(id);
            }
            return notGeneratedGroupProps;
        } else
            return groupProperties;
    }

    @Override
    public void expandAll() {
        final int pageIndex = getCurrentPageFirstItemIndex();
        ((GroupTableContainer) items).expandAll();
        setCurrentPageFirstItemIndex(pageIndex, false);
        resetPageBuffer();
        refreshRenderedCells();
        markAsDirty();
    }

    @Override
    public void expand(Object id) {
        expand(id, true);
    }

    @Override
    public void collapseAll() {
        final int pageIndex = getCurrentPageFirstItemIndex();
        ((GroupTableContainer) items).collapseAll();
        setCurrentPageFirstItemIndex(pageIndex, false);
        resetPageBuffer();
        refreshRenderedCells();
        markAsDirty();
    }

    @Override
    public void collapse(Object id) {
        collapse(id, true);
    }

    @Override
    public boolean hasGroups() {
        return ((GroupTableContainer) items).hasGroups();
    }

    @Override
    public void groupBy(Object[] properties) {
        groupBy(properties, true);
    }

    @Override
    public boolean isGroup(Object itemId) {
        return ((GroupTableContainer) items).isGroup(itemId);
    }

    @Override
    public Collection<?> rootGroups() {
        return ((GroupTableContainer) items).rootGroups();
    }

    @Override
    public boolean hasChildren(Object id) {
        return ((GroupTableContainer) items).hasChildren(id);
    }

    @Override
    public Collection<?> getChildren(Object id) {
        return ((GroupTableContainer) items).getChildren(id);
    }

    @Override
    public Object getGroupProperty(Object itemId) {
        return ((GroupTableContainer) items).getGroupProperty(itemId);
    }

    @Override
    public Object getGroupPropertyValue(Object itemId) {
        return ((GroupTableContainer) items).getGroupPropertyValue(itemId);
    }

    @Override
    public Collection<?> getGroupItemIds(Object itemId) {
        return ((GroupTableContainer) items).getGroupItemIds(itemId);
    }

    @Override
    public int getGroupItemsCount(Object itemId) {
        return ((GroupTableContainer) items).getGroupItemsCount(itemId);
    }

    @Override
    public boolean isExpanded(Object id) {
        return ((GroupTableContainer) items).isExpanded(id);
    }

    public boolean isFixedGrouping() {
        return fixedGrouping;
    }

    public void setFixedGrouping(boolean fixedGrouping) {
        this.fixedGrouping = fixedGrouping;
        markAsDirty();
    }

    public GroupPropertyValueFormatter getGroupPropertyValueFormatter() {
        return groupPropertyValueFormatter;
    }

    public void setGroupPropertyValueFormatter(GroupPropertyValueFormatter groupPropertyValueFormatter) {
        this.groupPropertyValueFormatter = groupPropertyValueFormatter;
    }

    public interface GroupPropertyValueFormatter {
        String format(Object groupId, Object value);
    }
}