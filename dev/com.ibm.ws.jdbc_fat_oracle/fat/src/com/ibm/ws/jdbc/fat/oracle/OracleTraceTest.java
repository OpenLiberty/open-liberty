/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.OracleContainer;

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

    public static final OracleContainer oracle = FATSuite.oracle;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.initDatabaseTables();

        // Set server environment variables
        server.addEnvVar("URL", oracle.getJdbcUrl());
        server.addEnvVar("USER", oracle.getUsername());
        server.addEnvVar("PASSWORD", oracle.getPassword());
        server.addEnvVar("SSL_PASSWORD", SSL_PASSWORD);

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "trace.web");

        // Start Server
        server.startServer();
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
}
