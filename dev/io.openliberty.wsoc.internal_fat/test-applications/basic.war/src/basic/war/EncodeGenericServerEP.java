/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import basic.war.coding.EncoderTextStream;

/**
 *
 */
@ServerEndpoint(value = "/EncodeGeneric", encoders = { EncoderTextStream.class, })
public class EncodeGenericServerEP {

    Session currentSession = null;

    @OnOpen
    public void onOpen(Session session, EndpointConfig ec) {
        currentSession = session;
    }

    @OnMessage
    public void sendBackEncodedText(String message) {

        Character c = 'Z';

        try {
            // WebSockets will identify the "Character" type as a type to encode by EncoderTextStream extends EncoderTextStreamGeneric<Character>
            // Character encoding should get used to send back the string in EncoderTextStreamGeneric<Character>
            currentSession.getBasicRemote().sendObject(c);
        } catch (IOException e) {
            System.out.println("Caught unexpected IOException: " + e);
        } catch (EncodeException e) {
            System.out.println("Caught unexpected EncodeException: " + e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        // no clean up is needed here for this sample
    }

    @OnError
    public void onError(Throwable t) {
        // no error processing will be done for this sample
    }

}
