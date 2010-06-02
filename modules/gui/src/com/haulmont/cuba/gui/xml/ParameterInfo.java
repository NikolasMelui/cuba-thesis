/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 05.10.2009 18:30:40
 *
 * $Id$
 */
package com.haulmont.cuba.gui.xml;

import java.io.Serializable;

public class ParameterInfo implements Serializable {

    public enum Type {
        DATASOURCE("ds"),
        COMPONENT("component"),
        PARAM("param"),
        SESSION("session"),
        CUSTOM("custom");

        private String prefix;

        Type(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private Type type;
    private String path;
    private boolean caseInsensitive;

    ParameterInfo(String name, Type type, boolean caseInsensitive) {
        this.path = name;
        this.type = type;
        this.caseInsensitive = caseInsensitive;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return (type.getPrefix() + "$" + path);
    }

    public String getFlatName() {
        return (type.getPrefix() + "." + path).replaceAll("\\.", "_");
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParameterInfo that = (ParameterInfo) o;

        return path.equals(that.path) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return type + " : " + path;
    }
}
