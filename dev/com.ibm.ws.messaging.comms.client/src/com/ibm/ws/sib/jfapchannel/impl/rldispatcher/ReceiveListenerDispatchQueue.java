/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.rldispatcher;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.Connection;
import com.ibm.ws.sib.jfapchannel.impl.ConversationImpl;
import com.ibm.ws.sib.jfapchannel.threadpool.ThreadPool;
import com.ibm.ws.sib.utils.Runtime;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/*
 * Queue used to hold invocation requests prior to them being processed. This class wrappers a
 * list to provide a FIFO queue with size control to pace arriving msgs.
 */

//@ThreadSafe
public class ReceiveListenerDispatchQueue implements DispatchQueue, Runnable {
  private static final TraceComponent tc = SibTr.register(ReceiveListenerDispatchQueue.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

 
  // The following properties are supported by this class
  //
  // com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE          - Maximum ME-ME & ME-Client queue size (maintained for backward compatibility)
  // com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE_ME       - Maximum ME-ME queue size in bytes (overrides RL_DISPATCHER_MAXQUEUESIZE)
  // com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE_CLIENT   - Maximum ME-Client queue size (overrides RL_DISPATCHER_MAXQUEUESIZE)
  // com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUEMSGS_ME       - Maximum ME-ME queue size in msgs
  // com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUEMSGS_CLIENT   - Maximum ME-Client queue in msgs
  // com.ibm.ws.sib.jfapchannel.MAX_CONCURRENT_DISPATCHES           - Number of dispatch queues per ReceiveListenerDispatcher instance
  // JFapChannelConstants.RLD_REPOOL_THREAD_DELAY_PROPERTY          - Time (ms) a thread delays before repooling itself when queue becomes empty

  static final String MAXQUEUESIZE = "com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE";
  static final String MAXQUEUESIZE_ME = "com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE_ME";
  static final String MAXQUEUEMSGS_ME = "com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUEMSGS_ME";
  static final String MAXQUEUESIZE_CLIENT = "com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUESIZE_CLIENT";
  static final String MAXQUEUEMSGS_CLIENT = "com.ibm.ws.sib.jfapchannel.RL_DISPATCHER_MAXQUEUEMSGS_CLIENT";
  static final String MAX_CONCURRENT_DISPATCHES = "com.ibm.ws.sib.jfapchannel.MAX_CONCURRENT_DISPATCHES";

  // The maximum amount of data, in bytes, which will be stored in the queue after which the
  // queue is blocked to pace throughput. The maximum queue size allowed depends on the type
  // of the connection.

  private static int MAX_ME_QUEUE_SIZE; // Max ME-ME queue size in bytes allowed
  private static int MAX_ME_QUEUE_MSGS; // Max ME-ME queue size in msgs allowed
  private static int MAX_CLIENT_QUEUE_SIZE; // Max ME-Client queue size in bytes allowed
  private static int MAX_CLIENT_QUEUE_MSGS; // Max ME-Client queue size in msgs allowed

  private static int rldThreadRepoolDelay; // Thread repooling delay when queue becomes empty

  static {
    
    // Set up the maximum queue sizes

    try {
      final int numberOfQueues = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(MAX_CONCURRENT_DISPATCHES, ""+ReceiveListenerDispatcher.DEFAULT_MAX_CONCURRENT_DISPATCHES_SERVER));
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Number of Receive Listener Dispatch queues = " + numberOfQueues);

      // Calculate and set suitable default maxmimum queue size values. For ME-Client connections we
      // use a total size for all queues of 20% of currently unused heap space - the 20% size is
      // divided equally between the number of queues. ME-ME connections use a fixed size default as
      // a smaller size helps avoid timeouts, nacks and unnecessary resends between MEs. In addition
      // ME-ME connections limit the number of queued msgs. As many small control msgs can flow ME-ME
      // pacing just on size is not always satisfactory so we also limit on msg number queued also in
      // order not to allow too many msgs to become queued between MEs.

      final java.lang.Runtime rt = java.lang.Runtime.getRuntime();
      final long maxMemory = rt.maxMemory();
      final long totalMemory = rt.totalMemory();
      final long freeMemory = rt.freeMemory();
      final long available = maxMemory - totalMemory + freeMemory;
      final long twentyPercent = (long) (available * 0.20);
      MAX_CLIENT_QUEUE_SIZE = (int) (twentyPercent / numberOfQueues);

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        final NumberFormat f = NumberFormat.getInstance();
        SibTr.debug(tc, "Max memory   : " + f.format(maxMemory));
        SibTr.debug(tc, "Total memory : " + f.format(totalMemory));
        SibTr.debug(tc, "Free memory  : " + f.format(freeMemory));
        SibTr.debug(tc, "Available    : " + f.format(available));
        SibTr.debug(tc, "20%          : " + f.format(twentyPercent));
        SibTr.debug(tc, "Default ME-Client queue size : " + MAX_CLIENT_QUEUE_SIZE);
      }

      MAX_CLIENT_QUEUE_MSGS = 0; // 0 means do not use
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Default ME-Client queue msgs : " + MAX_CLIENT_QUEUE_MSGS);

      MAX_ME_QUEUE_SIZE = 16 * 1024; // ME-ME queues need to be small otherwise we snarl up because of timeouts, nacks & resends
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Default ME-ME queue size :" + MAX_ME_QUEUE_SIZE);

      MAX_ME_QUEUE_MSGS = 96; // ME-ME queues want to buffer a small number of msgs only
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Default ME-ME queue msgs : " + MAX_ME_QUEUE_MSGS);

      // Next look for the legacy custom property which sets the maximum queue size for both ME-ME & ME-Client queues

      final String mqs = RuntimeInfo.getProperty(MAXQUEUESIZE);
      if (mqs != null) {
        Runtime.changedPropertyValue(MAXQUEUESIZE, mqs);
        MAX_ME_QUEUE_SIZE = MAX_CLIENT_QUEUE_SIZE = Integer.parseInt(mqs);
      }

      // Next look for connection specific custom properties which set the maximum queue size for a specific type of queue

      final String mqs_client = RuntimeInfo.getProperty(MAXQUEUESIZE_CLIENT);
      if (mqs_client != null) {
        Runtime.changedPropertyValue(MAXQUEUESIZE_CLIENT, mqs_client);
        MAX_CLIENT_QUEUE_SIZE = Integer.parseInt(mqs_client);
      }

      final String mqm_client = RuntimeInfo.getProperty(MAXQUEUEMSGS_CLIENT);
      if (mqm_client != null) {
        Runtime.changedPropertyValue(MAXQUEUEMSGS_CLIENT, mqm_client);
        MAX_CLIENT_QUEUE_MSGS = Integer.parseInt(mqm_client);
      }

      final String mqs_me = RuntimeInfo.getProperty(MAXQUEUESIZE_ME);
      if (mqs_me != null) {
        Runtime.changedPropertyValue(MAXQUEUESIZE_ME, mqs_me);
        MAX_ME_QUEUE_SIZE = Integer.parseInt(mqs_me);
      }

      final String mqm_me = RuntimeInfo.getProperty(MAXQUEUEMSGS_ME);
      if (mqm_me != null) {
        Runtime.changedPropertyValue(MAXQUEUEMSGS_ME, mqm_me);
        MAX_ME_QUEUE_MSGS = Integer.parseInt(mqm_me);
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Max ME-Client queue size: " + MAX_CLIENT_QUEUE_SIZE + " max msgs: "+ MAX_CLIENT_QUEUE_MSGS);
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Max ME-ME queue size: " + MAX_ME_QUEUE_SIZE + " max msgs: "+ MAX_ME_QUEUE_MSGS);
    } catch (NumberFormatException e) {
      // No FFDC Code Needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.exception(tc, e);
    }

    try {
      rldThreadRepoolDelay = Integer.parseInt(RuntimeInfo.getPropertyWithMsg(JFapChannelConstants.RLD_REPOOL_THREAD_DELAY_PROPERTY,""+JFapChannelConstants.RLD_REPOOL_THREAD_DELAY_DEFAULT));
    } catch(NumberFormatException e) {
      // No FFDC Code Needed
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.exception(tc, e);
    }
  }

  // Where more than one synchronized lock is taken which includes "this" the lock hierarchy for this class is:
  //
  // i) this
  // ii) barrier or other object

  // The underlying queue implementation - queue is itself not thread safe so synchronise on "barrier" when accessing the queue

  //GuardedBy("barrier")
  private final ArrayList<AbstractInvocation> queue = new ArrayList<AbstractInvocation>();

  // The pacing barrier. When a queue becomes full (either for bytes or msgs) the barrier is locked, all further
  // attempts to pass the barrier then block until the barrier is unlocked. The barrier is unlocked when both
  // the bytes and msgs are below their maximum.

  //GuardedBy("barrier")
  private final ReceiveListenerDispatchBarrier barrier = new ReceiveListenerDispatchBarrier();

  // List of empty queues to which this queue should add itself when empty

  //GuardedBy("emptyDispatchQueues")
  private final List<ReceiveListenerDispatchQueue> emptyDispatchQueues;

  // Pool from which processing threads are started

  private final ThreadPool threadPool;

  //GuardedBy("barrier")
  private int queueSize = 0; // The amount of data currently in this queue

  // Enumerator representing the queue type

  public enum QueueType {
    ME_ME,
    ME_Client
  }

  // Type of this queue

  private final QueueType queueType;
  private final int maxQueueSize; // Maximum allowed queue size in bytes
  private final int maxQueueMsgs; // Maximum allowed queue size in msgs (0 means no maximum)

  // Thread management variables

  private volatile boolean running = false; // Indicates that a thread is running against this queue

  // Constructor

  protected ReceiveListenerDispatchQueue (final List<ReceiveListenerDispatchQueue> edq, final ThreadPool tp, final QueueType qt) {

    // Perform a bit of jiggery pokery to ensure we don't get ConcurrentModificationException building a String for tracing
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
      final String edqString;

      if (edq != null) {
        synchronized (edq) {
          edqString = edq.toString();
        }
      } else edqString = null;

      SibTr.entry(this, tc, "<init>", "emptyDispatchQueues="+edqString+", threadPool="+tp+", queueType="+qt);
    }

    emptyDispatchQueues = edq;
    threadPool = tp;
    queueType = qt;

    if (queueType == QueueType.ME_ME) {
      maxQueueSize = MAX_ME_QUEUE_SIZE;
      maxQueueMsgs = MAX_ME_QUEUE_MSGS;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue type is ME-ME maxQueueSize="+maxQueueSize+", maxQueueMsgs="+maxQueueMsgs);
    } else {
      maxQueueSize = MAX_CLIENT_QUEUE_SIZE;
      maxQueueMsgs = MAX_CLIENT_QUEUE_MSGS;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Queue type is ME-Client maxQueueSize="+maxQueueSize+", maxQueueMsgs="+maxQueueMsgs);
    }

    // Since this queue is curently empty add ourselves to the empty dispatch queue list

    if (emptyDispatchQueues != null) {
      synchronized (emptyDispatchQueues) {
        emptyDispatchQueues.add(this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Added this queue (" + this + ") to empty dispatch list ("+emptyDispatchQueues+")");
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  // Enqueue an invocation by adding it to the end of the queue

  protected void enqueue (final AbstractInvocation invocation) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "enqueue", invocation);

    barrier.pass(); // Block until allowed to pass
  
    // We need to ensure that the thread processing this queue does not change the "running" state before we decide whether a new
    // thread needs to be started or not so synchronize on "this"

    synchronized (this) {
      final boolean isEmpty = isEmpty(); // Remember whether queue is currently empty or not

      // Add the invocation to the queue

      synchronized (barrier) {
        queue.add(invocation);

        queueSize += invocation.getSize();

        // Check to see if the queue limits have been exceeded and the queue should now be locked

        if (queueSize >= maxQueueSize || (maxQueueMsgs > 0 && queue.size() >= maxQueueMsgs)) {
          barrier.lock();
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Locked the barrier: bytes="+queueSize+" ("+maxQueueSize+") msgs="+queue.size()+" ("+maxQueueMsgs+")");
        } else {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Leaving barrier unlocked: bytes="+queueSize+" ("+maxQueueSize+") msgs="+queue.size()+" ("+maxQueueMsgs+")");
        }
      }

      // If queue was previously empty we need to either prompt the existing processing thread or start a new thread to
      // process the new invocation request

      if (isEmpty) {
        if (running) { // A processing thread already exists for this queue so wake it up
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Notifying existing thread");
          notify();
        } else { // We need to start a new processing thread
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Starting a new thread");
          boolean interrupted = true;
          while (interrupted) {
            try {
              threadPool.execute(this);
              interrupted = false;
            } catch (InterruptedException e) {
              // No FFDC code needed
            }
          }
        }
      }
    } // synchronized (this)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "enqueue");
  }

  // Dequeue an invocation by removing it from the front of the queue

  private AbstractInvocation dequeue() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dequeue");

    AbstractInvocation invocation;

    synchronized (barrier) {
      invocation = queue.remove(0);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dequeue", invocation);
    return invocation;
  }

  /**
   * Unlock the barrier.
   *
   * @param size from last AbstractInvocation
   * @param conversationType from last AbstractInvocation
   */
  private void unlockBarrier(final int size, final Conversation.ConversationType conversationType)
  {
     if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "unlockBarrier", new Object[]{Integer.valueOf(size), conversationType});

     synchronized(barrier)
     {
       queueSize -= size;

       // If queue size is now below the maximum size for both data in bytes and number of msgs unlock the barrier (if not already unlocked)

       if(queueSize < maxQueueSize && (maxQueueMsgs == 0 || (maxQueueMsgs > 0 && queue.size() < maxQueueMsgs)))
       {
         if(barrier.unlock())
         {
           if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unlocked the barrier: bytes="+queueSize+" ("+maxQueueSize+") msgs="+queue.size()+" ("+maxQueueMsgs+")");
         }
         else
         {
           if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Barrier already unlocked: bytes="+queueSize+" ("+maxQueueSize+") msgs="+queue.size()+" ("+maxQueueMsgs+")");
         }
       }
       else
       {
         if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Leaving barrier locked: bytes="+queueSize+" ("+maxQueueSize+") msgs="+queue.size()+" ("+maxQueueMsgs+")");
       }
     }

     if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "unlockBarrier");
   }

  // Return true if the queue is empty

  protected boolean isEmpty () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isEmpty");

    boolean rc;

    synchronized (barrier) {
      rc = queue.isEmpty();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isEmpty", rc);
    return rc;
  }

  // Return the depth of the queue

  protected int getDepth() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getDepth");

    int depth;

    synchronized (barrier) {
       depth = queue.size();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getDepth", depth);
    return depth;
  }

  /**
   * @return the first AbstractInvocation from the queue without removing it.
   */
  private AbstractInvocation head()
  {
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "head");

    final AbstractInvocation rc;

    synchronized (barrier)
    {
      rc = queue.get(0);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "head", rc);
    return rc;
  }

  // Entry point for thread spun up to process this dispatch queue
  public void run () {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "run");

    //A thread is now processing the queue.
    running = true;

    /*
     *Can't use the running flag here as can allow multiple threads to be running at the same time. For example:
     *
     *Thread 1 is performing run processing, there is nothing left on the queue so it sets running to false.
     *Another thread calls enqueue, running is false so another thread, thread 2 is started.
     *Thread 2 enters the run method and sets running to true.
     *Thread 1 loops round, reads the running flag which is now true so carries on processing. We now get two threads in the run method.
     */
    boolean continueProcessing = true;

    while (continueProcessing) { // Thread should still run
      try {
        //Get invocation but don't dequeue it.
        //Only perform dequeue once invocation has been invoked otherwise a race condition can arise with threads
        //that are dispatching to all non-empty dispatch queues. See defect 538065 for more details.
        final AbstractInvocation invocation = head();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "invocation=" + invocation);

        final Dispatchable dispatchable = invocation.getDispatchable();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "dispatchable="+ dispatchable);

        //Whether the invocation should be repooled after it has been invoked.
        boolean repoolInvocation = false;

        //Need to be got when holding lock on invocation.
        final int size;
        final Conversation.ConversationType conversationType;

        // Synchronize on the item as it may have been dispatched to all queues, in
        // which case we do not want to risk corrupting its reference count.
        synchronized (invocation) {
          invocation.decrementReferenceCount();

          // If this this the last thread to 'see' the invocation - and it is ready (ie. the thread that
          // queued it is no longer considering invoking it) then invoke the method represented by the
          // invocation object.
          if (invocation.getReferenceCount() == 0 && invocation.isReady()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invoking: "+ invocation);

            invocation.invoke();

            final ConversationImpl convImpl = (ConversationImpl)invocation.getConversation();
            synchronized (convImpl.getTotalOutstandingRequestCountLock()) {
              convImpl.decrementTotalOutstandingCount();

              // Check to see if we have processed all outstanding invocation requests
              // for the conversation.  If so, deliver any errors to the SIConnections
              // event listener.  This prevents error delivery preceeding responses to
              // API calls that might have generated the error.
              if (convImpl.getTotalOutstandingRequestCount() == 0) {
                final AbstractInvocation errorInvoc = convImpl.getErrorOccurredInvocation();
                if (errorInvoc != null) {
                  errorInvoc.invoke();
                  errorInvoc.repool();
                  convImpl.setErrorOccurredInvocation(null);
                }
              }
            }

            //Now that invocation has been invoked, repool it.
            repoolInvocation = true;
          } else {
            //AbstractInvocation.toString includes refCount and ready flag information so no need for extra information.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Not invoking " + invocation + " at this time");
          }


          //Need to do this when holding the invocation lock in order to ensure values don't get reset by another
          //thread repooling invocation at the same.
          size = invocation.getSize();

          final Connection connection = invocation.getConnection();
          if (connection != null) {
            conversationType = connection.getConversationType();
          } else {
            conversationType = null;
          }
        }

        //We are now done with invocation, dequeue it regardless of whether we invoked it or not.
        dequeue();

        //Unlock barrier using stashed information.
        unlockBarrier(size, conversationType);

        //All done, repool invocation.
        if(repoolInvocation) invocation.repool();

        // Reduce the conversation's use count for this dispatch queue.
        // If this hits zero then disassociate the conversation from this queue.

        synchronized (dispatchable.getDispatchLockObject()) {
          dispatchable.decrementDispatchQueueRefCount();
          if (dispatchable.getDispatchQueueRefCount() == 0) {
            dispatchable.setDispatchQueue(null);
          }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Dispatchable refCount = " + dispatchable.getDispatchQueueRefCount());

        // The queue may now be empty but to ensure correct synchronisation with enqueue synchronise on this object

        synchronized (this) {
          if (isEmpty()) {
            if (emptyDispatchQueues != null) {
              synchronized (emptyDispatchQueues) {
                emptyDispatchQueues.add(this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Added this queue (" + this + ") to empty dispatch list ("+emptyDispatchQueues+")");
              }
            }

            // Sleep for a few seconds to see if more work comes in. This is done to avoid the contention on the WAS
            // threadpool that backs the dispatch queues.

            try {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Thread waiting " + rldThreadRepoolDelay + "ms for new work to arrive");
              wait(rldThreadRepoolDelay);
            } catch (InterruptedException e) {
              // No FFDC Code Needed
            }

            //Do we need to continue processing?
            continueProcessing = !isEmpty();

            // If we are to continue processing remove ourselves from the emptyDispatchQueues list
            if (continueProcessing) {
              if (emptyDispatchQueues != null) {
                synchronized (emptyDispatchQueues) {
                  emptyDispatchQueues.remove(this);
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Removed this queue (" + this + ") from empty dispatch list ("+emptyDispatchQueues+")");
                }
              }
            }

            //Indicate whether a new thread needs to be started to process future work.
            running = continueProcessing;
          }
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "continue running thread="+continueProcessing);
        }
      } catch(Throwable e) {
        FFDCFilter.processException(e, "com.ibm.ws.sib.jfapchannel.impl.rldispatcher.ReceiveListenerDispatchQueue.run", JFapChannelConstants.RLDISPATCHQUEUE_RUN_01, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "ReceiveListenerDispatchQueue thread caught exception: ", e);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(this, tc, e);
      }
    } // while (continueProcessing)

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "run");
  }

  // Return true if a conversation is already in this queue

  protected boolean doesQueueContainConversation (final Conversation conversation) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "doesQueueContainConversation", conversation);

    boolean rc = false;

    synchronized (barrier) {
      for (int i = 0; i < queue.size(); i++) {
        final AbstractInvocation invocation = queue.get(i);
        if (invocation.getConversation().equals(conversation)) {
          rc = true;
          break;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "doesQueueContainConversation", rc);
    return rc;
  }
}
