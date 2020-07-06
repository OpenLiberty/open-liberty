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
package basic.war;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import basic.war.coding.FormatOne;

/**
 * This test show-cases MessageHandler used in inheritance scenario
 */
public class BaseMsgHandlerServerEP extends Endpoint implements
                MessageHandler.Whole<FormatOne> {

    private Session session = null;

    @Override
    public void onOpen(final Session session, EndpointConfig ec) {
        this.session = session;
        session.addMessageHandler(this);
    }

    @Override
    public void onMessage(FormatOne formatOne) {
        try {
            this.session.getBasicRemote().sendObject(formatOne);
        } catch (Exception ex) {
            Logger.getLogger(BaseMsgHandlerServerEP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
