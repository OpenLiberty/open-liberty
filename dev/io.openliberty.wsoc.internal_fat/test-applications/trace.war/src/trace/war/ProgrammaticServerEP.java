/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 *
 */
public abstract class ProgrammaticServerEP extends Endpoint {

    public static class TextEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {

                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (Exception ex) {
                        Logger.getLogger(TextEndpoint.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    public static class CloseEndpointOnOpen extends ProgrammaticServerEP {
        Session ses = null;

        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            ses = session;
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf("1001")), "THIS IS A TEST CLOSING STATUS FROM onOPEN"));

            } catch (Exception ex) {
                Logger.getLogger(ProgrammaticServerEP.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        public void onMessage(String text) {
            String[] vals = text.split(":");

            try {
                ses.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf("9999")), "SHOULD NOT GET HERE"));

            } catch (Exception ex) {
                Logger.getLogger(ProgrammaticServerEP.class.getName()).log(Level.SEVERE, null, ex);
            }

            return;
        }

        @Override
        public void onClose(Session session, CloseReason reason) {

            // Shouldn't usually call session.close in onClose, but there is a loop condition that this might catch.
            try {
                session.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
                e.printStackTrace();
            }

        }
    }

    public static class CloseEndpoint extends ProgrammaticServerEP {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {

            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {
                    String[] vals = text.split(":");

                    try {
                        // CloseCode = CloseCodes.
                        session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(Integer.valueOf(vals[0])), vals[1]));

                    } catch (Exception ex) {
                        Logger.getLogger(ProgrammaticServerEP.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

        }

        @Override
        public void onClose(Session session, CloseReason reason) {

            // Shouldn't usually call session.close in onClose, but there is a loop condition that this might catch.
            try {
                session.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onClose(Session session, CloseReason reason) {

    }

    @Override
    public void onError(Session session, Throwable thr) {

    }

}
