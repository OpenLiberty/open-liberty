/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.war;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class ProgrammaticExtendCDIServerCDI12EP extends Endpoint implements
                MessageHandler.Whole<String> {

    private Session session = null;

    public @Inject
    CounterDependentScoped depScopedCounter;

    public @Inject
    CounterApplicationScoped appScopedCounter;

    // SessionScope not supported by WELD/CDI1.2

    @Override
    public void onOpen(final Session session, EndpointConfig ec) {
        this.session = session;
        session.addMessageHandler(this);
    }

    @Override
    public void onMessage(String msg) {
        Logger.getLogger(ProgrammaticExtendCDIServerCDI12EP.class.getName()).log(Level.INFO, "recieved msg", msg);
        String responseMessage = "Nothing yet";
        try {
            int depCount = depScopedCounter.getNext();
            depCount = depScopedCounter.getNext();

            int appCount = appScopedCounter.getNext();
            appCount = appScopedCounter.getNext();
            appCount = appScopedCounter.getNext();

            // string to test for on first iteration: "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3"
            responseMessage = "Dependent Scoped Counter: " + depCount + " ApplicationScopedCounter: " + appCount;

        } catch (Exception ex) {
            Logger.getLogger(ProgrammaticExtendCDIServerCDI12EP.class.getName()).log(Level.SEVERE, null, ex);
            responseMessage = ex.toString();
        }
        try {
            Logger.getLogger(ProgrammaticExtendCDIServerCDI12EP.class.getName()).log(Level.INFO, "Sending responseMessage", responseMessage);
            this.session.getBasicRemote().sendText(responseMessage);
        } catch (Exception ex) {
            Logger.getLogger(ProgrammaticExtendCDIServerCDI12EP.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
