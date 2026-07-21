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
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_APP;
import static io.openliberty.classloading.classpath.fat.FATSuite.TRACE_TEST_EAR;
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
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.EnterpriseApplication;
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

    // Trace message prefix
    private static final String TRACE_CLASS_LOAD_PRFIX = "CLASS LOAD:";
    private static final String TRACE_CLASS_FAIL_PREFIX = "CLASS FAIL:";

    // Field tokens present in every trace line
    private static final String FIELD_CLASS = "class=[";
    private static final String FIELD_CLASSLOADER = "classloader=[";
    private static final String FIELD_LOCATION = "location=[";
    private static final String FIELD_CODESOURCE = "codeSource=[";

    // Classloader type that appear inside classloader=[...]
    private static final String APP_CL = "AppClassLoader";
    private static final String PL_CL = "ParentLastClassLoader";

    private static final String PF = "PF"; //Parent first delegation

    private static final String PL = "PL"; //Parent Last delegation

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final List<String> testsRequiringConfigUpdate = Arrays.asList("testPackageSpecificClassLoadingTrace",
                                                                                     "testPackageSpecificAndBroaderClassLoadingTraceEnabledTogether",
                                                                                     "testPackageSpecificTraceForClassFailScenario",
                                                                                     "testAppClassLoaderTraceForParentLastDelegation");

    @BeforeClass
    public static void setUp() throws Exception {
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
     * Verifies a CLASS LOAD trace line produced when a class is successfully defined.
     * The format is:
     * {@code CLASS LOAD: class=[<name>]; classloader=[<ClassLoaderName@hex>:<domain>:<app>:PF|PL]; location=[<url>]; codeSource=[<url>]}
     *
     * @param traceLine      the raw trace line containing the CLASS LOAD prefix
     * @param className      the expected binary class name
     * @param classLoader    substring expected inside {@code classloader=[...]} (e.g. "AppClassLoader")
     * @param location       substring expected inside {@code location=[...]}
     * @param codeSource     substring expected inside {@code codeSource=[...]}
     * @param delegationMode the delegation mode expected in the classloader field (e.g. "PF" or "PL")
     */
    private void checkTrace(String traceLine, String className, String classLoader, String location, String codeSource, String delegationMode) {
        assertNotNull("Expected CLASS LOAD trace for " + className + " not found", traceLine);

        String traceMsg = traceLine.substring(traceLine.indexOf(TRACE_CLASS_LOAD_PRFIX) + TRACE_CLASS_LOAD_PRFIX.length());
        String[] traceElements = traceMsg.split(";");

        assertTrue("First element of the trace should contain the string " + FIELD_CLASS,
                   traceElements[0].contains(FIELD_CLASS));
        assertTrue("First element of the trace " + traceElements[0] + " should contain class name: " + className,
                   traceElements[0].contains(className));

        assertTrue("Second element of the trace should contain the string " + FIELD_CLASSLOADER,
                   traceElements[1].contains(FIELD_CLASSLOADER));
        checkClassLoaderField(traceElements[1], classLoader, delegationMode);

        assertTrue("Third element of the trace should contain the string " + FIELD_LOCATION,
                   traceElements[2].contains(FIELD_LOCATION));
        assertTrue("Third element of the trace " + traceElements[2] + " should reference the location "+ location,
                   traceElements[2].contains(location));

        assertTrue("Fourth element of the trace should contain the string " + FIELD_CODESOURCE,
                   traceElements[3].contains(FIELD_CODESOURCE));
        assertTrue("Fourth element of the trace " + traceElements[3] + " should reference the code source "+ codeSource ,
                   traceElements[3].contains(codeSource));
    }

    /**
     * Verifies a CLASS FAIL trace line produced when defineClass() throws ClassFormatError.
     * The format is:
     * {@code CLASS FAIL: class=[<name>]; classloader=[<ClassLoaderName@hex>:PF|PL]; location=[<url>]; codeSource=[unknown]}
     *
     * @param traceLine   the raw trace line containing the CLASS FAIL prefix
     * @param className   the expected binary class name
     * @param classLoader substring expected inside {@code classloader=[...]}  (e.g. "AppClassLoader")
     * @param location    substring expected inside {@code location=[...]}
     */
    private void checkFailTrace(String traceLine, String className, String classLoader, String location) {
        assertNotNull("Expected CLASS FAIL trace for " + className + " not found", traceLine);

        String traceMsg = traceLine.substring(traceLine.indexOf(TRACE_CLASS_FAIL_PREFIX) + TRACE_CLASS_FAIL_PREFIX.length());
        String[] traceElements = traceMsg.split(";");

        assertTrue("First element of the CLASS FAIL trace should contain the string " + FIELD_CLASS,
                   traceElements[0].contains(FIELD_CLASS));
        assertTrue("First element of the CLASS FAIL trace " + traceElements[0] + " should contain class name: " + className,
                   traceElements[0].contains(className));

        assertTrue("Second element of the CLASS FAIL trace should contain the string " + FIELD_CLASSLOADER,
                   traceElements[1].contains(FIELD_CLASSLOADER));
        assertTrue("Second element of the CLASS FAIL trace " + traceElements[1] + " should identify " + classLoader,
                   traceElements[1].contains(classLoader));

        assertTrue("Third element of the CLASS FAIL trace should contain the string " + FIELD_LOCATION,
                   traceElements[2].contains(FIELD_LOCATION));
        assertTrue("Third element of the CLASS FAIL trace " + traceElements[2] + " should reference the location " + location,
                   traceElements[2].contains(location));

        assertTrue("Fourth element of the CLASS FAIL trace should contain the string " + FIELD_CODESOURCE,
                   traceElements[3].contains(FIELD_CODESOURCE));
        // codeSource is "unknown" when defineClass() throws error, because the Class object is null
        assertTrue("Fourth element of the CLASS FAIL trace " + traceElements[3] + " should contain 'unknown' codeSource",
                   traceElements[3].contains("unknown"));
    }

    private void checkClassLoaderField(String traceElement, String classLoader, String delegationMode) {
        String domain = "EARApplication";
        String app = "traceTestEar";
        String[] classLoaderString = traceElement.split(":");

        assertTrue("First element of the classLoaderString " + traceElement+ " should identify as " + classLoader,
                   classLoaderString[0].contains(classLoader));
        assertTrue("Second element of the classLoaderString " + traceElement + " should have the domain " + domain,
                   classLoaderString[1].contains(domain));
        assertTrue("Third element of the classLoaderString " + traceElement + " should have the app " + app,
                   classLoaderString[2].contains(app));
        assertTrue("Fourth element of the classLoaderString " + traceElement + " should have the delegation mode " + delegationMode,
                   classLoaderString[3].contains(delegationMode));
    }

    /**
     * Verifies that {@code AppClassLoader} emits a {@code CLASS LOAD} trace line when a class
     * from an EJB module JAR ({@code testEjb1.jar}) is loaded.
     *
     * <p>The servlet triggers the load of {@code EjbLib1}; the test then waits for the
     * corresponding trace line and validates that the {@code class}, {@code classloader},
     * {@code location}, and {@code codeSource} fields all point to the EJB JAR and that
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
        //            location=[file:<path-to-testEjb1>/testEjb1.jar],
        //            codeSource=[file:<path-to-testEjb1>/testEjb1.jar]
        System.out.println("TRACE = " + traceLine);
        checkTrace(traceLine, className, APP_CL, sourceLoc, sourceLoc, PF);
    }

    /**
     * Verifies that {@code AppClassLoader} emits {@code CLASS LOAD} trace lines for both a
     * class resolved from an EAR shared library JAR ({@code testLib1.jar}) and an API class
     * bundled inside that same JAR.
     *
     * <p>Two distinct class names are checked — the library implementation class
     * ({@code Lib1}) and the API class ({@code API_A1}) — confirming that both the
     * {@code location} and {@code codeSource} fields reference {@code testLib1.jar} and that
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
        //            location=[file:<path>/testLib1.jar],
        //            codeSource=[file:<path>/testLib1.jar]
        checkTrace(traceLine1, className1, APP_CL, sourceLoc1, sourceLoc1, PF);


        String className2 = "test.bundle.api1.a.API_A1";
        String traceLine2 = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className2);
        String sourceLoc2 = "testLib1.jar";

        //Trace looks as follows
        //CLASS LOAD: class=[test.bundle.api1.a.API_A1],
        //            classloader=[AppClassLoader@<hex>:PF],
        //            location=[file:<path>.jar],
        //            codeSource=[file:<path>/testLib1.jar]
        checkTrace(traceLine2, className2, APP_CL, sourceLoc2, sourceLoc2, PF);
    }

    /**
     * Verifies that {@code AppClassLoader} emits a {@code CLASS LOAD} trace line when a class
     * is loaded from a RAR module ({@code testRar1.rar}).
     *
     * <p>The servlet triggers the load of {@code RarLib1}; the test waits for the matching
     * trace line and confirms that the {@code location} and {@code codeSource} fields reference
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
        //            location=[file:<path>/testRar1.rar/testResourceAdaptor.jar],
        //            codeSource=[file:<path>/testRar1.rar/testResourceAdaptor.jar]
        checkTrace(traceLine, className, APP_CL, sourceLoc, sourceLoc, PF);
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
                checkTrace(trace, className1, APP_CL, "testEjb1.jar", "testEjb1.jar", PF);
            } else {
                checkTrace(trace, className2, APP_CL, "testEjb1.jar", "testEjb1.jar", PF);
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
     *             classloader=[...AppClassLoader@...], location=[...testBadClass.jar],
     *             codeSource=[unknown]
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
        //             location=[file:<path>/testBadClass.jar],
        //             codeSource=[unknown]
        checkFailTrace(traceLine, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");
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
        //             location=[file:<path>/testBadClass.jar],
        //             codeSource=[unknown]
        checkFailTrace(traceLine, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");
    }

    /**
     * Verifies that when the EAR application's classloader is configured with
     * {@code delegation="parentLast"} in {@code server.xml}, the {@code CLASS LOAD}
     * trace line reflects parent-last ({@code PL}) delegation.
     *
     * <p>The test saves the current server configuration, injects a
     * {@code <classloader delegation="parentLast"/>} element into the
     * {@code <enterpriseApplication>} stanza, starts the server, triggers a class
     * load from {@code testLib1.jar}, and asserts that the {@code classloader=[...]}
     * field in the trace contains {@code PL}.  The original configuration is
     * restored by {@link #afterTest()}.
     */
    @Test
    public void testAppClassLoaderTraceForParentLastDelegation() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        // Locate the traceTestEar application and add a parent-last classloader element
        EnterpriseApplication app = config.getEnterpriseApplications().getBy("name", "traceTestEar");
        ClassloaderElement cl = new ClassloaderElement();
        cl.setDelegation("parentLast");
        app.getClassloaders().add(cl);

        server.updateServerConfiguration(config);
        server.startServer(testName.getMethodName() + ".log");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String className = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className);

        //Trace looks as follows
        //CLASS LOAD: class=[io.openliberty.classloading.classpath.test.lib1.Lib1],
        //            classloader=[ParentLastClassLoader@<hex>:PL],
        //            location=[file:<path>/testLib1.jar],
        //            codeSource=[file:<path>/testLib1.jar]
        checkTrace(traceLine, className, PL_CL, "testLib1.jar", "testLib1.jar", PL);
    }
}
