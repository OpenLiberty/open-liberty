/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl.rldispatcher;

import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.Dispatchable;
import com.ibm.ws.sib.jfapchannel.impl.Connection;

/**
 * Represents the common features of a receive listener method which can
 * be invoked.  Specialisations exist to deal with the variations in actual
 * receive listener methods which may be invoked.
 */
public abstract class AbstractInvocation
{
   protected Connection connection;
   protected int size;
   protected int segmentType;
   protected int requestNumber;
   protected int priority;
   protected volatile Conversation conversation;
   private Dispatchable dispatchable = null;   // Must be kept thread-safe  


   /**
    * This invocation can be queued onto multiple dispatch queues. The ref count indicates
    * how many queues this invocation is on and is currently waiting to be processed. Threads
    * processing this invocation should only invoke it when the ref count is 0.
    */
   private int refCount = 1;      // Must be kept thread-safe

   /**
    * To avoid locking all the dispatch queues while this invocation is queued to it (if it is
    * being queued to all of them) we use this flag to indicate when the reference count is
    * valid and the invocation can now be processed.
    */
   private boolean ready = true;  // Must be kept thread-safe

   protected AbstractInvocation(Connection connection,
                                int size,
                                int segmentType,
                                int requestNumber,
                                int priority,
                                Conversation conversation)
   {
      this.connection = connection;
      this.size = size;
      this.segmentType = segmentType;
      this.requestNumber = requestNumber;
      this.priority = priority;
      this.conversation = conversation;
   }

   /**
    * Invoke the appropriate receive listener method.  Implementors
    * of subclasses should write an implementation which uses the
    * information encapsulated within this class to invoke the appropraite
    * receive listerner method.
    */
   protected abstract void invoke();

   /**
    * Invoked when this class may safely repool itself into an object
    * pool.  Implementors of subclasses can implement this method to
    * repool the object into a pool if they wish.
    */
   protected abstract void repool();

   /**
    * This method will ask the receive listener for any thread context for this invocation.
    * If the receive listener does not care, or does not have one, it should return null.
    *
    * @return Returns a thread context or null, if it is not implemented / needed / none exists.
    */
   protected abstract Dispatchable getThreadContext();

   /**
    * Getter for the conversation. Note conversation is volatile so
    * synchronization is not required. We do not want to be 
    * sychronized just to query this value, because the invocation 
    * lock is held throughout the invocation, so a thread simply 
    * looking at the conversation to see if it's one it's 
    * interested in would block for the length of the invocation. 
    * @return Returns the conversation associated with this invocation.
    */
   protected Conversation getConversation()
   {
      return conversation;
   }

   /**
    * Returns the dispatchable associated with this invocation request.
    * @return Dispatchable
    */
   protected synchronized Dispatchable getDispatchable()
   {
      return dispatchable;
   }

   /**
    * Sets the dispatchable to be associated with this invocation request.
    * @param dispatchable
    */
   protected synchronized void setDispatchable(Dispatchable dispatchable)
   {
      this.dispatchable = dispatchable;
   } 
   
   protected int getSize()
   {
      return size;
   }
   
   protected synchronized Connection getConnection()
   {
      return connection;
   }
   
   /**
    * This method will reset all the reference counts to their default values and put it in the
    * state where the invocation is ready to be executed. This is needed because the invocations
    * are pooled.
    */
   protected synchronized void resetReferenceCounts()
   {
      refCount = 1;
      ready = true;
   }

   /**
    * This method should be called on the invocation when the invocation needs to be queued to
    * more than one queue. This method needs to be executed before queueing to any queues. Its
    * purpose is to set the reference count to zero and put it in the not-ready state.
    */
   protected synchronized void zeroReferenceCounts()
   {
      refCount = 0;
      ready = false;
   }

   /**
    * This method increments the invocation reference count. This should be called when queueing
    * data to a dispatch queue.
    */
   protected synchronized void incrementReferenceCount()
   {
      refCount++;
   }

   /**
    * This method decrements the invocation reference count. This should be called just before
    * processing data from a dispatch queue.
    */
   protected synchronized void decrementReferenceCount()
   {
      refCount--;
   }

   /**
    * This method returns the reference count for this invocation. If the reference count is 0 then
    * there are no more queues waiting to process this invocation. Callers can then assume that when
    * the count is 0 and the invocation is ready, this invocation can safely be invoked.
    *
    * @return Returns the reference count for the queue.
    */
   protected synchronized int getReferenceCount()
   {
      return refCount;
   }

   /**
    * This method is used to mark the invocation as ready to be invoked.
    */
   protected synchronized void setReady()
   {
      this.ready = true;
   }

   /**
    * @return Returns whether this invocation is ready to be invoked.
    */
   protected synchronized boolean isReady()
   {
      return ready;
   }

   /**
    * @return Returns some status about the invocation.
    */
   public synchronized String toString()
   {
      return super.toString() + ": ref count: " + refCount + ", ready: " + ready;
   }
}
