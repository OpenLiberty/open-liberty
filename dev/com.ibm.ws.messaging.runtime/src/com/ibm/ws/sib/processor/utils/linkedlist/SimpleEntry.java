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
 * An entry in the SinpleLinkedList. There is no locking and little function but
 * it does exactly what we need
 * 
 * @author dware
 */
public class SimpleEntry
{
  protected SimpleLinkedList list = null;
  
  //The next entry in the list
  protected SimpleEntry next = null;
  
  //The next entry in the list
  protected SimpleEntry previous = null;
  
  private static TraceComponent tc =
    SibTr.register(
      SimpleEntry.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /**
   * Return the next entry in the list
   * 
   * @return
   */
  public SimpleEntry next()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "next", this);
      SibTr.exit(tc, "next", this.next);
    }
    
    return this.next;
  }
  
  /**
   * Remove this entry from the list
   *
   */
  public void remove()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove", this);
    
    if(previous != null)
      previous.next = next;
    else
      list.first = next;
    
    if(next != null)
      next.previous = previous;
    else
      list.last = previous;

    previous = null;
    next = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove", list.printList());
}

}
