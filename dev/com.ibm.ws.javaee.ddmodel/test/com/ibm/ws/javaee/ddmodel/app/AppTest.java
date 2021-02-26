/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.version.JavaEEVersion;

/**
 * Application deployment descriptor parse tests.
 */
public class AppTest extends AppTestBase {
    public static final Version[] TEST_VERSIONS = new Version[] {
                                                                  JavaEEVersion.VERSION_6_0,
                                                                  JavaEEVersion.VERSION_7_0,
                                                                  JavaEEVersion.VERSION_8_0,
                                                                  JavaEEVersion.VERSION_9_0
    };

    public static final int CMP_LT = -1;
    public static final int CMP_GT = +1;
    public static final int CMP_EQ = 0;

    public static int asCmp(int cmpResult) {
        return ((cmpResult < 0) ? CMP_LT : ((cmpResult > 0) ? CMP_GT : CMP_EQ));
    }

    public static int[][] COMPARE_VERSION_RESULTS = new int[][] {
                                                                  { CMP_EQ, CMP_LT, CMP_LT, CMP_LT },
                                                                  { CMP_GT, CMP_EQ, CMP_LT, CMP_LT },
                                                                  { CMP_GT, CMP_GT, CMP_EQ, CMP_LT },
                                                                  { CMP_GT, CMP_GT, CMP_GT, CMP_EQ }
    };

    @Test
    public void testCompareVersions() throws Exception {
        for (int v1No = 0; v1No < TEST_VERSIONS.length; v1No++) {
            Version v1 = TEST_VERSIONS[v1No];
            for (int v2No = 0; v2No < TEST_VERSIONS.length; v2No++) {
                Version v2 = TEST_VERSIONS[v2No];

                int expectedCmp = COMPARE_VERSION_RESULTS[v1No][v2No];
                int actualCmp = asCmp(v1.compareTo(v2));

                boolean matchCmp = (expectedCmp == actualCmp);
                if (!matchCmp) {
                    Assert.assertEquals(
                                        "Version [ " + v1 + " ] compared with [ " + v2 + " ]." +
                                        "  Expecting [ " + Integer.toString(expectedCmp) + " ]" +
                                        " but received [ " + Integer.toString(actualCmp) + " ]",
                                        expectedCmp, actualCmp);
                }
            }
        }
    }

    // JavaEE6 cases ...

    // Parse everything except 7.0 and 8.0 and 9.0

    @Test
    public void testEE6App12() throws Exception {
        parse(app12() + appTail());
    }

    @Test
    public void testEE6App13() throws Exception {
        parse(app13() + appTail());
    }

    @Test
    public void testEE6App14() throws Exception {
        parse(app14() + appTail());
    }

    @Test
    public void testEE6App50() throws Exception {
        parse(app50() + appTail());
    }

    @Test
    public void testEE6App60() throws Exception {
        parse(app60() + appTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6App70() throws Exception {
        parse(app70() + appTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6App80() throws Exception {
        parse(app80() + appTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6App90() throws Exception {
        parse(app90() + appTail());
    }

    // JavaEE7 cases ...

    // Parse everything except 8.0 and 9.0

    @Test
    public void testEE7App12() throws Exception {
        parse(app12() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App13() throws Exception {
        parse(app13() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App14() throws Exception {
        parse(app14() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App50() throws Exception {
        parse(app50() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App60() throws Exception {
        parse(app60() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App70() throws Exception {
        parse(app70() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7App80() throws Exception {
        parse(app80() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7App90() throws Exception {
        parse(app90() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    // JavaEE8 cases ...

    // Parse everything except 9.0.

    @Test
    public void testEE8App12() throws Exception {
        parse(app12() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App13() throws Exception {
        parse(app13() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App14() throws Exception {
        parse(app14() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App50() throws Exception {
        parse(app50() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App60() throws Exception {
        parse(app60() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App70() throws Exception {
        parse(app70() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App80() throws Exception {
        parse(app80() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE8App90() throws Exception {
        parse(app90() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    // JakartaEE9 cases ...

    // Parse everything.
    @Test
    public void testEE9App12() throws Exception {
        parse(app12() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App13() throws Exception {
        parse(app13() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App14() throws Exception {
        parse(app14() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App50() throws Exception {
        parse(app50() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App60() throws Exception {
        parse(app60() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App70() throws Exception {
        parse(app70() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App80() throws Exception {
        parse(app80() + appTail(), JavaEEVersion.VERSION_9_0);
    }

    @Test
    public void testEE9App90() throws Exception {
        parse(app90() + appTail(), JavaEEVersion.VERSION_9_0);
    }
}
