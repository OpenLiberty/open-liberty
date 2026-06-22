/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.classloading.trace.fat;

import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_EAR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to verify classloading trace for library classes
 * Uses complex EAR with multiple modules (WARs, JARs, EJBs, RARs) from FATSuite
 */
@RunWith(FATRunner.class)
public class LibraryClassLoadingTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "traceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Export complex EAR to server
        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);

        // Start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {

            server.stopServer("SRVE9967W");
        }
    }

    /**
     * Test that verifies classloading trace for loading a library class
     *
     * This test:
     * 1. Invokes the servlet which loads TestLibraryClass from the library
     * 2. Waits for the trace to be written
     * 3. Verifies that trace messages show the library class being loaded
     */
    @Test
    public void testLibraryClassLoadingTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLibraryClass");

        String expectedClassName = "io.openliberty.classloading.trace.lib.TestLibraryClass";

        String traceEntry = server.waitForStringInTrace(expectedClassName);
        assertNotNull("Should find classloading trace for " + expectedClassName + " in trace file",
                      traceEntry);

        // Verify it's actually a classloading trace
        assertTrue("Trace entry should be from classloading component",
                   traceEntry.contains("classloading") || traceEntry.contains("ClassLoading"));

        System.out.println("SUCCESS: Found classloading trace for " + expectedClassName + " in trace file");
    }

    /**
     * Test that verifies classloading trace for TEST_LIB1_JAR classes
     * Tests loading of Lib1 and API classes (API_A1, API_B1, API_C1) from module
     */
    @Test
    public void testLib1ClassLoadingTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        // Verify Lib1 class loading
        String lib1ClassName = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String lib1TraceEntry = server.waitForStringInTrace(lib1ClassName);
        assertNotNull("Should find classloading trace for " + lib1ClassName + " in trace file",
                      lib1TraceEntry);

        // Verify API classes loading
        String[] apiClasses = {
            "test.bundle.api1.a.API_A1",
            "test.bundle.api1.b.API_B1",
            "test.bundle.api1.c.API_C1"
        };

        for (String apiClass : apiClasses) {
            String apiTraceEntry = server.waitForStringInTrace(apiClass);
            assertNotNull("Should find classloading trace for " + apiClass + " in trace file",
                          apiTraceEntry);
        }

        System.out.println("SUCCESS: Found classloading traces for TEST_LIB1_JAR classes in trace file");
    }

    /**
     * Test that verifies classloading trace for TEST_LIB2_JAR classes
     * Tests loading of Lib2 and API classes (API_A2, API_B2, API_C2) from EAR lib/
     */
    @Test
    public void testLib2ClassLoadingTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib2Classes");

        // Verify Lib2 class loading
        String lib2ClassName = "io.openliberty.classloading.classpath.test.lib2.Lib2";
        String lib2TraceEntry = server.waitForStringInTrace(lib2ClassName);
        assertNotNull("Should find classloading trace for " + lib2ClassName + " in trace file",
                      lib2TraceEntry);

        // Verify API classes loading
        String[] apiClasses = {
            "test.bundle.api2.a.API_A2",
            "test.bundle.api2.b.API_B2",
            "test.bundle.api2.c.API_C2"
        };

        for (String apiClass : apiClasses) {
            String apiTraceEntry = server.waitForStringInTrace(apiClass);
            assertNotNull("Should find classloading trace for " + apiClass + " in trace file",
                          apiTraceEntry);
        }

        System.out.println("SUCCESS: Found classloading traces for TEST_LIB2_JAR classes in trace file");
    }

    /**
     * Test that verifies classloading trace for TEST_EJB1_JAR classes
     * Tests loading of EjbLib1 from the EJB module
     */
    @Test
    public void testEjbClassLoadingTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        String ejbClassName = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
        String ejbTraceEntry = server.waitForStringInTrace(ejbClassName);
        assertNotNull("Should find classloading trace for " + ejbClassName + " in trace file",
                      ejbTraceEntry);

        assertTrue("Trace entry should be from classloading component",
                   ejbTraceEntry.contains("classloading") || ejbTraceEntry.contains("ClassLoading"));

        System.out.println("SUCCESS: Found classloading trace for TEST_EJB1_JAR classes in trace file");
    }

    /**
     * Test that verifies classloading trace for TEST_RAR1_RAR classes
     * Tests loading of RarLib1, RarLib2, Lib12, and Lib17 from the RAR module
     */
    @Test
    public void testRarClassLoadingTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadRarClasses");

        // Verify RAR library classes loading
        String[] rarClasses = {
            "io.openliberty.classloading.classpath.test.rar1.RarLib1",
            "io.openliberty.classloading.classpath.test.rar1.RarLib2",
            "io.openliberty.classloading.classpath.test.lib12.Lib12",
            "io.openliberty.classloading.classpath.test.lib17.Lib17"
        };

        for (String rarClass : rarClasses) {
            String rarTraceEntry = server.waitForStringInTrace(rarClass);
            assertNotNull("Should find classloading trace for " + rarClass + " in trace file",
                          rarTraceEntry);
        }

        System.out.println("SUCCESS: Found classloading traces for TEST_RAR1_RAR classes in trace file");
    }
}