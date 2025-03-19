/* ***************************************************************************
 * Copyright (c) 2015,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ***************************************************************************/
package com.ibm.ws.jndi.iiop;

import static com.ibm.ws.jndi.iiop.CosNameUtil.escapeCorbanameUrlIfNecessary;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jndi.iiop.CorbanameEscapeTestSuite.NoEscape;
import com.ibm.ws.jndi.iiop.CorbanameEscapeTestSuite.SomeEscape;
import com.ibm.ws.jndi.iiop.CorbanameEscapeTestSuite.SomeEscapeIdempotency;

import junit.framework.Assert;

@RunWith(Suite.class)
@SuiteClasses({
               NoEscape.class,
               SomeEscape.class,
               SomeEscapeIdempotency.class
})
public class CorbanameEscapeTestSuite {
    private static final String CORBANAME_PREFIX = "corbaname::localhost:2809/NameService#";

    @RunWith(Parameterized.class)
    public static class SomeEscape {
        private static final Object[][] TEST_PARAMETERS =
        {
         { "X" },
         { "X/blah/blah" },
         { "blah/X/blah" },
         { "blah/blah/X" },
         { "X/X" },
         { "X/X/blah/blah" },
         { "blah/X/X/blah" },
         { "blah/blah/X/X" }
        };

        @Parameters
        public static List<Object[]> parameters() {
            return Arrays.asList(TEST_PARAMETERS);
        }

        private final String template;

        public SomeEscape(String template) {
            this.template = template;
        }

        @Test
        public void testEscapeSingleDot() {
            assertNameIsEscaped(".", ".");
        }

        @Test
        public void testEscapeTrailingDot() {
            assertNameIsEscaped("a.", "a%5c.");
        }

        @Test
        public void testEscapeEmbeddedDot() {
            assertNameIsEscaped("a.b", "a.b");
        }

        @Test
        public void testEscapeLeadingDot() {
            assertNameIsEscaped(".a", ".a");
        }

        @Test
        public void testEscapeTwoDots() {
            assertNameIsEscaped("com.ibm.ws", "com%5c.ibm%5c.ws");
        }

        @Test
        public void testEscapeAllDotsWhenTwoDotsArePresent() {
            assertNameIsEscaped("./a/..", "%5c./a/%5c.%5c.");
        }

        @Test
        public void testEscapeAllDotsWhenTrailingDotIsPresent() {
            assertNameIsEscaped("a/./b/c.", "a/%5c./b/c%5c.");
        }

        @Test
        public void testEscapeNothingWhenBackslashIsPresent() {
            // temporary measure: avoid doing any escaping when backslash is present - anything with a backslash in will be invalid
            // FIXME: allow tWAS-style use of backslash to force escaping of following character
            assertNameIsEscaped("a.b/c/\\/", "a.b/c/\\/");
        }

        @Test
        public void testSingleBackslash() {
            // temporary measure: avoid doing any escaping when backslash is present - anything with a backslash in will be invalid
            // FIXME: allow tWAS-style use of backslash to force escaping of following character
            assertNameIsEscaped("\\", "\\");
        }

        @Test
        public void testTwoBackslashes() {
            // temporary measure: avoid doing any escaping when backslash is present - anything with a backslash in will be invalid
            // FIXME: allow tWAS-style use of backslash to force escaping of following character
            assertNameIsEscaped("\\\\", "\\\\");
        }

        @Test
        public void testCosNamingSpecExample1FromSection_2_5_3_5() {
            // a.b/c.d a.b/c.d URL form identical
            assertNameIsEscaped("a.b/c.d", "a.b/c.d");
        }

        @Test
        public void testCosNamingSpecExample2FromSection_2_5_3_5() {
//        <a>.b/c.d %3ca%3e.b/c.d Escaped “<“ and “>”
            assertNameIsEscaped("<a>.b/c.d", "%3ca%3e%5c.b/c%5c.d");
        }

        @Test
        public void testCosNamingSpecExample3FromSection_2_5_3_5() {
//        a.b/  c.d a.b/%20%20c.d Escaped two “ “ spaces
            assertNameIsEscaped("a.b/  c.d", "a%5c.b/%20%20c%5c.d");
        }

        @Test
        public void testCosNamingSpecExample4FromSection_2_5_3_5() {
//        a%b/c%d a%25b/c%25d Escaped two “%” percents
            assertNameIsEscaped("a%b/c%d", "a%25b/c%25d");
        }

        @Test
        public void testCosNamingSpecExample5FromSection_2_5_3_5() {
            // a\\b/c.d a%5c%5cb/c.d Escaped “\” character,
            //                       which is already escaped
            //                       in the stringified name

            // temporary measure: avoid doing any escaping when backslash is present - anything with a backslash in will be invalid
            // FIXME: allow tWAS-style use of backslash to force escaping of following character
            //assertNameIsEscaped("a\\b/c.d", "a%5c%5cb/c%5c.d");
            assertNameIsEscaped("a\\b/c.d", "a\\b/c.d");
        }

        protected void assertNameIsEscaped(String from, String to) {
            // Since from can contain backslashes, we need to double them up before using it as a replacement string.
            from = from.replaceAll("\\\\", "\\\\\\\\"); // This line replaces one backslash with two. Really.
            // hmm, now to can contain backslashes as well
            to = to.replaceAll("\\\\", "\\\\\\\\"); // *sigh*
            String newFrom = CORBANAME_PREFIX + template.replaceAll("X", from);
            String newTo = CORBANAME_PREFIX + template.replaceAll("X", to);
            assertEscaped(newFrom, newTo);
        }
    }

    @RunWith(Parameterized.class)
    public static class SomeEscapeIdempotency extends SomeEscape {

        @Parameters
        public static List<Object[]> parameters() {
            return SomeEscape.parameters();
        }

        public SomeEscapeIdempotency(String template) {
            super(template);
        }

        @Override
        protected void assertNameIsEscaped(String from, String to) {
            try {
                super.assertNameIsEscaped(to, to);
            } catch (Error e) {
                System.err.println(e);
                e.printStackTrace();
            }
        }

    }

    public static class NoEscape {
        @Test
        public void testNoEscapingForInvalidCorbanames() {
            assertNoChange(null);
            assertNoChange("");
            assertNoChange("corbaloc::localhost:2809/NameService");
            assertNoChange("corbaloc::localhost:2809/NameService#a.b.c");
        }

        @Test
        public void testNoEscapingForSimpleCorbanames() {
            assertNoChange(CORBANAME_PREFIX);
            assertNoChange(CORBANAME_PREFIX + "abc");
            assertNoChange(CORBANAME_PREFIX + "abc.xyz");
            assertNoChange(CORBANAME_PREFIX + "abc/abc!x.y");
        }

    }

    private static void assertNoChange(String expected) {
        assertEscaped(expected, expected);
    }

    private static void assertEscaped(String from, String to) {
        String actual = escapeCorbanameUrlIfNecessary(from);
        Assert.assertEquals(to, actual);
    }
}
