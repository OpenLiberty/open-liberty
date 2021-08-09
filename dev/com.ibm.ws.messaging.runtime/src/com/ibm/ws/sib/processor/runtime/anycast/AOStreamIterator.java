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
import com.ibm.ws.sib.processor.impl.AOStream;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.impl.TransmitMessageRequest;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * An Iterator over the AOStream
 */
public class AOStreamIterator implements SIMPIterator
{
  private static TraceComponent tc =
    SibTr.register(
      AOStreamIterator.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

	private AOStream aoStream;
	private Iterator msgIterator;
  private MessageProcessor messageProcessor;
  private AnycastOutputHandler aoh;
	
	public AOStreamIterator(AOStream aoStream,
                          MessageProcessor messageProcessor, AnycastOutputHandler aoh)
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AOStreamIterator", new Object[]{aoStream, messageProcessor, aoh});
 
		this.aoStream = aoStream;
    List ticks = aoStream.getTicksOnStream();
		msgIterator = ticks.iterator();
    this.messageProcessor = messageProcessor;
    this.aoh = aoh;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AOStreamIterator", this);
  }

	/* (non-Javadoc)
	 * @see com.ibm.ws.sib.processor.runtime.SIMPIterator#finished()
	 */
	public void finished()
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "finished");
 
		msgIterator = null;
		aoStream = null;
    
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
    {
      SibTr.entry(tc, "next");
    } 
    
    Object result;
		if (msgIterator.hasNext())
		{
      Long valueStamp = (Long) msgIterator.next();
      
			result = new TransmitMessageRequest(valueStamp.longValue(), aoStream.itemStream, aoStream, messageProcessor, aoh.getDestinationHandler());
		}
		else
		{
			result = null;
		}
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "next", result);
    } 
    return result;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove()
	{
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "remove");
    } 
    
		if (msgIterator.hasNext())
		{
			msgIterator.remove();
		}
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "remove");
    } 
	}

}
