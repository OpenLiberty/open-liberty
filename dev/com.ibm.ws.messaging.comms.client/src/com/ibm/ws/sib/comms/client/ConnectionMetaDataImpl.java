/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client;

import java.net.InetAddress;
import javax.net.ssl.SSLSession;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.comms.ProtocolVersion;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.HandshakeProperties;

/**
 * Comms implementation of ConnectionMetaData that wrappers of JFAP ConversationMetData
 */
public class ConnectionMetaDataImpl implements ConnectionMetaData
{
   private final ConversationMetaData conversationMetaData;
   private final HandshakeProperties handshakeProperties;                              // F247975
   
   public ConnectionMetaDataImpl(ConversationMetaData conversationMetaData,
                                 HandshakeProperties handshakeProperties)        // F247975
   {
      this.conversationMetaData = conversationMetaData;
      this.handshakeProperties = handshakeProperties;                               // F247975
   }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#getChainName() */
   public String getChainName() { return conversationMetaData.getChainName(); }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#containsSSLChannel() */
   public boolean containsSSLChannel() { return conversationMetaData.containsSSLChannel(); }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#containsHTTPTunnelChannel() */
   public boolean containsHTTPTunnelChannel() { return conversationMetaData.containsHTTPTunnelChannel(); }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#isInbound() */
   public boolean isInbound() { return conversationMetaData.isInbound(); }
   
   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#isTrusted() */
   public boolean isTrusted() { return conversationMetaData.isTrusted(); }         // F224759.1
   
   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#getSSLSession() */
   public SSLSession getSSLSession() { return conversationMetaData.getSSLSession(); }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#requiresNonJavaBootstrap() */
   // begin F247975
   public boolean requiresNonJavaBootstrap()
   {
      return (handshakeProperties.getCapabilites() & 
              CommsConstants.CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP) == CommsConstants.CAPABILITIY_REQUIRES_NONJAVA_BOOTSTRAP;
   }
   // end F247975

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemoteAddress() */
   // begin F246161.5
   public InetAddress getRemoteAddress()
   {
      return conversationMetaData.getRemoteAddress();
   }
   // end F246161.5

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemotePortNumber() */
   public int getRemotePortNumber()
   {
      return conversationMetaData.getRemotePort();
   }

   /** @see com.ibm.ws.sib.comms.ConnectionMetaData#getProtocolVersion() */
   public ProtocolVersion getProtocolVersion()
   {
      final short fapLevel = handshakeProperties.getFapLevel();
      ProtocolVersion result = ProtocolVersion.UNKNOWN;

      if (fapLevel == 1)                       result = ProtocolVersion.VERSION_6_0;
      else if (fapLevel >= 2 && fapLevel <= 4) result = ProtocolVersion.VERSION_6_0_2;
      else if (fapLevel >= 5 && fapLevel <= 8) result = ProtocolVersion.VERSION_6_1;
      else if (fapLevel >= 9)                  result = ProtocolVersion.VERSION_7;
      
      return result;
   }

   /**
    * @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemoteCellName()
    */
   public String getRemoteCellName() 
   {
      return handshakeProperties.getRemoteCellName();
   }

   /**
    * @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemoteNodeName()
    */
   public String getRemoteNodeName() 
   {
      return handshakeProperties.getRemoteNodeName();
   }

   /**
    * @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemoteServerName()
    */
   public String getRemoteServerName() 
   {
      return handshakeProperties.getRemoteServerName();
   }

   /**
    * @see com.ibm.ws.sib.comms.ConnectionMetaData#getRemoteClusterName()
    */
   public String getRemoteClusterName() 
   {
      return handshakeProperties.getRemoteClusterName();
   }
}
