/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.jar;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/annotatedAddEndpoint")
public class JarAnnotatedEndpoint {

    private Session _curSess = null;

    @OnMessage
    public void echoText(String val) {

        Future<Void> future = _curSess.getAsyncRemote().sendText(val);

        // System.out.println("TestEndPointFuture: before future.get() at: " + System.currentTimeMillis());
        try {
            future.get();
        } catch (InterruptedException e) {
            //TODO log msg
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO log msg
            e.printStackTrace();
        }
        // System.out.println("TestEndPointFuture: after future.get()  at: " + System.currentTimeMillis());

    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }
}