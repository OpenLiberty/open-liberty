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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.rules.repeater.EE6FeatureReplacementAction;

/**
 * Test to ensure that the tcpOptions inactivityTimeout works.
 */
@SkipForRepeat(EE6FeatureReplacementAction.ID) // Part.getSubmittedFileName requires Servlet 3.1+
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MaxMessageSizeLimitTests {

    private static final Logger LOG = Logger.getLogger(MaxMessageSizeLimitTests.class.getName());
    private static final String APP_NAME = "FileUpload";
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server("MaxMessageSize")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.transport.http.fileupload.servlet");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(MaxMessageSizeLimitTests.class.getSimpleName() + ".log");

        // Go through logs and check if Netty is being used.
        // Wait for the TCP Channel to finish loading and get the TCP Channel started message.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv6) port 8010.
        String tcpChannelMessage = server.waitForStringInLog("CWWKO0219I: TCP Channel defaultHttpEndpoint");
        LOG.info("Endpoint: " + tcpChannelMessage);

        runningNetty = tcpChannelMessage.contains(NETTY_TCP_CLASS_NAME);
        LOG.info("Running Netty? " + runningNetty);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Save the server configuration before each test, this should be the default server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the server to the default state.
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    @Test
    public void testFileThatIsWithinLimit() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(3000); // Larger than our file content size

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        String boundary = "-----------------" + System.currentTimeMillis();
        
        URL url = new URL("http://" + server.getHostname() + ":" + 
                         server.getHttpDefaultPort() + "/" + APP_NAME + "/FileUploadServlet");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true); // Sending Data 
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String fileName = "test.txt";
        String fileContent = "Hello, world! This is some test file content!";
        
        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
        body.append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(fileContent).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");

        // Send the request with file content
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        try {
            assertEquals("Expected HTTP 200", 200, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }
    }

    @Test
    public void testFileThatExceedsLimit() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getHttpOptions().setMessageSizeLimit(2); // Smaller than our file content size, should cause an error

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*FileUpload.*");

        String boundary = "-----------------" + System.currentTimeMillis();
        
        URL url = new URL("http://" + server.getHostname() + ":" + 
                         server.getHttpDefaultPort() + "/" + APP_NAME + "/FileUploadServlet");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true); // Sending Data 
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        String fileName = "test.txt";
        String fileContent = "This is test file content!";
        
        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
        body.append(fileName).append("\"\r\n");
        body.append("Content-Type: text/plain\r\n\r\n");
        body.append(fileContent).append("\r\n");
        body.append("--").append(boundary).append("--\r\n");

        // Send the request with file content
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        try {
            assertEquals("Expected HTTP 413", 413, conn.getResponseCode());
        } finally {
            conn.disconnect();
        }

    }
}
