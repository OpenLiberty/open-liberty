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

import com.ibm.ws.sib.jfapchannel.ConversationMetaData;

/**
 * Contextual information about a network connection.  Implementations of
 * this interface may be obtained by invoking the getNetworkConnectionContext
 * method of a NetworkConnection interface implementation.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 */
public interface NetworkConnectionContext
{
   /**
    * Close the network connection.  This drops the link between this
    * process and its peer.  The user should ensure that a successful
    * call to NetworkConnection.requestPermissionToClose has been made
    * (and no further read or write requests have been made) prior to 
    * invoking this method.
    * @param networkConnection the network connection to close.
    * @param throwable an (optional) throwable which is used to indicate
    * the reason for closing the network connection.
    */
   void close(NetworkConnection networkConnection, 
              Throwable throwable);

   /**
    * @return an IO context used for reading and writing data to this
    * network connection.
    */
   IOConnectionContext getIOContextForDevice();
   
   /**
    * @return meta data about the connection.
    */
   ConversationMetaData getMetaData();
}
