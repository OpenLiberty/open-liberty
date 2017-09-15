/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.upgrade;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.H2UpgradeHandler;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConnectionUpgrade;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * Wrap H2UpgradeHandler, since the project containing that class does not have access to HttpUpgradeHandler
 */
public class H2UpgradeHandlerWrapper implements HttpUpgradeHandler, TransportConnectionUpgrade {

    private final static TraceComponent tc = Tr.register(H2UpgradeHandlerWrapper.class, WebContainerConstants.TR_GROUP, LoggerFactory.MESSAGES);

    H2UpgradeHandler wrappedHandler;

    public H2UpgradeHandlerWrapper() {
    }
    
    public void init (H2UpgradeHandler handler) {
        wrappedHandler = handler;
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpUpgradeHandler#destroy()
     */
    @Override
    public void destroy() {
        if (wrappedHandler != null) {
            wrappedHandler.destroy();
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpUpgradeHandler#init(javax.servlet.http.WebConnection)
     */
    @Override
    public void init(WebConnection connection) {
        // NO-OP
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.transport.access.TransportConnectionUpgrade#init(com.ibm.ws.transport.access.TransportConnectionAccess)
     */
    @Override
    public void init(TransportConnectionAccess x) {
        if (wrappedHandler != null) {
            wrappedHandler.init(x);
        }        
    }

}
