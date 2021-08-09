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
package com.ibm.ws.sib.comms.client.proxyqueue.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.client.ConsumerSessionProxy;
import com.ibm.ws.sib.comms.client.Transaction;
import com.ibm.ws.sib.comms.client.proxyqueue.ProxyQueue;
import com.ibm.ws.sib.comms.client.proxyqueue.queue.Queue;
import com.ibm.ws.sib.comms.common.CommsUtils;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of the locked message enumeration used on the
 * client.
 */
public class LockedMessageEnumerationImpl implements LockedMessageEnumeration
{
   /** The trace */
   private static final TraceComponent tc = SibTr.register(LockedMessageEnumerationImpl.class, 
                                                           CommsConstants.MSG_GROUP, 
                                                           CommsConstants.MSG_BUNDLE);
   /** NLS */
   private static final TraceNLS nls = TraceNLS.getTraceNLS(CommsConstants.MSG_BUNDLE);
   
   /** Log source info on static load */
   static
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Source info: @(#) SIB/ws/code/sib.comms.client.impl/src/com/ibm/ws/sib/comms/client/proxyqueue/impl/LockedMessageEnumerationImpl.java, SIB.comms, WASX.SIB, uu1215.01 1.53");
   }
   
   /** The consumer session this enumeration was created in the context of */
   private ConsumerSessionProxy consumerSession = null;
   
   /** The messages enumerated over by this enumeration */
   private JsMessage[] messages = null;
   
   /** The next array index that will be returned when calling nextLocked() */
   private int nextIndex = 0;
   
   /** The conversation helper this iterator used to communicate with the ME sending it messages */
   ConversationHelper convHelper = null;
     
   /**
    * Has this enumeration been marked invalid.  As soon as a thread
    * returns from its asynchronous consumer callbackl, the enumeration
    * it was handed is marked as invalid.
    */
   private volatile boolean invalid = false;
      
   /** 
    * Callback thread that "owns" this enumeration.  This allows us to
    * enforce the "only valid in callback" rules.
    */
   private Thread owningThread = null;
   
   /** The object which we synchronize on which is given to us by the owning session */
   private Object lmeOperationMonitor;
   
   /**
    * Creates a new locked message enumeration.
    * 
    * @param descriptor
    * @param messages
    * @param owningThread
    */
   public LockedMessageEnumerationImpl(ProxyQueue proxyQueue,
                                       Queue queue,
                                       JsMessage[] messages,
                                       Thread owningThread,
                                       Object lmeOperationMonitor)                      // d187521.2.1
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"<init>", new Object[] {proxyQueue, queue, messages, owningThread});
      
      this.messages = new JsMessage[messages.length];
      System.arraycopy(messages, 0, this.messages, 0, messages.length);
      this.consumerSession = (ConsumerSessionProxy)proxyQueue.getDestinationSessionProxy();
      this.convHelper = proxyQueue.getConversationHelper();
      this.owningThread = owningThread;
      this.lmeOperationMonitor = lmeOperationMonitor;                // D249096
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"<init>");
   }
   
   /**
    * Unit test constructor - not to be used for production code.
    * 
    * @param convHelper
    * @param session
    * @param messages
    * @param owningThread
    */
   public LockedMessageEnumerationImpl(ConversationHelper convHelper,
                                       ConsumerSessionProxy session,
                                       JsMessage[] messages,
                                       Thread owningThread)
   {
      this.convHelper = convHelper;
      this.consumerSession = session;
      this.messages = new JsMessage[messages.length];
      System.arraycopy(messages, 0, this.messages, 0, messages.length);
      this.owningThread = owningThread;
      lmeOperationMonitor = new Object();                            // D249096
   }
	
   /**
    * Returns the next available locked message in the enumeration.
    * A value of null is returned if there is no next message.
    * 
    * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#nextLocked()
    */
   public SIBusMessage nextLocked()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"nextLocked");
      JsMessage retMsg = null;
      
      synchronized(lmeOperationMonitor)
      {
         checkValid();   
         
         // At this point we look at each item in the array up to end of the array for the next
         // non-null item. This is because some points in the array may be null if they have been
         // deleted or unlocked.
         while (nextIndex != messages.length)
         {
            retMsg = messages[nextIndex];
            nextIndex++;
            
            if (retMsg != null) break;
         }
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"nextLocked", retMsg);
      return retMsg;
	}

   /**
    * Unlocks the current message.
    * 
    * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent()
    */
   public void unlockCurrent()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIIncorrectCallException, SIMessageNotLockedException, 
             SIErrorException
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"unlockCurrent");
      
      synchronized(lmeOperationMonitor)
      {
         checkValid();
   
         // If nextIndex = 0 then we are pointing at the first item, therefore unlocking the current
         // is invalid. Also, if the message is null, then we must have deleted - therefore also
         // invalid.
         if ((nextIndex == 0) || (messages[nextIndex - 1] == null))
         {
            throw new SIIncorrectCallException(
               nls.getFormattedMessage("LME_UNLOCK_INVALID_MSG_SICO1017", null, null)
            );
         }
         
         JsMessage retMsg = messages[nextIndex - 1];
         
         if (CommsUtils.isRecoverable(retMsg, consumerSession.getUnrecoverableReliability()))
         {
            convHelper.unlockSet(new SIMessageHandle[] {retMsg.getMessageHandle()});
         }
         
         messages[nextIndex - 1] = null;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"unlockCurrent");
	}

   /**
    * Deletes the current message.
    * 
    * @param transaction
    * 
    * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteCurrent(SITransaction)
    */
   public void deleteCurrent(SITransaction transaction)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException, 
             SIErrorException
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"deleteCurrent", transaction);
      
      synchronized(lmeOperationMonitor)
      {
         checkValid();
         
         // If nextIndex = 0 then we are pointing at the first item, therefore unlocking the current
         // is invalid. Also, if the message is null, then we must have deleted - therefore also
         // invalid.
         if ((nextIndex == 0) || (messages[nextIndex - 1] == null))
         {
            throw new SIIncorrectCallException(
               nls.getFormattedMessage("LME_DELETE_INVALID_MSG_SICO1018", null, null)
            );
         }
         
         JsMessage retMsg = messages[nextIndex - 1];
         
         // Only flow the delete if it would not have been done on the server already.
         // This will have been done if the message is determined to be 'unrecoverable'.
         if (CommsUtils.isRecoverable(retMsg, consumerSession.getUnrecoverableReliability()))
         {
            // Start d181719
            JsMessage[] msgs = new JsMessage[] {retMsg};         // f191114
            
            if (transaction != null)
            {
               synchronized (transaction)
               {
                  // Check transaction is in a valid state.
                  // Enlisted for an XA UOW and not rolledback or
                  // completed for a local transaction.
                  if (!((Transaction)transaction).isValid())
                  {
                     throw new SIIncorrectCallException(
                        nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1022", null, null)
                     );
                  }
                  
                  deleteMessages(msgs, transaction);             // f191114
               }
            }
            else
            {
               deleteMessages(msgs, null);                       // f191114
            }
            // End d181719
         }
         
         // Now remove it from the LME
         messages[nextIndex - 1] = null;
         
      }  
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"deleteCurrent");
	}

   /**
    * Deletes all messages seen so far.
    * 
    * @param transaction
    * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteSeen(SITransaction)
    */
   public void deleteSeen(SITransaction transaction)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException, 
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"deleteSeen", transaction);
      
      synchronized(lmeOperationMonitor)
      {
         
         checkValid();
               
         // If we have seen any messages....
         int numSeenMsgs = getSeenMessageCount();
         
         if (numSeenMsgs > 0)
         {
            // Start d181719
            JsMessage[] seenRecoverableMessages = new JsMessage[numSeenMsgs];
            int numOfMessagesNeedingDeleting = 0;
            
            // Go through each seen message
            for (int i=0; i < nextIndex; ++i)
            {
               if (messages[i] != null)
               {
                  // Only add the message to the list of messages to delete if it is recoverable
                  if (CommsUtils.isRecoverable(messages[i], consumerSession.getUnrecoverableReliability()))                               // f177889
                  {                                                                 // f177889
                     seenRecoverableMessages[numOfMessagesNeedingDeleting] = messages[i];         // f177889
                     ++numOfMessagesNeedingDeleting;
                  }                                                                 // f177889
                  
                  // Delete it from the main pile
                  messages[i] = null;
               }
            }
            
            if (numOfMessagesNeedingDeleting > 0)
            {
               if (transaction != null)
               {
                  synchronized (transaction)
                  {
                     // Check transaction is in a valid state.
                     // Enlisted for an XA UOW and not rolledback or
                     // completed for a local transaction.
                     if (!((Transaction) transaction).isValid())
                     {
                        throw new SIIncorrectCallException(
                           nls.getFormattedMessage("TRANSACTION_COMPLETE_SICO1022", null, null)
                        );
                     }
                     
                     deleteMessages(seenRecoverableMessages, transaction);
                  }
               }
               else
               {
                  deleteMessages(seenRecoverableMessages, null);
               }
            }
            // End d181719
         }
         // End f191114
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"deleteSeen");
   }
   
   // Start f191114
   // Start d181719
   /**
    * This private method actually performs the delete by asking the conversation helper
    * to flow the request across the wire. However, this method does not obtain any locks
    * required to perform this operation and as such should be called by a method that does
    * do this.
    * 
    * @param messagesToDelete
    * @param transaction
    * 
    * @throws SICommsException
    */
   private void deleteMessages(JsMessage[] messagesToDelete, SITransaction transaction)
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, SILimitExceededException,
             SIIncorrectCallException, SIMessageNotLockedException, 
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"deleteMessages");
      
         int priority = JFapChannelConstants.PRIORITY_MEDIUM;                      // d178368
   
         if (transaction != null) {
            Transaction commsTransaction = (Transaction)transaction;
            priority = commsTransaction.getLowestMessagePriority();   // d178368  // f181927
            
            // Inform the transaction that our consumer session has deleted
            // a recoverable message under this transaction. This means that if
            // a rollback is performed (and strict rollback ordering is enabled)
            // we can ensure that this message will be redelivered in order.
            commsTransaction.associateConsumer(consumerSession);            
         }
         
         // begin F219476.2
         SIMessageHandle[] messageHandles = new SIMessageHandle[messagesToDelete.length];
         for (int x = 0; x < messagesToDelete.length; x++)                       // f192215
         {
            if (messagesToDelete[x] != null)
            {
               messageHandles[x] = messagesToDelete[x].getMessageHandle();
            }
         }
         convHelper.deleteMessages(messageHandles, transaction, priority);                // d178368
         // end F219476.2
//      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"deleteMessages");
   }
   // End d181719
   // End f191114

   // Start d172528
   /**
    * This method will reset the cursor and allow the LME to be traversed again. Note that any
    * messages that were deleted or unlocked will not be available again.
    */
   public void resetCursor()    
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException,
             SIResourceException, SIConnectionLostException, 
             SIErrorException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"resetCursor");

      nextIndex = 0;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"resetCursor");
   }

   // Start D202977
   /**
    * Returns the amount of messages left in the locked message enumeration.
    * 
    * @return int
    */
   public int getRemainingMessageCount()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"getRemainingMessageCount");
      checkValid();
      
      int remain = getUnSeenMessageCount();
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"getRemainingMessageCount", ""+remain);
      return remain;
   }
   // End D202977
   // end d172528
   
   /**
    * Private method to determine how many messages have been seen and not unlocked or deleted. It
    * does this by looking at the number of non-null elements before the current item.
    * <p>
    * Note that if an item was seen but then the cursor was reset it will become unseen again 
    * until it is viewed with nextLocked().
    * 
    * @return Returns the number of seen items that have not been deleted or unlocked.
    */
   private int getSeenMessageCount()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"getSeenMessageCount");
      
      int seenMsgs = 0;
      for (int x = 0; x < nextIndex; x++)
      {
         if (messages[x] != null) seenMsgs++;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"getSeenMessageCount", ""+seenMsgs);
      return seenMsgs;
   }
   
   /**
    * Private method to determine how many messages have not been seen as yet. It does this by 
    * looking at the number of non-null elements from the current item to the end of the array.
    * 
    * @return Returns the number of unseen items.
    */
   private int getUnSeenMessageCount() 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"getUnseenMessageCount");
      
      int remain = 0;
      for (int x = nextIndex; x < messages.length; x++)
      {
         if (messages[x] != null) remain++;
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"getUnseenMessageCount", ""+remain);
      return remain;
   }

   // Start D218666.1
   /**
    * This method is used to unlock any messages that were not viewed by using nextLocked()
    * as the Core SPI spec says that any unseen messages should be implicitly unlocked.
    */
   // begin F219476.2
   public void unlockUnseen() 
      throws SIResourceException, SIConnectionDroppedException, SIConnectionLostException,   // F247845
             SIIncorrectCallException, SIMessageNotLockedException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"unlockUnseen");
    
      // The conversation helper needs a properly sized array of id's, so first create an array
      // that is as large as we'll ever need it to be, populate it and then resize it.
      SIMessageHandle[] idsToUnlock = new SIMessageHandle[getUnSeenMessageCount()];

      int arrayPos = 0;
      for (int startingIndex = nextIndex; startingIndex < messages.length; startingIndex++)
      {
         if (messages[startingIndex] != null)
         {
            // Start F247845
            if (CommsUtils.isRecoverable(messages[startingIndex], consumerSession.getUnrecoverableReliability()))
            {
               idsToUnlock[arrayPos] = messages[startingIndex].getMessageHandle();
               arrayPos++;
            }
            // End F247845
            
            // Delete it from the main pile
            messages[startingIndex] = null;
         }
      }
      
      //Resize array to prevent NPEs.
      if(idsToUnlock.length != arrayPos)
      {
         if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "compacting array");
         
         final SIMessageHandle[] tempArray = new SIMessageHandle[arrayPos];
         System.arraycopy(idsToUnlock, 0, tempArray, 0, arrayPos);
         idsToUnlock = tempArray;
      }
      
      convHelper.unlockSet(idsToUnlock);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"unlockUnseen");
   }
   // end F219476.2
   // End D218666.1

   /**
    * Returns the consumer session this enumeration contains messages
    * delivered to.
    * 
    * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getConsumerSession()
    */
   public ConsumerSession getConsumerSession()
      throws SISessionUnavailableException, SISessionDroppedException,
             SIConnectionUnavailableException, SIConnectionDroppedException
	{
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"getConsumerSession");          // f173765.2
      checkValid();                                                            // f173765.2
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"getConsumerSession");           // f173765.2
      
      return consumerSession;
	}

   /**
    * Helper function.  Determines if the session (or connection)
    * associated with this locked message enumeration is valid or not.
    * A number of things can make a locked message enumeration invalid,
    * including it being closed, or used from outside the consume
    * messages callback.  If the enumeration is invalid, an
    * invalid state for operation exception is thrown.
    * 
    * @throws SISessionUnavailableException
    */
   private void checkValid() throws SISessionUnavailableException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"checkValid");
      
      // Start f
      if (invalid ||                                     // If the callback has finished
          consumerSession.isClosed() ||                  // If the parent session is closed
          (Thread.currentThread() != owningThread))      // If we are on a different thread
      {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc,"not valid", this);
         invalid = true;
         throw new SISessionUnavailableException(null);
      }
      // End f
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"checkValid");
   }
   
   /**
    * Marks the enumeration as invalid.  This is called once the asynchronous
    * consumer callback returns to ensure that this locked message enumeration
    * cannot be used from outside the callback.
    *
    */
   protected void markInvalid()
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"markInvalid");
      invalid = true;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"markInvalid");
   }
   
   /**
    * @return Returns info about this LME.
    */
   public String toString()
   {
      String arrContents = "[ ";
      for (int x = 0; x < messages.length; x++)
      {
         if (x == nextIndex)
         {
            arrContents += "* " + messages[x];
         }
         else
         {
            arrContents += messages[x];
         }
         
         if (x == ((messages.length) - 1 ))
         {
            arrContents += " ]";
         }
         else
         {
            arrContents += ", ";
         }
      }
      
      return "LockedMessageEnumerationImpl@" + Integer.toHexString(hashCode()) + 
             "- contents: " + arrContents +
             ", invalid: " + invalid + 
             ", nextIndex: " + nextIndex +
             ", consumerSession: " + consumerSession +
             ", owningThread: " + owningThread + 
             ", currentThread: " + Thread.currentThread();
   }

   // being F219476.2
   /** @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#hasNext() */
   public boolean hasNext() 
   throws SISessionUnavailableException, 
          SISessionDroppedException, 
          SIConnectionUnavailableException, 
          SIConnectionDroppedException, 
          SIIncorrectCallException
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc,"hasNext");
      
      boolean hasNext = getRemainingMessageCount() > 0;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc,"hasNext", ""+hasNext);
      return hasNext;
   }
   // end F219476.2
}
