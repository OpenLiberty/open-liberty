/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.deployment;

import java.io.IOException;
import java.nio.ByteBuffer;

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
public class DeploymentClientEP implements TestHelper {

    public WsocTestContext _wtr = null;

    @ClientEndpoint
    public static class TextTest extends DeploymentClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public TextTest(String[] data) {
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

    }

    @ClientEndpoint
    public static class ByteArrayTest extends DeploymentClientEP {

        public byte[][] _data = {};
        public int _counter = 0;

        public ByteArrayTest(byte[][] data) {
            _data = data;
        }

        @OnMessage
        public byte[] echoData(byte[] data) {
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
                byte[] ba = _data[_counter++];
                sess.getBasicRemote().sendBinary(ByteBuffer.wrap(ba));
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }

    @ClientEndpoint
    public static class DeploymentTest extends DeploymentClientEP {

        public String[] _data = {};
        public int _counter = 0;

        public DeploymentTest(String[] data) {
            _data = data;
        }

        @OnMessage
        public String echoText(String data) {

            try {
                _wtr.addMessage(data);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing msg", e);
            }

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
