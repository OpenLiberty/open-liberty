/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to test Content-MD5 header is set correctly for HTTP requests
 */
@RunWith(FATRunner.class)
public class ContentMD5HeaderTest {

    private static final Class<?> c = ContentMD5HeaderTest.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Server("ContentMD5Header")
    public static LibertyServer server;


    private static final String TEST_APP = "ContentMD5HeaderApp";
    private static final String TEST_APP_CONTEXT_ROOT = TEST_APP;

    //private static final String SERVER_NAME = "FipsTestServer";
    //private static final String HTTP_URL = "http://localhost:9080/md5test";

    /**
     *
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception{
        // Create a simple web application with test resources
        ShrinkHelper.defaultApp(server, TEST_APP, "io.openliberty.transport.http_fat.contentmd5test.servlets");

        // Make sure the apps are in the server before starting it
        server.addInstalledAppForValidation(TEST_APP);

        // Start the server and wait for it to be ready
        server.startServer();
        // ensure app has started.
        server.waitForStringInLog("CWWKT0016I:.*" + TEST_APP + ".*");
    }

    /**
     * shut down the test server
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test verifies the current behavior.
     * The server should start normally and successfully set the Content-MD5 header.
     */
    @Test
    public void testMd5Header() throws Exception {
        Log.info(c, "testMd5Header", "Running test: testMd5Header");

        HttpURLConnection con = getConnection(TEST_APP_CONTEXT_ROOT+"/md5test");
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        // Assert that the request was successful
        assertTrue("Unexpected response code: " + responseCode,responseCode == HttpURLConnection.HTTP_OK);

        // Assert that the Content-MD5 header was set
        String md5Header = con.getHeaderField("Content-MD5");
        assertNotNull(md5Header, "Content-MD5 header should be present in non-FIPS mode.");
        Log.info(c,"testMd5Header","Success: Received Content-MD5 header as expected.");
    }

    /**
     * Creates an HttpURLConnection to the specified path
     * 
     * @param path The path to connect to
     * @return An HttpURLConnection
     * @throws IOException If an error occurs
     */
    private HttpURLConnection getConnection(String path) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con;
    }
}