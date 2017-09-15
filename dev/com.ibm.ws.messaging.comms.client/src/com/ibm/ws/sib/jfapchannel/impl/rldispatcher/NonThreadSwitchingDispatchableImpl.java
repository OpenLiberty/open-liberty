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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.DispatchQueue;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.NonThreadSwitchingDispatchable;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This is the concrete implementation of a non thread switching dispatchable. When used as a
 * dispatchable, this implementation will not allow any thread switch to take place for the
 * receive listener invocations to be exectuted.
 * <p>
 * This is acheived as follows:
 * <p>
 * The receive listener dispatcher will ask the dispatchable instance for the dispatch queue and
 * the data will be queued up in this queue. If the queue was initially empty, then the dispatcher
 * performs a thread switch and the data is invoked on another thread.
 * <p>
 * This implementation craftily does not have a proper dispatch queue backing it. As such, the queue
 * will always appear non-empty (so the dispatcher will not start a thread on it) and the call to
 * enqueue the data to the queue will just perform the invocation before returning. When the
 * dispatcher has queued the data it will forget about the queue and the data - and by this time
 * the invocation has already been completed.
 *
 * @author Gareth Matthews
 */
public class NonThreadSwitchingDispatchableImpl extends NonThreadSwitchingDispatchable
{
   /** Trace */
   private static final TraceComponent tc = SibTr.register(NonThreadSwitchingDispatchableImpl.class,
                                                           JFapChannelConstants.MSG_GROUP,
                                                           JFapChannelConstants.MSG_BUNDLE);

   /** Our lock object */
   private Object lock = new Object();

   /** Our dummy dispatch queue */
   private DispatchQueue dummyDispatchQueue = new DummyDispatchQueue();

   /**
    * This method will return our special dummy instance of a queue only.
    *
    * @return DispatchQueue
    */
   public DispatchQueue getDispatchQueue()
   {
      return dummyDispatchQueue;
   }

   /**
    * @return Returns our lock object.
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
    * This method should never be called as we always appear to have a backing queue - however,
    * if it does, then it can be safely ignored.
    *
    * @param queue
    */
   public void setDispatchQueue(DispatchQueue queue)
   {
   }

   /**
    * Our dummy dispatch queue. This class extends the normal one and overrides the important
    * methods.
    *
    * Actually what this class should do is implement DispatchQueue (which should be properly
    * configured with the required methods) and not extend ReceiveListenerDispatchQueue which
    * is just one implentation of DispatchQueue this should be an alternative implementation (mkv)
    */
   private static class DummyDispatchQueue extends ReceiveListenerDispatchQueue {

      // Constructor
      private DummyDispatchQueue () {
        super(null, null, ReceiveListenerDispatchQueue.QueueType.ME_Client); // Doesn't matter what queue type we use here
      }

      /**
       * This method is called by the JFap channel when there is an invocation ready to be
       * processed. A well behaved dispatch queue would queue this so that it could be serviced by
       * another thread - but we will just exectute it now.
       *
       * @param AbstractInvocation The invocation to process.
       */
      public void enqueue(AbstractInvocation invocation)
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "DummyDispatchQueue.enqueue", invocation);

         invocation.invoke();
         invocation.repool();

         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "DummyDispatchQueue.enqueue");
      }

      /**
       * Called by the JFap channel prior to enqueue'ing data to ascertain whether to dispatch a
       * new thread to service us. As we don't want a new thread (ever) - always return false.
       *
       * @return Returns false
       */
      public boolean isEmpty()
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "DummyDispatchQueue.isEmpty");
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "DummyDispatchQueue.isEmpty", ""+false);
         return false;
      }
   }
}

