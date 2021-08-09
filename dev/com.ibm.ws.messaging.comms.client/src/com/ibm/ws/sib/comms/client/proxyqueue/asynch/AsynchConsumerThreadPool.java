/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.client.proxyqueue.asynch;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A thread pool used for executing asynchronous consumer callbacks. This thread pool is backed by a WAS thread-pool, the
 * implementation attempts to avoid having contending for a single monitor.
 */

//@Threadsafe
public class AsynchConsumerThreadPool {
   private static final TraceComponent tc = SibTr.register(AsynchConsumerThreadPool.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

   //@start_class_string_prolog@
   public static final String $sccsid = "@(#) 1.28 SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/asynch/AsynchConsumerThreadPool.java, SIB.comms, WASX.SIB, uu1215.01 10/05/11 10:39:07 [4/12/12 22:14:06]";
   //@end_class_string_prolog@

   static {
     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source Info: " + $sccsid);
   }

   /** Default maximum number of threads in the backing WAS thread pool */
   private static final int DEFAULT_MAX_THREADS = 10;

   /** Maximum number of threads in the backing WAS thread pool */
   private static int MAX_THREADS;

   /** Custom property name for max threads in thread pool */
   public final static String CLIENT_ASYNC_CONSUMER_THREADPOOL_MAX_SIZE_PROPERTY  = "com.ibm.ws.sib.comms.client.impl.MaximumClientAsyncConsumerThreadPoolSize";

   /** Name to use for the backing WAS thread pool */
   private static final String THREADPOOL_NAME = "Asynchronous Consumer";

   private final ScheduleQueue[] scheduleQueues;
   private final EmptyScheduleQueueStack emptyStack;
   /* Holds a map of inuse schedulequeues and asynchconsumer proxy queues */
   private Map<AsynchConsumerProxyQueue, ScheduleQueue> inUseScheduleQueues = Collections.synchronizedMap(new HashMap<AsynchConsumerProxyQueue, ScheduleQueue>());
   
   private static AsynchConsumerThreadPool instance;

   static
   {
      try
      {
         MAX_THREADS = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(CLIENT_ASYNC_CONSUMER_THREADPOOL_MAX_SIZE_PROPERTY, Integer.toString(DEFAULT_MAX_THREADS)));
      }
      catch(NumberFormatException nfe)
      {
         //No FFDC code needed
         if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "NumberFormatException was thrown for custom property " + CLIENT_ASYNC_CONSUMER_THREADPOOL_MAX_SIZE_PROPERTY, nfe);
         MAX_THREADS = DEFAULT_MAX_THREADS;
      }
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Max async consumer threads: " + MAX_THREADS);

      instance = new AsynchConsumerThreadPool();
   }

   /**
    * Singleton pattern used obtaining the sole instance of this
    * class in existence.
    * @return A reference to the single AsynchConsumerThreadPool
    * class for this JVM.
    */
   public static AsynchConsumerThreadPool getInstance()
   {
      return instance;
   }

   enum StateEnum
   {
      IDLE_STATE, RUNNING_STATE, WAITING_STATE
   }

   /**
    * The schedule queue associates a queue of work with a thread.
    * When the queue is empty, it may not be associated with a
    * thread.  In this case, when work is added to an empty queue,
    * a thread will be taken from the WAS thread pool and used
    * to execute the work.  When the last piece of work is executed,
    * the thread is held for a few seconds before it is returned
    * to the WAS threadpool.  This behaviour attempts to avoid
    * contention on the threadpool.
    */
   private class ScheduleQueue implements Runnable
   {
      LinkedList<AsynchConsumerProxyQueue> work = new LinkedList<AsynchConsumerProxyQueue>();
      private StateEnum state = StateEnum.IDLE_STATE;
      ThreadPool threadPool;
      private String CLASS_NAME_SCHQ = ScheduleQueue.class.getName();

      public ScheduleQueue(ThreadPool threadPool)
      {
         this.threadPool = threadPool;
      }

      /**
       * Adds work to the schedule queue.  If no thread is servicing
       * the queue, one will be taken from the thread pool and used.
       * @param queue
       */
      public synchronized void addWork(AsynchConsumerProxyQueue queue)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ScheduleQueue.addWork", queue);
         work.addLast(queue);

         if (state != StateEnum.RUNNING_STATE)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State != RUNNING_STATE");

            // If the queue is not already being serviced by an
            // active thread - maybe it has a thread waiting idle
            if (state == StateEnum.WAITING_STATE)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State == WAITING_STATE");

               // Notify this thread that it should start to service
               // the queue again.
               state = StateEnum.RUNNING_STATE;
               notify();
            }
            else
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Servicing this queue");

               // There was no thread servicing this queue, and no
               // idle thread associated with the queue.  Take a new
               // thread from the threadpool and use it to service
               // the queue.
               try
               {
                  state = StateEnum.RUNNING_STATE;
                  threadPool.execute(this);
               }
               catch (InterruptedException e)
               {
                  // No FFDC Code Needed
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Interrupted", e);
               }
            }
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ScheduleQueue.addWork");
      }

      /**
       * Processes work added to the schedule queue.
       * @see Thread#run()
       */
      public void run()
      {
         try
         { 
        	 if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ScheduleQueue.run");

        	 // Dequeue the first piece of work.
        	 AsynchConsumerProxyQueue queue = null;
        	 synchronized(this)
        	 {
        		 queue = work.removeFirst();
        	 }

        	 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Processing queue: " + queue);

        	 boolean running = true;

        	 while(running)
        	 {
        		 // Process the work.
        		 try
        		 {
        			 queue.deliverMessages();
        		 }
        		 catch (RuntimeException re)
        		 {
        			 FFDCFilter.processException(re, 
        				CLASS_NAME_SCHQ + ".run", 
        				CommsConstants.ASYNC_CON_THREADPOOL_SCHQ_RUN_01, 
        				this);
        		 }
        		 synchronized(this)
        		 {
        			 // Take a lock on this queue to ensure no other thread
        			 // is changing its state.
        			 if (work.isEmpty())
        			 {
        				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Work queue is empty");

        				 // If there is nothing on the work queue then go into
        				 // waiting state where we wait a small amount of time
        				 // for more work to be given to us.
        				 state = StateEnum.WAITING_STATE;
        				 synchronized (inUseScheduleQueues) 
        				 {
        					 // Make sure we see the same view for the emptyStack and the inUseScheduleQueues Map
        					 emptyStack.push(this);     // Place ourself on the empty stack
        					 inUseScheduleQueues.remove(queue); //Remove this queue from the schedule queue
        				 }
        				 try
        				 {
        					 wait(10000);
        				 }
        				 catch (InterruptedException e)
        				 {
        					 // No FFDC Code Needed
        					 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Interrupted", e);
        				 }
        			 }
        			 else
        			 {
        				 //Remove this queue as we could potentially have new work from
        				 // a different proxy queue (i.e. we are round robining)
        				 inUseScheduleQueues.remove(queue);
        			 }

        			 // If another thread has given us more work, and woken us
        			 // up - then it will have set us into the running state.
        			 // Continue to process work.  Otherwise set ourselves into
        			 // idle state, so we quit looping and the thread is
        			 // repooled into the WAS thread pool.
        			 if (state == StateEnum.RUNNING_STATE)
        			 {
        				 queue = work.removeFirst();
        				 inUseScheduleQueues.put(queue, this);  //New work so the schedule queue is inuse again
        			 }
        			 else state = StateEnum.IDLE_STATE;

        			 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "State is now: " + state);

        			 running = state == StateEnum.RUNNING_STATE;
        		 }
        	 }
         }
         catch (RuntimeException re)
         {
   	      	FFDCFilter.processException(re, 
      	    	CLASS_NAME_SCHQ + ".run", 
      	    	CommsConstants.ASYNC_CON_THREADPOOL_SCHQ_RUN_02, 
      	    	this);
   	      	throw re;
         }
         catch (Error err)
         {
   	      	FFDCFilter.processException(err, 
      	    	CLASS_NAME_SCHQ + ".run", 
      	    	CommsConstants.ASYNC_CON_THREADPOOL_SCHQ_RUN_03, 
      	    	this);
   	      	throw err;
         }           
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ScheduleQueue.run");
      }
   }

   /**
    * A LIFO stack used to reference schedule queues that currently
    * have no work.  A LIFO structure is used, as it maximizes the
    * changes of obtaining a schedule queue with a thread still
    * associated.
    * @see ScheduleQueue
    */
   private static class EmptyScheduleQueueStack
   {
//      private int maxStackSize;
      private ScheduleQueue[] stackArray;
      private int currentStackSize = 0;

      private EmptyScheduleQueueStack(int maxStackSize)
      {
//         this.maxStackSize = maxStackSize;
         stackArray = new ScheduleQueue[maxStackSize];
      }

      /**
       * Pushes a queue onto the top of the stack.
       * @param queue The queue to push onto the stack.
       */
      public synchronized void push(ScheduleQueue queue)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "EmptyScheduleQueueStack.push", queue);

         // check not already on stack
         // begin D257554
         boolean alreadyOnStack = false;
         for (int i=0; i < currentStackSize; ++i)
         {
            alreadyOnStack |= stackArray[i] == queue;
         }
         if (!alreadyOnStack)
         {
            stackArray[currentStackSize] = queue;
            ++currentStackSize;
         }
         // end D257554

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "EmptyScheduleQueueStack.push");
      }

      /**
       * Pops a queue from the top of the stack.
       * @return The queue from the top of the stack or
       * null if the stack is empty.
       */
      public synchronized ScheduleQueue pop()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "EmptyScheduleQueueStack.pop");

         ScheduleQueue queue = null;
         if (currentStackSize != 0)
            queue = stackArray[--currentStackSize];

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "EmptyScheduleQueueStack.pop", queue);
         return queue;
      }
   }

   private AsynchConsumerThreadPool()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      ThreadPool threadPool = Framework.getInstance().getThreadPool(THREADPOOL_NAME, 0, MAX_THREADS);
      scheduleQueues = new ScheduleQueue[MAX_THREADS];
      emptyStack = new EmptyScheduleQueueStack(MAX_THREADS);

      for (int i = 0; i < MAX_THREADS; ++i)
      {
         scheduleQueues[i] = new ScheduleQueue(threadPool);
         emptyStack.push(scheduleQueues[i]);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Dispatches work into the threadpool.
    * @param queue The work to be dispatched.
    */

   //@Guardedby("this")
   private int roundRobinCounter = 0;

   public void dispatch(final AsynchConsumerProxyQueue queue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dispatch", queue);

      ScheduleQueue scheduleQueue;
      synchronized (inUseScheduleQueues)  //Lock when checking the inuseScheduleQueue 
      {
        if (inUseScheduleQueues.containsKey(queue))
        {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "We have a scheduleQueue already in use: "+queue+" "+inUseScheduleQueues);
           scheduleQueue = (ScheduleQueue)inUseScheduleQueues.get(queue);
        }
        else
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No ScheduleQueue servicing this queue, using emptyStack");
          // If not then find an empty queue
          // see if there is an empty schedule queue.
          scheduleQueue = emptyStack.pop();
          if (scheduleQueue != null)
          {
            // Only put the scheduleQueue in if we find one on the emptyStack
            inUseScheduleQueues.put(queue, scheduleQueue);
          }
        }
      }
      
      if (scheduleQueue == null)
      {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No ScheduleQueues on the emptyStack, round-robin on the running scheduleQueues");
          // There are currently no empty schedule queues, so
          // assign this work to a queue using a round robin
          // algorithm.
          synchronized (this) {
              scheduleQueue = scheduleQueues[roundRobinCounter];
              roundRobinCounter = (roundRobinCounter + 1) % MAX_THREADS;
              // No need to add this the inUseScheduleQueue map here as the fact that we are here means,
              //  scheduleQueue threads are running. When we add this work and the ScheduleQueue thread
              //  picks it up then it will be assigned then.
          }
      }
      scheduleQueue.addWork(queue);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dispatch");
   }
}
