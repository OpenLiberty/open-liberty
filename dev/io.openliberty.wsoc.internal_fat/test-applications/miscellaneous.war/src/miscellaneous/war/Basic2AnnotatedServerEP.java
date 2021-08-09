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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/basic2AnnotatedEndpoint")
public class Basic2AnnotatedServerEP {

    @OnMessage(maxMessageSize = 100000000)
    public String echoText(Reader val) {
        int x = 0;
        StringBuffer retValue = new StringBuffer();
        try {
            while ((x = val.read()) >= 0) {
                retValue.append((char) x);
            }
        } catch (IOException ie) {
            ie.printStackTrace();
        }

        return retValue.toString();

    }

    @OnMessage(maxMessageSize = 100000000)
    public byte[] echoText(InputStream data) {
        int x;

        try {
            if (data instanceof ByteArrayInputStream) {
                ByteArrayInputStream ba = (ByteArrayInputStream) data;
                byte[] retData = new byte[ba.available()];
                ba.read(retData);
                return retData;

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
        return null;

    }

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxTextMessageBufferSize(100000000);
        session.setMaxTextMessageBufferSize(100000000);

    }

}
