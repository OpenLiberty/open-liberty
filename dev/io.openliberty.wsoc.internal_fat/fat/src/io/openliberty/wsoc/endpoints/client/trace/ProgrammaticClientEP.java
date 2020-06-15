/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.trace;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public abstract class ProgrammaticClientEP extends Endpoint implements TestHelper {

    private static final Logger LOG = Logger.getLogger(ProgrammaticClientEP.class.getName());

    /**
     *
     */
    public ProgrammaticClientEP() {
        // TODO Auto-generated constructor stub
    }

    public WsocTestContext _wtr = null;

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    public static class CloseTest extends ProgrammaticClientEP {
        private String[] _data = {};

        public CloseTest(String[] data) {
            _data = data;
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            _wtr.addMessage(closeReason.getCloseCode().getCode() + ":" + closeReason.getReasonPhrase());
            _wtr.setClosedAlready(true);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
         */
        @Override
        public void onOpen(Session sess, EndpointConfig arg1) {
            try {
                sess.getBasicRemote().sendText(_data[0]);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }

        }

    }

    public static class CloseTestOnOpen extends ProgrammaticClientEP {
        private String[] _data = {};

        public CloseTestOnOpen(String[] data) {
            _data = data;
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            if ((closeReason.getCloseCode().getCode() == 1006) && (closeReason.getReasonPhrase().equals("WebSocket Read EOF"))) {
                System.out.println("Jetty detected premature closure...");
            }
            _wtr.addMessage(closeReason.getCloseCode().getCode() + ":" + closeReason.getReasonPhrase());

            _wtr.setClosedAlready(true);

            if (_wtr.limitReached()) {
                _wtr.terminateClient();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.websocket.Endpoint#onOpen(javax.websocket.Session, javax.websocket.EndpointConfig)
         */
        @Override
        public void onOpen(Session sess, EndpointConfig arg1) {}

    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        try {
            session.close();
        } catch (IOException e) {
            _wtr.addExceptionAndTerminate("Error closing session", e);
        }
    }

    @Override
    public void onError(Session session, java.lang.Throwable throwable) {

        _wtr.addExceptionAndTerminate("Error during wsoc session", throwable);
    }

}
