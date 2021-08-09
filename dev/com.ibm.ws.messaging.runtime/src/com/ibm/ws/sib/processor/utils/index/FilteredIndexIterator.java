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
package com.ibm.ws.sib.processor.utils.index;

import java.util.NoSuchElementException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.utils.linkedlist.Cursor;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Class to represent the various lookups for different destination types
 */
public class FilteredIndexIterator implements SIMPIterator
{
  /**
   * Trace for the component
   */
  private static final TraceComponent tc =
    SibTr.register(
      FilteredIndexIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  //NLS for component
   private static final TraceNLS nls =
     TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private Index index;
  private Cursor cursor;
  private IndexFilter filter;
  private Index.Entry next;
  private Index.Entry removable;

  public FilteredIndexIterator(Index index)
  {
    this(index, null);
  }

  public FilteredIndexIterator(Index index, IndexFilter filter)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "FilteredIndexIterator", new Object[] { index, filter });

    this.index = index;
    cursor = index.newCursor();
    this.filter = filter;
    next = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "FilteredIndexIterator", this);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public synchronized boolean hasNext()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "hasNext");

    if(next == null)
    {
      synchronized(index)
      {
        next = (Index.Entry) cursor.next();
        if(filter != null)
        {
          while(next != null && !filter.matches(next.type))
          {
            next = (Index.Entry) cursor.next();
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "hasNext", new Boolean(next != null));

    return (next != null);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public synchronized Object next()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "next");

    Object next = null;
    if(hasNext())
    {
      next = this.next.data;
      removable = this.next;
      this.next = null;
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "next", "NoSuchElementException");
      throw new NoSuchElementException(
        nls.getFormattedMessage(
          "NO_ELEMENTS_ERROR_CWSIP0601",
          null,
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "next", next);

    return next;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public synchronized void remove()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove");

    if(removable != null)
    {
      index.remove(removable);
    }
    else
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "remove", "IllegalStateException");
      throw new IllegalStateException(
        nls.getFormattedMessage(
          "NO_ELEMENTS_ERROR_CWSIP0602",
          null,
          null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPIterator#finished()
   */
  public void finished()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finished");

    cursor.finished();
    cursor = null;
    synchronized(this)
    {
      filter = null;
      index = null;
      next = null;
      removable = null;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "finished");
  }
}
