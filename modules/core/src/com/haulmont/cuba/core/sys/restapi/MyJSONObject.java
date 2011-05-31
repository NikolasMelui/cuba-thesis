/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.restapi;

import java.io.PrintWriter;
import java.util.*;

/**
 * A JSON instance for persistence.
 * <br>
 * Persistent instances have a persistent identity that extends beyond the process lifetime unlike other common
 * identity such as {@linkplain System#identityHashCode(Object) identity hash code} for a Java instance in a JVM.
 * <br>
 * A JSONObject instance must need such a persistent identity.
 *
 * @author Pinaki Poddar
 *
 */
public class MyJSONObject implements MyJSON {
    private final String _id;
    private final boolean _ref;
    private final Map<String, Object> _values;

    public MyJSONObject(Object id, boolean ref) {
        _id   = id.toString();
        _ref  = ref;
        _values = new LinkedHashMap<String, Object>();
    }

    public void set(String key, Object value) {
        _values.put(key, value);
    }

    public void write(PrintWriter writer) {
        writer.println(toString());
    }
    public String toString() {
        return asString(0).toString();
    }

    public StringBuilder asString(int indent) {
        StringBuilder buf = new StringBuilder().append(OBJECT_START);
        buf.append(encodeField(_ref ? REF_MARKER : ID_MARKER, ior(), 0));
        if (_ref) {
            return buf.append(OBJECT_END);
        }
        StringBuilder tab = newIndent(indent+1);
        for (Map.Entry<String, Object> e : _values.entrySet()) {
            buf.append(FIELD_SEPARATOR).append(NEWLINE);
            buf.append(tab).append(encodeField(e.getKey(), e.getValue(), indent+1));
        }
        buf.append(NEWLINE)
           .append(newIndent(indent))
           .append(OBJECT_END);
        return buf;
    }

    /**
     * Encoding a JSON field is a quoted field name, followed by a :, followed by a value (which itself can be JSON)
     * @param field
     * @param value
     * @param indent
     * @return
     */
    private static StringBuilder encodeField(String field, Object value, int indent) {
        return new StringBuilder()
              .append(quoteFieldName(field))
              .append(VALUE_SEPARATOR)
              .append(quoteFieldValue(value, indent));
    }

    private static StringBuilder newIndent(int indent) {
        char[] tabs = new char[indent*4];
        Arrays.fill(tabs, SPACE);
        return new StringBuilder().append(tabs);
    }


    String ior() {
        return _id;
    }

    private static StringBuilder quoteFieldName(String s) {
        return new StringBuilder().append(QUOTE).append(s).append(QUOTE);
    }

    /**
     * Creates a StringBuilder for the given value.
     * If the value is null, outputs <code>null</code> without quote
     * If the value is Number, outputs the value without quote
     * If the value is JSON, outputs the string rendition of value
     * Otherwise quoted value
     * @param o
     * @return
     */
    private static StringBuilder quoteFieldValue(Object o, int indent) {
        if (o == null) return new StringBuilder(NULL_LITERAL);
        if (o instanceof Number) return new StringBuilder(o.toString());
        if (o instanceof MyJSON) return ((MyJSON)o).asString(indent);
        return quoted(o.toString());
    }

    private static StringBuilder quoted(Object o) {
        if (o == null) return new StringBuilder(NULL_LITERAL);
        return new StringBuilder().append(QUOTE).append(o.toString()).append(QUOTE);
    }

    /**
     * An array of objects. Members can be JSON too.
     *
     * @author Pinaki Poddar
     *
     */
    public static class Array implements MyJSON {
        private List<Object> _members = new ArrayList<Object>();

        public void add(Object o) {
            _members.add(o);
        }

        public String toString() {
            return asString(0).toString();
        }

        public StringBuilder asString(int indent) {
            StringBuilder buf = new StringBuilder().append(ARRAY_START);
            StringBuilder tab = MyJSONObject.newIndent(indent+1);
            for (Object o : _members) {
                if (buf.length() > 1) buf.append(MEMBER_SEPARATOR);
                buf.append(NEWLINE).append(tab);
                if (o instanceof MyJSON)
                    buf.append(((MyJSON)o).asString(indent+1));
                else
                    buf.append(o);
            }
            buf.append(NEWLINE)
               .append(MyJSONObject.newIndent(indent))
               .append(ARRAY_END);

            return buf;
        }
    }

    /**
     * A map whose key or value can be JSON.
     * A map is encoded as JSON as an array of entries. Each entry is a key value pair separated with :
     *
     * @author Pinaki Poddar
     *
     */
    public static class KVMap implements MyJSON {
        private Map<Object,Object> _entries = new LinkedHashMap<Object,Object>();

        public void put(Object k, Object v) {
            _entries.put(k,v);
        }

        public String toString() {
            return asString(0).toString();
        }

        public StringBuilder asString(int indent) {
            StringBuilder buf = new StringBuilder().append(ARRAY_START);
            StringBuilder tab = MyJSONObject.newIndent(indent+1);
            for (Map.Entry<Object, Object> e : _entries.entrySet()) {
                if (buf.length() > 1) buf.append(MEMBER_SEPARATOR);
                buf.append(NEWLINE).append(tab);
                Object key = e.getKey();
                if (key instanceof MyJSON) {
                    buf.append(((MyJSON)key).asString(indent+1));
                } else {
                    buf.append(key);
                }
                buf.append(VALUE_SEPARATOR);
                Object value = e.getValue();
                if (value instanceof MyJSON) {
                    buf.append(((MyJSON)value).asString(indent+2));
                } else {
                    buf.append(value);
                }

            }
            buf.append(NEWLINE)
               .append(MyJSONObject.newIndent(indent))
               .append(ARRAY_END);
            return buf;
        }
    }

    public static void main(String[] args) throws Exception {
        MyJSONObject o = new MyJSONObject("Person-1234", false);
        MyJSONObject r = new MyJSONObject("Person-1234", true);
        MyJSONObject f = new MyJSONObject("Person-2345", false);
        Array  a = new Array();
        a.add(f);
        a.add(3456);
        a.add(null);
        a.add(r);
        a.add(null);
        KVMap map = new KVMap();
        map.put("k1", r);
        map.put("k2", f);
        map.put("k3", null);
        map.put("k4", 3456);
        map.put(null, 6789);

        f.set("name", "Mary");
        f.set("age", 30);
        f.set("friend", r);
        o.set("name", "John");
        o.set("age", 20);
        o.set("friend", f);
        o.set("friends", a);
        o.set("map", map);

        System.err.println(o);
    }
}
