/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.annotation.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.app.FATServlet;
import componenttest.topology.impl.LibertyServer;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
public class TestServletProcessorTests {
    public static class NOOPTestServlet extends FATServlet {}

    public static class NonStaticServletTestClass {
        @TestServlet(servlet = NOOPTestServlet.class)
        public LibertyServer NOOP;
    }

    @Test
    public void testNotStaticServer() {
        try {
            TestServletProcessor.getServletTests(new TestClass(NonStaticServletTestClass.class));
            fail("Annotated field must be static");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().endsWith("' must be static."));
        }
    }

    public static class PrivateServletTestClass {
        @TestServlet(servlet = NOOPTestServlet.class)
        private static LibertyServer NOOP;
    }

    @Test
    public void testPrivateServer() {
        try {
            TestServletProcessor.getServletTests(new TestClass(PrivateServletTestClass.class));
            fail("Annotated field must be public");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().endsWith("' must be public."));
        }
    }

    public static class NonServerServletTestClass {
        @TestServlet(servlet = NOOPTestServlet.class)
        public static String NOOP;
    }

    @Test
    public void testNonServer() {
        try {
            TestServletProcessor.getServletTests(new TestClass(NonServerServletTestClass.class));
            fail("Annotated field must be LibertyServer");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("' must be of type or subtype of "));
        }
    }

    public static class InvalidServletTestClass {
        @TestServlet(servlet = InvalidTestServlet.class)
        public static LibertyServer NOOP;

        public static class InvalidTestServlet {}
    }

    @Test
    public void testHttpServletCheck() {
        try {
            TestServletProcessor.getServletTests(new TestClass(InvalidServletTestClass.class));
            fail("Non-servlet class should not be valid for @TestServlet servlet field");
        } catch (IllegalArgumentException e) {
            //pass
            System.out.println("Caught: " + e.toString());
        }
    }

    public static class ServletTestClass {
        @TestServlet(servlet = ValidTestServlet.class)
        public static LibertyServer NOOP;

        @WebServlet("/*")
        public static class ValidTestServlet extends FATServlet {
            @Before
            public void setup() {}

            @Test
            public void test1() {}

            @Test
            public void test2() {}

            public void nonTest() {}

            @After
            public void teardown() {}
        }
    }

    @Test
    public void testProcessorCount() {
        List<FrameworkMethod> methods = TestServletProcessor.getServletTests(new TestClass(ServletTestClass.class));
        assertEquals(6, methods.size());
        assertEquals("setup", methods.get(0).getMethod().getName());
        assertEquals("test1", methods.get(1).getMethod().getName());
        assertEquals("teardown", methods.get(2).getMethod().getName());
        assertEquals("setup", methods.get(3).getMethod().getName());
        assertEquals("test2", methods.get(4).getMethod().getName());
        assertEquals("teardown", methods.get(5).getMethod().getName());
    }

    public static class ManyToManyServletTestClass {
        @TestServlet(servlet = ValidTestServlet1.class)
        public static LibertyServer NOOP1;

        @TestServlet(servlet = ValidTestServlet2.class)
        public static LibertyServer NOOP2;

        @WebServlet("/*")
        public static class ValidTestServlet1 extends FATServlet {
            @Test
            public void test1_1() {}

            @Test
            public void test1_2() {}
        }

        @WebServlet("/*")
        public static class ValidTestServlet2 extends FATServlet {
            @Test
            public void test2_1() {}

            @Test
            public void test2_2() {}
        }
    }

    @Test
    public void testProcessorCountManyToMany() {
        List<FrameworkMethod> methods = TestServletProcessor.getServletTests(new TestClass(ManyToManyServletTestClass.class));
        assertEquals(4, methods.size());
        //Order not guarenteed
        List<String> expectedNames = Arrays.asList("test1_1", "test1_2", "test2_1", "test2_2");
        for (FrameworkMethod method : methods) {
            assertTrue(expectedNames.contains(method.getMethod().getName()));
        }
    }

    public static class ManyToOneServletTestClass {
        @TestServlets({
                        @TestServlet(servlet = ValidTestServlet1.class),
                        @TestServlet(servlet = ValidTestServlet2.class)
        })
        public static LibertyServer NOOP;

        @WebServlet("/*")
        public static class ValidTestServlet1 extends FATServlet {
            @Test
            public void test1_1() {}

            @Test
            public void test1_2() {}
        }

        @WebServlet("/*")
        public static class ValidTestServlet2 extends FATServlet {
            @Test
            public void test2_1() {}

            @Test
            public void test2_2() {}
        }
    }

    @Test
    public void testProcessorCountManyToOne() {
        List<FrameworkMethod> methods = TestServletProcessor.getServletTests(new TestClass(ManyToOneServletTestClass.class));
        //Order not guarenteed
        List<String> expectedNames = Arrays.asList("test1_1", "test1_2", "test2_1", "test2_2");
        for (FrameworkMethod method : methods) {
            assertTrue(expectedNames.contains(method.getMethod().getName()));
        }
    }
}