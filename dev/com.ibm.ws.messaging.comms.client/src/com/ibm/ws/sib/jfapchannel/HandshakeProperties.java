/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

/**
 * Properties pertaining to the handshaking of a JFAP connection.  These properties may
 * be associated with a link level connection via invoking the conversation.setHandshakeProperties
 * method. 
 */
public interface HandshakeProperties 
{
   /**
    * @return the version of the FAP being used for this link.
    */
   short getFapLevel();
   
   /**
    * @return a bitmap providing information about the capabilities supported by the link.
    */
   short getCapabilites();
   
   /**
    * @return the major version of the Core SPI being used via the link.
    */
   short getMajorVersion();
   
   /**
    * @return the minor version of the Core SPI being used via the link.
    */
   short getMinorVersion();
   
   /**
    * Provides the name of the WAS cell that the remote process is associated with. 
    * 
    * <br>
    * This method can return null for several reasons:
    * <ul>
    *    <li>The cell name of the remote process can't be established</li>
    *    <li>The remote process didn't establish this connection</li>
    *    <li>The remote process isn't running inside an AppServer</li>
    * </ul>
    * <br>
    * 
    * NB: A non-null value will only be returned if the remote process established the connection.
    * 
    * @return the name of the WAS cell that the remote process is running in, or null if the information is not known.
    */
   String getRemoteCellName();
   
   /**
    * Provides the name of the WAS node that the remote process is associated with. 
    * <br>
    * This method can return null for several reasons:
    * <ul>
    *    <li>The node name of the remote process can't be established</li>
    *    <li>The remote process didn't establish this connection</li>
    *    <li>The remote process isn't running inside an AppServer</li>
    * </ul>
    * <br>
    * 
    * NB: A non-null value will only be returned if the remote process established the connection.
    * 
    * @return the name of the WAS node that the remote process is running in, or null if the information is not known.
    */
   String getRemoteNodeName();
   
   /**
    * Provides the name of the WAS server that the remote process is associated with. 
    * 
    * <br>
    * This method can return null for several reasons:
    * <ul>
    *    <li>The server name of the remote process can't be established</li>
    *    <li>The remote process didn't establish this connection</li>
    *    <li>The remote process isn't running inside an AppServer</li>
    * </ul>
    * <br>
    * 
    * NB: A non-null value will only be returned if the remote process established the connection.
    * 
    * @return the name of the WAS server that the remote process is running in, or null if the information is not known.
    */
   String getRemoteServerName();
   
   /**
    * Provides the name of the WAS cluster that the remote process is associated with. 
    * 
    * <br>
    * This method can return null for several reasons:
    * <ul>
    *    <li>The remote process isn't running in a WAS cluster</li>
    *    <li>The cluster name of the remote process can't be established</li>
    *    <li>The remote process didn't establish this connection</li>
    *    <li>The remote process isn't running inside an AppServer</li>
    * </ul>
    * <br>
    * 
    * NB: A non-null value will only be returned if the remote process established the connection.
    * 
    * @return the name of the WAS cluster that the remote process is running in, or null if the information is not known.
    */
   String getRemoteClusterName();   
}
