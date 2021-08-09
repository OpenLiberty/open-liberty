/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.jfapchannel.impl.rldispatcher;

import java.util.ArrayList;
import java.util.List;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.ConversationReceiveListener;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.ReceiveListener;
import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.ConversationImpl;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Manages a pool of threads which dispatch receive listener callbacks.
 * The origional design for the JFAP Channel saw the receive listener
 * callbacks being invoked on the same Channel Framework thread that
 * received the data.  This, taken with other design decisions, introduced
 * a potential benchmark bottleneck.
 * <p>
 * During benchmarking it became apparent that transacted client operations
 * were running more slowley than they should be.  This turned out to be
 * because, for some configurations, the client code does not exploit all
 * available parallelism when commiting transactions.  The reason for this
 * is that the test case uses only a few clients each multiplexing lots of
 * conversations over the same underlying socket.  As the Channel Framework
 * (not to mention the underlying OS) serialise reading from the socket -
 * thus, in this case, we lose the opportunity for parallelism.
 * <p>
 * The chosen solution to this benchmark problem is to introduce a second
 * threadpool responsible for dispatching receive listener callbacks.  Thus
 * although data is read from a socket in a serial fashion - several different
 * threads may end up processing this data in parallel.
 * <p>
 * Although it might sound quite trivial to add a thread pool for dispatching
 * receive listener callbacks - there are a couple of gotcha's and design points
 * worth noting:
 * <ul>
 * <li> Care must be taken to dispatch callback invocations in the correct order.
 *      Although it is possible for multiple "conversations" to be multiplexed over
 *      the same socket - they may not be.  The implementation can not allow
 *      two invocations for the same "conversation" to either run concurrently
 *      or be invoked out of order (perhaps at the whim of the operating system's
 *      thread scheduler).
 * </li>
 * <li> In the same breath, it is acceptable for us to potentially dispatch data from
 *      the same conversation concurrently. This will occur at the instruction of the
 *      receive listener implementation before each invocation is dispatched to a
 *      thread. If the implementation wishes, it may return an instance of an object
 *      which implements <code>Dispatchable</code> and data will be dispatched to
 *      that instance. If it decides it does not wish to do this, it will return null
 *      indicating to us that normal rules still apply and the data is invoked behind
 *      any data for that conversation. The receive listener can indicate which thread
 *      to dispatch on through its implementation of the <code>getThreadContext</code>
 *      method.
 *      <p>
 *      Why would anyone want to do this? The reason is that transactions for example
 *      can span a connection and so it is important that any operations that involve
 *      the same transaction are executed in order on the same thread.
 * </li>
 * <li> It would be ill advised to continue to accept data ad-infinitum.  This
 *      was not as pronounced a problem with the old approach which contained
 *      a certain amount of self regulation -- the Channel Framework would choke
 *      as its Thread Pool was exhaused.  With the new implementation we keep a
 *      tally of the amound of un-despatched data and when this crosses a certain
 *      threashold start blocking requests to queue data.  This should provide
 *      a similar level of regulation.</li>
 * </ul>
 * <p>
 * The implementation of the receive listener dispatcher uses an array of
 * lists to retain information about and order the receive listener invocation
 * requests.  The size of this array matches the size of a thread pool.  Threads
 * from this pool service lists from the array.  There is a one-to-one
 * relationship between elements of the array of lists and the runnables
 * dispatched into the thread pool.
 * <p>
 * The algorithm for dispatching an invocation of a receive listener is as follows:
 * <ul>
 * <li> The conversation associated with the receive listener is tested to see if
 *      it has already been associated with a dispatch queue.  If it has - then it
 *      is dispatched to the same queue again (this preserves ordering of invocations
 *      on a per conversation basis).</li>
 * <li> If the conversation is not already associated with a dispatch queue then a
 *      list of empty dispatch queues is consulted to see if there is a queue which
 *      currently has no work.  If such a queue can be found then the conversation
 *      is associated with this queue and the invocation dispatched into it.</li>
 * <li> If no empty queue exists then a monotomically incrementing count in the
 *      conversation is divided modulus number of queues to determine which dispatch
 *      queue the conversation is associated with and its invocation dispatched
 *      into.</li>
 * </ul>
 * <p>
 * Some notes on <code>getThreadContext</code>:
 * <br>
 * This method can be implemented to produce a varying set of results as to the order in which
 * data is dispatched:
 * <ul>
 * <li> Returning null indicates that data should be dispatched in Conversation order. This
 *      is the safest method for guarenteing simple ordering.
 * </li>
 * <li> Returning an instance of an object that implements <code>Dispatchable</code> will
 *      cause dispatching by that instance.
 * </li>
 * <li> Returning an instance of <code>NonThreadSwitchingDispatchable.getInstance()</code>
 *      will cause the JFap channel to perform the invocation <strong>on the current
 *      thread</strong>. Note that this should be used with caution as this will block
 *      the channel framework thread while the invocation is invoked.
 * </li>
 * <li> Returning an instance of <code>DispatchToAllNonEmptyDispatchable.getInstance()</code>
 *      will cause the JFap channel to queue the invocation to all non empty dispatch
 *      queues. The last queue to dequeue the data will perform the invocation. Note that
 *      if all the queues are empty, JFap will invoke the invocation on the current thread.
 *      This should be used when you are unsure which dispatchable instance to dispatch by
 *      but the invocation should be invoked after all the currently received data has
 *      been invoked.
 * </li>
 * </ul>
 */
public final class ReceiveListenerDispatcher
{
   private static final TraceComponent tc = SibTr.register(ReceiveListenerDispatcher.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "08:47:51 @(#) 1.31");
   }

   // This class implements a singleton pattern - this field references
   // the single instance of this class which is created.
   private static ReceiveListenerDispatcher clientInstance;
   private static ReceiveListenerDispatcher serverInstance;
   private static ReceiveListenerDispatcher meInstance;

   /**
    * Default maximum number of receive listener callbacks which can be simultaneously invoked on the server side.
    */
   protected static final int DEFAULT_MAX_CONCURRENT_DISPATCHES_SERVER = 32;

   /**
    * Default maximum number of receive listener callbacks which can be simultaneously invoked on the client side.
    */
   protected static final int DEFAULT_MAX_CONCURRENT_DISPATCHES_CLIENT = 1;

   // Default for whether we use this new dispatching mechanism at all.
   private static final boolean DEFAULT_DISPATCHER_ENABLED = true;

   // Value for maximum number of receive listener callbacks which can
   // be simultaneously invoked.
   private int maxConcurrentDispatches = DEFAULT_MAX_CONCURRENT_DISPATCHES_SERVER;

   /**
    * Value for minimum number of receive listener callbacks which can simultaneously be invoked.
    * Default is 1.
    */
   private static final int MIN_CONCURRENT_DISPATCHES = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.MIN_CONCURRENT_DISPATCHES, "1"));

   /**
    * Value for keep alive time of RLD threadpool in milliseconds.
    * Default is 5 seconds (5000).
    */
   private static final long RLD_KEEP_ALIVE_TIME = Long.parseLong(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.RLD_KEEP_ALIVE_TIME, "5000"));

   // Value for whether we use this new dispatching mechanism at all.
   private boolean dispatcherEnabled = DEFAULT_DISPATCHER_ENABLED;

   // Thread pool that invocations are dispatched into.
   private ThreadPool threadPool;

   // An array of lists of invocation objects.  This is where invocations
   // pending dispatch into the thread pool are stored.
   private ReceiveListenerDispatchQueue[] dispatchQueues;

   // An list of dispatch queues which currently have no invocation objects
   // queued on to them.  When we want to start queueing invocation objects
   // for a session which is not already associated with a dispatch queue we
   // check this list to try and find an empty queue to associate it with.
   private List<ReceiveListenerDispatchQueue> emptyDispatchQueues;

   // Pool of data received invocation objects.
   private ObjectPool receiveListenerDataReceivedInvocationPool;
   private ObjectPool conversationReceiveListenerDataReceivedInvocationPool;
   private ObjectPool receiveListenerErrorOccurredInvocationPool;
   private ObjectPool conversationReceiveListenerErrorOccurredInvocationPool;

   /**
    * Create an instance of ReceiveListenerDispatcher. The value of the com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_DISABLED property will be
    * used to establish whether to back this instance by a thread pool or not.
    *
    * @param isClientSide true if this ReceiveListenerDispatcher is for use on the client side of a client-me connection.
    * @param isMEClient true if this is ReceiveListenerDispatcher is for a ME-Client connection
    */
   private ReceiveListenerDispatcher(final boolean isClientSide, final boolean isMEClient)
   {
      this(RuntimeInfo.getProperty("com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_DISABLED") == null, isClientSide, isMEClient);
   }

   /**
    * Create an instance of ReceiveListenerDispatcher.
    *
    * @param dispatcherEnabled true if backing this ReceiveListenerDispatcher by a thread pool, false otherwise.
    * @param isClientSide true if this ReceiveListenerDispatcher is for use on the client side of a client-me connection.
    * @param isMEClient true if this is ReceiveListenerDispatcher is for a ME-Client connection
    */
   private ReceiveListenerDispatcher(final boolean dispatcherEnabled, final boolean isClientSide, final boolean isMEClient)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[]{Boolean.valueOf(dispatcherEnabled), Boolean.valueOf(isClientSide)});

      this.dispatcherEnabled = dispatcherEnabled;

      if (dispatcherEnabled)
      {
         if(isClientSide)
         {
            maxConcurrentDispatches = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.MAX_CONCURRENT_DISPATCHES_CLIENT, "" + DEFAULT_MAX_CONCURRENT_DISPATCHES_CLIENT));
         }
         else
         {
            maxConcurrentDispatches = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.MAX_CONCURRENT_DISPATCHES, "" + DEFAULT_MAX_CONCURRENT_DISPATCHES_SERVER));
         }

         //Don't bother tracing out values from properties as they will get traced out by methods
         threadPool = Framework.getInstance().getThreadPool("JS-ReceiveListenerDispatcher", MIN_CONCURRENT_DISPATCHES, maxConcurrentDispatches);
         threadPool.setRequestBufferSize(maxConcurrentDispatches);
         threadPool.setKeepAliveTime(RLD_KEEP_ALIVE_TIME);
         dispatchQueues = new ReceiveListenerDispatchQueue[maxConcurrentDispatches];
         emptyDispatchQueues = new ArrayList<ReceiveListenerDispatchQueue>();
         for (int i=0; i < maxConcurrentDispatches; ++i) {
           dispatchQueues[i] = new ReceiveListenerDispatchQueue(emptyDispatchQueues, threadPool, isMEClient ? ReceiveListenerDispatchQueue.QueueType.ME_Client : ReceiveListenerDispatchQueue.QueueType.ME_ME);
         }
      }

      // Size of the object pools used for invocation objects.  This is a
      // expressed as a multiple of the maximum number of concurrent dispatches
      // as we anticipate the number of objects required will be proportional.
      // This may have been updated from the defaults if dispatcherEnabled is true.
      int dataReceivedInvocationPoolSize = 5 * maxConcurrentDispatches;
      int errorOccurredInvocationPoolSize = 5 * maxConcurrentDispatches;
      receiveListenerDataReceivedInvocationPool =
            new ObjectPool("JS data received invocation pool", dataReceivedInvocationPoolSize);
      receiveListenerErrorOccurredInvocationPool =
            new ObjectPool("JS error occurred invocation pool", errorOccurredInvocationPoolSize);
      conversationReceiveListenerDataReceivedInvocationPool =
            new ObjectPool("JS data received invocation pool", dataReceivedInvocationPoolSize);
      conversationReceiveListenerErrorOccurredInvocationPool =
            new ObjectPool("JS error occurred invocation pool", errorOccurredInvocationPoolSize);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /** Allocates a data received invocation object from an object pool */
   private ReceiveListenerDataReceivedInvocation allocateDataReceivedInvocation(Connection connection,
                                                                 ReceiveListener listener,
                                                                 WsByteBuffer data,
                                                                 int size,
                                                                 int segmentType,
                                                                 int requestNumber,
                                                                 int priority,
                                                                 boolean allocatedFromPool,
                                                                 boolean partOfExchange,
                                                                 Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateDataReceivedInvocation", new Object[]{connection, listener, data, ""+size, ""+segmentType, ""+requestNumber, ""+priority, ""+allocatedFromPool, ""+partOfExchange, conversation});
      ReceiveListenerDataReceivedInvocation retInvocation = (ReceiveListenerDataReceivedInvocation)receiveListenerDataReceivedInvocationPool.remove();
      if (retInvocation == null)
      {
         retInvocation = new ReceiveListenerDataReceivedInvocation(connection,
                                                                   listener,
                                                                   data,
                                                                   size,
                                                                   segmentType,
                                                                   requestNumber,
                                                                   priority,
                                                                   allocatedFromPool,
                                                                   partOfExchange,
                                                                   conversation,
                                                                   receiveListenerDataReceivedInvocationPool);
      }
      else
      {
         retInvocation.reset(connection,
                             listener,
                             data,
                             size,
                             segmentType,
                             requestNumber,
                             priority,
                             allocatedFromPool,
                             partOfExchange,
                             conversation);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateDataReceivedInvocation", retInvocation);
      return retInvocation;
   }

   /** Allocates a data received invocation object from an object pool */
   private ConversationReceiveListenerDataReceivedInvocation allocateDataReceivedInvocation(Connection connection,
                                                                 ConversationReceiveListener listener,
                                                                 WsByteBuffer data,
                                                                 int size,
                                                                 int segmentType,
                                                                 int requestNumber,
                                                                 int priority,
                                                                 boolean allocatedFromPool,
                                                                 boolean partOfExchange,
                                                                 Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateDataReceivedInvocation", new Object[]{connection, listener, data, ""+size, ""+segmentType, ""+requestNumber, ""+priority, ""+allocatedFromPool, ""+partOfExchange, conversation});
      ConversationReceiveListenerDataReceivedInvocation retInvocation = (ConversationReceiveListenerDataReceivedInvocation)conversationReceiveListenerDataReceivedInvocationPool.remove();
      if (retInvocation == null)
      {
         retInvocation = new ConversationReceiveListenerDataReceivedInvocation(connection,
                                                                               listener,
                                                                               data,
                                                                               size,
                                                                               segmentType,
                                                                               requestNumber,
                                                                               priority,
                                                                               allocatedFromPool,
                                                                               partOfExchange,
                                                                               conversation,
                                                                               conversationReceiveListenerDataReceivedInvocationPool);
      }
      else
      {
         retInvocation.reset(connection,
                             listener,
                             data,
                             size,
                             segmentType,
                             requestNumber,
                             priority,
                             allocatedFromPool,
                             partOfExchange,
                             conversation);
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateDataReceivedInvocation", retInvocation);
      return retInvocation;
   }

   /** Allocates an error occurred object from an object pool. */
   private ReceiveListenerErrorOccurredInvocation allocateErrorOccurredInvocation(Connection connection,
                                                                   ReceiveListener listener,
                                                                   SIConnectionLostException exception,
                                                                   int segmentType,
                                                                   int requestNumber,
                                                                   int priority,
                                                                   Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateErrorOccurredInvocation", new Object[] {connection, listener, exception, ""+segmentType, ""+requestNumber, ""+priority, conversation});
      ReceiveListenerErrorOccurredInvocation retInvocation = (ReceiveListenerErrorOccurredInvocation)receiveListenerErrorOccurredInvocationPool.remove();
      if (retInvocation == null)
      {
         retInvocation = new ReceiveListenerErrorOccurredInvocation(connection,
                                                                    listener,
                                                                    exception,
                                                                    segmentType,
                                                                    requestNumber,
                                                                    priority,
                                                                    conversation,
                                                                    receiveListenerErrorOccurredInvocationPool);
      }
      else
      {
         retInvocation.reset(connection,
                             listener,
                             exception,
                             segmentType,
                             requestNumber,
                             priority,
                             conversation);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateErrorOccurredInvocation", retInvocation);
      return retInvocation;
   }

   /** Allocates an error occurred object from an object pool. */
   private ConversationReceiveListenerErrorOccurredInvocation allocateErrorOccurredInvocation(Connection connection,
                                                                   ConversationReceiveListener listener,
                                                                   SIConnectionLostException exception,
                                                                   int segmentType,
                                                                   int requestNumber,
                                                                   int priority,
                                                                   Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "allocateErrorOccurredInvocation", new Object[] {connection, listener, exception, ""+segmentType, ""+requestNumber, ""+priority, conversation});
      ConversationReceiveListenerErrorOccurredInvocation retInvocation = (ConversationReceiveListenerErrorOccurredInvocation)conversationReceiveListenerErrorOccurredInvocationPool.remove();
      if (retInvocation == null)
      {
         retInvocation = new ConversationReceiveListenerErrorOccurredInvocation(connection,
                                                                                listener,
                                                                                exception,
                                                                                segmentType,
                                                                                requestNumber,
                                                                                priority,
                                                                                conversation,
                                                                                conversationReceiveListenerErrorOccurredInvocationPool);
      }
      else
      {
         retInvocation.reset(connection,
                             listener,
                             exception,
                             segmentType,
                             requestNumber,
                             priority,
                             conversation);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "allocateDataReceivedInvocation", retInvocation);
      return retInvocation;
   }


   /**
    * Queues any type of invocation entry into an appropriate queue.  The queue is selected first
    * by determining if a requests associated conversation is already associated with a queue.  If
    * it is not then a list of empty queues is consulted before finally reverting to using a
    * the modulus of a monatomically incrementing counter (associated with conversation instance).
    * <p>
    * Note: this method contains some reasonably complex synchronization.  One underlying assumption
    * in this is that it cannot be invoked concurrently to queue an invocation for the same conversation.
    * This pre-condition is kept by virtue of the fact that the channel framework will not concurrently
    * read data from a single socket - and this method must be executed prior to making another read
    * request.
    * @param invocation
    * @param conversation
    */
   private void queueInvocationCommon(AbstractInvocation invocation, ConversationImpl conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueInvocationCommon", new Object[] {invocation, conversation});
      if (dispatcherEnabled)
      {
         // Start F201521

         try                                                                              // D202636
         {                                                                                // D202636
            // First job is to ask the ConversationReceiveListener for the object which may contain
            // a dispatch queue. If they return null here, we assume that we should use the dispatch
            // queue held in the Conversation.
            // Note this method may throw a RuntimeException indicating that there was a problem
            // executing this method. If this occurs, we don't want to dispatch this segment as
            // the conversation will be closed.
            Dispatchable dispatchable = invocation.getThreadContext();

            // If this was null, then use the Conversation as the Dispatchable object
            if (dispatchable == null) dispatchable = conversation;

            // Save this in the invocation
            invocation.setDispatchable(dispatchable);

            // Update the count in the Conversation of how many requests are outstanding on
            // this Conversation. This is needed when we are processing errorOccurred
            // notifications as we want to ensure that errorOccurred callbacks come after
            // all the data has been processed for that Conversation.
            synchronized (conversation.getTotalOutstandingRequestCountLock())
            {
               conversation.incrementTotalOutstandingCount();
            }

            ReceiveListenerDispatchQueue dispatchQueue = null;
            Object dispatchableLock = dispatchable.getDispatchLockObject();

            // Take a lock on the dispatchable to determin its dispatch queue.  This is
            // required to exclude other threads whilst we check if there is an associated
            // dispatch queue and if there is - increment its reference count.  We can drop
            // out of the synchronize block once the reference count is increased as other
            // threads will not disassociate a dispatch queue with a non-zero reference count.
            synchronized(dispatchableLock)
            {
               dispatchQueue = (ReceiveListenerDispatchQueue) dispatchable.getDispatchQueue();
               if (dispatchQueue != null)
                  dispatchable.incrementDispatchQueueRefCount();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "queueInvocationCommon", "dispatchQueue="+dispatchQueue);
            // End F201521

            if (dispatchQueue == null)
            {
               // The dispatchable was not already associated with a dispatch queue, so try
               // finding a suitable queue in the list of empty queues.  We synchronize on the
               // empty queues list to prevent corruption of the data structure by concurrent
               // access.  We also want to prevent anyone else from adding or removing queues
               // from the list until we have either removed a queue of our own or chosen a
               // queue to dispatch into.
               synchronized(emptyDispatchQueues)
               {
                  if (!emptyDispatchQueues.isEmpty())
                     dispatchQueue = emptyDispatchQueues.remove(emptyDispatchQueues.size()-1);  // D217401
                  else
                  {
                     // There was no empty queues so choose a queue by taking the modulus of
                     // the monotomically incrementing counter associated with the conversation.
                     // We must do this inside a block synchronized on the empty dispatch queue
                     // to prevent a race condition where someone could be adding the queue we
                     // decide upon into the empty queues list.
                     int queueNumber = conversation.getInstanceCounterValue();
                     dispatchQueue = dispatchQueues[queueNumber % maxConcurrentDispatches];
                  }
               }
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatchQueue="+dispatchQueue);

               // Now we have decided upon a dispatch queue - associate it with the
               // dispatchable and ensure it has a non-zero reference count.  This is
               // done inside a synchronize block to ensure the change is kept consistent
               // across threads accessing the dispatchable.
               synchronized(dispatchableLock)                                                // F201521
               {
                  dispatchable.setDispatchQueue(dispatchQueue);                              // F201521
                  dispatchable.incrementDispatchQueueRefCount();                             // F201521
               }
            }

            //Reset invocation prior to enqueue to prevent its reference count getting messed up. 
            invocation.resetReferenceCounts();
            
            // Finally enqueue the work to our chosen dispatch queue
            dispatchQueue.enqueue(invocation);
         }
         catch (RuntimeException e)
         {
            // No FFDC code needed
            // Note the connection has already been blown away by this point - so no further action
            // needs to be taken.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Looks like getThreadContext failed:", e);
         }
         // Start D213108
         // It's important that we carry on here and do not barf back up to the TCP channel
         // as we do not want to knacker any threads that will service user requests
         catch (Throwable t)
         {
            FFDCFilter.processException(t, "com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerDispatcher",
                                        JFapChannelConstants.RLDISPATCHER_QUEUEINVOCCOMMON_01, this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "RL Dispatcher threw an exception: ", t);
         }
         // End D213108
         // End D202636
      }
      else // dispatcherEnabled == false
      {
         // If we are not using the new dispatching code then just invoke
         // the appropriate method on this thread.
         invocation.invoke();
         invocation.repool();
         invocation.resetReferenceCounts();                                               // D213108
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueInvocationCommon");
   }

   /** Queues the invocation of a data received method into the dispatcher. */
   public void queueDataReceivedInvocation(Connection connection,
                                              ReceiveListener listener,
                                              WsByteBuffer data,
                                              int segmentType,
                                              int requestNumber,
                                              int priority,
                                              boolean allocatedFromBufferPool,
                                              boolean partOfExchange,
                                              Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueDataReceivedInvocation", new Object[] {connection, listener, data, ""+segmentType, ""+requestNumber, ""+priority, ""+allocatedFromBufferPool, ""+partOfExchange, conversation});

      int dataSize = 0;
      if (dispatcherEnabled)
         dataSize = data.position();                                                                                                      // D240062

      AbstractInvocation invocation =
         allocateDataReceivedInvocation(connection,
                                        listener,
                                        data,
                                        dataSize,
                                        segmentType,
                                        requestNumber,
                                        priority,
                                        allocatedFromBufferPool,
                                        partOfExchange,
                                        conversation);

      queueInvocationCommon(invocation, (ConversationImpl)conversation);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueDataReceivedInvocation");
   }

   /** Queues the invocation of a data received method into the dispatcher. */
   public void queueDataReceivedInvocation(Connection connection,
                                              ConversationReceiveListener listener,
                                              WsByteBuffer data,
                                              int segmentType,
                                              int requestNumber,
                                              int priority,
                                              boolean allocatedFromBufferPool,
                                              boolean partOfExchange,
                                              Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueDataReceivedInvocation", new Object[] {connection, listener, data, ""+segmentType, ""+requestNumber, ""+priority, ""+allocatedFromBufferPool, ""+partOfExchange, conversation});

      int dataSize = 0;
      if (dispatcherEnabled)
         dataSize = data.position();                                                                                                      // D240062

      AbstractInvocation invocation =
         allocateDataReceivedInvocation(connection,
                                        listener,
                                        data,
                                        dataSize,
                                        segmentType,
                                        requestNumber,
                                        priority,
                                        allocatedFromBufferPool,
                                        partOfExchange,
                                        conversation);

      queueInvocationCommon(invocation, (ConversationImpl)conversation);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueDataReceivedInvocation");
   }

   /** Queues the invocation of an error occurred method into the dispatcher. */
   public void queueErrorOccurredInvocation(Connection connection,
                                               ReceiveListener listener,
                                               SIConnectionLostException exception,
                                               int segmentType,
                                               int requestNumber,
                                               int priority,
                                               Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueErrorOccurredInvocation", new Object[] {connection, listener, exception, ""+segmentType, ""+requestNumber, ""+priority, conversation});

      AbstractInvocation invocation =
         allocateErrorOccurredInvocation(connection,
                                         listener,
                                         exception,
                                         segmentType,
                                         requestNumber,
                                         priority,
                                         conversation);
      queueInvocationCommon(invocation, (ConversationImpl)conversation);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueErrorOccurredInvocation");
   }

   /** Queues the invocation of an error occurred method into the dispatcher. */
   public void queueErrorOccurredInvocation(Connection connection,
                                               ConversationReceiveListener listener,
                                               SIConnectionLostException exception,
                                               int segmentType,
                                               int requestNumber,
                                               int priority,
                                               Conversation conversation)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueErrorOccurredInvocation", new Object[] {connection, listener, exception, ""+segmentType, ""+requestNumber, ""+priority, conversation});

      AbstractInvocation invocation =
         allocateErrorOccurredInvocation(connection,
                                         listener,
                                         exception,
                                         segmentType,
                                         requestNumber,
                                         priority,
                                         conversation);

      // Start F201521
      // We don't always want to queue this as it could end up on some random thread.
      // As such, ask the conversation how many things it has left to process - if that
      // is 0, then queue it - otherwise, save it in the Conversation so that the last
      // thread working on that Conversatioon will invoke it.
      ConversationImpl convImpl = (ConversationImpl) conversation;

      int conversationRequestCount = 0;
      // Start D262285
      boolean queueIt = true;
      synchronized (convImpl.getTotalOutstandingRequestCountLock())
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queueing a errorOccurred invocation for ", conversation);
         conversationRequestCount = convImpl.getTotalOutstandingRequestCount();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Request count is: " + conversationRequestCount);

         if (conversationRequestCount != 0)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Count != 0, saving");
            convImpl.setErrorOccurredInvocation(invocation);
            queueIt = false;
         }
      }

      if (queueIt)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Count = 0, queueing");
         queueInvocationCommon(invocation, convImpl);
      }
      // End D262285
      // End F201521

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueErrorOccurredInvocation");
   }

   // Start D213108
   /**
    * This method returns the array of all the dispatch queues.
    * <p>
    * <strong>Use this method with care!</strong>
    *
    * @return Returns an array of all of the dispatch queues.
    */
   protected ReceiveListenerDispatchQueue[] getDispatchQueues()
   {
      return dispatchQueues;
   }
   // End D213108

   // Start D242116
   /**
    * Return a reference to the instance of this class.  Used to implement the
    * singleton design pattern.
    * @return ReceiveListenerDispatcher
    */
   public static ReceiveListenerDispatcher getInstance(Conversation.ConversationType convType, boolean isOnClientSide)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getInstance",
                                           new Object[] {""+convType, ""+isOnClientSide});

      ReceiveListenerDispatcher retInstance;

      // A conversation has type CLIENT if it is an Outbound or Inbound connection from a JetStream
      // client. The client side RLD is different from the server side RLD, so we need the
      // isOnClientSide flag to distinguish them.
      // A conversation has type ME if it is an Outbound or Inbound connection from another ME.
      if (convType == Conversation.CLIENT)
      {
         if (isOnClientSide)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Returning client instance");
            synchronized(ReceiveListenerDispatcher.class)
            {
               if (clientInstance == null)
               {
                  clientInstance = new ReceiveListenerDispatcher(true, true); // Client side of ME-Client conversation
               }
            }
            retInstance = clientInstance;
         }
         else
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Returning server instance");
            synchronized(ReceiveListenerDispatcher.class)
            {
               if (serverInstance == null)
               {
                  serverInstance = new ReceiveListenerDispatcher(false, true); // ME side of ME-Client conversation
               }
            }
            retInstance = serverInstance;
         }
      }
      else
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Returning ME-ME instance");
         synchronized(ReceiveListenerDispatcher.class)
         {
            if (meInstance == null)
            {
               meInstance = new ReceiveListenerDispatcher(false, false); // ME-ME conversation
            }
         }
         retInstance = meInstance;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getInstance", retInstance);
      return retInstance;
   }
   // End D242116
}
