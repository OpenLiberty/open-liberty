/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.context;

import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;

/**
 *
 */
public class SimpleClientEP implements TestHelper {

    private static final Logger LOG = Logger.getLogger(Session5ClientEP.class.getName());

    public WsocTestContext _wtr = null;

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    @ClientEndpoint
    public static class WriteAndRead extends SimpleClientEP {

        String writeData = "default write";

        public WriteAndRead(String x) {
            writeData = x;
        }

        @OnMessage
        public void getText(String x) {

            if (x == null) {
                x = "null";
            }

            LOG.info("Client received message: " + x);
            _wtr.overwriteSingleMessage(x);

            _wtr.terminateClient();
            return;
        }

        @OnOpen
        public void onOpen(Session sess) {
            _wtr.connected();
            try {
                sess.getBasicRemote().sendText(writeData);
            } catch (Exception e) {
                _wtr.addExceptionAndTerminate("Error publishing initial message", e);

            }
        }

    }
}
