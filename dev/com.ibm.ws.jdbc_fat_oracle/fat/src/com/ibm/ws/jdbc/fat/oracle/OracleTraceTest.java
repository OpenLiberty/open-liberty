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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.oracle.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import trace.web.OracleTraceTestServlet;

@RunWith(FATRunner.class)
public class OracleTraceTest extends FATServletClient {

    public static final String JEE_APP = "oracletracefat";
    public static final String SERVLET_NAME = "OracleTraceTestServlet";
    private static final String SSL_PASSWORD = "{xor}Lz4sLCgwLTtubw==";
    private static final String DB_PASSWORD = "PassW0rd";

    @Server("com.ibm.ws.jdbc.fat.oracle.trace")
    @TestServlet(servlet = OracleTraceTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    private static final OracleContainer oracle = FATSuite.getSharedOracleContainer();

    @BeforeClass
    public static void setUp() throws Exception {

        // Set server environment variables
        server.addEnvVar("ORACLE_URL", oracle.getJdbcUrl());
        server.addEnvVar("ORACLE_USER", oracle.getUsername());
        server.addEnvVar("ORACLE_PASSWORD", oracle.getPassword());
        server.addEnvVar("SSL_PASSWORD", SSL_PASSWORD);

        Map<String, String> jvmOps = server.getJvmOptionsAsMap();
        jvmOps.put("-Doracle.jdbc.timezoneAsRegion", "false");
        jvmOps.put("-Doracle.jdbc.Trace", "true");

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "trace.web");

        // Start Server
        server.startServer(OracleTraceTest.class.getName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
    }

    //@Test TODO test not running since the docker oracle image we are using
    //      does not come with a script to update system password.  If we switch
    //      oracle images in the future this would be a good test to have.
    public void testDBPasswordNotLogged() throws Exception {
        int passwords = server.findStringsInTrace(Pattern.quote(DB_PASSWORD)).size();
        if (passwords > 0)
            fail("Database password logged in trace " + passwords + " times. Password: " + DB_PASSWORD);
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
}
