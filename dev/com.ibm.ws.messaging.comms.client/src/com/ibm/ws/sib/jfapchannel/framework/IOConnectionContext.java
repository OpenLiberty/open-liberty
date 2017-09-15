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

import java.net.InetAddress;

/**
 * Provides contextual information about a network connection.  Instances of this
 * class may be obtained by invoking the getNetworkConnectionContext method of
 * implementations of the the NetworkConnection interface.
 *  
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 */
public interface IOConnectionContext
{
   /**
    * @return the address of the local network adapter to which this connection is bound.
    */
   InetAddress getLocalAddress();
   
   /**
    * @return the address of the remote network adapter to which this connection is bound.
    */
   InetAddress getRemoteAddress();
   
   /**
    * @return the local port number that this network connection is using.
    */
   int getLocalPort();
   
   /**
    * @return the remote port number that this network connection is using.
    */
   int getRemotePort();
   
   /**
    * @return a read context that may be used for reading data from this network connection.
    */
   IOReadRequestContext getReadInterface();
   
   /**
    * @return a write context that may be used for writing data to this network connection.
    */   
   IOWriteRequestContext getWriteInterface();
}
