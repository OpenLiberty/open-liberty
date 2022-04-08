/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSession;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import io.netty.channel.Channel;

/**
 * Implementation of conversation meta data interface.
 * @see com.ibm.ws.sib.jfapchannel.ConversationMetaData
 */
public class ConversationMetaDataImpl implements ConversationMetaData
{
   private static final TraceComponent tc = SibTr.register(ConversationMetaData.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
   
   private String chainName;
   private boolean isInbound;
   //Romil liberty changes changed BaseChannelLink to ChannelLink
   private ConnectionLink baseLink;                                             // F206161.5
   
   // Romil liberty change make this method public so that server pacakge can access it
//   public ConversationMetaDataImpl(ChainData chainData, ConnectionLink baseLink, Channel channel)            // F206161.5
   public ConversationMetaDataImpl(ChainData chainData, ConnectionLink baseLink) 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{chainData, baseLink});   // F206161.5
      
      chainName = chainData.getName();
      isInbound = chainData.getType() == FlowType.INBOUND;
      this.baseLink = baseLink;
         
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "chainName="+chainName+
                                                     "\nisInbound="+isInbound);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /** @see com.ibm.ws.sib.jfapchannel.ConversationMetaData#getChainName() */
   public String getChainName()               { return chainName; }

   /** @see com.ibm.ws.sib.jfapchannel.ConversationMetaData#isInbound() */
   public boolean isInbound()                 { return isInbound; }

   /** @see com.ibm.ws.sib.jfapchannel.ConversationMetaData#getRemoteAddress() */
   // begin F206161.5
   public InetAddress getRemoteAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteAddress");
      InetAddress result = null;
      TCPConnectionContext tcpContext = null;
      ConnectionLink connLinkRef = baseLink.getDeviceLink();
      if (connLinkRef != null)
      {
         tcpContext = (TCPConnectionContext)connLinkRef.getChannelAccessor();
         if (tcpContext != null) result = tcpContext.getRemoteAddress();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteAddress", result);
      return result;
   }
   // end F206161.5
   
   /**
    * @return Returns the remote port number. Returns 0 if it is not known.
    */
   public int getRemotePort()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemotePort");
      
      int portNumber = 0;
      TCPConnectionContext tcpContext = null;
      ConnectionLink connLinkRef = baseLink.getDeviceLink();
      if (connLinkRef != null)
      {
         tcpContext = (TCPConnectionContext)connLinkRef.getChannelAccessor();
         if (tcpContext != null) portNumber = tcpContext.getRemotePort();
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemotePort", ""+portNumber);
      return portNumber;
   }
   
   /**
    * @return Returns the SSL session from the TCPConnectionContext
    * @see com.ibm.ws.sib.jfapchannel.ConversationMetaData#getSSLSession()
    */
   public SSLSession getSSLSession()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSSLSession");
      
      SSLSession sslSession = null;
      TCPConnectionContext tcpContext = null;
      ConnectionLink connLinkRef = baseLink.getDeviceLink();
      
      if (connLinkRef != null)
      {
         tcpContext = (TCPConnectionContext)connLinkRef.getChannelAccessor();
         if (tcpContext != null)
         {
            SSLConnectionContext sslConnCtx = tcpContext.getSSLContext();
            if (sslConnCtx != null) sslSession = sslConnCtx.getSession();
         }
      }
 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSSLSession", sslSession);
      return sslSession;
   }
}
