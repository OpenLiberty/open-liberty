package com.ibm.ws.sib.msgstore.persistence.impl;

/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * The abstract base class for the cached data used in the Tasks.
 */
public abstract class CachedPersistableImpl implements Tuple
{
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final Tuple _masterPersistable;

    public CachedPersistableImpl(Persistable masterPersistable)
    {
        _masterPersistable = (Tuple) masterPersistable;
    }

    public Persistable getPersistable()
    {
        return _masterPersistable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationBegun()
     */
    @Override
    public void persistableOperationBegun() throws SevereMessageStoreException
    {
        _masterPersistable.persistableOperationBegun();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationCompleted()
     */
    @Override
    public void persistableOperationCompleted() throws SevereMessageStoreException
    {
        _masterPersistable.persistableOperationCompleted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationCancelled()
     */
    @Override
    public void persistableOperationCancelled() throws SevereMessageStoreException
    {
        _masterPersistable.persistableOperationCancelled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableRepresentationWasCreated()
     */
    @Override
    public boolean persistableRepresentationWasCreated()
    {
        return _masterPersistable.persistableRepresentationWasCreated();
    }

    // Defect 496154
    @Override
    public int persistableOperationsOutstanding()
    {
        return _masterPersistable.persistableOperationsOutstanding();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#requiresPersistence()
     */
    @Override
    public boolean requiresPersistence()
    {
        return _masterPersistable.requiresPersistence();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setLogicallyDeleted(boolean)
     */
    @Override
    public void setLogicallyDeleted(boolean logicallyDeleted)
    {
        _masterPersistable.setLogicallyDeleted(logicallyDeleted);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setPersistentTranId(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    @Override
    public void setPersistentTranId(PersistentTranId xid)
    {
        _masterPersistable.setPersistentTranId(xid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setPermanentTableId(int)
     */
    @Override
    public void setPermanentTableId(int permanentTableId)
    {
        _masterPersistable.setPermanentTableId(permanentTableId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getPermanentTableId()
     */
    @Override
    public int getPermanentTableId()
    {
        return _masterPersistable.getPermanentTableId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setTemporaryTableId(int)
     */
    @Override
    public void setTemporaryTableId(int temporaryTableId)
    {
        _masterPersistable.setTemporaryTableId(temporaryTableId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getTemporaryTableId()
     */
    @Override
    public int getTemporaryTableId()
    {
        return _masterPersistable.getTemporaryTableId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setItemClassId(int)
     */
    @Override
    public void setItemClassId(int itemClassId)
    {
        _masterPersistable.setItemClassId(itemClassId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getItemClassId()
     */
    @Override
    public int getItemClassId()
    {
        return _masterPersistable.getItemClassId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setCanExpireSilently(boolean)
     */
    @Override
    public void setCanExpireSilently(boolean canExpireSilently)
    {
        _masterPersistable.setCanExpireSilently(canExpireSilently);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getCanExpireSilently()
     */
    @Override
    public boolean getCanExpireSilently()
    {
        return _masterPersistable.getCanExpireSilently();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setLockID(long)
     */
    @Override
    public void setLockID(long lockID)
    {
        _masterPersistable.setLockID(lockID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getLockID()
     */
    @Override
    public long getLockID()
    {
        return _masterPersistable.getLockID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setItemClassName(java.lang.String)
     */
    @Override
    public void setItemClassName(String classname)
    {
        _masterPersistable.setItemClassName(classname);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getItemClassName()
     */
    @Override
    public String getItemClassName()
    {
        return _masterPersistable.getItemClassName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setAbstractItemLink(com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink)
     */
    @Override
    public void setAbstractItemLink(AbstractItemLink link)
    {
        _masterPersistable.setAbstractItemLink(link);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getData()
     */
    // Feature SIB0112b.ms.1
    @Override
    public List<DataSlice> getData() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        return _masterPersistable.getData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setPersistentSize(int)
     */
    @Override
    public void setPersistentSize(int persistentDataSize)
    {
        _masterPersistable.setPersistentSize(persistentDataSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPersistentSize()
     */
    @Override
    public int getPersistentSize()
    {
        return _masterPersistable.getPersistentSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setInMemoryByteSize(int)
     */
    @Override
    public void setInMemoryByteSize(int byteSize)
    {
        _masterPersistable.setInMemoryByteSize(byteSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getInMemoryByteSize()
     */
    @Override
    public int getInMemoryByteSize()
    {
        return _masterPersistable.getInMemoryByteSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getUniqueId()
     */
    @Override
    public long getUniqueId()
    {
        return _masterPersistable.getUniqueId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getContainingStreamId()
     */
    @Override
    public long getContainingStreamId()
    {
        return _masterPersistable.getContainingStreamId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setPriority(int)
     */
    @Override
    public void setPriority(int priority)
    {
        _masterPersistable.setPriority(priority);
    }

    @Override
    public void setRedeliveredCount(int redeliveredCount)
    {
        _masterPersistable.setRedeliveredCount(redeliveredCount);
    }

    @Override
    public int getRedeliveredCount()
    {
        return _masterPersistable.getRedeliveredCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPriority()
     */
    @Override
    public int getPriority()
    {
        return _masterPersistable.getPriority();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setSequence(long)
     */
    @Override
    public void setSequence(long sequence)
    {
        _masterPersistable.setSequence(sequence);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getSequence()
     */
    @Override
    public long getSequence()
    {
        return _masterPersistable.getSequence();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setReferredID(long)
     */
    @Override
    public void setReferredID(long referredID)
    {
        _masterPersistable.setReferredID(referredID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getReferredID()
     */
    @Override
    public long getReferredID()
    {
        return _masterPersistable.getReferredID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setStorageStrategy(int)
     */
    @Override
    public void setStorageStrategy(int storageStrategy)
    {
        _masterPersistable.setStorageStrategy(storageStrategy);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getStorageStrategy()
     */
    @Override
    public int getStorageStrategy()
    {
        return _masterPersistable.getStorageStrategy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getTupleType()
     */
    @Override
    public TupleTypeEnum getTupleType()
    {
        return _masterPersistable.getTupleType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setExpiryTime(long)
     */
    @Override
    public void setExpiryTime(long expiryTime)
    {
        _masterPersistable.setExpiryTime(expiryTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getExpiryTime()
     */
    @Override
    public long getExpiryTime()
    {
        return _masterPersistable.getExpiryTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setDeliveryDelayTime(long)
     */
    @Override
    public void setDeliveryDelayTime(long deliveryDelayTime) {
        _masterPersistable.setDeliveryDelayTime(deliveryDelayTime);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTime()
     */
    @Override
    public long getDeliveryDelayTime() {
        return _masterPersistable.getDeliveryDelayTime();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTimeIsSuspect()
     */
    @Override
    public boolean getDeliveryDelayTimeIsSuspect() {
    	 return _masterPersistable.getDeliveryDelayTimeIsSuspect();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPersistentTranId()
     */
    @Override
    public PersistentTranId getPersistentTranId()
    {
        return _masterPersistable.getPersistentTranId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#isLogicallyDeleted()
     */
    @Override
    public boolean isLogicallyDeleted()
    {
        return _masterPersistable.isLogicallyDeleted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#createPersistable(long, com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum)
     */
    @Override
    public Persistable createPersistable(long uniqueID, TupleTypeEnum tupleType)
    {
        return _masterPersistable.createPersistable(uniqueID, tupleType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setContainingStream(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    @Override
    public void setContainingStream(Persistable containingStream)
    {
        _masterPersistable.setContainingStream(containingStream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getContainingStream()
     */
    @Override
    public Persistable getContainingStream()
    {
        return _masterPersistable.getContainingStream();
    }

    // Defect 463642 
    // Revert to using spill limits previously removed in SIB0112d.ms.2
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setWasSpillingAtAddition(boolean)
     */
    @Override
    public void setWasSpillingAtAddition(boolean wasSpilling)
    {
        _masterPersistable.setWasSpillingAtAddition(wasSpilling);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#xmlWrite(com.ibm.ws.sib.msgstore.FormattedWriter)
     */
    @Override
    public void xmlWrite(FormattedWriter writer) throws IOException
    {
        _masterPersistable.xmlWrite(writer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        //No trace neccessary
        return "uniqueId: " + getUniqueId()
               + ", containingStreamId: " + getContainingStreamId()
               + ", className: " + getItemClassName()
               + ", classId: " + getItemClassId()
               + ", dataSize: " + getPersistentSize()
               + ", permanentTableId: " + getPermanentTableId()
               + ", temporaryTableId: " + getTemporaryTableId()
               + ", storageStrategy: " + getStorageStrategy()
               + ", tupleType: " + getTupleType()
               + ", priority: " + getPriority()
               + ", sequence: " + getSequence()
               + ", canExpireSilently: " + getCanExpireSilently()
               + ", lockId: " + getLockID()
               + ", referredId: " + getReferredID()
               + ", expiryTime: " + getExpiryTime()
               + ", logicallyDeleted: " + isLogicallyDeleted()
               + ", xid: " + getPersistentTranId()
               + ", deliveryDelayTime: " + getDeliveryDelayTime()
               + ", deliveryDelayTimeIsSuspect: " +getDeliveryDelayTimeIsSuspect()
               + LINE_SEPARATOR;
    }
}
