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
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.impl.LinkHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitterControllable;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkTransmitterControl extends SourceStreamSetControl implements
    SIMPLinkTransmitterControllable {

  private static TraceComponent tc =
    SibTr.register(
      LinkTransmitterControl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  public LinkTransmitterControl(SIBUuid8 remoteMEUuid, StreamSet streamSet, boolean local) {
    super(remoteMEUuid, streamSet);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkTransmitterControl", new Object[] { remoteMEUuid,
              streamSet, Boolean.valueOf(local) });
      
    // Set the initial health state to be RED until the link is started (if the link is local
    // to this ME, otherwise we have to assume it's GREEN as we won't be told the actual state
    // of the link itself)
    if(local)
      ((HealthStateListener)_healthState).updateHealth(HealthStateListener.CONNECTION_UNAVAILABLE_STATE, HealthState.RED);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkTransmitterControl", this);
  }

  public long getTimeSinceLastMessageSent() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTimeSinceLastMessageSent");
      
    long maxTime=0;
    SIMPIterator iterator = getStreams();
    while(iterator.hasNext())
    {
      SourceStreamControl srcStreamControl = 
        (SourceStreamControl)iterator.next();
      long streamTime = srcStreamControl.getLastMsgSentTime();
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
    
    boolean isPI = 
      !(_sourceStreamManager.getDestinationHandler().isSendAllowed());
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isPutInhibited", new Boolean(isPI));
    
    return isPI;
  }

  public String getLinkType() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkType");
    
    String type =
      ((LinkHandler)_sourceStreamManager.getDestinationHandler()).getType();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkType", type);
    
    return type;
  }

  public String getLinkUuid() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkUuid");
    
    SIBUuid12 uuid = 
      _sourceStreamManager.getDestinationHandler().getUuid();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkUuid", uuid);
    
    return uuid.toString();
  }

  public String getLinkName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getLinkName");
    
    String name = 
      _sourceStreamManager.getDestinationHandler().getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getLinkName", name);
    
    return name;
  }

  public String getTargetBusName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTargetBusName");
    
    String name = null;
    DestinationHandler dh = 
      _sourceStreamManager.getDestinationHandler();
    if(dh.isLink())
      name = ((LinkHandler)dh).getBusName();
    
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
  
  public String getTargetDestination() 
  {
    // We dont know the target dest for ptop links
    return null;
  }

  public boolean isPublicationTransmitter() {
    return false;
  }

}
