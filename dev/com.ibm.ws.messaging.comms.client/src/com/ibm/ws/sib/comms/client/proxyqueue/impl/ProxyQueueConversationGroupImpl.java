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

package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.comms.client.proxyqueue.AsynchConsumerProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.BrowserProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of a proxy queue conversation group.
 * Groups together all the proxy queues which belong to
 * a particular conversation.
 */
public class ProxyQueueConversationGroupImpl implements ProxyQueueConversationGroup
{
   /** Class name for FFDC's */
   private static String CLASS_NAME = ProxyQueueConversationGroupImpl.class.getName();
   
   /** Trace */
   private static final TraceComponent tc = SibTr.register(ProxyQueueConversationGroup.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
                                                           
   /** NLS handle */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);
   
   /** Log class info on load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/ProxyQueueConversationGroupImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.50");
   }
   
   private Conversation conversation = null;
   private ProxyQueueConversationGroupFactoryImpl factory = null;
   
   private ShortIdAllocator idAllocator = new ShortIdAllocator(false);
   private boolean closed = false;

   private class ImmutableId
   {
      protected short value; 
      public ImmutableId(short value) {this.value = value;}
      public short getValue() { return value; }
      public int hashCode() { return value; }
      public boolean equals(Object o)
      {
         boolean result = false;
         if ((o != null) && (o instanceof ImmutableId))
            result = value == ((ImmutableId)o).value;
         return result;
      }
   }
   
   private class MutableId extends ImmutableId
   {
      public MutableId() { super((short)0); }
      public void setValue(short value) { this.value = value; }
   }
   
   private MutableId mutableId = new MutableId();
   
   private final HashMap<ImmutableId, ProxyQueue> idToProxyQueueMap;
   
   /** A map of order context id's to proxy queues */
   //private HashMap orderContextIdToProxyMap = null;                                       // f200337
   
   /**
    * Constructs a new proxy queue group.
    * @param conversation
    */
   public ProxyQueueConversationGroupImpl(Conversation conversation, 
                                          ProxyQueueConversationGroupFactoryImpl factory)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "<init>", new Object[] {conversation, factory});
      
      this.conversation = conversation;
      this.factory = factory;	
      idToProxyQueueMap = new HashMap<ImmutableId, ProxyQueue>();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>");
   }

   /**
    * Creates a new browser proxy queue for this group.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#createBrowserProxyQueue()
    */
   public synchronized BrowserProxyQueue createBrowserProxyQueue()                    // F171893
      throws SIResourceException, SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createBrowserProxyQueue");            // F171893
      checkClosed();
      short id = nextId();
      BrowserProxyQueue proxyQueue = new BrowserProxyQueueImpl(this, id, conversation);

      idToProxyQueueMap.put(new ImmutableId(id), proxyQueue);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createBrowserProxyQueue", proxyQueue);
      return proxyQueue;      
   }

   // Start D213014
   /**
    * Creates a new asynchronous consumer proxy queue for this group.
    * @throws SIResourceException
    * @throws SIResourceException
    * @throws SIIncorrectCallException
    * @throws SIIncorrectCallException
    * @throws SIErrorException
    * @throws SIConnectionUnavailableException
    * @throws SISessionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SISessionDroppedException
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#createAsynchConsumerProxyQueue()
    */
   public synchronized AsynchConsumerProxyQueue 
                  createAsynchConsumerProxyQueue(OrderingContext oc) 
      throws SIResourceException, SIIncorrectCallException
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createAsynchConsumerProxyQueue");
      short id = nextId();

      // begin D249096
      AsynchConsumerProxyQueue proxyQueue = null;
      
      if (oc == null)
      {
         proxyQueue = 
            new NonReadAheadSessionProxyQueueImpl(this, id, conversation);
      }
      else
      {
         proxyQueue = 
            new OrderedSessionProxyQueueImpl(this, id, conversation, oc);
      }
      // end D249096
      
      idToProxyQueueMap.put(new ImmutableId(id), proxyQueue);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createAsynchConsumerProxyQueue", proxyQueue);
      return proxyQueue;      
	}
   
   /**
    * Creates a new asynchronous consumer proxy queue for this group.
    * @throws SIIncorrectCallException
    * @throws SIIncorrectCallException
    * @throws SIErrorException
    * @throws SIConnectionUnavailableException
    * @throws SISessionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SISessionDroppedException
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#createAsynchConsumerProxyQueue()
    */
   public synchronized AsynchConsumerProxyQueue 
                  createAsynchConsumerProxyQueue(short id, 
                                                 short seqNumber, 
                                                 OrderingContext oc) 
      throws SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createAsynchConsumerProxyQueue");
      
      checkClosed();

      // begin D249096
      AsynchConsumerProxyQueue proxyQueue = null; 
      
      if (oc == null)
      {
         proxyQueue =  new NonReadAheadSessionProxyQueueImpl(this, id, conversation);
      }
      else
      {
         proxyQueue =  new OrderedSessionProxyQueueImpl(this, id, conversation, oc);
      }

      idToProxyQueueMap.put(new ImmutableId(id), proxyQueue);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createAsynchConsumerProxyQueue", proxyQueue);
      return proxyQueue;    
      // end 249096
   }
   // End D213014

   /**
    * Creates a new read ahead proxy queue for this group.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#createReadAheadProxyQueue(Reliability)
    */
   public synchronized AsynchConsumerProxyQueue createReadAheadProxyQueue(Reliability unrecoverableReliability) // f187521.2.1
      throws SIResourceException, SIIncorrectCallException
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createReadAheadProxyQueue");
      checkClosed();
      
      // begin D249096
      short id = nextId();
      AsynchConsumerProxyQueue proxyQueue = 
         new ReadAheadSessionProxyQueueImpl(this, id, conversation, unrecoverableReliability);   // f187521.2.1 // f191114
      // end D249096
      
      idToProxyQueueMap.put(new ImmutableId(id), proxyQueue);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createReadAheadProxyQueue", proxyQueue);
      return proxyQueue;      
	}
   
   // Start D213014
   // Start f200337
   /**
    * This method is used to create a proxy queue that is used for asynchronous consumers with an
    * associated message ordering context.
    * 
    * @param context
    * 
    * @throws SIResourceException
    * @throws SIInvalidStateForOperationException
    */
   public synchronized AsynchConsumerProxyQueue createOrderedProxyQueue(OrderingContext context)
      throws SIResourceException, SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createOrderedProxyQueue", context);
      
      short id = nextId();
         
      AsynchConsumerProxyQueue proxyQueue = createOrderedProxyQueue(context, id, (short) 0); // D249096
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createOrderedProxyQueue", proxyQueue);
      return proxyQueue;
   }
   // End f200337
   
   /**
    * This method is used to create a proxy queue that is used for asynchronous consumers with an
    * associated message ordering context.
    * 
    * @param context
    * @param id
    * @param seqNumber
    * 
    * @throws SIResourceException
    * @throws SIInvalidStateForOperationException
    */
   public synchronized AsynchConsumerProxyQueue createOrderedProxyQueue(OrderingContext context,
                                                                        short id,
                                                                        short seqNumber)
      throws SIResourceException, SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "createOrderedProxyQueue", context);
      
      checkClosed();
      
      AsynchConsumerProxyQueue proxyQueue = new OrderedSessionProxyQueueImpl(this, 
                                                                             id, 
                                                                             conversation, 
                                                                             context);
      idToProxyQueueMap.put(new ImmutableId(id), proxyQueue);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createOrderedProxyQueue", proxyQueue);
      return proxyQueue;
   }
   // End D213014
   
   // Start d180495
   /**
    * If a session is failed to be created then we need to bury it so
    * that it never bothers us again. The queue is simply removed from
    * the conversation group - no attempt is made to remove messages.
    * It is assumed that if something went wrong in the creation then no
    * messages would ever get to the queue.
    * 
    * @param queue
    */
   public synchronized void bury(ProxyQueue queue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "bury");
      
      // Get the proxy queue id
      short id = queue.getId();
      
      // Remove it from the table
      mutableId.setValue(id);
      idToProxyQueueMap.remove(mutableId);
            
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "bury");
   }
   // End d180495

   /**
    * Closes all the proxy queues in this group.
    * A word of warning:
    * We need to be somewhat careful in our synchronization of this operation.  Take it
    * from someone who was caught out.  If we synchronize the whole method, then it is
    * possible to deadlock.  The sequence of events is as follows:
    * <ol>
    * <li>Take lock on this object by calling close.</li>
    * <li>The close operation calls close on any open proxy queues which can flow
    *     an exchange over the wire.</li>
    * <li>Before we get a response to the close request, another proxy queue message
    *     turns up.  This cauese an invokation of the "find" method.  Unfortunately
    *     this blocks because the method call is on another thread and the thread
    *     doing the close operation already owns this objects monitor.<li>
    * <li>Deadlock!  As close will never return because it is blocked on an exchange
    *     operation who's response is queued up behind the blocked proxy queue message.</li>
    * </ol>
    * To avoid the above, the close method is now not synchronized.  However, care must
    * be taken to ensure we avoid a concurrent modification of the proxy queue map.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#close()
    */
   // begin D179183
   public void closeNotification()
      throws SIResourceException, SIConnectionLostException,
             SIErrorException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "closeNotification");

      // Determine if we need to actually perform the close (we might already be closed).
      // This needs to be synchronized to avoid a race condition into close.      
      LinkedList<ProxyQueue> closeList = null;
      synchronized(this)
      {
         if (!closed)
         {
            // Mark ourself as closed.
            closed = true;
            
            // Make a copy of the map's values to avoid a concurrent modification
            // exception later.  Then clear the map.
            closeList = new LinkedList<ProxyQueue>();
            closeList.addAll(idToProxyQueueMap.values());
         } 
      }
      
      // If we actually carried out a close operation then closeList will
      // be non-null.
      if (closeList != null)
      {
         // Close each proxy queue in turn.  This is not synchronized with this
         // objects monitor to avoid the deadlock described above.
         SIException caughtException = null;
         Iterator iterator = closeList.iterator();
         while(iterator.hasNext())
         {
            ProxyQueue queue = (ProxyQueue)iterator.next();
            
            try
            {
               if (queue.getDestinationSessionProxy() instanceof ConsumerSessionProxy)
                  ((ConsumerSessionProxy)queue.getDestinationSessionProxy()).close(true);
               else
                  queue.getDestinationSessionProxy().close();
            }
            catch(SIException e)
            {
               // No FFDC code needed.
               caughtException = e;
            }
         }
         
         // If we caught an exception when closing one of the proxy queues, report it
         // by throwing an exception of our own, linking one of the exceptions we caught.         
         if (caughtException != null)
         {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "exception caught when closing queues");
            SIConnectionLostException closeException = new SIConnectionLostException(
               nls.getFormattedMessage("ERROR_CLOSING_PROXYQUEUE_GROUP_SICO1025", new Object[] {caughtException}, null) // d192293
            );
            closeException.initCause(caughtException);
            throw closeException;
         }         
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "closeNotification");
	}
   // end D179183

   /**
    * Locates a proxy queue, from this group, via its queue ID.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#find(short)
    */
   public synchronized ProxyQueue find(short proxyQueueId)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "find", ""+proxyQueueId);
      
      mutableId.setValue(proxyQueueId);
      ProxyQueue retQueue = idToProxyQueueMap.get(mutableId);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "find", retQueue);      
      return retQueue;
   }

   /**
    * Returns the conversation this group is associated with.
    * @see com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueueConversationGroup#getConversation()
    */
   public Conversation getConversation() 
      throws SIIncorrectCallException
   {      
      checkClosed();
      return conversation;
   }
   
   // Start D209401
   /**
    * This method can be used to get a map of all proxy queue Id's mapped to the actual proxy queue
    * for this conversation.
    * 
    * @return Returns a map of all PQ ids to PQ's
    */
   public Map getProxyQueues()
   {
      return idToProxyQueueMap;
   }
   // End D209401

   /**
    * Notified when a proxy queue in this group is closed.  Allows
    * us to free up the ID assigned to it.
    * @param queue
    */
   protected void notifyClose(ProxyQueue queue)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "notifyClose", queue);
      try
		{
         final short id = queue.getId();
         idAllocator.releaseId(id);
         
         //Remove the id from the map
         synchronized(this)
         {
            idToProxyQueueMap.remove(new ImmutableId(id));
         }
		}
		catch (IdAllocatorException e)
		{
         FFDCFilter.processException(e, CLASS_NAME + ".notifyClose", 
                                     CommsConstants.PROXYQUEUECONVGROUPIMPL_NOTIFYCLOSE_01, this);  // D177231

         if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.exception(tc, e);
		}
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "notifyClose");
   }
   
   /**
    * Helper method.  Returns the next id to be used for a proxy
    * queue.
    * @return short The next ID to use.
    * @throws SIResourceException Thrown if there are no more free Ids.
    */
   private short nextId() throws SIResourceException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "nextId");
      short id;
      try
      {
         id = idAllocator.allocateId();
      }
      catch (IdAllocatorException e)
      {
         // No FFDC code needed
         SIResourceException resourceException = new SIResourceException(
            nls.getFormattedMessage("MAX_SESSIONS_REACHED_SICO1019", new Object[]{""+Short.MAX_VALUE}, null)  // d192293
         );
         resourceException.initCause(e);
         throw resourceException;
      } 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "nextId", ""+id);
      return id;
   }
   
   /**
    * Helper method.  Checks if this group has been closed and throws
    * an exception if it has.
    * @throws SIIncorrectCallException
    */
   private void checkClosed() throws SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "checkClosed", ""+closed);
      if (closed) 
         throw new SIIncorrectCallException(
            TraceNLS.getFormattedMessage(CommsConstants.MSG_BUNDLE, "PROXY_QUEUE_CONVERSATION_GROUP_CLOSED_SICO1059", null, "PROXY_QUEUE_CONVERSATION_GROUP_CLOSED_SICO1059") // D270373
         );
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "checkClosed");
   }
   
   /**
    * Closes this proxy queue conversation group.
    */
   public void close()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close");
      synchronized(this)
      {
         if (closed)
         {
            idToProxyQueueMap.clear();
            factory.groupCloseNotification(conversation, this);
         } 
      }      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * Invoked to notify the group that the conversation that backs it has gone away.
    * Iterate over the queues and notify them.
    */
   public void conversationDroppedNotification()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "conversationDroppedNotification");
      LinkedList<ProxyQueue> notifyList = null;
      synchronized(this)
      {
         // Make a copy of the map's values to avoid a concurrent modification
         // exception later.
         notifyList = new LinkedList<ProxyQueue>();
         notifyList.addAll(idToProxyQueueMap.values());
      }

      Iterator iterator = notifyList.iterator();
      while(iterator.hasNext())
      {
         ProxyQueue queue = (ProxyQueue)iterator.next();
         queue.conversationDroppedNotification();
      }
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "conversationDroppedNotification");
   }
}
