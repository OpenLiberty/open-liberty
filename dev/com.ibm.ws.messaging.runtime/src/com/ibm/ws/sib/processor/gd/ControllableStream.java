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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class ControllableStream implements Stream
{
  private static final TraceComponent tc =
    SibTr.register(
      ControllableStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  private static final String ID_SEPERATOR = ":";
  /**
   * Gets a list of all tick values on the state stream
   * @param stream
   * @return
   */
  public synchronized List<Long> getTicksOnStream()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTicksOnStream");
      
    List<Long> msgs = new ArrayList<Long>();    
    StateStream stateStream = getStateStream();
    
    // Initial range in stream is always completed Range
    stateStream.setCursor(0);
    
    // skip this and move to next range
    stateStream.getNext();
     
    // Get the first TickRange after completed range and move cursor to the next one
    TickRange tr = stateStream.getNext();

    // Iterate until we reach final Unknown range
    while (tr.endstamp < RangeList.INFINITY)
    {
      if( !(tr.type == TickRange.Completed) )
      {
        msgs.add(new Long(tr.startstamp));
      }
      tr = stateStream.getNext();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTicksOnStream", msgs);
    return msgs;
  }
  
  public synchronized TickRange getTickRange(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTickRange");
      
    TickRange range = null;    
    StateStream stateStream = getStateStream();
    stateStream.setCursor(tick);

    // Get the TickRange
    range = (TickRange) stateStream.getNext().clone();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTickRange", range);
    return range;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getCompletedPrefix()
   */
  public abstract long getCompletedPrefix();

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getStateStream()
   */
  public abstract StateStream getStateStream();

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#writeSilenceForced(long)
   */
  public abstract void writeSilenceForced(long tick) throws SIException;

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.gd.Stream#getID()
   */
  public String getID()
  {
    String id = "" + getReliability() + ID_SEPERATOR + getPriority() + ID_SEPERATOR;    
    return id;
  }
  
  protected abstract Reliability getReliability(); 
  
  protected abstract int getPriority(); 
}
