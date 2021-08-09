/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package context.war;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.openliberty.wsoc.common.Constants;

/**
 *
 */
@ServerEndpoint(value = "/sessionHandlerErrorEndpoint")
public class SessionMessageHandlerErrorServerEP {

    String messageToSend = new String("Message Start. ");

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec)
    {
        try {
            session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {

                @Override
                public void onMessage(ByteBuffer val) {}
            });
        } catch (IllegalStateException ise) {
            messageToSend = messageToSend + "...Inside onOpen, adding binary correctly caught IllegalStateException.";

            try {
                session.addMessageHandler(new MessageHandler.Whole<PongMessage>() {

                    @Override
                    public void onMessage(PongMessage msg) {}
                });
            } catch (IllegalStateException ise2) {
                messageToSend = messageToSend + "...Inside onOpen, adding pong correctly caught IllegalStateException.";
                return;
            } catch (Throwable t) {
                messageToSend = messageToSend + "...Inside onOpen, adding pong caught the wrong exception: " + t + "..." + Constants.FAILED;
                return;
            }

            messageToSend = messageToSend + "...Inside onOpen, Error, adding pong did not catch any exception..." + Constants.FAILED;
            return;

        } catch (Throwable t) {
            messageToSend = messageToSend + "...Inside onOpen, adding binary caught the wrong exception: " + t + "..." + Constants.FAILED;
            return;
        }

        messageToSend = messageToSend + "...Inside onOpen, Error, adding binary did not catch any exception..." + Constants.FAILED;
        return;

    }

    @OnMessage
    public String receiveMessage(String message, Session messageSession) {

        // try to make another text message handler, which should result in an error, verify the error and return

        try {
            messageSession.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String val) {}
            });
        } catch (IllegalStateException ise) {

            messageToSend = messageToSend + "...Inside receiveMessage, correctly caught IllegalStateException..." + Constants.SUCCESS;
            return messageToSend;
        } catch (Throwable t) {
            messageToSend = messageToSend + "...Inside receiveMessage, caught the wrong exception: " + t + "..." + Constants.FAILED;
            return messageToSend;
        }

        messageToSend = messageToSend + "...Error: did NOT catch any exception.  Expected an IllegalStateException..." + Constants.FAILED;
        return messageToSend;

        // messageToSend will get automatically sent back to the client since it is the String that is returned by this onMessage routine. 

    }

    @OnMessage
    public ByteBuffer receiveMessage(ByteBuffer message, Session messageSession) {
        // no-op, don't expected to get called during the test.
        return null;
    }

    @OnMessage
    public byte[] onMessage(PongMessage msg, Session pongSession) {
        // no-op, don't expected to get called during the test.
        return null;
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // just in case someone sends this message after onClose is called:
        messageToSend = messageToSend + "...in onClose with reason of: " + reason;
    }

    @OnError
    public void onError(Session session, Throwable t) {
        // maybe we can still send a string back, at least it is worth trying.
        messageToSend = messageToSend + "...Error: onError was called with..." + t + "..." + Constants.FAILED;
        try {
            session.getBasicRemote().sendText(messageToSend);
        } catch (IOException e) {
        }
    }

}
