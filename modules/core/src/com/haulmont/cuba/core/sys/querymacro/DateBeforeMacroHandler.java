/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 14.03.11 18:58
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys.querymacro;

import com.haulmont.cuba.core.sys.QueryMacroHandler;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.context.annotation.Scope;

import javax.annotation.ManagedBean;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ManagedBean("cuba_DateBeforeQueryMacroHandler")
@Scope("prototype")
public class DateBeforeMacroHandler implements QueryMacroHandler {

    protected static final Pattern MACRO_PATTERN = Pattern.compile("@dateBefore\\(([^\\)]+)\\)");

    protected int count;
    protected Map<String, Object> namedParameters;
    protected List<String> paramNames = new ArrayList<String>();

    public String expandMacro(String queryString) {
        count = 0;
        Matcher matcher = MACRO_PATTERN.matcher(queryString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, doExpand(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String doExpand(String macro) {
        count++;
        String[] args = macro.split(",");
        if (args.length != 2)
            throw new RuntimeException("Invalid macro: " + macro);

        String field = args[0].trim();
        String param = args[1].trim().substring(1);
        paramNames.add(param);

        return String.format("(%s < :%s)", field, param);
    }

    public void setQueryParams(Map<String, Object> namedParameters) {
        this.namedParameters = namedParameters;
    }

    public Map<String, Object> getParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String paramName : paramNames) {
            Date date = (Date) namedParameters.get(paramName);
            if (date == null)
                throw new RuntimeException("Parameter " + paramName + " not found for macro");

            Date d = DateUtils.truncate(date, Calendar.DAY_OF_MONTH);
            params.put(paramName, d);
        }
        return params;
    }
}
