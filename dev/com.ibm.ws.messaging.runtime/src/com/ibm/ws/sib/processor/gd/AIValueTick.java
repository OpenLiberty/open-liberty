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
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.store.items.AIMessageItem;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *
 */
public class AIValueTick
{
  
  private static final TraceComponent tc =
    SibTr.register(
      AIValueTick.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
    
  
  private long tick;
  private Reliability msgReliability;
  private int msgPriority;
  private AIMessageItem msgItem; // null if delivered=true
  private boolean delivered;
  private RemoteDispatchableKey ck; // null if delivered=true
  private long originalTimeout;
  private long issueTime;
  private long unlockCount;

  public AIValueTick(long tick, AIMessageItem msgItem, boolean delivered, RemoteDispatchableKey ck, 
                     long originalTimeout, long issueTime, int redeliveredCount)
  {
    this.tick = tick;
    JsMessage jsMsg = msgItem.getMessage();
    this.msgReliability = jsMsg.getReliability();
    this.msgPriority = jsMsg.getPriority().intValue();
    this.msgItem = (delivered ? null : msgItem);
    this.delivered = delivered;
    this.ck = (delivered ? null : ck);
    this.originalTimeout = originalTimeout;
    this.issueTime = issueTime;
  }
  
  public AIValueTick(long tick, Reliability rel)
  {
    this.tick = tick;
    this.msgReliability = rel;
    this.msgPriority = 0;
    this.msgItem = null;
    this.delivered = true;
    this.ck = null;
    this.originalTimeout = 0;
    this.issueTime = 0;
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
  
  public Reliability getMsgReliability()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMsgReliability");
      SibTr.exit(tc, "getMsgReliability", msgReliability);
    }
    return msgReliability;
  }
  
  public int getMsgPriority()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMsgPriority");
      SibTr.exit(tc, "getMsgPriority", msgPriority);
    }
    return msgPriority;
  }
  
  public AIMessageItem getMsg()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMsg");
      SibTr.exit(tc, "getMsg", msgItem);
    }
    return msgItem;
  }

  public boolean isDelivered()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isDelivered");
      SibTr.exit(tc, "isDelivered", Boolean.valueOf(delivered));
    }
    return delivered;
  }

  public void setDelivered(boolean delivered)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isDelivered", Boolean.valueOf(delivered));
      SibTr.exit(tc, "isDelivered");
    }
    this.delivered = delivered;
    this.msgItem = null;
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
  
  public void incRMEUnlockCount() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "incRMEUnlockCount");
    unlockCount++;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "incRMEUnlockCount",Long.valueOf(unlockCount));
  }
  
  public long getRMEUnlockCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRMEUnlockCount");
      SibTr.exit(tc, "getRMEUnlockCount", Long.valueOf(unlockCount));
    }
    return unlockCount;
  }
}
