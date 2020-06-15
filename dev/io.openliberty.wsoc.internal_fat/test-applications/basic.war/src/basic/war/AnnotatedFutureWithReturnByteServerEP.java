/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/annotatedFutureWithReturnByte")
public class AnnotatedFutureWithReturnByteServerEP extends AnnotatedServerEP {

    private Session _curSess = null;

    @OnMessage
    public byte echoText(byte val) {

        // This is an async write, the other side will confirm we wrote it.
        _curSess.getAsyncRemote().sendObject(val);

        // not return a String, so that we are doing a Sync Write (internally of the return value) on the same thread that has the async write outstanding
        byte newval = (byte) (val + 11);
        return newval;

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }
}