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
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_SHARED_LIB;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_FAIL_PREFIX;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_PRFIX;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassFailTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassLoadTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkDelegationPath;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourceTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourcesTrace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
 * FAT tests that verify {@code AppClassLoader} emits correctly structured
 * {@code CLASS LOAD} and {@code CLASS FAIL} trace lines under various
 * classloading scenarios.
 */
@RunWith(FATRunner.class)
public class AppClassLoaderTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "AppClassLoaderTraceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final List<String> testsRequiringConfigUpdate = Arrays.asList("testPackageSpecificClassLoadingTrace",
                                                                                     "testPackageSpecificAndBroaderClassLoadingTraceEnabledTogether",
                                                                                     "testPackageSpecificTraceForClassFailScenario");

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.exportToServer(server, "lib", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);
    }

    @Before
    public void beforeTest() throws Exception {
        if (testsRequiringConfigUpdate.contains(testName.getMethodName())) {
            server.saveServerConfiguration();
        } else {
            server.startServer(testName.getMethodName() + ".log");
        }
    }

    @After
    public void afterTest() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer("SRVE9967W");
            }
        } finally {
            if (testsRequiringConfigUpdate.contains(testName.getMethodName())) {
                server.restoreServerConfiguration();
            }
        }
    }

    /**
     * Verifies that {@code AppClassLoader} emits a {@code CLASS LOAD} trace line when a class
     * from an EJB module JAR ({@code testEjb1.jar}) is loaded.
     *
     * <p>The servlet triggers the load of {@code EjbLib1}; the test then waits for the
     * corresponding trace line and validates that the {@code class}, {@code classloader},
     * {@code location} fields all point to the EJB JAR and that
     * the delegation mode is parent-first ({@code PF}).
     */
    @Test
    public void testAppClassLoaderTraceForEJBModule() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        String className = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className);
        String sourceLoc = "testEjb1.jar";

        //Trace looks as follows
        //CLASS LOAD: class=[io.openliberty.classloading.classpath.test.ejb1.EjbLib1],
        //            classloader=[AppClassLoader@<hex>:PF],
        //            location=[file:<path-to-testEjb1>/testEjb1.jar]
        System.out.println("TRACE = " + traceLine);
        checkClassLoadTrace(traceLine, className, APP_CL, sourceLoc);
    }

    /**
     * Verifies that {@code AppClassLoader} emits {@code CLASS LOAD} trace lines for both a
     * class resolved from an EAR shared library JAR ({@code testLib1.jar}) and an API class
     * bundled inside that same JAR.
     *
     * <p>Two distinct class names are checked — the library implementation class
     * ({@code Lib1}) and the API class ({@code API_A1}) — confirming that both the
     * {@code location} fields reference {@code testLib1.jar} and that
     * the delegation mode is parent-first ({@code PF}).
     */
    @Test
    public void testAppClassLoaderTraceForEARLibAndApi() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String className1 = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String traceLine1 = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className1);
        String sourceLoc1 = "testLib1.jar";

        //Trace looks as follows
        //CLASS LOAD: class=[io.openliberty.classloading.classpath.test.lib1.Lib1],
        //            classloader=[AppClassLoader@<hex>:PF],
        //            location=[file:<path>/testLib1.jar]
        checkClassLoadTrace(traceLine1, className1, APP_CL, sourceLoc1);


        String className2 = "test.bundle.api1.a.API_A1";
        String traceLine2 = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className2);
        String sourceLoc2 = "testLib1.jar";

        //Trace looks as follows
        //CLASS LOAD: class=[test.bundle.api1.a.API_A1],
        //            classloader=[AppClassLoader@<hex>:PF],
        //            location=[file:<path>.jar]
        checkClassLoadTrace(traceLine2, className2, APP_CL, sourceLoc2);
    }

    /**
     * Verifies that {@code AppClassLoader} emits a {@code CLASS LOAD} trace line when a class
     * is loaded from a RAR module ({@code testRar1.rar}).
     *
     * <p>The servlet triggers the load of {@code RarLib1}; the test waits for the matching
     * trace line and confirms that the {@code location} fields reference
     * {@code testRar1.rar} and that the delegation mode is parent-first ({@code PF}).
     */
    @Test
    public void testAppClassLoaderTraceForRARModule() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadRarClasses");

        String className = "io.openliberty.classloading.classpath.test.rar1.RarLib1";
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className);
        String sourceLoc = "testRar1.rar";

        //Trace looks as follows
        //CLASS LOAD: class=[io.openliberty.classloading.classpath.test.rar1.RarLib1],
        //            classloader=[AppClassLoader@<hex>:PF],
        //            location=[file:<path>/testRar1.rar/testResourceAdaptor.jar]
        checkClassLoadTrace(traceLine, className, APP_CL, sourceLoc);
    }



    /**
     * Verifies that the per-package trace spec
     * {@code com.ibm.ws.class.load.<packageName>=all} produces CLASS LOAD output
     * when the broader {@code com.ibm.ws.classloading.internal.*=all} is NOT set.
     *
     * <p>The test stops the server, replaces the trace specification with a
     * package-specific-only spec, restarts, triggers a class load from that
     * package, asserts the trace line is present, and finally restores the
     * original specification.
     */
    @Test
    public void testPackageSpecificClassLoadingTrace() throws Exception {
        // Package whose classes are loaded by testLoadEjbClasses()
        final String targetPackage = "io.openliberty.classloading.classpath.test.ejb1";
        final String packageTraceSpec = "*=info:com.ibm.ws.class.load." + targetPackage + "=all";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();

        logging.setTraceSpecification(packageTraceSpec);
        server.updateServerConfiguration(config);

        server.startServer(testName.getMethodName() + ".log");

        // Trigger a load of a class from the target package
        // This loads the EJBLib1 class. InitBean1 was loaded on startup since it is annotated by @Startup
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        String className1 = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
        String className2 = "io.openliberty.classloading.classpath.test.ejb1.InitBean1";

        //Counting all the trace lines that have the string "CLASS LOAD:".
        String regexp = TRACE_CLASS_LOAD_PRFIX + ".*";
        List<String> traceLines = server.findStringsInLogsAndTraceUsingMark(regexp);


        // Must have found exactly two trace lines – the per-package cltc fired and
        // the broader tc com.ibm.ws.classloading.internal.*=all was not enabled)
        assertEquals("Expected exactly 2 trace lines for " + regexp
                     + " but found: " + traceLines, 2, traceLines.size());

        // Verify the content of the trace lines
        for (int i = 0; i < traceLines.size(); i++) {
            String trace = traceLines.get(i);
            if (trace.contains(className1) ) {
                checkClassLoadTrace(trace, className1, APP_CL, "testEjb1.jar");
            } else {
                checkClassLoadTrace(trace, className2, APP_CL, "testEjb1.jar");
            }
        }
    }

    /**
     * Verifies that when both the per-package trace spec
     * ({@code com.ibm.ws.class.load.<packageName>=all}) and the broader
     * {@code com.ibm.ws.classloading.internal.*=all} spec are active simultaneously, the
     * broader spec takes precedence and produces {@code CLASS LOAD} output for more than just
     * the two classes in the targeted package.
     *
     * <p>The combined trace specification is set before the server starts; after triggering
     * a class load from the target package the test asserts that the number of observed
     * {@code CLASS LOAD} lines exceeds two, confirming that the broader component is active.
     */
    @Test
    public void testPackageSpecificAndBroaderClassLoadingTraceEnabledTogether() throws Exception {
        // Package whose classes are loaded by testLoadEjbClasses()
        final String targetPackage = "io.openliberty.classloading.classpath.test.ejb1";
        final String combinedTrace = "*=info:com.ibm.ws.class.load." + targetPackage + "=all:*=info:com.ibm.ws.classloading.*=all:com.ibm.ws.classloading.internal.*=all";

        ServerConfiguration config = server.getServerConfiguration();
        Logging logging = config.getLogging();

        logging.setTraceSpecification(combinedTrace);
        server.updateServerConfiguration(config);

        server.startServer(testName.getMethodName() + ".log");

        // Trigger a load of a class from the target package
        // This loads the EJBLib1 class. InitBean1 was loaded on startup since it is annotated by @Startup
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        //Counting all the trace lines that have the string "CLASS LOAD:".
        String regexp = TRACE_CLASS_LOAD_PRFIX + ".*";
        List<String> traceLines = server.findStringsInLogsAndTraceUsingMark(regexp);


        // Must have found more than two trace lines
        // the broader tc com.ibm.ws.classloading.*=all:com.ibm.ws.classloading.internal.*=all should override the per-package cltc)
        assertTrue("Expected more than 2 trace lines for " + regexp
                     + " but found: " + traceLines.size(), traceLines.size() > 2);
    }



    /**
     * Verifies that AppClassLoader emits a {@code CLASS FAIL} trace line when
     * {@code defineClass()} throws {@link ClassFormatError} due to corrupt class bytes.
     *
     * <p>The test triggers a load of {@code BadClass}, whose JAR entry contains
     * deliberately invalid bytes.  The expected trace line is:
     * <pre>
     * CLASS FAIL: class=[io.openliberty.classloading.classpath.test.badclass.BadClass],
     *             classloader=[...AppClassLoader@...], location=[...testBadClass.jar]
     * </pre>
     */
    @Test
    public void testAppClassLoaderTraceForClassFailScenario() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // Trigger the load of the corrupted class; the servlet catches the error so the request succeeds
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadBadClass");

        // Wait for the CLASS FAIL trace line that matches the bad class name
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_FAIL_PREFIX + ".*" + BAD_CLASS_NAME);

        // Trace looks as follows:
        // CLASS FAIL: class=[io.openliberty.classloading.classpath.test.badclass.BadClass],
        //             classloader=[AppClassLoader@<hex>:PF],
        //             location=[file:<path>/testBadClass.jar]
        checkClassFailTrace(traceLine, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");
    }

    /**
     * Verifies that no {@code CLASS LOAD} trace line is emitted for the corrupt class —
     * only a {@code CLASS FAIL} line should appear.  This guards against a regression
     * where a failed define might still produce a spurious CLASS LOAD entry.
     */
    @Test
    public void testClassFailDoesNotEmitClassLoadTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadBadClass");

        // CLASS FAIL must be present
        String failTraceLine = server.waitForStringInTrace(TRACE_CLASS_FAIL_PREFIX + ".*" + BAD_CLASS_NAME);
        assertNotNull("Expected CLASS FAIL trace for " + BAD_CLASS_NAME + " was not found", failTraceLine);

        // CLASS LOAD must NOT be present for the same class
        String loadTraceLine = server.waitForStringInTraceUsingMark(TRACE_CLASS_LOAD_PRFIX + ".*" + BAD_CLASS_NAME, 0);
        assertNull("CLASS LOAD trace for " + BAD_CLASS_NAME + " should not be emitted on defineClass failure",
                   loadTraceLine);
    }

    /**
     * Verifies that the package-specific trace spec
     * {@code com.ibm.ws.class.load.<packageName>=all} also captures CLASS FAIL events
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

        String traceLine = server.waitForStringInTrace(TRACE_CLASS_FAIL_PREFIX + ".*" + BAD_CLASS_NAME);
        // Trace looks as follows:
        // CLASS FAIL: class=[io.openliberty.classloading.classpath.test.badclass.BadClass],
        //             classloader=[AppClassLoader@<hex>:PF],
        //             location=[file:<path>/testBadClass.jar]
        checkClassFailTrace(traceLine, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");
    }

    /**
     * Verifies that {@code AppClassLoader} emits the expected trace lines when
     * {@code getResource()} finds or does not find a resource.
     *
     * <p>The servlet runs in the WAR module; {@code lib1.properties} lives in {@code testLib1.jar}
     * which is on the EAR classloader's classpath.  The WAR classloader delegates parent-first to
     * the EAR classloader, which finds the resource on its local classpath and emits:
     * <pre>
     * Resource=[...lib1.properties] found at location=[jar:file:.../testLib1.jar!/...]
     *   on the local classpath; classloader=[AppClassLoader@&lt;EAR&gt;];
     *   delegation path=[AppClassLoader@&lt;WAR&gt; -> AppClassLoader@&lt;EAR&gt;]
     * </pre>
     *
     * <p>Not-found trace (emitted by the WAR classloader after both loaders fail):
     * <pre>
     * Resource=[...NoSuchResource.txt] not found; classloader=[AppClassLoader@&lt;WAR&gt;]
     * </pre>
     */
    @Test
    public void testGetResourceTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — lib1.properties is in testLib1.jar on the EAR classloader's local classpath;
        // the WAR classloader delegates parent-first to the EAR classloader which finds it.
        // GatewayClassLoader won't find it so no ambiguity with other loaders.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceFound");
        String foundTraceLine = server.waitForStringInTrace(
                "Resource=\\[.*lib1\\.properties.*on the local classpath; classloader=\\[" + APP_CL);
        checkResourceTrace(foundTraceLine, "lib1.properties", APP_CL, true);
        // WAR delegates parent-first to EAR; EAR finds lib1.properties on its local classpath.
        // Path: WebModule -> EARApplication
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on APP_CL to match only the AppClassLoader line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        String notFoundTraceLine = server.waitForStringInTrace(
                "Resource=\\[.*NoSuchResource\\.txt.*\\] not found; classloader=\\[" + APP_CL);
        checkResourceTrace(notFoundTraceLine, "NoSuchResource.txt", APP_CL, false);
    }

    /**
     * Verifies that {@code AppClassLoader} emits the expected trace lines when
     * {@code getResources()} finds or does not find resources.
     *
     * <p>The servlet runs in the WAR module; {@code META-INF/MANIFEST.MF} is present in the EAR
     * lib JARs on the EAR classloader's classpath.  The WAR classloader delegates parent-first to
     * the EAR classloader, which finds the resources on its local classpath and emits:
     * <pre>
     * Resources=[META-INF/MANIFEST.MF] found at locations=[jar:file:.../testLib1.jar!/META-INF/MANIFEST.MF, ...]
     *   on the local classpath; classloader=[AppClassLoader@&lt;EAR&gt;];
     *   delegation path=[AppClassLoader@&lt;WAR&gt; -> AppClassLoader@&lt;EAR&gt;]
     * </pre>
     *
     * <p>Not-found trace (emitted by the WAR classloader after both loaders fail):
     * <pre>
     * Resources=[...NoSuchResource.txt] not found by classloader=[AppClassLoader@&lt;WAR&gt;]
     * </pre>
     */
    @Test
    public void testGetResourcesTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — META-INF/MANIFEST.MF is in the EAR lib JARs; anchor on AppClassLoader to
        // match the EAR local-classpath line and avoid the GatewayClassLoader line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesFound");
        String foundTraceLine = server.waitForStringInTrace(
                "Resources=\\[META-INF/MANIFEST\\.MF\\].*on the local classpath; classloader=\\[" + APP_CL);
        checkResourcesTrace(foundTraceLine, "META-INF/MANIFEST.MF", APP_CL, true);
        // WAR delegates parent-first to EAR; EAR finds MANIFEST.MF on its local classpath.
        // Path: WebModule -> EARApplication
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on APP_CL to match only the AppClassLoader line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        String notFoundTraceLine = server.waitForStringInTrace(
                "Resources=\\[.*NoSuchResource\\.txt.*\\] not found.*classloader=\\[" + APP_CL);
        checkResourcesTrace(notFoundTraceLine, "NoSuchResource.txt", APP_CL, false);
    }

    /**
     * Verifies that {@code AppClassLoader} emits the {@code "by common library loader"} trace
     * when {@code getResource()} resolves {@code lib3.properties} through the {@code testLib3}
     * {@code afterApp} delegate.
     *
     * <p>{@code lib3.properties} is absent from the EAR's local classpath, so
     * {@code findResourceInternal} falls through to
     * {@code findResourceCommonLibraryClassLoaders(afterApp)}, which finds it in
     * {@code testLib3.jar} and emits the trace.
     */
    @Test
    public void testGetResourceFoundViaCommonLibrary() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetCommonLibResourceFound");

        // Format: Resource=[...lib3.properties...] found at location=[...] by common library loader; classloader=[AppClassLoader@...]
        String traceLine = server.waitForStringInTrace(
                "Resource=\\[.*lib3\\.properties.*\\] found at location=.*by common library loader.*" + APP_CL);
        assertNotNull("Expected 'by common library loader' trace for lib3.properties was not found", traceLine);

        checkResourceTrace(traceLine, "lib3.properties", APP_CL, true);

        // lib3.properties is absent from both WAR and EAR local classpaths.
        // EAR's afterApp delegate (testLib3 Shared Library) finds it.
        // The trace is emitted by the EAR's findResourceCommonLibraryClassLoaders with path=WAR -> EAR,
        // then " -> " + testLib3 CL is appended — giving: WebModule -> EARApplication -> Shared Library
        checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_SHARED_LIB);
    }

    /**
     * Verifies that {@code AppClassLoader} emits the {@code "by common library loader"} trace
     * when {@code getResources()} resolves {@code lib3.properties} through the {@code testLib3}
     * {@code afterApp} delegate.
     */
    @Test
    public void testGetResourcesFoundViaCommonLibrary() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetCommonLibResourcesFound");

        // Format: Resources=[...lib3.properties...] found at locations=[...] by common library loader; classloader=[AppClassLoader@...]
        String traceLine = server.waitForStringInTrace(
                "Resources=\\[.*lib3\\.properties.*\\] found at locations=.*by common library loader.*" + APP_CL);
        assertNotNull("Expected 'by common library loader' resources trace for lib3.properties was not found", traceLine);

        checkResourcesTrace(traceLine, "lib3.properties", APP_CL, true);

        // Same three-hop path as the getResource() case.
        // Path: WebModule -> EARApplication -> Shared Library
        checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_SHARED_LIB);
    }
}
