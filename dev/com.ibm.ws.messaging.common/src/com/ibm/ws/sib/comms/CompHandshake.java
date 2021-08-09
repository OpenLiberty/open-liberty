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
 * Implementing this interface allows a component to participate in the handshake
 * phase when a new Connection is estalished with a peer. Once Comms handshaking
 * has completed, a components CompStartHandshake method will be driven to alert
 * it to the handshaking phase. Dur
 *
 * @author Niall
 */
public interface CompHandshake
{
   /**
    * Comms will drive this method upon establishing a connection with a new
    * peer to allow a component to facilitate a handshake with the same component
    * on the peer. When called, this method may use the methods supplied on the 
    * CommsConnection object to either handshake or send message schema data.
    * @param conn The CommsConnection associated with the peer
    * @param productVersion The version of the product which is running on the 
    * remote side of the connection
    * @return boolean indicating success or failure of the handshake. A failure
    * will drive Comms to close the connection
    */
   boolean compStartHandshake(CommsConnection conn, int productVersion);
      
   /**
    * This method is called when the connection to the remote machine has 
    * terminated, for whatever reason. It can be used by the handshake component 
    * to clean up any state information.
    * @param conn The CommsConnection that has been closed
    */
   void compClose(CommsConnection conn);
      
   /**
    * Called whenever data is received from the peer.  
    * @param conn The CommsConnection associated with the peer
    * @param productVersion The version of the product which is running on the 
    * remote side of the connection
    * @param data The byte buffer sent by the remote partner ComponentHandshake 
    * method. This method will be called whenever unsolicited data is received 
    * for a component which does not satisfy an outstanding handshake request
    * @return Returns true if the data was received by the component successfully.
    */
   boolean compData(CommsConnection conn, int productVersion, byte[] data);

   /**
    * Called when handshake data is received from the peer.  
    * @param conn The CommsConnection associated with the peer
    * @param productVersion The version of the product which is running on the 
    * remote side of the connection
    * @param data The byte buffer sent by the remote partner CompStartHandshake 
    * method. 
    * @return byte[] Any reply to the handshake
    */
   byte[] compHandshakeData(CommsConnection conn, int productVersion, byte[] data);
   
   // Start F247845
   /**
    * Called when a process makes a request of the peer. Currently there is only one request that is
    * expected - which is when MFP request a Schema (most probably for a multicast message that has
    * been received).
    * <p>
    * The <code>packetId</code> parameter will indicate what type of request is being made. For MFP
    * schema requests this will be <code>JFapChannelConstants.SEG_REQUEST_MFP_SCHEMA.
    * <p>
    * If the request cannot be fulfilled (for example if the schema cannot be located) a null 
    * should be returned from this method. This indicates to the communications layer that an 
    * unexpected error occurred and it will send this error to the client.
    * 
    * @param conn The comms connection associated with the connection
    * @param productVersion The product version
    * @param packetId Used to distinguish the type of request.
    * @param data The data
    * 
    * @return Returns a byte array containing the reply information that should be sent to the 
    *         requester.
    */
   byte[] compRequest(CommsConnection conn, int productVersion, int packetId, byte[] data);
   // End F247845
}
