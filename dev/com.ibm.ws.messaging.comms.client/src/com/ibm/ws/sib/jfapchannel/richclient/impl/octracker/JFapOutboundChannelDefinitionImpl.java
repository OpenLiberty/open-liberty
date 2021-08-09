/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.impl.octracker;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Our own implementation of the OutboundChannelDefinition interface.
 * We shouldn't really have this - but until we get a nicer way to change
 * the thread pool property of the TCP Channel - this will have to do.
 * Most of this code is lifted directly from:
 * com.ibm.ws.channel.framework.impl.OutboundChannelDefinitionImpl
 */
public class JFapOutboundChannelDefinitionImpl implements OutboundChannelDefinition
{
   private static final long serialVersionUID=6745291707003512194L;     // LIDB3706-5.209

   private static final TraceComponent tc = SibTr.register(JFapOutboundChannelDefinitionImpl.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
   
   private Class factory = null; 
   private Map factoryProps = null; 
   private Map<Object, Object> channelProps = null;
   
   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/impl/octracker/JFapOutboundChannelDefinitionImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
   }
   
   @SuppressWarnings("unchecked")
   public JFapOutboundChannelDefinitionImpl(OutboundChannelDefinition existingChanDef, Map newProps, boolean overwriteExisting) 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{existingChanDef, newProps, ""+overwriteExisting});
      
      // Copy the existing channel definition into this one. 
      factory = existingChanDef.getOutboundFactory(); 
      factoryProps = existingChanDef.getOutboundFactoryProperties(); 
      channelProps = existingChanDef.getOutboundChannelProperties(); 
       
      // Check for null props and replace with empty map. 
      if (factoryProps == null) { 
          factoryProps = new HashMap<Object, Object>(); 
      } 
      if (channelProps == null) { 
          channelProps = new HashMap<Object, Object>(); 
      } 
       
      // Ensure newProps is not null. 
      if (newProps == null) { 
          return; 
      } 
       
      // Loop through the set in new properties passed in.  
      Iterator iter = newProps.entrySet().iterator();
      Object propKey = null; 
      Object newValue = null; 
      while (iter.hasNext()) { 
          Map.Entry entry = (Map.Entry)iter.next();
          propKey = entry.getKey(); 
          if (channelProps.containsKey(propKey)) { 
              // Found the key in the existing props.  Determine if it can be overwritten. 
              if (overwriteExisting) { 
                  newValue = entry.getValue(); 
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                      Object oldValue = channelProps.get(propKey); 
                      SibTr.debug(tc, "Found existing property, "+propKey+", value "+oldValue+" replacing with "+newValue); 
                  } 
                  channelProps.put(propKey, newValue); 
              } else { 
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                      SibTr.debug(tc, "Found existing property, "+propKey+", but not overwriting"); 
                  }                     
              } 
          } else { 
              // Did not find the key so add it to the list. 
              newValue = entry.getValue();
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                  SibTr.debug(tc, "Adding property, "+propKey+", value "+newValue); 
              } 
              channelProps.put(propKey, newValue); 
          } 
      } 
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /** @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactory() */
   public Class getOutboundFactory()
   {
      return factory;
   }

   /** @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactoryProperties() */
   public Map getOutboundFactoryProperties()
   {
      return factoryProps;
   }

   /** @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundChannelProperties() */
   public Map getOutboundChannelProperties()
   {
      return channelProps;
   } 
}