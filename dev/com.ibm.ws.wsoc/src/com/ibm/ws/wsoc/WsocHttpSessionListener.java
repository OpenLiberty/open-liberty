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
package com.ibm.ws.wsoc;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class WsocHttpSessionListener implements HttpSessionListener {

    private static final TraceComponent tc = Tr.register(WsocHttpSessionListener.class);

    EndpointManager endpointManager = null;

    public void initialize(EndpointManager em) {
        endpointManager = em;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionCreated(HttpSessionEvent event) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "HttpSession created: HttpSession ID: " + event.getSession().getId());
        }
        // nothing to do here, since later the code will add the session id to a map 

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {

        String id = event.getSession().getId();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "HttpSession destroyed: HttpSession ID: " + event.getSession().getId());
        }

        // close the websocket session from here if the websocket session was secure.
        if (endpointManager != null) {
            endpointManager.httpSessionExpired(id);
        }
    }

}
