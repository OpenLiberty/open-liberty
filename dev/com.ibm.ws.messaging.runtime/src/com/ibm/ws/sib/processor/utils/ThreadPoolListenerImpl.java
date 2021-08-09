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
package com.ibm.ws.sib.processor.utils;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ThreadPool;
import com.ibm.ws.util.ThreadPoolListener;

/**
 * Implementation used to associate thread with the Messging Engine for trace purposes.
 */
public class ThreadPoolListenerImpl implements ThreadPoolListener
{
  private static final TraceComponent tc =
    SibTr.register(
      ThreadPoolListenerImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private MessageProcessor _messageProcessor;
  
  public ThreadPoolListenerImpl(MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "ThreadPoolListenerImpl", messageProcessor);
    
    _messageProcessor = messageProcessor; 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "ThreadPoolListenerImpl", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.util.ThreadPoolListener#threadPoolCreated(com.ibm.ws.util.ThreadPool)
   */
  public void threadPoolCreated(ThreadPool tp)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.util.ThreadPoolListener#threadCreated(com.ibm.ws.util.ThreadPool, int)
   */
  public void threadCreated(ThreadPool tp, int poolSize)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.util.ThreadPoolListener#threadStarted(com.ibm.ws.util.ThreadPool, int, int)
   * 
   * Only need to push the messaging engine at thread started time.  This should be a one off cost.
   */
  public void threadStarted(ThreadPool tp, int activeThreads, int maxThreads)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "threadStarted", new Object[]{tp, new Integer(activeThreads), new Integer(maxThreads)});
    
    if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled() || tc.isDumpEnabled() || tc.isEntryEnabled() || tc.isEventEnabled()))
      SibTr.push(_messageProcessor.getMessagingEngine());  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "threadStarted");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.util.ThreadPoolListener#threadReturned(com.ibm.ws.util.ThreadPool, int, int)
   */
  public void threadReturned(ThreadPool tp, int activeThreads, int maxThreads)
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.util.ThreadPoolListener#threadDestroyed(com.ibm.ws.util.ThreadPool, int)
   */
  public void threadDestroyed(ThreadPool tp, int poolSize)
  {
  }

}
