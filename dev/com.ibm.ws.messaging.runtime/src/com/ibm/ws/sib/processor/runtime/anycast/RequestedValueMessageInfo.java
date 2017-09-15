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
package com.ibm.ws.sib.processor.runtime.anycast;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.AIValueTick;
import com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This object represents a Value tick
 */
public class RequestedValueMessageInfo implements SIMPRequestedValueMessageInfo
{
  private static TraceComponent tc =
    SibTr.register(
  RequestedValueMessageInfo.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  // The value tick 
  private AIValueTick aiValueTick;
  
  public RequestedValueMessageInfo(AIValueTick aiValueTick)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "RequestedValueMessageInfo", new Object[]{aiValueTick});
      
    this.aiValueTick = aiValueTick;
      
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "RequestedValueMessageInfo", this);
  }
    

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo#getIssueTime()
   */
  public long getIssueTime()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getIssueTime");
      
    long issueTime = aiValueTick.getIssueTime();
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getIssueTime", new Long(issueTime));
    return issueTime;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo#getMessagePrority()
   */
  public int getMessagePriority()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMessagePriority");
      
    int priority = aiValueTick.getMsgPriority();
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getMessagePriority", new Integer(priority));
    return priority;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo#getMessageReliability()
   */
  public Reliability getMessageReliability()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMessageReliability");
      
    Reliability reliability = aiValueTick.getMsgReliability();
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getMessageReliability", reliability);
    return reliability;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo#getTimeout()
   */
  public long getTimeout()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTimeout");
      
    long timeout = aiValueTick.getOriginalTimeout();
        
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTimeout", new Long(timeout));
    return timeout;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRequestedValueMessageInfo#isDelivered()
   */
  public boolean isDelivered()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "isDelivered");
      
    boolean delivered = aiValueTick.isDelivered();
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "isDelivered", Boolean.valueOf(delivered));
    return delivered;
  }

}
