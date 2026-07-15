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
import static org.junit.Assert.assertFalse;
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
 * Verifies that {@code GatewayClassLoader} emits a {@code CLASS LOAD} trace line
 * when it resolves a class from an OSGi bundle that is wired to the application.
 */
@RunWith(FATRunner.class)
public class GatewayClassLoaderTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "GatewayClassLoaderTraceTestServer";

    // Field tokens present in every trace line
    private static final String TRACE_CLASS_LOAD_PRFIX  = "CLASS LOAD:";
    private static final String FIELD_CLASS = "class=[";
    private static final String FIELD_CLASSLOADER = "classloader=[";
    private static final String FIELD_CODESOURCE = "codeSource=[";

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
     * Verifies a {@code CLASS LOAD} trace line produced by {@code GatewayClassLoader} when it
     * resolves a class from an OSGi bundle.
     * The format is:
     * {@code CLASS LOAD: class=[<name>]; classloader=[<ClassLoaderName@hex>:...]; codeSource=[<url>]}
     *
     * @param traceLine   the raw trace line containing the {@code CLASS LOAD:} prefix
     * @param className   the expected binary class name
     * @param classLoader substring expected inside {@code classloader=[...]} (e.g. "GatewayClassLoader")
     * @param codeSource  substring expected inside {@code codeSource=[...]} (e.g. "test.bundle.api.jar")
     */
    private void checkTrace(String traceLine, String className, String classLoader, String codeSource) {
        assertNotNull("Expected CLASS LOAD trace for " + className + " not found", traceLine);

        String traceMsg = traceLine.substring(traceLine.indexOf(TRACE_CLASS_LOAD_PRFIX) + TRACE_CLASS_LOAD_PRFIX.length());
        String[] traceElements = traceMsg.split(";");

        assertTrue("First element of the trace should contain the string " + FIELD_CLASS,
                   traceElements[0].contains(FIELD_CLASS));
        assertTrue("First element of the trace " + traceElements[0] + " should contain class name: " + className,
                   traceElements[0].contains(className));

        assertTrue("Second element of the trace should contain the string " + FIELD_CLASSLOADER,
                   traceElements[1].contains(FIELD_CLASSLOADER));
        assertTrue("Second element of the trace " + traceElements[1] + " should identify as " + classLoader,
                   traceElements[1].contains(classLoader));

        assertTrue("Third element of the trace should contain the string " + FIELD_CODESOURCE,
                   traceElements[2].contains(FIELD_CODESOURCE));
        assertTrue("Third element of the trace " + traceElements[2] + " should reference the code source "+ codeSource ,
                   traceElements[2].contains(codeSource));
    }

    /**
     * Verifies that {@code GatewayClassLoader} emits a {@code CLASS LOAD} trace line when a
     * class is resolved from an OSGi bundle wired to the application gateway.
     *
     * <p>The servlet triggers a load of {@code API_A1} from the {@code test.bundle.api1.a}
     * package, which is exported by the {@code test.bundle.api} OSGi bundle installed in
     * {@link #setUp()}.  The test waits for the matching trace line and validates that the
     * {@code class}, {@code classloader}, and {@code codeSource} fields all reference the
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
        //           codeSource=[file:<path>/wlp/lib/test.bundle.api.jar]
        checkTrace(traceLine, className, EQUINOX_CL, sourceLoc);
    }

}
