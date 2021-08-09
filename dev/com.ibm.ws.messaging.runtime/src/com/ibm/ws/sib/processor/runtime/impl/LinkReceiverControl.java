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
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLinkReceiverControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkReceiverControl extends TargetStreamSetControl implements
    SIMPLinkReceiverControllable {

  private static TraceComponent tc =
    SibTr.register(
      LinkReceiverControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private String linkTarget;
  
  public LinkReceiverControl (StreamSet streamSet, String linkTarget) {
    super(streamSet);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkReceiverControl", new Object[] { streamSet,
          linkTarget });
    this.linkTarget = linkTarget;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkReceiverControl", this);
  }

  public long getTimeSinceLastMessageReceived() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTimeSinceLastMessageReceived");
      
    long maxTime=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      TargetStreamControl targetStreamControl = 
        (TargetStreamControl)iterator.next();
      long streamTime = targetStreamControl.getLastMsgReceivedTime();
      if (streamTime > maxTime) maxTime = streamTime;
    }
    
    if (maxTime == 0)
      maxTime = -1; // No previous message
    else
      maxTime = System.currentTimeMillis() - maxTime;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTimeSinceLastMessageReceived", maxTime);
    
    return maxTime;
  }

  public String getLinkUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkUuid");
    
    SIBUuid12 uuid = 
      tsm.getDestinationHandler().getUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkUuid", uuid);
    
    return uuid.toString();
  }

  public String getLinkName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkName");
    
    String name = 
      tsm.getDestinationHandler().getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkName", name);
    
    return name;
  }

  public String getSourceBusName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSourceBusName");
    
    String name = null;
    DestinationHandler dh = 
      tsm.getDestinationHandler();
    if(dh.isLink())
      name = ((LinkHandler)dh).getBusName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSourceBusName", name);
    
    return name;
  }

  public String getSourceEngineUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSourceEngineUuid");
    
    String uuid = getRemoteEngineUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSourceEngineUuid", uuid);
    
    return uuid;
  }

  public String getTargetDestinationName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTargetDestinationName");
    
    String target = linkTarget;
    if (!isPublicationReceiver())
      target = SIMPConstants.UNKNOWN_TARGET_DESTINATION;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetDestinationName", target);
    
    return target;
  }

  public boolean isPublicationReceiver() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isPublicationReceiver");
    
    boolean isPubSub = true;
    if (linkTarget!=null && linkTarget.equals(SIMPConstants.PTOP_TARGET_STREAM))
        isPubSub = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isPublicationReceiver", isPubSub);
    return isPubSub;
  }
  
  

}
