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

import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * This class is intended as a bridge between the MessageStoreInterface
 * component and the MessageStoreImplementation component.  It should 
 * only be used by MessageStore Code. 
 */
public interface ItemCollection extends Membership 
{
    /**
     * @param item
     * @param lockID id under which the item will be locked when the add is 
     * complete, or {@link AbstractItem#NO_LOCK_ID} if the added item is to
     * be unlocked.
     * @param transaction
     * 
     * @throws {@link OutOfCacheSpace} if there is not enough space in the 
     * unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     *  
     * @throws {@link StreamIsFull} if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     *
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public void addItem(Item item, long lockID, final Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException;

    /**
     * @param itemStream
     * @param lockID id under which the itemStream will be locked when the add is 
     * complete, or {@link AbstractItem#NO_LOCK_ID} if the added itemStream is to
     * be unlocked.
     * @param transaction
     * 
     * @throws {@link OutOfCacheSpace} if there is not enough space in the 
     * unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     *  
     * @throws {@link StreamIsFull} if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     *
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public void addItemStream(ItemStream itemStream, long lockID, final Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException;

    /**
     * @param referenceStream
     * @param lockID id under which the referenceStream will be locked when the add is 
     * complete, or {@link AbstractItem#NO_LOCK_ID} if the added referenceStream is to
     * be unlocked.
     * @param transaction
     * 
     * @throws {@link OutOfCacheSpace} if there is not enough space in the 
     * unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     *  
     * @throws {@link StreamIsFull} if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     *
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public void addReferenceStream(ReferenceStream referenceStream, long lockID, final Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException;

    /**
     * Reply the item in the receiver with a matching ID.  The item returned
     * stream is neither removed from the message store nor locked for exclusive use
     * of the caller.
     * 
     * @param itemId
     * 
     * @return item found or null if none.
     * @throws MessageStoreException
     */
    public AbstractItem findById(long itemId) throws SevereMessageStoreException;

    /*
     * @param filter
     * @param transaction
     * @return {@link AbstractItem}
     */
    public AbstractItem findFirstMatchingItem(final Filter filter) throws MessageStoreException;

    public ItemStream findFirstMatchingItemStream(final Filter filter) throws MessageStoreException;
    public ReferenceStream findFirstMatchingReferenceStream(final Filter filter) throws MessageStoreException;

    /**
     * Find the item that has been known to the stream for longest.  The item returned
     * may be in any of the states defined in the state model. The caller should not
     * assume that the item can be used for any particular purpose.
     * @return Item, may be null.
     * @throws {@link MessageStoreException} if the item was spilled and could not
     * be unspilled.  Or if item not found in backing store.
     */
    public AbstractItem findOldestItem() throws MessageStoreException;

    /**
     * @return owning itemStream or null if none.
     */
    public ItemStream getOwningItemStream() throws SevereMessageStoreException;

    public Statistics getStatistics() throws SevereMessageStoreException;

    /**
     * @return true if the list is spilling.
     */
    public boolean isSpilling();

    /*
     * @param filter
     * @param jumpbackEnabled
     * @return
     */
    public LockingCursor newLockingItemCursor(Filter filter, boolean jumpbackEnabled) throws PersistenceException, SevereMessageStoreException;

    /*
     * @param filter
     * @return
     */
    public NonLockingCursor newNonLockingItemCursor(Filter filter) throws SevereMessageStoreException;
    public NonLockingCursor newNonLockingItemStreamCursor(Filter filter);
    public NonLockingCursor newNonLockingReferenceStreamCursor(Filter filter);

    /*
     * This method is part of the interface between the MessageStoreInterface
     * component and the MessageStoreImplementation component.  It should 
     * only be used by MessageStore Code.<br> 
     * @param filter
     * @param transaction
     * @return {@link AbstractItem}
     */
    public AbstractItem removeFirstMatchingItem(final Filter filter, final Transaction transaction) throws MessageStoreException;

    public ItemStream removeFirstMatchingItemStream(final Filter filter, final Transaction transaction) throws MessageStoreException;
    
    public ReferenceStream removeFirstMatchingReferenceStream(final Filter filter, final Transaction transaction) throws MessageStoreException;

    public void setWatermarks(long countLow, long countHigh, long bytesLow, long bytesHigh) ;
}
