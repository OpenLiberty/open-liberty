/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.util.logging.Logger;

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
@ClientEndpoint
public class IdleTimeoutTCKClientEP implements TestHelper {

    public String[] _data = {};
    public int _counter = 0;
    public long startTime = 0;
    public long endTime = 0;
    public boolean closeCalledAlready = false;
    private boolean EXPECT_TIMEOUT_ERROR = false;
    private final String CLOSE_1006_ERROR_EXCEPTION = "org.eclipse.jetty.websocket.api.ProtocolException: Frame forbidden close status code: 1006";

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(IdleTimeoutTCKClientEP.class.getName());

    public IdleTimeoutTCKClientEP(String[] data) {
        _data = data;
        this.EXPECT_TIMEOUT_ERROR = true;
    }

    @OnMessage
    public String echoText(String data) {

        // should not see any messages coming from the server
        _wtr.addMessage(data);

        _wtr.terminateClient();

        return null;
    }

    @OnOpen
    public void onOpen(Session sess) {
        try {
            startTime = System.currentTimeMillis();
            String s = _data[_counter++];
            sess.getBasicRemote().sendText(s);
        } catch (Exception e) {
            _wtr.addExceptionAndTerminate("Error publishing initial message", e);

        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (!closeCalledAlready) {
            closeCalledAlready = true;

            endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // success if timeout is within 10 second of the 15 second idle timeout, rather than the 45 second test timeout
            int closeCode;
            //First look for closecode inside closeReasonPhrase because if server sends a closecode of of 1006, current jetty client 
            //implementation converts the closeCode to 1002 and closeReasonPhrase to "Invalid close code: 1006" before calling this 
            //onClose() method. Tyrus client does not do this conversion from 1006--> 1002 and TCK test case also expects 1006 from server
            //when idleTimeout occurs at the server
            if (closeReason.getReasonPhrase().contains("1006"))
                closeCode = 1006;
            else
                closeCode = closeReason.getCloseCode().getCode();

            LOG.info("idle tiemout time was: " + totalTime);
            LOG.info("closeReason is: " + closeCode);

            if ((closeCode == CloseReason.CloseCodes.CLOSED_ABNORMALLY.getCode())
                && (totalTime >= 15000) && (totalTime < 25000)) {
                _wtr.addMessage("SUCCESS");
            } else {
                _wtr.addMessage("FAILED: Total Time is: " + totalTime + " Close code is " + closeReason.getCloseCode().getCode());
            }

            _wtr.terminateClient();
        }
    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {
        // Client automatically throws an error when a 1006 response is added. Verify if this
        // is what is happening on this client and do not throw error when it does since as seen above
        // on the onClose we expect it to behave this way and need to verify it
        LOG.warning(throwable.toString());
        if (this.EXPECT_TIMEOUT_ERROR && throwable.toString().equals(CLOSE_1006_ERROR_EXCEPTION))
            LOG.info("Skipping error when receiving a 1006 response since it is expected.");
        else
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
