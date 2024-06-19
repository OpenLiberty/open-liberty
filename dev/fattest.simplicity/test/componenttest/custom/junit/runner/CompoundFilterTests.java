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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import jakarta.servlet.annotation.WebServlet;

/**
 *
 */
public class CompoundFilterTests {
    private static final Filter[] testFiltersToApply = new Filter[] {
                                                                      new TestModeFilter(),
                                                                      new TestNameFilter(),
                                                                      new FeatureFilter(),
                                                                      new SystemPropertyFilter(),
                                                                      new JavaLevelFilter(),
                                                                      new CheckpointSupportFilter()
    };

    @Mode(TestMode.FULL) //Should be superseeded by servlet or method annotation
    @RunWith(FATRunner.class)
    public static class TestClassFull {
        public static LibertyServer NOOP;

        @Test
        public void dummyTestForRunner() {}
    }

    @WebServlet("*/")
    public static class TestServletLiteMethod extends FATServlet {

        @Mode(TestMode.LITE)
        public void testMethod() {}
    }

    @Test
    public void testMethodFilterSuperseedsTestClassFilter() throws Exception {
        SyntheticServletTest testMethod = new SyntheticServletTest(TestServletLiteMethod.class, //
                        TestClassFull.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletLiteMethod.class.getMethod("testMethod"));

        //Replicate FATRunner.describeChild()
        Description testDescription = Description.createTestDescription(TestClassFull.class, testMethod.getName(), testMethod.getAnnotations());

        CompoundFilter filter = new CompoundFilter(testFiltersToApply);

        assertTrue(filter.shouldRun(testDescription));
    }

    @Mode(TestMode.LITE)
    @WebServlet("*/")
    public static class TestServletLite extends FATServlet {

        public void testMethod() {}
    }

    @Test
    public void testServletFilterSuperseedsTestClassFilter() throws Exception {
        SyntheticServletTest testMethod = new SyntheticServletTest(TestServletLite.class, //
                        TestClassFull.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletLite.class.getMethod("testMethod"));

        //Replicate FATRunner.describeChild()
        Description testDescription = Description.createTestDescription(TestClassFull.class, testMethod.getName(), testMethod.getAnnotations());

        CompoundFilter filter = new CompoundFilter(testFiltersToApply);

        assertTrue(filter.shouldRun(testDescription));
    }

    @RunWith(FATRunner.class)
    public static class TestClass {
        public static LibertyServer NOOP;

        @Test
        public void dummyTestForRunner() {}
    }

    @Mode(TestMode.FULL) //Should be superseeded by method annotation
    @WebServlet("*/")
    public static class TestServletFullWithLiteMethod extends FATServlet {

        @Mode(TestMode.LITE)
        public void testMethod() {}
    }

    @Test
    public void testServletMethodFilterSuperseedsTestServletFilter() throws Exception {
        SyntheticServletTest testMethod = new SyntheticServletTest(TestServletFullWithLiteMethod.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestServletFullWithLiteMethod.class.getMethod("testMethod"));

        //Replicate FATRunner.describeChild()
        Description testDescription = Description.createTestDescription(TestClass.class, testMethod.getName(), testMethod.getAnnotations());

        CompoundFilter filter = new CompoundFilter(testFiltersToApply);

        assertTrue(filter.shouldRun(testDescription));
    }

}
