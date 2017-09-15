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

import java.net.InetSocketAddress;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget;
import com.ibm.ws.sib.jfapchannel.impl.JFapAddress;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.OutboundProtocol;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkConnection. It
 * basically wrappers the com.ibm.websphere.channelfw.VirtualConnection code in the
 * underlying TCP channel.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 * @see com.ibm.websphere.channelfw.VirtualConnection
 *
 * @author Gareth Matthews
 */
public class CFWNetworkConnection implements NetworkConnection
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(CFWNetworkConnection.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWNetworkConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
   }

   /** The virtual connection */
   private VirtualConnection vc = null;

   /**
    * @param vc
    */
   public CFWNetworkConnection(VirtualConnection vc)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", vc);
      this.vc = vc;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    * @return Returns the virtual connection.
    */
   VirtualConnection getVirtualConnection()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getVirtualConnection");
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getVirtualConnection", vc);
      return vc;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#requestPermissionToClose(long)
    */
   public boolean requestPermissionToClose(long timeout)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestPermissionToClose", Long.valueOf(timeout));
      boolean canProcess = vc.requestPermissionToClose(timeout);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestPermissionToClose", Boolean.valueOf(canProcess));
      return canProcess;
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#connectAsynch(com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget, com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener)
    */
   @SuppressWarnings("unchecked")   // Channel FW implements the state map
   public void connectAsynch(final NetworkConnectionTarget target, final ConnectRequestListener listener)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connectAsynch", new Object[]{target, listener});

      final CFWNetworkConnection readyConnection = this;

      // First create a TCP connection target object that simply wraps our original target object
      TCPConnectRequestContext tcpTarget = new TCPConnectRequestContext() {
         /**
          * @return Returns the local address from the target.
          * @see com.ibm.wsspi.tcp.channel.TCPConnectRequestContext#getLocalAddress()
          */
         public InetSocketAddress getLocalAddress()
         {
            return target.getLocalAddress();
         }
         /**
          * @return Returns the remote address from the target.
          * @see com.ibm.wsspi.tcp.channel.TCPConnectRequestContext#getRemoteAddress()
          */
         public InetSocketAddress getRemoteAddress()
         {
            return target.getRemoteAddress();
         }
         /**
          * @return Returns the connect timeout from the target.
          * @see com.ibm.wsspi.tcp.channel.TCPConnectRequestContext#getConnectTimeout()
          */
         public int getConnectTimeout()
         {
            return target.getConnectTimeout();
         }
      };

      // Create a TCP connection callback that wraps our own callback
      ConnectionReadyCallback callback = new ConnectionReadyCallback() {
         /**
          * Called when the connection has successfully connected.
          *
          * @see com.ibm.wsspi.channel.ConnectionReadyCallback#ready(com.ibm.websphere.channelfw.VirtualConnection)
          */
         public void ready(VirtualConnection conn)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ready", conn);
            listener.connectRequestSucceededNotification(readyConnection);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ready");
         }

         /**
          * Called if the connection fails.
          *
          * @see com.ibm.wsspi.channel.ConnectionReadyCallback#destroy(java.lang.Exception)
          */
         public void destroy(Exception exception)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy", exception);
            listener.connectRequestFailedNotification(exception);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");
         }
      };

      // Get the underlying JFapAddress and update the virtual connection state map
      JFapAddress jfapAddress = (JFapAddress)target;
      if ((jfapAddress != null) && (jfapAddress.getAttachType() == Conversation.CLIENT))
         vc.getStateMap().put(OutboundProtocol.PROTOCOL, "BUS_CLIENT");
      else
         vc.getStateMap().put(OutboundProtocol.PROTOCOL, "BUS_TO_BUS");

      // Now start the connect
      ((OutboundVirtualConnection)vc).connectAsynch(tcpTarget, callback);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connectAsynch");
   }

   /**
    *
    * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#getNetworkConnectionContext()
    */
   public NetworkConnectionContext getNetworkConnectionContext()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNetworkConnectionContext");

      ConnectionLink connLink = (ConnectionLink) ((OutboundVirtualConnection)vc).getChannelAccessor();
      NetworkConnectionContext context = new CFWNetworkConnectionContext(this, connLink);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNetworkConnectionContext", context);
      return context;
   }

}
