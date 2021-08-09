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
package com.ibm.ws.sib.comms;

import javax.security.auth.Subject;

/**
 * An interface used to notify TRM code running in an ME that a new client
 * has connected to it.  This code is <strong>not</strong> applicable to the 
 * client side of the connection.
 */
public interface ComponentData
{
   /**
    * This method is invoked when a client based ClientConnection object
    * sends data to an ME via the exchange method.  Each time the exchange
    * method is invoked on the client side, this method will be invoked on the
    * ME side.
    * <p>
    * It is anticpated that the code in this method will invoke the send
    * method of the ClientConnection object passed as an argument.  This is
    * required to respond to the client's exchange and hence unblock it. 
    * @see ConnectionProperties 
    * @see ClientConnection#trmHandshakeExchange(byte[])
    * @see ClientComponentHandshake#connect(ClientConnection)
    * @param cc The client connection object which is a "peer" of the
    * object which had the exchange method invoked upon it.
    * @param data The data passed across the network via the exchange call.
    * @return byte[] The TRM data handhsake to be passed back to TRM on the client.
    */
   byte[] handShake(ClientConnection cc, byte[] data);	
   
   
   /**
    * This method is invoked when an initiating ME based MEConnection object
    * sends data to a receiving ME via the exchange method.  Each time the exchange
    * method is invoked on the initiating ME side, this method will be invoked 
    * on the destination ME.
    * <p>
    * @see ConnectionProperties 
    * @see MEConnection#trmHandshakeExchange(byte[])
    * @see MEComponentHandshake#connect(MEConnection)
    * @param me The ME connection object which is a "peer" of the
    * object which had the exchange method invoked upon it.
    * @param data The data passed across the network via the exchange call.
    * @return byte[] The TRM data handhsake to be passed back to TRM on the client.
    */
   byte[] handShake(MEConnection me, byte[] data);
   
   
   /**
    * This methid is invoked by a client who wishes to name specific properties about
    * a messaging engine and get a connection to it that way. This is commonly used by
    * the MA88 client code when a remote MA88 client connects and requests to conenct
    * to a messaging engine.
    * 
    * @param dc The instance of DirectConnection that specifies the connection properties.
    * @param subject The credentials to be passed to MP when creating the connection
    *
    * @return Returns true if a connection was available.
    */
   boolean directConnect(DirectConnection dc, Subject subject);
}
