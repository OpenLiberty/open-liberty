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
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_EAR;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_GATEWAY;
import static io.openliberty.classloading.classpath.util.TestUtils.DOMAIN_WEB_MODULE;
import static io.openliberty.classloading.classpath.util.TestUtils.TRACE_CLASS_LOAD_PRFIX;
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
 * Verifies that {@code GatewayClassLoader} emits {@code CLASS LOAD} and resource
 * loading trace lines when it resolves classes and resources from OSGi bundles
 * wired to the application.
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
     * Verifies that {@code GatewayClassLoader} emits a {@code CLASS LOAD} trace line when a
     * class is resolved from an OSGi bundle wired to the application gateway.
     *
     * <p>The servlet triggers a load of {@code API_A1} from the {@code test.bundle.api1.a}
     * package, which is exported by the {@code test.bundle.api} OSGi bundle installed in
     * {@link #setUp()}.  The test waits for the matching trace line and validates that the
     * {@code class}, {@code classloader}, and {@code location} fields all reference the
     * expected class name and bundle JAR.
     */
    @Test
    public void testGatewayClassLoaderTraceForOsgiBundleClass() throws Exception {
        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        String className = "test.bundle.api1.a.API_A1"; // The class will be loaded by Bundle Loader (EquinoxClassLoader)
        String traceLine = server.waitForStringInTrace(TRACE_CLASS_LOAD_PRFIX + ".*" + className);
        String sourceLoc = "test.bundle.api.jar";

        //Trace looks as follows:
        //CLASS LOAD: class=[test.bundle.api1.a.API_A1];
        //           classloader=[org.eclipse.osgi.internal.loader.EquinoxClassLoader@38832d62[test.bundle.api:1.0.116.202607101434(id=155)]];
        //           location=[file:<path>/wlp/lib/test.bundle.api.jar]
        checkClassLoadTrace(traceLine, className, EQUINOX_CL, sourceLoc);
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
                server.waitForStringInTrace("Resource=\\[.*api_a1\\.txt.*\\] found at location=.*from liberty API packages.*" + GATEWAY_CL),
                "test/bundle/api1/a/api_a1.txt", GATEWAY_CL, true);

        // Assert the AppClassLoader's "by parent classloader" trace with the three-hop delegation path:
        // WebModule -> EARApplication -> GatewayClassLoader
        String parentClTraceLine = server.waitForStringInTrace(
                "Resource=\\[.*api_a1\\.txt.*\\] found at location=.*by parent classloader=\\[" + GATEWAY_CL);
        checkResourceTrace(parentClTraceLine, "test/bundle/api1/a/api_a1.txt", APP_CL, true);
        checkDelegationPath(parentClTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_GATEWAY);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on GatewayClassLoader to match only the gateway line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourceNotFound");
        checkResourceTrace(
                server.waitForStringInTrace("Resource=\\[.*NoSuchResource\\.txt.*\\].*not found.*" + GATEWAY_CL),
                "io/openliberty/classloading/nonexistent/NoSuchResource.txt", GATEWAY_CL, false);
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
                server.waitForStringInTrace("Resources=\\[.*api_a1\\.txt.*\\] found at locations=.*from liberty API packages.*" + GATEWAY_CL),
                "test/bundle/api1/a/api_a1.txt", GATEWAY_CL, true);

        // Assert the AppClassLoader's "by parent classloader" trace with the three-hop delegation path:
        // WebModule -> EARApplication -> GatewayClassLoader
        String parentClTraceLine = server.waitForStringInTrace(
                "Resources=\\[.*api_a1\\.txt.*\\] found at locations=.*by parent classloader=\\[" + GATEWAY_CL);
        checkResourcesTrace(parentClTraceLine, "test/bundle/api1/a/api_a1.txt", APP_CL, true);
        checkDelegationPath(parentClTraceLine, DOMAIN_WEB_MODULE, DOMAIN_EAR, DOMAIN_GATEWAY);

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        // NOT FOUND — both GatewayClassLoader and AppClassLoader emit a not-found trace;
        // anchor on GatewayClassLoader to match only the gateway line.
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testGetResourcesNotFound");
        checkResourcesTrace(
                server.waitForStringInTrace("Resources=\\[.*NoSuchResource\\.txt.*\\].*not found.*" + GATEWAY_CL),
                "io/openliberty/classloading/nonexistent/NoSuchResource.txt", GATEWAY_CL, false);
    }

}
