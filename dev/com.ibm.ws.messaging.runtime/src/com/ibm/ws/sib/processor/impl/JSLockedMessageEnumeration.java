/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MPLockedMessageEnumeration;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPILockedMessageEnumeration;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JsMessageWrapper;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * This is a list of locked messages. It extends LinkedMap so that we can easily
 * iterate over the list from a given start position. There should be one LME
 * per ConsumerSession
 * 
 * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration
 */
public final class JSLockedMessageEnumeration extends AbstractLockedMessageEnumeration
  implements MPLockedMessageEnumeration, AlarmListener
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      JSLockedMessageEnumeration.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  private ConsumableKey consumerKey;

  //the LocalConsumerPoint to which this LME belongs
  private JSLocalConsumerPoint localConsumerPoint;
  //is the consumer remote from the localisation?

  /**
   * Create a new LockedMessageEnumeration which may be used to hold references
   * to messages locked by the given GetCursor, for consumption via the given
   * ConsumerSession.
   * 
   * @param consumerSession The ConsumerSession to which this LME is associated
   * @param getCursor The GetCursor used to lock the messages
   */
  JSLockedMessageEnumeration(
    JSLocalConsumerPoint localConsumerPoint,
    ConsumableKey consumerKey,
    MessageProcessor messageProcessor) 
  {
    super(localConsumerPoint,messageProcessor);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "JSLockedMessageEnumeration", 
        new Object[]{localConsumerPoint, consumerKey, messageProcessor});
            
    // Cache all the interesting things about our consumer
    this.localConsumerPoint = localConsumerPoint;
    this.consumerKey = consumerKey;    
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "JSLockedMessageEnumeration", this);
  }
  
  void removeMessageFromStore(JsMessageWrapper jsMsgWrapper,
                              TransactionCommon transaction,
                              boolean decrementActiveMessage)
    throws SIResourceException, SIIncorrectCallException, SIMPMessageNotLockedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "removeMessageFromStore", 
        new Object[]{jsMsgWrapper, transaction, Boolean.valueOf(decrementActiveMessage)});
    
    try
    {
      LocalTransaction localTransaction = null;
      
      SIMPMessage msg = (SIMPMessage) jsMsgWrapper; 
      if(transaction == null)
      {
        // Either create a local transaction if we have to (more than one thing needs
        // to be done, e.g. send a report message or send to multiple subscribers)
        if(!msg.isItemReference() && msg.getReportCOD() != null || msg.isRemoteGet())
        {
          localTransaction = txManager.createLocalTransaction(true);
          transaction = localTransaction;
        }
        // Or use the auto commit transaction if possible
        else
        {
          // As we hold the asynch consumer lock we can use the LCP autoCommitTran
          // if possible
          transaction = localConsumerPoint.getAutoCommitTransaction();
        }
      }
      else if (msg.isRemoteGet() && transaction.isAutoCommit())
      {
        // If the transaction is auto commit then we need to upgrade to a 
        // local transaction.
        localTransaction = txManager.createLocalTransaction(true);
        transaction = localTransaction;
      }
      
      // Register the message with the transaction if we have to
      if(!transaction.isAutoCommit())
        transaction.registerCallback(msg); 
      
      // If we're counting active messages and we have a transaction we need a callback
      // to decrement the count. We can't use the post commit/rollback events on the
      // message as by the time they are called the message could be owned by a different
      //consumer (who needs to register their callbacks) as a message is available before
      // the post phase of events.
      // Instead we register the consumer itself with the transaction, once for every
      // message.
      // WARNING: This cannot be done while the caller holds the LCP lock, either it isn't
      //          locked while this method is called or the transaction used is not autocommit
      //          and therefore the commit happens sometime later (without the lock being held)
      if(decrementActiveMessage && localConsumerPoint.isCountingActiveMessages())
        transaction.registerCallback(localConsumerPoint); 
      
      // Remove the message from the ItemStream (outside the LME lock in case
      // the remove fails and an unlock event is driven).
      Transaction msTran = txManager.resolveAndEnlistMsgStoreTransaction(transaction);
      
      // if ordered, set the current transaction
      if (localConsumerPoint.getConsumerManager().getDestination().isOrdered())
        // Set the current transaction to this one
        localConsumerPoint.
            getConsumerManager().
            setCurrentTransaction(msTran, this);
      
      try
      {
        // Remove message
        msg.remove(msTran, msg.getLockID()); // 172968
      }         
      catch (NotInMessageStore e)
      {
        // No FFDC code needed
        SibTr.exception(tc, e);
        // See defect 387591
        // We should only get this exception if the message was deleted via
        // the controllable objects i.e. the admin console. If we are here for
        // another reason that we have a real error.
        
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
            "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
            "1:232:1.8.1.10",
            e });
          
        SIMPMessageNotLockedException notLockedException =  new SIMPMessageNotLockedException(
            nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {
                    "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
                    "1:240:1.8.1.10",
                    e },
                    null),
                    new SIMessageHandle[] {msg.getMessage().getMessageHandle()});
        notLockedException.initCause(e.getCause());
        
        if (localTransaction != null)
        {              
          localTransaction.commit();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "removeMessageFromStore", notLockedException);
        throw notLockedException;
      }

      // If we created the transaction, we commit it
      if (localTransaction != null)
        localTransaction.commit();
    }
    catch (MessageStoreException e)
    {      
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration.removeMessageFromStore",
        "1:265:1.8.1.10",
        this);
      
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
          "1:272:1.8.1.10",
          e });
    
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(tc, "removeMessageFromStore", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
            "1:283:1.8.1.10",
            e },
          null),
        e);
    }
        
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(tc, "removeMessageFromStore");
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPLockedMessageEnumeration#relockCurrent()
   */
  protected SIBusMessage relockSavedMsg()   
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "relockSavedMsg", new Object[] {new Integer(hashCode()),this});
      
    SIBusMessage siBusMsg = null;
    
    synchronized(this)
    {
      boolean failedToGetMessage = false;
      
      // We can only relock the message if the cursor hasn't moved since we
      // unlocked it.
      if(currentUnlockedMessage != null)
      {
        // Get the message from the list again
        SIMPMessage msg = (SIMPMessage) currentUnlockedMessage.message;
        
        if(msg != null)
        {
          // Attempt to lock it again
          LockingCursor cursor = 
            consumerKey.getGetCursor(msg);
          
          try
          {
            if(msg.lockItemIfAvailable(cursor.getLockID()))
            {
              // We locked it!
              msg.eventLocked();
                siBusMsg = currentUnlockedMessage.getJsMessage();
                messageAvailable = true;
                currentUnlockedMessage = null;
              
              
            }
            else
            {
              failedToGetMessage = true;
            }
          }
          catch(Exception e)
          {     
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter
                .processException(
                    e,
                    "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration.relockSavedMsg",
                    "1:343:1.8.1.10", this);
                
            SibTr.exception(tc, e);
            // Set the msg to null and leave the handling of the retry to the calling method
            failedToGetMessage = true;
          }
          
          if (failedToGetMessage)
          {
            // We failed to lock/get it so pretend we never saw it and clean up our
            // reference to it
            msg = null;        
            removeMessage(currentUnlockedMessage);
            currentUnlockedMessage = null;
          }
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "relockSavedMsg", siBusMsg);
    
    return siBusMsg;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent()
   */
  public void unlockCurrent() 
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, 
         SIIncorrectCallException,
         SIErrorException, SIMPMessageNotLockedException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", 
        new Object[] { new Integer(hashCode()), this});
    
    unlockCurrent(false);

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", this);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#unlockCurrent(boolean)
   */
  public void unlockCurrent(boolean redeliveryCountUnchanged) 
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, 
         SIIncorrectCallException,
         SIErrorException, SIMPMessageNotLockedException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", 
        new Object[] { new Integer(hashCode()), this, new Boolean(redeliveryCountUnchanged)});

    checkValidState("unlockCurrent");
          
    localConsumerPoint.checkNotClosed();  
    SIMPMessageNotLockedException notLockedException = null;
    
    synchronized(this)
    {  
      //check that there is a valid current message
      checkCurrentMessageAvailable(null);
      
      if(localConsumerPoint.getConsumerManager().getDestination().isOrdered())
      {
        unlockAll();
      }
      else
      {
        if(currentMsg != null)
        {
          // Remember that we've just unlocked this message, they may want to re-lock it
          // later
          currentUnlockedMessage = currentMsg;
  
          // We only actualy need to unlock it if it's actually in the MS
          if(currentMsg.isStored)
          {
            SIMPMessage msg = (SIMPMessage) (currentMsg.message);
            try
            {
              if (msg != null)
              {
                msg.unlockMsg(msg.getLockID(), null, !redeliveryCountUnchanged);
              }
            }
            catch (NotInMessageStore e)
            {
              // No FFDC code needed
              SibTr.exception(tc, e);
              // See defect 387591
              // We should only get this exception if the message was deleted via
              // the controllable objects i.e. the admin console. If we are here for
              // another reason that we have a real error.
              
              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                  new Object[] {
                  "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
                  "1:445:1.8.1.10",
                  e });
                
              notLockedException =  new SIMPMessageNotLockedException(
                  nls.getFormattedMessage(
                      "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                      new Object[] {
                          "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
                          "1:453:1.8.1.10",
                          e },
                          null),
                          new SIMessageHandle[] {msg.getMessage().getMessageHandle()});
              notLockedException.initCause(e.getCause());
            }
            catch (MessageStoreException e)
            {
              // MessageStoreException shouldn't occur so FFDC.
              FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration.unlockCurrent",
                "1:465:1.8.1.10",
                this);
          
              SibTr.exception(tc, e);
              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
                  "1:472:1.8.1.10",
                  e });
          
              if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
                SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", e);
  
              throw new SIResourceException(
                nls.getFormattedMessage(
                  "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
                    "1:483:1.8.1.10",
                    e },
                  null),
                e);
            }
          }
        }
      } // synchronized
  
          
      // Decrement the number of active messages for this consumer (outside the LME lock)
      localConsumerPoint.removeActiveMessages(1);
      
      if (notLockedException != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
          SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", this);
        
        throw notLockedException;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "unlockCurrent", this);
  }
   
  protected void unlockMessage(JsMessageWrapper jsMsg, boolean bumpRedeliveryCount) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockMessage", 
        new Object[] { jsMsg, Boolean.valueOf(bumpRedeliveryCount)});
    
    try
    {
      SIMPMessage msg = (SIMPMessage) jsMsg;
      if (msg != null)
      {
        try
        {
          msg.unlockMsg(msg.getLockID(),null,bumpRedeliveryCount);
        }
        catch (NotInMessageStore e)
        {
          // No FFDC code needed
          SibTr.exception(tc, e);
          // See defect 387591
          // We should only get this exception if the message was deleted via
          // the controllable objects i.e. the admin console. If we are here for
          // another reason that we have a real error.
        }
      }
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration.unlockMessage",
        "1:540:1.8.1.10",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
          "1:547:1.8.1.10",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "unlockMessage", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.JSLockedMessageEnumeration",
            "1:558:1.8.1.10",
            e },
          null),
        e);
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockMessage", this);
  }

  void handleSessionDroppedException(SISessionDroppedException e)
  {
    //No-op
    //SISessionDroppedExceptions don't get throw by the JS code
    //it's the RMQ code which needs this.
  }

  boolean isRMQ()
  {
    return false;
  }
}
