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
package com.ibm.ws.sib.processor.impl.store.filters;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;

/**
 * @author tevans
 * 
 * This filter returns true whenever the me uuid given matches that of
 * the cellule in a StreamSet.
 */
public final class SourceStreamSetFilter implements Filter
{
  private static TraceComponent tc =
    SibTr.register(
      SourceStreamSetFilter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * The class to use in filter comparisons.
   */
  private SIBUuid8 comparisonMEUuid;  
  
  public SourceStreamSetFilter(SIBUuid8 me)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "SourceStreamSetFilter", me);    
    
    // For PubSub this will be null as there is only one
    // streamSet stored on the stream
    this.comparisonMEUuid = me;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SourceStreamSetFilter", this);    
  }

  /**
   * @see com.ibm.ws.sib.msgstore.Filter
   */
  public boolean filterMatches(AbstractItem item)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean matchResult = false;
    
    if (item.getClass().equals(StreamSet.class))
    {
      StreamSet streamSet = (StreamSet) item;
      SIBUuid8 me = streamSet.getRemoteMEUuid();
      if(comparisonMEUuid == null ) 
        matchResult = true;
      else if(me.equals(comparisonMEUuid)) 
        matchResult = true;
    }
       
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", new Boolean(matchResult));

    return matchResult;
  }
}
