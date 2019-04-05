/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This FAT bucket will test Symbol Resolver for symbols prefixed with "env". The values that
 * symbols resolve to are stored in server.env. This FAT creates an App in the server's /Apps directory and
 * resolves a "env." symbol in order to find it and launch it.
 *
 * The verifyMyTestAppIsOnline test checks the app
 * is running on the server by sending a GET request to server and parsing the results of that GET request for
 * a string that is found in the app. If the string is found, the app is online, and the test case passes.
 *
 * The verifyTraceFilename test check the server's logs directory to see if a trace file exists under a specific
 * name. This name is set to a symbol in server.env and this symbol is used in server.xml to set the traceFileName
 * attribute for logging. If this name resolves correctly to the value in server.env, the test case will pass.
 */

@RunWith(FATRunner.class)
public class SymbolResolverTest {
    final static TraceComponent tc = Tr.register(SymbolResolverTest.class);
    public final static String APP_NAME = "myTestApp";

    @Server("ENVResolverTestServer")

    public static LibertyServer server;

    /**
     * Creates myTestApp.war in ENVResolverTestServer and starts server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "");
        server.startServer();
    }

    /**
     * Stops ENVResolverTestServer
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Confirm that the example Artifactory dependency was download and is available on the classpath
     *
     * @throws Exception
     */
    @Test
    public void verifyArtifactoryDependency() throws Exception {
        org.apache.commons.logging.Log.class.getName();
    }

    /**
     * See of URL is reachable by sending GET request to server, checking response code is 200 (OK) and
     * confirming that the string "Hello World!" is present on the response StringBuffer
     *
     * @throws Exception
     */
    @Test
    public void verifyMyTestAppIsOnline() throws Exception {
        HttpURLConnection con = null;
        BufferedReader in = null;
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME;
            URL url = new URL(urlStr);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            System.out.println("Sending GET request to " + urlStr);
            assertEquals(200, con.getResponseCode());

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = "";
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            int responseIndex = response.indexOf("Hello World!");
            assert (responseIndex != -1);
            System.out.println("GET request success, MyTestApp is online");

        } finally {
            if (in != null) {
                in.close();
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * Checks logs folder in server for specified trace log filename. The
     * trace log filename is assigned to an environment variable in server.env.
     * That variable is specified in server.xml. If file exists, then the EnvSymbolResolver
     * properly resolved the symbol for the trace filename.
     *
     * @throws Exception
     */
    @Test
    public void verifyTraceFilename() throws Exception {
        String filename = "SRTrace.log";
        File tmpDir = new File(server.getLogsRoot() + filename);
        assertTrue(tmpDir.exists());
    }
}