/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class LDAPUtilsTest {
    private static final String[] TEXT_FORMATS = { "%s", "%sfoo", "foo%s", "foo%sbar" };

    private static void assertEscapeFilter(String reason, String expected, String input) {
        String actual = LDAPUtils.escapeLDAPFilterTerm(input);
        assertEquals(reason, expected, actual);
    }

    private static void testEscapeFilterChar(char c, String expectedEscape) {
        final String reason = "'" + c + "' should be escaped to \"" + expectedEscape + "\"";
        for (String format : TEXT_FORMATS) {
            String expected = String.format(format, expectedEscape);
            String input = String.format(format, c);
            assertEscapeFilter(reason, expected, input);
        }
    }

    @Test
    public void testEscapeFilterTerm() {
        assertEscapeFilter("null string should not be escaped", null, null);
        assertEscapeFilter("empty string should not be escaped", "", "");
        testEscapeFilterChar('a', "a");
        testEscapeFilterChar('*', "\\2a");
        testEscapeFilterChar('(', "\\28");
        testEscapeFilterChar(')', "\\29");
        testEscapeFilterChar('\\', "\\5c");
        testEscapeFilterChar('\u00a3', "\\c2\\a3"); // the pound sign
    }

}
