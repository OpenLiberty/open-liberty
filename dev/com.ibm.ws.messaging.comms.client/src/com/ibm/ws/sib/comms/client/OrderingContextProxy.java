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
package com.ibm.ws.sib.comms.client;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue;
import com.ibm.ws.sib.comms.common.CommsByteBuffer;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * This class represents the comms implementation of the message ordering context. A message order
 * context is used to ensure that message delivery occurs in a defined order.
 * <p>
 * A message order context can be used when sending a message and when asynchronously delivering a
 * message. When a message order context is used when sending a message, the server code knows that
 * it must execute all sends with the same context on the same thread - i.e. one after the other.
 * It can therefore also perform other operations for that session(s) on another thread.
 * <p>
 * When receiving messages asynchronously on sessions using the same message ordering context the
 * client code will guarentee to dispatch the callbacks serially on the same thread and the order
 * that the server side callbacks are dispatched (and thus the order we receive the messages across
 * the wire) will denote the order they are dispatched to the callbacks. We guarentee not to
 * dispatch callbacks at the same time, but they will be processed one after the other.
 * <p>
 * So as we clean up the server side ordering context object, this object maintains a use count that
 * is incremented and decremented by users of the ordering context. When the use count becomes 0, we
 * close the ordering context meaning that the server code can relinquish resources held for it.
 * However, at the moment it is perfectly legal for the application to re-use the ordering context
 * and so if the use count goes 1 -> 0 -> 1, the ordering context will be recreated automatically.
 *
 * @author Gareth Matthews
 */
public class OrderingContextProxy extends Proxy implements OrderingContext
{
   /** Class name for FFDC */
// Commented out as the class no longer takes FFDCs
//   private static String CLASS_NAME = OrderingContextProxy.class.getName();

   /** Register Class with Trace Component */
   private static final TraceComponent tc = SibTr.register(OrderingContextProxy.class,
                                                           CommsConstants.MSG_GROUP,
                                                           CommsConstants.MSG_BUNDLE);

   /** Log Source code level on static load of class */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/OrderingContextProxy.java, SIB.comms, WASX.SIB, uu1215.01 1.21");
   }

   /** This is the number of producers / async consumers using this ordering context */
   private int useCount = 0;

   /** Flag to indicate whether we are dead or alive */
   private boolean dead = false;

   // Queue that this context is associated with (for asynchronous message
   // delivery purposes) if any.
   private Queue queue;

   /**
    * Constructor that assigns an id to this order context
    *
    * @param con
    * @param cp
    *
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIErrorException
    */
   public OrderingContextProxy(Conversation con, ConnectionProxy cp)
          throws SIConnectionUnavailableException, SIConnectionDroppedException,
                         SIErrorException
   {
      super(con, cp);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>");
      setOrderingContextProxyId();
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }

   /**
    * Unit test constructor.
    *
    * @param id
    */
   public OrderingContextProxy(int id)
   {
      super (null, null);

      setProxyID((short) id);
   }

   /**
    * @return Returns the id for this ordering context - this is needed by code that flows this
    *         ordering context.
    */
   public short getId()
   {
      return getProxyID();
   }

   /**
    * @return Returns the use count for this ordering context
    */
   public int getUseCount()
   {
      return useCount;
   }

   /**
    * This method will increment the use count on this order context.
    *
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIErrorException
    */
   public synchronized void incrementUseCount()
          throws SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "incrementUseCount");

      // Check if this ordering conext instance is "dead". If it is, then this order context has been closed and
      // we need to obtain a new proxy id for it.
      if (dead) {
        setOrderingContextProxyId();
        dead = false;
      }

      useCount++;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Use count is now: " + useCount);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "incrementUseCount");
   }

   // Obtain and set an ordering context proxy id. Proxy ids can be obtained (i) from a cached pool held by the
   // owning connection which is cheap or (ii) from the messaging engine which is expensive.
   private void setOrderingContextProxyId () throws SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setOrderingContextProxyId");

     final ConnectionProxy cp = getConnectionProxy();
     final Short orderContext = cp.getOrderContext();

     if (orderContext != null) {
       setProxyID(orderContext.shortValue());
     } else {
       create();
     }

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setOrderingContextProxyId");
   }

   /**
    * This method will decrement the use count on this order context.
    *
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIErrorException
    */
   public synchronized void decrementUseCount()
          throws SIConnectionDroppedException, SIErrorException

   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "decrementUseCount");

      useCount--;
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Use count is now: " + useCount);

      // Now check the use count. If it is 0, we should close the order contex so that the server
      // side code can release the resources held by it.
      if (useCount == 0)
      {
         close();
         dead = true;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "decrementUseCount");
   }

   /**
    * This method actually creates an ordering context and assigns us an ID (given to us by the
    * server).
    *
    * @throws SIConnectionUnavailableException
    * @throws SIConnectionDroppedException
    * @throws SIErrorException
    */
   public void create()
          throws SIConnectionUnavailableException, SIConnectionDroppedException, SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "create");

      CommsByteBuffer request = getCommsByteBuffer();
      request.putShort(getConnectionObjectID());

      CommsByteBuffer reply = null;

      try
      {
         // Pass on call to server
         reply = jfapExchange(request,
                              JFapChannelConstants.SEG_CREATE_ORDER_CONTEXT,
                              JFapChannelConstants.PRIORITY_HIGH,
                              true);

         short err = reply.getCommandCompletionCode(JFapChannelConstants.SEG_CREATE_ORDER_CONTEXT_R);
         if (err != CommsConstants.SI_NO_EXCEPTION)
         {
            checkFor_SIConnectionUnavailableException(reply, err);
            checkFor_SIConnectionDroppedException(reply, err);
            checkFor_SIConnectionLostException(reply, err);
            checkFor_SIErrorException(reply, err);
            defaultChecker(reply, err);
         }

         short ocId = reply.getShort();
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Created the order proxy. ID: " + ocId);

         // Now save the id
         setProxyID(ocId);
      }
      catch (SIConnectionLostException e)
      {
         // No FFDC Code needed
         // Temporarily re-throw as dropped
         throw new SIConnectionDroppedException("", e);
      }
      finally
      {
         if (reply != null) reply.release();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "create");
   }

   /**
    * This method will close the ordering context. This method should ideally be called by the API
    * application. But at present, it is called by us when the use count is 0. The order context
    * is only returned to the connection pool of free order contexts as this avoids an exchange
    * with the server which is costly.
    *
    * @throws SIConnectionDroppedException
    * @throws SIErrorException
    */
   public void close()
          throws SIConnectionDroppedException, SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "close");

      queue = null;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Returning order context to the order context pool");
      final ConnectionProxy cp = getConnectionProxy();
      cp.addOrderContext(getProxyID());

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "close");
   }

   /**
    * @return Returns info about the order context
    */
   public String toString()
   {
      return "CommsOrderContext@" + Integer.toHexString(hashCode()) + ". Id: " + getProxyID() +
                 ". Use count: " + useCount + ". Dead: " + dead;
   }

   /**
    * Called by a proxy queue to associate itself with this ordering context.
    *
    * @param queueToAssociate
    */
   public void associateWithQueue(Queue queueToAssociate)
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "associateWithQueue", queueToAssociate);
      this.queue = queueToAssociate;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "associateWithQueue");
   }

   /**
    * @return Returns the queue currently associated with this ordering context.
    */
   public Queue getAssociatedQueue()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getAssociatedQueue");
      Queue result = queue;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getAssociatedQueue", result);
      return result;
   }
}
