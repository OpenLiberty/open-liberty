/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.jar;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/pathparamonopentest/{String-var}/{Integer-var}")
public class PathParamOnOpenServerEP {
    public String onOpenParamValue;
    //declared as static to preserve the value from onClose() until next invocation on onMessage() in ErrorTest
    public static String onCloseParamValue;
    //declared as static to preserve the value from onError() until next invocation on onMessage() in ErrorTest
    public static String onErrorParamValue;

    @OnMessage
    public String echoText(Session sess, String text) {
        String returnText = null;
        if (onOpenParamValue != null) {
            returnText = text + "," + onOpenParamValue;
        }
        //  Only sent in test of upgrade servlet...
        Map<String, List<String>> themap = sess.getRequestParameterMap();

        if (themap != null) {
            List<String> lone = themap.get("TEST1");
            if (lone != null) {
                returnText = returnText + "," + lone.get(0);

            }
            List<String> ltwo = themap.get("TEST2");
            if (ltwo != null) {
                returnText = returnText + "," + ltwo.get(0);
            }
        }
        return returnText;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec, @PathParam("Integer-var") Integer integerVar) {
        if (session != null && ec != null) { //if the session & EndpointConfig are declared, runtime should be passing them in
            onOpenParamValue = integerVar.toString();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason, @PathParam("String-var") String stringVar) {
        try {
            if (session != null && reason != null) { //if the session & CloseReason are declared, runtime should be passing them in
                onCloseParamValue = stringVar;
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(final Session session, Throwable error, @PathParam("String-var") String stringVar) {

        try {
            if (session != null && error != null) { //if the session is declared, runtime should be passing them in. Throwable is a mandatory parameter
                onErrorParamValue = stringVar;
                session.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
