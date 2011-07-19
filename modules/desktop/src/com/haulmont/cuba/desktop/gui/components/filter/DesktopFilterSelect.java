/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.components.filter;

import com.haulmont.cuba.desktop.sys.vcl.ExtendedComboBox;
import com.haulmont.cuba.security.entity.FilterEntity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>$Id$</p>
 *
 * @author devyatkin
 */
public class DesktopFilterSelect extends ExtendedComboBox {
    private Set<FilterEntity> filters = new HashSet<FilterEntity>();
    private ItemWrapper<FilterEntity> noFilterWrapper;

    public void setNoFilter(ItemWrapper<FilterEntity> noFilterWrapper){
      this.noFilterWrapper=noFilterWrapper;
    }

    public void addItem(Object item) {
        super.addItem(item);
        if (!item.equals(noFilterWrapper))
        filters.add(((ItemWrapper<FilterEntity>) item).getItem());
    }

    public Collection<FilterEntity> getFilters() {
        return filters;
    }

    public void removeAllItems() {
        super.removeAllItems();
        filters.clear();
    }
}
