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

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.runtime.SIMPControllable;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;

public class ControllableIterator implements SIMPIterator
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private Iterator parent;
  SIMPControllable next = null;

  public ControllableIterator(Iterator parent)
  {
    this.parent = parent;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext()
  {
    while(next == null)
    {
      try
      {
        next = (SIMPControllable) next();
      }
      catch(NoSuchElementException e)
      {
        //No FFDC code needed
        break;
      }
    }
    boolean hasNext = (next != null);
    return hasNext;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Object next()
  {
    SIMPControllable control = null;
    if(next == null)
    {
      ControllableResource controllable = (ControllableResource)parent.next();
      if(controllable != null)
      {
        control = controllable.getControlAdapter();
      }
      while(control == null)
      {
        controllable = (ControllableResource)parent.next();
        if(controllable != null)
        {
          control = controllable.getControlAdapter();
        }
      }
    }
    else
    {
      control = next;
      next = null;
    }
    return control;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove()
  {
    throw new InvalidOperationException(nls.getFormattedMessage(
      "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.runtime.ControllableIterator",
        "1:110:1.12" },
      null)
      );
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPIterator#finished()
   */
  public void finished()
  {
    if(parent instanceof SIMPIterator)
    {
      ((SIMPIterator)parent).finished();
    } 
  }

}
