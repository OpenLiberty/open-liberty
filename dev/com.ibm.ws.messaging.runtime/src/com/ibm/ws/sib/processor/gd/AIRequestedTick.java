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
package com.ibm.ws.sib.processor.gd;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.utils.am.AbstractBatchedTimeoutEntry;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

/**
 *
 */
public class AIRequestedTick extends AbstractBatchedTimeoutEntry
{
 
  private static final TraceComponent tc =
    SibTr.register(
      AIRequestedTick.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  private long tick;
  private SelectionCriteria[] criterias;
  private RemoteDispatchableKey ck;
  private long originalTimeout;
  private long timeout;
  private boolean slowed;
  private long ackingDMEVersion;
  private long issueTime;
  private AOValue restoringAOValue;

  public AIRequestedTick(long tick, SelectionCriteria[] criterias, RemoteDispatchableKey ck, long timeout, boolean slowed, long issueTime)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIRequestedTick", new Object[] {Long.valueOf(tick),
                                                       criterias,
                                                       ck,
                                                       Long.valueOf(timeout),
                                                       Boolean.valueOf(slowed),
                                                       Long.valueOf(issueTime)});
    
    this.tick = tick;
    this.criterias = criterias;
    this.ck = ck;
    this.originalTimeout = timeout;
    this.timeout = timeout;
    this.slowed = slowed;
    this.ackingDMEVersion = -1;
    this.issueTime = issueTime;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIRequestedTick", this);
  }
  
  /**
   * SIB0113 - Constructor used for restoration requests. These are requests which are for retrieving
   * a particular message after a restart. The AIMessageItem has gone (because it is not persisted)
   * but we need it back if we are an IME doing remote gather. I.e. An AOValue has been restored
   * and needs to reference the msg we used to have.
   */
  public AIRequestedTick(long tick, AOValue value, long timeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIRequestedTick", new Object[] {Long.valueOf(tick),
                                                       value,
                                                       Long.valueOf(timeout)});
    this.tick = tick;
    this.originalTimeout = timeout;
    this.timeout = timeout;
    this.slowed = false;
    this.ackingDMEVersion = -1;
    this.issueTime = System.currentTimeMillis();
    this.restoringAOValue = value;  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIRequestedTick", this);
  }

  ////////////////////////////////////////////////////////////////////////
  // Aditional methods
  ////////////////////////////////////////////////////////////////////////

  public long getTick()
  {
    return tick;
  }

  public SelectionCriteria[] getCriterias()
  {
    return criterias;
  }

  public RemoteDispatchableKey getRemoteDispatchableKey()
  {
    return ck;
  }

  public long getOriginalTimeout()
  {
    return originalTimeout;
  }

  public long getTimeout()
  {
    return timeout;
  }

  public void resetTimeout(long timeout)
  {
    this.timeout = timeout;
  }

  public boolean isSlowed()
  {
    return slowed;
  }

  public void setSlowed(boolean slowed)
  {
    this.slowed = slowed;
  }

  public long getAckingDMEVersion()
  {
    return ackingDMEVersion;
  }

  public void setAckingDMEVersion(long v)
  {
    ackingDMEVersion = v;
  }

  public long getIssueTime()
  {
    return issueTime;
  }

  public AOValue getRestoringAOValue() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRestoringAOValue"); 
      SibTr.exit(tc, "getRestoringAOValue", restoringAOValue);
    }
    return restoringAOValue;
  }
  
  @Override
  public String toString() 
  {
    return "[Tick : "+tick+", Timeout : "+timeout+", OriginalTimeout : "+originalTimeout+"]";
  }
}
