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
 
// NOTE: D181601 is not changed flagged as it modifies every line of trace and FFDC.

package com.ibm.ws.sib.jfapchannel.richclient.impl;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.channelfw.OutboundChannel;





/**
 * A JFAP channel which is used for outbound connections.  This is required
 * to participate as a channel in the Channel Framework.
 * @author prestona
 */
 public class JFapChannelOutbound implements OutboundChannel, OutboundChannelDefinition                                 // F189000
{
   private static final long serialVersionUID = -956779783769159680L;   // LIDB3706-5.209

	private static final TraceComponent tc = SibTr.register(JFapChannelOutbound.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
	
   private ChannelFactoryData channelFactoryData;// D196678.10.1
   private ChannelData chfwConfig = null;
   
	static
	{
		if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapChannelOutbound.java, SIB.comms, WASX.SIB, uu1215.01 1.17");
	}

	/**
	 * Create a new outbound channel
	 * @param cc
	 */
	public JFapChannelOutbound(ChannelFactoryData factoryData, ChannelData cc)              // F177053, D196678.10.1
	{		
		update(cc);
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {factoryData, cc});  // D196678.10.1
      channelFactoryData = factoryData;                                                   // D196678.10.1
      chfwConfig = cc;
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
	}

	/**
	 * Get a new outbound connection link.
	 */



	/**
	 * Notification that the channel configuration has been updated.
	 */
	public void update(ChannelData cc)                   // F177053
	{
		if (tc.isEntryEnabled()) SibTr.entry(this, tc, "update", cc);  // F177053
		// TODO: decide what to do if we are notified our configuration has changed.
		if (tc.isEntryEnabled()) SibTr.exit(this, tc, "update");   // F177053
		this.chfwConfig=cc;
	}

   // begin F177053
   public void start()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "start");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "start");
   }
   // end F177053

   // begin F177053   
   public void stop(long millisec)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "stop", ""+millisec);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "stop");
   }
   // end F177053

   // begin F177053
   public void init()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "init");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "init");
   }
   // end F177053

   // begin F177053
   public void destroy()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");
   }
   // end F177053


   // begin F177053
   public Class getDeviceAddress()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getDeviceAddress");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getDeviceAddress", TCPConnectRequestContext.class); // F188491
      return TCPConnectRequestContext.class;                                                             // F188491
   }
   // end F177053
   
   // begin F177053   
   public Class getDeviceInterface()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getDeviceInterface");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getDeviceInterface", TCPConnectionContext.class);   // F184828
      return TCPConnectionContext.class;     // F184828
   }
   // end F177053   

   // begin F189000
   public Class getOutboundFactory()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getOutboundFactory");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getOutboundFactory", JFapChannelOutbound.class);      
      return JFapChannelOutbound.class;
   }
   // end F189000

   // begin F189000
   public Map getOutboundFactoryProperties()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getOutboundFactoryProperties");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getOutboundFactoryProperties", null);
      return null;
   }
   // end F189000
   
   // begin F189000
   public Map getOutboundChannelProperties()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getOutboundChannelProperties");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getOutboundChannelProperties", null);
      return null;
   }
   // end F189000   
   
   public ConnectionLink getConnectionLink(VirtualConnection vc, ChannelData config) {
       if (tc.isEntryEnabled())
           SibTr.entry(this, tc, "getConnectionLink", vc);
       ConnectionLink retValue = new JFapOutboundConnLink(vc, channelFactoryData, config); // D196678.10.1 
       if (tc.isEntryEnabled())
           SibTr.exit(this, tc, "getConnectionLink", retValue);
       return retValue;
   }

public ConnectionLink getConnectionLink(VirtualConnection vc) {
	// TODO Auto-generated method stub
	return new JFapOutboundConnLink(vc,channelFactoryData,chfwConfig);
}


@Override
public String getName() {
	// TODO Auto-generated method stub
   return this.chfwConfig.getName();
}

@Override
public Class<?>[] getApplicationAddress() {
   // TODO Auto-generated method stub
   return null;
}

@Override
public Class<?> getApplicationInterface() {
   // TODO Auto-generated method stub
   return null;
}
}
