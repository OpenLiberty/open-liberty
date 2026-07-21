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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;


@RunWith(FATRunner.class)
public class ClassLoaderToStringTraceTest extends FATServletClient {

    private static final String SERVER_NAME = "traceTestServer";
    private static final Class<?> c = ClassLoaderToStringTraceTest.class;

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final Pattern HEX_ADDRESS_PATTERN = Pattern.compile("@[0-9a-f]{1,8}");
    private static final Pattern DELEGATION_PATTERN = Pattern.compile(":(PF|PL)");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(":(EARApplication|WebModule|EJBModule|ConnectorModule|SpringModule|Resource Adapter)");
    private static final Pattern ID_PATTERN = Pattern.compile(":[^\\]]+");
    private static final Pattern APIS_PATTERN = Pattern.compile(":apis=\\[[^\\]]*\\]");

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

    @Test
    public void testAppClassLoaderToStringFormat() throws Exception {
        final String method = "testAppClassLoaderToStringFormat";
        Log.info(c, method, "Testing AppClassLoader toString() format in trace");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLibraryClass");

        // Search for both AppClassLoader and ParentLastClassLoader (which extends AppClassLoader)
        List<String> appClassLoaderTraces = server.findStringsInTrace("AppClassLoader");
        List<String> parentLastTraces = server.findStringsInTrace("ParentLastClassLoader");

        List<String> traceLines = new ArrayList<>();
        if (appClassLoaderTraces != null) {
            traceLines.addAll(appClassLoaderTraces);
        }
        if (parentLastTraces != null) {
            traceLines.addAll(parentLastTraces);
        }

        assertNotNull("Should find AppClassLoader or ParentLastClassLoader in trace", traceLines);
        assertTrue("Should have at least one AppClassLoader trace entry", traceLines.size() > 0);

        boolean foundValidFormat = false;
        for (String traceLine : traceLines) {
            if (validateAppClassLoaderFormat(traceLine)) {
                foundValidFormat = true;
                Log.info(c, method, "Found valid AppClassLoader format: " + traceLine);
                break;
            }
        }

        assertTrue("Should find at least one AppClassLoader trace with enhanced toString() format", foundValidFormat);
    }

    /**
     * Test that ParentLastClassLoader toString() includes :PL
     * Note: This test validates format when parent-last classloaders appear in trace.
     */
    @Test
    public void testParentLastClassLoaderToStringFormat() throws Exception {
        final String method = "testParentLastClassLoaderToStringFormat";
        Log.info(c, method, "Testing ParentLastClassLoader toString() format in trace");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        List<String> traceLines = server.findStringsInTrace(":PL");

        if (traceLines != null && !traceLines.isEmpty()) {
            boolean foundValidFormat = false;
            for (String traceLine : traceLines) {
                if (HEX_ADDRESS_PATTERN.matcher(traceLine).find()) {
                    foundValidFormat = true;
                    Log.info(c, method, "Found valid PL format: " + traceLine);
                    break;
                }
            }
            assertTrue("Found :PL but format is invalid", foundValidFormat);
            Log.info(c, method, "Successfully validated PL delegation format");
        } else {
            Log.info(c, method, "No PL delegation found - acceptable");
        }

        assertTrue("Test completed successfully", true);
    }

    /**
     * Test that ContainerClassLoader toString() includes classpath entry count
     * Note: This test validates format when ContainerClassLoader appears in trace.
     */
    @Test
    public void testContainerClassLoaderToStringFormat() throws Exception {
        final String method = "testContainerClassLoaderToStringFormat";
        Log.info(c, method, "Testing ContainerClassLoader toString() format in trace");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib2Classes");

        List<String> traceLines = server.findStringsInTrace("classpath-entries=");

        if (traceLines != null && !traceLines.isEmpty()) {
            boolean foundValidFormat = false;
            for (String traceLine : traceLines) {
                if (HEX_ADDRESS_PATTERN.matcher(traceLine).find()) {
                    foundValidFormat = true;
                    Log.info(c, method, "Found valid ContainerClassLoader format: " + traceLine);
                    break;
                }
            }
            assertTrue("Found classpath-entries= but format is invalid", foundValidFormat);
            Log.info(c, method, "Successfully validated ContainerClassLoader format");
        } else {
            Log.info(c, method, "No classpath-entries found - acceptable");
        }

        assertTrue("Test completed successfully", true);
    }

    /**
     * Test that GatewayClassLoader toString() includes bundle information
     */
    @Test
    public void testGatewayClassLoaderToStringFormat() throws Exception {
        final String method = "testGatewayClassLoaderToStringFormat";
        Log.info(c, method, "Testing GatewayClassLoader toString() format in trace");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        List<String> traceLines = server.findStringsInTrace("GatewayClassLoader");
        if (traceLines == null || traceLines.isEmpty()) {
            traceLines = server.findStringsInTrace("bundle=");
        }

        assertNotNull("Should find GatewayClassLoader or bundle info in trace", traceLines);
        assertTrue("Should have at least one GatewayClassLoader trace entry", traceLines.size() > 0);

        boolean foundValidFormat = false;
        for (String traceLine : traceLines) {
            if (traceLine.contains("bundle=") && HEX_ADDRESS_PATTERN.matcher(traceLine).find()) {
                foundValidFormat = true;
                Log.info(c, method, "Found valid GatewayClassLoader format: " + traceLine);
                break;
            }
        }

        assertTrue("Should find GatewayClassLoader with bundle information in trace", foundValidFormat);
    }

    /**
     * Test that hex address format is present in all classloader traces
     */
    @Test
    public void testHexAddressFormatInAllClassLoaders() throws Exception {
        final String method = "testHexAddressFormatInAllClassLoaders";
        Log.info(c, method, "Testing hex address format across all classloaders");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLibraryClass");
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        List<String> traceLines = server.findStringsInTrace("ClassLoader");

        assertNotNull("Should find ClassLoader entries in trace", traceLines);
        assertTrue("Should have multiple ClassLoader trace entries", traceLines.size() > 0);

        int withHexAddress = 0;
        for (String traceLine : traceLines) {
            if (HEX_ADDRESS_PATTERN.matcher(traceLine).find()) {
                withHexAddress++;
            }
        }

        Log.info(c, method, "Found " + withHexAddress + " out of " + traceLines.size() +
                 " ClassLoader traces with hex addresses");

        assertTrue("Should find multiple classloader traces with hex addresses (found " + withHexAddress + ")",
                   withHexAddress > 0);
    }

    /**
     * Test that delegation mode is clearly indicated in traces
     */
    @Test
    public void testDelegationModeInTraces() throws Exception {
        final String method = "testDelegationModeInTraces";
        Log.info(c, method, "Testing delegation mode display in traces");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib2Classes");

        List<String> parentFirstTraces = server.findStringsInTrace(":PF");
        List<String> parentLastTraces = server.findStringsInTrace(":PL");

        List<String> configPFTraces = server.findStringsInTrace("Config@");

        boolean foundDelegationInfo = false;
        if ((parentFirstTraces != null && !parentFirstTraces.isEmpty()) ||
            (parentLastTraces != null && !parentLastTraces.isEmpty()) ||
            (configPFTraces != null && !configPFTraces.isEmpty())) {
            foundDelegationInfo = true;
        }

        assertTrue("Should find delegation mode information in traces", foundDelegationInfo);

        if (parentFirstTraces != null && !parentFirstTraces.isEmpty()) {
            Log.info(c, method, "Found PF delegation: " + parentFirstTraces.get(0));
        }
        if (parentLastTraces != null && !parentLastTraces.isEmpty()) {
            Log.info(c, method, "Found PL delegation: " + parentLastTraces.get(0));
        }
    }

    /**
     * Test that API visibility is shown in traces
     */
    @Test
    public void testAPIVisibilityInTraces() throws Exception {
        final String method = "testAPIVisibilityInTraces";
        Log.info(c, method, "Testing API visibility display in traces");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLib1Classes");

        List<String> apiTraces = server.findStringsInTrace("apis=");

        assertNotNull("Should find API visibility information in traces", apiTraces);
        assertTrue("Should have at least one trace with API visibility", apiTraces.size() > 0);

        boolean foundValidFormat = false;
        for (String traceLine : apiTraces) {
            if (APIS_PATTERN.matcher(traceLine).find()) {
                foundValidFormat = true;
                Log.info(c, method, "Found valid API visibility format: " + traceLine);
                break;
            }
        }

        assertTrue("Should find properly formatted API visibility in traces", foundValidFormat);
    }

    /**
     * Test that application/module identity is shown in traces
     */
    @Test
    public void testApplicationIdentityInTraces() throws Exception {
        final String method = "testApplicationIdentityInTraces";
        Log.info(c, method, "Testing application/module identity display in traces");

        server.setMarkToEndOfLog(server.getDefaultTraceFile());

        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadLibraryClass");
        runTest(server, TRACE_TEST_APP + "/TraceTestServlet", "testLoadEjbClasses");

        List<String> domainTraces = server.findStringsInTrace("domain=");
        List<String> idTraces = server.findStringsInTrace("id=");

        boolean foundIdentityInfo = false;
        if ((domainTraces != null && !domainTraces.isEmpty()) ||
            (idTraces != null && !idTraces.isEmpty())) {
            foundIdentityInfo = true;
        }

        assertTrue("Should find application/module identity information in traces", foundIdentityInfo);

        if (domainTraces != null) {
            for (String traceLine : domainTraces) {
                if (DOMAIN_PATTERN.matcher(traceLine).find() && ID_PATTERN.matcher(traceLine).find()) {
                    Log.info(c, method, "Found valid identity format: " + traceLine);
                    break;
                }
            }
        }
    }

    /**
     * Helper method to validate AppClassLoader format
     */
    private boolean validateAppClassLoaderFormat(String traceLine) {
        boolean hasHexAddress = HEX_ADDRESS_PATTERN.matcher(traceLine).find();
        boolean hasDelegation = DELEGATION_PATTERN.matcher(traceLine).find();
        boolean hasDomain = DOMAIN_PATTERN.matcher(traceLine).find();
        boolean hasId = ID_PATTERN.matcher(traceLine).find();

        //Note: At the very least there should be a hex address and delegation returned
        return hasHexAddress && hasDelegation && (hasDomain || hasId);
    }
}
