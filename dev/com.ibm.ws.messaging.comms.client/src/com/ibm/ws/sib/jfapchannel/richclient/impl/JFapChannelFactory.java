/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.impl;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

/**
 * Factory class for JFAP channels.  Part of the code required to work in the channel framework.
 * @author prestona
 */
public class JFapChannelFactory implements ChannelFactory                                // D196658
{
   private static final TraceComponent tc = SibTr.register(JFapChannelFactory.class, 
                                                           JFapChannelConstants.MSG_GROUP, 
                                                           JFapChannelConstants.MSG_BUNDLE);

   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapChannelFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.22");
   }

   /** Constant for channel properties map to pass in connection table */
   public static final String CONNECTION_TABLE = "jfapchannel.CONNECTION_TABLE";

   /** Constant for channel properties map to pass in accept listener */
   public static final String ACCEPT_LISTENER = "jfapchannel.ACCEPT_LISTENER";
   
   /** Array to pass to out on getDeviceInterface() */
   private Class[] devSideInterfaceClasses = null;                                        // D232185
	
   private ChannelFactoryData channelFactoryData;                          // D196678.10.1
   
   /**
    * Creates a new channel.  Uses channel configuration to determine if the channel should be
    * inbound or outbound.
    * @see BaseChannelFactory#createChannel(com.ibm.websphere.channelfw.ChannelData)
    */
   protected Channel createChannel(ChannelData config) throws ChannelException   
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "createChannel", config);
      
      Channel retChannel;
      if (config.isInbound())
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "createChannel", "inbound");
         try
         {
            Class clazz = Class.forName(JFapChannelConstants.INBOUND_CHANNEL_CLASS);
            Constructor contruct = clazz.getConstructor(new Class[] 
                                                        {
                                                           ChannelFactoryData.class, 
                                                           ChannelData.class 
                                                        });
            retChannel = (Channel) contruct.newInstance(new Object[] 
                                                        { 
                                                           channelFactoryData, 
                                                           config 
                                                        });
         }
         catch (Exception e)
         {
            FFDCFilter.processException(e, 
                                        "com.ibm.ws.sib.jfapchannel.impl.JFapChannelFactory.createChannel",  
                                        JFapChannelConstants.JFAPCHANNELFACT_CREATECHANNEL_01,
                                        this);
            
            if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to instantiate inbound channel", e);

            // Rethrow as a channel exception
            throw new ChannelException(e);
         }
      }
      else
      {
         retChannel = new JFapChannelOutbound(channelFactoryData, config);
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "createChannel", retChannel);
      return retChannel;
   }
   
   /**
    * Returns the device side interfaces which our channels can work with.
    * This is always the TCPServiceContext.
    */
   public Class[] getDeviceInterface()                                    // F177053
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getDeviceInterface");        // F177053

      // Start D232185
      if (devSideInterfaceClasses == null)
      {
         devSideInterfaceClasses = new Class[1];
         devSideInterfaceClasses[0] = com.ibm.wsspi.tcpchannel.TCPConnectionContext.class;   // f167363, F184828, F189000
      }
      // End D232185
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getDeviceInterface", devSideInterfaceClasses);        // F177053
      return devSideInterfaceClasses;
   }

   // begin F177053
   public void init(ChannelFactoryData properties) throws ChannelFactoryException         // D194678
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "init", properties);
      channelFactoryData = properties;                                                    // D196678.10.1
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
   
   // begin D196658
   public Class getApplicationInterface()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getApplicationInterface");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getApplicationInterface");
      return JFapChannelFactory.class;                                           // 196678.10
   }
   // end D196658

@Override
public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
	// TODO Auto-generated method stub
   return new JFapChannelOutbound(channelFactoryData, config);
}

@Override
public OutboundChannelDefinition getOutboundChannelDefinition(
		Map<Object, Object> props) {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Map<Object, Object> getProperties() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void updateProperties(Map<Object, Object> properties)
		throws ChannelFactoryPropertyIgnoredException {
	// TODO Auto-generated method stub
	
}
}
