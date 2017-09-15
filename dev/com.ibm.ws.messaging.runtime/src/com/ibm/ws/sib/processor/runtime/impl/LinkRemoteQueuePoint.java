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

import java.io.PrintStream;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.gd.SourceStreamManager;
import com.ibm.ws.sib.processor.impl.PtoPOutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPLinkRemoteMessagePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLinkTransmitterControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkRemoteQueuePoint extends AbstractRegisteredControlAdapter implements SIMPLinkRemoteMessagePointControllable 
{
  private DestinationHandler destinationHandler;
  private PtoPXmitMsgsItemStream itemStream;
  private String id;
  private VirtualLinkControl linkControl;
    
  private static TraceComponent tc =
    SibTr.register(
      LinkRemoteQueuePoint.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  public LinkRemoteQueuePoint(DestinationHandler dh, PtoPXmitMsgsItemStream itemstream) {
    super(dh.getMessageProcessor(), ControllableType.LINK_TRANSMITTER);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkRemoteQueuePoint", new Object[] {dh, itemstream});
    
    this.destinationHandler = dh;
    this.itemStream = itemstream;
    this.linkControl = (VirtualLinkControl)destinationHandler.getControlAdapter();
    
    linkControl.addRemoteMessagePointControl(this);
    
    id = destinationHandler.getUuid().toString()+
    RuntimeControlConstants.LINK_TRANSMITTER_ID_INSERT;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkRemoteQueuePoint", this);
  }

  public SIMPLinkTransmitterControllable getOutboundTransmit() {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getOutboundTransmit");
    
    SourceStreamManager ssm = ((PtoPOutputHandler) itemStream.getOutputHandler()).getSourceStreamManager();
    SIMPLinkTransmitterControllable linkTransmit = 
      (SIMPLinkTransmitterControllable)ssm.getStreamSetRuntimeControl();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getOutboundTransmit", linkTransmit);
    
    return linkTransmit;
  }

  public String getName() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");

    String name = destinationHandler.getControlAdapter().getName();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName", name);
    return name;
  }

  public String getId() {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", id);
    }
    return id;
  }
  
  public boolean isPublicationTransmitter() {
    return false;
  }

  public void assertValidControllable() throws SIMPControllableNotFoundException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");

    if(itemStream != null)
    {
      ((ControlAdapter)getOutboundTransmit()).assertValidControllable(); 
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
  }

  public void formatState(PrintStream print, boolean descend) {
    // TODO Auto-generated method stub
    
  }
  
  public void dereferenceControllable() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "dereferenceControllable");
    super.dereferenceControllable();
    linkControl.removeRemoteMessagePointControl(this);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");
  }
  
  public String getTargetDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTargetDestination");

    String name = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTargetDestination", name);
    return name;
  }
}
