/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.oracle.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import trace.web.OracleTraceTestServlet;

@RunWith(FATRunner.class)
public class OracleTraceTest extends FATServletClient {

    public static final String JEE_APP = "oracletracefat";
    public static final String SERVLET_NAME = "OracleTraceTestServlet";
    private static final String SSL_PASSWORD = "{xor}Lz4sLCgwLTtubw==";

    @Server("com.ibm.ws.jdbc.fat.oracle.trace")
    @TestServlet(servlet = OracleTraceTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    private static final OracleContainer oracle = FATSuite.getSharedOracleContainer();

    private static void addEnvVars(LibertyServer server, OracleContainer oracle) {
        // Set server environment variables
        server.addEnvVar("ORACLE_URL", oracle.getJdbcUrl());
        server.addEnvVar("ORACLE_USER", oracle.getUsername());
        server.addEnvVar("ORACLE_PASSWORD", oracle.getPassword());
        server.addEnvVar("SSL_PASSWORD", SSL_PASSWORD);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        addEnvVars(server, oracle);

        Map<String, String> jvmOps = server.getJvmOptionsAsMap();
        jvmOps.put("-Doracle.jdbc.timezoneAsRegion", "false");

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "trace.web");

        // Start Server
        server.startServer(OracleTraceTest.class.getName() + "Inital.log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testDBPasswordNotLogged() throws Exception {
        int passwords = server.findStringsInTrace(Pattern.quote(oracle.getPassword())).size();
        if (passwords > 0)
            fail("Database password logged in trace " + passwords + " times. Password: " + oracle.getPassword());
    }

    @Test
    public void testURLEmbeddedPasswordNotLogged() throws Exception {
        int urlPasswords = server.findStringsInTrace(Pattern.quote(oracle.getJdbcUrl())).size();
        if (urlPasswords > 0)
            fail("URL embedded password logged in trace " + urlPasswords + " times. URL: " + oracle.getJdbcUrl());
    }

    @Test
    public void testConnectionPropsEmbeddedPasswordNotLogged() throws Exception {
        int sslPasswords = server.findStringsInTrace(Pattern.quote(SSL_PASSWORD)).size();
        if (sslPasswords > 0)
            fail("SSL passwords logged in trace " + sslPasswords + " times. SSL Password: " + SSL_PASSWORD);
    }

    @Test
    public void testReadOnlyInfo() throws Exception {
        runTest(server, JEE_APP + "/" + SERVLET_NAME, getTestMethodSimpleName());
        List<String> occurrences = server.findStringsInLogs("DSRA8207I");
        assertEquals("The DSRA8207I message should have only been logged exactly once", 1, occurrences.size());
    }

    /**
     * Verify that oracle 23 trace will be enabled if and only if:
     * 1. oracle=all is set in trace specification
     * 2. -Doracle.jdbc.diagnostic.enableLogging=true is set as a JVM property
     *
     * Prior to Oracle 23 oracle.jdbc.diagnostic.enableLogging=true was not required
     */
    @Test
    @Mode(TestMode.FULL)
    public void testOracle23Trace() throws Exception {
        // String to know if logging is enabled
        final String REGEX = Pattern.quote("createNSProperties entering args (oracle.jdbc.") + ".*" + Pattern.quote(")");
        List<String> results;
        String debug;

        // Get original server config and jvmOptions
        ServerConfiguration originalConfig = server.getServerConfiguration().clone();
        Map<String, String> originalJVMOptions = new LinkedHashMap<String, String>();

        // Stop server
        if (server.isStarted())
            server.stopServer();

        // Ensure if any intermittent tests fail we restart the original server
        try {
            // Add configuration to enable tracing
            ServerConfiguration config = server.getServerConfiguration().clone();
            Logging loggingElement = config.getLogging();
            String traceSpec = loggingElement.getTraceSpecification();
            loggingElement.setTraceSpecification(traceSpec + ":oracle=all");

            // Add configuration to jvmOptions
            Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
            jvmOptions.put("-Doracle.jdbc.diagnostic.enableLogging", "true");

            // Update server
            server.updateServerConfiguration(config);
            server.setJvmOptions(jvmOptions);
            addEnvVars(server, oracle);

            // Start server
            server.startServer("oracle23TraceEnabled.log");

            // Mark end of trace before using Datasource for the first time
            server.setTraceMarkToEndOfDefaultTrace();
            runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=10");

            // Assert logging is enabled
            results = server.findStringsInLogsAndTraceUsingMark(REGEX);
            assertTrue("Expected to find " + REGEX + " in logs.", results.size() > 0);

            // Restart server without JVM option
            server.stopServer();
            server.updateServerConfiguration(config);
            server.setJvmOptions(originalJVMOptions);
            addEnvVars(server, oracle);
            server.startServer("oracle23TraceNoJVMOption.log");

            // Mark end of trace before using Datasource for the first time
            server.setTraceMarkToEndOfDefaultTrace();
            runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=11");

            // Assert logging is disabled
            results = server.findStringsInLogsAndTraceUsingMark(REGEX);
            debug = results.size() == 0 ? null : results.get(0);
            assertTrue("Did not expect to find " + REGEX + " in logs. "
                       + "Found " + results.size() + " times. "
                       + "First instance: " + debug, results.size() == 0);

            // Restart server without trace specification
            server.stopServer();
            server.updateServerConfiguration(originalConfig);
            server.setJvmOptions(jvmOptions);
            addEnvVars(server, oracle);
            server.startServer("oracle23TraceNoTraceSpec.log");

            // Mark end of trace before using Datasource for the first time
            server.setTraceMarkToEndOfDefaultTrace();
            runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=12");

            // Assert logging is disabled
            results = server.findStringsInLogsAndTraceUsingMark(REGEX);
            debug = results.size() == 0 ? null : results.get(0);
            assertTrue("Did not expect to find " + REGEX + " in logs. "
                       + "Found " + results.size() + " times. "
                       + "First instance: " + debug, results.size() == 0);

        } finally {
            // Restart server with original specification
            if (server.isStarted())
                server.stopServer();
            server.updateServerConfiguration(originalConfig);
            server.setJvmOptions(originalJVMOptions);
            addEnvVars(server, oracle);
            server.startServer(OracleTraceTest.class.getName() + "Restart.log");

            // Mark end of trace before using Datasource for the first time
            server.setTraceMarkToEndOfDefaultTrace();
            runTestWithResponse(server, JEE_APP + "/" + SERVLET_NAME, "testOracleLogging&iteration=13");

            // Assert logging is disabled
            results = server.findStringsInLogsAndTraceUsingMark(REGEX);
            debug = results.size() == 0 ? null : results.get(0);
            assertTrue("Did not expect to find " + REGEX + " in logs. "
                       + "Found " + results.size() + " times. "
                       + "First instance: " + debug, results.size() == 0);
        }
    }
}
