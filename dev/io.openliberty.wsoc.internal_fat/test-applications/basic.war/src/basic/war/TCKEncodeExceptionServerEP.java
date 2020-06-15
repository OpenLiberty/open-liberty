/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.BinaryEncoder;
import io.openliberty.wsoc.common.BinaryFormater;

// This tests application defined binary encoder throwing runtime exception
@ServerEndpoint(value = "/TCKEncodeException", encoders = { BinaryEncoder.class,
})
public class TCKEncodeExceptionServerEP {
    Session session = null;

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        this.session = session;
    }

    //TCK test case
    @OnMessage
    public String tckEncoderThrowsException(String msg) {
        BinaryFormater returnFormater = new BinaryFormater("EXCEPTION");
        System.out.println("before sendObject");
        Future<Void> future = session.getAsyncRemote().sendObject(returnFormater);
        System.out.println("after sendObject");
        try {
            future.get();
            System.out.println("after get()");
            return "FAILURE";
        } catch (ExecutionException e) { //Success case. encoder throws RuntimeException intentionally which should be caught here when future.get() is executed.
            System.out.println("in ExecutionException");
            return "SUCCESS";
        } catch (InterruptedException e) {
            System.out.println("in InterruptedException");
            return "FAILURE";
        }
    }

    // Using the OnClose annotation will cause this method to be called when the WebSocket Session is being closed.
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        try {
            System.out.println("IN TCKEncodeExceptionServerEP onClose");
        } catch (Exception e) {
        }

    }

    // Using the OnError annotation will cause this method to be called when the WebSocket Session has an error to report. For the Alpha version
    // of the WebSocket implementation on Liberty, this will not be called on error conditions.
    @OnError
    public void onError(final Session session, Throwable t) {
        System.out.println("IN TCKEncodeExceptionServerEP onError() " + t.getMessage());
    }
}
