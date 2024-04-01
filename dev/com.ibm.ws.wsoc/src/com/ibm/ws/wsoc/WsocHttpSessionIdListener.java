/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Session listener to notify the EndpointManager's httpSessionMap sessionId key should be changed
 */
public class WsocHttpSessionIdListener implements HttpSessionIdListener {

    private static final TraceComponent tc = Tr.register(WsocHttpSessionIdListener.class);

    EndpointManager endpointManager = null;

    public void initialize(final EndpointManager em) {
        endpointManager = em;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionListener#sessionIdChanged(javax.servlet.http.HttpSessionEvent, java.lang.String)
     */
    @Override
    public void  sessionIdChanged(final HttpSessionEvent event, final String oldSessionId) {

        final String newSessionId = event.getSession().getId();

        // change the sessions
        if (endpointManager != null) {
            endpointManager.httpSessionIdChanged(newSessionId, oldSessionId);
        }

    }
}
