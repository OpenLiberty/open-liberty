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

import javax.ejb.EJB;
//import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
@ServerEndpoint(value = "/miscNoCDIInjectedEndpoint")
public class MiscNoCDIInjectedServerEP {

    @EJB
    StatelessInterface ejbBean;

    public static String verify1 = "...PostContructCalled";

    @OnMessage
    public void echoText(String val, Session session) {

        String toSend = "test failed ";

        if (ejbBean != null) {
            String s = ejbBean.getValue();
            if (s != null) {
                if (s.indexOf(verify1) != -1) {
                    // echo back input string which indicates success
                    toSend = val;
                } else {
                    toSend = toSend + s;
                }
            } else {
                toSend = toSend + "- injected EBJ was null";
            }
        }

        try {
            session.getBasicRemote().sendText(toSend);
        } catch (IOException e) {
        }

    }

    @OnOpen
    public void onOpen(Session session) {

    }

}
