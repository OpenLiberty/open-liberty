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
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;

public class TickRangeObjectPool extends ObjectPool
{
  public TickRangeObjectPool(String name, int size)
  {
    super(name, size);
  }

  private static TraceComponent tc =
    SibTr.register(
      TickRangeObjectPool.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  protected TickRange getNewTickRange(TickRangeType type, long start, long end)
  {
    return getNewTickRange(type,start,end,null);
  }
  
  protected TickRange getNewTickRange(TickRangeType type,
                                      long start,
                                      long end,
                                      TickData data)
  {
    TickRange tr = null;
    synchronized(this)
    {
      tr = (TickRange) remove();
    }
    if(tr == null)
    {
      tr = new TickRange(type, start, end, data);
    }
    else
    {
      tr.reset(type, start, end, data);
    }
    return tr;
  }
  
  protected void returnTickRange(TickRange tr)
  {    
    if(tr.isInUse())
    {
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0008",
          new Object[] {
            "com.ibm.ws.sib.processor.gd.statestream.LinkedRangeList",
            "1:92:1.4",
            this,
            tr},
          null));
    }
        
    synchronized(this)
    {
      add(tr);
    }   
  }
}
