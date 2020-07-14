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

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import io.openliberty.wsoc.common.Constants;

/**
 * Simple websocket test runner class that will run a test until endpoint terminates the test (often using numMsgsExcpected)or until specified timeout occurs.
 */
public class WsocTestRunner {

    private static final Logger LOG = Logger.getLogger(WsocTestRunner.class.getName());

    private Object _clientEndpoint = null;

    private ClientEndpointConfig _cfg = null;

    private URI _uri = null;

    public static int DEFAULT_MAX_MESSAGES = 1;

    private static int _runTimeout = Constants.getDefaultTimeout();

    private WsocTestContext _wtr = null;

    public static ClientEndpointConfig getDefaultConfig() {
        Builder b = ClientEndpointConfig.Builder.create();
        return b.build();
    }

    public static ClientEndpointConfig getConfig(ClientEndpointConfig.Configurator configurator) {
        Builder b = ClientEndpointConfig.Builder.create();
        b.configurator(configurator);
        return b.build();
    }

    /**
     * 
     * @param edp - Annotated or programmatic endpoint
     * @param uri - URI to connect to - in the form of ws:// or ws:///
     * @param cfg - endpoint config
     * @param numMsgsExpected - Each client is expected to receive this many msgs before shutting down.. Endpoints should check the text context limit reached to determine when to
     *            shut down endpoint
     * @param runTimeout - how long test will run before stopping
     * 
     * @throws Exception
     */
    public WsocTestRunner(Object edp, URI uri, ClientEndpointConfig cfg, int numMsgsExpected, int runTimeout) throws Exception {
        _clientEndpoint = edp;
        _uri = uri;
        _runTimeout = runTimeout;
        _cfg = cfg;

        WsocTestContext.completeLatch = new CountDownLatch(1);
        _wtr = new WsocTestContext(numMsgsExpected);

        if (!(edp instanceof TestHelper)) {
            throw new WsocTestException("Test class does not implement TestHelper,   can't run this test.");
        }
        TestHelper th = (TestHelper) edp;
        th.addTestResponse(_wtr);

    }

    /**
     * Run the actual test
     * 
     * @return test context data including any messages added , exceptions thrown, if run timed out, etc.
     * @throws Exception
     */
    public WsocTestContext runTest() throws Exception {

        WebSocketContainer c = TestWsocContainer.getRef();
        LOG.info("Connecting to wsoc server container: " + c + " endpoint: " +
                 _clientEndpoint.getClass() + " config: " + _cfg  + ".");
        Session sess = null;
        if (_clientEndpoint instanceof Endpoint) {
            sess = c.connectToServer((Endpoint) _clientEndpoint, _cfg, _uri);
        }
        else {
            sess = c.connectToServer(_clientEndpoint, _uri);
        }

        LOG.info("Waiting for wsoc test to finish");

        if (!WsocTestContext.completeLatch.await(_runTimeout, TimeUnit.MILLISECONDS)) {
            _wtr.setTimedout(true);
        }
        // We've had some test failures that are not found in local env and some builds.  This short wait should flesh them out locally...    
        java.lang.Thread.sleep(50);
        if (sess.isOpen()) {
            //LOG.info("Reached max messages or test timeout, closing wsoc session");
            if (!_wtr.getClosedAlready()) {
                sess.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal Close"));
            }
        }
        return _wtr;

    }
}
