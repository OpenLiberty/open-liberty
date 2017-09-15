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
package com.ibm.wsspi.udpchannel;

import java.net.InetAddress;

/**
 * A context object encapsulating data related to a UDPChannel.
 * 
 */
public interface UDPContext {

    /**
     * Get the Read Object for this interface
     * 
     * @return UDPReadRequestContext
     */
    UDPReadRequestContext getReadInterface();

    /**
     * Get the Write Object for this interface
     * 
     * @return UDPWriteRequestContext
     */
    UDPWriteRequestContext getWriteInterface();

    /**
     * Get the InetAddress for the locally bound interface.
     * 
     * @return InetAddress
     */
    InetAddress getLocalAddress();

    /**
     * Get the port number for the locally bound interface.
     * 
     * @return int
     */
    int getLocalPort();

}
