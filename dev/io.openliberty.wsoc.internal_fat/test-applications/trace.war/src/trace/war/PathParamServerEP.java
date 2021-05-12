/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trace.war;

import java.io.IOException;

import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
public class PathParamServerEP {

    @ServerEndpoint(value = "/runtimeexceptionTCK/{param1}")
    public static class RuntimeExceptionTCKTest extends PathParamServerEP {
        private final String MSG = "TEST PUPRPOSELY THROWS RUNTIME EXCEPTION";

        @OnMessage
        public String echoText(String text) {
            System.out.println("In RuntimeExceptionTCKTest.echoText() " + MSG);
            throw new RuntimeException(MSG);
        }

        @OnError
        public void onError(final Session session, Throwable error, @PathParam("{param1}") long param) {
            try {
                //success case
                if (error.getMessage().equals(MSG)) {
                    session.getBasicRemote().sendText(Long.toString(param));
                } else { //failure case
                    //print into logs
                    error.printStackTrace();
                    session.getBasicRemote().sendText("FAILURE");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
