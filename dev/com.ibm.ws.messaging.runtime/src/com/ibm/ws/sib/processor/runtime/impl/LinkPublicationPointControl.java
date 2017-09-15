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
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitterControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkPublicationPointControl extends InternalOutputStreamSetControl implements
    SIMPLinkTransmitterControllable {

  private static TraceComponent tc =
    SibTr.register(
        LinkPublicationPointControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private DestinationHandler _topicspace = null;
  LinkRemoteTopicSpaceControl control = null;

  public LinkPublicationPointControl(StreamSet streamSet) 
  {
    super(streamSet);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkPublicationPointControl", streamSet);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkPublicationPointControl", this);
  }
  
  public void setParentControlAdapter(ControlAdapter parent)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setParentControlAdapter", parent);
    
    super.setParentControlAdapter(parent);
    control = (LinkRemoteTopicSpaceControl)parent;
    this._topicspace = control.getOutputHandler().getDestinationHandler();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setParentControlAdapter");
  }

  public long getTimeSinceLastMessageSent() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTimeSinceLastMessageSent");
      
    long maxTime=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      InternalOutputStreamControl streamControl = 
        (InternalOutputStreamControl)iterator.next();
      long streamTime = streamControl.getLastMsgSentTime();
      if (streamTime > maxTime) maxTime = streamTime;
    }
    
    if (maxTime == 0)
      maxTime = -1; // No previous message
    else
      maxTime = System.currentTimeMillis() - maxTime;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTimeSinceLastMessageSent", maxTime);
    
    return maxTime;
  }

  public boolean isPutInhibited() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isPutInhibited");
    
    boolean isPI = !(_topicspace.isSendAllowed());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isPutInhibited", Boolean.valueOf(isPI));
    
    return isPI;
  }

  public String getLinkType() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkType");
    
    String type = control.getLinkControl().getType();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkType", type);
    
    return type;
  }

  public String getLinkUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkUuid");
    
    String uuid = control.getLinkControl().getId();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkUuid", uuid);
    
    return uuid;
  }

  public String getLinkName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkName");
    
    String name = control.getLinkControl().getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkName", name);
    
    return name;
  }

  public String getTargetBusName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTargetBusName");
    
    String name = control.getLinkControl().getTargetBus();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetBusName", name);
    
    return name;
  }

  public String getTargetEngineUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTargetEngineUuid");
      SibTr.exit(tc, "getTargetEngineUuid", super.getRemoteEngineUuid());
    }
    return super.getRemoteEngineUuid();
  }

  public void reallocateAllTransmitMessages() throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException {
    // Reallocates all messages on the outbound streams so that they
    // are sent to a different localization.  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reallocateAllTransmitMessages");

    // No reallocation for pubsub 
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reallocateAllTransmitMessages");  
  }

  public String getTargetDestination() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTargetDestination");
    String target = ((LinkRemoteTopicSpaceControl)parent).getTargetDestination();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetDestination", target);
    return target;
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
