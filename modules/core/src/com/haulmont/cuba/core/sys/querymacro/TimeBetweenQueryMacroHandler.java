/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.core.sys.querymacro;

import com.haulmont.cuba.core.global.TimeProvider;
import com.haulmont.cuba.core.sys.QueryMacroHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.context.annotation.Scope;

import javax.annotation.ManagedBean;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ManagedBean("cuba_TimeBetweenQueryMacroHandler")
@Scope("prototype")
public class TimeBetweenQueryMacroHandler implements QueryMacroHandler {

    protected static final Pattern MACRO_PATTERN = Pattern.compile("@between\\s*\\(([^\\)]+)\\)");
    protected static final Pattern PARAM_PATTERN = Pattern.compile("(now)\\s*([+-]*)\\s*(\\d*)");
    
    protected static Map<String, Object> units = new HashMap<String, Object>();

    static {
        units.put("year", Calendar.YEAR);
        units.put("month", Calendar.MONTH);
        units.put("day", Calendar.DAY_OF_MONTH);
        units.put("hour", Calendar.HOUR_OF_DAY);
        units.put("minute", Calendar.MINUTE);
        units.put("second", Calendar.SECOND);
    }

    protected int count;
    protected Map<String, Object> params = new HashMap<String, Object>();

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

    public void setQueryParams(Map<String, Object> namedParameters) {
    }

    public Map<String, Object> getParams() {
        return params;
    }

    protected String doExpand(String macro) {
        count++;
        String[] args = macro.split(",");
        if (args.length != 4)
            throw new RuntimeException("Invalid macro: " + macro);

        String field = args[0];
        String param1 = getParam(args, 1);
        String param2 = getParam(args, 2);

        return String.format("(%s >= :%s and %s < :%s)", field, param1, field, param2);
    }

    protected String getParam(String[] args, int idx) {
        String arg = args[idx].trim();
        String unit = args[3].trim();

        Matcher matcher = PARAM_PATTERN.matcher(arg);
        if (!matcher.find())
            throw new RuntimeException("Invalid macro argument: " + arg);

        String op = matcher.group(2);
        int num = 0;
        if (!StringUtils.isBlank(op)) {
            try {
                num = Integer.valueOf(matcher.group(3));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid macro argument: " + arg, e);
            }
            if (op.equals("-"))
                num = num * (-1);
        }

        Date date = computeDate(num, unit);

        String paramName = args[0].trim().replace(".", "_") + "_" + count + "_" + idx;
        params.put(paramName, date);

        return paramName;
    }

    protected Date computeDate(int num, String unit) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(TimeProvider.currentTimestamp());
        int calField1 = getCalendarField(unit);
        if (num != 0) {
            cal.add(calField1, num);
        }
        int calField = calField1;
        Date date = DateUtils.truncate(cal.getTime(), calField);
        return date;
    }

    protected int getCalendarField(String unit) {
        Integer calField = (Integer) units.get(unit.toLowerCase());
        if (calField == null)
            throw new RuntimeException("Invalid macro argument: " + unit);
        return calField;
    }

}
