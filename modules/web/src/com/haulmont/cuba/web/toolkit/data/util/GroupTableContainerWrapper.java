/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 10.11.2009 18:08:47
 *
 * $Id$
 */
package com.haulmont.cuba.web.toolkit.data.util;

import com.haulmont.cuba.web.toolkit.data.GroupTableContainer;
import com.haulmont.cuba.web.toolkit.data.AggregationContainer;
import com.vaadin.data.Container;
import com.vaadin.data.util.ContainerOrderedWrapper;

import java.util.Collection;
import java.util.Map;

public class GroupTableContainerWrapper extends ContainerOrderedWrapper
        implements GroupTableContainer, AggregationContainer
{
    private boolean groupTableContainer;

    public GroupTableContainerWrapper(Container toBeWrapped) {
        super(toBeWrapped);

        groupTableContainer = toBeWrapped instanceof GroupTableContainer;
    }

    public void groupBy(final Object[] properties) {
        if (groupTableContainer) {
            ((GroupTableContainer) container).groupBy(properties);
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public boolean isGroup(Object id) {
        return groupTableContainer && ((GroupTableContainer) container).isGroup(id);
    }

    public Collection<?> rootGroups() {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).rootGroups();
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public boolean hasChildren(Object id) {
        return groupTableContainer && ((GroupTableContainer) container).hasChildren(id);
    }

    public Collection<?> getChildren(Object id) {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getChildren(id);
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public boolean hasGroups() {
        return groupTableContainer && ((GroupTableContainer) container).hasGroups();
    }

    public Object getGroupProperty(Object itemId) {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getGroupProperty(itemId);
        }
        throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                + container.getClass());
    }

    public Object getGroupPropertyValue(Object itemId) {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getGroupPropertyValue(itemId);
        }
        throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                + container.getClass());
    }

    public Collection<?> getGroupItemIds(Object itemId) {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getGroupItemIds(itemId);
        }
        throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                + container.getClass());
    }

    public int getGroupItemsCount(Object itemId) {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getGroupItemsCount(itemId);
        }
        throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                + container.getClass());
    }

    public Collection<?> getGroupProperties() {
        if (groupTableContainer) {
            return ((GroupTableContainer) container).getGroupProperties();
        }
        throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                + container.getClass());
    }

    public void expand(Object id) {
        if (groupTableContainer) {
            ((GroupTableContainer) container).expand(id);
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public boolean isExpanded(Object id) {
        return groupTableContainer && ((GroupTableContainer) container).isExpanded(id);
    }

    public void expandAll() {
        if (groupTableContainer) {
            ((GroupTableContainer) container).expandAll();
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public void collapseAll() {
        if (groupTableContainer) {
            ((GroupTableContainer) container).collapseAll();
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public void collapse(Object id) {
        if (groupTableContainer) {
            ((GroupTableContainer) container).collapse(id);
        } else {
            throw new IllegalStateException("Wrapped container is not GroupTableContainer:"
                    + container.getClass());
        }
    }

    public Collection getAggregationPropertyIds() {
        if (container instanceof AggregationContainer) {
            return ((AggregationContainer) container).getAggregationPropertyIds();
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + container.getClass());
    }

    public Type getContainerPropertyAggregation(Object propertyId) {
        if (container instanceof AggregationContainer) {
            return ((AggregationContainer) container).getContainerPropertyAggregation(propertyId);
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + container.getClass());
    }

    public void addContainerPropertyAggregation(Object propertyId, Type type) {
        if (container instanceof AggregationContainer) {
            ((AggregationContainer) container).addContainerPropertyAggregation(propertyId, type);
        } else {
            throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                    + container.getClass());
        }
    }

    public void removeContainerPropertyAggregation(Object propertyId) {
        if (container instanceof AggregationContainer) {
            ((AggregationContainer) container).removeContainerPropertyAggregation(propertyId);
        } else {
            throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                    + container.getClass());
        }
    }

    public Map<Object, String> aggregate(Collection itemIds) {
        if (container instanceof AggregationContainer) {
            return ((AggregationContainer) container).aggregate(itemIds);
        }
        throw new IllegalStateException("Wrapped container is not AggregationContainer: "
                + container.getClass());
    }
}
