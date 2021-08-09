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
import com.ibm.ws.sib.processor.impl.AOBrowserSession;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteBrowserReceiverControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This object represnts a remote browse of this local queue. 
 */
public class RemoteBrowserReceiver implements SIMPRemoteBrowserReceiverControllable
{
  private static TraceComponent tc =
    SibTr.register(
      RemoteBrowserReceiver.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  // The browserSession that this object represents.    
  private AOBrowserSession aoBrowserSession;
  
  public RemoteBrowserReceiver(AOBrowserSession aoBrowserSession)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "RemoteBrowserReceiver", aoBrowserSession);
      
    this.aoBrowserSession = aoBrowserSession;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "RemoteBrowserReceiver", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteBrowserReceiverControllable#getBrowseID()
   */
  public long getBrowseID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getBrowseID");
      
    long browseID = aoBrowserSession.getKey().getBrowseId();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getBrowseID", new Long(browseID));
    return browseID;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteBrowserReceiverControllable#getExpectedSequenceNumber()
   */
  public long getExpectedSequenceNumber()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getExpectedSequenceNumber");
      
    long expectedSeqNumber = aoBrowserSession.getExpectedSequenceNumber();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getExpectedSequenceNumber", new Long(expectedSeqNumber));
    return expectedSeqNumber;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getId");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getId", aoBrowserSession.toString());
    return aoBrowserSession.toString();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getConfigId()
   */
  public String getConfigId()
  {    
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getRemoteEngineUuid()
   */
  public String getRemoteEngineUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteEngineUuid");
      
    String remoteMEId = aoBrowserSession.getKey().getRemoteMEUuid().toString();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteEngineUuid", remoteMEId);
    return remoteMEId;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.admin.Controllable#getUuid()
   */
  public String getUuid()
  {
    return null;
  }

}
