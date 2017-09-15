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
package com.ibm.ws.sib.processor.runtime.anycast;

import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.AIStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.RemoteMessageRequest;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An interator over the ticks in the AIStream
 */
public class AIStreamIterator implements SIMPIterator
{    
  private static TraceComponent tc =
    SibTr.register(
      AIStreamIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

	private AIStream aiStream;
	private Iterator msgIterator;
  private int size;
	

	public AIStreamIterator(AIStream aiStream)
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIStreamIterator", new Object[]{aiStream});
 
		this.aiStream = aiStream;
		List<Long> ticks = aiStream.getTicksOnStream();
    size = ticks.size();
		msgIterator = ticks.iterator();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIStreamIterator", this);
 	}
	
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPIterator#finished()
   */
  public void finished()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finished");
      
		msgIterator = null;
		aiStream = null;
    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "finished");
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "hasNext");
      SibTr.exit(tc, "hasNext");
    }
      
    return msgIterator.hasNext();
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Object next()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "next");
    
    Long ticks;  
    RemoteMessageRequest remoteMessageRequest;
    if (msgIterator.hasNext())
    {
      ticks =  (Long) msgIterator.next();
      
      remoteMessageRequest = new RemoteMessageRequest(ticks.longValue(), aiStream);
    }
    else
    {
    	remoteMessageRequest = null;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "next", remoteMessageRequest);
      
    return remoteMessageRequest;      
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "remove");
      
		if (msgIterator.hasNext())
		{
			msgIterator.remove();
		}
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remove");
  }
  
  public int getSize()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSize");
      SibTr.exit(tc, "getSize", size);
    }
      
    return size;
  }
}
