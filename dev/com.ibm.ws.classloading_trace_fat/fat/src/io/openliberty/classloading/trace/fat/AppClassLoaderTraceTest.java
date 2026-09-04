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
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_SHARED_LIB;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCES_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCE_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_BY_COMMON_LIB;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_DEFINE_FAIL;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_DEFINE_SUCCESS;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_FAIL;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_SUCCESS;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_LOCAL_CLASSPATH;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_NOT_FOUND;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassFailTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassLoadTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkDelegationPath;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourceTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourcesTrace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

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
 * FAT tests that verify {@code AppClassLoader} emits correctly structured trace
 * lines under various classloading and resource-lookup scenarios.
 */
@RunWith(FATRunner.class)
public class AppClassLoaderTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "AppClassLoaderTraceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.exportToServer(server, "lib", TEST_LIB3_JAR, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE9967W");
        }
    }

    /**
     * Verifies that {@code AppClassLoader} emits the expected trace lines when a class
     * is loaded from an EJB module JAR on the EAR classloader's local classpath.
     *
     * <p>Expected trace sequence for {@code EjbLib1}:
     * <ol>
     *   <li>{@code Class=[...EjbLib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
     *       delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]}</li>
     *   <li>{@code Class=[...EjbLib1] was successfully defined; classloader=[AppClassLoader@<EAR>:PF];
     *       location=[file:<path>/testEjb1.jar]}</li>
     * </ol>
     */
    @Test
    public void testAppClassLoaderTraceForEJBModule() throws Exception {
        String className = "io.openliberty.classloading.classpath.test.ejb1.EjbLib1";
        String sourceLoc = "testEjb1.jar";

        server.setMarkToEndOfLog(server.getDefaultTraceFile());
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        // Class=[...EjbLib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
        //        delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]
        String localCpTrace = server.waitForStringInTrace(
                               CLASS_REGEX + className + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace, className, APP_CL, null);
        checkDelegationPath(localCpTrace, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        // Class=[...EjbLib1] was successfully defined; classloader=[AppClassLoader@<EAR>:PF];
        //        location=[file:<path>/testEjb1.jar]
        String defineSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_DEFINE_SUCCESS);
        checkClassLoadTrace(defineSuccessTrace, className, APP_CL, sourceLoc);

        // Class=[...EjbLib1] was successfully loaded; classloader=[AppClassLoader@<WAR>:PF]
        String loadSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_LOAD_SUCCESS);
        checkClassLoadTrace(loadSuccessTrace, className, APP_CL, null);
    }

    /**
     * Verifies that {@code AppClassLoader} emits the expected trace lines when loading
     * two classes from the EAR classloader: one from an EAR library JAR and one from
     * a Liberty API bundle.
     *
     * <p>Expected trace sequence for {@code Lib1} (found on EAR local classpath):
     * <ol>
     *   <li>{@code Class=[...Lib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
     *       delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]}</li>
     *   <li>{@code Class=[...Lib1] was successfully defined; classloader=[AppClassLoader@<EAR>:PF];
     *       location=[file:<path>/testLib1.jar]}</li>
     * </ol>
     *
     * <p>Expected trace sequence for {@code API_A1} (Liberty API, not on local classpath):
     * <ol>
     *   <li>{@code Class=[...API_A1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
     *       delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]}</li>
     *   <li>{@code Class=[...API_A1] was successfully defined; classloader=[AppClassLoader@<EAR>:PF];
     *       location=[file:<path>/test.bundle.api.jar]}</li>
     * </ol>
     */
    @Test
    public void testAppClassLoaderTraceForEARLibAndApi() throws Exception {
        String className1 = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String className2 = "test.bundle.api1.a.API_A1";
        String sourceLoc = "testLib1.jar";

        server.setMarkToEndOfLog(server.getDefaultTraceFile());
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");


        // Class=[...Lib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
        //        delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]
        String localCpTrace1 = server.waitForStringInTrace(
                               CLASS_REGEX + className1 + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace1, className1, APP_CL, null);
        checkDelegationPath(localCpTrace1, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        // Class=[...Lib1] was successfully defined; classloader=[AppClassLoader@<EAR>];
        //        location=[file:<path>/testLib1.jar]
        String defineSuccessTrace1 = server.waitForStringInTrace(CLASS_REGEX + className1 + TRACE_CLASS_DEFINE_SUCCESS);
        checkClassLoadTrace(defineSuccessTrace1, className1, APP_CL, sourceLoc);

        // Class=[...Lib1] was successfully loaded; classloader=[AppClassLoader@<WAR>:PF]
        String loadSuccessTrace1 = server.waitForStringInTrace(CLASS_REGEX + className1 + TRACE_CLASS_LOAD_SUCCESS);
        checkClassLoadTrace(loadSuccessTrace1, className1, APP_CL, null);



        // Class=[...API_A1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
        //        delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]
        String localCpTrace2 = server.waitForStringInTrace(
                               CLASS_REGEX + className2 + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace2, className2, APP_CL, null);
        checkDelegationPath(localCpTrace2, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        // Class=[...API_A1] was successfully defined;
        //        classloader=[AppClassLoader@<hex>:PF]; location=[file:<path>.jar]
        String defineSuccessTrace2 = server.waitForStringInTrace(CLASS_REGEX + className2 + TRACE_CLASS_DEFINE_SUCCESS);
        checkClassLoadTrace(defineSuccessTrace2, className2, APP_CL, sourceLoc);

        // Class=[...API_A1] was successfully loaded; classloader=[AppClassLoader@<WAR>:PF]
        String loadSuccessTrace2 = server.waitForStringInTrace(CLASS_REGEX + className2 + TRACE_CLASS_LOAD_SUCCESS);
        checkClassLoadTrace(loadSuccessTrace2, className2, APP_CL, null);
    }

    /**
     * Verifies that {@code AppClassLoader} emits the expected trace lines when a class
     * is loaded from a RAR module archive on the EAR classloader's local classpath.
     *
     * <p>Expected trace sequence for {@code RarLib1}:
     * <ol>
     *   <li>{@code Class=[...RarLib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
     *       delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]}</li>
     *   <li>{@code Class=[...RarLib1] was successfully defined; classloader=[AppClassLoader@<EAR>:PF];
     *       location=[file:<path>/testRar1.rar]}</li>
     * </ol>
     */
    @Test
    public void testAppClassLoaderTraceForRARModule() throws Exception {
        String className = "io.openliberty.classloading.classpath.test.rar1.RarLib1";
        String sourceLoc = "testRar1.rar";

        server.setMarkToEndOfLog(server.getDefaultTraceFile());
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadRarClasses");

        // Class=[...RarLib1] found on the local classpath; classloader=[AppClassLoader@<EAR>];
        //        delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]
        String localCpTrace = server.waitForStringInTrace(
                               CLASS_REGEX + className + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace, className, APP_CL, null);
        checkDelegationPath(localCpTrace, DOMAIN_WEB_MODULE, DOMAIN_EAR);


        // Class=[...RarLib1] was successfully defined;
        //        classloader=[AppClassLoader@<hex>:PF]; location=[file:<path-to-testEjb1>/testEjb1.jar]
        String defineSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_DEFINE_SUCCESS);
        checkClassLoadTrace(defineSuccessTrace, className, APP_CL, sourceLoc);

        // Class=[...RarLib1] was successfully loaded; classloader=[AppClassLoader@<WAR>:PF]
        String loadSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_LOAD_SUCCESS);
        checkClassLoadTrace(loadSuccessTrace, className, APP_CL, null);
    }



    /**
     * Verifies that {@code AppClassLoader} emits a {@code "loaded by common library loader"} trace
     * line when a class is resolved through the {@code testLib3} {@code afterApp} delegate.
     *
     * <p>{@code Lib3} is absent from both the WAR and EAR local classpaths. The EAR classloader
     * falls through to {@code findClassCommonLibraryClassLoaders(afterApp)}, which finds it in
     * {@code testLib3.jar} and emits:
     * <pre>
     * Class=[io.openliberty.classloading.classpath.test.lib3.Lib3] loaded by common library loader;
     *        classloader=[AppClassLoader@&lt;Shared Library&gt;];
     *        delegation path=[AppClassLoader@&lt;WAR&gt; -> AppClassLoader@&lt;EAR&gt; -> AppClassLoader@&lt;Shared Library&gt;]
     * </pre>
     */
    @Test
    public void testClassLoadedByCommonLibraryTrace() throws Exception {
        String className = "io.openliberty.classloading.classpath.test.lib3.Lib3";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadCommonLibClass");

        // Class=[...Lib3] loaded by common library loader; classloader=[AppClassLoader@<Shared Library>];
        //        delegation path=[WebModule -> EARApplication -> Shared Library]
        String traceLine = server.waitForStringInTrace(
                CLASS_REGEX + className + TRACE_BY_COMMON_LIB);
        checkClassLoadTrace(traceLine, className, APP_CL, null);
        checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_SHARED_LIB);

        // Class=[...Lib3] was successfully loaded; classloader=[AppClassLoader@<WAR>:PF]
        String loadSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_LOAD_SUCCESS);
        checkClassLoadTrace(loadSuccessTrace, className, APP_CL, null);
    }


    /**
     * Verifies that {@code AppClassLoader} emits a class-define-failed trace line when
     * {@code defineClass()} throws {@link ClassFormatError} due to corrupt class bytes,
     * and that no class-defined-successfully trace is emitted for the same class.
     *
     * <p>The expected trace sequence for {@code BadClass} is:
     * <ol>
     *   <li>{@code Class=[...BadClass] found on the local classpath} — bytes located by the EAR classloader.</li>
     *   <li>{@code Class=[...BadClass] failed to be defined} — {@code defineClass()} threw.</li>
     * </ol>
     * The {@code was successfully defined} trace must be absent — this guards against a regression
     * where a failed define might still produce a spurious success entry.
     */
    @Test
    public void testAppClassLoaderTraceForClassDefineFailScenario() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // Trigger the load of the corrupted class; the servlet catches the error so the request succeeds
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadBadClass");

        // Class=[...BadClass] found on the local classpath; classloader=[AppClassLoader@<EAR>];
        //        delegation path=[AppClassLoader@<WAR> -> AppClassLoader@<EAR>]
        String localCpTrace = server.waitForStringInTrace(
                               CLASS_REGEX + BAD_CLASS_NAME + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace, BAD_CLASS_NAME, APP_CL, null);
        checkDelegationPath(localCpTrace, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        // Class=[...BadClass] failed to be defined;
        //        classloader=[AppClassLoader@<hex>:PF]; location=[file:<path>/testBadClass.jar]
        String defineFailedTrace = server.waitForStringInTrace(CLASS_REGEX + BAD_CLASS_NAME + TRACE_CLASS_DEFINE_FAIL);
        checkClassFailTrace(defineFailedTrace, BAD_CLASS_NAME, APP_CL, TEST_BAD_CLASS + ".jar");

        // class-defined-successfully must NOT be present for the same class
        String successTrace = server.waitForStringInTraceUsingMark(CLASS_REGEX + BAD_CLASS_NAME + TRACE_CLASS_DEFINE_SUCCESS, 0);
        assertNull("Class-defined-successfully trace for " + BAD_CLASS_NAME + " should not be emitted on defineClass failure",
                   successTrace);

        // class-loaded-successfully must NOT be present for the same class
        String loadSuccessTrace = server.waitForStringInTraceUsingMark(CLASS_REGEX + BAD_CLASS_NAME + TRACE_CLASS_LOAD_SUCCESS, 0);
        assertNull("Class-loaded-successfully trace for " + BAD_CLASS_NAME + " should not be emitted on defineClass failure",
                   loadSuccessTrace);
    }

    /**
     * Verifies that {@code AppClassLoader} emits a {@code "failed to load"} trace line when a
     * class does not exist anywhere on the delegation chain.
     *
     * <p>The servlet attempts to load {@code com.example.nonexistent.NoSuchClass} via
     * {@code Class.forName()}. The WAR classloader searches all delegates and the parent without
     * success, then emits:
     * <pre>
     * Class=[com.example.nonexistent.NoSuchClass] failed to load; classloader=[AppClassLoader@&lt;WAR&gt;]
     * </pre>
     */
    @Test
    public void testClassFailedToLoadTrace() throws Exception {
        String className = "com.example.nonexistent.NoSuchClass";

        server.setMarkToEndOfLog(server.getDefaultTraceFile());
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadNonExistentClass");

        // Class=[com.example.nonexistent.NoSuchClass] failed to load; classloader=[AppClassLoader@<WAR>]
        String regex = CLASS_REGEX  + className + TRACE_CLASS_LOAD_FAIL;
        List<String> traceLines = server.findStringsInLogsAndTraceUsingMark(regex);

        // Must have found exactly four traces (GatewayLC + AppCL-Shared + AppCL-EAR + AppCL-WAR)
        assertEquals("Expected 4 trace lines for " + regex, 4, traceLines.size());

        for (int i = 0; i < traceLines.size(); i++) {
            String trace = traceLines.get(i);
            if (trace.contains(APP_CL) ) {
                checkClassLoadTrace(trace, className, APP_CL, null);
                break;
            }
        }
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
        String resourceName = "lib1.properties";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — lib1.properties is in testLib1.jar on the EAR classloader's local classpath;
        // the WAR classloader delegates parent-first to the EAR classloader which finds it.
        // GatewayClassLoader won't find it so no ambiguity with other loaders.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceFound");
        String foundTraceLine = server.waitForStringInTrace(RESOURCE_REGEX + resourceName + TRACE_LOCAL_CLASSPATH);
        checkResourceTrace(foundTraceLine, "lib1.properties", APP_CL, true);
        // WAR delegates parent-first to EAR; EAR finds lib1.properties on its local classpath.
        // Path: WebModule -> EARApplication
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        resourceName = "NoSuchResource.txt";
        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on APP_CL to match only the AppClassLoader line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        String notFoundTraceLine = server.waitForStringInTrace(RESOURCE_REGEX + resourceName + TRACE_NOT_FOUND + APP_CL);
        checkResourceTrace(notFoundTraceLine, resourceName, APP_CL, false);
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
        String resourceName = "MANIFEST.MF";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — META-INF/MANIFEST.MF is in the EAR lib JARs; anchor on AppClassLoader to
        // match the EAR local-classpath line and avoid the GatewayClassLoader line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesFound");
        String foundTraceLine = server.waitForStringInTrace(
                RESOURCES_REGEX + resourceName + TRACE_LOCAL_CLASSPATH + APP_CL);
        checkResourcesTrace(foundTraceLine, resourceName, APP_CL, true);
        // WAR delegates parent-first to EAR; EAR finds MANIFEST.MF on its local classpath.
        // Path: WebModule -> EARApplication
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on APP_CL to match only the AppClassLoader line.
        resourceName = "NoSuchResource.txt";
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        String notFoundTraceLine = server.waitForStringInTrace(RESOURCES_REGEX + resourceName + TRACE_NOT_FOUND + APP_CL);
        checkResourcesTrace(notFoundTraceLine, resourceName, APP_CL, false);
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
        String resourceName = "lib3.properties";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetCommonLibResourceFound");

        // Format: Resource=[...lib3.properties...] found at location=[...] by common library loader; classloader=[AppClassLoader@...]
        String traceLine = server.waitForStringInTrace(RESOURCE_REGEX + resourceName + TRACE_BY_COMMON_LIB);
        checkResourceTrace(traceLine, resourceName, APP_CL, true);

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
        String resourceName = "lib3.properties";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetCommonLibResourcesFound");

        // Format: Resources=[...lib3.properties...] found at locations=[...] by common library loader; classloader=[AppClassLoader@...]
        String traceLine = server.waitForStringInTrace(RESOURCES_REGEX + resourceName + TRACE_BY_COMMON_LIB);
        checkResourcesTrace(traceLine, resourceName, APP_CL, true);

        // Same three-hop path as the getResource() case.
        // Path: WebModule -> EARApplication -> Shared Library
        checkDelegationPath(traceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_SHARED_LIB);
    }
}
