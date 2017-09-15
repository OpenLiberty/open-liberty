/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.threadpool.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPoolFullException;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class provides a Thread Pool implementation which is backed by a real WAS thread pool.
 *
 * @author Gareth Matthews
 */
public class RichThreadPoolImpl implements ThreadPool
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(RichThreadPoolImpl.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/threadpool/impl/RichThreadPoolImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
   }

   /** The underlying WAS thread pool */
   private com.ibm.ws.util.ThreadPool wasThreadPool = null;

   /**
    * Creates the underlying WAS thread pool.
    *
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#initialise(java.lang.String, int, int)
    */
   public void initialise(String name, int minSize, int maxSize)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initialise",
                                           new Object[]{name, minSize, maxSize});

      wasThreadPool = new com.ibm.ws.util.ThreadPool(name, minSize, maxSize);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initialise");
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#setGrowAsNeeded(boolean)
    */
   public void setGrowAsNeeded(boolean growAsNeeded)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setGrowAsNeeded", Boolean.valueOf(growAsNeeded));
      wasThreadPool.setGrowAsNeeded(growAsNeeded);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setGrowAsNeeded");
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#execute(java.lang.Runnable, int)
    */
   public void execute(Runnable runnable, int was_thread_mode)
      throws InterruptedException, IllegalStateException, ThreadPoolFullException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "execute", new Object[]{runnable, was_thread_mode});
      try
      {
         wasThreadPool.execute(runnable, was_thread_mode);
      }
      catch (com.ibm.ws.util.ThreadPool.ThreadPoolQueueIsFullException e)
      {
         // No FFDC Code Needed
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Caught and rethrowing: ", e);
         throw new ThreadPoolFullException(e);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "execute");
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#execute(java.lang.Runnable)
    */
   public void execute(Runnable runnable)
      throws InterruptedException, IllegalStateException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "execute", runnable);
      wasThreadPool.execute(runnable);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "execute");
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#setKeepAliveTime(long)
    */
   public void setKeepAliveTime(long msecs)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setKeepAliveTime", Long.valueOf(msecs));
      wasThreadPool.setKeepAliveTime(msecs);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setKeepAliveTime");
   }

   /**
    * @see com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool#setRequestBufferSize(int)
    */
   public void setRequestBufferSize(int size)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setRequestBufferSize", Integer.valueOf(size));
      wasThreadPool.setRequestBufferSize(size);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setRequestBufferSize");
   }
}
