/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.server;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundApplicationLink;

/**
 * A simple connection link implementation for the HTTP server channel.
 */
@SuppressWarnings("unused")
public class HTTPServerConnLink extends InboundApplicationLink {

    /**
     * Constructor
     * 
     * @param inVC
     */
    public HTTPServerConnLink(VirtualConnection inVC) {
        init(inVC);
        super.vc.getStateMap().put("TestServerConnLink", this);
    }

    public void ready(VirtualConnection inVC) {
        // Queue work into our own worker thread implementation. This is
        // required because the ready method is invoked on the TCPChannel's
        // thread and doing the work required to service a HTTP request may
        // be non-trivial.
        HTTPServerWorkQueue.getRef().queueWork(this);
    }

    public void destroy(Exception e) {
        // we have a connlink that does everything so ignore destroys
    }
}
