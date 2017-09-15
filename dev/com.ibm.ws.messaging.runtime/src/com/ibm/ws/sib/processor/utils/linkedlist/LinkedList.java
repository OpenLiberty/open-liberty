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
 * This class maintains a doubly linked list of Objects.
 * 
 * Calls to this object are NOT thread-safe.
 * 
 * @author tevans
 */
public class LinkedList
{
  //The first entry in the list
  protected Entry first = null;
  //the last entry in the list
  protected Entry last = null;
  //the first in a list of cursors which are sitting at the top of the
  //list, not pointing to any particular enty in the list
  protected Cursor firstTopCursor = null;
  //the first in a list of cursors which are sitting at the bottom of the
  //list, not pointing to any particular enty in the list
  protected Cursor firstBottomCursor = null;
  
  private static final TraceComponent tc =
    SibTr.register(
      LinkedList.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Create a new LinkedList
   */
  public LinkedList()
  {
  }

  /**
   * Synchronized. Creates a new cursor over this list, initially sitting
   * at the top of the list.
   */
  public Cursor newCursor(String name)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "newCursor", name);

    Cursor cursor = new Cursor(name,this,true);
        
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "newCursor", cursor);
    
    return cursor;
  }
  
  /**
   * Synchronized. Creates a new cursor over this list, initially sitting
   * at the bottoms of the list.
   */
  public Cursor newCursorAtBottom(String name)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "newCursorAtBottom", name);

    Cursor cursor = new Cursor(name,this,false);
      
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "newCursorAtBottom", cursor);
  
    return cursor;
  }

  /**
   * The same as insertAtBottom(Entry)
   * 
   * @param entry
   * @return The entry inserted
   */
  public Entry put(Entry entry)
  {
    return insertAtBottom(entry);
  }
    
  /**
   * Synchronized. Insert a new entry in to the list at the bottom.
   * The new entry must not be already in any list, including this one.
   * 
   * @param entry The entry to be added.
   * @return The entry after it has been added
   */
  public Entry insertAtBottom(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "insertAtBottom", new Object[] { entry });

    //only add in the new entry if it is not already in a list.
    if(entry.parentList == null)
    {
      //if the list is empty
      if(last == null)
      {
        //double check that the link references are null
        entry.previous = null;
        entry.next = null;
        //record the first and last pointers
        first = entry;
        last = entry;
        //set the entry's parent list to show that it is now part of this list
        entry.parentList = this;
      }
      else //if there are already entries in the list
      {
        //insert the new entry after the last one in the list
        insertAfter(entry, last);        
      }            
                 
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "insertAtBottom", entry);
    
      return entry;
    }
    
    //if the entry is already in a list, throw a runtime exception
    SIErrorException e = new SIErrorException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
          "1:166:1.18" },
        null));
          
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList.insertAtBottom",
      "1:172:1.18",
      this);
            
    SibTr.exception(tc, e);   
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
    new Object[] {
      "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
      "1:179:1.18" });   
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "insertAtBottom", e);
          
    throw e;
  }
  
  /**
   * Synchronized. Insert a new entry in to the list at the top.
   * The new entry must not be already in any list, including this one.
   * 
   * @param entry The entry to be added.
   * @return The entry after it has been added
   */
  public Entry insertAtTop(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "insertAtTop", new Object[] { entry });

    //only add the entry in if it is not already in a list
    if(entry.parentList == null)
    {
      //if the list is currently empty
      if(first == null)
      {
        //make sure the link pointers are null
        entry.previous = null;
        entry.next = null;
        //if the list was empty then this new entry must be the last
        //entry as well as the first
        last = entry;
      }
      else //if there were already entries in the list
      {
        //link this entry to the first one in the list
        entry.previous = null;
        entry.next = first;
        
        //list the old first one to this new entry
        first.previous = entry;        
      }
      
      //the new entry is now the first one
      first = entry;
      //mark it to show that it is in this list
      entry.parentList = this;
                 
      if (tc.isEntryEnabled())
        SibTr.exit(tc, "insertAtTop", entry);
    
      return entry;
    }
   
    //if the new entry was already in a list, throw a runtime exception    
    SIErrorException e = new SIErrorException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
          "1:239:1.18" },
        null));
          
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList.insertAtTop",
      "1:245:1.18",
      this);
            
    SibTr.exception(tc, e); 
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
        "1:252:1.18" });     
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "insertAtTop", e);        
    throw e;
  }
    
  /**
   * Synchronized. Insert an entry into the list after a given one. The
   * new entry must not already be in a list. The entry after which the new
   * one is to be inserted must be in this list.
   * 
   * @param newEntry The entry to be added.
   * @param insertAfter The entry after which the new one is to be inserted
   */
  public Entry insertAfter(Entry newEntry, Entry insertAfter)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "insertAfter", new Object[] { newEntry, insertAfter });

    Entry insertedEntry = null;    

    //check that the params are not null, if either is, there is nothing to do.
    if(newEntry != null &&
       insertAfter != null)
    {
      //call the internal unsynchronized insert method
      insertedEntry = insertAfter.forceInsertAfter(newEntry);            
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "insertAfter", insertedEntry);
    
    return insertedEntry;
  }

  public boolean contains(Entry entry)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "contains", new Object[] { entry });

    boolean contains = (entry != null &&
                        entry.parentList == this);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "contains", new Boolean(contains));

    return contains;
  }
            
  /**
   * Synchronized. Remove an Entry from the list.
   * 
   * @param removePointer The Entry to be removed
   * @return The Entry which was removed
   */
  public Entry remove(Entry removePointer)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "remove", new Object[] { removePointer });
    
    Entry removedEntry = null;
    
    //check that the entry to be removed is not null and is in this list
    if(contains(removePointer))
    {         
      //call the internal unsynchronized remove method on the entry to be removed.
      removedEntry = removePointer.forceRemove();
    }
    else //if the entry is not found in this list, throw a runtime exception
    {
      SIErrorException e = new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
            "1:328:1.18" },
          null));
      
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList.remove",
        "1:334:1.18",
        this);
        
      SibTr.exception(tc, e);   
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.utils.linkedlist.LinkedList",
          "1:341:1.18" });   

      if (tc.isEntryEnabled())
        SibTr.exit(tc, "remove", e);
      
      throw e;
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "remove", removedEntry);
    
    //return the object which was removed
    return removedEntry;
  }    
  
  /**
   * Synchronized. Get the first entry in the list.
   * 
   * @return the first entry in the list.
   */
  public Entry getFirst()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getFirst");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getFirst", first);

    return first;
  }
  
  /**
   * Synchronized. Get the last entry in the list.
   * 
   * @return the last entry in the list.
   */
  public Entry getLast()
  {
    if (tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLast");     
      SibTr.exit(tc, "getLast", last);
    }

    return last;
  }

  /**
   * Synchronized. Test if the list contains any entries.
   * 
   * @return true if the list is empty, otherwise false
   */
  public boolean isEmpty()
  {
    return first == null;
  }
  
  /**
   * Synchronized. Transfer all of the entries in a given list in to
   * this one. This is the same as removing each one from the given
   * list and inserting it in to this list at the bottom.
   * 
   * @param transferFrom The list from which the entries are to be transfered from.
   */
  public void transfer(LinkedList transferFrom)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "transfer", new Object[] { transferFrom });

    //synchronize on the other list so that we can call the internal,
    //unsynchronized methods and access the entries in the list directly
    synchronized(transferFrom)
    {
      //iterate over all of the entries in the list, removing them and
      //adding them in to this list at the bottom.
      Entry entry = transferFrom.first;
      while(entry != null)
      {
        insertAtBottom(entry.forceRemove());
        entry = transferFrom.first;
      }
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "transfer");
  }
            
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    //return toString with no initial indentation
    return toString("");
  }
  
  /**
   * Return a string representation of this list, the entries in it
   * and any cursors currently pointing in to this list.
   * 
   * @param indent Each line in the resulting string will begin with this string
   * @return a string representation of this list
   */
  public String toString(String indent)
  {    
    StringBuffer buffer = new StringBuffer();
  
    //Write out a marker for the Head of the list and any cursors
    //which are currently sitting at the top of this list, not pointing
    //to any actual entries
    buffer.append(indent);
    buffer.append("Head");
    Cursor cursor = firstTopCursor;
    while(cursor != null)
    {
      buffer.append("\n");
      buffer.append(indent);
      buffer.append("\\-->");
      buffer.append(cursor);      
      cursor = (Cursor) cursor.next;
    }
    
    //indent a little more before writing out the entries in the list
    String entryIndent = indent + "  ";
    Entry printPointer = first;
    //iterate over the entries in the list, calling their toString methods.
    //The default behaviour of Entry.toString() includes any cursors which are pointing
    //at that entry.
    while(printPointer != null)
    {
      buffer.append("\n");
      buffer.append(printPointer.toString(entryIndent));                  
      printPointer = printPointer.next;
    }    

    //Write out a marker for the Tail of the list and any cursors
    //which are currently sitting at the bottom of this list, not pointing
    //to any actual entries
    buffer.append("\n");
    buffer.append(indent);
    buffer.append("Tail");
    cursor = firstBottomCursor;
    while(cursor != null)
    {
      buffer.append("\n");
      buffer.append(indent);
      buffer.append("\\-->");
      buffer.append(cursor);      
      cursor = (Cursor) cursor.next;
    }
  
    return buffer.toString();
  }
}
