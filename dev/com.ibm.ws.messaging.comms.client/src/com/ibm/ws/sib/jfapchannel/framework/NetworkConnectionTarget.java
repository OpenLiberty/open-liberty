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

import java.net.InetSocketAddress;

/**
 * Identifies a remote end point with which a network connection may
 * be established.  Users of this package should supply their implementation
 * of this interface to the connectAsynch method of a NetworkConnection
 * interface implementation in order to establish a network connection.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 */
public interface NetworkConnectionTarget
{
   /**
    * @return the local address to which the network connection should
    * be bound (or null if the system should assign an address).
    */
   InetSocketAddress getLocalAddress();
   
   /**
    * @return the remote address to which the network connection should
    * be established.
    */
   InetSocketAddress getRemoteAddress();
   
   /**
    * @return the amount of time (in milliseconds) to wait for the network 
    * connection to be established.
    */
   int getConnectTimeout();
}
