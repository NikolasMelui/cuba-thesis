/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.chile.core.model.Instance;

import java.util.Comparator;

public abstract class AbstractComparator<T> implements Comparator<T> {
    protected boolean asc;

    protected AbstractComparator(boolean asc) {
        this.asc = asc;
    }

    protected int __compare(Object o1, Object o2) {
        int c;

        if (o1 instanceof String && o2 instanceof String) {
            c = ((String) o1).compareToIgnoreCase((String) o2);
        } else if (o1 instanceof Comparable && o2 instanceof Comparable) {
            c = ((Comparable) o1).compareTo(o2);
        } else if (o1 instanceof Instance && o2 instanceof Instance) {
            c = ((Instance) o1).getInstanceName().compareToIgnoreCase(((Instance) o2).getInstanceName());
        } else if (o1 == null) {
            if (o2 != null) {
                c = 1;
            } else {
                c = 0;
            }
        } else {
            c = -1;
        }

        return asc ? c : -c;
    }
}
