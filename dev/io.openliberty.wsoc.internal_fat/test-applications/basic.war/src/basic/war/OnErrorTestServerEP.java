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
package basic.war;

import java.io.IOException;

import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 *
 */
public class OnErrorTestServerEP {

    public String onOpenParamValue;
    //declared as static to preserve the value from onClose() until next invocation on onMessage() in ErrorTest
    public static String onCloseParamValue;
    //declared as static to preserve the value from onError() until next invocation on onMessage() in ErrorTest
    public static String onErrorParamValue = "Not Set";

    @ServerEndpoint(value = "/OnErrorTestEP/OnMessageError")
    public static class TestOnMessageError extends OnErrorTestServerEP {
        @OnMessage
        public String echoText(String text) {
            if (text.equals("FirstMessage"))
                throw new NullPointerException("First Message Error");
            if (text.equals("SecondMessage"))
                return onErrorParamValue;
            else
                return "I Shouldn't Get Here";
        }

        @OnError
        public void onError(final Session session, Throwable error) {
            // System.out.println("EXCEPTION in onError(): " + error + " " + error.getMessage());
            try {
                if (session != null && error != null) { //if the session is declared, runtime should be passing them in. Throwable is a mandatory parameter
                    if (error.getMessage().equals("First Message Error")) {
                        onErrorParamValue = "Test success";
                        session.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
