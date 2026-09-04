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
import static io.openliberty.classloading.classpath.util.TestUtils.CLASS_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.PL_CL;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCES_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCE_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_DEFINE_SUCCESS;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_LOCAL_CLASSPATH;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_NOT_FOUND;
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
 * FAT tests that verify {@code ParentLastClassLoader} emits correctly structured trace
 * lines under parent-last classloading and resource-lookup scenarios.
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
     * Verifies that {@code ParentLastClassLoader} emits the expected trace lines when a class
     * is loaded with parent-last delegation.
     *
     * <p>{@code Lib1} lives in {@code testLib1.jar} which is on the EAR classloader's classpath,
     * not the WAR's.  With parent-last delegation the WAR {@code ParentLastClassLoader} checks
     * itself first (misses), then delegates to the EAR {@code ParentLastClassLoader} which finds
     * the class on its local classpath and defines it.
     *
     * <p>Expected trace sequence:
     * <ol>
     *   <li>{@code Class=[...Lib1] found on the local classpath;
     *       classloader=[ParentLastClassLoader@&lt;EAR&gt;:PL];
     *       delegation path=[ParentLastClassLoader@&lt;WAR&gt; -&gt; ParentLastClassLoader@&lt;EAR&gt;]}</li>
     *   <li>{@code Class=[...Lib1] was successfully defined;
     *       classloader=[ParentLastClassLoader@&lt;EAR&gt;:PL];
     *       location=[file:&lt;path&gt;/testLib1.jar]}</li>
     * </ol>
     */
    @Test
    public void testParentLastClassLoaderTraceForClassLoad() throws Exception {
        String className = "io.openliberty.classloading.classpath.test.lib1.Lib1";
        String sourceLoc = "testLib1.jar";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String localCpTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_LOCAL_CLASSPATH);
        checkClassLoadTrace(localCpTrace, className, PL_CL, null);
        checkDelegationPath(localCpTrace, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        String defineSuccessTrace = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_CLASS_DEFINE_SUCCESS);
        checkClassLoadTrace(defineSuccessTrace, className, PL_CL, sourceLoc);
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
        String resourceName = "io/openliberty/classloading/test/resources/lib1.properties";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — lib1.properties is in testLib1.jar on the EAR's classpath, not the WAR's.
        // The WAR's ParentLastClassLoader checks itself first (misses), then delegates to the EAR
        // ParentLastClassLoader which finds it on its local classpath.
        // The trace is emitted by the EAR-level ParentLastClassLoader.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceFound");
        String foundTraceLine = server.waitForStringInTrace(RESOURCE_REGEX + resourceName + TRACE_LOCAL_CLASSPATH);
        checkResourceTrace(foundTraceLine, resourceName , PL_CL, true);
        // Two-hop path: WAR ParentLastClassLoader delegated to EAR ParentLastClassLoader.
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — GatewayClassLoader also emits a not-found trace for the same name;
        // anchor on ParentLastClassLoader to match only the PL line.
        resourceName = "NoSuchResource.txt";
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        checkResourceTrace(
                server.waitForStringInTrace(RESOURCE_REGEX + resourceName + TRACE_NOT_FOUND + PL_CL),
                resourceName, PL_CL, false);
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
        String resourceName = "META-INF/MANIFEST.MF";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — META-INF/MANIFEST.MF exists in multiple JARs; the first match is on the EAR's
        // classpath. The WAR's ParentLastClassLoader checks itself first (finds nothing there),
        // then delegates to the EAR ParentLastClassLoader which finds it on its local classpath.
        // Anchor the wait pattern on ParentLastClassLoader to skip GatewayClassLoader's own trace.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesFound");
        String foundTraceLine = server.waitForStringInTrace(RESOURCES_REGEX + resourceName + TRACE_LOCAL_CLASSPATH  + PL_CL);
        checkResourcesTrace(foundTraceLine, resourceName, PL_CL, true);
        // Two-hop path: WAR ParentLastClassLoader delegated to EAR ParentLastClassLoader.
        checkDelegationPath(foundTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — GatewayClassLoader also emits a not-found trace for the same name;
        // anchor on ParentLastClassLoader to match only the PL line.
        resourceName = "NoSuchResource.txt";
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        checkResourcesTrace(
                server.waitForStringInTrace(RESOURCES_REGEX + resourceName + TRACE_NOT_FOUND  + PL_CL),
                resourceName, PL_CL, false);
    }
}
