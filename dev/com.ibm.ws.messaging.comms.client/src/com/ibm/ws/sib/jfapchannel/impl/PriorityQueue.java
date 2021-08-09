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
package com.ibm.ws.sib.jfapchannel.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapByteBuffer;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.SendListener;
import com.ibm.ws.sib.jfapchannel.Conversation.ThrottlingPolicy;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;


/**
 * A table-like structure which tracks data to be sent by priority.
 * The idea is that every send calls queues its data to be sent into
 * this table, then de-queues the higest priority piece of data and
 * sends that.  By doing this, we implement a kind of priority
 * transmission mechanism.
 */
public class PriorityQueue
{
   private static final TraceComponent tc = SibTr.register(PriorityQueue.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

   /** Enumeration for state of priority queue */
   private static final class StateEnum
   {
      private String description;
      private StateEnum(String description)
      {
         this.description = "PriorityQueue state: "+description;
      }
      public String toString()
      {
         return description;
      }
   }

   /**
    * A barrier style monitor.  Depending on whether the monitor is enabled or
    * disabled it will either block everything or let everything past.
    */
   private static class ActivatableMonitor
   {
      private boolean enabled = true;

      /**
       * Creates a new monitor.
       * @param enabled Will the monitor initially block everything or not?
       */
      public ActivatableMonitor(boolean enabled)
      {
         this.enabled = enabled;
      }

      /**
       * Waits on the monitor.  This will do nothing if the monitor is not active.
       * If however the monitor is active then the callers thread will block until
       * the monitor is deactivated.
       */
      public synchronized void waitOn()
      {
         if (enabled)
         {
            while(true)
            {
               try
               {
                  wait();
                  break;
               }
               catch (InterruptedException e)
               {
                  // No FFDC code needed
               }
            }
         }
      }

      /**
       * Sets whether the monitor is active or not.
       * @param enabled
       */
      private synchronized void setActive(boolean enabled)
      {
         if (this.enabled != enabled)
         {
            this.enabled = enabled;
            if (!enabled) notifyAll();
         }
      }
   }

   // States that the priority queue may be in.
   private static final StateEnum OPEN = new StateEnum("OPEN");
   private static final StateEnum CLOSING = new StateEnum("CLOSING");
   private static final StateEnum CLOSED = new StateEnum("CLOSED");

   // Current state of the priority queue
   private StateEnum state = OPEN;

   // Type of conversation this queue is associated with.  This is used for PMI.
   private Conversation.ConversationType type = Conversation.UNKNOWN;            // F193735.3

   private volatile static int clientQueuedBytes = 0;                            // F193735.3
   private volatile static int meQueuedBytes = 0;                                // F193735.3

   // Set the maximum queue depth we permit to 100
   private static int maxQueueDepth = 100;

   // Set the maximum bytes queued on a single queue to 100K
   private static int maxQueueBytes = 1024* 100;

   
   // =========================================================================
   // All the attributes declared in this section should not be examined or
   // modified unless one owns the object monitor for 'queueMonitor'

   // begin D217401
   /**
    * FIFO queue implementation based upon an array.  We used to use an ArrayList
    * for this - however dequing things from the front of the array list involved an
    * arraycopy which was relatively expensive.
    */
   private class CircularFIFOArrayBuffer
   {
      private TransmissionDataIterator[] array;
      private int size = 0;
      private int firstElementIndex = 0;
      private int maxSize = 0;
      public CircularFIFOArrayBuffer(int size)
      {
         array = new TransmissionDataIterator[size];
         maxSize = size;
      }

      public void enqueue(TransmissionDataIterator iterator)
      {
         array[(firstElementIndex + size) % maxSize] = iterator;
         ++size;
      }

      public TransmissionDataIterator dequeue()
      {
         TransmissionDataIterator result = null;
         if (size > 0)
         {
            result = array[firstElementIndex];
            array[firstElementIndex] = null;
            firstElementIndex = (firstElementIndex + 1) % maxSize;
            --size;
         }
         return result;
      }

      public TransmissionDataIterator head()
      {
         TransmissionDataIterator result = null;
         if (size > 0) result = array[firstElementIndex];
         return result;
      }
   }
   // end D217401

   // Holds information about data at a particular priority level.
   private class Queue extends CircularFIFOArrayBuffer   // D217401
   {
      // Add 10 to the max queue depth in the event that any auto-queued system messages are
      // needing to be sent.
      private Queue() { super(maxQueueDepth + 10); }          // D217401
      int depth = 0;
      int bytes = 0;
      boolean hasCapacity = true;
      ActivatableMonitor monitor = new ActivatableMonitor(false);
   }

   // An array of queues that form the basis for the priority queue.
   private Queue[] queueArray = new Queue[JFapChannelConstants.MAX_PRIORITY_LEVELS];

   // Index into the queueArray of the lowest priority level with capacity
   private int lowestPriorityWithCapacity = 0;

   // Total depth of all queues.  When zero, the priority queue is empty.
   private int totalQueueDepth = 0;

   // Monitor object.  Must be held before any of the queues internal structures
   // are examined or updated.
   public Object queueMonitor;

   // Monitor used to block waiters whilst the queue closes.
   private ActivatableMonitor closeWaitersMonitor = new ActivatableMonitor(true);

   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.common.impl/src/com/ibm/ws/sib/jfapchannel/impl/PriorityQueue.java, SIB.comms, WASX.SIB, uu1215.01 1.44");

      // Retrieve any user configured values for the max queue depth or number of bytes.
      try { maxQueueDepth = Integer.parseInt(RuntimeInfo.getProperty(JFapChannelConstants.RUNTIMEINFO_KEY_MAX_PRIORITY_QUEUE_DEPTH)); }
      catch(NumberFormatException nfe) { // No FFDC code needed
      }
      try { maxQueueBytes = Integer.parseInt(RuntimeInfo.getProperty(JFapChannelConstants.RUNTIMEINFO_KEY_MAX_PRIORITY_QUEUE_BYTES)); }
      catch(NumberFormatException nfe) { // No FFDC code needed
      }
   }

   /**
    * Creates a new priority queue.
    */
   public PriorityQueue()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");

      for (int i=0; i < JFapChannelConstants.MAX_PRIORITY_LEVELS; ++i)
         queueArray[i] = new Queue();
      queueMonitor = this;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

	/**
	 * Queues the specified request into the priority table.
	 */
   public void queue(JFapByteBuffer bufferData,
                     int segmentType,
                     int requestNumber,
                     int priority,
                     SendListener sendListener,
                     Conversation conversation,
                     Connection connection,
                     int conversationId,
                     boolean pooledBuffers,
                     boolean partOfExchange,
                     long size,
                     boolean terminal,
                     ThrottlingPolicy throttlingPolicy)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queue");
      TransmissionDataIterator iterator =
         TransmissionDataIterator.allocateFromPool(connection,
                                                   bufferData,
                                                   priority,
                                                   pooledBuffers,
                                                   partOfExchange,
                                                   segmentType,
                                                   conversationId,
                                                   requestNumber,
                                                   conversation,
                                                   sendListener,
                                                   terminal,
                                                   (int)size);

      // begin F193735.3
      if (type == Conversation.ME)           meQueuedBytes += size;
      else if (type == Conversation.CLIENT)  clientQueuedBytes += size;
      // end F193735.3

      queueInternal(iterator, throttlingPolicy, terminal);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queue");
   }

   // begin F181603.2
   private void queueInternal(TransmissionDataIterator data,
                              ThrottlingPolicy throttlingPolicy,
                              boolean isTerminal)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "queueInternal", data);

      boolean done = false;
      boolean releaseTransmission = false;

      int priority = data.getPriority();
      int size = data.getSize();
      Queue queue = null;
      while(!done)
      {
         ActivatableMonitor monitor = null;

         synchronized(queueMonitor)
         {
            if (state == CLOSED)
            {
               // If we are closed - simply bin anything we are passed.
               done = true;
            }
            else
            {
               if (isTerminal) state = CLOSING;

               if (queue == null)
               {
                  // If data was assigned the special "lowest priority" value
                  // resolve this to an actual priority level.
                  if (priority == Conversation.PRIORITY_LOWEST)
                  {
                     int max = JFapChannelConstants.MAX_PRIORITY_LEVELS-1;
                     priority = 0;
                     while((priority < max) && (queueArray[priority].depth == 0)) ++priority;
                     data.setPriority(priority);
                     queue = queueArray[priority];
                  }
                  else
                  {
                     queue = queueArray[priority];
                  }
               }

               // If we are not allowed to perform throttling, or we have capacity,
               // accept the data and queue it.
               if ((throttlingPolicy == ThrottlingPolicy.DO_NOT_THROTTLE) ||
                   (queue.hasCapacity && (priority >= lowestPriorityWithCapacity)))
               {
                  // Queue has capacity.  Enqueue the data.
                  queue.enqueue(data);                                  // D217401
                  queue.bytes += data.getSize();
                  ++ queue.depth;
                  ++ totalQueueDepth;
                  done = true;

                  // Decide if this has caused a change to the level of capacity we advertise.
                  if (queue.hasCapacity &&
                      ((queue.depth >= maxQueueDepth) || (queue.bytes >= maxQueueBytes)))
                  {
                     queue.hasCapacity = false;

                     // If the change in capacity has resulted in a change to the lowest level
                     // with priority - activate the monitors for the lower capacity level
                     if (priority >= lowestPriorityWithCapacity)
                     {
                        int newLowestPriorityWithCapacity = priority+1;
                        while (lowestPriorityWithCapacity < newLowestPriorityWithCapacity)
                        {
                           queueArray[lowestPriorityWithCapacity].monitor.setActive(true);
                           ++lowestPriorityWithCapacity;
                        }
                     }
                  }
               }
               else
               {
                  // Queue does not have capacity.  Our action depends on the
                  // throttling policy.
                  if (throttlingPolicy == ThrottlingPolicy.BLOCK_THREAD)
                  {
                     // Block this thread until we have capacity
                     monitor = queue.monitor;
                  }
                  else if (throttlingPolicy == ThrottlingPolicy.DISCARD_TRANSMISSION)
                  {
                     // Don't queue the transmission - just return.
                     releaseTransmission = true;
                     done = true;
                  }
               }
            }
         }

         if (monitor != null) monitor.waitOn();
      }

      if (releaseTransmission) data.release();

      if (type == Conversation.CLIENT)
         clientQueuedBytes += size;
      else
         meQueuedBytes += size;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "queueInternal");
   }
   // end F181603.2

	/**
	 * De-queues the highest priority entry from the table
    * @throws SIConnectionDroppedException if the priorty queue has been
    * closed or purged.
	 * @return RequestData Returns the highest priority request data
	 * object or null.
	 */
   public TransmissionData dequeue()
   throws SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "dequeue");

      TransmissionData retValue = null;

      Queue queue = null;
      synchronized(queueMonitor)
      {
         if (state == CLOSED)
         {
            throw new SIConnectionDroppedException(TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "PRIORITY_QUEUE_PURGED_SICJ0077", null, "PRIORITY_QUEUE_PURGED_SICJ0077"));
         }
         else
         {
            // Find the highest non-empty priority
            int priority = JFapChannelConstants.MAX_PRIORITY_LEVELS-1;
            while((priority >= 0) && (queueArray[priority].depth == 0)) --priority;

            if (priority >= 0)
            {
               queue = queueArray[priority];

               // Dequeue the data and update the queue appropriately.
               TransmissionDataIterator iterator = queue.head();               // D217401
               retValue = iterator.next();

               queue.bytes -= retValue.getSize();

               // If there is no more data left to iterate over then remove the
               // transmission data from the queue and update the depth counters.
               if (!iterator.hasNext())
               {
                  queue.dequeue();                                      // D217401
                  --queue.depth;
                  --totalQueueDepth;
               }

               // If the priority queue is in closing state and we have just emptied
               // the queue, mark the queue as closed and unblock anyone waiting for the
               // queue to close.
               if ((totalQueueDepth == 0) && (state == CLOSING))
               {
                  state = CLOSED;                           // Transition to the close state
                  closeWaitersMonitor.setActive(false);     // Unblock anyone waiting for the close

                  // Un-block any queue monitors, in case other threads are blocked waiting
                  // to try and queue data.
                  for (int i=0; i < JFapChannelConstants.MAX_PRIORITY_LEVELS; ++i)
                     queueArray[i].monitor.setActive(false);
               }

               // If dequeueing the data caused a change in capacity then
               // take the appropriate actions
               if (!queue.hasCapacity &&
                   (queue.bytes < maxQueueBytes) &&
                   queue.depth < maxQueueDepth)
               {
                  // Mark the queue as having capacity
                  queue.hasCapacity = true;

                  // If the change in capacity results in more priority levels having capacity then...
                  if (priority < lowestPriorityWithCapacity)
                  {
                     // Unblock any threads waiting on the lower capacity levels
                     int newLowestPriorityWithCapacity = priority;
                     while((newLowestPriorityWithCapacity > 0) &&
                           queueArray[newLowestPriorityWithCapacity-1].hasCapacity)
                     {
                        queueArray[newLowestPriorityWithCapacity].monitor.setActive(false);
                        --newLowestPriorityWithCapacity;
                     }
                     lowestPriorityWithCapacity = newLowestPriorityWithCapacity;
                     queueArray[lowestPriorityWithCapacity].monitor.setActive(false);
                  }
               }
            }
         }
      }

      if (retValue != null)
      {
         if (type == Conversation.CLIENT)
            clientQueuedBytes -= retValue.getSize();
         else
            meQueuedBytes -= retValue.getSize();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "dequeue", retValue);
      return retValue;
   }

   /**
    * Checks to see if a given priority level has the capacity to accept another
    * message to be queued.
    * @param priority
    * @return boolean
    */
   public boolean hasCapacity(int priority)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasCapacity", ""+priority);

      boolean result;
      synchronized(queueMonitor)
      {
         result = priority >= lowestPriorityWithCapacity;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasCapacity", ""+result);
      return result;
   }

   /**
    * Closes the priority queue.  This causes all new queue requests to be ignored.
    * Any existing data that has been queued may be dequeued, unless the immediate flag
    * has been set, in which case, we consider ourselves closed.
    */
   public void close(boolean immediate)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close", ""+immediate);

      synchronized(queueMonitor)
      {
         if (immediate || (totalQueueDepth == 0))
         {
            state = CLOSED;
            closeWaitersMonitor.setActive(false);
         }
         else
            state = CLOSING;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * Purges the content of the priority queue.  This closes the queue and
    * wakes up any blocked threads
    */
   public void purge()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "purge");

	   synchronized(queueMonitor)
	   {
		   state = CLOSED;
		   for (int i=0; i < JFapChannelConstants.MAX_PRIORITY_LEVELS-1; ++i)
		   {
			   queueArray[i].monitor.setActive(false);
		   }
		   closeWaitersMonitor.setActive(false);
	   }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "purge");
   }

   // begin D203646
   /**
    * Blocks until a close operation has completed.  I.e. the priority queue has been
    * drained.  If the queue is already closed, this method returns immeidately.
    */
   public void waitForCloseToComplete()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "waitForCloseToComplete");

      closeWaitersMonitor.waitOn();

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "waitForCloseToComplete");
   }
   // end D203646

   // begin F193735.3
   /**
    * Attributes a "type" to this priority queue.  This information is used for PMI.
    */
   protected void setType(Conversation.ConversationType type)
   {
      this.type = type;
   }
   // end F193735.3

   /**
    * Returns true iff this priority queue is empty.
    * @throws SIConnectionDroppedException if the priorty queue has been
    * closed or purged.
    * @return True iff this priority queue is empty.
    */
   public boolean isEmpty() throws SIConnectionDroppedException
   {
      synchronized(queueMonitor)
      {
         if (state == CLOSED) throw new SIConnectionDroppedException(TraceNLS.getFormattedMessage(JFapChannelConstants.MSG_BUNDLE, "PRIORITY_QUEUE_PURGED_SICJ0077", null, "PRIORITY_QUEUE_PURGED_SICJ0077"));
         return totalQueueDepth == 0;
      }
   }
}
