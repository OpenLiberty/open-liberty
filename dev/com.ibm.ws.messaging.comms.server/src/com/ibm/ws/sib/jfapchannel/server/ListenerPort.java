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
package com.ibm.ws.sib.jfapchannel.server;

import com.ibm.ws.sib.jfapchannel.AcceptListener;

/**
 * An abstraction of a port currently being listen upon for new connections.
 * Implementations can only be obtained by invoking the "listen" method of
 * the conneciton manager.
 * @author prestona
 */
public interface ListenerPort
{
   /**
    * Stop listening for new connections on this port.
    */
   void close();
   
   /**
    * Retrieve the accept listener previously associated with this port.
    * @return AcceptListener The Accept Listener.
    */
   AcceptListener getAcceptListener();
   
   /**
    * Retrieve the port number.
    * @return int Port number.
    */
   int getPortNumber();
}
