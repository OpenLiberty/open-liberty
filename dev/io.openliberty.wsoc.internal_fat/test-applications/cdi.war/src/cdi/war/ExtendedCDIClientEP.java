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
package cdi.war;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 *
 */
public class ExtendedCDIClientEP extends Endpoint implements MessageHandler.Whole<String> {

    private Session session = null;

    public @Inject
    @Named("Producer1")
    StringBuffer N1;

    @Override
    public void onOpen(Session arg0, EndpointConfig arg1) {

        session = arg0;
        session.addMessageHandler(this);

        try {
            session.getBasicRemote().sendText("from clientEPExtended");
        } catch (IOException e) {
        }
    }

    @Override
    public void onMessage(String input) {

        if (input.compareTo(SimpleServerEP.sseRet) == 0) {
            String s = null;
            if (N1 == null) {
                s = "FailedTest: injection failed.";
            } else {
                s = N1.toString();
                if (s.indexOf(":ProducerBean Field Injected") == 0) {
                    s = "SuccessfulTest " + s;
                } else {
                    s = "FailedTest" + s;
                }
            }
            ClientTestServletCDI.clientEPTestMessage = s + " input from serverEP: " + input;
        } else {
            ClientTestServletCDI.clientEPTestMessage = "FailedTest input from serverEP: " + input;
        }
        ClientTestServletCDI.waitForMessage.incrementAndGet();
    }

}
