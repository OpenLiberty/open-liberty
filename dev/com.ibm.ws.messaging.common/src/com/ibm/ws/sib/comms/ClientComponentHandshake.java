/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms;

/**
 * Represents a client component handshake for use by the client only.  An
 * instance of this class must be specified when a client attempts to connect
 * to an ME (using a ClientConnection object).  This is used to notify the
 * client of the success (or otherwise) of the connection attempt in an
 * asynchronous fashion.
 * @see com.ibm.ws.sib.comms.ClientConnection
 */
public interface ClientComponentHandshake
{
   /**
    * Invoked by the Comms code as part of processing an invocation of
    * the connect method on the ClientConnection class at the client side.
    * The implementation of this class is expected to use the exchange
    * method of the client connection to handshake with the ME side of the
    * connection.  
    * @see ClientConnection#connect(ConnectionProperties, ClientComponentHandshake)
    * @param cc The client connection object which issued the connection
    * attempt.
    * @return boolean The method should return true if the connection should
    * now be used, or false if the connection should be closed immediately.
    * Specifying false is equivalent to calling close immediately after the
    * ClientConnection connect method returns.
    */
   boolean connect(ClientConnection cc);
   
   /**
    * Invoked by the Comms code if a failure occurres whilst attempting to
    * process an invocation of the connect method on the ClientConnection
    * class.
    * @see ClientConnection#connect(ConnectionProperties, ClientComponentHandshake)
    * @param cc The client connection object which issued the connection
    * attempt
    * @param t A throwable which relates to the the problem which caused this
    * method to be invoked.  Under some circumstances this may be null.
    */
   void fail(ClientConnection cc, Throwable t);
   
}
