/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.endpoints.client.basic;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import io.openliberty.wsoc.common.Constants;

/**
 *
 */
@ClientEndpoint
public class ProgrammaticMaxMessageSizeClientEP implements TestHelper {

    public String[] data = {};
    public int messageCounter = 0;
    public boolean closeCalledAlready = false;

    public WsocTestContext _wtr = null;
    private static final Logger LOG = Logger.getLogger(ProgrammaticMaxMessageSizeClientEP.class.getName());

    private Session session = null;

    public ProgrammaticMaxMessageSizeClientEP(String[] input) {
        data = input;
    }

    @OnMessage
    public void echoText(String data) {

        new Exception().printStackTrace();

        messageCounter++;
        _wtr.addMessage(data);

        if (messageCounter == 1 || messageCounter == 2) {

            // if messageCounter is 2, then the send of TEST_MAX_MSG_SIZE should fail with a close code of TOO_BIG (1009)

            // second message to send is a ByteBuffer, binary data,  with a deafult max size
            int iSize = (int) Constants.TEST_MAX_MSG_SIZE;
            byte[] ba = new byte[iSize];

            // initialize it for no real good reason
            for (int i = 0; i < iSize; i++) {
                ba[i] = (byte) i;
            }

            ByteBuffer buffer = ByteBuffer.wrap(ba);

            buffer.position(0);
            buffer.limit(iSize);
            try {
                LOG.info("client sending message of size: " + iSize);
                session.getBasicRemote().sendBinary(buffer);
            } catch (IOException e) {
            }
        }

        if (messageCounter > 2) {
            _wtr.terminateClient();
        }
    }

    @OnOpen
    public void onOpen(Session sess) {
        session = sess;
        try {
            // sess.getBasicRemote().sendText(_data[0]);
            int iSize = data[0].length();
            byte[] ba = new byte[iSize];

            // initialize it for no real good reason
            for (int i = 0; i < iSize; i++) {
                ba[i] = (byte) data[0].charAt(i);
            }

            ByteBuffer buffer = ByteBuffer.wrap(ba);

            buffer.position(0);
            buffer.limit(iSize);
            try {
                LOG.info("client sending first message of size: " + iSize);
                session.getBasicRemote().sendBinary(buffer);
            } catch (IOException e) {
            }

        } catch (Exception e) {
            _wtr.addExceptionAndTerminate("Error publishing initial message", e);

        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (!closeCalledAlready) {
            closeCalledAlready = true;

            LOG.info("closeReason is: " + closeReason.getCloseCode().getCode());

            if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.TOO_BIG.getCode()) {
                _wtr.addMessage("TOO_BIG or UNEXPECTED_CONDITION");
            } else if (closeReason.getCloseCode().getCode() == CloseReason.CloseCodes.UNEXPECTED_CONDITION.getCode()) {
                // seems that at least on loopback connection, if the server does a write-close-frame, then close socket sequence,
                // the client tcp stack only see the close socket sequence, so this test needs to tolerate that.
                _wtr.addMessage("TOO_BIG or UNEXPECTED_CONDITION");
            } else {
                _wtr.addMessage("FAILED: code was: " + closeReason.getCloseCode().getCode());
            }

            _wtr.terminateClient();
        }
    }

    @OnError
    public void onError(Session session, java.lang.Throwable throwable) {

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
