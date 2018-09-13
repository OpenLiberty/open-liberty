package com.ibm.ws.sib.msgstore.persistence;

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

import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * This interface forms the contract between the persistence code and objects which
 * are persisted.
 */
public interface Persistable
{
    /**
     * Indicates whether the object requires a persistent representation.
     * The return value for a given instance must always be the same.
     * Instances which are {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_NEVER} always return false, instances which are
     * {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_ALWAYS} or {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_EVENTUALLY} always return true,
     * and instances which are {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_MAYBE} returns true if
     * they are spilling and false if they are not.
     * 
     * @return true if the object requires a persistent representation
     */
    public boolean requiresPersistence();

    /**
     * Determines whether the item can be removed from the persistent store, due
     * to expiry, without reinstantiating the item in order to perform callbacks
     * on it.
     * 
     * @param canExpireSilently is <code>true</code> if the item can be expired
     *            silently, <code>false</code> otherwise
     */
    public void setCanExpireSilently(boolean canExpireSilently);

    /**
     * Indicates whether the item can be removed from the persistent store, due
     * to expiry, without reinstantiating the item in order to perform callbacks
     * on it.
     * 
     * @return <code>true</code> if the item can be expired silently, <code>false</code> otherwise
     */
    public boolean getCanExpireSilently();

    /**
     * Sets the lock id
     * 
     * @param lockID the lock id
     */
    public void setLockID(long lockID);

    /**
     * Returns the lock id
     * 
     * @return the lock id
     */
    public long getLockID();

    /**
     * Sets the java class name of the item being stored
     * 
     * @param classname the class name
     */
    public void setItemClassName(String classname);

    /**
     * Returns the the java class name of item being stored.
     * 
     * @return the class name
     */
    public String getItemClassName();

    /**
     * Stores the {@link AbstractItemLink} object. This is stored because it is
     * the link to the serialized binary data.
     * 
     * @param link the {@link AbstractItemLink}
     */
    public void setAbstractItemLink(AbstractItemLink link);

    /**
     * Returns the serialized binary data.
     * 
     * @return the binary data
     * @throws PersistentDataEncodingException
     */
    // FSIB0112b.ms.1
    public List<DataSlice> getData() throws PersistentDataEncodingException, SevereMessageStoreException;

    /**
     * Set the size of the serialized binary data
     * 
     * @param persistentDataSize the size of the binary data
     */
    public void setPersistentSize(int persistentDataSize);

    /**
     * Returns the size of the serialized binary data
     * 
     * @return
     *         A return value less than zero will be taken to mean that
     *         the size hase not been set.
     *         A return value of zero will be taken to mean that there
     *         is no data to persist
     *         A return value greater than zero will be taken as an approximate
     *         size only. To get the actual size, retrieve the byte[] then do
     *         array.length
     */
    public int getPersistentSize();

    /**
     * Set the (approximate) maximum size of the data in memory
     * 
     * @param byteSize the size of the binary data
     */
    public void setInMemoryByteSize(int byteSize);

    /**
     * Returns the (approximate) maximum size of the data in memory
     * 
     * @return
     *         A return value less than zero will be taken to mean that
     *         the size hase not been set.
     *         A return value of zero will be taken to mean that there
     *         is no data to persist
     *         A return value greater than zero will be taken as an approximate
     *         size, in memory, of the data in its worst case. (For a message this means
     *         being both encoded and fully-fluffed.)
     */
    public int getInMemoryByteSize();

    /**
     * Returns the id of the <code>Persistable</code>.
     * 
     * @return the id
     */
    public long getUniqueId();

    /**
     * Returns the id of the stream that contains the <code>Persistable</code>.
     * 
     * @return the id of the stream
     * @see TupleTypeEnum#ITEM_STREAM
     */
    public long getContainingStreamId();

    /**
     * Sets the message priority
     * 
     * @param priority the message priority
     */
    public void setPriority(int priority);

    /**
     * Returns the message priority.
     * 
     * @return the message priority
     */
    public int getPriority();

    /**
     * Sets the message redelivery count
     * 
     * @param redeliverycount the message redelivery count
     */
    public void setRedeliveredCount(int redeliveredCount);

    /**
     * Returns the message redelivery count.
     * 
     * @return the message redelivery count
     */
    public int getRedeliveredCount();

    /**
     * Sets the sequence number for the message
     * 
     * @param sequence the sequence number for the message
     */
    public void setSequence(long sequence);

    /**
     * Returns the sequence number of the message.
     * 
     * @return the sequence number for the message
     */
    public long getSequence();

    /**
     * Set to the id of the <code>Persistable</code> that we want to refer to.
     * Typically, this will only be set for item references.
     * 
     * @param referredID the unique id if the <code>Persistable</code> that we
     *            want to refer to.
     */
    public void setReferredID(long referredID);

    /**
     * Returns the id of a <code>Persistable</code> that is referenced. Typically,
     * this will only be set for an item reference.
     * 
     * @return (long) the id of the referred item if this tuple represents
     */
    public long getReferredID();

    /**
     * Sets the storeage strategy for the <code>Persistable</code>. The
     * storage strategy determines the route that the <code>Persistable</code>
     * will take to the persistent store. The meanings of the values are
     * currently represented by the constants {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_ALWAYS}, {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_EVENTUALLY},
     * {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_MAYBE}, {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_NEVER}.
     * 
     * @param storageStrategy the storeage strategy
     */
    public void setStorageStrategy(int storageStrategy);

    /**
     * Returns the storage strategy for the <code>Persistable</code>. The
     * storage strategy determines the route that the <code>Persistable</code>
     * will take to the persistent store. The meanings of the values are
     * currently represented by the constants {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_ALWAYS}, {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_EVENTUALLY},
     * {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_MAYBE}, {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_NEVER}.
     * 
     * @return an integer representing the storage strategy.
     */
    public int getStorageStrategy();

    /**
     * Returns the type of the <code>Persistable</code>
     * 
     * @return TupleTypeEnum a {@link TupleTypeEnum} object to indicate the type
     *         of the Persistable
     */
    public TupleTypeEnum getTupleType();

    /**
     * Sets the expiry time
     * 
     * @param expiryTime the time (in milliseconds)
     */
    public void setExpiryTime(long expiryTime);

    /**
     * Returns the expiry time
     * 
     * @return the expiry time (in milliseconds)
     */
    public long getExpiryTime();

    /**
     * Sets the transaction id
     * 
     * @param xid a {@link PersistentTranId} object
     */
    public void setPersistentTranId(PersistentTranId xid);

    /**
     * Returns the transaction id
     * 
     * @return a {@link PersistentTranId} object
     */
    public PersistentTranId getPersistentTranId();

    /**
     * Set the deleted status of the <code>Persistable</code>.
     * 
     * @param logicallyDeleted <code>true</code> indicates that the
     *            <code>Persistable</code> has been deleted even though it has
     *            not been removed from the persistence mechanism, <code>false</code>
     *            means that it has not been deleted
     */
    public void setLogicallyDeleted(boolean logicallyDeleted);

    /**
     * Returns the deleted status of the <code>Persistable</code>
     * 
     * @return <code>true</code> indicates that the
     *         <code>Persistable</code> has been deleted even though it has
     *         not been removed from the persistence mechanism, <code>false</code>
     *         means that it has not been deleted
     */
    public boolean isLogicallyDeleted();

    /**
     * Factory method for <code>Persistable</code> objects
     * 
     * @param uniqueID the id to give the <code>Persistable</code>
     * @param tupleType a {@link TupleTypeEnum} object to indicate the type you want
     * @return a <code>Persistable</code> object
     */
    public Persistable createPersistable(long uniqueID, TupleTypeEnum tupleType);

    /**
     * Sets up a link between the <code>Persistable</code> object and the stream
     * that contains it.
     * 
     * @param containingStream the <code>Persistable</code> of the containing stream.
     * @see TupleTypeEnum#ITEM_STREAM
     */
    public void setContainingStream(Persistable containingStream);

    /**
     * Returns the stream that contains the <code>Persistable</code>
     * 
     * @return the <code>Persistable</code> of the containing stream
     * @see TupleTypeEnum#ITEM_STREAM
     */
    public Persistable getContainingStream();

    // Defect 463642 
    // Revert to using spill limits previously removed in SIB0112d.ms.2
    /**
     * Indicates whether the stream was in spilling mode when an item is added to it.
     * This status is significant only for {@link com.ibm.ws.sib.msgstore.AbstractItem#STORE_MAYBE} items
     * 
     * @param wasSpilling
     * @see TupleTypeEnum#ITEM_STREAM
     */
    public void setWasSpillingAtAddition(boolean wasSpilling);

    /**
     * Used to dump the internal state of the message store
     * 
     * @param writer
     * @throws IOException
     */
    public void xmlWrite(FormattedWriter writer) throws IOException;

    /**
     * Sets the delivery delay time
     * 
     * @param deliveryDelayTime the time (in milliseconds)
     */
    public void setDeliveryDelayTime(long deliveryDelayTime);

    /**
     * Returns the delivery delay time
     * 
     * @return the delivery delay time (in milliseconds)
     */
    public long getDeliveryDelayTime();

	/**
	 * @return true if the deliveryDelayTime was migrated from 
	 *              serialVersionUIDV1 = 7600601884916130911L and might have been migrated incorrectly.
	 */
	public boolean getDeliveryDelayTimeIsSuspect();
}
