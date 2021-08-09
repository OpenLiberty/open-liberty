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
package miscellaneous.war;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/partialAnnotatedEndpoint")
public class PartialAnnotatedServerEP {

    int length = 0;

    @OnMessage(maxMessageSize = 100000000)
    public void echoText(String val, Session session, boolean isLast) {
        try {
            session.getBasicRemote().sendText(val, isLast);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }

    }

    @OnMessage(maxMessageSize = 100000000)
    public void echoText(byte[] data, Session session, boolean isLast) {
        if (isLast) {
            length = 0;
        }
        length = length + data.length;
        try {
            ByteBuffer wb = ByteBuffer.wrap(data);
            System.out.println("RECEIVED " + data.length + " Sending " + wb.limit() + " total received: " + length);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(data), isLast);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxTextMessageBufferSize(100000000);
        session.setMaxTextMessageBufferSize(100000000);

    }

}
