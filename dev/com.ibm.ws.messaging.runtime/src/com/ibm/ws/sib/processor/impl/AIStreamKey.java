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
package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.store.items.AIProtocolItem;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class stores metadata corresponding to a get request that is satisfied with a value tick
 * or a completed tick.
 * When associated with a value tick, the AIMessageItem contains its AIStreamKey. If the AIMessageItem
 * is spilled and then restored, a new AIStreamKey object will be created. When that occurs, some of the
 * fields in this object will not contain meaningful values. But that is acceptable.
 */
public class AIStreamKey
{
  
  // Standard debug/trace
  private static final TraceComponent tc =
    SibTr.register(
      AIStreamKey.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
   
  private final long tick;
  private RemoteDispatchableKey ck;
  private final long originalTimeout;
  private final long issueTime;
  
  
  // TODO this is a hack, to pass the id of the persistent L/A from the pre-prepare flow to the post-commit flow
  // Note that the AIMessageItem which holds a reference to 'this' is in the locked state between the pre-prepare
  // and post-commit flow. Even though the AIMessageItem could be spilled during this time, the in-memory
  // version will be retained, hence remembering the acceptedItem for this period of time is acceptable.
  private AIProtocolItem acceptedItem;

  /**
   * The usual constructor. Initializes all the fields
   * @param timestamp
   * @param ck
   * @param originalTimeout
   * @param issueTime
   */
  public AIStreamKey(long timestamp, RemoteDispatchableKey ck, long originalTimeout, long issueTime)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIStreamKey",
        new Object[]{Long.valueOf(timestamp),
                     ck,
                     Long.valueOf(originalTimeout),
                     Long.valueOf(issueTime)});
    
    this.tick = timestamp;
    this.ck = ck;
    this.originalTimeout = originalTimeout;
    this.issueTime = issueTime;
    this.acceptedItem = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIStreamKey", this);
  }

  /**
   * This constructor is only used in the following cases:
   * (1) by AIProtocolItem at post-commit-add and post-rollback-add, at which point only the
   * timestamp and the reference to the protocol item (which is set separately) are needed
   * (2) when the AIMessageItem is restored.
   * @param timestamp
   */
  public AIStreamKey(long timestamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIStreamKey", Long.valueOf(timestamp));
    
    this.tick = timestamp;
    this.ck = null;
    this.originalTimeout = 0;
    this.issueTime = 0;
    this.acceptedItem = null;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIStreamKey", this);
  }

  public RemoteDispatchableKey getRemoteDispatchableKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteDispatchableKey");
      SibTr.exit(tc, "getRemoteDispatchableKey", ck);
    }    
    return ck;
  }
  public void clearRemoteDispatchableKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "clearRemoteDispatchableKey");
      SibTr.exit(tc, "clearRemoteDispatchableKey");
    }    
    ck = null;
  }

  public long getTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTick");
      SibTr.exit(tc, "getTick", Long.valueOf(tick));
    }    
    return tick;
  }

  public long getOriginalTimeout()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getOriginalTimeout");
      SibTr.exit(tc, "getOriginalTimeout", Long.valueOf(originalTimeout));
    }    
    return originalTimeout;
  }

  public long getIssueTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getIssueTime");
      SibTr.exit(tc, "getIssueTime", Long.valueOf(issueTime));
    }    
    return issueTime;
  }

  public AIProtocolItem getAcceptedItem()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAcceptedItem");
      SibTr.exit(tc, "getAcceptedItem", acceptedItem);
    }    
    return acceptedItem;
  }

  public void setAcceptedItem(AIProtocolItem item)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setAcceptedItem", item);
      SibTr.exit(tc, "setAcceptedItem");
    } 
    this.acceptedItem = item;
  }
}
