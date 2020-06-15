/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2013, 2015 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package basic.war;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

//TODO - move this class into AnnotatedEndpoint once we get onMessage session argument passed in.
@ServerEndpoint(value = "/annotatedAsyncText")
public class AnnotatedAsyncTextServerEP extends AnnotatedServerEP {

    private Session _curSess = null;

    volatile int echoHitCount = 0;
    volatile int resultHitCount = 0;

    @OnMessage
    public void echoText(String val) {

        echoHitCount++;
        int iterationCounter = 0;
        int iterationCountLimit = 20; // five seconds

        // if we enter here before we see the result for the last entry, we need to wait.  Can't do two async writes in a row.
        // wait for 5 seconds for condition to be met, then just go on anyways.
        while (echoHitCount - resultHitCount > 1) {
            try {
                iterationCounter++;
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }

            if (iterationCounter == iterationCountLimit) {
                return; // test will fail since we are not sending back data
            }
        }

        _curSess.getAsyncRemote().sendText(val, new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                resultHitCount++;
                boolean ok = result.isOK();
                if (!ok) {
                    // TODO - fail the test somehow?
                }
            }

        });
        return;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        echoHitCount = 0;
        resultHitCount = 0;
        _curSess = session;
    }
}