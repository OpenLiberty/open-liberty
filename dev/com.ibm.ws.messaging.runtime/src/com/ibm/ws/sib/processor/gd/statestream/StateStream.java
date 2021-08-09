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
package com.ibm.ws.sib.processor.gd.statestream;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implements a stream to maintain state information for each tick
 *
 */

public class StateStream extends LinkedRangeList
{
  private static final TraceComponent tc =
    SibTr.register(
      StateStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Initializes a stream with completed tick at 0 and
   * all remaining ticks in Unknown state.
   */
  public StateStream()
  {
    super();
  }
  
  public boolean writeValueRange(long tick, TickData data)
  {
    return writeRange(TickRangeType.VALUE,tick,tick,data,true);                                   
  }
  
  public boolean writeRange(TickRangeType type, long start, long end, TickData data, boolean errorCheck)
  {
    TickRange tr = getNewTickRange(type,
                                    start,
                                    end,
                                    data);
    return setRange(tr, errorCheck);                                   
  }
  
  public boolean containsState(TickRangeType state)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "containsState", new Object[] { state });

    // return value
    boolean containsState = containsState(state, TickRange.MIN, TickRange.MAX);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "containsState", new Boolean(containsState));

    return containsState;
  }

  public TickRangeType getState(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState", new Long(tick));

    TickRange curr = findRange(tick);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", curr.type);

    return curr.type;
  }

  /**
    * This method finds the lowest tick in Completed state
    * that is contiguous with the tick at stamp.
    * It will always return a value <= stamp.
    */
  public long discoverPrevCompleted(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "discoverPrevCompleted",
        new Object[] { new Long(stamp) });

    long prevCompleted = stamp;
    TickRange tr = findRange(stamp);
    if(tr.type == TickRangeType.COMPLETED) prevCompleted = tr.start;
    
    TickRange prev = (TickRange) tr.getPrevious();
    while(prev != null && prev.type == TickRangeType.COMPLETED)
    {
      prevCompleted = prev.start;
      prev = (TickRange) prev.getPrevious();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "discoverPrevCompleted", new Long(prevCompleted));
    
    return prevCompleted;
  }
  
  //--------------------------------------------------------------------------
  // cursor read methods
  
  public TickRange getCurrent()
  {
    return getCursor();
  }

  public TickRange getRange(long tick)
  {
    TickRange range = null;
    if(tick == TickRange.MIN)
    {
      range = moveCursorToStart();
    }
    else if(tick == TickRange.MAX)
    {
      range = moveCursorToEnd();
    }
    else
    {
      range = findRange(tick);
    } 
//    if(range != null) range = cloneTickRange(range);
//    else range = null;
    return range;
  }

  public TickRange getNext()
  {
    TickRange next = next();
//    if(next != null) next = cloneTickRange(next);
//    else next = null;
    return next;
  }

  public TickRange getPrev()
  {
    TickRange prev = previous();
//    if(prev != null) prev = cloneTickRange(prev);
//    else prev = null;
    return prev;
  }
  //---------------------------------------------------------------------------

  public long getCompletedPrefix()
  {
    return 0; //getCompletedPrefix();
  }

  // Advance the Completed prefix.
  public boolean setCompletedPrefix(long newprefix)
  {
    if (newprefix > getCompletedPrefix())
    {
      return writeRange(TickRangeType.COMPLETED,0,newprefix,null,true);
    }
    return false;
  }

  /**
   * Create a formatted string describing the stream state.
   *
   * @param str    Description of the stream.
   *
   * @return The string describing the state of the ticks in the stream.
   */
  public String stateString(String str)
  {
    TickRange ro;
    String ret;
    ret =
      "Elements in stream " + str + ": Completedpre=" + getCompletedPrefix() + ":\n";
    // Loop over all of the ticks in the stream starting with the oldest.
    ro = getRange(0);
    while(ro != null)
    {
      ret += ro.toString() + "\n";
      ro = getNext();
    }
    return ret;
  }
}
