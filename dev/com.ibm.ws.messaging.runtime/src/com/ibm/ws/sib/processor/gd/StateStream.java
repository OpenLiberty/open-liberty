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

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.utils.BlockVector;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implements a stream to maintain state information for each tick
 *
 */

public final class StateStream
{

  private static TraceComponent tc =
    SibTr.register(
      StateStream.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static byte Unknown = TickRange.Unknown;
  private static byte Requested = TickRange.Requested;
  private static byte Uncommitted = TickRange.Uncommitted;
  private static byte Value = TickRange.Value;
  private static byte Discarded = TickRange.Discarded;
  private static byte Accepted = TickRange.Accepted;
  private static byte Rejected = TickRange.Rejected;
  private static byte Completed = TickRange.Completed;
  private static byte Error = TickRange.Error;

  private static byte nextState[][] = {
  // Existing state:-
  // Unknown      Requested    Uncommitted Value      Discarded  Accepted   Rejected   Completed  Error
   { Unknown,     Unknown,     Error,      Error,     Error,     Error,     Error,     Error,     Error },
   { Requested,   Requested,   Error,      Error,     Error,     Error,     Error,     Error,     Error },
   { Uncommitted, Uncommitted, Error,      Error,     Error,     Error,     Error,     Error,     Error },
   { Value,       Value,       Value,      Value,     Error,     Error,     Error,     Completed, Error },
   { Discarded,   Discarded,   Discarded,  Error,     Error,     Error,     Error,     Error,     Error },
   { Accepted,    Accepted,    Error,      Accepted,  Error,     Error,     Error,     Error,     Error },
   { Rejected,    Rejected,    Error,      Rejected,  Error,     Error,     Error,     Error,     Error },
   { Completed,   Completed,   Completed,  Completed, Completed, Completed, Completed, Completed, Error },
   { Error,       Error,       Error,      Error,     Error,     Error,     Error,     Error,     Error }

   };

  // Completed prefix used to indicate start of stream
  private long completedPrefix;

  // List of Range objects representing stream
  private ARangeList list;

  // reused marker
  private Object mark = null;

  /**
   * Initializes a stream with completed tick at 0 and
   * all remaining ticks in Unknown state.
   */
  public void init()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "init");

    TickRange tr = new TickRange(TickRange.Unknown, 0, RangeList.INFINITY);
    list = new ARangeList();
    list.init(tr);
    
    // Put a Completed tick at 0 in the stream
    tr = new TickRange(TickRange.Completed, 0, 0);
    list.setCursor(0);
    list.replacePrefix(tr);
         
    completedPrefix = 0;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "init");
  }

  /**
   *  The following are all the write operations needed for this stream
   */

  /**
    * Writes ticks for the given range into the stream
    * New state is determined from current state using nextState table
    * This method does not do any accumulation of ranges
    *
    * @param   r       The TickRange to write to the stream
    * @return  boolean  true if TickRange causes the stream to change

    */
  public boolean writeRange(TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeRange", new Object[] {r, Long.valueOf(completedPrefix)});

    boolean streamChanged = false;
    byte newState = r.type;

    // Walk the list, changing stuff as we go along
    list.setCursor(r.startstamp);
    TickRange tr1, tr2;

    // Retrieve the tick range which contains the startstamp of our range
    tr2 = (TickRange)list.getCurr();
    do
    {
      tr1 = tr2;

      newState = changeState(r.type, tr1.type);

      if (newState != tr1.type)
      {
        // candidate for replacement. tr1 represents the range [a, b]
        if (tr1.startstamp < r.startstamp)
        {
          // split the range [a, b] to [a, r.startstamp-1] [r.startstamp, b]
          list.splitStart(r.startstamp);

          // update tr1 and tr2 to get the [r.startstamp, b] range
          tr1 = tr2 = (TickRange)list.getCurr();
        }
        // tr1 again represents the range [a, b]
        if (tr1.endstamp > r.endstamp)
        {
          // split the range [a, b] to [a, r.endstamp] [r.endstamp+1, b]
          list.splitEnd(r.endstamp);

          // update tr1 and tr2 to get the [a, r.endstamp] range
          tr1 = tr2 = (TickRange)list.getCurr();
        }
        // now mutate the type of tr1 to our new range
        tr1.type = newState;
        tr1.value = r.value;
        tr1.valuestamp = r.valuestamp;
        tr1.itemStreamIndex = r.itemStreamIndex;

        streamChanged = true;
      }

      if (tr1.type == TickRange.Completed)
      {
        // also check if we should advance the Completed prefix
        if (tr1.startstamp <= (completedPrefix + 1))
          setCompletedPrefix(tr1.endstamp);
      }

      list.getNext();
      tr2 = (TickRange)list.getCurr();

    }
    while ((tr2.startstamp <= r.endstamp) && (tr1 != tr2));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeRange", new Boolean(streamChanged));

    return streamChanged;
  }

  /**
    * Determines whether the specified TickRange contains any ticks in
    * Requested state
    *
    * @param   r  The TickRange to check
    * @return  boolean  true if TickRange contains ticks in Requested state
    *
    */
  public boolean containsRequested(TickRange r)
  {
    // TODO: replace calls to this method with calls to containsState below.
    return containsState(r, TickRange.Requested);
  }

  /**
    * Determines whether the specified TickRange contains any ticks in
    * the specified state.
    *
    * @param   r  The TickRange to check
    * @return  boolean  true if TickRange contains ticks in state r
    *
    */
  public boolean containsState(TickRange r, byte state)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "containsState", new Object[] { r, new Byte(state) });

    // return value
    boolean containsState = false;

    // set the cursor to startstamp of range we want to check
    list.setCursor(r.startstamp);

    // and get the TickRange containing our startstamp
    TickRange tr2 = (TickRange)list.getNext();
    TickRange tr1;
    do
    {
      // return as soon as we find any TickRange in specified state
      if (tr2.type == state)
      {
        containsState = true;
        break;
      }
      tr1 = tr2;
      tr2 = (TickRange)list.getNext();
    }
    while ((tr1 != tr2) && (tr2.endstamp < r.endstamp));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "containsState", new Boolean(containsState));

    return containsState;
  }

  /**
    * Determines whether the specified tick is in Requested state
    *
    * @param   stamp    The tick to check
    * @return  boolean  true if tick is in Requested state
    *
    */
  public boolean isRequested(long stamp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "isRequested", new Long(stamp));

    // return value
    boolean isRequested = false;

    // set the cursor to startstamp of range we want to check
    list.setCursor(stamp);

    // and get the TickRange containing our stamp
    TickRange tr = (TickRange)list.getNext();

    // Check whether tick is in Requested state
    if (tr.type == TickRange.Requested)
      isRequested = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isRequested", new Boolean(isRequested));

    return isRequested;
  }

  /**
   * The TickRange represents zero or more ticks in Completed state
   * followed by a single tick in either Unknown or Value state
   * followed by zero or or more ticks in Completed state
   * The TickRange.valuestamp gives the position of the single tick and
   * the TickRange.type indicates whether it is in Unknown or Value state.
   * The TickRange.value will be set if the tick is in Value state and null
   * if not.
   * The completedPrefix will be advanced over the first set of ticks
   * in Completed state if possible
   *
   * @param   r        The TickRange to check
   * @return  boolean  true if the stream is changed
   *
   */
  public boolean writeCombinedRange(TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeCombinedRange", new Object[] {r, Long.valueOf(completedPrefix)});

    boolean streamChanged = false;

    // TickRange to use as workarea
    TickRange tr = null;

    // If new range object lies before start of our stream do nothing
    if ((r.endstamp <= completedPrefix) && (r.valuestamp <= completedPrefix))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "writeCombinedRange", new Boolean(streamChanged));
      return streamChanged;
    }

    // Write the first Completed range

    // make sure we don't turn any ticks <= completedPrefix into Completed
    // as there's no point
    long startstamp = max(completedPrefix + 1, r.startstamp);

    // If there are some Completed ticks to write before the
    // Value or Unknown tick
    if (startstamp < r.valuestamp)
    {
      // This will also advance the completedPrefix if possible
      tr = new TickRange(TickRange.Completed, startstamp, r.valuestamp - 1);
      writeCompletedRange(tr);
    }

    // Write the single tick
    tr = new TickRange(r.type, r.valuestamp, r.valuestamp);
    tr.itemStreamIndex = r.itemStreamIndex;
    tr.value = r.value;
    tr.valuestamp = r.valuestamp;

    streamChanged = writeRange(tr);

    // If there are some ticks in Completed state
    // after the single tick then write them into the stream
    if (r.endstamp > r.valuestamp)
    {
      tr = new TickRange(TickRange.Completed, r.valuestamp + 1, r.endstamp);

      writeCompletedRange(tr);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeCombinedRange", new Boolean(streamChanged));
    return streamChanged;
  }

  /**
   * This method returns the maximum range or ticks in Completed state either
   * side of the tick at the valuestamp of the specified TickRange
   *
   * @param   r         The TickRange containing the valuestamp
   *                     to start checking from
   * @return  TickRange The range of ticks in Completed state
   *                     either side of the specified tick.
   */
  public TickRange findCompletedRange(TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "findCompletedRange", r);

    long stamp = r.valuestamp;

    // starts will be set to first contiguous tick in Completed state
    // ends will be set to last contiguous tick in Completed state
    // initialise both to specified tick and work outwards from there.
    long starts = stamp;
    long ends = stamp;

    // set the cursor to range which includes the stamp we are interested in
    // and mark that position as we'll need it later
    list.setCursor(stamp);
    mark = list.getMark(mark);

    // Get range at cursor
    TickRange tr1 = (TickRange)list.getNext();

    // walk ahead and see if adjacent ticks are in Completed state
    // The tr1 range contains the stamp tick, and the
    // list cursor is at next position after tr1
    ends = discoverNextCompleted(ends, tr1);

    // reset cursor to mark as previous method will have moved it
    list.setCursor(mark);

    // walk back to see if adjacent ticks are in Completed state
    tr1 = (TickRange)list.getPrev();
    starts = discoverPrevCompleted(starts, tr1);

    r.startstamp = starts;
    r.endstamp = ends;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findCompletedRange", r);
    return r;
  }
  

  /**
   * This method finds the highest tick in Completed state
   * that is contiguous with the tick at ends.
   * It will always return a value >= ends.
   */
  private long discoverNextCompleted(long ends, TickRange tr)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "discoverNextCompleted",
        new Object[] { new Long(ends), tr });

    TickRange tr1;

    // will start checking from tr onwards
    TickRange tr2 = tr;

    // if stamp we were asked to check from is already at the
    // end of the current range then start checking from next range
    if (tr2.endstamp == ends)
    {
      tr2 = (TickRange)list.getNext();
    }

    // Keep updating ends until we reach a range which is not
    // in Completed state or get to the end of the list
    do
    {
      if (tr2.type == TickRange.Completed)
        ends = tr2.endstamp;
      else
        break;
      tr1 = tr2;
      tr2 = (TickRange)list.getNext();
    }
    while (tr1 != tr2);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "discoverNextCompleted", new Long(ends));

    return ends;
  }

  /**
    * This method finds the lowest tick in Completed state
    * that is contiguous with the tick at starts.
    * It will always return a value <= starts.
    */
  private long discoverPrevCompleted(long starts, TickRange tr)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "discoverPrevCompleted",
        new Object[] { new Long(starts), tr });

    TickRange tr1;

    // will start checking from tr onwards
    TickRange tr2 = tr;

    // if stamp we were asked to check from is already at the
    // start of the current range then start checking from previous range
    if (tr2.startstamp == starts)
    {
      tr2 = (TickRange)list.getPrev();
    }

    // Keep updating starts until we reach a range which is not in
    // Completed state or get to the start of the list
    do
    {
      if (tr2.type == TickRange.Completed)
        starts = tr2.startstamp;
      else
        break;
      tr1 = tr2;
      tr2 = (TickRange)list.getPrev();
    }
    while (tr1 != tr2);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "discoverPrevCompleted", new Long(starts));

    return starts;
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
    list.setCursor(stamp);
    
    // walk back to see if adjacent ticks are in Completed state
    TickRange tr = (TickRange)list.getPrev();
    prevCompleted = discoverPrevCompleted(stamp, tr);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "discoverPrevCompleted", new Long(prevCompleted));
    
    return prevCompleted;
  }
  
  /**
   * Writes a single Completed tick into the stream
   * Combines it with existing Completed ranges on either side
   * Note that startstamp = endstamp as only single tick in range
   * Returns range of Completed ticks either side of new Completed tick
   */
  public TickRange writeCompleted(TickRange r)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeCompleted", new Object[] { r, Long.valueOf(completedPrefix)});

    long starts = r.startstamp;
    long ends = r.startstamp;

    // Do nothing if tick is before start of stream
    if ((r.endstamp <= completedPrefix) && (r.valuestamp <= completedPrefix))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writeCompleted", r);
      return r;
    }

    // set the cursor to range containing our tick
    // Note startstamp=endstamp as single tick
    list.setCursor(r.startstamp);

    // Get range at cursor
    TickRange tr1 = (TickRange)list.getCurr();

    // tr1 is the range containing our tick
    // check here if anything is going to be changed
    byte newState;

    newState = changeState(r.type, tr1.type);

    // Only proceed if we are making a state change
    if (newState != tr1.type)
    {
      // Only attempt to combine with adjacent ranges if new state is Completed
      if (newState == TickRange.Completed)
      {
        TickRange tr2;
        
        // If the tick we are updating is ajacent to the previous range
        // check whether we should combine it
        if ( starts == tr1.startstamp)
        {
          // Combine with previous range if necessary
          // Move the cursor back a range
          list.getPrev();
          tr2 = (TickRange)list.getNext();
          if (tr2.type == TickRange.Completed)
          {
            starts = tr2.startstamp;
          }
        }
        
        // If the tick we are updating is ajacent to the next range
        // check whether we should combine it  
        if ( ends == tr1.endstamp)
        {
          // skip the range containing our tick
          list.getNext();
          // Combine with next range if necessary
          tr2 = (TickRange)list.getCurr();
          if (tr2.type == TickRange.Completed)
          {
            ends = tr2.endstamp;
          }
        }
        
        // Return range of ticks in Completed state to caller
        r.startstamp = starts;
        r.endstamp = ends;
      
        // If no gap between current completedPrefix and
        // new Completed ticks then advance completedPrefix over them
        if (starts <= (completedPrefix + 1))
        {
          // Don't use the setCompletedPrefix() method
          // here as the code below will replace the range
          completedPrefix = ends;
        }
      }

      // New range may not be Completed range
      // Always use newState.
      TickRange tr = new TickRange(newState, starts, ends);
      BlockVector bVector = new BlockVector();
      bVector.add(tr);      
      list.replace(bVector);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeCompleted", r);

    return r;
  }

  /**
   * Writes a range of Completed ticks into the stream
   * Combines it with existing Completed ranges on either side
   * Returns range of Completed ticks either side of new Completed tick
   */
  public TickRange writeCompletedRange(TickRange r) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeCompletedRange", new Object[] { r, Long.valueOf(completedPrefix)});

    // Do nothing if whole tick range is before start of stream
    if ((r.endstamp <= completedPrefix) && (r.valuestamp <= completedPrefix))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writeCompletedRange", r);

      return r;
    }

    // Walk the list, checking that everything in TickRange r
    // will change to Completed state
    boolean stateChangeError = false;
    long starts = r.startstamp;
    long ends = r.endstamp;

    // Set the cursor to the tickRange containing our startstamp
    // Use mark to remember this for later use
    list.setCursor(r.startstamp);
    mark = list.getMark(mark);

    TickRange tr1, tr2;

    // Retrieve the tick range which contains the startstamp of our range
    tr2 = (TickRange)list.getNext();

    // Loop through list until we reach the endstamp of our range
    // or the end of the list
    do
    {
      tr1 = tr2;

      byte changed = TickRange.Completed;

      changed = changeState(r.type, tr1.type);

      if (changed != TickRange.Completed)
      {
        stateChangeError = true;
        break;
      }
      tr2 = (TickRange)list.getNext();
    }
    while ((tr2.startstamp <= r.endstamp) && (tr1 != tr2));

    if (stateChangeError == false)
    {

      // We are at the end.
      // If the range we are modifying is a Completed range
      // we want to combine it with ours
      if ( tr1.type == TickRange.Completed )
      {
        // modify the end
        if ( tr1.endstamp > r.endstamp )
        {
          ends = tr1.endstamp;
        }
      }

      // Check whether there are is an Completed range after this which we
      // need to combine with ours.
      // tr1 is the range containing our endstamp
      // if we have overwritten the whole of tr1 then
      // check whether the next Range in the list is an Completed range
      // and if so combine it with ours
      if (tr1.endstamp == r.endstamp)
      {
        // tr2 is the next range
        if (tr2.type == TickRange.Completed)
          ends = tr2.endstamp;
      }

      // Go back to the start
      // Reset cursor to range containing our startsamp
      list.setCursor(mark);
      tr1 = (TickRange)list.getPrev();
      
      // If the range we are modifying is a Completed range
      // we want to combine it with ours
      if ( tr1.type == TickRange.Completed )
      {
        // Modify the start
        if ( tr1.startstamp < r.startstamp )
        {
          starts = tr1.startstamp;
        }
      }
  
      // Check whether there are is an Completed range before this which we
      // need to combine with ours.
      if (tr1.startstamp == r.startstamp)
      {
        tr2 = (TickRange)list.getPrev();
        if (tr2.type == TickRange.Completed)
          starts = tr2.startstamp;
      }

      // If no gap between current completedPrefix and
      // new S's then advance completedPrefix over them
      if (starts <= (completedPrefix + 1))
      {
        // Don't use the setCompletedPrefix() method
        // here as the code below will replace the range
        completedPrefix = ends;
      }

      TickRange tr = new TickRange(TickRange.Completed, starts, ends);
      BlockVector bVector = new BlockVector();
      bVector.add(tr);      
      list.replace(bVector);

      // Return range of ticks in Completed state to caller
      r.startstamp = starts;
      r.endstamp = ends;
    }
    else
    {
      // We can't write our new range in as a single replacement Completed range
      // because there is a state change error in one of the ranges
      // it updates. We will call the common writeRange() instead as
      // this will update each exsting range in turn.
      writeRange(r);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "writeCompletedRange", r);

    return r;
  }
  /**
   * This method does not check the existing state as it is
   * used to remove messages from the stream and so is 'allowed'
   * to overwrite Values with Silence
   * Writes a range of Completed ticks into the stream
   * Combines it with existing Completed ranges on either side
   * Returns range of Completed ticks either side of new Completed tick
   */
  public TickRange writeCompletedRangeForced(TickRange r) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "writeCompletedRangeForced", new Object[] { r, Long.valueOf(completedPrefix)});

    // Do nothing if whole tick range is before start of stream
    if ((r.endstamp <= completedPrefix) && (r.valuestamp <= completedPrefix))
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "writeCompletedRangeForced", r);
      return r;
    }
      
    long starts = r.startstamp;
    long ends = r.endstamp;

    // Set the cursor to the tickRange containing our startstamp
    list.setCursor(r.startstamp);
    TickRange tr1 = (TickRange)list.getPrev();
      
    // If the range we are modifying is a Completed range
    // we want to combine it with ours
    if ( tr1.type == TickRange.Completed )
    {
      // Modify the start
      if ( tr1.startstamp < r.startstamp )
      {
        starts = tr1.startstamp;
      }
    }
  
    // Check whether there are is an Completed range before this which we
    // need to combine with ours.
    if (tr1.startstamp == r.startstamp)
    {
      tr1 = (TickRange)list.getPrev();
      if (tr1.type == TickRange.Completed)
        starts = tr1.startstamp;
    }

    // Set the cursor to the tickRange containing our endstamp
    list.setCursor(r.endstamp);
    tr1 = (TickRange)list.getNext();
	
    // If the range we are modifying is a Completed range
    // we want to combine it with ours
    if ( tr1.type == TickRange.Completed )
    {
      // modify the end
      if ( tr1.endstamp > r.endstamp )
      {
        ends = tr1.endstamp;
      }	
    }  

    // Check whether there are is an Completed range after this which we
    // need to combine with ours.
    if (tr1.endstamp == r.endstamp)
    {
      tr1 = (TickRange)list.getNext();
      if (tr1.type == TickRange.Completed)
        ends = tr1.endstamp;
    }

    // If no gap between current completedPrefix and
    // new completedRange then advance completedPrefix over them
    if (starts <= (completedPrefix + 1))
    {
      // Don't use the setCompletedPrefix() method
      // here as the code below will replace the range
      completedPrefix = ends;
    }

    TickRange tr = new TickRange(TickRange.Completed, starts, ends);
    BlockVector bVector = new BlockVector();
    bVector.add(tr);      
    list.replace(bVector);
    // Return range of ticks in Completed state to caller
    r.startstamp = starts;
    r.endstamp = ends;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    SibTr.exit(tc, "writeCompletedRangeForced", r);
    return r;
  }


  private byte changeState(byte toState, byte fromState)
  {

    byte newState = nextState[toState][fromState];

    // Check for ErrorState.
    if (newState == Error)
    {
      throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0008",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.StateStream",
              "1:893:1.51",
              new Byte(fromState),
              new Byte(toState)},
            null));
    }
    return newState;
  }

  // Utility method
  private static final long max(long a, long b)
  {
    return a > b ? a : b;
  }

  //--------------------------------------------------------------------------
  // cursor read methods

  public final void lookup(
    long startstamp,
    long endstamp,
    BlockVector readList)
  {
    list.get(startstamp, endstamp, readList);
  }

  public final void setCursor(long stamp)
  {
    list.setCursor(stamp);
  }

  public final TickRange getNext()
  {
    return (TickRange)list.getNext();
  }

  public final TickRange getPrev()
  {
    return (TickRange)list.getPrev();
  }
  //---------------------------------------------------------------------------

  public final long getCompletedPrefix()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(this, tc, "getCompletedPrefix");
      SibTr.exit(tc, "getCompletedPrefix", Long.valueOf(completedPrefix));
    }  
    return completedPrefix;
  }

  // Advance the Completed prefix.
  public final boolean setCompletedPrefix(long newprefix)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "setCompletedPrefix", Long.valueOf(newprefix));
    boolean returnValue = false;  
    if (newprefix > completedPrefix)
    {
      completedPrefix = newprefix;

      TickRange tr,tr2;
      
      if( completedPrefix < RangeList.INFINITY )
      {	   
    	  /*PM51310- Start*/
    	  /*list.setCursor(completedPrefix + 1);
        	tr = (TickRange)list.getCurr();
        	if (tr.type == TickRange.Completed)
        	{
          	completedPrefix = tr.endstamp;
        	}*/
    	  // The earlier code was referring to just the immediate tick after the 
    	  // last one and checking whether it has been completed or not. If the 
    	  // messages have not been delivered in sequence and a message with higher
    	  // tick gets processed it cannot mark itself complete as the ones with 
    	  // lower tick value have not been processed yet.But once the lower ones
    	  // finish there is no way to check the completion of the earlier processed
    	  // message with higher tick value since that tick was never sent across 
    	  // again.So the ME on the other side would keep waiting for that tick to 
    	  // be sent over. With this code change we are checking the completion status 
    	  // of tick and we keep moving the completedPrefix ahead so that it points 
    	  // to the last message that is processed from beginning without any gaps.In 
    	  // other words the completed prefix would be 1 less than the first uncompleted 
    	  // message.   
    	  
    	  list.setCursor(completedPrefix);
    	  tr2 = (TickRange)list.getNext();
    	  do
    	  {
    		  tr = tr2 ;
    		  if (tr.type == TickRange.Completed) {
    			  completedPrefix = tr.endstamp;
    		  } else {
    			  break;
    		  }
    		  tr2 = (TickRange)list.getNext();
    	  } while ((tr != tr2));   	  
      }
      
      // Construct a TickRange from the prefix
      tr = new TickRange(TickRange.Completed, 0, completedPrefix);
      list.setCursor(0);
      list.replacePrefix(tr);
      returnValue = true;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setCompletedPrefix", new Object[] {Boolean.valueOf(returnValue),
                                                         Long.valueOf(completedPrefix)});    
    return returnValue;
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
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	  SibTr.entry(this, tc,"stateString", new Object[]{this, str});
    TickRange ro, ro2;
    String ret;
    ret =
      "Elements in stream " + str + ": Completedpre=" + completedPrefix + ":\n";
    // Loop over all of the ticks in the stream starting with the oldest.
    setCursor(0);
    ro = getNext();
    do
    {
      ret += ro.toString() + "\n";
      ro2 = ro;
    }
    while ((ro = getNext()) != ro2);
    
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		  SibTr.exit(tc,"stateString", ret);
    return ret;
  }

}
