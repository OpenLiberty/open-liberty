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
package com.ibm.ws.sib.msgstore.test.transactions;

/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *                 24/11/04 gareth   Test transaction callback contracts
 * 247513          14/01/05 schofiel Improve spilling performance
 * 321392          05/12/05 schofiel Complete removal of tick value
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * SIB0112d.ms.2   28/06/07 gareth   MemMgmt: SpillDispatcher improvements - datastore
 * 463642          04/09/07 gareth   Revert to using spill limits
 * 538096          25/07/08 susana   Use getInMemorySize for spilling & persistence
 * F1332-51592     13/10/11 vmadhuka Persist redelivery count
 * ============================================================================
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

public class NullPersistable implements Persistable {
    public void persistableOperationBegun() {}

    public void persistableOperationCompleted() {}

    public void persistableOperationCancelled() {}

    public boolean persistableIsOperationInProgress() {
        return false;
    }

    public boolean persistableRepresentationWasCreated() {
        return false;
    }

    @Override
    public boolean requiresPersistence() {
        return true;
    }

    public void setPermanentTableId(int permanentTableId) {}

    public int getPermanentTableId() {
        return 0;
    }

    public void setTemporaryTableId(int temporaryTableId) {}

    public int getTemporaryTableId() {
        return 0;
    }

    public void setItemClassId(int itemClassId) {}

    public int getItemClassId() {
        return 0;
    }

    @Override
    public void setCanExpireSilently(boolean canExpireSilently) {}

    @Override
    public boolean getCanExpireSilently() {
        return false;
    }

    @Override
    public void setLockID(long lockID) {}

    @Override
    public long getLockID() {
        return 0;
    }

    @Override
    public void setItemClassName(String classname) {}

    @Override
    public String getItemClassName() {
        return "NullPersistable";
    }

    @Override
    public void setAbstractItemLink(AbstractItemLink link) {}

    @Override
    public List<DataSlice> getData() {
        List<DataSlice> list = new ArrayList<DataSlice>(1);
        list.add(new DataSlice("Null".getBytes()));
        return list;
    }

    @Override
    public void setPersistentSize(int persistentDataSize) {}

    @Override
    public int getPersistentSize() {
        return 4;
    }

    @Override
    public void setInMemoryByteSize(int byteSize) {}

    @Override
    public int getInMemoryByteSize() {
        return 16;
    }

    @Override
    public long getUniqueId() {
        return 0L;
    }

    @Override
    public long getContainingStreamId() {
        return 0L;
    }

    @Override
    public void setPriority(int priority) {}

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void setRedeliveredCount(int redeliveredCount) {}

    @Override
    public int getRedeliveredCount() {
        return 0;
    }

    @Override
    public void setSequence(long sequence) {}

    @Override
    public long getSequence() {
        return 0L;
    }

    @Override
    public void setReferredID(long referredID) {}

    @Override
    public long getReferredID() {
        return 0L;
    }

    @Override
    public void setStorageStrategy(int storageStrategy) {}

    @Override
    public int getStorageStrategy() {
        return 0;
    }

    @Override
    public TupleTypeEnum getTupleType() {
        return TupleTypeEnum.ITEM;
    }

    @Override
    public void setExpiryTime(long expiryTime) {}

    @Override
    public long getExpiryTime() {
        return 0L;
    }

    public int getMaxDepth() {
        return 1;
    }

    public void setMaxDepth(int depth) {}

    @Override
    public void setPersistentTranId(PersistentTranId xid) {}

    @Override
    public PersistentTranId getPersistentTranId() {
        return null;
    }

    @Override
    public void setLogicallyDeleted(boolean logicallyDeleted) {}

    @Override
    public boolean isLogicallyDeleted() {
        return false;
    }

    @Override
    public Persistable createPersistable(long uniqueID, TupleTypeEnum tupleType) {
        return null;
    }

    @Override
    public void setContainingStream(Persistable containingStream) {}

    @Override
    public Persistable getContainingStream() {
        return null;
    }

    // Defect 463642
    // Revert to using spill limits previously removed in SIB0112d.ms.2
    @Override
    public void setWasSpillingAtAddition(boolean wasSpilling) {}

    @Override
    public void xmlWrite(FormattedWriter writer) throws IOException {}

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#setDeliveryDelayTime(long)
     */
    @Override
    public void setDeliveryDelayTime(long deliveryDelayTime) {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTime()
     */
    @Override
    public long getDeliveryDelayTime() {

        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.sib.msgstore.persistence.Persistable#getDeliveryDelayTimeIsSuspect()
     */
    //@Override
    public boolean getDeliveryDelayTimeIsSuspect() {
        return false;
    }
}
