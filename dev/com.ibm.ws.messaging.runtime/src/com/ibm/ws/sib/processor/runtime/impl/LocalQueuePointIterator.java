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
import com.ibm.ws.sib.processor.runtime.SIMPLocalQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueueControllable;
import com.ibm.ws.sib.utils.ras.SibTr;

public class LocalQueuePointIterator implements SIMPIterator
{
  private static final TraceComponent tc =
    SibTr.register(
      LocalQueuePointIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  // NLS for component
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  SIMPIterator parent;
  
  public LocalQueuePointIterator(SIMPIterator localQueueIterator)
  {
    this.parent = localQueueIterator;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Object next()
  {
    SIMPQueueControllable control = null;
    SIMPLocalQueuePointControllable localControl = null;
    
    while (null != (control = (SIMPQueueControllable)parent.next()))
    {
      // Skip over queues which are corrupt
      try
      {
        localControl = control.getLocalQueuePointControl();
        break;
      }
      catch (SIMPException e)
      {
        // No FFDC code needed
        
        if (tc.isDebugEnabled())
          SibTr.exception(tc, e);
      }
    }
    
    return localControl;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove()
  {
    throw new InvalidOperationException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.runtime.LocalQueuePointIterator",
          "1:94:1.9" },
        null));
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext()
  {
    return parent.hasNext();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPIterator#finished()
   */
  public void finished()
  {
    parent.finished();
  }

}
