/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.security.global;

import junit.framework.TestCase;

public class IpMatcherTest extends TestCase {

    public void testValid() {
        boolean match;
        IpMatcher matcher;

        matcher = new IpMatcher("192.168.1.1");
        match = matcher.match(null);
        assertTrue(match);
        match = matcher.match("");
        assertTrue(match);
        match = matcher.match("127.0.0.1");
        assertTrue(match);
        match = matcher.match("192.168.1.1");
        assertTrue(match);
        match = matcher.match("192.168.1.2");
        assertFalse(match);

        matcher = new IpMatcher("192.168.1.*");
        match = matcher.match("192.168.1.2");
        assertTrue(match);
        match = matcher.match("192.168.1.21");
        assertTrue(match);
        match = matcher.match("192.168.2.21");
        assertFalse(match);

        matcher = new IpMatcher("192.168.1.*, 85.68.129.*");
        match = matcher.match("192.168.2.2");
        assertFalse(match);
        match = matcher.match("85.68.129.10");
        assertTrue(match);

        matcher = new IpMatcher("192.168.*.*, 85.68.129.*");
        match = matcher.match("192.168.2.2");
        assertTrue(match);
        match = matcher.match("192.10.2.2");
        assertFalse(match);
    }

    public void testInvalid() {
        boolean match;
        IpMatcher matcher;

        matcher = new IpMatcher("192.168.*");
        match = matcher.match("192.10.1.1");
        assertTrue(match);

        matcher = new IpMatcher("192.168.1.*");
        match = matcher.match("192.10.1.1.767");
        assertTrue(match);
    }
}
