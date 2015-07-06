/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.querymacro;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.chile.core.datatypes.impl.EnumClass;
import com.haulmont.cuba.core.sys.QueryMacroHandler;
import org.springframework.context.annotation.Scope;

import javax.annotation.ManagedBean;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author krivopustov
 * @version $Id$
 */
@ManagedBean("cuba_EnumQueryMacroHandler")
@Scope("prototype")
public class EnumQueryMacroHandler implements QueryMacroHandler {

    protected Map<String, Object> namedParameters;

    protected static final Pattern MACRO_PATTERN = Pattern.compile("@enum\\s*\\(([^\\)]+)\\)");

    @Override
    public String expandMacro(String queryString) {
        Matcher matcher = MACRO_PATTERN.matcher(queryString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, doExpand(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String doExpand(String enumString) {
        int idx = enumString.lastIndexOf('.');
        String className = enumString.substring(0, idx);
        String valueName = enumString.substring(idx + 1);
        Class<Enum> aClass;
        try {
            //noinspection unchecked
            aClass = (Class<Enum>) ReflectionHelper.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error expanding JPQL macro", e);
        }
        for (Enum anEnum : aClass.getEnumConstants()) {
            if (valueName.equals(anEnum.name())) {
                if (anEnum instanceof EnumClass) {
                    Object id = ((EnumClass) anEnum).getId();
                    if (id instanceof String) {
                        return "'" + id + "'";
                    } else {
                        return id.toString();
                    }
                } else {
                    return String.valueOf(anEnum.ordinal());
                }
            }
        }
        throw new RuntimeException("Error expanding JPQL macro: enum " + enumString + " is not found");
    }

    @Override
    public void setQueryParams(Map<String, Object> namedParameters) {
        this.namedParameters = namedParameters;
    }

    @Override
    public Map<String, Object> getParams() {
        return namedParameters;
    }

    @Override
    public String replaceQueryParams(String queryString, Map<String, Object> params) {
        return queryString;
    }
}
