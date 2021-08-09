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

/**
 * Callback interface invoked as part of TRM handshake for ME to ME connections.
 * The (TRM) implementor of this interface can use the methods it provides to
 * determine if a ME to ME connection attempt should succeed or not.  It is
 * anticipated this will be done as part of the handshake which takes place
 * when an ME to ME connection is esablished.
 * @see com.ibm.ws.sib.comms.MEConnection
 */
public interface MEComponentHandshake
{
   /**
    * This method is invoked in the originating ME when a new MEConnection
    * is being established.  The implementation of this method will, more than
    * likely, invoke the exchange method on the MEConnection passed as an
    * argument to flow TRM data.  The value returned by this method is used
    * to determine if the connection attempt should be allowed to proceed.
    * @param meConnection The MEConnection for which a connection attempt is
    * being made.
    * @return boolean True iff the connection attempt should be allowed to
    * proceed.  If False is returned then the connection is closed.
    */
   boolean connect(MEConnection meConnection);
   
   /**
    * Invoked to indicate that a communications problem occurred during the
    * connection process.
    * @param meConnection The MEConnection object which was attempting to
    * establish a connection.
    * @param throwable The exceptional condition which prevented the connection
    * from being established or sustained.
    */
   void fail(MEConnection meConnection, Throwable throwable);
}
