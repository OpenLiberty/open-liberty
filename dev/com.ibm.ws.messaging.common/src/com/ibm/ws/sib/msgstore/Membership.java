package com.ibm.ws.sib.msgstore;
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

import java.io.IOException;
import java.util.List;

import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * This class is intended as a bridge between the MessageStoreInterface
 * component and the MessageStoreImplementation component.  It should 
 * only be used by MessageStore Code. 
 */
public interface Membership
{
    /**
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @param lockID
     * @param transaction
     */
    public void cmdRemove(long lockID, Transaction transaction) throws MessageStoreException;

    /**
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @return long the receivers id
     */
    public long getID();

    /**
     * @return the lock ID if the item is currently locked, or {@link AbstractItem#NO_LOCK_ID} if not locked.
     * @throws MessageStoreException
     */
    public long getLockID() throws MessageStoreException;

    public MessageStore getMessageStore();

    /**
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @return the priority
     */
    public int getPriority();

    /**
     * @return transaction id or null;
     */
    public PersistentTranId getTransactionId() ;

    /**
     * @return an approximation to the number of attempted get operations
     * on the item that have been rolled back in the current session.  The
     * number is neither persistent, nor accurate, but can be used to
     * indicate a dangerous cycle in processing a 'bad' item.  Such occurrences
     * may be indicated by an ever-increasing backout count.
     */
    public int guessBackoutCount();

    /**
     * @return an approximation to the number of attempted lock operations
     * on the item that have been rescinded (unlocked) in the current session.  The
     * number is neither persistent, nor accurate, but can be used to
     * indicate a dangerous cycle in processing a 'bad' item.  Such occurrences
     * may be indicated by an ever-increasing unlock count.
     */
    public int guessUnlockCount();

    public abstract boolean isAdding();

    public abstract boolean isAvailable();

    public abstract boolean isExpiring();

    public abstract boolean isInStore();

    public abstract boolean isLocked();

    /**
     * @return true if the receiver is locked persistently.
     */
    public boolean isPersistentlyLocked();

    // Feature SIB0112le.ms.1
    // This method is used to allow AbstractItem to check if
    // it's data has made it to disk. Used in conjunction with
    // readDataFromPersistence() this can allow the Item to release
    // it's in memory copy of it's data as it knows that it
    // can be read back from disk at a later point.
    public boolean isPersistentRepresentationStable();

    public abstract boolean isRemoving();

    public abstract boolean isUpdating();

    public boolean lockItemIfAvailable(long lockID) throws SevereMessageStoreException;

    public void persistLock(final Transaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException;

    public int getPersistedRedeliveredCount() throws MessageStoreException;
    public void persistRedeliveredCount(int redeliveredCount) throws SevereMessageStoreException;
    // Feature SIB0112le.ms.1
    public List<DataSlice> readDataFromPersistence() throws SevereMessageStoreException;

    /**
     * Request an update
     * 
     * @param transaction
     * 
     * @throws MessageStoreException
     */
    public void requestUpdate(Transaction transaction) throws MessageStoreException;

    public void requestXmlWriteOn(FormattedWriter writer) throws IOException ;

    /**
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @param lockID
     * @param transaction transaction for unlocking if lock is persistent,
     * null if lock is not persistent.  If non-null and lock is non-persistent
     * then this argument is ignored.
     * @param incrementUnlockCountIfNonpersistent whether to increment the unlock count for a nonpersistent lock
     * @throws MessageStoreException
     */
    public void unlock(long lockID, Transaction transaction, boolean incrementUnlockCountIfNonpersistent) throws MessageStoreException;

}
