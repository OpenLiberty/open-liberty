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

import java.nio.ByteBuffer;

import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/basicAnnotatedEndpoint")
public class BasicAnnotatedServerEP {

    @OnMessage(maxMessageSize = 100000000)
    public String echoText(String val) {
        return val;

    }

    @OnMessage(maxMessageSize = 100000000)
    public ByteBuffer echoText(ByteBuffer data) {
        return data;
    }

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxTextMessageBufferSize(100000000);
        session.setMaxTextMessageBufferSize(100000000);

    }

}
