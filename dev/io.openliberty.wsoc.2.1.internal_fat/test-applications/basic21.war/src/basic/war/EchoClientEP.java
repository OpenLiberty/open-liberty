/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package basic.war;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

import jakarta.websocket.Endpoint;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.EndpointConfig;

import io.openliberty.wsoc.common.Utils;

/*
 * Echos messages sent to this endpoint.
 */
public class EchoClientEP extends Endpoint {

    Session session = null;

    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
            public void onMessage(String text) {
                try {

                    System.out.println("Received " + text);
                    session.close();
                } catch (IOException ioe) {

                }
            }
        });

        try {
            session.getBasicRemote().sendText("sending hello");
        } catch (IOException ioe) {

        }
    }

}
