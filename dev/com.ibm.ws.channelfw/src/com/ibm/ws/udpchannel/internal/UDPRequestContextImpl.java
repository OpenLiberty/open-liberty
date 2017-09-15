/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import com.ibm.wsspi.udpchannel.UDPContext;

/**
 * Basic UDP request context object.
 * 
 * @author mjohnson
 */
public abstract class UDPRequestContextImpl {

    private UDPConnLink udpConnLink = null;
    private WorkQueueManager workQueueMgr = null;

    /**
     * Constructor.
     * 
     * @param udpContext
     * @param wqm
     */
    public UDPRequestContextImpl(UDPConnLink udpContext, WorkQueueManager wqm) {
        this.udpConnLink = udpContext;
        this.workQueueMgr = wqm;
    }

    /**
     * Access the UDP context object.
     * 
     * @return UDPContext
     */
    public UDPContext getInterface() {
        return this.udpConnLink;
    }

    /**
     * Access the UDP connection link object.
     * 
     * @return UDPConnLink
     */
    public UDPConnLink getConnLink() {
        return this.udpConnLink;
    }

    /**
     * Query whether this context is a read or write one.
     * 
     * @return boolean
     */
    public abstract boolean isRead();

    /**
     * Access the work queue manager for this context.
     * 
     * @return WorkQueueManager
     */
    protected WorkQueueManager getWorkQueueManager() {
        return this.workQueueMgr;
    }
}
