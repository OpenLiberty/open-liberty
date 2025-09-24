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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test to ensure that the tcpOptions inactivityTimeout works.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class InactivityTimeoutTests {

    private static final Logger LOG = Logger.getLogger(InactivityTimeoutTests.class.getName());
    private static final String APP_NAME = "InactivityTimeout";
    private static final String NETTY_TCP_CLASS_NAME = "io.openliberty.netty.internal.tcp.TCPUtils";
    private static boolean runningNetty = false;

    @Server("InactivityTimeout")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.transport.http.inactivity.timeout.servlet");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(InactivityTimeoutTests.class.getSimpleName() + ".log");

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
            //CWWKO0211E: TCP Channel defaultHttpEndpoint has been constructed with an incorrect configuration property value. Name: inactivityTimeout Value: -1 Valid Range: Minimum 0, Maximum 3600000 <br>[4/8/25, 21:04:00:557 EDT] 0000004c com.ibm.ws.channelfw.internal.ChannelFrameworkImpl
            //CWWKO0029E: An exception was generated when initializing chain CHAIN-defaultHttpEndpoint because of exception com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name: defaultHttpEndpoint name: inactivityTimeout value: -1 minimum Value: 0 maximum Value: 3600000
            server.stopServer("CWWKO0211E", "CWWKO0029E");
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

    /**
     * The test will set inactivityTimeout to a value of 120s and validate in the trace file that
     * the correct value is being used.
     *
     * The below configuration will be used to set inactivityTimeout to 120s:
     * <tcpOptions inactivityTimeout="120s"/>
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_nonDefault() throws Exception {
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("120s");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate that inactivityTimeout is set to 120000 (120s).
        assertNotNull("The configured value of inactivityTimeout was not 120000!", server.waitForStringInTraceUsingMark("inactivityTimeout: 120000"));
    }

    /**
     * The test will set inactivityTimeout to a value of -1 and validate that an error occurs.
     *
     * The below configuration will be used to set inactivityTimeout to -1:
     * <tcpOptions inactivityTimeout="-1"/>
     *
     * ExpectedFFDCs:
     * ------Start of DE processing------ = [5/29/25, 10:23:45:956 EDT]
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration
     * probeid = 102
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: inactivityTimeout value: -1 minimum Value: 0 maximum Value: 3600000
     * at com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration.setValues(TCPChannelConfiguration.java:553)
     *
     * ------Start of DE processing------ = [5/29/25, 10:23:45:970 EDT]
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.channelfw.internal.ChannelFrameworkImpl.initChainInternal
     * probeid = 2206
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: inactivityTimeout value: -1 minimum Value: 0 maximum Value: 3600000
     * at com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration.setValues(TCPChannelConfiguration.java:553)
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("com.ibm.wsspi.channelfw.exception.ChannelException")
    @AllowedFFDC("io.openliberty.netty.internal.exception.NettyException")
    public void testInactivityTimeout_tooLow() throws Exception {
        String expectedFFDC;

        if (!runningNetty) {
            expectedFFDC = "com.ibm.wsspi.channelfw.exception.ChannelException";
        } else {
            expectedFFDC = "io.openliberty.netty.internal.exception.NettyException";
        }

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("-1");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate error messages due to invalid config.
        assertNotNull("CWWKO0211E was not found and should have been!", server.waitForStringInTraceUsingMark("CWWKO0211E"));
        assertNotNull("CWWKO0029E was not found and should have been!", server.waitForStringInTraceUsingMark("CWWKO0029E"));

        assertTrue("There were not two FFDCs created!", server.waitForMultipleStringsInLogUsingMark(2, "FFDC1015I") == 2);

        List<String> ffdcFileNames = server.listFFDCFiles(server.getServerName());

        // There should be 2 FFDCs generated for this test but there could be 2 from the testInactivityTimeout_tooHigh test.
        int numberOfFFDCs = ffdcFileNames.size();
        assertTrue("The number of FFDCs was not 2 or 4 but was: " + numberOfFFDCs, numberOfFFDCs == 2 || numberOfFFDCs == 4);

        // Get the latest two FFDCs.
        RemoteFile ffdcFile1 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 1));
        RemoteFile ffdcFile2 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 2));

        List<String> lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile1.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);

        lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile2.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);

    }

    /**
     * The test will set inactivityTimeout to a value of 3600001 and validate that an error occurs.
     *
     * The below configuration will be used to set inactivityTimeout to 3600001:
     * <tcpOptions inactivityTimeout="3600001"/>
     *
     * ExpectedFFDCs:
     * ------Start of DE processing------ = [5/29/25, 10:47:58:919 EDT]
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration
     * probeid = 102
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: inactivityTimeout value: 3600001 minimum Value: 0 maximum Value: 3600000
     * at com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration.setValues(TCPChannelConfiguration.java:553)
     *
     * ------Start of DE processing------ = [5/29/25, 10:47:58:923 EDT]
     * Exception = com.ibm.wsspi.channelfw.exception.ChannelException
     * Source = com.ibm.ws.channelfw.internal.ChannelFrameworkImpl.initChainInternal
     * probeid = 2206
     * Stack Dump = com.ibm.wsspi.channelfw.exception.ChannelException: A TCP Channel has been constructed with incorrect configuration property value, Channel Name:
     * defaultHttpEndpoint name: inactivityTimeout value: 3600001 minimum Value: 0 maximum Value: 3600000
     * at com.ibm.ws.tcpchannel.internal.TCPChannelConfiguration.setValues(TCPChannelConfiguration.java:553)
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("com.ibm.wsspi.channelfw.exception.ChannelException")
    @AllowedFFDC("io.openliberty.netty.internal.exception.NettyException")
    public void testInactivityTimeout_tooHigh() throws Exception {
        String expectedFFDC;

        if (!runningNetty) {
            expectedFFDC = "com.ibm.wsspi.channelfw.exception.ChannelException";
        } else {
            expectedFFDC = "io.openliberty.netty.internal.exception.NettyException";
        }

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("3600001");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(null);

        // Validate error messages due to invalid config.
        assertNotNull("CWWKO0211E was not found and should have been!", server.waitForStringInTraceUsingMark("CWWKO0211E"));
        assertNotNull("CWWKO0029E was not found and should have been!", server.waitForStringInTraceUsingMark("CWWKO0029E"));

        assertTrue("There were not two FFDCs created!", server.waitForMultipleStringsInLogUsingMark(2, "FFDC1015I") == 2);

        List<String> ffdcFileNames = server.listFFDCFiles(server.getServerName());

        // There should be 2 FFDCs generated for this test but there could be 2 from the testInactivityTimeout_tooLow test.
        int numberOfFFDCs = ffdcFileNames.size();
        assertTrue("The number of FFDCs was not 2 or 4 but was: " + numberOfFFDCs, numberOfFFDCs == 2 || numberOfFFDCs == 4);

        // Get the latest two FFDCs.
        RemoteFile ffdcFile1 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 1));
        RemoteFile ffdcFile2 = server.getFFDCLogFile(ffdcFileNames.get(ffdcFileNames.size() - 2));

        List<String> lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile1.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);

        lines = server.findStringsInFileInLibertyServerRoot(expectedFFDC, "logs/ffdc/" + ffdcFile2.getName());
        assertTrue("The expected FFDC: " + expectedFFDC + " was not found!", lines.size() > 0);
    }

    /**
     * The test will set the inactivitiyTimeout to a value of 5s.
     *
     * The below configuration will be used to set inactivityTimeout to 5s:
     * <tcpOptions inactivityTimeout="5s"/>
     *
     * A socket will be opened.
     *
     * The test will sleep for 20s which is 4X greater than the inactivityTimeout.
     * On the initial read the server will retry one time if the timeout is hit. So we need to wait a bit more
     * than 2 times the inactivityTimeout to ensure the inactivityTimeout is working as expected.
     *
     * The test will then send a request and validate that the response is "HTTP/1.1 408 Request Timeout" or
     * the connection was closed before the test could read the response and a SocketException occurs.
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_one_request() throws Exception {
        String expectedResponse = "HTTP/1.1 408 Request Timeout";
        boolean expectedResponseFound = false;
        boolean socketExceptionOccurred = false;

        String address = server.getHostname() + ":" + server.getHttpDefaultPort();

        String request = "GET /" + "InactivityTimeout" + "/InactivityTimeoutServlet" + " HTTP/1.1\r\n" +
                         "Host: " + address + "\r\n" +
                         "\r\n";

        // Set the inactivityTimeout to 5 seconds to make testing quicker than the default 60 second timeout.
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("5s");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*InactivityTimeout.*");

        // Validate that inactivityTimeout is set to 5000 (5s).
        assertNotNull("The configured value of inactivityTimeout was not 5000!", server.waitForStringInTraceUsingMark("inactivityTimeout: 5000"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

        LOG.info("Creating a Socket connection.");
        URL url = HttpUtils.createURL(server, "/InactivityTimeoutServlet");
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {

            socket.setKeepAlive(true);

            // Wait for "TCPReadReques 1   read (async) requested for local: localhost/127.0.0.1:8010 remote: localhost/127.0.0.1:36762" and start sleeping after
            // to ensure the inactivtyTimeout is invoked. Only check when running Channel Framework. Netty won't read but we'll wait for the timeout two times
            // before actually timing out so the behavior is the same.
            if (!runningNetty) {
                server.waitForStringInTraceUsingMark(".*read \\(async\\) requested for local");
            }

            // Sleep 4X the inactivityTimeout since the read is retried one time.
            Thread.sleep(20000);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            LOG.info("Sending a request: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response:");
            try {
                while ((line = reader.readLine()) != null) {
                    LOG.info(line);
                    if (line.equals(expectedResponse)) {
                        LOG.info("Expected response was found!");
                        expectedResponseFound = true;
                    }
                }
            } catch (SocketException e) {
                // If the connection is closed before we can read the response.
                LOG.info("SocketException occurred!");
                socketExceptionOccurred = true;
            }
        }

        assertTrue("A timeout did not occur!", expectedResponseFound || socketExceptionOccurred);

        // Verify that there was a SocketTimeoutException in the trace.
        if (!runningNetty) {
            assertTrue("The SocketTimeoutException was not found in the trace and should have been!",
                       server.findStringsInLogsAndTraceUsingMark("SocketTimeoutException").size() == 1);
        } else {
            assertTrue("The connection closed message was not found in the trace and should have been!",
                       server.findStringsInLogsAndTraceUsingMark("connection closed due to idle timeout").size() == 1);
        }
    }

    /**
     * The test will set the inactivitiyTimeout to a value of 5s.
     *
     * The below configuration will be used to set inactivityTimeout to 5s:
     * <tcpOptions inactivityTimeout="5s"/>
     *
     * A socket will be opened.
     *
     * A request is sent and the initial response is read and validated to be the correct response from the Servlet.
     *
     * The test will sleep for 6 seconds which is 1X + 1 the inactivityTimeout.
     *
     * The test will then send another request and ensure no response is returned and that there
     * is a SocketTimeoutException in the trace indicating the inactivtyTimeout worked correctly.
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_two_requests() throws Exception {
        String expectedResponse1 = "Response from InactivityTimeoutServlet!";
        boolean requestOnePassed = false;
        boolean requestTwoFailed = false;

        String address = server.getHostname() + ":" + server.getHttpDefaultPort();

        String request = "GET /" + "InactivityTimeout" + "/InactivityTimeoutServlet" + " HTTP/1.1\r\n" +
                         "Host: " + address + "\r\n" +
                         "\r\n";

        // Set the inactivityTimeout to 5 seconds to make testing quicker than the default 60 second timeout.
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("5s");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*InactivityTimeout.*");

        // Validate that inactivityTimeout is set to 5000 (5s).
        assertNotNull("The configured value of inactivityTimeout was not 5000!", server.waitForStringInTraceUsingMark("inactivityTimeout: 5000"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

        LOG.info("Creating a Socket connection.");
        URL url = HttpUtils.createURL(server, "/InactivityTimeoutServlet");
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {

            socket.setKeepAlive(true);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            LOG.info("Sending request 1: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response from request 1:");
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                if (line.equals(expectedResponse1)) {
                    requestOnePassed = true;
                }
            }

            assertTrue("The expected response: " + expectedResponse1 + " was not received!", requestOnePassed);

            // Sleep 1X + 1 the inactivityTimeout to ensure the timeout is reached.
            Thread.sleep(6000);

            // Drive another request
            LOG.info("Sending request 2: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response from request 2:");
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                requestTwoFailed = true; // There should be no response!
            }
        }

        assertFalse("There was no response expected but one was received", requestTwoFailed);

        // Verify that there was a SocketTimeoutException in the trace.
        if (!runningNetty) {
            assertTrue("The SocketTimeoutException was not found in the trace and should have been!",
                       server.findStringsInLogsAndTraceUsingMark("SocketTimeoutException").size() == 1);
        } else {
            assertTrue("The connection closed message was not found in the trace and should have been!",
                       server.findStringsInLogsAndTraceUsingMark("connection closed due to idle timeout").size() == 1);
        }
    }

    /**
     * The test will set the inactivitiyTimeout to a value of 5s.
     *
     * The below configuration will be used to set inactivityTimeout to 5s:
     * <tcpOptions inactivityTimeout="5s"/>
     *
     * A socket will be opened.
     *
     * The test will sleep for 6 seconds which is 1X + 1 the inactivityTimeout.
     *
     * A request is sent and the initial response is read and validated to be the correct response from the Servlet.
     *
     * Since the initial read is retried before invoking the inactivityTimeout this request and response should work with
     * only the 6s sleep vs the 20s sleep where it would be expected to have a "HTTP/1.1 408 Request Timeout" response or a SocketException.
     *
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_verify_read_retry() throws Exception {
        String expectedResponse = "Response from InactivityTimeoutServlet!";
        boolean expectedResponseFound = false;

        String address = server.getHostname() + ":" + server.getHttpDefaultPort();

        String request = "GET /" + "InactivityTimeout" + "/InactivityTimeoutServlet" + " HTTP/1.1\r\n" +
                         "Host: " + address + "\r\n" +
                         "\r\n";

        // Set the inactivityTimeout to 5 seconds to make testing quicker than the default 60 second timeout.
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("5s");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*InactivityTimeout.*");

        // Validate that inactivityTimeout is set to 5000 (5s).
        assertNotNull("The configured value of inactivityTimeout was not 5000!", server.waitForStringInTraceUsingMark("inactivityTimeout: 5000"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

        LOG.info("Creating a Socket connection.");
        URL url = HttpUtils.createURL(server, "/InactivityTimeoutServlet");
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {

            socket.setKeepAlive(true);

            // Wait for "TCPReadReques 1   read (async) requested for local: localhost/127.0.0.1:8010 remote: localhost/127.0.0.1:36762" and start sleeping after
            // to ensure the inactivtyTimeout is invoked. Only check when running Channel Framework. Netty won't read but we'll wait for the timeout two times
            // before actually timing out so the behavior is the same.
            if (!runningNetty) {
                server.waitForStringInTraceUsingMark(".*read \\(async\\) requested for local");
            }

            // Sleep 1X + 1 the inactivityTimeout since the read is retried one time, the inactivityTimeout should not be reached.
            Thread.sleep(6000);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            LOG.info("Sending a request: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response:");
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                if (line.equals(expectedResponse)) {
                    expectedResponseFound = true;
                }
            }
        }

        assertTrue("The expected response: " + expectedResponse + " was not received!", expectedResponseFound);
    }

    /**
     * The test will set the inactivitiyTimeout to a value of 5s.
     *
     * The below configuration will be used to set inactivityTimeout to 5s:
     * <tcpOptions inactivityTimeout="5s"/>
     *
     * Also the httpOptions readTimeout will be set back to 60s. This timeout should take priority
     * over the inactivityTimeout. This is the same test as the testInactivityTimeout_one_request test but now we're
     * expecting a response!
     *
     * A socket will be opened.
     *
     * The test will sleep for 20s which is 4X greater than the inactivityTimeout. A timeout should not occur since
     * the readTimeout should take priority and that is configured to 60 seconds.
     *
     * A request is sent and the initial response is read and validated to be the correct response from the Servlet.
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_with_readTimeout() throws Exception {
        String expectedResponse = "Response from InactivityTimeoutServlet!";
        boolean expectedResponseFound = false;

        String address = server.getHostname() + ":" + server.getHttpDefaultPort();

        String request = "GET /" + "InactivityTimeout" + "/InactivityTimeoutServlet" + " HTTP/1.1\r\n" +
                         "Host: " + address + "\r\n" +
                         "\r\n";

        // Set the inactivityTimeout to 5 seconds to make testing quicker than the default 60 second timeout.
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("5s");
        // Set the readTimeout back to the default of 60s.
        httpEndpoint.getHttpOptions().setExtraAttribute("readTimeout", "60s");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*InactivityTimeout.*");

        // Validate that inactivityTimeout is set to 5000 (5s).
        assertNotNull("The configured value of inactivityTimeout was not 5000!", server.waitForStringInTraceUsingMark("inactivityTimeout: 5000"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

        LOG.info("Creating a Socket connection.");
        URL url = HttpUtils.createURL(server, "/InactivityTimeoutServlet");
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {

            socket.setKeepAlive(true);

            // Wait for "TCPReadReques 1   read (async) requested for local: localhost/127.0.0.1:8010 remote: localhost/127.0.0.1:36762" and start sleeping after
            // to ensure the inactivtyTimeout is invoked. Only check when running Channel Framework. Netty won't read but we'll wait for the timeout two times
            // before actually timing out so the behavior is the same.
            if (!runningNetty) {
                server.waitForStringInTraceUsingMark(".*read \\(async\\) requested for local");
            }

            // Sleep 4X the inactivityTimeout since the read is retried one time.
            Thread.sleep(20000);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            LOG.info("Sending a request: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response:");
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                if (line.equals(expectedResponse)) {
                    LOG.info("Expected response was found!");
                    expectedResponseFound = true;
                }
            }

        }

        assertTrue("The expected response: " + expectedResponse + " was not received!", expectedResponseFound);
    }

    /**
     * The test will set the inactivitiyTimeout to a value of 0.
     *
     * The below configuration will be used to set inactivityTimeout to 0:
     * <tcpOptions inactivityTimeout="0"/>
     *
     * A socket will be opened.
     *
     * The test will sleep for 90 seconds which is longer than any default timeout.
     * The readTimeout, writeTimeout and inactivityTimeout default to 60 seconds.
     *
     * The goal of the test is to verify that all timeouts are disabled.
     *
     * A request is sent and the initial response is read and validated to be the correct response from the Servlet.
     *
     * @throws Exception
     */
    @Test
    public void testInactivityTimeout_read_write_timeouts_zero() throws Exception {
        String expectedResponse = "Response from InactivityTimeoutServlet!";
        boolean expectedResponseFound = false;

        String address = server.getHostname() + ":" + server.getHttpDefaultPort();

        String request = "GET /" + "InactivityTimeout" + "/InactivityTimeoutServlet" + " HTTP/1.1\r\n" +
                         "Host: " + address + "\r\n" +
                         "\r\n";

        // Set the inactivityTimeout to 5 seconds to make testing quicker than the default 60 second timeout.
        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        httpEndpoint.getTcpOptions().setInactivityTimeout("0");

        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(configuration);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false, "CWWKT0016I:.*InactivityTimeout.*");

        // Validate that inactivityTimeout is set to 0.
        assertNotNull("The configured value of inactivityTimeout was not 0!", server.waitForStringInTraceUsingMark("inactivityTimeout: 0"));

        // Ensure the TCP Channel has started.
        // CWWKO0219I: TCP Channel defaultHttpEndpoint has been started and is now listening for requests on host *  (IPv4) port 8010.
        assertNotNull("The TCP Channel was not started!", server.waitForStringInLogUsingMark("CWWKO0219I"));

        LOG.info("Creating a Socket connection.");
        URL url = HttpUtils.createURL(server, "/InactivityTimeoutServlet");
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {

            socket.setKeepAlive(true);

            // Wait for "TCPReadReques 1   read (async) requested for local: localhost/127.0.0.1:8010 remote: localhost/127.0.0.1:36762" and start sleeping after
            // to ensure the inactivtyTimeout is invoked. Only check when running Channel Framework. Netty won't read but we'll wait for the timeout two times
            // before actually timing out so the behavior is the same.
            if (!runningNetty) {
                server.waitForStringInTraceUsingMark(".*read \\(async\\) requested for local");
            }

            Thread.sleep(90000);

            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            LOG.info("Sending a request: " + request);
            os.write(request.getBytes());

            LOG.info("Read the response:");
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                if (line.equals(expectedResponse)) {
                    LOG.info("Expected response was found!");
                    expectedResponseFound = true;
                }
            }
        }

        assertTrue("The expected response: " + expectedResponse + " was not received!", expectedResponseFound);
    }
}
