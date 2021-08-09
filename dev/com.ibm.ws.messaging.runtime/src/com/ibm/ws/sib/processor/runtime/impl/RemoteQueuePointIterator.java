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
package com.ibm.ws.sib.processor.runtime.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPQueueControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class RemoteQueuePointIterator extends BasicSIMPIterator
{
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  

  private static final TraceComponent tc =
    SibTr.register(
      RemoteQueuePointIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private SIMPQueueControllable _currentQueue;
  private SIMPIterator _currentSubIterator;

  public RemoteQueuePointIterator(SIMPIterator parent)
  {
    super(parent);
    _currentQueue = null;
    _currentSubIterator = null;
  }

  /**
   * Move to the next stream which has active ranges
   */
  private void nextQueue()
  {
    if(super.hasNext())
    {      
      do
      {
        if(_currentSubIterator != null) _currentSubIterator.finished();
        _currentSubIterator = null;
        _currentQueue = (SIMPQueueControllable) super.next();
        
        try
        {
          _currentSubIterator = _currentQueue.getRemoteQueuePointIterator();
        }
        catch (SIMPException e) 
        {
          // No FFDC code needed
        
          if (tc.isDebugEnabled())
            SibTr.exception(tc, e);          
        }       
      }
      // Retry if we failed to get an iterator, that iterator does not
      // have any remote queue points in it and we have more queues available.
      while((_currentSubIterator == null || !_currentSubIterator.hasNext())
        && super.hasNext());
    }
    else
    {
      _currentQueue = null;
      if(_currentSubIterator != null) _currentSubIterator.finished();
      _currentSubIterator = null;
    }
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext()
  {
    boolean hasNext = false;

    if(_currentSubIterator == null || !_currentSubIterator.hasNext())
    {
      nextQueue();
    }

    if(_currentSubIterator != null)
    {
      hasNext = _currentSubIterator.hasNext(); 
    }
    
    return hasNext;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Object next()
  {
    if(_currentSubIterator == null || !_currentSubIterator.hasNext())
    {
      nextQueue();
    }
    
    Object nextRemoteQueue = null;

    if(_currentSubIterator != null)
    {
      nextRemoteQueue = _currentSubIterator.next(); 
    }

    return nextRemoteQueue;        
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove()
  {
    InvalidOperationException finalE =
      new InvalidOperationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0005",
          new Object[] {"RemoteQueuePointIterator.remove",
                         "1:152:1.14",
                        this},
          null));

    SibTr.exception(tc, finalE);
    throw finalE;
  }

}
