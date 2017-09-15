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

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.JsMessageHandle;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MPLockedMessageEnumeration;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPILockedMessageEnumeration;
import com.ibm.ws.sib.processor.impl.interfaces.JsMessageWrapper;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * This is a list of locked messages. It extends LinkedMap so that we can easily
 * iterate over the list from a given start position. There should be one LME
 * per ConsumerSession
 * 
 * LOCKING:
 *   
 *   The LME uses itself as a lock, this is below the 'owning' consumer's locks.
 *   
 *   There are two types of methods on an LME, the methods that get called from
 *   inside a LCP.consumeMessages() method (e.g. nextLocked(), deleteCurrent(), etc.)
 *   and those that can get called at a different time (e.g. alarm(), deleteSet(), etc.).
 *   
 *   The first will be called while the LCP is already locked down so anything
 *   can be performed under the LME lock. The latter methods will not hold any
 *   LCP locks so any operation performed by the LME that may invoke a LCP method
 *   (e.g. changing the state of the message, like unlocking it, so that the ConsumerDispatcher
 *   may redeliver it to the same LCP) cannot be performed under the LME lock. Instead
 *   these operations must be performed outside of the LME lock.
 *
 * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration
 */
abstract class AbstractLockedMessageEnumeration
  implements MPLockedMessageEnumeration, AlarmListener
{
  /**
   * A class to encapsulate a locked message
   */
  final class LMEMessage
  {
    protected long expiryTime = 0;
    protected long expiryMsgReferenceTime = 0;
    protected long id;
    protected boolean isRecoverable;
    protected boolean isStored;
    protected JsMessage jsMessage = null;
    protected boolean lockExpired;
    protected JsMessageWrapper message;
    protected LMEMessage next;
    protected BifurcatedConsumerSessionImpl owner = null;
    protected LMEMessage previous;

    protected SIBUuid8 uuid;
    protected boolean wasRead = false;

    LMEMessage(long key, SIBUuid8 uuid, JsMessageWrapper message, boolean isStored, boolean isRecoverable, long messageLockExpiry, long messageReferenceExpiry)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc,
                    "LMEMessage",
                    new Object[]{Long.valueOf(key),
                                 uuid,
                                 message,
                                 Boolean.valueOf(isStored),
                                 Boolean.valueOf(isRecoverable),
                                 Long.valueOf(messageLockExpiry),
                                 Long.valueOf(messageReferenceExpiry)});

      this.message = message;
      this.id = key;
      this.uuid = uuid;
      this.isStored = isStored;
      this.isRecoverable = isRecoverable;
      this.lockExpired = false;

      if((messageLockExpiry != 0) && isRecoverable)
        this.expiryTime = System.currentTimeMillis() + messageLockExpiry;

      if (messageReferenceExpiry != 0)
        this.expiryMsgReferenceTime = System.currentTimeMillis() + messageReferenceExpiry;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "LMEMessage", this);
    }

    public JsMessage getJsMessage() throws SIResourceException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "getJsMessage");

      if (jsMessage == null)
      {
        this.jsMessage = message.getMessage();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getJsMessage", jsMessage);

      return jsMessage;
    }



    public void reuseMessage(long key, SIBUuid8 uuid, JsMessageWrapper message, boolean isStored, boolean isRecoverable, long messageLockExpiry, long messageReferenceExpiry)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc,
                    "reuseMessage",
                    new Object[]{new Long(key),
                                 uuid,
                                 message,
                                 new Boolean(isStored),
                                 new Boolean(isRecoverable),
                                 new Long(messageLockExpiry),
                                 new Long(messageReferenceExpiry)});

      this.message = message;
      this.wasRead = false;
      this.jsMessage = null; // Blank this out, someone may have set it after it was pooled
      this.id = key;
      this.uuid = uuid;
      this.isStored = isStored;
      this.isRecoverable = isRecoverable;
      this.lockExpired = false;

      if((messageLockExpiry != 0) && isRecoverable)
        this.expiryTime = System.currentTimeMillis() + messageLockExpiry;

      if (messageReferenceExpiry != 0)
        this.expiryMsgReferenceTime = System.currentTimeMillis() + messageReferenceExpiry;

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reuseMessage", this);
    }

    public String toString()
    {
      return "Key:" + uuid + "_" + id + " Str:" + isStored + " Rec:" + isRecoverable +
             " ExpTime:" + expiryTime + " MessageRefExpTime:" + expiryMsgReferenceTime + " LckExp:" + lockExpired + " Owner:" + ((owner != null) ? owner.hashCode() : 0) +
             " Msg:" + message + "\n";
    }
  } // LMEMessage inner class


  // inner class for the MessageReferenceExpiry alarm
  final class MessageReferenceExpiryAlarm implements AlarmListener
  {
    public void alarm(Object object)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alarm", new Object[]{this, nextMsgReferenceToExpire, object});

      long currentTime = System.currentTimeMillis();

      synchronized (AbstractLockedMessageEnumeration.this)
      {
        while(nextMsgReferenceToExpire != null)
        {
          // Check that the timer popped at the right time for this message
          if(nextMsgReferenceToExpire.expiryMsgReferenceTime <= currentTime)
          {
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
              SibTr.debug(tc, "alarm", "Removing the msg refernence on: " + nextMsgReferenceToExpire);
            }

            try
            {
              // remove the referenced jsmsg from the msgitem
              if (nextMsgReferenceToExpire.message != null)
              {
                ((SIMPMessage)nextMsgReferenceToExpire.message).releaseJsMessage();
              }
            }
            catch (Exception e)
            {
              // No FFDC code needed
              // We failed to release the message, this isn't the end of the world. We should
              // log the exception and carry on.
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Got exception when releasing JsMessage from the message item: " + e);
            }

            // Remove the message from the element but keep the element in the list. A Core SPI
            // user still has to delete these messages from the LME even if they've expired.
            nextMsgReferenceToExpire.jsMessage = null;

            // Move on to the next expiring message in the list (if any)
            nextMsgReferenceToExpire = nextMsgReferenceToExpire.next;
          }
          // Otherwise, the timer popped too soon (the message the alarm was registered for
          // has probably been deleted) this will be the next lock to expire, break out and
          // re-register the alarm
          else
            break;
        } // while

        // If there is a lock to expire in the future register an alarm for it
        if(nextMsgReferenceToExpire != null)
        {
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "alarm", "Registering Msg Reference Expiry alarm for: " + nextMsgReferenceToExpire);
          alarmManager.create((nextMsgReferenceToExpire.expiryMsgReferenceTime - currentTime), this);
        }
        else
        {
          msgReferenceAlarmRegistered = false;
        }
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alarm",this);
   }
  }

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // NLS for CWSIR component
  private static final TraceNLS nls_cwsir =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIR_RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
        AbstractLockedMessageEnumeration.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  private MPAlarmManager alarmManager = null;
  private boolean alarmRegistered = false;
  private boolean msgReferenceAlarmRegistered = false;
  private LMEMessage callbackEntryMsg;
  //the ConsumerSession to which this LME belongs
  protected ConsumerSessionImpl consumerSession;
  protected LMEMessage currentMsg;

  protected LMEMessage currentUnlockedMessage;

  private boolean endReached;
  protected LMEMessage firstMsg;

  protected boolean isPubsub = false;
  protected LMEMessage lastMsg;
  //the LocalConsumerPoint to which this LME belongs
  private LocalConsumerPoint localConsumerPoint;
  //is a message available via nextLocked etc
  protected boolean messageAvailable = false;

  private long messageLockExpiry = 0;
  protected MessageProcessor messageProcessor;
  private LMEMessage nextMsgToExpire;
  private LMEMessage nextMsgReferenceToExpire;
  private int pooledCount;
  private LMEMessage pooledMsg;

  // Max number of pooled LMEMessage objects, they're only small so there's
  // no need to configure this
  private static final int poolSize = 20;

  protected SIMPTransactionManager txManager;

  // true if currently in a consumeMessages callback
  private boolean validState = false;

  private boolean setWaitTime = false;

  private boolean copyMsg = false;

  /**
   * Create a new LockedMessageEnumeration which may be used to hold references
   * to messages locked by the given GetCursor, for consumption via the given
   * ConsumerSession.
   *
   * @param consumerSession The ConsumerSession to which this LME is associated
   * @param getCursor The GetCursor used to lock the messages
   */
  AbstractLockedMessageEnumeration(
    LocalConsumerPoint localConsumerPoint,
    MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AbstractLockedMessageEnumeration",
        new Object[]{localConsumerPoint, messageProcessor});

    firstMsg = null;
    lastMsg = null;
    callbackEntryMsg = null;
    currentMsg = null;
    pooledMsg = null;
    currentUnlockedMessage = null;
    pooledCount = 0;
    endReached = false;
    nextMsgToExpire = null;
    nextMsgReferenceToExpire = null;

    // Cache all the interesting things about our consumer
    this.localConsumerPoint = localConsumerPoint;
    consumerSession = localConsumerPoint.getConsumerSession();
    this.messageProcessor = messageProcessor;
    this.txManager = messageProcessor.getTXManager();
    alarmManager = messageProcessor.getAlarmManager();
    copyMsg = ((ConnectionImpl) consumerSession.getConnectionInternal()).getMessageCopiedWhenReceived();
    setWaitTime = ((ConnectionImpl) consumerSession.getConnectionInternal()).getSetWaitTimeInMessage();
    isPubsub = localConsumerPoint.getConsumerManager().getDestination().isPubSub();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AbstractLockedMessageEnumeration", this);
  }

  /**
   * Add a new message to te end of the LME
   * @param id
   * @param message
   * @param isStored
   * @param isRecoverable
   * @throws SIResourceException
   */
  void addNewMessage(JsMessageWrapper message, boolean isStored, boolean isRecoverable)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,
                  "addNewMessage",
                  new Object[] { new Integer(hashCode()),
                                 message,
                                 new Boolean(isStored)});

    JsMessage jsMsg = message.getMessage();

    long id = jsMsg.getSystemMessageValue();
    SIBUuid8 uuid = jsMsg.getSystemMessageSourceUuid();

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc,
                  "addNewMessage",
                  new Object[] { new Integer(hashCode()),
                                 new Long(jsMsg.getSystemMessageValue()),
                                 jsMsg.getSystemMessageSourceUuid(),
                                 message,
                                 new Boolean(isStored)});

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(localConsumerPoint, tc, "verboseMsg OUT : " + message.getMessage().toVerboseString());

    synchronized(this)
    {
      LMEMessage newElement = null;
      long messageReferenceExpiry = messageProcessor.getCustomProperties().get_message_reference_expiry_value();

      // We have to create an object if the pool is empty
      if(pooledMsg == null)
      {
        newElement = new LMEMessage(id, uuid, message, isStored, isRecoverable, messageLockExpiry, messageReferenceExpiry);
      }
      // Otherwise pop the first pooled object and use that
      else
      {
        newElement = pooledMsg;
        pooledMsg = pooledMsg.next;
        newElement.next = null;
        pooledCount--;

        newElement.reuseMessage(id, uuid, message, isStored, isRecoverable, messageLockExpiry, messageReferenceExpiry);
      }

      // Add the message to the end of the list
      newElement.previous = lastMsg;
      if(lastMsg != null)
        lastMsg.next = newElement;
      else
        firstMsg = newElement;
      lastMsg = newElement;

      // If the locks are set to expire and this is the first message we need to
      // register an alarm
      if(messageLockExpiry != 0)
      {
        // If there are no other messages expiring, this will be the first one
        if(nextMsgToExpire == null)
          nextMsgToExpire = newElement;

        // If we don't have an alarm registered, register one now
        if(!alarmRegistered)
        {
          if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "addNewMessage", "Registering MessageLock Expiry alarm for: " + nextMsgToExpire);

          alarmManager.create(messageLockExpiry, this);
          alarmRegistered = true;
        }
      }

      if (messageReferenceExpiry != 0)
      {
        if (messageLockExpiry != 0 && messageReferenceExpiry > messageLockExpiry)
        {
          // The messageReferenceExpiry is greater than the messageLockExpiry. There is no
          // point in creating the alarm for messageReferenceExpiry as the message lock expiry
          // will always go first.
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          {
            SibTr.debug(tc, "MessageReferenceExpiry: "+messageReferenceExpiry+" is greater than messageLockExpiry: "+messageLockExpiry);
            SibTr.debug(tc, "MessageReferneceExpiry Alarm not registered");
          }
        }
        else
        {
          try
          {
            if (message.getReportCOD() == null)
            {
              // Now register the message reference expiry alarm
              // If there are no other message references expiring, this will be the first one
              if(nextMsgReferenceToExpire == null)
                nextMsgReferenceToExpire = newElement;

              // If we don't have an alarm registered, register one now
              if(!msgReferenceAlarmRegistered)
              {
                if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  SibTr.debug(tc, "addNewMessage", "Registering MessageReference Expiry alarm for: " + nextMsgReferenceToExpire);

                alarmManager.create(messageReferenceExpiry, new MessageReferenceExpiryAlarm());
                msgReferenceAlarmRegistered = true;
              }
            }
          }
          catch(SIResourceException e)
          {
            // No FFDC code needed

            // There was a problem getting hold of the ReportCOD of the message
            // assume it is set and don't create the expiry alarm.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Thrown exception when trying to create msg ref expiry alarm: " + e);
          }
        }
     }

      // By adding a message we must be about to call consumeMessages and therefore
      // we're in a valid state for calls
      validState = true;
    } // synchronized

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNewMessage", this);
  }

  public void alarm(Object object)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "alarm", new Object[]{this, nextMsgToExpire, object});

    ArrayList<JsMessageWrapper> messagesToUnlock = null;
    int unlockedActiveMessages = 0;

    synchronized(this)
    {
      long currentTime = System.currentTimeMillis();

      while(nextMsgToExpire != null)
      {
        // If the lock is too old, unlock it
        if(nextMsgToExpire.expiryTime <= currentTime)
        {
          // We only expire messages with an expiry time, messages with no expiry time
          // either never had one set or the message has been read, which cancels the expiry
          // on the lock.
          if(nextMsgToExpire.expiryTime != 0)
          {
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "alarm", "Expiring lock on: " + nextMsgToExpire);

            // We can't unlock the message here as we hold the LME lock
            // and the unlock could cause the message to be redelivered
            // to the same consumer which will attempt to take the LCP
            // lock - which breaks the lock hierarchy.

            // Build an list of messages to unlock once the LME lock is
            // released
            if(messagesToUnlock == null)
              messagesToUnlock = new ArrayList<JsMessageWrapper>();

            messagesToUnlock.add(nextMsgToExpire.message);
            
            // Rememeber we unlocked it
            nextMsgToExpire.lockExpired = true;

            // Remove the message from the element but keep the element in the list. A Core SPI
            // user still has to delete these messages from the LME even if they've expired.
            nextMsgToExpire.isStored = false;
            nextMsgToExpire.message = null;
            nextMsgToExpire.jsMessage = null;

            // If we're counting active messages, remember we unlocked a message
            unlockedActiveMessages++;
          }
          else
          {
            if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "alarm", "No expiry for: " + nextMsgToExpire);
          }

          // Move on to the next expiring message in the list (if any)
          do
          {
            nextMsgToExpire = nextMsgToExpire.next;
          }
          while((nextMsgToExpire != null) && (nextMsgToExpire.expiryTime == 0));
        }
        // Otherwise, the timer popped too soon (the message the alarm was registered for
        // has probably been deleted) this will be the next lock to expire, break out and
        // re-register the alarm
        else
          break;
      } // while

      // If there is a lock to expire in the future register an alarm for it
      if(nextMsgToExpire != null)
      {
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "alarm", "Registering alarm for: " + nextMsgToExpire);

        alarmManager.create((nextMsgToExpire.expiryTime - currentTime), this);
      }
      else
        alarmRegistered = false;
    } // synchronized

    // If we deferred any work until we didn't hold the LME lock, do it now...
    
    if(unlockedActiveMessages != 0)
      localConsumerPoint.removeActiveMessages(unlockedActiveMessages);

    if(messagesToUnlock != null)
    {
      Iterator itr = messagesToUnlock.iterator();
      
      while(itr.hasNext())
      {
        try
        {
          unlockMessage((JsMessageWrapper)itr.next(),false);
        }
        catch (SIResourceException e)
        {
          // SIResourceException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.alarm",
            "1:676:1.154.3.1",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
              "1:683:1.154.3.1",
              e });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alarm", e);
        }
        catch (SISessionDroppedException e)
        {
          // SIResourceException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.alarm",
            "1:695:1.154.3.1",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
              "1:702:1.154.3.1",
              e });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alarm", e);
        }
        catch (SIMPMessageNotLockedException e)
        {
          // No FFDC code needed
          SibTr.exception(tc, e);
          // See defect 387591
          // We should only get this exception if the message was deleted via
          // the controllable objects i.e. the admin console. If we are here for
          // another reason that we have a real error.
        }
      } // while
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alarm",this);
  }

  protected boolean checkCurrentMessageAvailable(TransactionCommon transaction) throws SIIncorrectCallException, SIMPMessageNotLockedException, SIResourceException, SISessionUnavailableException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", transaction);

    //check that there is a valid current message
    if (!messageAvailable)
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", "Invalid current Message");

      throw new SIIncorrectCallException(
        nls.getFormattedMessage("INVALID_MESSAGE_ERROR_CWSIP0191",
          new Object[] { localConsumerPoint.getConsumerManager().getDestination().getName(),
                         localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},
        null));
    }

    // if ordered, check this is the first message or that the provided tran is the current tran
    if (localConsumerPoint.getConsumerManager().getDestination().isOrdered())
    {
      if (currentMsg != firstMsg)
      {
          if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
            SibTr.exit(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", "Ordering error - Current message is not first message");

          throw new SIIncorrectCallException(
            nls.getFormattedMessage("ORDERED_MESSAGING_ERROR_CWSIP0194",
              new Object[] { localConsumerPoint.getConsumerManager().getDestination().getName(),
                             localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},
            null));
      }
      else
      if (!localConsumerPoint.getConsumerManager().isNewTransactionAllowed((Transaction)transaction))
      {
          if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
            SibTr.exit(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", "Ordering error - Transaction active");

          throw new SISessionUnavailableException(
            nls.getFormattedMessage("ORDERED_MESSAGING_ERROR_CWSIP0194",
              new Object[] { localConsumerPoint.getConsumerManager().getDestination().getName(),
                             localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},
            null));
      }
    }


    if(currentMsg.lockExpired)
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", "Message lock expired");

      SIMessageHandle msgHandles[] = {(MessageProcessor.getJsMessageHandleFactory().createJsMessageHandle(currentMsg.uuid, currentMsg.id))};

      throw new SIMPMessageNotLockedException(
        nls.getFormattedMessage(
          "MESSAGE_LOCK_EXPIRED_ERROR_CWSIP0193",
          new Object[] {
            msgHandles[0].toString(),
            localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},
          null),
          msgHandles);
    }

    //make sure that after this call, no message is available until another call to
    //nextLocked
    messageAvailable = false;

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "checkCurrentMessageAvailable", true);

    return true;
  }

  protected void checkValidState(String method)
  throws SIIncorrectCallException
  {
    if(!validState)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "checkValidState");

      SIIncorrectCallException e = new SIIncorrectCallException(
        nls_cwsir.getFormattedMessage("LME_ERROR_CWSIR0131",
          new Object[] { method },
          null));

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "checkValidState", e);

      throw e;
    }
  }

  /**
   * When a bifurcated consumer is closed all locked messages owned by that consumer must
   * be unlocked
   * @param owner
   * @throws SIResourceException
   * @throws SISessionDroppedException
   */
  protected void cleanOutBifurcatedMessages(BifurcatedConsumerSessionImpl owner, boolean bumpRedeliveryOnClose) throws SIResourceException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cleanOutBifurcatedMessages", new Object[]{new Integer(hashCode()),this});

    int unlockedMessages = 0;

    synchronized(this)
    {
      if(firstMsg != null)
      {
        LMEMessage message = firstMsg;
        LMEMessage unlockMsg = null;

        while(message != null)
        {
          unlockMsg = message;
          message = message.next;

          // Only unlock messages that are owned by the bifurcated consumer.
          if(unlockMsg.owner == owner)
          {

            // Unlock the message from the message store
            if(unlockMsg.isStored)
            {
              try
              {
                unlockMessage(unlockMsg.message, bumpRedeliveryOnClose);
              }
              catch (SIMPMessageNotLockedException e)
              {
                // No FFDC code needed
                SibTr.exception(tc, e);
                // See defect 387591
                // We should only get this exception if the message was deleted via
                // the controllable objects i.e. the admin console. If we are here for
                // another reason that we have a real error.
              }
            }

            // Remove the element from the liss
            removeMessage(unlockMsg);

            unlockedMessages++;
          }

        }
      }
    }

    if(unlockedMessages != 0)
      localConsumerPoint.removeActiveMessages(unlockedMessages);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cleanOutBifurcatedMessages", this);

  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteCurrent(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteCurrent(SITransaction transaction)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException,
         SIIncorrectCallException,
         SIErrorException, SIMPMessageNotLockedException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "deleteCurrent",
        new Object[] {new Integer(hashCode()), transaction, this});

    checkValidState("deleteCurrent");

    localConsumerPoint.checkNotClosed();

    if (transaction != null && !((TransactionCommon)transaction).isAlive())
    {
      SIIncorrectCallException e = new SIIncorrectCallException( nls.getFormattedMessage(
        "TRANSACTION_DELETE_USAGE_ERROR_CWSIP0778",
           new Object[] { consumerSession.getDestinationAddress() },
           null) );

      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteCurrent", e);

      throw e;
    }

    JsMessageWrapper msg = null;
    boolean isStored = false;

    synchronized(this)
    {
      checkCurrentMessageAvailable((TransactionCommon) transaction);

      // Pull out the interesting bits of the message
      msg = currentMsg.message;
      isStored = currentMsg.isStored;

      //remove the message from the list
      removeMessage(currentMsg);

      // There is now no message under the cursor
      messageAvailable = false;
    } // synchronized


    if((msg != null))
    {
      // If the message was not recoverable (not on an itemStream) we've finished
      // Otherwise delete it from MS
      if (isStored)
      {
        removeMessageFromStore(msg,
                               (TransactionCommon) transaction,
                               true); // true = decrement active message count
      }
      else // !isStored
      {
        localConsumerPoint.removeActiveMessages(1);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteCurrent", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#deleteSeen(com.ibm.wsspi.sib.core.SITransaction)
   */
  public void deleteSeen(SITransaction transaction)
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, SILimitExceededException,
         SIIncorrectCallException,
         SIErrorException, SIMPMessageNotLockedException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "deleteSeen",
        new Object[] {new Integer(hashCode()), transaction, this});

    checkValidState("deleteSeen");

    localConsumerPoint.checkNotClosed();

    if (transaction != null && !((TransactionCommon)transaction).isAlive())
    {
      SIIncorrectCallException e = new SIIncorrectCallException( nls.getFormattedMessage(
        "TRANSACTION_DELETE_USAGE_ERROR_CWSIP0778",
           new Object[] { consumerSession.getDestinationAddress() },
           null) );

      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteSeen", e);

      throw e;
    }

    // if ordered, check that the provided tran is the current tran
    if (localConsumerPoint.getConsumerManager().getDestination().isOrdered() &&
        !localConsumerPoint.getConsumerManager().isNewTransactionAllowed((TransactionCommon) transaction))
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteSeen", "Ordering error - Transaction active");

      throw new SIIncorrectCallException(
          nls.getFormattedMessage("ORDERED_MESSAGING_ERROR_CWSIP0194",
              new Object[] { localConsumerPoint.getConsumerManager().getDestination().getName(),
              localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName()},
              null));
    }

    LocalTransaction localTransaction = null;
    int deletedActiveMessages = 0;
    SIMPMessageNotLockedException notLockedException = null;
    int notLockedExceptionMessageIndex = 0;

    synchronized(this)
    {
      if(currentUnlockedMessage != null)
      {
        removeMessage(currentUnlockedMessage);
        currentUnlockedMessage = null;
      }
      messageAvailable = false;

      if(firstMsg != null)
      {
        // Make the array atleast the size of the number of locked msgs
        SIMessageHandle[] messageHandles = new SIMessageHandle[getNumberOfLockedMessages()];
        LMEMessage pointerMsg = firstMsg;

        LMEMessage removedMsg;
        LMEMessage endMsg;

        // There are two reasons for the currentMsg to be null, either it hasn't
        // been used jet and therefore nothing has been 'seen' so nothing for us
        // to do or it's moved off the end of the list in which case we delete
        // the whole list
        if((currentMsg == null) && endReached)
          endMsg = lastMsg;
        else
          endMsg = currentMsg;

        if(endMsg != null)
        {
          TransactionCommon tranImpl = (TransactionCommon)transaction;
          boolean more = true;

          if(tranImpl != null)
            tranImpl.registerCallback(pointerMsg.message);

          while(more)
          {
            if(pointerMsg == endMsg)
              more = false;

            // If the message is in the MS we need to remove it
            if(pointerMsg.isStored)
            {
              // Create a local tran if we weren't given one
              if(tranImpl == null)
              {
                localTransaction = txManager.createLocalTransaction(!isRMQ());
                tranImpl = localTransaction;
                tranImpl.registerCallback(pointerMsg.message);
              }

              try
              {
                removeMessageFromStore(pointerMsg.message,
                                       tranImpl,
                                       true); // true = decrement active message count (on commit)
              }
              catch (SIMPMessageNotLockedException e)
              {
                // No FFDC code needed
                SibTr.exception(tc, e);
                // See defect 387591
                // We should only get this exception if the message was deleted via
                // the controllable objects i.e. the admin console. If we are here for
                // another reason that we have a real error.

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                    new Object[] {
                    "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
                    "1:1081:1.154.3.1",
                    e });

                // Place the problem messagehandle in the array
                messageHandles[notLockedExceptionMessageIndex] = pointerMsg.getJsMessage().getMessageHandle();
                notLockedExceptionMessageIndex++;

                notLockedException = e;

//              Because we couldn't do the remove we wouldn't get the callback on the
                // afterCompletion to removeActiveMessages so we should do it here now
                if (localTransaction != null)
                  deletedActiveMessages++;
              }
            }
            // If the message wasn't stored we need to decrement the count now, but we can't
            // while we hold the LME lock.
            else
              deletedActiveMessages++;

            removedMsg = pointerMsg;
            pointerMsg = pointerMsg.next;
            removeMessage(removedMsg);
          }
        }
      }
    } // synchronized

    // Now we've released the lock we can decrement the active message count (which
    // may resume the consumer)
    if(deletedActiveMessages != 0)
      localConsumerPoint.removeActiveMessages(deletedActiveMessages);

    // Commit outside of the lock - it may be expensive and we may need other locks
    // if we fail and events are driven on the messages
    if(localTransaction != null)
    {
      localTransaction.commit();
    }

    if (notLockedException != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
        SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteSeen", this);

      throw notLockedException;
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "deleteSeen", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getConsumerSession()
   */
  public ConsumerSession getConsumerSession()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "getConsumerSession",
        new Integer(hashCode()));

    checkValidState("getConsumerSession");

    localConsumerPoint.checkNotClosed();

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "getConsumerSession", consumerSession);

    return consumerSession;
  }

  /**
   * Return the number of locked messages that are part of this locked message enumeration.
   * The count will start from thr firstMsg unlike th getRemainingMessageCount which starts
   * from the currentMsg.
   *
   * @return the number of locked messages
   */
  protected int getNumberOfLockedMessages()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getNumberOfLockedMessages");

    int count = 0;

    synchronized(this)
    {
      LMEMessage message;

      message = firstMsg;
      while(message != null)
      {
        count++;
        message = message.next;
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getNumberOfLockedMessages", new Integer(count));
    return count;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#getRemainingMessageCount()
   */
  public int getRemainingMessageCount()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "getRemainingMessageCount",
        new Object[] {new Integer(hashCode()), this});

    int count = 0;

    checkValidState("getRemainingMessageCount");

    localConsumerPoint.checkNotClosed();

    synchronized(this)
    {
      // If we've reached the end the count is zero
      if(!endReached)
      {
        LMEMessage message;

        // If currentMsg is null it means we start from the begining
        if(currentMsg == null)
          message = firstMsg;
        else
          message = currentMsg.next;

        while(message != null)
        {
          count++;
          message = message.next;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "getRemainingMessageCount", new Integer(count));

    return count;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#hasNext()
   */
  public boolean hasNext() throws SISessionUnavailableException, SISessionDroppedException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "hasNext",
        new Object[] {new Integer(hashCode()), this});

    boolean hasNext = false;

    checkValidState("hasNext");

    localConsumerPoint.checkNotClosed();

    synchronized(this)
    {
      if(!endReached)
      {
        // If currentMsg is null it means we start from the begining
        if(currentMsg == null)
        {
          if(firstMsg != null)
            hasNext = true;
        }
        else
        {
          if(currentMsg.next != null)
            hasNext = true;
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "hasNext", new Boolean(hasNext));

    return hasNext;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#nextLocked()
   */
  public SIBusMessage nextLocked()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException,
         SIErrorException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "nextLocked",
        new Object[] {new Integer(hashCode()), this});

    JsMessage jsMsg = null;
    JsMessageWrapper msg = null;
    LMEMessage dirtyMessage = null;
    boolean removeMsg = false;

    checkValidState("nextLocked");

    localConsumerPoint.checkNotClosed();

    // Make any list modifications under the lock
    synchronized(this)
    {
      // If we still have an unlocked message in the list remove it now
      if(currentUnlockedMessage != null)
      {
        removeMessage(currentUnlockedMessage);
        currentUnlockedMessage = null;
      }

      if(currentMsg != null)
        currentMsg = currentMsg.next;
      else if(!endReached)
        currentMsg = firstMsg;

      if(currentMsg != null)
      {
        msg = currentMsg.message;
        jsMsg = currentMsg.jsMessage;

        // Due to the slight dodgyness of setting jsMsg (see comment on dirtyMessage)
        // this tries to ensure our logic is correct
        if((jsMsg != null) && !currentMsg.wasRead)
        {
          SIErrorException e =
            new SIErrorException(nls.getFormattedMessage(
                            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                            new Object[] {
                              "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
                              "1:1318:1.154.3.1" },
                              null));

          if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
            SibTr.exit(CoreSPILockedMessageEnumeration.tc, "nextLocked", e);

          throw e;
        }

        // If the message was added to the message store but the consumer does
        // not require recovery we can delete the message before we return it.
        // We don't want to do it here under the lock, so we remember to do it
        // later but pretend we've already done it by the time we release the
        // lock - that way no-one else will think its in the message store when it
        // isn't.
        if(currentMsg.isStored && !currentMsg.isRecoverable)
        {
          removeMsg = true;
          currentMsg.isStored = false;
        }

        // We're about to release the list lock but we need to be able to set
        // the jsMessage back into the LMEMessage object later so we save a
        // pointer to it. BUT there is a slight possibility that by the time
        // we write the jsMessage into it the LMEMessage has been deleted from
        // the list (e.g. by an unlockAll() from the consumer), that's not a
        // problem in itself. BUT by deleting it from the list we actually can
        // put it into the set of pooled objects so you could be writing over
        // another message BUT we currently hold the AsynchConsumerLock and we
        // know no new messages can be added to the list unless they hold that
        // lock SO we may have moved into the pool but we couldn't have been
        // re-used SO we're safe!
        dirtyMessage = currentMsg;
        currentMsg.wasRead = true;

        //indicate that there is a message under the cursor
        messageAvailable = true;
      }
      else
      {
        messageAvailable = false;
        endReached = true;
      }
    } // synchronized

    //check it isn't null
    if((msg != null) && (jsMsg == null))
    {
      jsMsg = setPropertiesInMessage(currentMsg);


      if(removeMsg)
      {
        try
        {
          removeMessageFromStore(msg,
                                 null,
                                 false); // false = DON'T decrement active message count
                                         // (wait until the message is deleted/unlocked from the LME)
        }
        catch (SIMPMessageNotLockedException e)
        {
//        SIMPMessageNotLockedException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.nextLocked",
            "1:1396:1.154.3.1",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
              "1:1403:1.154.3.1",
              e });

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "nextLocked", e);

          throw new SIResourceException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
                "1:1414:1.154.3.1",
                e },
              null),
            e);
        }
      }

      dirtyMessage.jsMessage = jsMsg; // Read earlier comment before touching this!
    }

    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
      UserTrace.trace_Receive(null,
                              jsMsg,
                              consumerSession.getDestinationAddress(),
                              consumerSession.getIdInternal());

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "nextLocked", new Object[] {jsMsg});
    //return the message or null
    return jsMsg;
  }

  /**
   * Peek at the next message on the enumeration.
   */
  public SIBusMessage peek() throws SISessionUnavailableException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "peek", this);

    checkValidState("peek");

    localConsumerPoint.checkNotClosed();

    JsMessage jsMsg = null;

    synchronized(this)
    {
      if(!endReached)
      {
        // If currentMsg is null it means we start from the begining
        if(currentMsg == null)
        {
          if(firstMsg != null)
          {
            jsMsg = setPropertiesInMessage(firstMsg);
          }
        }
        else
        {
          if(currentMsg.next != null)
          {
            jsMsg = setPropertiesInMessage(currentMsg.next);
          }
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "peek", jsMsg);
    return jsMsg;
  }

  /**
   * Unlock/delete/read a set of locked messages based on their short ID (which
   * is unique to this consumer). Warning: The array must be in order
   *
   * @param msgIds
   * @throws SISessionDroppedException
   */
  protected SIBusMessage[] processMsgSet(SIMessageHandle[] msgHandles,
                                         TransactionCommon transaction,
                                         BifurcatedConsumerSessionImpl bifurcatedConsumer,
                                         boolean unlock,
                                         boolean delete,
                                         boolean read,
                                         boolean incrementLockCount) throws SIConnectionLostException, SIIncorrectCallException, SIResourceException, SIErrorException, SIMPMessageNotLockedException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processMsgSet",
        new Object[]{new Integer(hashCode()),
                     SIMPUtils.messageHandleArrayToString(msgHandles),
                     transaction,
                     bifurcatedConsumer,
                     new Boolean(unlock),
                     new Boolean(delete),
                     new Boolean(read),
                     new Boolean(incrementLockCount),
                     this});

    int numMsgs = msgHandles.length;
    LocalTransaction localTransaction = null;

    if (transaction != null && !transaction.isAlive())
    {
      SIIncorrectCallException e = new SIIncorrectCallException( nls.getFormattedMessage(
        "TRANSACTION_DELETE_USAGE_ERROR_CWSIP0778",
           new Object[] { consumerSession.getDestinationAddress() },
           null) );

      if (TraceComponent.isAnyTracingEnabled() && TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processMsgSet", e);

      throw e;
    }

    // Create an array to hold any read messages
    SIBusMessage messages[] = null;
    if(read)
      messages = new SIBusMessage[numMsgs];

    SIMessageHandle missingMsgs[] = null;
    int missingMsgIndex = 0;
    int removedActiveMessages = 0;

    JsMessageWrapper messagesToUnlock[] = null;
    int unlockIndex = 0;

    JsMessageWrapper autoCommitMessage = null;

    try
    {
      synchronized(this)
      {
        int nextId = 0;

        // Search For each messageId in the LME, always moving forwards as both lists
        // are ordered
        for(nextId=0; nextId<numMsgs; nextId++)
        {
          if(msgHandles[nextId] != null)
          {
            //set the LME to start at the first entry
            LMEMessage lmeMessage = firstMsg;
            boolean msgFound = false;
            boolean expiredMsgFound;

            // Walk down the LME until we find the message or walk off the end
            while(lmeMessage != null)
            {
              expiredMsgFound = false;
              if( (lmeMessage.id == ((JsMessageHandle)msgHandles[nextId]).getSystemMessageValue())
                &&(lmeMessage.uuid.equals(((JsMessageHandle)msgHandles[nextId]).getSystemMessageSourceUuid()))
                &&(lmeMessage != currentUnlockedMessage) )
              {
                // We can only process messages that have not expired their lock
                if(!lmeMessage.lockExpired)
                {
                  // We also only process a message if it is still owned by the root consumer
                  // or we are processing a request from the owning bifurcated consumer
                  if((lmeMessage.owner == null) || (lmeMessage.owner == bifurcatedConsumer))
                  {
                    // Return the message to the caller if requested
                    if(read)
                    {
                      // Add the message item into the list to return.
                      messages[nextId] = lmeMessage.getJsMessage();

                      // Reading the message cancels the lock expiry
                      lmeMessage.expiryTime = 0;

                      // If this is being read by a bifurcated consumer we transfer the scope
                      // of its lock to them (unless we're going to delete it too, in which case
                      // there is no point)
                      if((bifurcatedConsumer != null) && !delete)
                      {
                        lmeMessage.owner = bifurcatedConsumer;
                      }
                    }

                    boolean markMsgAsMissing = false; // used to mark the message as missing when it is not in msgstore

                    // Delete the message if required
                    if (delete)
                    {
                      // Not all messages will still be in the message store
                      if(lmeMessage.isStored)
                      {
                        // If we weren't given a transaction we need to get one
                        if(transaction == null)
                        {
                          // If we have multiple messages to delete or we may perform extra
                          // messagestore operations as a result of the delete we'll need a
                          // local transaction
                          if((numMsgs > 1) ||
                             (!lmeMessage.message.isReference() &&
                              lmeMessage.message.getReportCOD() != null ||
                              lmeMessage.message.isRemoteGet()))
                          {
                            localTransaction = txManager.createLocalTransaction(!isRMQ());
                            transaction = localTransaction;
                          }
                          // Otherwise, we can get away with just re-using an autoCommitTran
                          // (but we can't use the LCP one as we don't hold any consumer level
                          // lock at this point, instead we use one owned by the LME as we hold the
                          // LME lock.
                          else
                          {
                            // Remember the the message to remove (to be done once the
                            // LME lock isn't held).
                            autoCommitMessage = lmeMessage.message;
                          }
                        }
                        else if (lmeMessage.message.isRemoteGet() && transaction.isAutoCommit())
                        {
                          // If an auto commit transaction was passed in and
                          // we have a remote ME then the transaction needs to be upgrated to a
                          // local transaction.
                          localTransaction = txManager.createLocalTransaction(!isRMQ());
                          transaction = localTransaction;
                        }

                        // If we have a true transaction we register for callbacks and
                        // remove the message. If we're using an autocommit transaction
                        // we can't remove it here under the LME lock as the remove may
                        // fail and re-drive the CD with the message which in turn will
                        // try to get the LCP lock. This breaks the locking hierarchy,
                        // AsynchLock->LCPLock->LMELock
                        if(autoCommitMessage == null)
                        {
                          try
                          {
                            removeMessageFromStore(lmeMessage.message,
                                                   transaction,
                                                   true); // true = decrement active message count (on commit)
                          }
                          catch (SIMPMessageNotLockedException e)
                          {
                            // No FFDC code needed
                            SibTr.exception(tc, e);
                            // See defect 387591
                            // We should only get this exception if the message was deleted via
                            // the controllable objects i.e. the admin console. If we are here for
                            // another reason that we have a real error.

                            // We don't need a special case here to decrement the active messages as the
                            // afterCompletion will still occur on a transaction with no workitems

                            markMsgAsMissing = true;
                          }
                        }
                      }
                      // Messages not in the MS need their count decrementing now, but not under the lock
                      else
                        removedActiveMessages++;

                      // Remove the reference to the message from the LME
                      removeMessage(lmeMessage);
                    }
                    // Or unlock it
                    else if(unlock)
                    {
                      // Not all messages will still be in the message store
                      if(lmeMessage.isStored)
                      {
                        // We can't unlock the message here as we hold the LME lock
                        // and the unlock could cause the message to be redelivered
                        // to the same consumer which will attempt to take the LCP
                        // lock - which breaks the lock hierarchy.

                        // Build an array of messages to unlock once the LME lock is
                        // released
                        if(messagesToUnlock == null)
                          messagesToUnlock = new JsMessageWrapper[numMsgs];

                        messagesToUnlock[unlockIndex++] = lmeMessage.message;
                      }
                      removeMessage(lmeMessage);

                      // If we're counting active messages decrement the count, but not under the lock
                      removedActiveMessages++;
                    }

                    if (!markMsgAsMissing) //only mark the msg as found if there wasn't a problem with it
                      msgFound = true;
                  }

                  // Break out the while now that we've processed the message
                  break;
                }
                // Otherwise, we simply remove the entry now that we've told someone
                else
                {
                  // Store the ref to the next msg as the remove destroys it
                  LMEMessage temp = lmeMessage.next;
                  removeMessage(lmeMessage);
                  // Next msg
                  lmeMessage = temp;
                  expiredMsgFound = true;
                }

              }
              if (!expiredMsgFound)
                lmeMessage = lmeMessage.next;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc,"Checking msg for read and delete : "+lmeMessage);
            }
            if(!msgFound)
            {
              // We couldn't find the message, remember this
              if(missingMsgs == null)
                missingMsgs = new SIMessageHandle[numMsgs];

              missingMsgs[missingMsgIndex] = msgHandles[nextId];
              missingMsgIndex++;
            }
          }
          else
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "processMsgSet", "SIIncorrectCallException one or more null SIMessageHandles");

            throw new SIIncorrectCallException(
              nls_cwsir.getFormattedMessage("LME_ERROR_CWSIR0132",
              null,
              null));
          }
        }
      }//synchronized

      // If we deferred removing the message we do it here now that the LME lock has
      // been released.
      if(autoCommitMessage != null)
      {
        try
        {
          removeMessageFromStore(autoCommitMessage,
                                 null,
                                 true); // true = decrement active message count
        }
        catch (SIMPMessageNotLockedException  e)
        {
          // No FFDC code needed
          SibTr.exception(tc, e);
          // As this should only occur when a msg has been deleted by the user then
          // we should have marked this message with been missing and so be processed
          // soon
        }

        // If we used an autoCommitTran we won't get any callbacks so we remember
        // we've removed another active message
        //removedActiveMessages++;
      }
      // if we created an internal transaction, commit it
      else if (localTransaction != null)
      {
        localTransaction.commit();
      }

      if(removedActiveMessages != 0)
        localConsumerPoint.removeActiveMessages(removedActiveMessages);

      //    If we deferred any unlocks until we didn't hold the LME lock, unlock them
      //    now
      if(unlockIndex != 0)
      {
        for(int i = 0; i < unlockIndex; i++)
        {
          try
          {
            unlockMessage(messagesToUnlock[i],incrementLockCount);
          }
          catch (SIMPMessageNotLockedException e)
          {
            // No FFDC code needed
            SibTr.exception(tc, e);
            // As this should only occur when a msg has been deleted by the user then
            // we should have marked this message with been missing and so be processed
            // soon
          }
        }
      }

      // If we haven't found all the message ids, then this is
      // an error and the SICoreMessageNotFound exception should
      // be thrown.
      if (missingMsgs != null)
      {
        // Crop the missing message array
        SIMessageHandle croppedMissingMsgs[] = new SIMessageHandle[missingMsgIndex];
        System.arraycopy(missingMsgs, 0, croppedMissingMsgs, 0, missingMsgIndex);

        SIMPMessageNotLockedException e =  new SIMPMessageNotLockedException (
          nls.getFormattedMessage(
                  "CORE_MESSAGE_NOT_FOUND_ERROR_CWSIP0173",
                  new Object[] {SIMPUtils.messageHandleArrayToString(croppedMissingMsgs),
                         localConsumerPoint.getConsumerManager().getDestination().getName(),
                         localConsumerPoint.getConsumerManager().getMessageProcessor().getMessagingEngineName() },
                  null),
         croppedMissingMsgs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processMsgSet", e);

        throw e;
      }
    }
    catch (SIResourceException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.processMsgSet",
        "1:1824:1.154.3.1",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
          "1:1831:1.154.3.1",
          e });

      // If we created a transaction, try to roll it back before we
      // exit
      if(localTransaction != null)
      {
        localTransaction.rollback();
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "processMsgSet", e);

      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processMsgSet", new Object[] {messages, this});

    return messages;
  }

  abstract boolean isRMQ();

  /**
   * Remove a message object from the list
   * @param message
   */
  protected void removeMessage(LMEMessage message)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMessage", new Object[] {new Integer(hashCode()), message, this });

    // If this was the message we entered the callback with we need
    // to move the start point back a bit
    if(message == callbackEntryMsg)
      callbackEntryMsg = message.previous;

    // If this is our current message we need to move the cursor
    // back a bit
    if(message == currentMsg)
      currentMsg = message.previous;

    // If this message was the next to expire we move the expiry cursor on to
    // the next eligible message. We leave any alarm registered so that it pops,
    // when this happens it realises the alarm is not valid for the next expiring
    // message and re-registers itself for the correct time. This gives us the
    // best performance when no locks are expiring as we're not continually
    // registering and de-registering alarms for every message
    if(message == nextMsgToExpire)
    {
      do
      {
        nextMsgToExpire = nextMsgToExpire.next;
      }
      while((nextMsgToExpire != null) && (nextMsgToExpire.expiryTime == 0));
    }

    if(message == nextMsgReferenceToExpire)
    {
      do
      {
        nextMsgReferenceToExpire = nextMsgReferenceToExpire.next;
      }
      while((nextMsgReferenceToExpire != null) && (nextMsgReferenceToExpire.expiryTime == 0));
    }

    // Unlink this message from the list
    if(message.previous != null)
      message.previous.next = message.next;
    else
      firstMsg = message.next;

    if(message.next != null)
      message.next.previous = message.previous;
    else
      lastMsg = message.previous;

    // Pool the element if there's space
    if(pooledCount < poolSize)
    {
      message.message = null;
      message.next = pooledMsg;
      message.previous = null;
      message.owner = null;
      message.jsMessage = null;
      message.expiryTime = 0;
      message.expiryMsgReferenceTime = 0;
      pooledMsg = message;
      pooledCount++;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMessage", this);
  }

  abstract void removeMessageFromStore(JsMessageWrapper msg,
                                       TransactionCommon tran,
                                       boolean decrementActiveMessage)
    throws SIResourceException, SISessionDroppedException, SIIncorrectCallException, SIMPMessageNotLockedException;

  /**
   * This is called when a consumeMessages call has completed, it returns the
   * LME to a consistent state ready for the next consumeMessages.
   *
   * This will unlock any messages that were locked on this LME that haven't been read.
   * @throws SISessionDroppedException
   */
  protected void resetCallbackCursor() throws SIResourceException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetCallbackCursor", new Integer(hashCode()));

    synchronized(this)
    {
      unlockAllUnread();

      callbackEntryMsg = lastMsg;
      currentMsg = lastMsg;
      messageAvailable = false;
      endReached = false;
      validState = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetCallbackCursor", this);
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.LockedMessageEnumeration#resetCursor()
   */
  public void resetCursor()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIErrorException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.entry(CoreSPILockedMessageEnumeration.tc, "resetCursor",
        new Object[] {this, new Integer(hashCode())});

    checkValidState("resetCursor");

    localConsumerPoint.checkNotClosed();

    synchronized(this)
    {
      if(currentUnlockedMessage != null)
      {
        removeMessage(currentUnlockedMessage);
        currentUnlockedMessage = null;
      }

      // Move the cursor back to the callback entry point
      currentMsg = callbackEntryMsg;
      endReached = false;
      messageAvailable = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && CoreSPILockedMessageEnumeration.tc.isEntryEnabled())
      SibTr.exit(CoreSPILockedMessageEnumeration.tc, "resetCursor", this);
  }

  /**
   * @param messageLockExpiry
   */
  protected void setMessageLockExpiry(long messageLockExpiry)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setMessageLockExpiry", new Long(messageLockExpiry));
      SibTr.exit(tc, "setMessageLockExpiry");
    }

    synchronized(this)
    {
      this.messageLockExpiry = messageLockExpiry;
    }
  }

  /**
   * Sets the redelivered count and the message wait time if required.
   *
   * Copies the message if required
   *
   * @param theMessage
   * @return
   */
  final JsMessage setPropertiesInMessage(LMEMessage theMessage)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setPropertiesInMessage", theMessage);

    boolean copyMade = false;

    //  If this is pubsub we share this message with other subscribers so we have
    //  to make a copy prior to any updates. If this is pt-to-pt then any changes
    //  we make here can be done before the copy, the copy is still required to
    //  prevent a consumer from rolling back after modifying the message but
    //  anything we set here will be reset the next time round so there's no harm
    //  done. It is best to defer the 'copy' until after we've changed it so there
    //  is less likelyhood of a real copy being required.
    JsMessageWrapper jsMessageWrapper = (JsMessageWrapper) theMessage.message;
    JsMessage jsMsg = jsMessageWrapper.getMessage();

    try
    {
      //set the redeliveredCount if required
      if (jsMessageWrapper.guessRedeliveredCount() != 0)
      {
        if(isPubsub)
        {
          jsMsg = jsMsg.getReceived();
          copyMade = true;
        }

        jsMsg.setRedeliveredCount(jsMessageWrapper.guessRedeliveredCount());
      }

      long waitTime = jsMessageWrapper.updateStatisticsMessageWaitTime();

      //Only store wait time in message where explicitly
      // asked
      if (setWaitTime )
      {
        //defect 256701: we only set the message wait time if it is
        //a significant value.
        boolean waitTimeIsSignificant =
          waitTime > messageProcessor.getCustomProperties().get_message_wait_time_granularity();

        if(waitTimeIsSignificant)
        {
          if(isPubsub && !copyMade)
          {
            jsMsg = jsMsg.getReceived();
            copyMade = true;
          }

          jsMsg.setMessageWaitTime(waitTime);
        }
      }

      // If we deferred the copy till now we need to see if one is
      // needed. This will be the case either if the consumer has indicated that
      // it may change the message (not a comms client) and the message is still
      // on an itemStream (i.e. they could rollback the message with their changes).
      // Otherwise we can just give them our copy as we'll never want it back or
      // give it to anyone else.
      if(!copyMade && ((theMessage.isStored && copyMsg) || isPubsub))
        jsMsg = jsMsg.getReceived();
    }
    catch (MessageCopyFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.setPropertiesInMessage",
        "1:2086:1.154.3.1",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
          "1:2093:1.154.3.1",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "setPropertiesInMessage", e);

      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
            "1:2104:1.154.3.1",
            e },
          null),
        e);
    }


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setPropertiesInMessage", jsMsg);
    return jsMsg;
  }

  public String toString()
  {
    return toString("");
  }

  public String toString(String indent)
  {
    StringBuffer buffer = new StringBuffer();

    buffer.append(indent + "endReached:" + endReached + "\n");
    buffer.append(indent + "msgAvailable:" + messageAvailable + "\n");

    if(currentMsg == null)
      buffer.append(indent + "No current message\n");

    LMEMessage msg = firstMsg;
    if(msg != null)
    {
      while(msg != null)
      {
        if(msg == firstMsg)
          buffer.append(indent + "firstMsg:\n");
        if(msg == callbackEntryMsg)
          buffer.append(indent + "callbackEntryMsg:\n");
        if(msg == currentUnlockedMessage)
          buffer.append(indent + "currentUnlockedMessage:\n");
        if(msg == currentMsg)
          buffer.append(indent + "currentMsg:\n");
        if(msg == nextMsgToExpire)
          buffer.append(indent + "nextMsgToExpire:\n");
        if(msg == nextMsgReferenceToExpire)
          buffer.append(indent + "nextMsgReferenceToExpire:\n");
        if(msg == lastMsg)
          buffer.append(indent + "lastMsg:\n");


        buffer.append(indent + "  " + msg.toString());

        msg = msg.next;
      }
    }
    else
      buffer.append(indent + "No messages in the enumeration\n");

    buffer.append(indent + "pooledCount: " + pooledCount + "\n");

    return buffer.toString();
  }

  protected void unlockAll() throws SIResourceException, SIMPMessageNotLockedException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockAll");

    unlockAll(false);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockAll");
  }

  /**
   * Unlock all the messages in the list
   * @throws SISessionDroppedException
   * @throws SIStoreException
   */
  protected void unlockAll(boolean closingSession) throws SIResourceException, SIMPMessageNotLockedException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockAll", new Object[]{new Integer(hashCode()),this});

    int unlockedMessages = 0;
    SIMPMessageNotLockedException notLockedException = null;
    int notLockedExceptionMessageIndex = 0;

    synchronized(this)
    {
      messageAvailable = false;

      if(firstMsg != null)
      {
        LMEMessage pointerMsg = firstMsg;
        LMEMessage removedMsg = null;
        SIMessageHandle[] messageHandles = new SIMessageHandle[getNumberOfLockedMessages()];

        boolean more = true;
        while(more)
        {
          // See if this is the last message in the list
          if(pointerMsg == lastMsg)
            more = false;

          // Only unlock messages that are in the MS (and haven't already
          // been unlocked by us)
          if(pointerMsg != currentUnlockedMessage)
          {
            if(pointerMsg.isStored)
            {
              try
              {
                unlockMessage(pointerMsg.message,true);
              }
              catch (SIMPMessageNotLockedException e)
              {
                // No FFDC code needed
                SibTr.exception(tc, e);
                // See defect 387591
                // We should only get this exception if the message was deleted via
                // the controllable objects i.e. the admin console. If we are here for
                // another reason that we have a real error.

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                    new Object[] {
                    "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
                    "1:2229:1.154.3.1",
                    e });

                messageHandles[notLockedExceptionMessageIndex] = pointerMsg.getJsMessage().getMessageHandle();
                notLockedExceptionMessageIndex++;

                if(notLockedException == null)
                  notLockedException = e;
              }
              catch (SISessionDroppedException e)
              {
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.unlockAll",
                  "1:2243:1.154.3.1",
                  this);

                if(!closingSession)
                {
                  handleSessionDroppedException(e);
                }

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.unlockAll",
                    "1:2255:1.154.3.1",
                    e });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                  SibTr.exit(tc, "unlockAll", e);

                throw e;
              }

            }

            unlockedMessages++;
          }

          removedMsg = pointerMsg;
          pointerMsg = pointerMsg.next;
          // Remove the element from the list
          removeMessage(removedMsg);
        }

        currentUnlockedMessage = null;
      }
    }

    if(unlockedMessages != 0)
      localConsumerPoint.removeActiveMessages(unlockedMessages);

    if (notLockedException != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "unlockAll", this);
      throw notLockedException;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockAll", this);
  }

  abstract void handleSessionDroppedException(SISessionDroppedException e);

  /**
   * Method to unlock all messages which haven't been read
   * @param incrementRedeliveryCount
   * @throws SISessionDroppedException
   */
  private void unlockAllUnread() throws SIResourceException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockAllUnread", new Object[] { new Integer(hashCode()), this });

    int unlockedMessages = 0;

    synchronized(this)
    {
      messageAvailable = false;

      // Only unlock messages if we have reached the end of the list.
      if(firstMsg != null && !endReached)
      {
        LMEMessage pointerMsg = null;

        if (currentMsg == null)
          pointerMsg = firstMsg;
        else
          pointerMsg = currentMsg.next;

        LMEMessage removedMsg = null;

        boolean more = true;

        if (pointerMsg != null)
        {
          while(more)
          {
            // See if this is the last message in the list
            if(pointerMsg == lastMsg)
              more = false;

            // Only unlock messages that are in the MS (and haven't already
            // been unlocked by us)
            if(pointerMsg != currentUnlockedMessage)
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Unlocking Message " + pointerMsg);
              if(pointerMsg.isStored)
              {
                try
                {
                  unlockMessage(pointerMsg.message,false);
                }
                catch (SIMPMessageNotLockedException e)
                {
                  // No FFDC code needed
                  SibTr.exception(tc, e);
                  // See defect 387591
                  // We should only get this exception if the message was deleted via
                  // the controllable objects i.e. the admin console. If we are here for
                  // another reason that we have a real error.
                }
              }

              unlockedMessages++;
            }

            removedMsg = pointerMsg;
            pointerMsg = pointerMsg.next;
            // Remove the element from the list
            removeMessage(removedMsg);
          }
        }
      }
    }

    if(unlockedMessages != 0)
      localConsumerPoint.removeActiveMessages(unlockedMessages);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockAllUnread", this);
  }

  protected abstract void unlockMessage(JsMessageWrapper msg, boolean bumpRedelieveryCount) throws SIMPMessageNotLockedException, SISessionDroppedException, SIResourceException;
  
  /**
   * Unlock all the messages in the list
   * @throws SISessionDroppedException
   * @throws SIStoreException
   */
  protected void unlockAll(boolean closingSession,boolean incrementUnlockCount) throws SIResourceException, SIMPMessageNotLockedException, SISessionDroppedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockAll", new Object[]{new Integer(hashCode()),this,incrementUnlockCount});

    int unlockedMessages = 0;
    SIMPMessageNotLockedException notLockedException = null;
    int notLockedExceptionMessageIndex = 0;

    synchronized(this)
    {
      messageAvailable = false;

      if(firstMsg != null)
      {
        LMEMessage pointerMsg = firstMsg;
        LMEMessage removedMsg = null;
        SIMessageHandle[] messageHandles = new SIMessageHandle[getNumberOfLockedMessages()];

        boolean more = true;
        while(more)
        {
          // See if this is the last message in the list
          if(pointerMsg == lastMsg)
            more = false;

          // Only unlock messages that are in the MS (and haven't already
          // been unlocked by us)
          if(pointerMsg != currentUnlockedMessage)
          {
            if(pointerMsg.isStored)
            {
              try
              {
                unlockMessage(pointerMsg.message,incrementUnlockCount);
              }
              catch (SIMPMessageNotLockedException e)
              {
                // No FFDC code needed
                SibTr.exception(tc, e);
                // See defect 387591
                // We should only get this exception if the message was deleted via
                // the controllable objects i.e. the admin console. If we are here for
                // another reason that we have a real error.

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                    new Object[] {
                    "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration",
                    "1:2228:1.154.1.32",
                    e });

                messageHandles[notLockedExceptionMessageIndex] = pointerMsg.getJsMessage().getMessageHandle();
                notLockedExceptionMessageIndex++;

                if(notLockedException == null)
                  notLockedException = e;
              }
              catch (SISessionDroppedException e)
              {
                FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.unlockAll",
                  "1:2242:1.154.1.32",
                  this);

                if(!closingSession)
                {
                  handleSessionDroppedException(e);
                }

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                  new Object[] {
                    "com.ibm.ws.sib.processor.impl.AbstractLockedMessageEnumeration.unlockAll",
                    "1:2254:1.154.1.32",
                    e });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                  SibTr.exit(tc, "unlockAll", e);

                throw e;
              }

            }

            unlockedMessages++;
          }

          removedMsg = pointerMsg;
          pointerMsg = pointerMsg.next;
          // Remove the element from the list
          removeMessage(removedMsg);
        }

        currentUnlockedMessage = null;
      }
    }

    if(unlockedMessages != 0)
      localConsumerPoint.removeActiveMessages(unlockedMessages);

    if (notLockedException != null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "unlockAll", this);
      throw notLockedException;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockAll", this);
  }

}
