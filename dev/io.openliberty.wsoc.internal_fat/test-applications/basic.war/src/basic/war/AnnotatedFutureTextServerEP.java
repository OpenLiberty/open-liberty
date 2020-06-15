/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//TODO - move this class into AnnotatedEndpoint once we get onMessage session argument passed in.
@ServerEndpoint(value = "/annotatedFutureText")
public class AnnotatedFutureTextServerEP extends AnnotatedServerEP {

    private Session _curSess = null;

    @OnMessage
    public void echoText(String val) {

        Future<Void> future = _curSess.getAsyncRemote().sendText(val);

        try {
            future.get();
        } catch (InterruptedException e) {
            //TODO log msg
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO log msg
            e.printStackTrace();
        }

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }
}