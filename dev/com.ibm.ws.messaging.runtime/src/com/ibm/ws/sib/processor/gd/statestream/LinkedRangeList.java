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
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.utils.linkedlist2.LinkedList;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LinkedRangeList extends LinkedList
{
  private TickRangeObjectPool tickRangePool;

  private static TraceComponent tc =
    SibTr.register(
      LinkedRangeList.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private TickRange cursor;
  private boolean error = false;

  public LinkedRangeList()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LinkedRangeList");

    tickRangePool = new TickRangeObjectPool("TickRangePool",20);
    TickRange tr = getNewTickRange(TickRangeType.COMPLETED, 0, 0);
    put(tr);    
    tr = getNewTickRange(TickRangeType.UNKNOWN, 1, TickRange.MAX);
    put(tr);
    
    cursor = tr;    

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LinkedRangeList", this);
  }  
  
  protected TickRange getNewTickRange(TickRangeType type, long start, long end)
  {
    return tickRangePool.getNewTickRange(type,start,end);
  }
  
  protected TickRange getNewTickRange(TickRangeType type,
                                      long start,
                                      long end,
                                      TickData data)
  {
    return tickRangePool.getNewTickRange(type,start,end,data);
  }
  
  protected void returnTickRange(TickRange tr)
  {
    //returnTickRange(tr);
  }
  
  protected TickRange cloneTickRange(TickRange old)
  {
    return getNewTickRange(old.type,old.start,old.end,old.data);
  }
  
  public boolean isInError()
  {
    return error;
  }
  
  protected void error()
  {
    
  }
    
  protected boolean setRange(TickRange tr)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRange", new Object[] { tr });

    boolean changed = setRange(tr, true);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRange", new Boolean(changed));
    
    return changed;
  }
  
  protected boolean setRange(TickRange tr, boolean errorCheck)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.entry(tc, "setRange", new Object[] { tr, new Boolean(errorCheck) });

    boolean changed = false;

    if(tr.isInUse() || error)
    {
      SIMPErrorException e = new SIMPErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
              "1:136:1.7",
              this},
            null));
      
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList.setRange",
        "1:143:1.7",
        this);
        
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         SibTr.exception(tc, e); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "setRange");
      
      throw e;
    }
    
    changed = setRange(tr, errorCheck, true);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRange", new Boolean(changed));
    
    return changed;
  }
  
  private boolean setRange(TickRange tr, boolean errorCheck, boolean consolidateCompleted)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRange", new Object[] { tr, new Boolean(errorCheck), new Boolean(consolidateCompleted) });

    boolean changed = false;

    //find the range which currently contains the new start tick
    TickRange insertPoint = findRange(tr.start);
    //if it exists, which it must!
    if(insertPoint != null)
    {
      //check the state change for the first range
      TickRangeType state = TickRangeType.stateTransition(tr.type, insertPoint.type);
      //if there is an error
      if(errorCheck && state == TickRangeType.ERROR)
      {
        error = true;
        
        SIMPErrorException e = new SIMPErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
                "1:186:1.7",
                this},
              null));
        
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList.setRange",
          "1:193:1.7",
          this);
          
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
           SibTr.exception(tc, e); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "setRange");
        
        throw e;
      } //end of error block ... if we went in here then we don't go any further
      
      //if there was no error
      
      if(consolidateCompleted && tr.type == TickRangeType.COMPLETED && tr.type == insertPoint.type)
      {
        //if the two ranges are of the same type then there is no need to modify
        //the existing range ... so shrink the new range down and restart if
        //there is anything left to add in
        if(insertPoint.end < TickRange.MAX)
        {
          tr.start = insertPoint.end + 1;
          if(tr.start <= tr.end)
          {
            changed = setRange(tr, errorCheck) || changed;
          }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "setRange", new Boolean(changed));
          
        return changed;
      }
      
      //work out what kind of insert, remove or alter we are doing
      boolean midStart = (tr.start > insertPoint.start);
      boolean midEnd = (tr.end <= insertPoint.end);
      
      if(midStart)
      {
        if(midEnd)
        {
          if(tr.end == insertPoint.end)
          {
            changed = true;
            //the start is in the middle and the end is exactly at the end
            //of the checked range
            //just shift up the existing range and insert the new one
            insertPoint.end = tr.start - 1;
            
            TickRange insertBefore = (TickRange) insertPoint.getNext();
            if(insertBefore != null &&
               consolidateCompleted &&
               tr.type == TickRangeType.COMPLETED
               && tr.type == insertBefore.type)
            {
              //if the next range has the same type as the new one then all we have to
              //do is expand that one rather than insert the new one.
              insertBefore.start = tr.start;
            }
            else
            {
              insertAfter(tr,insertPoint);
            } 
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "setRange", new Boolean(changed));
            return changed;
          }
                    
          //if both the start and end of the new range is within the
          //insertAfter range then we need to split the range to insert the new one
          TickRange tempInsert = cloneTickRange(insertPoint);
          tempInsert.start = tr.start;
                                                             
          //we already did error checking for these ranges
          changed = setRange(tempInsert, false, false);
          changed = setRange(tr, false) || changed;
          
        }
        else //midStart && !midEnd
        {
          //the new range starts in the middle of the current one and ends
          //after it's end so we are shrinking this one
          //then move on to the next range
          TickRange insertBefore = (TickRange)insertPoint.getNext();
          changed = processRanges(tr, insertPoint, insertBefore, consolidateCompleted);
        }
      }      
      else // !midStart
      {
        //the start of the new range is the same as the start of the checked range
        //so the insertPoint is the previous range
        TickRange insertBefore = insertPoint;
        insertPoint = (TickRange)insertPoint.getPrevious();

        changed = processRanges(tr, insertPoint, insertBefore, consolidateCompleted);        
      }
    }
    else
    {
      //if the insertion point is null now then we are inserting at the
      //start of the list
      //should never happen!
      error = true;
      
      SIMPErrorException e = new SIMPErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {
              "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
              "1:301:1.7",
              this},
            null));
      
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList.setRange",
        "1:308:1.7",
        this);
        
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
         SibTr.exception(tc, e); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "setRange");
      
      throw e;
    }      
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRange", new Boolean(changed));
      
    return changed;
  }

  /**
   * We know the the start of the new range is now directly after the insertAfter
   * range (this processing has already been done). However, we do not yet know
   * exactly where the new range ends. The insertBefore range is the next one in the
   * existing list. This method can then be called recursively, removing ranges,
   * until the insertBefore range is the one which contains the end of the new range.
   * we then adjust it accordingly.
   * 
   * @param tr
   * @param insertAfter
   * @param insertBefore
   */
  private boolean processRanges(TickRange tr,
                             TickRange insertAfter,
                             TickRange insertBefore,
                             boolean consolidateCompleted)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "processRanges",
        new Object[] { tr, insertAfter, insertBefore, new Boolean(consolidateCompleted) });

    boolean changed = false;
    
    while(insertBefore != null && tr.end >= insertBefore.end)
    {
      error = true;
      //check for errors
      TickRangeType state = TickRangeType.stateTransition(tr.type, insertBefore.type);
      if(state == TickRangeType.ERROR)
      {
        error = true;
        
        SIMPErrorException e = new SIMPErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {
                "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
                "1:364:1.7",
                this},
              null));
        
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList.processRanges",
          "1:371:1.7",
          this);
          
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
           SibTr.exception(tc, e); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processRanges");
        
        throw e;            
      }
      //if not an error then we are simply overwriting this range, i.e. removing it
      insertBefore = removeRange(insertBefore);                  
    }

    //we should now have found the range which contains the end of the new range
    //there should always be a range at the end, ending at TickRange.MAX
    //so this should never be null, but check anyway
    if(insertBefore != null)
    {
      changed = true;
      if(tr.end >= insertBefore.start)
      { 
        //check for errors on this one last range
        TickRangeType state = TickRangeType.stateTransition(tr.type, insertBefore.type);
        if(state == TickRangeType.ERROR)
        {          
          error = true;
          
          SIMPErrorException e = new SIMPErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                new Object[] {
                  "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
                  "1:404:1.7",
                  this},
                null));
          
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList.processRanges",
            "1:411:1.7",
            this);
            
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
             SibTr.exception(tc, e); 
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processRanges");
          
          throw e;
        }
                
        //shrink the range after ours to start just after the new one ends
        insertBefore.start = tr.end + 1;          
        
      }
      if(consolidateCompleted)
      {
        boolean preConsolidation = (tr.type == TickRangeType.COMPLETED && tr.type == insertAfter.type);
        boolean postConsolidation = (tr.type == TickRangeType.COMPLETED && tr.type == insertBefore.type);
      
        if(preConsolidation)
        {
          if(postConsolidation)
          {
            removeRange(insertBefore);
            insertAfter.end = insertBefore.end;
          }
          else
          {
            insertAfter.end = tr.end;
          }
        }
        else // !preConsolidation
        {
          //shrink the range before to end just before ours starts
          insertAfter.end = tr.start - 1;
          if(postConsolidation)
          {
            insertBefore.start = tr.start;
          }
          else
          {
            insertAfter(tr,insertAfter);
          }
        }
      }
      else
      {
        //shrink the range before to end just before ours starts
        insertAfter.end = tr.start - 1;
        //insert the new range
        insertAfter(tr,insertAfter);
      }
    }
    else
    {
      //insert the new range ... right at the very end
      tr.end = TickRange.MAX;
      insertAfter(tr,insertAfter);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processRanges", new Boolean(changed));
      
    return changed;
  }

  private TickRange removeRange(TickRange range)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeRange", new Object[] { range });

    if(cursor == range)
    {
      //we're never going to remove the first entry but we might remove the last one
      if(range == last)
      {
        cursor = (TickRange) cursor.getPrevious();
      }
      else
      {
        cursor = (TickRange) cursor.getNext();
      }
    } 
    TickRange next = (TickRange) range.getNext();
    remove(range);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeRange", next);
      
    return next;
  }
    
  protected TickRange findRange(long tick)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "findRange", new Object[] { new Long(tick) });

    TickRange found = null;
    long diff = cursor.diff(tick);
    while(diff != 0)
    {
      if(diff > 0)
      {
        if(cursor.end == TickRange.MAX)
          break;
        
        cursor = (TickRange) cursor.getNext();        
      }
      else
      {
        if(cursor.start == TickRange.MIN)
          break;
        
        cursor = (TickRange) cursor.getPrevious();
      }
      diff = cursor.diff(tick);      
    }
    if(diff == 0) found = cursor;        
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "findRange", found);
    
    return found;
  }
  
  protected void setCursor(TickRange range)
  {
    cursor = range;
  }
  
  protected TickRange moveCursorToStart()
  {
    cursor = (TickRange)getFirst();
    return cursor;
  }
  
  protected TickRange moveCursorToEnd()
  {
    cursor = (TickRange)getLast();
    return cursor;
  }
  
  protected TickRange getCursor()
  {
    return cursor;
  }
  
  protected TickRange next()
  {
    TickRange next = null;
    cursor = (TickRange) cursor.getNext();
    if(cursor == null)
    {
      cursor = (TickRange) last;
    } 
    else
    {
      next = cursor;
    }
    return next;
  }
    
  protected TickRange previous()
  {
    TickRange prev = null;
    cursor = (TickRange) cursor.getPrevious();
    if(cursor == null)
    {
      cursor = (TickRange) first;
    }
    else
    {
      prev = cursor;
    }
    return prev;
  }
  
  protected long getCompletedPrefix()
  {
    return ((TickRange)getFirst()).end;  
  }
  
  public boolean containsState(TickRangeType state, long start, long end)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "containsState", new Object[] { state, new Long(start), new Long(end) });
      
    TickRange curr = findRange(start);
    boolean found = curr.type == state;
    while(!found && end > curr.end)
    {
      curr = (TickRange)curr.getNext();
      found = curr.type == state;
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "containsState", new Boolean(found));
     
    return found;
  }
}
