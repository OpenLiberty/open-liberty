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
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A very simple linked list. There is no locking and little function but
 * it does exactly what we need
 * 
 * @author dware
 */
public class SimpleLinkedList
{
  //The first entry in the list
  protected SimpleEntry first = null;
  //the last entry in the list
  protected SimpleEntry last = null;
  
  private static TraceComponent tc =
    SibTr.register(
      SimpleLinkedList.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);


  /**
   * Create a new LinkedList
   */
  public SimpleLinkedList()
  {
  }

  /**
   * Add an entry to the list
   * @param simpleEntry
   */
  public void put(SimpleEntry simpleEntry)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "put", simpleEntry);
    
    simpleEntry.previous = last;
    simpleEntry.list = this;
    
    if(last != null)
      last.next = simpleEntry;
    else
      first = simpleEntry;
      
    last = simpleEntry;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "put", printList());
  }
  
  /**
   * Return the first entry in the list (may be null)
   * @return
   */
  public SimpleEntry getFirst()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "getFirst");

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "getFirst", new Object[] {first, printList()});
    
    return first;          
  }
  
  // DO NOT TRACE
  // Return the first and last entries in the list 
  protected String printList()
  {
    String output = "[";
    
    SimpleEntry pointer = first;
    int counter = 0;
    while((pointer != null) && (counter < 3))
    {
      output += "@"+Integer.toHexString(pointer.hashCode());
      pointer = pointer.next;
      if(pointer != null)
        output +=  ", ";
      counter++;
    }
    if(pointer != null)
    {
      output += "..., @"+Integer.toHexString(last.hashCode()) + "]";
    }
    else
      output += "]";
    
    return output;
  }
}
