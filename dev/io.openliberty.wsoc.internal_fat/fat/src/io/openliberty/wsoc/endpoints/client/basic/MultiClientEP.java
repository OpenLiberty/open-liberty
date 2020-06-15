/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.io.IOException;

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
public class MultiClientEP implements TestHelper {

    public WsocTestContext _wtr = null;

    @ClientEndpoint
    public static class SimpleReceiverTest extends MultiClientEP {

        @OnMessage
        public void echoText(String data) {

            _wtr.addMessage(data);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();
        }
    }

    @ClientEndpoint
    public static class SimplePublisherTest extends MultiClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public SimplePublisherTest(String[] data) {
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
            _wtr.connected();
            try {
                String s = _data[_counter++];
                sess.getBasicRemote().sendText(s);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class NoPublishNoReceiveTest extends MultiClientEP {

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();

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

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
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
