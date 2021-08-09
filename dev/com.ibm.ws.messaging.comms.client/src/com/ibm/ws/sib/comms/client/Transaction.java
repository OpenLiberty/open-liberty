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
package com.ibm.ws.sib.comms.client;

import java.util.HashSet;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.jfapchannel.Conversation;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class acts as a super class for all transaction proxy classes. It provides
 * common functions such as storing the transaction Id and the lowest message 
 * priorities that all types of transactions need.
 * 
 * @author Gareth Matthews
 */
public abstract class Transaction extends Proxy
{
   /** Register Class with Trace Component */
   private static final TraceComponent tc =
      SibTr.register(Transaction.class, CommsConstants.MSG_GROUP, CommsConstants.MSG_BUNDLE);

   /** Class name for FFDCs */
   private static final String CLASS_NAME = Transaction.class.getName();
         
   /** The lowest priority message that has been sent to the server */
   protected short lowestPriority = JFapChannelConstants.PRIORITY_HIGH;

   /** Log Source code level on static load of class */
   static 
   {
      if (tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#)SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/Transaction.java, SIB.comms, WASX.SIB, uu1215.01 1.22");
   }

   /** 
    * The transaction Id 
    * 
    * NOTE: We could have used the proxy Id field for this value but
    * this is a short and we need 32 bits for the TX Id. Therefore,
    * do not call getProxyID(), but use getTransactionId instead.
    */
   private int localTranasctionId = 0;
   
   /** Whether we need to return all undelivered messages to the ME when a rollback occurs */
   private final boolean strictRedeliveryOrdering;
   
   /** A list of all consumer sessions that need to be informed of a rollback.
    *  Initialized in constructor.*/
   private final HashSet associatedConsumers;
   
   /** Synchronization class for consumersToRollback */
   private static final class AssociatedConsumersLock {}
   
   /** Synchronization object for consumersToRollback.
    *  We use separate synchronization to avoid the rollback method from needing
    *  to lock on this (which is not currently part of the locking strategy).
    *  Initialized in constructor.*/
   private final AssociatedConsumersLock associatedConsumersLock;
   
   /**
    * Contructor which assigns a transaction Id to this instance.
    * 
    * @param con
    * @param cp
    */
   public Transaction(Conversation con, ConnectionProxy cp)
   {
      super(con, cp);

      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {con, cp});
      
      // Here we need to assign a unique ID for the transaction
      // so get that from the link level state.
      ClientLinkLevelState linkState = (ClientLinkLevelState) con.getLinkLevelAttachment();
      // And get the next ID
      localTranasctionId = linkState.getNextTransactionId();

      // Is strict redelivery ordering required after a rollback?
      strictRedeliveryOrdering = cp.getStrictRedeliveryOrdering();   
      if (strictRedeliveryOrdering) {
        associatedConsumers = new HashSet();
        associatedConsumersLock = new AssociatedConsumersLock();
      }
      else {
        associatedConsumers = null;
        associatedConsumersLock = null;
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
   }
   
   /**
    * This method will return the id that has been assigned to this local transaction.
    * 
    * @return Returns the assigned tranasction Id
    */
   public int getTransactionId()
   {
      return localTranasctionId;
   }
   
   /**
    * This method gets the lowest message priority being used in this transaction
    * and as such is the JFAP priority that commit and rollback will be sent as.
    * 
    * @return short
    */
   public short getLowestMessagePriority()
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "getLowestMessagePriority");
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "getLowestMessagePriority", ""+lowestPriority);
      return lowestPriority;      
   }
   
   /**
    * This method is used to update the lowest message priority
    * that has been sent on this transaction. The value passed in
    * is stored if it is lower than a previous value. Otherwise
    * it is ignored. The stored value is then used on the exchanges
    * sent when we commit or rollback. 
    * 
    * @param messagePriority
    */
   public void updateLowestMessagePriority(short messagePriority)
   {
      if (tc.isEntryEnabled()) SibTr.entry(this, tc, "updateLowestMessagePriority", 
                                           new Object[] {""+messagePriority});
      
      // Only update if the message priority is lower than another message
      if (messagePriority < this.lowestPriority)
      {
         if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Updating lowest priority");
         this.lowestPriority = messagePriority;
      }
      
      if (tc.isEntryEnabled()) SibTr.exit(this, tc, "updateLowestMessagePriority");
   }
 
   /**
    * Called each time a recoverable message is deleted from a consumer using a
    * proxy queue under this transaction, to allow the transaction to callback
    * inform the proxy queue it should purge any read-ahead messages if required.
    * 
    * No-op if strict redelivery ordering is disabled.
    */
   public void associateConsumer(ConsumerSessionProxy consumer) {
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(this, tc, "associateConsumer", new Object[]{consumer, Boolean.valueOf(strictRedeliveryOrdering)});
     
     // This is a no-op if strict redelivery ordering is disabled
     if (strictRedeliveryOrdering && consumer != null) {
       synchronized (associatedConsumersLock) {
         associatedConsumers.add(consumer);
       }
     }
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.exit(this, tc, "associateConsumer");     
   }
   
   /**
    * Inform all associated consumers that a rollback has occurred.
    * 
    * No-op if strict redelivery ordering is disabled.
    */
   public void informConsumersOfRollback() {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.entry(this, tc, "informConsumersOfRollback", new Object[]{Boolean.valueOf(strictRedeliveryOrdering)});
     
     if (strictRedeliveryOrdering) {
       // Take a copy of the set of consumers, to avoid additional locking
       // when we call into the consumers to inform them of a rollback.
       ConsumerSessionProxy[] consumersToNotify;
       synchronized(associatedConsumersLock) {
         consumersToNotify = 
           new ConsumerSessionProxy[associatedConsumers.size()];
         consumersToNotify = (ConsumerSessionProxy[])
           associatedConsumers.toArray(consumersToNotify);
       }
       
       // Callback each consumer to inform them of the rollback
       for (int i = 0; i < consumersToNotify.length; i++) {
         try {
           consumersToNotify[i].rollbackOccurred();
         }
         catch (SIException e) {
           // FFDC for the error, but do not re-throw.
           // Most likely the connection to the ME is unavailable, or the
           // consumer has been closed and cleaned up.
           // In these cases the consumer will not be able to consume any
           // more messages - so redelivery ordering is not an issue.
           FFDCFilter.processException(e, CLASS_NAME + ".informConsumersOfRollback",
               CommsConstants.TRANSACTION_INFORMCONSUMERSOFROLLBACK_01, this);
           if (tc.isDebugEnabled()) SibTr.debug(tc, "Encountered error informing consumer of rollback: " + consumersToNotify[i]);
           if (tc.isEventEnabled()) SibTr.exception(tc, e);
         }
       }
     }
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
       SibTr.exit(this, tc, "informConsumersOfRollback");     
   }

   public abstract boolean isValid();
}