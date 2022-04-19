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

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.MetaDataProvider;
import com.ibm.ws.sib.jfapchannel.impl.JFapAddress;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.OutboundProtocol;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.OutboundProtocolLink;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * An outbound connection link.  This represents a single outbound connection.
 * This implementation is required to participate as a channel in the
 * Channel Framework.
 * @author prestona
 */
public class JFapOutboundConnLink extends OutboundProtocolLink implements MetaDataProvider
{
   private static final TraceComponent tc = SibTr.register(JFapOutboundConnLink.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   private ConversationMetaData metaData; // D196678.10.1
	
   static
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapOutboundConnLink.java, SIB.comms, WASX.SIB, uu1215.01 1.21");
   }

   /**
    * Creates a new outbound connection link.
    * 
    * @param vc
    */
   public JFapOutboundConnLink(VirtualConnection vc, ChannelFactoryData channelFactoryData,
            ChannelData channelData) // D196678.10.1
   {
      super(); // F177053
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]
      {
         vc, channelFactoryData, channelData
      });

      init(vc);
      
      // begin D196678.10.1
      try
      {
         ChainData[] chainDataArray = null;
         String channelName = channelData.getName();
         chainDataArray =
            ChannelFrameworkFactory.getChannelFramework().getInternalRunningChains( channelName);
         if (chainDataArray != null)
         {
            if ((chainDataArray.length != 1) && tc.isDebugEnabled())
               SibTr.debug(this, tc, "chain data contains more than one entry!");

            if (tc.isDebugEnabled())
               SibTr.debug(this, tc, "channelName="+channelName+" chainData="+chainDataArray[0]);

            metaData = new ConversationMetaDataImpl(chainDataArray[0], this);    // F206161.5
         }
         else if (tc.isDebugEnabled())
            SibTr.debug(this, tc, "cannot find a running chain for channel: "+channelName);
      }
      catch (ChannelException e)
      {
         FFDCFilter.processException
            (e, "com.ibm.ws.sib.jfapchannel.impl.JFapOutboundConnLink", JFapChannelConstants.JFAPOUTBOUNDCONNLINK_INIT_01);
         if (tc.isEventEnabled()) SibTr.exception(this, tc, e);
      }
      // end D196678.10.1

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Returns the interface provided by this channel. Always ourself.
    */
   public Object getChannelAccessor() // F177053
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getChannelAccessor"); // F177053
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getChannelAccessor", this); // F177053
      return this;
   }

   /**
    * Do we need to do processing once a connection is established.
    */
   // begin F176003
   protected void postConnectProcessing(VirtualConnection vc)     // F177053
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "postConnectionProcessing", vc);  // F177053
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "postConnectionProcessing");       // F177053
   }
   // end F176003
   
   /**
    * From a functional perspective, this method is not required.  All it does it invoke the
    * identical method on its superclass.  It does, however, give us the opportunity to
    * trace pertinant exception information as it flows through.
    */
   // begin D181601   
   public void close(VirtualConnection vc, Exception e)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "close", new Object[] {vc, e});
      if (tc.isEventEnabled() && (e != null)) SibTr.exception(this, tc, e);
      super.close(vc, e);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }
   // end D181601
   
   /**
    * From a functional perspective, this method is not required.  All it does it invoke the
    * identical method on its superclass.  It does, however, give us the opportunity to
    * trace pertinant exception information as it flows through.
    */
   // begin D181601
   public void destroy(Exception e)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy", e);
      if (tc.isEventEnabled() && (e != null)) SibTr.exception(this, tc, e);
      super.destroy(e);
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");
   }
   // end D181601
   
   // begin D196678.10.1
   public ConversationMetaData getMetaData()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getMetaData");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getMetaData", metaData);      
      return metaData;   
   }
   // end D196678.10.1
 
   @SuppressWarnings("unchecked") // Channel Framework implements state map...
   public void connect(Object address) throws Exception
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "connect", address);

      final JFapAddress jfapAddress = (JFapAddress) address;
      if ((jfapAddress != null) && (jfapAddress.getAttachType() == Conversation.CLIENT))
      {
         vc.getStateMap().put(OutboundProtocol.PROTOCOL, "BUS_CLIENT");
      }
      else
         vc.getStateMap().put(OutboundProtocol.PROTOCOL, "BUS_TO_BUS");
      
      super.connect(address);

      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "connect");
   }
}
