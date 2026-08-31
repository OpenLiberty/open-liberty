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
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.PL_CL;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_PRFIX;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassLoadTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkDelegationPath;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourceTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourcesTrace;

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
 * FAT tests that verify {@code ParentLastClassLoader} emits correctly structured
 * {@code CLASS LOAD} and resource trace lines.
 *
 * <p>The server has {@code delegation="parentLast"} baked into its {@code server.xml},
 * so no per-test configuration updates are needed.
 */
@RunWith(FATRunner.class)
public class ParentLastClassLoaderTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "ParentLastClassLoaderTraceTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
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
     * Verifies that {@code ParentLastClassLoader} emits a {@code CLASS LOAD} trace line
     * when a class is loaded from an EAR shared library JAR ({@code testLib1.jar}).
     *
     * <p>Example trace:
     * <pre>
     * CLASS LOAD: class=[io.openliberty.classloading.classpath.test.lib1.Lib1];
     *             classloader=[ParentLastClassLoader@hex:EARApplication:traceTestEar:PL];
     *             location=[file:.../testLib1.jar]
     * </pre>
     */
    @Test
    public void testParentLastClassLoaderTraceForClassLoad() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String className = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className);

        // Trace looks as follows:
        // CLASS LOAD: class=[io.openliberty.classloading.classpath.test.lib1.Lib1];
        //             classloader=[ParentLastClassLoader@<hex>:EARApplication:traceTestEar:PL];
        //             location=[file:<path>/testLib1.jar]
        checkClassLoadTrace(traceLine, className, PL_CL, "testLib1.jar");
    }

    /**
     * Verifies that {@code ParentLastClassLoader} emits a resource-found and a resource-not-found
     * trace line from its {@code getResource()} method, and that the delegation path on the
     * found trace points only to the {@code ParentLastClassLoader} (no {@code " -> "} hop)
     * because the resource is on its local classpath.
     *
     * <p>For the not-found case, both {@code GatewayClassLoader} and {@code ParentLastClassLoader}
     * emit a not-found trace for the same resource name; the pattern therefore anchors on
     * {@code ParentLastClassLoader} to match only the PL line.
     *
     * <p>Example traces emitted by {@code ParentLastClassLoader}:
     * <pre>
     * Resource=[...lib1.properties] found at location=[jar:file:.../testLib1.jar!/...]
     *   on the local classpath; classloader=[ParentLastClassLoader@...EARApplication...];
     *   delegation path=[ParentLastClassLoader@...WebModule... -> ParentLastClassLoader@...EARApplication...]
     *
     * Resource=[...NoSuchResource.txt] not found; classloader=[ParentLastClassLoader@...]
     * </pre>
     *
     * <p>{@code lib1.properties} lives in {@code testLib1.jar} which is on the EAR's classpath,
     * not the WAR's. With parent-last, the WAR loader checks itself first (step 2), misses, then
     * delegates to the parent EAR loader (step 3) which finds it. The delegation path therefore
     * has two hops: {@code WebModule → EARApplication}.
     */
    @Test
    public void testParentLastClassLoaderGetResourceTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — lib1.properties is in testLib1.jar on the EAR's classpath, not the WAR's.
        // The WAR's ParentLastClassLoader checks itself first (misses), then delegates to the EAR
        // ParentLastClassLoader which finds it on its local classpath.
        // The trace is emitted by the EAR-level ParentLastClassLoader.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceFound");
        String foundTraceLine = server.waitForStringInTrace(
                "Resource=\\[.*lib1\\.properties.*on the local classpath; classloader=\\[" + PL_CL);
        checkResourceTrace(foundTraceLine, "io/openliberty/classloading/test/resources/lib1.properties", PL_CL, true);
        // Two-hop path: WAR ParentLastClassLoader delegated to EAR ParentLastClassLoader.
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — GatewayClassLoader also emits a not-found trace for the same name;
        // anchor on ParentLastClassLoader to match only the PL line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        checkResourceTrace(
                server.waitForStringInTrace("Resource=\\[.*NoSuchResource\\.txt.*\\] not found.*classloader=\\[" + PL_CL),
                "io/openliberty/classloading/nonexistent/NoSuchResource.txt", PL_CL, false);
    }

    /**
     * Verifies that {@code ParentLastClassLoader} emits a resources-found and a resources-not-found
     * trace line from its {@code getResources()} method, and that the delegation path on the
     * found trace points only to the {@code ParentLastClassLoader} (no {@code " -> "} hop)
     * because the resources are on its local classpath.
     *
     * <p>For the not-found case, both {@code GatewayClassLoader} and {@code ParentLastClassLoader}
     * emit a not-found trace for the same resource name; the pattern therefore anchors on
     * {@code ParentLastClassLoader} to match only the PL line.
     *
     * <p>Example traces emitted by {@code ParentLastClassLoader}:
     * <pre>
     * Resources=[META-INF/MANIFEST.MF] found at locations=[jar:file:.../testLib1.jar!/META-INF/MANIFEST.MF, ...]
     *   on the local classpath; classloader=[ParentLastClassLoader@...EARApplication...];
     *   delegation path=[ParentLastClassLoader@...WebModule... -> ParentLastClassLoader@...EARApplication...]
     *
     * Resources=[...NoSuchResource.txt] not found; classloader=[ParentLastClassLoader@...]
     * </pre>
     */
    @Test
    public void testParentLastClassLoaderGetResourcesTrace() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — META-INF/MANIFEST.MF exists in multiple JARs; the first match is on the EAR's
        // classpath. The WAR's ParentLastClassLoader checks itself first (finds nothing there),
        // then delegates to the EAR ParentLastClassLoader which finds it on its local classpath.
        // Anchor the wait pattern on ParentLastClassLoader to skip GatewayClassLoader's own trace.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesFound");
        String foundTraceLine = server.waitForStringInTrace(
                "Resources=\\[META-INF/MANIFEST\\.MF\\].*on the local classpath; classloader=\\[" + PL_CL);
        checkResourcesTrace(foundTraceLine, "META-INF/MANIFEST.MF", PL_CL, true);
        // Two-hop path: WAR ParentLastClassLoader delegated to EAR ParentLastClassLoader.
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — GatewayClassLoader also emits a not-found trace for the same name;
        // anchor on ParentLastClassLoader to match only the PL line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        checkResourcesTrace(
                server.waitForStringInTrace("Resources=\\[.*NoSuchResource\\.txt.*\\] not found.*classloader=\\[" + PL_CL),
                "io/openliberty/classloading/nonexistent/NoSuchResource.txt", PL_CL, false);
    }
}
