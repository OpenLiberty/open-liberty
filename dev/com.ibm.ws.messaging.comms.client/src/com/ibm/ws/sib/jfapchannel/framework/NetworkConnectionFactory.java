/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.framework;

import com.ibm.wsspi.channelfw.VirtualConnectionFactory;

/**
 * A factory for implementations of the NetworkConnection interface.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 */
public interface NetworkConnectionFactory
{
    /**
     * Creates a NetworkConnection interface implementation from an endpoint.
     * 
     * @see com.ibm.ws.sib.jfapchannel.XMLEndPoint
     * 
     * @param endpoint the endpoint to create a network connection from. This should be an instance
     *            of XMLEndPoint.
     * 
     * @return Returns a network connection which, at the point it is returned, is not connected to
     *         the remote peer.
     * 
     * @throws FrameworkException if the connection cannot be created.
     */
    NetworkConnection createConnection(Object endpoint) throws FrameworkException;

    /**
     * @return an unconnected network connection.
     * 
     * @throws FrameworkException if the connection cannot be created.
     */
    NetworkConnection createConnection() throws FrameworkException;

    /**
     * 
     * @return an VirtualConnectionFactory
     */
    VirtualConnectionFactory getOutboundVirtualConFactory();
}
