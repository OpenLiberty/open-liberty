/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.rldispatcher;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.DispatchToAllNonEmptyDispatchable;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.ConversationImpl;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is a concrete implementation of a dispatchable that dispatches the invocation to all the
 * available dispatch queues.
 * <p>
 * This is needed when we are not sure which Dispatchable to dispatch the invocation by - but we
 * sure that it needs to go after any data currently held in our dispatch queues. An example of
 * this is a close of a session using a transaction. We are not able to easily determine which
 * queue the close should go on - so therefore queue it to all of them and let the last man invoke
 * him.
 * <p>
 * As we queue each invocation we increment it's reference count. When we have completed queueing
 * the invocation we will set it to 'ready' (guard against the fact that someone could de-queue
 * the invocation while we are adding it onto a different queue). When we, or a dispatch thread
 * finds an invocation that is ready and has a reference count of 0 (after decrementing it) it
 * can safely invoke it.
 *
 * @author Gareth Matthews
 */
public class DispatchToAllNonEmptyDispatchableImpl extends DispatchToAllNonEmptyDispatchable
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(DispatchToAllNonEmptyDispatchableImpl.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

  private static boolean increaseClosePriority;   //PK65014
  
  private static boolean alwaysDispatchClose; // PMR 87787
  
  private static boolean alwaysDispatchStop; 
  
  private static boolean alwaysDispatchUnlockAll;

  static
  {
    increaseClosePriority = JFapChannelConstants.DEFAULT_INCREASE_CLOSE_PRIORITY;
    alwaysDispatchClose = JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_CLOSE;
    alwaysDispatchStop = JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_STOP;
    alwaysDispatchUnlockAll = JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_UNLOCKALL;
    
    try
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Default increase close priority been used: " + increaseClosePriority);

       increaseClosePriority =
          Boolean.valueOf(RuntimeInfo.getPropertyWithMsg("com.ibm.ws.sib.jfapchannel.INCREASE_CLOSE_PRIORITY",Boolean.toString(JFapChannelConstants.DEFAULT_INCREASE_CLOSE_PRIORITY))).booleanValue();

       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "increase close prioirty property found and set: "+ increaseClosePriority);
       
       alwaysDispatchClose =
           Boolean.valueOf(RuntimeInfo.getPropertyWithMsg("com.ibm.ws.sib.jfapchannel.ALWAYS_DISPATCH_CLOSE",Boolean.toString(JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_CLOSE))).booleanValue();

       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "always dispatch close property found and set: "+ alwaysDispatchClose);
       
       alwaysDispatchStop =
         Boolean.valueOf(RuntimeInfo.getPropertyWithMsg("com.ibm.ws.sib.jfapchannel.ALWAYS_DISPATCH_STOP",Boolean.toString(JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_STOP))).booleanValue();

       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "always dispatch stop property found and set: "+ alwaysDispatchStop);
       
       alwaysDispatchUnlockAll =
         Boolean.valueOf(RuntimeInfo.getPropertyWithMsg("com.ibm.ws.sib.jfapchannel.ALWAYS_DISPATCH_UNLOCKALL",Boolean.toString(JFapChannelConstants.DEFAULT_ALWAYS_DISPATCH_UNLOCKALL))).booleanValue();

       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "always dispatch unlockAll property found and set: "+ alwaysDispatchUnlockAll);
       
    }
    catch(NumberFormatException nfe)
    {
       // No FFDC code needed
    }
  }

   /** Our lock object */
   private Object lock = new Object();

   /** Our dummy dispatch queue */
   private DispatchToAllDispatchQueue dummyDispatchQueue = new DispatchToAllDispatchQueue();

   /**
    * This method may be called when the dispatcher thread 'thinks' that we can be
    * disassociated with the dispatch queue - however, we are a fake dispatchable and so are really
    * never associated with anybody.
    */
   public void setDispatchQueue(DispatchQueue queue)
   {
   }

   /**
    * @return Returns our dummy dispatch queue
    */
   public DispatchQueue getDispatchQueue()
   {
      return dummyDispatchQueue;
   }

   /**
    * @return Returns the lock for this dispatchable
    */
   public Object getDispatchLockObject()
   {
      return lock;
   }

   /**
    * Called when the JFap channel is queuing data to the dispatch queue held by this class.
    * <p>
    * As we only never queue invocations, this can be ignored.
    */
   public void incrementDispatchQueueRefCount()
   {
   }

   /**
    * Called when the JFap channel has dequeued data from the dispatch queue held by this class.
    * <p>
    * As we only never queue invocations, this can be ignored.
    */
   public void decrementDispatchQueueRefCount()
   {
   }

   /**
    * Called by a dispatcher thread to determine if the association between the dispatch queue and
    * the dispatchable can be broken. This method will never be called as we are never called on the
    * dispatcher threads.
    *
    * @return Returns 0
    */
   public int getDispatchQueueRefCount()
   {
      return 0;
   }

   /**
    * This class acts like a dummy dispatch queue and when data is queued to it it will queue it
    * to all available, non-empty dispatch queues.
    *
    * @author Gareth Matthews
    */
   private static class DispatchToAllDispatchQueue extends ReceiveListenerDispatchQueue {

      // Constructor
      private DispatchToAllDispatchQueue () {
        super(null, null, ReceiveListenerDispatchQueue.QueueType.ME_Client); // Doesn't matter what queue type we use here
      }

      /**
       * At this point here we want to enqueue the data to all the non-empty queues
       *
       * @param AbstractInvocation The invocation to process.
       */
      public synchronized void enqueue(AbstractInvocation invocation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "DispatchToAllDispatchQueue.enqueue", invocation);

         // First get a handle on all the dispatch queues
         ReceiveListenerDispatchQueue[] queues =
            ReceiveListenerDispatcher.getInstance(invocation.getConnection().getConversationType(), false).getDispatchQueues(); // D242116

         // First initialise the invocation so that the reference counts come into play
         invocation.zeroReferenceCounts();
         
         // Keep track of the queue with the lowest number of dispatchables on it, so
         // we can make a sensible decision to dispatch the close later if we need to.
         ReceiveListenerDispatchQueue lowestQueue = null;
         int lowestQueueDepth = Integer.MAX_VALUE;

         // Now we need to go through all of the queues. If the queue is empty, leave it alone. If
         // the queue does have data onto it, lock it down and enqueue this request to it and
         // increment its reference count.
         int enqueuedToCount = 0;
         if (increaseClosePriority)
         {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "increaseClosePriority is true");
           // Change PK65014
           //  We will want to queue up on any non-empty queues for THIS conversation. This means we should be
           //  able to deferr the close until all current work with this connection has finished.
           Conversation thisConv = invocation.getConversation();
           ReceiveListenerDispatchQueue queue = null;
           for (int x = 0; x < queues.length; x++)
           {
              queue = queues[x];
              // Don't synchronize on the queue object here because if we can't enqueue because the queue is full then
              // we block the thread taking items off the queue which results in a deadlock.
              int queueDepth = queue.getDepth();
              if (queueDepth < lowestQueueDepth) {
            	  lowestQueueDepth = queueDepth;
            	  lowestQueue = queue;
              }
              if (queueDepth > 0 && queue.doesQueueContainConversation(thisConv))
              { // queue is not empty and contains work for this conversation
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "enqueuing");
                 enqueuedToCount++;
                 invocation.incrementReferenceCount();
                 queue.enqueue(invocation);
              }
           }
         }
         else
         {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "placing onto nonempty queue");
           // Now we need to go through all of the queues. If the queue is empty, leave it alone. If
           // the queue does have data onto it, lock it down and enqueue this request to it and
           // increment its reference count.
           ReceiveListenerDispatchQueue queue = null;
           for (int x = 0; x < queues.length; x++)
           {
              queue = queues[x];
              // Don't synchronize on the queue object here because if we can't enqueue because the queue is full then
              // we block the thread taking items off the queue which results in a deadlock.
              int queueDepth = queue.getDepth();
              if (queueDepth < lowestQueueDepth) {
            	  lowestQueueDepth = queueDepth;
            	  lowestQueue = queue;
              }
              if (queueDepth > 0)
              {
                 enqueuedToCount++;
                 invocation.incrementReferenceCount();
                 queue.enqueue(invocation);
              }
           }
         }

         synchronized (invocation)
         {
            // Inform the world that they can now start invoking this invocation
            invocation.setReady();

            // Now we have examined all the queues. We could be in 3 situations here:
            // 1) We successfully queued data onto the queues and the reference count is not zero.
            //    Here we can be sure that there are queues that are still waiting to process this
            //    so we can safely exit.
            // 2) We did not find any queues to enqueue the data to. As such, we should invoke it here
            //    and now - this will prevent data for this conversation being executed before this
            //    has been executed.
            // 3) We successfully queued data onto the queues and the reference count IS zero. In this
            //    case all the queues have beaten us to it and processed the information before we
            //    finish looking at the queues. In this case we must also execute the invocation here
            //    and now.

            // Start D262285
            boolean invokeIt = false;
            if (enqueuedToCount != 0 && invocation.getReferenceCount() != 0)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Invocation has not been executed yet");
            }
            else if (enqueuedToCount == 0)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "No active dispatch queues");
               invokeIt = true;
            }
            else if (enqueuedToCount != 0 && invocation.getReferenceCount() == 0)
            {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "All dispatch queues have processed the invocation");
               invokeIt = true;
            }

            if (invokeIt)
            {
              // We are currently holding a lock that prevents any other thread from entering
              // this code. That means it is not always sensible for us to invoke the close in-line.
              // It is not a 'free' operation, as it at minimum means sending back a close response packet
              // to the remote server. In extreme cases where we get a flood of closes, attempting
              // do do the close in-line here results in us basically single-threading the ME until
              // we process the backlog of close requests.
              // We have a tuning parameter that allows users to choose to dispatch the close requests
              // as normal in this circumstance.
              if (alwaysDispatchClose && lowestQueue != null &&
                  (invocation.segmentType == JFapChannelConstants.SEG_CLOSE_CONSUMER_SESS ||
                   invocation.segmentType == JFapChannelConstants.SEG_CLOSE_PRODUCER_SESS ||
                   invocation.segmentType == JFapChannelConstants.SEG_CLOSE_CONNECTION    ||
                   invocation.segmentType == JFapChannelConstants.SEG_CLOSE_ORDER_CONTEXT   )) {                
                // Dispatch this, now ready, invocation to the dispatch queue that was lowest
                // while we were running through our above logic
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Enqueuing close segment to lowest queue, rather than running in-line");                
                invocation.incrementReferenceCount(); // Now 1
                lowestQueue.enqueue(invocation);                
              }
              else if (alwaysDispatchStop && lowestQueue != null &&
                  invocation.segmentType == JFapChannelConstants.SEG_STOP_SESS)
              {                
                // Dispatch this, now ready, invocation to the dispatch queue that was lowest
                // while we were running through our above logic
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Enqueuing stop segment to lowest queue, rather than running in-line");                
                invocation.incrementReferenceCount(); // Now 1
                lowestQueue.enqueue(invocation);                
              }
              else if (alwaysDispatchUnlockAll && lowestQueue != null &&
                  invocation.segmentType == JFapChannelConstants.SEG_UNLOCK_ALL)
              {                
                // Dispatch this, now ready, invocation to the dispatch queue that was lowest
                // while we were running through our above logic
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(this, tc, "Enqueuing unlockAll segment to lowest queue, rather than running in-line");                
                invocation.incrementReferenceCount(); // Now 1
                lowestQueue.enqueue(invocation);                
              }
              else {
                // Invoke it in-line
                invocation.invoke();

                // Ensure that we decrement the total outstanding on the conversation otherwise
                // errorOccurred invocations may never be processed
                ConversationImpl conv = ((ConversationImpl) invocation.conversation);
                synchronized (conv.getTotalOutstandingRequestCountLock())
                {
                  conv.decrementTotalOutstandingCount();
                }

                invocation.repool();
              }
            }
            // End D262285
         }

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "DispatchToAllDispatchQueue.enqueue");
      }

      /**
       * Called by the JFap channel prior to enqueue'ing data to ascertain whether to dispatch a
       * new thread to service us. As we don't want a new thread (ever) - always return false.
       *
       * @return Returns false
       */
      public boolean isEmpty()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "DispatchToAllDispatchQueue.isEmpty");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "DispatchToAllDispatchQueue.isEmpty", ""+false);
         return false;
      }
   }
}
