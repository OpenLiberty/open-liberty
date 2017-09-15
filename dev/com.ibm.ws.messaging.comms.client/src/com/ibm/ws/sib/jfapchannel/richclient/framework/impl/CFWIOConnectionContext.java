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
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import java.net.InetAddress;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.IOReadRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.IOWriteRequestContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;



/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext. It
 * basically wrappers the com.ibm.wsspi.tcp.channel.TCPConnectionContext code in the
 * underlying TCP channel.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext
 * @see com.ibm.wsspi.tcp.channel.TCPConnectionContext
 *
 * @author Gareth Matthews
 */
public class CFWIOConnectionContext implements IOConnectionContext
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CFWIOConnectionContext.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWIOConnectionContext.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
   }

   /** The TCP Connection Context */
   private TCPConnectionContext tcpCtx = null;

   /** The connection reference */
   private NetworkConnection conn = null;

   /**
    * @param tcpCtx
    */
   public CFWIOConnectionContext(NetworkConnection conn, TCPConnectionContext tcpCtx)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{conn, tcpCtx});
      this.conn = conn;
      this.tcpCtx = tcpCtx;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getLocalAddress()
    */
   public InetAddress getLocalAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLocalAddress");
      InetAddress localAddress = tcpCtx.getLocalAddress();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLocalAddress", localAddress);
      return localAddress;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getRemoteAddress()
    */
   public InetAddress getRemoteAddress()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemoteAddress");
      InetAddress remoteAddress = tcpCtx.getRemoteAddress();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemoteAddress", remoteAddress);
      return remoteAddress;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getLocalPort()
    */
   public int getLocalPort()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getLocalPort");
      int localPort = tcpCtx.getLocalPort();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getLocalPort", Integer.valueOf(localPort));
      return localPort;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getRemotePort()
    */
   public int getRemotePort()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getRemotePort");
      int remotePort = tcpCtx.getRemotePort();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getRemotePort", Integer.valueOf(remotePort));
      return remotePort;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getReadInterface()
    */
   public IOReadRequestContext getReadInterface()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getReadInterface");
      IOReadRequestContext readCtx = new CFWIOReadRequestContext(conn, tcpCtx.getReadInterface());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getReadInterface", readCtx);
      return readCtx;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext#getWriteInterface()
    */
   public IOWriteRequestContext getWriteInterface()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getWriteInterface");
      IOWriteRequestContext writeCtx = new CFWIOWriteRequestContext(conn, tcpCtx.getWriteInterface());
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getWriteInterface", writeCtx);
      return writeCtx;
   }

}
