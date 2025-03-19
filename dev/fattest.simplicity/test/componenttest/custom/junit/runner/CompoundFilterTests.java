/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.custom.junit.runner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import jakarta.servlet.annotation.WebServlet;

public class CompoundFilterTests {

    private static final CompoundFilter filter = new CompoundFilter(new Filter[] { new TestModeFilter() });

    @Mode(TestMode.FULL) //Should be superseded by servlet or method annotation
    public static class TestClassFull {
        public static LibertyServer NOOP;
    }

    @WebServlet("*/")
    public static class TestServletLiteMethod extends FATServlet {

        @Mode(TestMode.LITE)
        public void liteTest() {}

        public void defaultTest() {}

    }

    @Test
    public void testMethodFilterSupersedesTestClassFilter() throws Exception {
        SyntheticServletTest liteTest = new SyntheticServletTest(TestServletLiteMethod.class, //
                        TestClassFull.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletLiteMethod.class.getMethod("liteTest"));

        //Replicate FATRunner.describeChild()
        Description liteTestDescription = Description.createTestDescription(TestClassFull.class, liteTest.getName(), liteTest.getAnnotations());
        assertTrue(filter.shouldRun(liteTestDescription));

        SyntheticServletTest defaultTest = new SyntheticServletTest(TestServletLiteMethod.class, //
                        TestClassFull.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletLiteMethod.class.getMethod("defaultTest"));

        //Replicate FATRunner.describeChild()
        Description defaultTestDescription = Description.createTestDescription(TestClassFull.class, defaultTest.getName(), defaultTest.getAnnotations());
        assertFalse(filter.shouldRun(defaultTestDescription));
    }

    @Mode(TestMode.LITE)
    @WebServlet("*/")
    public static class TestServletLite extends FATServlet {

        public void testMethod() {}

    }

    @Test
    public void testServletFilterSupersedesTestClassFilter() throws Exception {
        SyntheticServletTest testMethod = new SyntheticServletTest(TestServletLite.class, //
                        TestClassFull.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletLite.class.getMethod("testMethod"));

        //Replicate FATRunner.describeChild()
        Description testDescription = Description.createTestDescription(TestClassFull.class, testMethod.getName(), testMethod.getAnnotations());
        assertTrue(filter.shouldRun(testDescription));
    }

    public static class TestClass {
        public static LibertyServer NOOP;
    }

    @Mode(TestMode.FULL) //Should be superseded by method annotation
    @WebServlet("*/")
    public static class TestServletFullWithLiteMethod extends FATServlet {

        @Mode(TestMode.LITE)
        public void liteMethod() {}

        public void defaultMethod() {}
    }

    @Test
    public void testServletMethodFilterSupersedesTestServletFilter() throws Exception {
        SyntheticServletTest liteMethod = new SyntheticServletTest(TestServletFullWithLiteMethod.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletFullWithLiteMethod.class.getMethod("liteMethod"));

        //Replicate FATRunner.describeChild()
        Description liteTestDescription = Description.createTestDescription(TestClass.class, liteMethod.getName(), liteMethod.getAnnotations());
        assertTrue(filter.shouldRun(liteTestDescription));

        SyntheticServletTest defaultMethod = new SyntheticServletTest(TestServletFullWithLiteMethod.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletFullWithLiteMethod.class.getMethod("defaultMethod"));

        //Replicate FATRunner.describeChild()
        Description defaultTestDescription = Description.createTestDescription(TestClass.class, defaultMethod.getName(), defaultMethod.getAnnotations());
        assertFalse(filter.shouldRun(defaultTestDescription));
    }

}
