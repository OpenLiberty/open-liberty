/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import jakarta.servlet.annotation.WebServlet;

public class SyntheticServletTests {

    public static class TestClass {
        public static LibertyServer NOOP;
    }

    @WebServlet("*/")
    public static class TestFilterByMethodServlet extends FATServlet {

        @Mode(TestMode.FULL)
        public void fullTest() {}

        @Mode(TestMode.LITE)
        public void liteTest() {}

        public void defaultTest() {}
    }

    @Test
    public void testSyntheticTestHonorsMethodAnnotations() throws Exception {
        SyntheticServletTest fullTest = new SyntheticServletTest(TestFilterByMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByMethodServlet.class.getMethod("fullTest"));

        assertEquals(1, fullTest.getAnnotations().length);
        assertEquals(TestMode.FULL, fullTest.getAnnotation(Mode.class).value());

        SyntheticServletTest liteTest = new SyntheticServletTest(TestFilterByMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByMethodServlet.class.getMethod("liteTest"));

        assertEquals(1, liteTest.getAnnotations().length);
        assertEquals(TestMode.LITE, liteTest.getAnnotation(Mode.class).value());

        SyntheticServletTest defaultTest = new SyntheticServletTest(TestFilterByMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByMethodServlet.class.getMethod("defaultTest"));

        assertEquals(0, defaultTest.getAnnotations().length);
    }

    @Mode(TestMode.FULL)
    @WebServlet("*/")
    public static class TestFilterByClassAndMethodServlet extends FATServlet {

        @Mode(TestMode.FULL)
        public void fullTest() {}

        @Mode(TestMode.LITE)
        public void liteTest() {}

        public void defaultTest() {}
    }

    @Test
    public void testSyntheticTestHonorsServletAnnotations() throws Exception {
        SyntheticServletTest fullTest = new SyntheticServletTest(TestFilterByClassAndMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByClassAndMethodServlet.class.getMethod("fullTest"));

        assertEquals(1, fullTest.getAnnotations().length);
        assertEquals(TestMode.FULL, fullTest.getAnnotation(Mode.class).value());

        SyntheticServletTest liteTest = new SyntheticServletTest(TestFilterByClassAndMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByClassAndMethodServlet.class.getMethod("liteTest"));

        assertEquals(1, liteTest.getAnnotations().length);
        assertEquals(TestMode.LITE, liteTest.getAnnotation(Mode.class).value());

        SyntheticServletTest defaultTest = new SyntheticServletTest(TestFilterByClassAndMethodServlet.class, //
                        TestClass.class.getField("NOOP"), //
                        "FAKE/QUERY", //
                        TestFilterByClassAndMethodServlet.class.getMethod("defaultTest"));

        assertEquals(1, defaultTest.getAnnotations().length);
        assertEquals(TestMode.FULL, defaultTest.getAnnotation(Mode.class).value());
    }
}