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

import static io.openliberty.classloading.classpath.fat.FATSuite.BAD_CLASS_NAME;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_BAD_CLASS;
import static io.openliberty.classloading.classpath.fat.FATSuite.TEST_LIB3_JAR;
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.APP_CL;
import static io.openliberty.classloading.classpath.util.TestUtils.CLASS_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_DEFINE_FAIL;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_LOCAL_CLASSPATH;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_DEFINE_SUCCESS;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_FAIL;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_SUCCESS;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassFailTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassLoadTrace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * FAT tests that verify the per-package trace specification
 * {@code com.ibm.ws.class.load.<packageName>=all} produces correctly structured
 * trace lines independently of the broader
 * {@code com.ibm.ws.classloading.internal.*=all} component.
 */
@RunWith(FATRunner.class)
public class PackageSpecificTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "AppClassLoaderTraceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.exportToServer(server, "lib", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);
    }

    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void afterTest() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer("SRVE9967W");
            }
        } finally {
            server.restoreServerConfiguration();
        }
    }

    /**
     * Verifies that the per-package trace spec
     * {@code com.ibm.ws.class.load.<packageName>=all} produces both class-defined-successfully
     * and class-loaded-successfully output when the broader
     * {@code com.ibm.ws.classloading.internal.*=all} is NOT set.
     */
    @Test
    public void testPackageSpecificClassLoadingTrace() throws Exception {
        final String targetPackage = "io.openliberty.classloading.classpath.test.ejb1";
        final String packageTraceSpec = "*=info:com.ibm.ws.class.load." + targetPackage + "=all";
        String className1 = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
        String className2 = "io.openliberty.classloading.classpath.test.ejb1.InitBean1";
        String sourceLoc = "testEjb1.jar";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();
        logging.setTraceSpecification(packageTraceSpec);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        // Trigger a load of a class from the target package.
        // EjbLib1 is loaded by the servlet request; InitBean1 was loaded at startup (@Startup).
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        // Must have found exactly two "found on the local classpath" trace lines: one for
        // EjbLib1 (found by the EAR classloader) and one for InitBean1 (found at startup).
        String localCpRegexp = CLASS_REGEX + TRACE_LOCAL_CLASSPATH;
        List<String> localCpTraceLines = server.findStringsInLogsAndTraceUsingMark(localCpRegexp);
        assertEquals("Expected exactly 2 trace lines for " + localCpRegexp
                     + " but found: " + localCpTraceLines, 2, localCpTraceLines.size());

        for (String trace : localCpTraceLines) {
            if (trace.contains(className1)) {
                checkClassLoadTrace(trace, className1, APP_CL, null);
            } else {
                checkClassLoadTrace(trace, className2, APP_CL, null);
            }
        }

        // Must have found exactly two "was successfully defined" trace lines: one for EjbLib1
        // and one for InitBean1. The per-package trace component fired for both,
        // and the broader com.ibm.ws.classloading.internal.*=all was not enabled.
        String defineRegexp = CLASS_REGEX + TRACE_CLASS_DEFINE_SUCCESS;
        List<String> defineTraceLines = server.findStringsInLogsAndTraceUsingMark(defineRegexp);
        assertEquals("Expected exactly 2 trace lines for " + defineRegexp
                     + " but found: " + defineTraceLines, 2, defineTraceLines.size());

        for (String trace : defineTraceLines) {
            if (trace.contains(className1)) {
                checkClassLoadTrace(trace, className1, APP_CL, sourceLoc);
            } else {
                checkClassLoadTrace(trace, className2, APP_CL, sourceLoc);
            }
        }

        // Must have found exactly three "was successfully loaded" trace lines:
        // - EjbLib1 fires twice: once at EAR level (where the class was defined) and once at
        //   WAR level (where the original loadClass() call returns the result to the caller).
        // - InitBean1 fires once: loaded at startup directly by the EAR classloader, not via
        //   WAR delegation, so only the EAR-level trace is emitted.
        String loadRegexp = CLASS_REGEX + TRACE_CLASS_LOAD_SUCCESS;
        List<String> loadTraceLines = server.findStringsInLogsAndTraceUsingMark(loadRegexp);
        assertEquals("Expected exactly 3 trace lines for " + loadRegexp
                     + " but found: " + loadTraceLines, 3, loadTraceLines.size());

        for (String trace : loadTraceLines) {
            if (trace.contains(className1)) {
                checkClassLoadTrace(trace, className1, APP_CL, null);
            } else {
                checkClassLoadTrace(trace, className2, APP_CL, null);
            }
        }
    }

    /**
     * Verifies that when both the per-package trace spec
     * ({@code com.ibm.ws.class.load.<packageName>=all}) and the broader
     * {@code com.ibm.ws.classloading.internal.*=all} spec are active simultaneously, the
     * broader spec takes precedence and produces both class-defined-successfully and
     * class-loaded-successfully output for more than just the two classes in the targeted package.
     *
     * <p>The combined trace specification is set before the server starts; after triggering
     * a class load from the target package the test asserts that the number of observed
     * class-defined and class-loaded lines each exceed two, confirming that the broader
     * component is active.
     */
    @Test
    public void testPackageSpecificAndBroaderClassLoadingTraceEnabledTogether() throws Exception {
        final String targetPackage = "io.openliberty.classloading.classpath.test.ejb1";
        final String combinedTrace = "*=info:com.ibm.ws.class.load." + targetPackage + "=all:*=info:com.ibm.ws.classloading.*=all:com.ibm.ws.classloading.internal.*=all";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();
        logging.setTraceSpecification(combinedTrace);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        // EjbLib1 is loaded by the servlet request; InitBean1 was loaded at startup (@Startup).
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        // Must have found more than two "was successfully defined" trace lines: the broader
        // com.ibm.ws.classloading.*=all:com.ibm.ws.classloading.internal.*=all spec produces
        // define-success traces for all classes loaded during startup and the servlet request,
        // not just the two classes in the targeted package.
        String defineRegexp = CLASS_REGEX + TRACE_CLASS_DEFINE_SUCCESS;
        List<String> defineTraceLines = server.findStringsInLogsAndTraceUsingMark(defineRegexp);
        assertTrue("Expected more than 2 trace lines for " + defineRegexp
                     + " but found: " + defineTraceLines.size(), defineTraceLines.size() > 2);

        // Must have found more than two "was successfully loaded" trace lines for the same reason.
        String loadRegexp = CLASS_REGEX + TRACE_CLASS_LOAD_SUCCESS;
        List<String> loadTraceLines = server.findStringsInLogsAndTraceUsingMark(loadRegexp);
        assertTrue("Expected more than 2 trace lines for " + loadRegexp
                     + " but found: " + loadTraceLines.size(), loadTraceLines.size() > 2);
    }

    /**
     * Verifies that the package-specific trace spec
     * {@code com.ibm.ws.class.load.<packageName>=all} also captures class-define-failed events
     * (i.e. the per-package trace component is active during a define failure).
     */
    @Test
    public void testPackageSpecificTraceForClassFailScenario() throws Exception {
        final String badPackage = "io.openliberty.classloading.classpath.test.badclass";
        final String packageTraceSpec = "*=info:com.ibm.ws.class.load." + badPackage + "=all";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();
        logging.setTraceSpecification(packageTraceSpec);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        // Trigger the corrupted class load
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadBadClass");

        // Class=[...BadClass] failed to be defined; classloader=[AppClassLoader@<EAR>:PF];
        //        location=[file:<path>/testBadClass.jar]
        String defineFailedTrace = server.waitForStringInTrace(CLASS_REGEX + BAD_CLASS_NAME + TRACE_CLASS_DEFINE_FAIL);
        checkClassFailTrace(defineFailedTrace, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");
    }

    /**
     * Verifies that the per-package trace spec
     * {@code com.ibm.ws.class.load.<packageName>=all} captures the {@code "failed to load"} trace
     * when a class does not exist anywhere on the delegation chain.
     *
     * <p>The servlet attempts to load {@code com.example.nonexistent.NoSuchClass}.  With only the
     * per-package spec enabled, the failed-to-load trace is emitted for each {@code AppClassLoader}
     * in the chain whose package matches — confirming that {@link AppClassLoader#getActiveTraceComponent}
     * routes the trace through the per-package component when the broader spec is not set.
     */
    @Test
    public void testPackageSpecificTraceForClassLoadFailScenario() throws Exception {
        final String nonExistentClass = "com.example.nonexistent.NoSuchClass";
        final String targetPackage = "com.example.nonexistent";
        final String packageTraceSpec = "*=info:com.ibm.ws.class.load." + targetPackage + "=all";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();
        logging.setTraceSpecification(packageTraceSpec);
        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadNonExistentClass");

        // Class=[com.example.nonexistent.NoSuchClass] failed to load; classloader=[AppClassLoader@<...>:PF]
        // Fires once per AppClassLoader level that exhausts all search steps (WAR and EAR).
        String regex = CLASS_REGEX + nonExistentClass + TRACE_CLASS_LOAD_FAIL;
        String traceLine = server.waitForStringInTrace(regex);
        assertNotNull("Expected 'failed to load' trace for " + nonExistentClass + " but none found", traceLine);
        checkClassLoadTrace(traceLine, nonExistentClass, APP_CL, null);
    }
}
