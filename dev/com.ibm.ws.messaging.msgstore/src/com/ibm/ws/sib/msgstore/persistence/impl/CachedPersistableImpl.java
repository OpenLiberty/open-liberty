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
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final Tuple _primaryPersistable;

    public CachedPersistableImpl(Persistable primaryPersistable)
    {
        _primaryPersistable = (Tuple) primaryPersistable;
    }

    public Persistable getPersistable()
    {
        return _primaryPersistable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationBegun()
     */
    @Override
    public void persistableOperationBegun() throws SevereMessageStoreException
    {
        _primaryPersistable.persistableOperationBegun();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationCompleted()
     */
    @Override
    public void persistableOperationCompleted() throws SevereMessageStoreException
    {
        _primaryPersistable.persistableOperationCompleted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableOperationCancelled()
     */
    @Override
    public void persistableOperationCancelled() throws SevereMessageStoreException
    {
        _primaryPersistable.persistableOperationCancelled();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#persistableRepresentationWasCreated()
     */
    @Override
    public boolean persistableRepresentationWasCreated()
    {
        return _primaryPersistable.persistableRepresentationWasCreated();
    }

    // Defect 496154
    @Override
    public int persistableOperationsOutstanding()
    {
        return _primaryPersistable.persistableOperationsOutstanding();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#requiresPersistence()
     */
    @Override
    public boolean requiresPersistence()
    {
        return _primaryPersistable.requiresPersistence();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setLogicallyDeleted(boolean)
     */
    @Override
    public void setLogicallyDeleted(boolean logicallyDeleted)
    {
        _primaryPersistable.setLogicallyDeleted(logicallyDeleted);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setPersistentTranId(com.ibm.ws.sib.msgstore.transactions.PersistentTranId)
     */
    @Override
    public void setPersistentTranId(PersistentTranId xid)
    {
        _primaryPersistable.setPersistentTranId(xid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setPermanentTableId(int)
     */
    @Override
    public void setPermanentTableId(int permanentTableId)
    {
        _primaryPersistable.setPermanentTableId(permanentTableId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getPermanentTableId()
     */
    @Override
    public int getPermanentTableId()
    {
        return _primaryPersistable.getPermanentTableId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setTemporaryTableId(int)
     */
    @Override
    public void setTemporaryTableId(int temporaryTableId)
    {
        _primaryPersistable.setTemporaryTableId(temporaryTableId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getTemporaryTableId()
     */
    @Override
    public int getTemporaryTableId()
    {
        return _primaryPersistable.getTemporaryTableId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setItemClassId(int)
     */
    @Override
    public void setItemClassId(int itemClassId)
    {
        _primaryPersistable.setItemClassId(itemClassId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getItemClassId()
     */
    @Override
    public int getItemClassId()
    {
        return _primaryPersistable.getItemClassId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setCanExpireSilently(boolean)
     */
    @Override
    public void setCanExpireSilently(boolean canExpireSilently)
    {
        _primaryPersistable.setCanExpireSilently(canExpireSilently);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getCanExpireSilently()
     */
    @Override
    public boolean getCanExpireSilently()
    {
        return _primaryPersistable.getCanExpireSilently();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setLockID(long)
     */
    @Override
    public void setLockID(long lockID)
    {
        _primaryPersistable.setLockID(lockID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getLockID()
     */
    @Override
    public long getLockID()
    {
        return _primaryPersistable.getLockID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setItemClassName(java.lang.String)
     */
    @Override
    public void setItemClassName(String classname)
    {
        _primaryPersistable.setItemClassName(classname);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getItemClassName()
     */
    @Override
    public String getItemClassName()
    {
        return _primaryPersistable.getItemClassName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setAbstractItemLink(com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink)
     */
    @Override
    public void setAbstractItemLink(AbstractItemLink link)
    {
        _primaryPersistable.setAbstractItemLink(link);
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
        return _primaryPersistable.getData();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setPersistentSize(int)
     */
    @Override
    public void setPersistentSize(int persistentDataSize)
    {
        _primaryPersistable.setPersistentSize(persistentDataSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPersistentSize()
     */
    @Override
    public int getPersistentSize()
    {
        return _primaryPersistable.getPersistentSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setInMemoryByteSize(int)
     */
    @Override
    public void setInMemoryByteSize(int byteSize)
    {
        _primaryPersistable.setInMemoryByteSize(byteSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getInMemoryByteSize()
     */
    @Override
    public int getInMemoryByteSize()
    {
        return _primaryPersistable.getInMemoryByteSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getUniqueId()
     */
    @Override
    public long getUniqueId()
    {
        return _primaryPersistable.getUniqueId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getContainingStreamId()
     */
    @Override
    public long getContainingStreamId()
    {
        return _primaryPersistable.getContainingStreamId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setPriority(int)
     */
    @Override
    public void setPriority(int priority)
    {
        _primaryPersistable.setPriority(priority);
    }

    @Override
    public void setRedeliveredCount(int redeliveredCount)
    {
        _primaryPersistable.setRedeliveredCount(redeliveredCount);
    }

    @Override
    public int getRedeliveredCount()
    {
        return _primaryPersistable.getRedeliveredCount();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPriority()
     */
    @Override
    public int getPriority()
    {
        return _primaryPersistable.getPriority();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setSequence(long)
     */
    @Override
    public void setSequence(long sequence)
    {
        _primaryPersistable.setSequence(sequence);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getSequence()
     */
    @Override
    public long getSequence()
    {
        return _primaryPersistable.getSequence();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setReferredID(long)
     */
    @Override
    public void setReferredID(long referredID)
    {
        _primaryPersistable.setReferredID(referredID);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getReferredID()
     */
    @Override
    public long getReferredID()
    {
        return _primaryPersistable.getReferredID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setStorageStrategy(int)
     */
    @Override
    public void setStorageStrategy(int storageStrategy)
    {
        _primaryPersistable.setStorageStrategy(storageStrategy);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getStorageStrategy()
     */
    @Override
    public int getStorageStrategy()
    {
        return _primaryPersistable.getStorageStrategy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getTupleType()
     */
    @Override
    public TupleTypeEnum getTupleType()
    {
        return _primaryPersistable.getTupleType();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setExpiryTime(long)
     */
    @Override
    public void setExpiryTime(long expiryTime)
    {
        _primaryPersistable.setExpiryTime(expiryTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getExpiryTime()
     */
    @Override
    public long getExpiryTime()
    {
        return _primaryPersistable.getExpiryTime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setDeliveryDelayTime(long)
     */
    @Override
    public void setDeliveryDelayTime(long deliveryDelayTime) {
        _primaryPersistable.setDeliveryDelayTime(deliveryDelayTime);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTime()
     */
    @Override
    public long getDeliveryDelayTime() {
        return _primaryPersistable.getDeliveryDelayTime();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTimeIsSuspect()
     */
    @Override
    public boolean getDeliveryDelayTimeIsSuspect() {
    	 return _primaryPersistable.getDeliveryDelayTimeIsSuspect();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getPersistentTranId()
     */
    @Override
    public PersistentTranId getPersistentTranId()
    {
        return _primaryPersistable.getPersistentTranId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#isLogicallyDeleted()
     */
    @Override
    public boolean isLogicallyDeleted()
    {
        return _primaryPersistable.isLogicallyDeleted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#createPersistable(long, com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum)
     */
    @Override
    public Persistable createPersistable(long uniqueID, TupleTypeEnum tupleType)
    {
        return _primaryPersistable.createPersistable(uniqueID, tupleType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#setContainingStream(com.ibm.ws.sib.msgstore.persistence.Persistable)
     */
    @Override
    public void setContainingStream(Persistable containingStream)
    {
        _primaryPersistable.setContainingStream(containingStream);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#getContainingStream()
     */
    @Override
    public Persistable getContainingStream()
    {
        return _primaryPersistable.getContainingStream();
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
        _primaryPersistable.setWasSpillingAtAddition(wasSpilling);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.persistence.Tuple#xmlWrite(com.ibm.ws.sib.msgstore.FormattedWriter)
     */
    @Override
    public void xmlWrite(FormattedWriter writer) throws IOException
    {
        _primaryPersistable.xmlWrite(writer);
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
