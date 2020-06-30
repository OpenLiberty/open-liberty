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

import java.util.concurrent.Future;

import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/annotatedFutureWithReturnText")
public class AnnotatedFutureWithReturnTextServerEP extends AnnotatedServerEP {

    private Session _curSess = null;

    @OnMessage
    public String echoText(String val) {

        // This is an async write, the other side will confirm we wrote it.
        Future<Void> future = _curSess.getAsyncRemote().sendText(val);

        // not return a String, so that we are doing a Sync Write (internally of the return value) on the same thread that has the async write outstanding
        String s = val + val;
        return s;

    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {

        _curSess = session;
    }
}