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
package com.ibm.ws.sib.processor.utils.linkedlist;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A cursor in to a LinkedList
 * 
 * @author tevans
 */
public class Cursor extends Entry
{
  private static final TraceComponent tc =
    SibTr.register(
      Cursor.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
//NLS for component
   private static final TraceNLS nls =
     TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  //the entry currently pointed at
  protected Entry current = null;
  //the name of this cursor
  protected String name = "Cursor";
  //a flag which indicates if this cursor is currently at the top
  //of the list, not pointing at any actual entry
  protected boolean atTop = false;
  //a flag which indicates if this cursor is currently at the bottom
  //of the list, not pointing at any actual entry
  protected boolean atBottom = false;
    
  /**
   * Unsynchronized. It is the responsibility of the caller to ensure proper
   * synchronization of the linked list before this constructor is called.
   * 
   * Construct a new cursor which is initally at the top or bottom of the list,
   * not actually pointing to an entry yet.
   * 
   * @param name The name of the cursor (optional)
   * @param parentList The list in to which this cursor should point
   * @param atTop true if this cursor should start at the top of the list,
   * false if it should start at the bottom
   */
  Cursor(String name, LinkedList parentList, boolean atTop)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "Cursor", 
        new Object[] { name, parentList, new Boolean(atTop) });

    this.name = name;
    this.parentList = parentList;
    this.current = null;
    this.atTop = atTop;
    atBottom = !atTop;
    
    //if this cursor should start at the top of the list
    if(atTop)
    {
      //insert this new cursor in to the list's list of cursors which are at the top
      Cursor nextCursor = parentList.firstTopCursor;
      if(nextCursor != null)
      {
        nextCursor.previous = this;
        next = nextCursor;
      }
      parentList.firstTopCursor = this;
    }
    else //if this cursor should start at the bottom of the list
    {
      //insert this new cursor in to the list's list of cursors which are at the bottom
      Cursor nextCursor = parentList.firstBottomCursor;
      if(nextCursor != null)
      {
        nextCursor.previous = this;
        next = nextCursor;
      }
      parentList.firstBottomCursor = this;
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "Cursor", this);
  }
    
  /**
   * Synchronized. Move the cursor down to the next entry
   * in the list and return it.
   * 
   * @return The next entry in the list, or null if there is no next entry. 
   */
  public synchronized Entry next()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "next");

    // can only do anything if the cursor is still pointing in to a list
    checkEntryParent();
    
    Entry nextEntry = null;
    
    synchronized(parentList)
    {
      //get the next entry in the list
      nextEntry = getNextEntry();
      //if the next entry is null
      if(nextEntry == null)
      {
        //then hopefully we're at the end of the list
        if(current == parentList.last)
        {
          //so move the cursor to the bottom of the list,
          //not pointing to any actual entry
          moveToBottom();
        }
        else if(!atBottom)                   
        {
          //it should not be possible for the next entry to be null but the current
          //not be the last one in the list or already at the bottom
          SIErrorException e = new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] { "Cursor", "1:160:1.15" },
              null));
    
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.utils.linkedlist.Cursor.next",
            "1:166:1.15",
            this);
      
          SibTr.exception(tc, e);      
    
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "next", e);
    
          throw e;
        }
      }
      else
      {
        //move the cursor to the next entry
        moveCursor(nextEntry);
      }
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "next", nextEntry);

    return nextEntry;
  }
  
  /**
   * Synchronized. Move the cursor up to the previous entry
   * in the list and return it.
   * 
   * @return The previous entry in the list, or null if there is no previous entry. 
   */
  public synchronized Entry previous()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "previous");

//  can only do anything if the cursor is still pointing in to a list
    checkEntryParent();
  
    Entry previousEntry = null;
  
    synchronized(parentList)
    {
      //get the previous entry
      previousEntry = getPreviousEntry();
      //if it is null
      if(previousEntry == null)
      {
        //then hopefully we are at the top of the list
        if(current == parentList.first)
        {
          //so move the cursor to the top
          moveToTop();
        }
        else if(!atTop)
        {
          //it should not be possible for the previous entry to be null but the current
          //not be the first one in the list or already at the top
          SIErrorException e = new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.utils.linkedlist.Cursor",
                "1:228:1.15" },
             null));
    
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.utils.linkedlist.Cursor.previous",
            "1:234:1.15",
            this);
      
          SibTr.exception(tc, e);      
    
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "previous", e);
    
          throw e;
        }
      }
      else
      {
        //move the cursor to the previous entry
        moveCursor(previousEntry); 
      }
    }
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "previous", previousEntry);

    return previousEntry;
  }
    
  /**
   * Synchronized. Get the previous entry in the list (but do not move the cursor to it).
   * 
   * @return the previous entry in the list
   */
  public synchronized Entry getPreviousEntry()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getPreviousEntry");
    
    checkEntryParent();
    
    Entry previousEntry = null;
    
    synchronized(parentList)
    {            
      if(atBottom)
      {
        //if at the bottom of the list, return the last entry in the list
        previousEntry = parentList.last;
      }
      else if(!atTop)                        
      {
        //otherwise it's just the previous entry in the list
        previousEntry = current.getPrevious();                        
      }
      //if the cursor is at the top of the list then we should
      //just drop through and the previous entry is null
    }    
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getPreviousEntry", previousEntry);

    return previousEntry;
  }    
  
  /**
   * Synchronized. Get the next entry in the list (but do not move the cursor to it).
   * 
   * @return the previous entry in the list
   */
  public synchronized Entry getNextEntry()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getNextEntry");
    
    checkEntryParent();
    
    Entry nextEntry = null;
    
    synchronized(parentList)
    {
      if(atTop)
      {
        //if the cursor is at the top of the list, return the first entry in the list
        nextEntry = parentList.first;
      }
      else if(!atBottom)                        
      {
        //otherwise just return the next entry in the list
        nextEntry = current.getNext();                        
      }
      //if the cursor is at the bottom of the list then we should
      //just drop through and the next entry is null
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getNextEntry", nextEntry);
    
    return nextEntry;
  }
  
  /**
   * Synchronized. Get the entry currently pointed to by this cursor.
   * @return the entry currently pointed to by this cursor
   */
  public synchronized Entry current()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "current");

    checkEntryParent();
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "current", current);

    return current;
  }
  
  /**
   * Synchronized. Move the cursor to the top of the list.
   */
  public synchronized void moveToTop()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "moveToTop");

    checkEntryParent();

    //record the reference to the parent list because the call to forceRemove
    //will nullify it.
    LinkedList list = parentList;
    synchronized(list)
    {
      //remove the cursor from it's current postion
      forceRemove();
      //reset the parent list reference
      parentList = list;
      //mark this cursor as being at the top
      atTop = true;
      
      //link this cursor to the first cursor in the list's list of cursors
      //which are currently at the top
      next = list.firstTopCursor;
      //link the old first cursor to this one 
      if(next != null) next.previous = this;
      //mark this one as the first cursor in the list
      list.firstTopCursor = this;
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "moveToTop");
  }
  
  /**
   * Synchronized. Move the cursor to the bottom of the list.
   */
  public synchronized void moveToBottom()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "moveToBottom");

    checkEntryParent();
    
    //record the parent list
    LinkedList list = parentList;
    synchronized(list)
    {
      //remove the cursor from it current position
      forceRemove();
      //reset the parent list
      parentList = list;
      //mark it as at the bottom
      atBottom = true;
      
      //insert it in front of the old first bottom cursor
      next = list.firstBottomCursor;
      if(next != null) next.previous = this;
      //mark this on as the first bottom cursor
      list.firstBottomCursor = this;      
    }    
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "moveToBottom");
  }
  
  /**
   * Synchronized. Move this cursor to point at a different entry in the
   * same list.
   * 
   * @param newEntry
   */
  public synchronized void moveCursor(Entry newEntry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "moveCursor", new Object[] { newEntry });
    
    checkEntryParent();
    
    //if the entry is the same as the current one
    //then we don't need to bother doing anything
    if(newEntry != current)
    {
      //make sure we can find the new entry in this list
      if(newEntry != null && newEntry.parentList == parentList)
      {      
        LinkedList list = parentList;
        synchronized(list)
        {
          //remove the cursor from it's current postion
          forceRemove();
          
          //insert this cursor before the first on for the new entry
          next = newEntry.firstCursor;
          if(next != null) next.previous = this;
          newEntry.firstCursor = this;
          previous = null;
          parentList = list;
                 
          current = newEntry;
        }
      }
      else //if the new entry is not in this list, throw a runtime exception
      {
        SIErrorException e = new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.linkedlist.Cursor",
              "1:457:1.15" },
           null));

        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.utils.linkedlist.Cursor.moveCursor",
          "1:463:1.15",
          this);

        SibTr.exception(tc, e);      

        if (tc.isEntryEnabled())
          SibTr.exit(tc, "moveCursor", e);

        throw e;
      }
    }        
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "moveCursor");
  }
  
  /**
   * Synchronized. Move this cursor to point at the same one as a given cursor.
   * 
   * @param cursor
   */
  public synchronized void moveCursor(Cursor cursor)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "moveCursor", new Object[] { cursor });
    
    checkEntryParent();
      
    synchronized(parentList)
    {
      //make sure that the cursor is pointing to an entry in the same list
      if(cursor != null && cursor.parentList == parentList)
      {
        //if we are already pointing to the same entry, we may not have to
        //do anything
        if(cursor.current != current)
        {
          if(cursor.atTop)
          {
            //if the new cursor is at the top, move this one to the top
            moveToTop();
          }
          else if(cursor.atBottom)
          {
            //if the new cursor is at the bottom, move this one to the bottom
            moveToBottom();
          }
          else
          {
            //otherwise just move this cursor to point to the same entry as the
            //one pointed to by the given cursor
            moveCursor(cursor.current);
          }
        }
        else //it is possible that the current entries are both null if they are both
        {    //at the top or bottom on the list
          if(cursor.current == null)
          {
            if(cursor.atTop && atBottom)
            {
              //if the given cursor is at the top but we are at the bottom, move to the top
              moveToTop();
            }
            else if(cursor.atBottom && atTop)
            {
              //if the given cursor is at the bottom but we are at the top, move to the bottom
              moveToBottom();
            }
          }
        }
      }
      else //if the new entry is not in this list, throw a runtime exception
      {
        SIErrorException e = new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.linkedlist.Cursor",
              "1:541:1.15" },
           null));
  
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.utils.linkedlist.Cursor.moveCursor",
          "1:547:1.15",
          this);
  
        SibTr.exception(tc, e);      
  
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "moveCursor", e);
  
        throw e;
      }
    }        
  
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "moveCursor");
  }
  
  /**
   * Not Synchronized.
   * 
   * Removes a cursor from the list. It is the responsibility of the caller
   * to ensure that the list AND the cursor are properly synchronized before this method
   * is called.
   */
  Entry forceRemove()
  {
    Entry removedEntry = null;

    checkEntryParent();
        
    if(atTop)
    {
      //if we are at the top of the list and we are the first top cursor
      if(parentList.firstTopCursor == this)
      {
        //set the first top cursor to be the next one in the list
        parentList.firstTopCursor = (Cursor) next;                
      }
      //mark us as not at the top
      atTop = false;
    }
    else if(atBottom)
    {
      //if we are at the bottom of the list and we are the first bottom cursor
      if(parentList.firstBottomCursor == this)
      {
        //set the first bottom cursor to be the next one in the list
        parentList.firstBottomCursor = (Cursor) next;                
      }
      //mark us as not at the bottom
      atBottom = false;
    }
    else
    {
      //if we are pointing to an actual entry in the list
      if(current.firstCursor == this)
      {
        //and we are the first cursor on that entry
        //set the first cursor to be the next one in the list
        current.firstCursor = (Cursor) next;
      }
      //set the current entry to null
      current = null;
    }
    //actually remove this cursor from the list
    removedEntry = super.forceRemove();
    
    return removedEntry;
  }
  
  /**
   * Synchronized. Indicate that this cursor is finished with. This call will
   * remove this cursor from the list and make it subsequently unusable.
   */
  public synchronized void finished()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "finished");
    
    checkEntryParent();
    
    synchronized(parentList)
    {
      //remove the cursor from the list
      //this will set the parent list reference to null and make the
      //cursor seubsequently unusable
      forceRemove();      
    }    
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "finished");        
  }
  
  /**
   * Synchronized. Check that the parent list is not null and therefore the cursor
   * is still valid. If not, throw a runtime exception.
   * 
   * This is the same as the version in Entry except that it is synchronized.
   */
  synchronized void checkEntryParent()
  {
    super.checkEntryParent();
  }

  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return "Cursor("+name+")";
  }

  public synchronized boolean isAtBottom()
  {
    return atBottom;
  }
}
