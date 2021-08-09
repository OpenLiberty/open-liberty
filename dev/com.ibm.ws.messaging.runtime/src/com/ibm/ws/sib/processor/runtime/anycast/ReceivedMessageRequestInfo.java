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
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageRequestInfo;
import com.ibm.ws.sib.processor.runtime.impl.AbstractControlAdapter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;

/**
 * This class encapsulates information about a message request that has 
 * been received from a remote messaging engine
 * @author tpm100
 */
public class ReceivedMessageRequestInfo extends AbstractControlAdapter 
  implements SIMPReceivedMessageRequestInfo
{
  
  private static final TraceComponent tc =
    SibTr.register(
      ReceivedMessageRequestInfo.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  private long _issueTime;
  private long _timeout;
  private SelectionCriteria[] _selectionCriterias;
  private long _ackingDME; 
  private long _tick;     
  
  public ReceivedMessageRequestInfo(
    long issueTime,
    long timeout,
    SelectionCriteria[] selectionCriterias,
    long ackingDME,
    long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ReceivedMessageRequestInfo", 
      new Object[]{ new Long(issueTime), new Long(timeout), selectionCriterias, 
      new Long(ackingDME), new Long(tick)});    
    
    _issueTime = issueTime;
    _timeout = timeout;
    _selectionCriterias = selectionCriterias;
    _ackingDME = ackingDME;  
    _tick = tick;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ReceivedMessageRequestInfo", this);
  }
  
  
  public long getIssueTime()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getIssueTime");
      SibTr.exit(tc, "getIssueTime", new Long(_issueTime));
    }
    return _issueTime;       
  }
  
  public long getTimeout()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTimeout");
      SibTr.exit(tc, "getTimeout", new Long(_timeout));
    }
    return _timeout;        
  }
  
  public SelectionCriteria[] getCriterias()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getCriterias");
      SibTr.exit(tc, "getCriterias", _selectionCriterias);
    }
    return _selectionCriterias;
  }
  
  public long getACKingDME()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getACKingDME");
      SibTr.exit(tc, "getACKingDME", new Long(this._ackingDME));
    }
    return _ackingDME;    
  }
  
  /**
   * Return the completion time for this request.
   * -1 means infinite
   */
  public long getCompletionTime()
  {
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getCompletionTime");
    
    long completionTime =-1;
    //only calculate if the timeout is not infinite
    long timeOut = getTimeout();
    if(timeOut != SIMPConstants.INFINITE_TIMEOUT) 
    {
      completionTime = getIssueTime() + timeOut;  
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())      
      SibTr.exit(tc, "getCompletionTime", new Long(completionTime));
    
    return completionTime;    
  }

  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");
    String id = this.getId();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName", id);
    return id;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
  }  
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "dereferenceControllable");
      
    _issueTime = 0;
    _timeout = 0;
    _selectionCriterias = null;
    _ackingDME = 0;  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "dereferenceControllable");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getId");
    String id = new Long(_tick).toString();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getId", id);
    return id;
  }  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() 
  {
    //this is a NO-OP
  }  

}
