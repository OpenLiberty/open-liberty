/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

import java.net.URI;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;

import com.ibm.ws.fat.util.SharedServer;

import org.junit.Assert;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.endpoints.client.context.Session5ClientEP;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class WsocTest {

    private String host = "";
    private int port = 0;
    private boolean secure = false;
    private int alternatePort = 0;

    public WsocTest(String host, int port, boolean secure) {
        this.host = host;
        this.port = port;
        this.secure = secure;
    }


    private static final Logger LOG = Logger.getLogger(WsocTest.class.getName());

    public void runEchoTest(Object tep, String resource, Object[] data) throws Exception {
        runEchoTest(tep, resource, data, Constants.getDefaultTimeout());
    }

    public void runEchoTest(Object tep, String resource, Object[] data, int timeout) throws Exception {
        WsocTestContext testdata = runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, timeout);
        testdata.reThrowException();
        Assert.assertArrayEquals(data, testdata.getMessage().toArray());
    }

    /**
     * Run a test up until DEFAULT_MAX_MESSAGES (1) have been received or DEFAULT_TIMEOUT has occurred
     * Runs with default config. Endpoint should terminate client when numMsgsExpected is reached.
     * 
     * @param edp - Endpoint, either annotated or programmatic
     * @param uri - URI to connect to - int the form of ws:// or ws:///
     * @return WsocTextContext - test context
     * @throws Exception
     */
    public WsocTestContext runWsocTest(Object edp, String uri) throws Exception {

        LOG.info("Running a WSOC test with the default configurator");
        return runWsocTest(edp, uri, WsocTestRunner.getDefaultConfig(), WsocTestRunner.DEFAULT_MAX_MESSAGES, Constants.getDefaultTimeout());
    }

    /**
     * Run a test up until timeout has occurred or all clients are finished. Endpoint should terminate client when numMsgsExpected is reached.
     * 
     * @param edp - Endpoint, either annotated or programmatic
     * @param resource - URI to connect to - in the form of ws:// or ws:///
     * @param cfg - Endpoint config to use
     * @param numMsgsExpected - run the test until maxMessages is reached
     * @param timeout
     * @return WsocTextContext - test context
     * @throws Exception
     */
    public WsocTestContext runWsocTest(Object edp, String resource, ClientEndpointConfig cfg, int numMsgsExpected, int timeout) throws Exception {

        String url = getServerUrl(resource);
        URI uri = new URI(String.format(url));
        LOG.info("Creating new test runner with resource " + url + " number of messages expected: " + numMsgsExpected + " and timeout " + timeout);
        WsocTestRunner tc = new WsocTestRunner(edp, uri, cfg, numMsgsExpected, timeout);

        return tc.runTest();
    }

    /**
     * Run a multiple client test.
     * 
     * @param receiveEndpoints - Array of annotated or programmatic endpoints that may or may not do publishing...
     * @param publishEndpoint - single publisher endpoint that will likely be published. This endpoint waits for all other endpoints to be connected before connecting.
     * @param resource - URI to connect to - in the form of ws:// or ws:///
     * @param numMsgsExpected - Number of messages clients are expected to receive. Endpoint should terminate client when numMsgsExpected is reached.
     * @return MultiClienttestResult - test result with all receiver and publisher contexts and additional multi client test results.
     * @throws Exception
     */
    public MultiClientTestContext runMultiClientWsocTest(Object[] receiveEndpoints, Object publishEndpoint, String resource,
                                                         int numMsgsExpected) throws Exception {
        return runMultiClientWsocTest(receiveEndpoints, publishEndpoint, null, resource, WsocTestRunner.getDefaultConfig(),
                                      WsocTestRunner.DEFAULT_MAX_MESSAGES, Constants.getDefaultTimeout(), Constants.getConnectTimeout(), false);
    }

    /**
     * 
     * @param receiveEndpoints - Array of annotated or programmatic endpoints that may or may not do publishing...
     * @param publishEndpoint - single publisher endpoint that will likely be published. This endpoint waits for all other endpoints to be connected before connecting.
     * @param ptask - A publisher task that will run after all all receivers and the publisher is connected.
     * @param resource
     * @param cfg - Endpoint config to use
     * @param numMsgsExpected - - run the test until maxMessages are received.
     * @param runtime
     * @param connectTimeout
     * @param msgCountOnly
     * @return
     * @throws Exception
     */
    public MultiClientTestContext runMultiClientWsocTest(Object[] receiveEndpoints, Object publishEndpoint, PublishTask ptask, String resource,
                                                         ClientEndpointConfig cfg, int numMsgsExpected, int runtime, int connectTimeout,
                                                         boolean msgCountOnly) throws Exception {

        String url = getServerUrl(resource);
        URI uri = new URI(String.format(url));
        LOG.info("Creating new test runner with resource " + url + " maxMessages " + numMsgsExpected + " runtime " + runtime + " and timeout " + connectTimeout);
        MultiClientRunner tc = new MultiClientRunner(receiveEndpoints, publishEndpoint, ptask, uri, cfg);

        return tc.runTest(numMsgsExpected, runtime, connectTimeout, msgCountOnly);
    }

    public WsocTestContext[] runSession5WsocTest(Session5ClientEP[] endpoints, String resource,
                                                 ClientEndpointConfig cfg, int timeout, int testMode) throws Exception {

        String url = getServerUrl(resource);
        URI uri = new URI(String.format(url));
        LOG.info("Creating new test runner with resource " + url + " and timeout " + timeout);
        Session5TestClientRunner tc = new Session5TestClientRunner(endpoints, uri, cfg, timeout, testMode);

        return tc.runTest();
    }

    public void runEchoSingleObjectTest(Object tep, String resource, Object data) throws Exception {
        try {
            LOG.info("runEchoTest: " + tep.toString());
            WsocTestContext testdata = runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), 1, Constants.getLongTimeout());
            testdata.reThrowException();
            Object output = null;
            if (testdata.getMessage() != null) {
                output = testdata.getMessage().get(0).toString();
            }
            Assert.assertEquals(data.toString(), output);
        } catch (Throwable t) {
            Assert.fail("runEchoTest: Throwable String: " + t.toString() + "...Throwable message: " + t.getMessage());
        }
    }

    public String getServerUrl(String path) {

        StringBuilder url = new StringBuilder();
        if (secure) {
            url.append("wss://");
        }
        else {
            url.append("ws://");
        }
        url.append(host); // trust Simplicity to provide host
        url.append(":");
        url.append(port);

        if (path != null) {
            url.append(path);
        }
        return url.toString();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean getSecure() {
        return secure;
    }

    public void setAltPort(int port) {
        this.alternatePort = port;
    }

    public int getAltPort() {
        return alternatePort;
    }

}
