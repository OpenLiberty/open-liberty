/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import java.net.InetSocketAddress;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;                                                                             //PK58698
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget;
import com.ibm.ws.sib.utils.RuntimeInfo;                                                                       //PK58698
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;              // f167363, F188491

/**
 * Address object passed to the JFAP channel chain when establishing
 * an outbound connection.
 * @author prestona
 */
public class JFapAddress implements NetworkConnectionTarget
{
   private static final TraceComponent tc = SibTr.register(JFapAddress.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   static {
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapAddress.java, SIB.comms, WASX.SIB, uu1215.01 1.21");
   }

   private final InetSocketAddress remoteAddress;
   private final Conversation.ConversationType attachType;

   /**
    * Creates a new JFAP address.
    * @param remoteAddress the remote host to connect to.
    * @param attachType the type of outbound connection being established.
    */
   public JFapAddress(InetSocketAddress remoteAddress, Conversation.ConversationType attachType)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{remoteAddress, attachType});
      this.remoteAddress = remoteAddress;
      this.attachType = attachType;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Retrieve the address of the local NIC to bind to.
    * @see TCPConnectRequestContext#getLocalAddress()
    */
   public InetSocketAddress getLocalAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLocalAddress");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLocalAddress","rc="+null);
      return null;
   }

   /**
    * Retrieves the address of the remote address to connect to.
    * @see TCPConnectRequestContext#getRemoteAddress()
    */
   public InetSocketAddress getRemoteAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteAddress");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteAddress","rc="+remoteAddress);
      return remoteAddress;
   }

   // begin F188491
   /** @see TCPConnectRequestContext#getConnectTimeout() */
   public int getConnectTimeout()                                                                              //PK58698
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getConnectTimeout");

     int seconds = JFapChannelConstants.CONNECT_TIMEOUT_DEFAULT;
     try {
       seconds = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.CONNECT_TIMEOUT_JFAP_KEY, Integer.toString(seconds)));
     } catch (NumberFormatException e) {
       FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.impl.JFapAddress.getConnectTimeout", JFapChannelConstants.JFAPADDRESS_GETCONNECTTIMEOUT_01);
     }
     final int timeout = seconds * 1000; // seconds -> milliseconds

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getConnectTimeout","rc="+timeout);
     return timeout;
   }                                                                                                           //PK58698
   // end F188491

   /** Returns the type of connection that this address will be used to establish */
   public Conversation.ConversationType getAttachType()
   {
      return attachType;
   }

   public String toString()
   {
      return super.toString()+" [remoteAddress: "+remoteAddress+" attachType:"+attachType+"]";
   }
}
