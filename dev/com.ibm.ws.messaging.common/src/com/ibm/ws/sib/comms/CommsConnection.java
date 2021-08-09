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

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.ws.sib.mfp.ConnectionSchemaSet;

/**
 * A top level representation of a logical Comms Connection.
 *
 * @author Niall
 */
public interface CommsConnection
{
   /**
    * Exchanges TRM handshake data with this Connection's peer.
    * An exchange is defined as sending some data to our
    * "peer" then waiting until our "peer" responds with a reply.
    * <p>
    * This method will block the calling thread until the "peer" being
    * communicated with sends back a response.
    * @param data The data to send to the TRM component on this Connection's peer.
    * @return byte[] The data sent back by the peer in response.
    * @throws SIConnectionLostException Thrown if a communications problem is encountered
    * whilst attempting to send the data.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
    * @throws SIConnectionUnavailableException Thrown if it would be invalid to
    * attempt this operation at this time - for example if the Connection is
    * not currently in a connected state.
    */
   byte[] trmHandshakeExchange(byte[] data)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException;

   /**
    * Exchanges MFP handshake data with this Connection's peer.
    * An exchange is defined as sending some data to our
    * "peer" then waiting until our "peer" responds with a reply.
    * <p>
    * This method will block the calling thread until the "peer" being
    * communicated with sends back a response.
    * @param data The data to send to the MFP component on this Connection's peer.
    * @return byte[] The data sent back by the peer in response.
    * @throws SIConnectionLostException Thrown if a communications problem is encountered
    * whilst attempting to send the data.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed
    * @throws SIConnectionUnavailableException Thrown if it would be invalid to
    * attempt this operation at this time - for example if the Connection is
    * not currently in a connected state.
    */
   byte[] mfpHandshakeExchange(byte[] data)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException;

   /**
    * Attempts to send an MFP message schema to the peer at the highest possible
    * priority. At Message encode time MFP can discover that the destination entity
    * cannot decode the message about to be sent. This method is then called to
    * send a top priority transmission containing the message schema ahead of the
    * message relying on it to ensure it can be decoded correctly.
    * @param data The data to send to the MFP component on this Connection's peer.
    * @throws SIConnectionLostException Thrown if a communications failure occurres.
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed
    * @throws SIConnectionUnavailableException Thrown if it is invalid to invoke
    * this method at this point in time.  For example if the Connection has been
    * closed.
    */
   void sendMFPSchema(byte[] data)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException;

   // Start F247845
   /**
    * Used by the Message formatting layers to request an MFP schema in real-time. If a schema is
    * required before any processing can continue, they can be requested in this manner and this
    * call will block until the peer has responded to the request. In the event of catastrophic
    * failure on the server side (for example if the schema can no longer be located) an
    * SIErrorException will be thrown. It is not anticipated that this should happen, as MFP should
    * only request schema's from a peer they are sure has the schema (i.e. a peer who they received
    * the message from in the first place).
    * <p>
    * The format of the data is agnostic to the comms layer, as is the return data.
    *
    * @param data The data containing the information needed about the schemas by MFP at the peer.
    *
    * @return Returns data containing the information about the requested schemas.
    *
    * @throws SIConnectionLostException
    *    Thrown if a communications failure occurres.
    * @throws SIConnectionDroppedException
    *    Thrown if the underlying connection is closed
    * @throws SIConnectionUnavailableException
    *    Thrown if it is invalid to invoke this method at this point in time.  For example if the
    *    Connection has been closed.
    * @throws SIErrorException
    *    Thrown if the schema request fails at the peer side but the error was not due to
    *    communications failure.
    */
   byte[] requestMFPSchemata(byte[] data)
      throws
         SIConnectionLostException,
         SIConnectionDroppedException,
         SIConnectionUnavailableException,
         SIErrorException;
   // End F247845

   /**
    * @return Returns a friendly String containing information about where the connection
    *         is to.
    */
   String getConnectionInfo();

   /**
    * @return Returns connection meta-data, if available. A value of null is returned otherwise.
    */
   ConnectionMetaData getMetaData();

   /**
    * setSchemaSet
    * Sets the schemaSet in the underlying JFAP Connection.
    *
    * @param schemaSet   The SchemaSet which pertains to the underlying Connection.
    */
   public void setSchemaSet(ConnectionSchemaSet schemaSet);

   /**
    * getSchemaSet
    * Returns the MFP SchemaSet which pertains to the underlying JFAP Connection.
    *
    * @throws SIConnectionDroppedException Thrown if the underlying connection is closed.
    *
    * @return ConnectionSchemaSet The SchemaSet belonging to the underlying Connection.
    */
   public ConnectionSchemaSet getSchemaSet() throws SIConnectionDroppedException;
}
