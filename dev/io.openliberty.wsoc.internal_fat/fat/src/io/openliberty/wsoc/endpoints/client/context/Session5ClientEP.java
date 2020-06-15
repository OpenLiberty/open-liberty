/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.context;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.common.Constants;

/**
 *
 */
@ClientEndpoint
public class Session5ClientEP implements TestHelper {

    private static final Logger LOG = Logger.getLogger(Session5ClientEP.class.getName());
    public WsocTestContext _wtr = null;

    int localCount = 0;
    Session localSession = null;
    String writeData = "not initialized";
    String readData = "default read";

    public Session5ClientEP(int x) {
        localCount = x;

        // this string it matched by the server side app to know which client it is talking to.
        writeData = Constants.CLIENT_NUMBER + localCount;
    }

    @Override
    public void addTestResponse(WsocTestContext wtr) {
        _wtr = wtr;
    }

    @Override
    public WsocTestContext getTestResponse() {
        return _wtr;
    }

    @OnMessage
    public void getText(String x) {

        readData = x;
        _wtr.overwriteSingleMessage(x);

        if (x != null) {
            LOG.info("Client " + localCount + " received message: " + x);
            if ((x.indexOf(Constants.LATCH_DOWN) != -1)) {
                _wtr.messageLatchDown();
            }
        }
    }

    @OnOpen
    public void onOpen(Session sess) {
        _wtr.connected();
        localSession = sess;
        try {
            sess.getBasicRemote().sendText(writeData);
        } catch (Exception e) {
            _wtr.addExceptionAndTerminate("Error publishing initial message", e);

        }
    }

    @OnClose
    public void onClose(Session sess) {
        _wtr.terminateClient();
    }

    public void closeNow() {
        try {
            localSession.close();
        } catch (IOException e) {
        }
    }

}
