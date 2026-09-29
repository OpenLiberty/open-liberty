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
import static io.openliberty.classloading.classpath.util.TestUtils.APP_CL;
import static io.openliberty.classloading.classpath.util.TestUtils.CLASS_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_GATEWAY;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCES_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.RESOURCE_REGEX;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_BY_PARENT;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_LIBERTY_API_PACKAGES;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_NOT_FOUND;
import static io.openliberty.classloading.classpath.util.TestUtils.checkClassLoadTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkDelegationPath;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourceTrace;
import static io.openliberty.classloading.classpath.util.TestUtils.checkResourcesTrace;
import static org.junit.Assert.assertFalse;
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
 * FAT tests that verify trace lines emitted when the {@code GatewayClassLoader} resolves
 * classes and resources from OSGi bundles wired to the application gateway.
 */
@RunWith(FATRunner.class)
public class GatewayClassLoaderTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "GatewayClassLoaderTraceTestServer";

    // Classloader type that appear inside classloader=[...]
    private static final String GATEWAY_CL = "GatewayClassLoader";
    private static final String EQUINOX_CL = "EquinoxClassLoader";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    /**
     * Installs the {@code apiTestFeature-1.0} system feature and {@code test.bundle.api} OSGi
     * bundle that wire the {@code test.bundle.api1.*} packages to the application gateway,
     * deploys the trace test EAR, and starts the server.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Install the feature that wires test.bundle.api1.* to the application gateway
        server.installSystemFeature("apiTestFeature-1.0");
        assertTrue("apiTestFeature-1.0.mf should be in lib/features after install",
                   server.fileExistsInLibertyInstallRoot("lib/features/apiTestFeature-1.0.mf"));

        server.installSystemBundle("test.bundle.api");
        assertTrue("test.bundle.api.jar should be in lib after install",
                   server.fileExistsInLibertyInstallRoot("lib/test.bundle.api.jar"));

        ShrinkHelper.exportAppToServer(server, TRACE_TEST_EAR, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    /**
     * Stops the server and removes the {@code apiTestFeature-1.0} feature and
     * {@code test.bundle.api} bundle installed by {@link #setUp()}.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer("SRVE9967W");
            }
        } finally {
            server.uninstallSystemFeature("apiTestFeature-1.0");
            assertFalse("apiTestFeature-1.0.mf should have been removed from lib/features",
                        server.fileExistsInLibertyInstallRoot("lib/features/apiTestFeature-1.0.mf"));
            server.uninstallSystemBundle("test.bundle.api");
            assertFalse("test.bundle.api.jar should have been removed from lib",
                        server.fileExistsInLibertyInstallRoot("lib/test.bundle.api.jar"));
        }
    }

    /**
     * Verifies that two complementary trace lines are emitted when a Liberty API class is loaded
     * through the {@code GatewayClassLoader}.
     *
     * <p>{@code API_A1} belongs to the {@code test.bundle.api1.*} package, which is wired to the
     * application gateway via the {@code apiTestFeature-1.0} system feature.  The EAR
     * {@code AppClassLoader} does not find it on its local classpath and delegates parent-first to
     * the {@code GatewayClassLoader}, which in turn asks the {@code BundleLoader} (Equinox) for
     * the class.
     *
     * <p>Expected trace lines:
     * <ol>
     *   <li>Emitted by {@code GatewayClassLoader} — reports the defining {@code EquinoxClassLoader}
     *       and the JAR location inside the OSGi bundle:
     *       <pre>
     * Class=[test.bundle.api1.a.API_A1] loaded from liberty API packages;
     *   classloader=[EquinoxClassLoader@&lt;hex&gt;[test.bundle.api:...]];
     *   location=[file:&lt;wlp&gt;/lib/test.bundle.api.jar]
     *       </pre></li>
     *   <li>Emitted by the EAR {@code AppClassLoader} — reports the parent that resolved the class
     *       and the full Liberty delegation path:
     *       <pre>
     * Class=[test.bundle.api1.a.API_A1] loaded by parent classloader=[GatewayClassLoader@&lt;hex&gt;:...];
     *   delegation path=[AppClassLoader@&lt;WAR&gt; -&gt; AppClassLoader@&lt;EAR&gt; -&gt; GatewayClassLoader@&lt;hex&gt;:...]
     *       </pre></li>
     * </ol>
     */
    @Test
    public void testGatewayClassLoaderTraceForOsgiBundleClass() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String className = "test.bundle.api1.a.API_A1"; // The class will be loaded by Bundle Loader (EquinoxClassLoader)
        String traceLine = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_LIBERTY_API_PACKAGES);
        String sourceLoc = "test.bundle.api.jar";
        checkClassLoadTrace(traceLine, className, EQUINOX_CL, sourceLoc);

        String parentClTraceLine = server.waitForStringInTrace(CLASS_REGEX + className + TRACE_BY_PARENT);
        checkClassLoadTrace(parentClTraceLine, className, APP_CL, null);
        checkDelegationPath(parentClTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_GATEWAY);

    }

    /**
     * Verifies that {@code GatewayClassLoader} emits a resource-found and a resource-not-found
     * trace line from its {@code getResource()} method, and that the {@code AppClassLoader}
     * emits a {@code "by parent classloader"} trace with the full delegation path.
     *
     * <p>For the not-found case, both {@code GatewayClassLoader} and {@code AppClassLoader}
     * emit a not-found trace for the same resource name.  The {@code waitForStringInTrace}
     * pattern therefore anchors on {@code GatewayClassLoader} to avoid matching the
     * {@code AppClassLoader} line.
     *
     * <p>Example traces emitted:
     * <pre>
     * // GatewayClassLoader's own found trace:
     * Resource=[test/bundle/api1/a/api_a1.txt] found at location=[bundleresource://...]
     *   from liberty API packages by classloader=[GatewayClassLoader@...]
     *
     * // AppClassLoader's "by parent classloader" trace (emitted at EAR level):
     * Resource=[test/bundle/api1/a/api_a1.txt] found at location=[bundleresource://...]
     *   by parent classloader=[GatewayClassLoader@...];
     *   delegation path=[AppClassLoader@...WebModule... -> AppClassLoader@...EARApplication... -> GatewayClassLoader@...]
     *
     * Resource=[io/openliberty/.../NoSuchResource.txt] was not found
     *   by classloader=[GatewayClassLoader@...]
     * </pre>
     */
    @Test
    public void testGatewayClassLoaderGetResourceTrace() throws Exception {
        String resourceName = "api_a1.txt";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — api_a1.txt lives inside the test.bundle.api OSGi bundle.
        // The WAR AppClassLoader delegates parent-first to the EAR AppClassLoader, which in turn
        // delegates to the GatewayClassLoader (a non-AppClassLoader parent). The GatewayClassLoader
        // finds the resource and emits its own "from liberty API packages" trace. The EAR-level
        // AppClassLoader then emits the "by parent classloader" trace with the full delegation path.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetBundleResourceFound");

        // Assert the GatewayClassLoader's own found trace.
        // The "from liberty API packages" phrase uniquely identifies the GatewayClassLoader's own
        // trace line and avoids matching the AppClassLoader's "by parent classloader" line, which
        // also references GATEWAY_CL at the end of the delegation path.
        checkResourceTrace(
                server.waitForStringInTrace(RESOURCE_REGEX + TRACE_LIBERTY_API_PACKAGES + GATEWAY_CL),
                resourceName, GATEWAY_CL, true);

        // Assert the AppClassLoader's "by parent classloader" trace with the three-hop delegation path:
        // WebModule -> EARApplication -> GatewayClassLoader
        String parentClTraceLine = server.waitForStringInTrace(RESOURCE_REGEX + TRACE_BY_PARENT + APP_CL);
        checkResourceTrace(parentClTraceLine, resourceName, APP_CL, true);
        checkDelegationPath(parentClTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_GATEWAY);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on GatewayClassLoader to match only the gateway line.
        resourceName = "NoSuchResource.txt";
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        checkResourceTrace(
                server.waitForStringInTrace(RESOURCE_REGEX + TRACE_NOT_FOUND  + GATEWAY_CL),
                resourceName, GATEWAY_CL, false);
    }

    /**
     * Verifies that {@code GatewayClassLoader} emits a resources-found and a resources-not-found
     * trace line from its {@code getResources()} method, and that the {@code AppClassLoader}
     * emits a {@code "by parent classloader"} trace with the full delegation path.
     *
     * <p>For the not-found case, both {@code GatewayClassLoader} and {@code AppClassLoader}
     * emit a not-found trace for the same resource name.  The {@code waitForStringInTrace}
     * pattern therefore anchors on {@code GatewayClassLoader} to avoid matching the
     * {@code AppClassLoader} line.
     *
     * <p>Example traces emitted:
     * <pre>
     * // GatewayClassLoader's own found trace:
     * Resources=[test/bundle/api1/a/api_a1.txt] found at locations=[bundleresource://...]
     *   from liberty API packages by classloader=[GatewayClassLoader@...]
     *
     * // AppClassLoader's "by parent classloader" trace (emitted at EAR level):
     * Resources=[test/bundle/api1/a/api_a1.txt] found at locations=[bundleresource://...]
     *   by parent classloader=[GatewayClassLoader@...];
     *   delegation path=[AppClassLoader@...WebModule... -> AppClassLoader@...EARApplication... -> GatewayClassLoader@...]
     *
     * Resources=[io/openliberty/.../NoSuchResource.txt] not found;
     *   classloader=[GatewayClassLoader@...]
     * </pre>
     */
    @Test
    public void testGatewayClassLoaderGetResourcesTrace() throws Exception {
        String resourceName = "api_a1.txt";
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // FOUND — api_a1.txt lives inside the test.bundle.api OSGi bundle.
        // The WAR AppClassLoader delegates parent-first to the EAR AppClassLoader, which in turn
        // delegates to the GatewayClassLoader (a non-AppClassLoader parent). The GatewayClassLoader
        // finds the resource and emits its own "from liberty API packages" trace. The EAR-level
        // AppClassLoader then emits the "by parent classloader" trace with the full delegation path.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetBundleResourcesFound");

        // Assert the GatewayClassLoader's own found trace.
        // The "from liberty API packages" phrase uniquely identifies the GatewayClassLoader's own
        // trace line and avoids matching the AppClassLoader's "by parent classloader" line, which
        // also references GATEWAY_CL at the end of the delegation path.
        checkResourcesTrace(
                server.waitForStringInTrace(RESOURCES_REGEX + TRACE_LIBERTY_API_PACKAGES + GATEWAY_CL),
                resourceName, GATEWAY_CL, true);

        // Assert the AppClassLoader's "by parent classloader" trace with the three-hop delegation path:
        // WebModule -> EARApplication -> GatewayClassLoader
        String parentClTraceLine = server.waitForStringInTrace(
                RESOURCES_REGEX + TRACE_BY_PARENT + APP_CL);
        checkResourcesTrace(parentClTraceLine, resourceName, APP_CL, true);
        checkDelegationPath(parentClTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_GATEWAY);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on GatewayClassLoader to match only the gateway line.
        resourceName = "NoSuchResource.txt";
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        checkResourcesTrace(
                server.waitForStringInTrace(RESOURCES_REGEX + TRACE_NOT_FOUND  + GATEWAY_CL),
                resourceName, GATEWAY_CL, false);
    }

}
