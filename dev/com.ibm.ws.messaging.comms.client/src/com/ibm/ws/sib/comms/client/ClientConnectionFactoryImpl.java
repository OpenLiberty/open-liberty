/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.ClientConnection;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author Niall
 *
 * Used by TRM to return a ClientSideConnection. The ClientSideConnection is used by
 * TRM on the client to handshake with TRM on the server and is also the root object
 * of the Comms client code. So in effect, the Comms client side component is
 * bootstrapped by TRM by the createClientConnection method call.
 */
public class ClientConnectionFactoryImpl extends ClientConnectionFactory
{
   private static final TraceComponent tc =
      SibTr.register(
         ClientConnectionFactoryImpl.class,
         CommsConstants.MSG_GROUP,
         CommsConstants.MSG_BUNDLE);

   /**
    * Static constructor - creates an instance of the ConnectionFactory
    * class we will return from the "getInstance" method.
    * @see CommsFactory#getInstance()
    */
   static {
      if (tc.isDebugEnabled())
         SibTr.debug(
            tc,
            "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/ClientConnectionFactoryImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.9");
   }

   /**
    * Returns a ClientSideConnection that the TRM client side uses to 
    * handshake with the Server
    * @return ClientConnection
    */
   public ClientConnection createClientConnection()
   {
      return new ClientSideConnection();
   }

}
