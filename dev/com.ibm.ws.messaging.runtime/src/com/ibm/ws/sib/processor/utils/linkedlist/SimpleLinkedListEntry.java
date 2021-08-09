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
 * A class to encapsulate an entry in the linked list
 * 
 * @author tevans
 */
public class SimpleLinkedListEntry extends Entry
{
  //The original object
  public Object data = null;
  
  private static TraceComponent tc =
    SibTr.register(
      SimpleLinkedListEntry.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  public SimpleLinkedListEntry()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "SimpleLinkedListEntry");

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SimpleLinkedListEntry", this);      
  }
  
  public SimpleLinkedListEntry(Object data)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "SimpleLinkedListEntry", new Object[] { data });

    this.data = data;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SimpleLinkedListEntry", this);      
  }
  
  public synchronized String toString(String indent)
  {    
    StringBuffer buffer = new StringBuffer();
    
    if(parentList == null)
    {
      buffer.append("SimpleLinkedListEntry not in list");
    }
    else
    {
      buffer.append(indent);
      buffer.append("SimpleLinkedListEntry("+data+")");    
      Cursor cursor = firstCursor;
      while(cursor != null)
      {        
        buffer.append("\n");
        buffer.append(indent);
        buffer.append("\\-->");
        buffer.append(cursor);
        cursor = (Cursor) cursor.next;
      }
    }      
      
    return buffer.toString();
  }
}
