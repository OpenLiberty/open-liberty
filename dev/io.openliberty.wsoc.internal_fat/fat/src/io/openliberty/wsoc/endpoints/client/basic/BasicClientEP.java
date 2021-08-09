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
package io.openliberty.wsoc.endpoints.client.basic;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public class BasicClientEP implements TestHelper {

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(BasicClientEP.class.getName());

    @ClientEndpoint
    public static class TestOnOpen extends BasicClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestOnOpen(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

        @OnError
        public void onError(Session session, java.lang.Throwable throwable) {
            _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
        }
    }

    @ClientEndpoint
    public static class TestOnClose extends BasicClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestOnClose(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

        @OnError
        public void onError(Session session, java.lang.Throwable throwable) {

            _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
        }
    }

    @ClientEndpoint
    public static class TestOnError extends BasicClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TestOnError(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {
            LOG.info("BasicClientEP.TestOnError.echoText() " + data);

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
            else {
                return _data[_counter++];
            }
            return null;
        }

        @OnOpen
        public void onOpen(Session sess) {
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

        @OnError
        public void onError(Session session, java.lang.Throwable throwable) {

            _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {

        try {
            session.close();
        } catch (IOException e) {
            _wtr.addExceptionAndTerminate("Error closing session", e);
        }

    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

}
