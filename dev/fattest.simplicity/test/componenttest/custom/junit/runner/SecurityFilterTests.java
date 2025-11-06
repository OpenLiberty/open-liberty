/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.custom.junit.runner;

import static componenttest.annotation.SkipForSecurity.SEMERU;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForSecurity;
import componenttest.app.FATServlet;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import jakarta.servlet.annotation.WebServlet;

/**
 * Tests for the {@link SecurityFilter}
 */
public class SecurityFilterTests {

    private static final CompoundFilter filter = new CompoundFilter(new Filter[] { new SecurityFilter() });

    @BeforeClass
    public static void setup() {
        System.setProperty("sec.prop.true", "true");
        System.setProperty("sec.prop.false", "false");
        System.setProperty("sec.prop.value", "value");
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty("sec.prop.true");
        System.clearProperty("sec.prop.false");
        System.clearProperty("sec.prop.value");
    }

    private static Description createDescriptionForTest(Class<?> testClass, String testMethod) throws Exception {
        Annotation[] annos = testClass.getDeclaredMethod(testMethod).getAnnotations();
        return Description.createTestDescription(testClass, testMethod, annos);
    }

    private static Description createDescriptionForSyntheticTest(Class<?> testClass, Class<?> testServlet, String testMethod) throws Exception {
        SyntheticServletTest syth = new SyntheticServletTest( //
                        testServlet, //
                        testClass.getField("NOOP"), //
                        "FAKE/QUERY", //
                        testServlet.getMethod(testMethod));

        return Description.createTestDescription(testClass, syth.getName(), syth.getAnnotations());
    }

    // -----

    // Test class where all tests should be run because the property is false
    @SkipForSecurity(property = "sec.prop.false")
    public static class RunAllTestsInClass extends FATServletClient {
        @Test
        public void testShouldBeRun() {}
    }

    // Test class where all tests should be skipped because the property is true
    @SkipForSecurity(property = "sec.prop.true")
    public static class SkipAllTestsInClass extends FATServletClient {
        @Test
        public void testShouldBeSkipped() {}
    }

    @Test
    public void testSecurityFilterWorksOnTestClient() throws Exception {
        Description test1 = createDescriptionForTest(RunAllTestsInClass.class, "testShouldBeRun");
        assertTrue("Test not have run since system property was false.",
                   filter.shouldRun(test1));

        Description test2 = createDescriptionForTest(SkipAllTestsInClass.class, "testShouldBeSkipped");
        assertFalse("Test should not have run since system property was true.",
                    filter.shouldRun(test2));
    }

    // -----
    public static class FilterTestsInClass extends FATServletClient {
        @Test
        @SkipForSecurity(property = "sec.prop.false")
        public void testShouldBeRun() {}

        @Test
        @SkipForSecurity(property = "sec.prop.true")
        public void testShouldBeSkipped() {}
    }

    @Test
    public void testSecurityFilterWorksOnTestClientMethods() throws Exception {
        Description test1 = createDescriptionForTest(FilterTestsInClass.class, "testShouldBeRun");
        assertTrue("Test should have run since system property was false.",
                   filter.shouldRun(test1));

        Description test2 = createDescriptionForTest(FilterTestsInClass.class, "testShouldBeSkipped");
        assertFalse("Test should not have run since system property was true.",
                    filter.shouldRun(test2));
    }

    // -----
    public static class TestsInServlet extends FATServletClient {
        public static LibertyServer NOOP;
    }

    @WebServlet("*/")
    @SkipForSecurity(property = "sec.prop.false")
    public static class RunAllTestsInServlet extends FATServlet {
        @Test
        public void testShouldBeRun() {}
    }

    @WebServlet("*/")
    @SkipForSecurity(property = "sec.prop.true")
    public static class SkipAllTestsInServlet extends FATServlet {
        @Test
        public void testShouldBeSkipped() {}
    }

    @Test
    public void testSecurityFilterWorksOnTestServlet() throws Exception {
        Description test1 = createDescriptionForSyntheticTest(TestsInServlet.class, RunAllTestsInServlet.class, "testShouldBeRun");
        assertTrue("Test should have run since system property was false.",
                   filter.shouldRun(test1));

        Description test2 = createDescriptionForSyntheticTest(TestsInServlet.class, SkipAllTestsInServlet.class, "testShouldBeSkipped");
        assertFalse("Test should not have run since system property was true.",
                    filter.shouldRun(test2));
    }

    // -----
    @WebServlet("*/")
    public static class filterTestsInServlet extends FATServlet {
        @Test
        @SkipForSecurity(property = "sec.prop.false")
        public void testShouldBeRun() {}

        @Test
        @SkipForSecurity(property = "sec.prop.true")
        public void testShouldBeSkipped() {}
    }

    @Test
    public void testSecurityFilterWorksOnTestServletMethods() throws Exception {
        Description test1 = createDescriptionForSyntheticTest(TestsInServlet.class, filterTestsInServlet.class, "testShouldBeRun");
        assertTrue("Test should have run since system property was false.",
                   filter.shouldRun(test1));

        Description test2 = createDescriptionForSyntheticTest(TestsInServlet.class, filterTestsInServlet.class, "testShouldBeSkipped");
        assertFalse("Test should not have run since system property was true.",
                    filter.shouldRun(test2));
    }

    // ----
    public static class MissingExtendsClass {
        @Test
        @SkipForSecurity(property = "sec.prop.value=value",
                         runtimeName = "FAKERUNTIME")
        public void testErrorMissingExtends() {}
    }

    public static class MissingServerFieldClass extends FATServletClient {
        @Test
        @SkipForSecurity(property = "sec.prop.value=value",
                         runtimeName = "FAKERUNTIME")
        public void testErrorNoLibertyServer() {}
    }

    @Test
    public void testErrorPaths() throws Exception {
        Description test1 = createDescriptionForTest(MissingExtendsClass.class, "testErrorMissingExtends");
        try {
            filter.shouldRun(test1);
            fail("Should not have been able to filter test when the test class did not extend FATServletClient");
        } catch (IllegalStateException e) {
            //expected
        } catch (Exception e) {
            fail("Caught wrong expection, expected IllegalStateExeption, but was" + e.getMessage());
        }

        Description test2 = createDescriptionForTest(MissingServerFieldClass.class, "testErrorNoLibertyServer");
        try {
            filter.shouldRun(test2);
            fail("Should not have been able to filter test when the test class did not have a Liberty server configured");
        } catch (IllegalStateException e) {
            //expected
        } catch (Exception e) {
            fail("Caught wrong expection, expected IllegalStateExeption, but was" + e.getMessage());
        }
    }

    // ----
    public static class RuntimeTestsInClass extends FATServletClient {
        @Server("FAKE.SERVER")
        public static LibertyServer NOOP;

        @Test
        @SkipForSecurity(property = "sec.prop.value=value",
                         runtimeName = SEMERU)
        public void testShouldNotRunOnSemeru() {}

        @Test
        @SkipForSecurity(property = "sec.prop.value=value",
                         runtimeName = "FAKERUNTIME")
        public void testShouldRunOnFakeRuntime() {}
    }

    @Test
    public void testGetRuntimeForServer() throws Exception {
        try (MockedStatic<LibertyServerFactory> lsf = libertyServerRegistry()) {
            Description test1 = createDescriptionForTest(RuntimeTestsInClass.class, "testShouldNotRunOnSemeru");
            if (JavaInfo.forCurrentVM().runtimeName().contains(SEMERU)) {
                assertFalse("Test should not have run since system property was value, and current VM is semeru.",
                            filter.shouldRun(test1));
            } else {
                assertTrue("Test should have run since system property was value, and current VM is not semeru.",
                           filter.shouldRun(test1));
            }

            Description test2 = createDescriptionForTest(RuntimeTestsInClass.class, "testShouldRunOnFakeRuntime");
            assertTrue("Test should not have run since system property was value, and current VM is not FAKERUNTIME.",
                       filter.shouldRun(test2));
        }
    }

    /**
     * Must be called in a try-with-resources block
     */
    public static MockedStatic<LibertyServerFactory> libertyServerRegistry() {
        // Create a mocked instance of a liberty server
        LibertyServer instance = Mockito.mock(LibertyServer.class);
        System.out.println("java.home is: " + System.getProperty("java.home"));

        // with the same JAVA_HOME env variable as the current system
        Properties singleton = new Properties();
        singleton.setProperty("JAVA_HOME", System.getProperty("java.home"));
        when(instance.getServerEnv()).thenReturn(singleton);

        // Mock the LibertyServerFactory.getLibertyServer() static method
        // to return the mocked server
        MockedStatic<LibertyServerFactory> mockLibertyServerFactory = Mockito.mockStatic(LibertyServerFactory.class);
        mockLibertyServerFactory //
                        .when(() -> LibertyServerFactory.getLibertyServer(anyString()))
                        .thenReturn(instance);

        return mockLibertyServerFactory;
    }

    // ----
    public static class MultiplePropertyClass extends FATServletClient {
        @Server("FAKE.SERVER")
        public static LibertyServer NOOP;

        @Test
        @SkipForSecurity(property = { "sec.prop.true", "sec.prop.value=value" })
        public void testShouldNotRunMultipleTrue() {}

        @Test
        @SkipForSecurity(property = { "sec.prop.false", "sec.prop.value=value" })
        public void testShouldNotRunOneFalseOneTrue() {}

        @Test
        @SkipForSecurity(property = { "sec.prop.false", "sec.prop.value=notvalue" })
        public void testShouldNotRunMultipleFalse() {}
    }

    @Test
    public void testSecurityFilterMultipleProperties() throws Exception {
        Description test1 = createDescriptionForTest(MultiplePropertyClass.class, "testShouldNotRunMultipleTrue");
        assertFalse("Test should not have run since all system properties were true.",
                    filter.shouldRun(test1));

        Description test2 = createDescriptionForTest(MultiplePropertyClass.class, "testShouldNotRunOneFalseOneTrue");
        assertTrue("Test should have run since one system property was false.",
                   filter.shouldRun(test2));

        Description test3 = createDescriptionForTest(MultiplePropertyClass.class, "testShouldNotRunMultipleFalse");
        assertTrue("Test should have run since all system properties were false.",
                   filter.shouldRun(test2));
    }
}
