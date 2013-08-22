/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.chile.core.datatypes.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.util.Locale;

import com.haulmont.chile.core.datatypes.Datatype;
import com.haulmont.chile.core.datatypes.Datatypes;
import com.haulmont.chile.core.datatypes.FormatStrings;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author krivopustov
 * @version $Id$
 */
public class BooleanDatatype implements Datatype<Boolean> {

	public static String NAME = "boolean";

    @Nonnull
    @Override
	public String format(Boolean value) {
		return value == null ? "" : Boolean.toString(value);
	}

    @Nonnull
    @Override
    public String format(Boolean value, Locale locale) {
        if (value == null)
            return "";

        FormatStrings formatStrings = Datatypes.getFormatStrings(locale);
        if (formatStrings == null)
            return format(value);

        return value ? formatStrings.getTrueString() : formatStrings.getFalseString();
    }

    @Override
	public Class getJavaClass() {
		return Boolean.class;
	}

    @Override
	public String getName() {
		return NAME;
	}

    @Override
	public int getSqlType() {
		return Types.BOOLEAN;
	}

    protected Boolean parse(@Nullable String value, String trueString, String falseString) throws ParseException {
        if (!StringUtils.isBlank(value)) {
            String lowerCaseValue = StringUtils.lowerCase(value);
            if (trueString.equals(lowerCaseValue))
                return true;
            if (falseString.equals(lowerCaseValue))
                return false;
            throw new ParseException(String.format("Can't parse '%s'", value), 0);
        }
        return null;
    }

    @Override
    public Boolean parse(String value) throws ParseException {
        return parse(value, "true", "false");
    }

    @Override
    public Boolean parse(String value, Locale locale) throws ParseException {
        if (StringUtils.isBlank(value))
            return null;

        FormatStrings formatStrings = Datatypes.getFormatStrings(locale);
        if (formatStrings == null)
            return parse(value);

        return parse(value, formatStrings.getTrueString(), formatStrings.getFalseString());
    }

    @Override
	public Boolean read(ResultSet resultSet, int index) throws SQLException {
		Boolean value = resultSet.getBoolean(index);
		return resultSet.wasNull() ? null : value;
	}

    @Override
	public void write(PreparedStatement statement, int index, Boolean value) throws SQLException {
		if (value == null) {
			statement.setString(index, null);
		} else {
			statement.setBoolean(index, value);
		}
	}

    @Override
    public String toString() {
        return NAME;
    }
}