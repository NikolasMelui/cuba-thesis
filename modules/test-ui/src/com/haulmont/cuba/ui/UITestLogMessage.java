/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.ui;

import java.util.*;
import java.util.regex.*;

/**
 * <p>$Id$</p>
 *
 * @author artamonov
 */
public class UITestLogMessage {

    public static enum Level {
        INFO,
        DEBUG,
        STEP,
        ERROR,
        LOG,
        CONTENT
    }

    private Level messageLevel;
    private StringBuilder content = new StringBuilder();
    private String message;

    private static Map<Pattern, Level> logLevels = new HashMap<Pattern, Level>();

    static {
        logLevels.put(Pattern.compile("(\\[info\\] )(.*)"), Level.INFO);
        logLevels.put(Pattern.compile("(\\[log\\] )(.*)"), Level.LOG);
        logLevels.put(Pattern.compile("(\\[step\\] )(.*)"), Level.STEP);
        logLevels.put(Pattern.compile("(\\[debug\\] )(.*)"), Level.DEBUG);
        logLevels.put(Pattern.compile("(\\[error\\] )(.*)"), Level.ERROR);
        logLevels.put(Pattern.compile("(\\| )(.*)"), Level.CONTENT);
    }

    public static UITestLogMessage parse(String logEntry) {
        boolean find = false;

        String message = logEntry;
        Level level = Level.CONTENT;

        Iterator<Pattern> patternIterator = logLevels.keySet().iterator();
        while (patternIterator.hasNext() && (!find)) {
            Pattern pattern = patternIterator.next();
            Matcher matcher = pattern.matcher(logEntry);
            if (matcher.matches()) {
                message = matcher.group(2);
                level = logLevels.get(pattern);
                find = true;
            }
        }
        return new UITestLogMessage(level, message);
    }

    public UITestLogMessage(Level messageLevel, String message) {
        this.messageLevel = messageLevel;
        this.message = message;
    }

    public Level getMessageLevel() {
        return messageLevel;
    }

    public String getMessage() {
        return message;
    }

    public StringBuilder getContent() {
        return content;
    }
}
