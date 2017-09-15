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

package com.ibm.ws.sib.processor.impl.interfaces;

import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * An interface to allow access to Message Processor messages (MessageItem and
 * MessageItemReference).
 *
 * @author tevans
 */
public interface SIMPMessage extends ControllableResource, TransactionCallback, JsMessageWrapper
{  
  /**
   * Get the message's Reliability
   *
   * @return the message's Reliability
   */
  public Reliability getReliability();

  /**
   * Get the message's Priority
   *
   * @return the message's Priority
   */
  public int getPriority();

  /**
   * Returns the producerConnectionUuid.
   * @return SIBUuid12
   */
  public SIBUuid12 getProducerConnectionUuid();

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString();
  
  /**
   * Tells us if this message requires an new Id
   */
  public boolean getRequiresNewId();
  
  /**
   * Call this when the message requires a new Id
   */
  public void setRequiresNewId(boolean value);
  
  /**
  * Call this to restore the message from message store 
  * only if the message is available in the store
  */
  public JsMessage getMessageIfAvailable();

  /**
   * Was this message put transactionally
   *
   * @return true if this message was put transactionally
   */
  public boolean isTransacted();

  /**
   * @return true if the receiver is an instance of
   * ItemReference,  false otherwise. Default
   * implementation returns false.
   */
  public boolean isItemReference();

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#unlock(long)
   * @param lockId
   * @throws MessageStoreException
   */
 // public void unlock(long lockId) throws MessageStoreException;

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#unlock(long, Transaction)
   */
 // public void unlock(long lockID, Transaction transaction) throws MessageStoreException;

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#unlock(long, Transaction, boolean)
   */
 // public void unlock(long lockID, Transaction transaction, boolean incrementCount) throws MessageStoreException;

  /**
   * Wrapper class for unlock method
   * @param lockID
   * @param transaction
   * @param incrementUnlock
   * @throws MessageStoreException
   */
  public void unlockMsg(long lockID, Transaction transaction, boolean incrementUnlock) throws MessageStoreException;
  
  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#remove(Transaction, long)
   * @param transaction
   * @param lockId
   * @throws MessageStoreException
   */
  public void remove(Transaction transaction, long lockId)
    throws MessageStoreException;

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#guessBackoutCount()
   */
  public int guessBackoutCount();

  /**
   * The number of times this message may have been delivered
   * in the past. Typically implemented using
   * @see com.ibm.ws.sib.msgstore.AbstractItem#guessUnlockCount()
   */
  public int guessRedeliveredCount();

  /**
   * This seed is used to provide some degree of fairness when choosing
   * a start point in a set of ConsumerPoints. It should be different for
   * each put performed by a particular producer session.
   * @return The seed given by the producer session.
   */
  public int getProducerSeed();

  /**
   * Returns the lockId used to lock this message. This should only
   * be called from the rolledbackRemove callbacks.
   */
  public long getLockID() throws MessageStoreException;

  /**
   * Callback that the message has been locked
   */
  public void eventLocked();
  public void eventUnlocked() throws SevereMessageStoreException;
  
  public void registerMessageEventListener(int event, MessageEventListener listener);
  public void deregisterMessageEventListener(int event, MessageEventListener listener);

  public boolean lockItemIfAvailable(long lockID) throws MessageStoreException;

  public void persistLock(final Transaction transaction) throws MessageStoreException;
  public boolean isPersistentlyLocked();

  /**
   * Returns the Message Store's unique identifier for the underlying
   * item or itemreference
   */
  public long getID() throws MessageStoreException;

  /**
   * Calculate the wait time of this message since the last wait time update.
   *
   * @param timeNow  The time used to calculate the waited time.  Normally
   * we would expect this to be System.currentTimeMillis()
   *
   * @return Wait time since the last wait time update.
   */
  public long calculateWaitTimeUpdate(long timeNow);

  /**
   * Return aggregate wait time.
   *
   * @return  Aggregate wait time.
   *
   * @throws SIResourceException  Message property retrieval fails.
   */
  public long getAggregateWaitTime();

  /**
   * Return the latest wait time used to update the aggregate wait time.
   *
   * @return  Latest wait time update.
   *
   * @throws SIResourceException  Message property retrieval fails.
   */
  public long getLatestWaitTimeUpdate();

  /**
   * Method isToBeStoredAtSendTime.
   * <p>Returns true if the message is to be stored at send time, or false otherwise</p>
   * @return boolean
   */
  public boolean isToBeStoredAtSendTime();

  /**
   * Method storeAtSendTime.
   * <p>Indicates that the message should be stored at send time rather than
   * at pre-prepare time.  Currently implemented for pt-to-pt messages only</p>
   */
  public void setStoreAtSendTime(boolean store);
  
  /**
   * Sets a flag to indicate whether we guessed the 
   * the stream to use for this message
   *
   * @param streamIsGuess flag
   */
  public void setStreamIsGuess(boolean streamIsGuess);

  /**
   * Gets the flag which indicates whether we guessed the 
   * the stream to use for this message
   * 
   * @return boolean streamIsGuess flag
   */
  public boolean getStreamIsGuess();
  
  /**
   * Returns a cached version of the GuraranteedStreamUuid from the JsMessage
   */
  public SIBUuid12 getGuaranteedStreamUuid();

  /**
   * Sets the GuaranteedStreamUuid in the JsMessage and caches it
   */  
  public void setGuaranteedStreamUuid(SIBUuid12 uuid);
  
  /**
   * Returns the reportCOD value from the underlying message
   */
  public Byte getReportCOD();

  /**
   * 
   * @return true if the message came from another ME
   * 
   */
  public boolean isFromRemoteME(); 
  
  /**
   * Sets the message wait time for statistics
   */
  public long updateStatisticsMessageWaitTime();

  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#isLocked()
   */
  public boolean isLocked();
  
  /**
   * @see com.ibm.ws.sib.msgstore.AbstractItem#getTransactionId()
   */
  public PersistentTranId getTransactionId();
    
  /**
    * @see com.ibm.ws.sib.msgstore.AbstractItem#isInStore()
    */
  public boolean isInStore();
  
  /**
   * Is the message being re-driven due to an unlock?
   * @return boolean true - reavailable
   */
  public boolean isReavailable();
  
  /**
   * Set state on the msg to indicate it's redelivery count has been reached
   */
  public void setRedeliveryCountReached();
  
  /**
   * Releases the JsMessage from this SIMPMessage. This method should only
   * be called when it is known that we won't need the JsMessage in the near
   * future, as although we can restore the JsMessage, it will be a high 
   * performance cost.
   * 
   * If the JsMessage is not safe to be release from this SIMPMessage (because
   * the message hasn't been persisted yet) then it won't be and no exception
   * will be thrown.
   *
   */
  void releaseJsMessage();
  
  /**
   * Marks this message as been hidden. This has the affect of the 
   * message not been unlocked when it would normally be made available
   * for a consumer.
   * 
   * @param hiddenMessage
   */
  void markHiddenMessage(boolean hiddenMessage);
  
  /**
   * 
   * @return Is this message hidden
   */
  boolean isHidden();

  /**
   * Set the time that a message which is hidden should be unhidden.
   * 
   * @param expiryTime
   */
  public void setHiddenExpiryTime(long expiryTime);

  /**
   * If the message has been hidden then return the time at which it should be unhidden.
   * 
   * @return
   */
  public long getHiddenExpiryTime();  
  
  /**
   * 
   * @param messagingEngineUuid
   */
  public void setLocalisingME(SIBUuid8 messagingEngineUuid);

  /**
   * 
   * @return
   */
  public SIBUuid8 getLocalisingMEUuid();
  
  /**
   * Set the message control classification and cache it
   */
  public void setMessageControlClassification(String classification);
  
  /**
   * Get the message control classification
   */  
  public String getMessageControlClassification(boolean throwExceptionIfMessageNotAvailable);

  /**
   * If this msg is a being remotely received and then rejected, we need to 
   * take into account how many times it was rolledback on the remote ME
   * before it came back to us.
   * @param rmeUnlockCount
   */
  public void setRMEUnlockCount(long rmeUnlockCount);
}
