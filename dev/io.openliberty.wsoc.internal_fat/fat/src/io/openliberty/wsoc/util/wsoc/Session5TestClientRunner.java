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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Builder;
import javax.websocket.Endpoint;
import javax.websocket.WebSocketContainer;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.endpoints.client.context.Session5ClientEP;

/**
 *
 */
public class Session5TestClientRunner {

    private Session5ClientEP[] _endpoints;
    private ClientEndpointConfig _cfg = null;
    private URI _uri = null;

    public static int DEFAULT_MAX_MESSAGES = 1;
    private int _timeout = Constants.getDefaultTimeout();

    private int _testMode = 0;

    private WsocTestContext[] _receiveClients = null;

    public static ClientEndpointConfig getDefaultConfig() {
        Builder b = ClientEndpointConfig.Builder.create();
        return b.build();
    }

    /**
     * 
     */
    public Session5TestClientRunner(Session5ClientEP[] endpoints,
                                    URI uri, ClientEndpointConfig cfg, int timeout, int testMode) throws Exception {

        if (endpoints == null) {
            return;
        }

        _endpoints = endpoints;
        _uri = uri;
        _timeout = timeout;
        _cfg = cfg;
        _testMode = testMode;

    }

    public WsocTestContext[] runTest() throws Exception {

        WebSocketContainer wsc = TestWsocContainer.getRef();

        if (_testMode == 1) {
            return runTest1(wsc);
        }

        if (_testMode == 2) {
            return runTest2(wsc);
        }

        return null;
    }

    public WsocTestContext[] runTest1(WebSocketContainer c) throws Exception {

        // connect the first 3 clients, on the server side they will wait to see that all three are connected, and each send back a response message
        _receiveClients = new WsocTestContext[5];
        WsocTestContext.connectLatch = new CountDownLatch(3);
        WsocTestContext.messageLatch = new CountDownLatch(3);

        for (int x = 0; x <= 2; x++) {
            _receiveClients[x] = connectClient(_endpoints[x], c);
        }

        // waiting for 3 connects and 3 messages
        // on the server side, session 0 will send back success.
        WsocTestContext.connectLatch.await(_timeout, TimeUnit.MILLISECONDS);
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // to avoid a race condition, get the latch ready for the next step before closing session 0.
        WsocTestContext.messageLatch = new CountDownLatch(2);

        // close the first client connection
        _endpoints[0].closeNow();

        // wait for session 1 and 2 to see that session 0 closed, and then send back messages.
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // session 3 will now start. on the server side session 3 onOpen and onMessage should see only sessions 1, 2 and 3 
        WsocTestContext.connectLatch = new CountDownLatch(1);
        WsocTestContext.messageLatch = new CountDownLatch(3);
        _receiveClients[3] = connectClient(_endpoints[3], c);
        WsocTestContext.connectLatch.await(_timeout, TimeUnit.MILLISECONDS);
        // wait for session 3 so say it is active and for session 1 and 2 to say that they see 3 and therefore success for them
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        WsocTestContext.messageLatch = new CountDownLatch(1);
        // now close 1 and 2
        _endpoints[1].closeNow();
        _endpoints[2].closeNow();

        // wait for session 3 to respond that it sees only itself
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // closed session 3 so that no session are now open
        _endpoints[3].closeNow();

        // start session 4, it should see one session, itself,  in onOpen and onMessage at the server side
        WsocTestContext.connectLatch = new CountDownLatch(1);
        WsocTestContext.messageLatch = new CountDownLatch(1);

        _receiveClients[4] = connectClient(_endpoints[4], c);
        WsocTestContext.connectLatch.await(_timeout, TimeUnit.MILLISECONDS);
        // wait for session 4 to get back to us
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // closed session 4 so that no session are now open
        _endpoints[4].closeNow();

        return _receiveClients;

    }

    /*
     * test session parameter for onClose and onError
     * client 0,1 send back a message.
     * client side closes client 0.
     * server side session 0, test that onclose is called with all both sessions in the onClose session, and updates a static property.
     * once client 1 sees the there is only 1 session, it looks for the update user property and sends back success or failure.
     */

    public WsocTestContext[] runTest2(WebSocketContainer c) throws Exception {

        // connect the two clients, on the server side they will wait to see that all three are connected, and each send back a response message
        _receiveClients = new WsocTestContext[2];
        WsocTestContext.connectLatch = new CountDownLatch(1);
        WsocTestContext.messageLatch = new CountDownLatch(2);

        for (int x = 0; x <= 1; x++) {
            _receiveClients[x] = connectClient(_endpoints[x], c);
            WsocTestContext.connectLatch.await(_timeout, TimeUnit.MILLISECONDS);
            WsocTestContext.connectLatch = new CountDownLatch(1);
        }

        // waiting for 2 messages so we know they are active
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // to avoid a race condition, get the latch ready for the next step before closing session 0.
        WsocTestContext.messageLatch = new CountDownLatch(1);

        // close the first client connection
        _endpoints[0].closeNow();

        // wait for session 1 to send back it's status
        WsocTestContext.messageLatch.await(_timeout, TimeUnit.MILLISECONDS);

        // now close 1
        _endpoints[1].closeNow();

        return _receiveClients;

    }

    private WsocTestContext connectClient(Object endpoint, WebSocketContainer c) throws Exception {
        WsocTestContext wct = new WsocTestContext();

        if (!(endpoint instanceof TestHelper)) {
            throw new WsocTestException("Test class does not implement TestHelper,   can't run this test.");
        }
        TestHelper th = (TestHelper) endpoint;
        th.addTestResponse(wct);

        if (endpoint instanceof Endpoint) {
            wct.addSession(c.connectToServer((Endpoint) endpoint, _cfg, _uri));
        }
        else {
            wct.addSession(c.connectToServer(endpoint, _uri));
        }
        return wct;

    }

}
