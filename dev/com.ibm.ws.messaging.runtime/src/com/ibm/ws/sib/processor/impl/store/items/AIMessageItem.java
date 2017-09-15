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
package com.ibm.ws.sib.processor.impl.store.items;

import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.AIStreamKey;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.RemoteQPConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.RemoteDispatchableKey;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This represents a message in the RemoteConsumerDispatcher's ItemStream.
 * Regardless of reliability, this Item is STORE_MAYBE, since we don't need to persist messages at
 * the RME.
 */
public class AIMessageItem extends MessageItem
{
  private static final TraceComponent tc =
  SibTr.register(
    AIMessageItem.class,
    SIMPConstants.MP_TRACE_GROUP,
    SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);
  
  
  private AnycastInputHandler aih;
  private int cachedRedeliveredCount; // this is the value that came with the message from the DME

  private AIStreamKey key;

  // This value is false when the AIMessageItem is created. It is set to true if eventLocked() successfully
  // informs the RemoteQPConsumerKey that it has been locked, and set to false if eventUnlocked() does the
  // same. It is important to inform the RemoteQPConsumerKey for prefetching purposes. However, if the
  // AIMessageItem is restored (after spilling), the RemoteQPConsumerKey is not remembered. If the message
  // were not locked prior to the spilling this will prevent the RemoteQPConsumerKey from decrementing the
  // unlocked count it is maintaining. Over a period of time, many such messages may be spilled, in which
  // case the unlocked count in RemoteQPConsumerKey will become more and more inaccurate. This will make
  // prefetching perform badly since almost no prefetching will occur.
  // This implementation takes the optimistic view and decrements the unlocked count in the RemoteQPConsumerKey
  // when the item is being spilled. This is not the ideal solution, so we should look into improving it
  // in the future.
  private boolean informedConsumerKeyThatLocked;

  // The transaction ID of transaction that is removing a message that should be rejected is remembered here
  // and compared are precommitRemove and postcommitRemove to perform the correct behavior (accept or reject).
  private PersistentTranId rejectTransactionID = null;
  
  // SIB0113a
  // On an IME used for gathering, an AIMessageItem can be the target of a remoteGet. On restart,
  // if an AOValue was found that referenced an AIMessageItem we need to restore that item (since
  // AIMessageItems are not persisted).
  // The following is set on an AIMessageItem that has been rerequested to satisfy a particular AOValue.
  // It used to restore the link once this item is in the messagestore.
  private AOValue restoredTargetAOValue;
  
  // SIB0113
  // isReserved is set to true if this message is reserved for an existing AOValue. It is used to prevent
  // consumers from receiving the message before we have a chance to lock it to the AOValue. 
  private volatile boolean isReserved;

  // On a preUnlock we set this to true if we want to increase the unlockCount of the msg
  private boolean incLockCount;

  public AIMessageItem()
  {
    super();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIMessageItem");

    synchronized (this)
    {
      informedConsumerKeyThatLocked = false;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIMessageItem", this);
  }

  public AIMessageItem(JsMessage msg)
  {
    super(msg);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AIMessageItem", msg);

    synchronized (this)
    {
      informedConsumerKeyThatLocked = false;
      cachedRedeliveredCount = msg.getRedeliveredCount();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AIMessageItem", this);
  }

  public synchronized void setRejectTransactionID(PersistentTranId id)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRejectTransactionID", id);
    rejectTransactionID = id;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRejectTransactionID");
  }

  private synchronized boolean isRejectTransactionID(PersistentTranId id)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isRejectTransactionID", id);

    boolean retVal = false;

    if (rejectTransactionID != null)
      retVal = id.equals(rejectTransactionID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isRejectTransactionID", Boolean.valueOf(retVal));
    return retVal;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#getPersistentData()
   */
  // Feature SIB0112b.mp.1
  public List<DataSlice> getPersistentData() throws PersistentDataEncodingException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getPersistentData");

    // this is going to be spilled, so if not already told ConsumerKey that it is locked
    // tell it now!
    synchronized (this)
    {
      if (!informedConsumerKeyThatLocked)
      {
        RemoteDispatchableKey dkey = key.getRemoteDispatchableKey();
        RemoteQPConsumerKey ck = null;
        if (dkey instanceof RemoteQPConsumerKey)
          ck = (RemoteQPConsumerKey) dkey;
        if (ck != null)
        {
          ck.messageLocked(key);
          informedConsumerKeyThatLocked = true;
        }
      }
      // set RemoteDispatchableKey to null so no one can inform the RemoteDispatchableKey about any state change
      // in this message in the future. If we did not do this, it is possible that an eventUnlocked()
      // call occurs after this method call and before the in-memory AIMessageItem object is discarded,
      // which would cause informedConsumerKeyThatLocked to be set to false and the ConsumerKey unlocked
      // count would be incremented.
      key.clearRemoteDispatchableKey();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getPersistentData");

    return super.getPersistentData();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#restore(byte[])
   */
  // Feature SIB0112b.mp.1
  public void restore(final List<DataSlice> dataSlices)
  throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restore", dataSlices);

    super.restore(dataSlices);
    key = new AIStreamKey(getMessage().getGuaranteedRemoteGetValueTick());

    // find DestinationHandler and use it to initialize reference to RemoteConsumerDispatcher
    ItemStream itemstream = null;
    try
    {
       itemstream = getItemStream();
    }
    catch (SevereMessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(e,
          "com.ibm.ws.sib.processor.impl.store.items.AIMessageItem.restore",
          "1:249:1.44.1.10", this);
      
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "restore", e);
      throw new SIErrorException(e);
    }
    BaseDestinationHandler destinationHandler = null;

    PtoPMessageItemStream localisation = (PtoPMessageItemStream) itemstream;
    destinationHandler = localisation.getDestinationHandler();

    // Using the uuid of the source ME in the message to retrieve the corresponding RCD, on the
    // assumption that this RCD is the one to which the message was originally given to.
    // When the message originally arrived, MPIO used the message's source cellule obtained via TRM
    // to determine the AIH/RCD that would handle it.
    // The assumption then is that the two forms of obtaining the source ME uuid yield the same RCD.
    SIBUuid8 sourceMEId = getMessage().getGuaranteedSourceMessagingEngineUUID();
    SIBUuid12 gatheringUuid = getMessage().getGuaranteedGatheringTargetUUID();
    this.aih = destinationHandler.getAnycastInputHandler(sourceMEId, gatheringUuid, true);

    synchronized (this)
    {
      // restore the value by looking it up
      cachedRedeliveredCount = getMessage().getRedeliveredCount();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restore");

  }

  public final void setInfo(AnycastInputHandler aih, AIStreamKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setInfo", new Object[]{aih,key});

    synchronized (this)
    {
      this.aih = aih;
      this.key = key;      
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setInfo");
  }

  public final AIStreamKey getAIStreamKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAIStreamKey");
      SibTr.exit(tc, "getAIStreamKey",key);
    }
    return key;
  }

  public final int getStorageStrategy()
  {
    // a message is typically not persisted at the RME, but we allow spilling
    // Note that Reliability is still the same as the DME msg - only storage strategy is different
    return STORE_MAYBE; 
  }

  /**
   * Expiry at the Remote ME causes the message to be rejected by the
   * Remote ME.
   */
  public long getMaximumTimeInStore()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "getMaximumTimeInStore");
    long originalExpiryTime = super.getMaximumTimeInStore();
    long rejectTime = aih.getRCD().getRejectTimeout();
    if (originalExpiryTime == NEVER_EXPIRES)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getMaximumTimeInStore", Long.valueOf(rejectTime));
      return rejectTime;
    }
    else if (rejectTime == NEVER_EXPIRES)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getMaximumTimeInStore", Long.valueOf(originalExpiryTime));
      return originalExpiryTime;
    }
    else
    {
      // neither is NEVER_EXPIRES, so return the minimum of the two
      long min = originalExpiryTime < rejectTime?originalExpiryTime:rejectTime;
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMaximumTimeInStore", Long.valueOf(min));
      return min;
    }
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#canExpireSilently()
   */
  public boolean canExpireSilently()
  {
    super.canExpireSilently(); // just for completeness, call the super class method
    return false; // return false in all cases
  }

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#eventExpiryNotification(Transaction)
   */
  public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventExpiryNotification", new Object[]{transaction, key.getTick()});

    super.eventExpiryNotification(transaction);
    setRejectTransactionID(transaction.getPersistentTranId());
    
    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())    
      SibTr.debug(UserTrace.tc_mt,       
         nls_mt.getFormattedMessage(
         "REMOTE_MESSAGE_EXPIRED_CWSJU0034",
         new Object[] {
           getMessage(),
           key.getTick(),
           aih.getDestName(),
           aih.getMessageProcessor().getMessagingEngineUuid(),
           aih.getLocalisationUuid(),
           aih.getGatheringTargetDestUuid()},
         null));

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventExpiryNotification");

  }

  public void eventPrecommitRemove(final Transaction transaction) throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPrecommitRemove", new Object[]{transaction, this});

    super.eventPrecommitRemove(transaction);
    if (!isRejectTransactionID(transaction.getPersistentTranId()))
      aih.accept(key, transaction);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPrecommitRemove");
  }

  public void eventPostCommitRemove(final Transaction transaction) throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommitRemove", new Object[]{transaction, this});

    super.eventPostCommitRemove(transaction);
    // if for some reason we have not informed the RCK that this is locked, do so now.
    synchronized (this)
    {
      if (!informedConsumerKeyThatLocked)
      {
        RemoteDispatchableKey dkey = key.getRemoteDispatchableKey();
        RemoteQPConsumerKey ck = null;
        if (dkey instanceof RemoteQPConsumerKey)
          ck = (RemoteQPConsumerKey) dkey;
        if (ck != null)
        {
          ck.messageLocked(key);
          informedConsumerKeyThatLocked = true;
        }
      }
    }

    if (isRejectTransactionID(transaction.getPersistentTranId()))
    {      
      // PK63551 - Call rolledback() instead of reject() as do the same thing but rolledback() drains the flush queue. 
      aih.rolledback(key);
    }
    else
    {
      if (getReliability().compareTo(Reliability.RELIABLE_PERSISTENT) < 0)
      {
        aih.committed(key);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommitRemove");
  }

  /**
   * Notification that this message has been locked.
   */
  public void eventLocked()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventLocked");

    // modify prefetching info
    synchronized (this)
    {
      RemoteDispatchableKey dkey = key.getRemoteDispatchableKey();
      RemoteQPConsumerKey ck = null;
      if (dkey instanceof RemoteQPConsumerKey)
        ck = (RemoteQPConsumerKey) dkey;

      if (ck != null)
      {
        ck.messageLocked(key);
        informedConsumerKeyThatLocked = true;
      }
    }

    // call superclass
    super.eventLocked();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventLocked");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.store.AbstractItem#eventUnlocked()
   */
  public void eventUnlocked() throws SevereMessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventUnlocked");

    // modify prefetching info
    synchronized (this)
    {
      RemoteDispatchableKey dkey = key.getRemoteDispatchableKey();
      RemoteQPConsumerKey ck = null;
      if (dkey instanceof RemoteQPConsumerKey)
        ck = (RemoteQPConsumerKey) dkey;

      if (ck != null)
      {
        ck.messageUnlocked(key);
        informedConsumerKeyThatLocked = false;
      }
    }
    
    isReserved = false; // msg is no longer reserved for a particular consumer

    // Set the RMEUnlockCount for this msg's valueTick
    if (incLockCount)
      aih.incrementUnlockCount(getMessage().getGuaranteedRemoteGetValueTick());
    incLockCount = false;
    
    // call superclass
    super.eventUnlocked();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventUnlocked");
  }

  public int guessRedeliveredCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "guessRedeliveredCount");

    // add the count of the number of times it has been delivered at the DME with the local redelivery count
    int redeliveredCount = super.guessRedeliveredCount() + cachedRedeliveredCount;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "guessRedeliveredCount", Integer.valueOf(redeliveredCount));
    return redeliveredCount;
  }
  
  public boolean isRemoteGet() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isRemoteGet");
      SibTr.exit(tc, "isRemoteGet", Boolean.TRUE);
    }
    return true;
  }

  public void setRestoredTargetAOValue(AOValue restoredTargetAOValue) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setRestoredTargetAOValue", restoredTargetAOValue);
    
    this.restoredTargetAOValue = restoredTargetAOValue;
    isReserved = true; // the msg is now reserved for an AOValue
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setRestoredTargetAOValue");
  }

  public boolean isReserved() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isReserved");
      SibTr.exit(tc, "isReserved", isReserved);
    }
    return isReserved;
  }

  public void restoreAOData(long lockId) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restoreAOData");
  
    if (restoredTargetAOValue!=null)
    {      
      synchronized(restoredTargetAOValue)
      {
        if (!restoredTargetAOValue.isFlushing())
        {
          // Lock the msg to this AOValue
          
          SIMPTransactionManager txManager = aih.getMessageProcessor().getTXManager();
          Transaction tran = txManager.createAutoCommitTransaction();
          try
          {
            this.lockItemIfAvailable(lockId);
            this.persistLock(tran);          
            restoredTargetAOValue.setPLockId(this.getLockID());
            restoredTargetAOValue.setMsgId(this.getID());
          }
          catch (Exception e)
          {
            // FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.store.items.AIMessageItem.restoreAOData",
              "1:574:1.44.1.10",
              this);
            
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] { "com.ibm.ws.sib.processor.impl.store.items.AIMessageItem", "1:579:1.44.1.10" });
            
            isReserved = false;
          }          
        }
        else
          isReserved = false;
      }
      restoredTargetAOValue = null; // Release reference to AOValue so it can be cleaned up if necessary
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "restoreAOData");
  }

  @Override
  public void unlockMsg(long lockID, Transaction transaction, boolean incrementUnlock) 
  throws MessageStoreException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "unlockMsg", new Object[]{Long.valueOf(lockID), transaction, Boolean.valueOf(incrementUnlock)});
    
    this.incLockCount = incrementUnlock;    
    super.unlockMsg(lockID, transaction, incrementUnlock);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "unlockMsg");  
  }
}
