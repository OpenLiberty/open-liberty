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

/**
 * Represents a network connection which may be used to send and
 * receive data with a peer.  Network connections may be created using
 * an implementation of the NetworkConnectionFactory interface.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory
 */
public interface NetworkConnection
{
   /**
    * Request permission to close this network connection. 
    * @param timeout amount of time to wait for processing to complete such
    * that it is safe to close the network connection.
    * @return true if it is now safe to close the network connection.  False
    * otherwise.
    */
   boolean requestPermissionToClose(long timeout);
   
   /**
    * Asynchronously establish a network connection to the specified target.
    * @param target information about the remote network endpoint with which the
    * network connection should be established.
    * @param listener a listener implementation used to report successful (or
    * otherwise) establishement of a network connection.
    */
   void connectAsynch(NetworkConnectionTarget target,
                      ConnectRequestListener listener);
   
   /**
    * @return a network connection context which can be used to send or
    * receive data over a network connection.  A value of null will be returned
    * if the network connection is not connected, or was not successfully
    * connected.
    */
   NetworkConnectionContext getNetworkConnectionContext();
}
