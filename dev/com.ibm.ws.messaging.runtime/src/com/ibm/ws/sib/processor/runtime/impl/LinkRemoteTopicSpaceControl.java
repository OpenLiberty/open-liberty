/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.runtime.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLinkRemoteMessagePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitterControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkRemoteTopicSpaceControl extends RemoteTopicSpaceControl implements SIMPLinkRemoteMessagePointControllable
{
  private static final TraceComponent tc =
    SibTr.register(
      LinkRemoteTopicSpaceControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  MessageProcessor messageProcessor = null;
  String linkName = null;
    
  public LinkRemoteTopicSpaceControl(PubSubOutputHandler outputHandler, 
                                     MessageProcessor messageProcessor,
                                     String linkName)
                                      
  {
    super(outputHandler, null, messageProcessor);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "LinkRemoteTopicSpaceControl", new Object[] {
          outputHandler,
          messageProcessor,
          linkName});
         
    setType(ControllableType.LINK_TRANSMITTER);
    this.messageProcessor = messageProcessor;
    this.linkName = linkName;
    
    VirtualLinkControl linkControl = (VirtualLinkControl)
      ((MessageProcessorControl) messageProcessor.getControlAdapter())
              .getVirtualLinkByName(linkName);
    
    linkControl.addRemoteMessagePointControl(this);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "LinkRemoteTopicSpaceControl", this);
  }
  
  public VirtualLinkControl getLinkControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLinkControl");
    
    VirtualLinkControl linkControl = (VirtualLinkControl)
    ((MessageProcessorControl) messageProcessor.getControlAdapter())
            .getVirtualLinkByName(linkName);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLinkControl", linkControl);
    
    return linkControl;
  }

  public String getMessagingEngineName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMessagingEngineName");

    String name = super.getMessagingEngineName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMessagingEngineName", name);
    return name;
  }
  
  public String getTargetDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetDestination");

    String name = _outputHandler.getTopicSpaceMapping();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetDestination", name);
    return name;
  }
  
  @Override
  public void dereferenceControllable() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "dereferenceControllable");
    super.dereferenceControllable();
    getLinkControl().removeRemoteMessagePointControl(this);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");
  }

  public SIMPLinkTransmitterControllable getOutboundTransmit() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getOutboundTransmit");
    
    SIMPLinkTransmitterControllable outbound = null;
    //there will only be a stream set if the PSOH is not null
    if(_outputHandler!=null)
    {
      //at the moment there should be just one stream set in the
      //iterator
      SIMPIterator iterator = getPubSubOutboundTransmitIterator();
      if(iterator.hasNext())
      {
        outbound = (SIMPLinkTransmitterControllable)iterator.next();
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getOutboundTransmit", outbound);
    return outbound;
  }
  
  public boolean isPublicationTransmitter() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isPublicationTransmitter");
      SibTr.exit(tc, "isPublicationTransmitter", Boolean.valueOf(true));
    }
    return true;
  }
}
