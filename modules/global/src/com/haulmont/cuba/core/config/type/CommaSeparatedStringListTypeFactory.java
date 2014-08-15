/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.config.type;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author krivopustov
 * @version $Id$
 */
public class CommaSeparatedStringListTypeFactory extends TypeFactory {

    @Override
    public Object build(String string) {
        List<String> stringList = new ArrayList<>();
        if (StringUtils.isNotEmpty(string)) {
            String[] elements = string.split(",");
            for (String element : elements) {
                if (StringUtils.isNotEmpty(element)) {
                    stringList.add(element.trim());
                }
            }
        }
        return stringList;
    }
}