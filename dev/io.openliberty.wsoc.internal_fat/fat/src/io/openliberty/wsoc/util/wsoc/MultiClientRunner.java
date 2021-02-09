/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import io.openliberty.wsoc.common.Constants;

/**
 * Class to run multiple websocket client tests.
 */
public class MultiClientRunner {

    private static final Logger LOG = Logger.getLogger(MultiClientRunner.class.getName());

    private Object[] _receiveEndpoints = null;

    private Object _publishEndpoint = null;

    private WsocTestContext[] _receiveClients = null;

    private WsocTestContext _publishClient = null;

    private PublishTask _publishTask = null;

    private ClientEndpointConfig _cfg = null;

    private URI _uri = null;

    public static int DEFAULT_MAX_MESSAGES = 1;

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();

    public static ClientEndpointConfig getDefaultConfig() {
        Builder b = ClientEndpointConfig.Builder.create();
        return b.build();
    }

    /**
     * Basic constructor. Assumes default endpoint config and designated publisher client.
     * 
     * @param receiveEndpoints - Array of annotated or programmatic endpoints.
     * @param uri - URI to connect to - in the form of ws:// or ws:///
     * @param cfg - endpoint config
     */
    public MultiClientRunner(Object[] receiveEndpoints, URI uri, ClientEndpointConfig cfg) {

        this(receiveEndpoints, null, null, uri, cfg);
    }

    /**
     * Constructor that allows a publisher client and publisher task.
     * 
     * @param receiveEndpoints - array of annotated or programmatic endpoint
     * @param publishEndpoint - single endpoint for publisher task that runs once all receiverEndpoints are connected.
     * @param ptask - task the publisher client will run.
     * @param uri - URI to connect to - in the form of ws:// or ws:///
     * @param cfg - endpoint config
     */
    public MultiClientRunner(Object[] receiveEndpoints, Object publishEndpoint, PublishTask ptask, URI uri, ClientEndpointConfig cfg) {
        _receiveEndpoints = receiveEndpoints;

        _uri = uri;
        _cfg = cfg;
        _receiveEndpoints = receiveEndpoints;
        _publishEndpoint = publishEndpoint;
        _publishTask = ptask;

    }

    /**
     * Run a multiple client test.
     * 
     * @param numMsgsExpected - Each client is expected to receive this many msgs before shutting down.. Endpoints should check the text context limit reached to determine when to
     *            shut down endpoint
     * @param runTime - How long the test will run, can do longer runs with this and setting messageCountOnly to true.
     * @param connectTimeout - How long to wait before receivers ( and publisher) connects before aborting test.
     * @param messageCountOnly- WsocTestContext will just count the number of messages received and not store them.. for longer runs.
     * @return
     * @throws Exception
     */
    public MultiClientTestContext runTest(int numMsgsExpected, int runTime, int connectTimeout, boolean messageCountOnly) throws Exception {

        WebSocketContainer c = TestWsocContainer.getRef();

        int numClients = _receiveEndpoints.length;

        WsocTestContext.connectLatch = new CountDownLatch(numClients);

        int total = numClients;
        if (_publishEndpoint != null) {
            total++;
        }
        WsocTestContext.completeLatch = new CountDownLatch(total);

        MultiClientTestContext mctr = new MultiClientTestContext();

        _receiveClients = new WsocTestContext[numClients];
        mctr.setReceiverContexts(_receiveClients);

        for (int x = 0; x < _receiveEndpoints.length; x++) {
            _receiveClients[x] = connectClient(_receiveEndpoints[x], c, numMsgsExpected, messageCountOnly);
        }

        if (connectTimeout > 0) {
            if (!WsocTestContext.connectLatch.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new IOException("Websocket Exception, all receiver clients did not connect within " + connectTimeout + " milliseconds.");
            }
        }

        WsocTestContext.connectLatch = new CountDownLatch(1);

        ExecutorService publishExecutor = null;

        if (_publishEndpoint != null) {
            _publishClient = connectClient(_publishEndpoint, c, numMsgsExpected, messageCountOnly);
            mctr.setPublisherContext(_publishClient);

            if (connectTimeout > 0) {
                if (!WsocTestContext.connectLatch.await(connectTimeout, TimeUnit.MILLISECONDS)) {
                    throw new IOException("Websocket Exception, publisher client did not connect within " + connectTimeout + " milliseconds.");
                }
            }

            if (_publishTask != null) {
                _publishTask.setMultiTestContext(mctr);
                publishExecutor = Executors.newSingleThreadExecutor();
                publishExecutor.execute(_publishTask);
            }
        }

        LOG.info("Waiting for wsoc test to finish");

        if (!WsocTestContext.completeLatch.await(runTime, TimeUnit.MILLISECONDS)) {
            mctr.setTestTimedout(true);
            while (WsocTestContext.completeLatch.getCount() > 0) {
                WsocTestContext.completeLatch.countDown();
            }
        }

        // We'll close the publisher first
        if (_publishEndpoint != null) {
            if (_publishTask != null) {
                //       java.lang.Thread.sleep(1000);
                publishExecutor.shutdownNow();
            }
            closeSession(_publishClient);
            //  java.lang.Thread.sleep(1000);

        }

        for (int x = 0; x < numClients; x++) {
            closeSession(_receiveClients[x]);
        }

        return mctr;

    }

    private void closeSession(WsocTestContext wtc) throws Exception {
        Session sess = wtc.getSession();
        if (sess != null) {
            if (sess.isOpen()) {
                LOG.info("Reached max messages or test timeout, closing wsoc session");
                sess.close();
            }
        }
    }

    private WsocTestContext connectClient(Object endpoint, WebSocketContainer c, int maxMessages, boolean messagesCountOnly) throws Exception {
        WsocTestContext wct = new WsocTestContext(maxMessages);

        if (!(endpoint instanceof TestHelper)) {
            throw new WsocTestException("Test class does not implement TestHelper,   can't run this test.");
        }
        TestHelper th = (TestHelper) endpoint;
        th.addTestResponse(wct);
//        LOG.info("Client " + x + " connecting to wsoc server...");

        if (endpoint instanceof Endpoint) {
            wct.addSession(c.connectToServer((Endpoint) endpoint, _cfg, _uri));
        }
        else {
            wct.addSession(c.connectToServer(endpoint, _uri));
        }
        return wct;

    }
}
