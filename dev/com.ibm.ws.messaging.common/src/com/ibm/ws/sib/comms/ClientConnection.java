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

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.exception.SIAuthenticationException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * A representation of the logical connection from a client to an ME.  This
 * object is available in both the client and ME environments.
 */
public interface ClientConnection extends CommsConnection
{
   /**
    * Attempts to establish a client connection with the remote ME identified
    * by the connection properties argument.  Comms allocates communications
    * resources to the connection and if successful, invoke the client
    * component handshake implementation passed as an argument.
    * <p>
    * Normally, the caller will be notified of a failure to connect by
    * the fail method of the ClientComponentHandshake object being invoked.
    * @param cp A connection properties object which identifies the ME to
    * attempt to connect to.
    * @param cch A client component handshake object to call and notify when
    * a connection attempt succeeds or fails.
    * @throws SIResourceException Indicates that the connection attempt
    * failed but also that something so catastrophic went wrong that the
    * normal failure reporting mechanism offered by the client component
    * handshake object could not be invoked.
    * @throws SIAuthenticationException Indicates that on an initial handshake
    * the user ID and password combination was rejected.
    */
   void connect(ConnectionProperties cp, ClientComponentHandshake cch)
      throws SIResourceException, SIAuthenticationException;

   /**
    * Assoicates a SICoreConnection with this client connection.  It is not
    * valid to invoke this method from a client - it may only be invoked on
    * the ME instance of ClientConection.
    * <p>
    * From a implementation perspective, invoking this method does little more
    * than storing a reference to the SICoreConnection inside the implementation
    * of ClientConnection.
    */
   void setSICoreConnection(SICoreConnection conn);

   /**
    * Returns the SICoreConnection previously associated with the client
    * connection object, or null if no connection has been previously
    * associated.
    * <p>
    * When invoked on the client instance of the ClientConnection object, this
    * results in the following sequence of events:
    * <ol>
    * <li> The client ClientConnection object engages in a line turn around
    * communication with its peer ClientConnection object on the ME.  This
    * is done to obtain information about any SICoreConnection object being
    * held by the peer.</li>
    * <li> The client ClientConnection object uses the information it receives
    * to build a "proxy" object for the SICoreConnection object being held by
    * its ME based peer,</li>
    * </ol>
    * @see ClientConnection#setSICoreConnection(SICoreConnection)
    * @return SICoreConnection A class which implements SICoreConnection.  For the
    * ME ClientConnection, this will be whatever was set using the
    * setSICoreConnection method.  In the client ClientConnection case this will
    * be a "proxy" SICoreConnection object which represents the object set
    * using setSICoreConnection on the ClientConnection's ME based peer.  If no
    * SICoreConnection has been set at the point this method is invoked, a value
    * of null will be returned.
    * @throws SIConnectionLostException Thrown in the client ClientConnection case where
    * a communications failure occures when trying to retrieve the required
    * data to build a SICoreConnection "proxy" object.
    */
   SICoreConnection getSICoreConnection() throws SIConnectionLostException;      // F174602

   /**
    * Performs a controlled close of this connection.  It is not valid to
    * invoke this method whilst within connect processing.  If possible, a
    * close request is proporgated to the remote "peer" associated with this
    * ClientConnection.
    */
   void close();

}
