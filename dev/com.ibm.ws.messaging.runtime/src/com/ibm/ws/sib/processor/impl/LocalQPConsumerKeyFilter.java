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
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LocalQPConsumerKeyFilter implements Filter
{
  private static final TraceComponent tc =
    SibTr.register(
        LocalQPConsumerKeyFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** Back reference to parent consumerKey */
  private final Filter parentFilter;

  private int classIndex = 0;
  private String classificationName = null;
  
  private LockingCursor getCursor;
  
  public LocalQPConsumerKeyFilter(Filter parentFilter,
                             int classIndex,
                             String classificationName)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "LocalQPConsumerKeyFilter", 
        new Object[]{parentFilter, Integer.valueOf(classIndex), classificationName});
    
    this.parentFilter = parentFilter;
    this.classIndex = classIndex;
    this.classificationName = classificationName;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LocalQPConsumerKeyFilter", this);    
  }
  
  /**
   * Called by MS when scanning an ItemStream for matching items
   * @throws MessageStoreException 
   */
  public boolean filterMatches(AbstractItem item) throws MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean result = true;
    
    // If messages are classified by XD, then check the classification up front.
    // If the index is 0, then there is no classification
    if(classIndex>0)
    {
      // If we're classifying then the classification property must match this
      // filter's classification
      result = false;
      
      //Do not throw exception if Message not available in the store
      boolean throwExceptionIfMessageNotAvailable = false;
      
      // Need to get the classification out of the message
      String keyClassification = ((SIMPMessage)item).getMessageControlClassification(throwExceptionIfMessageNotAvailable);              

      if(keyClassification != null && keyClassification.equalsIgnoreCase(classificationName))
        result = true;
      else
      {
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "filter class: " + classificationName + ", message class: " + keyClassification);
      }
    }
    
    // If we still have a potential match, then drive any selector
    // processing through the parent class.
    if(result)
      result = parentFilter.filterMatches(item);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", Boolean.valueOf(result));
    
    return result;
  }

  public void setLockingCursor(LockingCursor cursor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setLockingCursor", cursor);

    getCursor = cursor;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setLockingCursor");

  }  

  protected LockingCursor getGetCursor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursor");
    
    LockingCursor cursor = getCursor;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursor", cursor);

    return cursor;
  }    
  
  /**
   * Detach processing for this filter
   */
  protected void detach()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detach");

    // Cleanly dispose of the getCursor
    getCursor.finished();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detach");
  }  

  /**
   * Discard processing for this filter
   */
  protected void discard()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "discard");

    // Discard any old cursor
    if(getCursor != null)
    {
      getCursor.finished();
      getCursor = null;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "discard");
  }    
  
}
