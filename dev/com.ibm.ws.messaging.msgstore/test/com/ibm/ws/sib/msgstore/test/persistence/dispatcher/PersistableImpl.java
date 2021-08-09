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
package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;

/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * ----------      -------- -------- ------------------------------------------
 * 184390.1.1      26/01/04 schofiel Revised Reliability Qualities of Service - MS - Tests for spill
 * 191575          20/02/04 pradine  Add support for MAXDEPTH column in the Item Table
 * 184390.1.3      27/02/04 schofiel Revised Reliability Qualities of Service - MS - PersistentDispatcher
 * 188051          29/03/04 pradine  Add support for temporary tables
 * 206970          03/06/04 schofiel Sort out to-dos in SpillDispatcher
 * 215986          13/07/04 pradine  Split the Persistable interface
 * 223636.2        26/08/04 corrigk  Consolidate dump
 * 247513          14/01/05 schofiel Improve spilling performance
 * 321392          05/12/05 schofiel Complete removal of tick value
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * SIB0112d.ms.2   28/06/07 gareth   MemMgmt: SpillDispatcher improvements - datastore
 * 463642          04/09/07 gareth   Revert to using spill limits
 * 496154          22/04/07 gareth   Improve spilling performance
 * 538096          25/07/08 susana   Use getInMemorySize for spilling & persistence
 * F1332-51592     13/10/11 vmadhuka Persist redelivery count
 * ============================================================================
 */

import java.io.IOException;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.persistence.impl.Tuple;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

public class PersistableImpl implements Tuple {
    private static TraceComponent tc = SibTr.register(PersistableImpl.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final int _id;
    private final PersistableEventDispatchListener _listener;
    private int _persistentDataSize;
    private int _inMemoryByteSize;
    private int _opsBegun;
    private int _opsCancelled;
    private int _opsCompleted;

    PersistableImpl() {
        this(0, null);
    }

    PersistableImpl(int id) {
        this(id, null);
    }

    PersistableImpl(PersistableEventDispatchListener listener) {
        this(0, listener);
    }

    PersistableImpl(int id, PersistableEventDispatchListener listener) {
        _id = id;
        _listener = listener;
        _persistentDataSize = 0;
    }

    @Override
    public synchronized void persistableOperationBegun() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "persistableOperationBegun");

        _opsBegun++;
        if (_listener != null)
            _listener.eventDispatchBegun(this);

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "persistableOperationBegun");
    }

    @Override
    public synchronized void persistableOperationCompleted() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "persistableOperationCompleted");

        _opsCompleted++;
        if (_listener != null)
            _listener.eventDispatchCompleted(this);

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "persistableOperationCompleted");
    }

    @Override
    public synchronized void persistableOperationCancelled() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "persistableOperationCancelled");

        _opsCancelled++;
        if (_listener != null)
            _listener.eventDispatchCancelled(this);

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "persistableOperationCancelled");
    }

    @Override
    public synchronized boolean persistableRepresentationWasCreated() {
        return (_opsCompleted > 0);
    }

    // Defect 496154
    @Override
    public synchronized int persistableOperationsOutstanding() {
        return (_opsBegun - _opsCompleted - _opsCancelled);
    }

    @Override
    public boolean requiresPersistence() {
        return true;
    }

    @Override
    public void setLogicallyDeleted(boolean logicallyDeleted) {}

    @Override
    public void setPersistentTranId(PersistentTranId xid) {}

    @Override
    public void setPermanentTableId(int permanentTableId) {}

    @Override
    public int getPermanentTableId() {
        return 0;
    }

    @Override
    public void setTemporaryTableId(int temporaryTableId) {}

    @Override
    public int getTemporaryTableId() {
        return 0;
    }

    @Override
    public void setItemClassId(int itemClassId) {}

    @Override
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
        return null;
    }

    @Override
    public void setAbstractItemLink(AbstractItemLink link) {}

    @Override
    public List<DataSlice> getData() {
        return null;
    }

    @Override
    public void setPersistentSize(int persistentDataSize) {
        _persistentDataSize = persistentDataSize;
    }

    @Override
    public int getPersistentSize() {
        return _persistentDataSize;
    }

    @Override
    public void setInMemoryByteSize(int byteSize) {
        _inMemoryByteSize = byteSize;
    }

    @Override
    public int getInMemoryByteSize() {
        return _inMemoryByteSize;
    }

    @Override
    public long getUniqueId() {
        return _id;
    }

    @Override
    public long getContainingStreamId() {
        return 0;
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
        return 0;
    }

    @Override
    public void setReferredID(long referredID) {}

    @Override
    public long getReferredID() {
        return 0;
    }

    @Override
    public void setStorageStrategy(int storageStrategy) {}

    @Override
    public int getStorageStrategy() {
        return 0;
    }

    public void setMaxDepth(int maxDepth) {}

    public int getMaxDepth() {
        return 0;
    }

    @Override
    public TupleTypeEnum getTupleType() {
        return null;
    }

    @Override
    public void setExpiryTime(long expiryTime) {}

    @Override
    public long getExpiryTime() {
        return 0;
    }

    @Override
    public PersistentTranId getPersistentTranId() {
        return null;
    }

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
    public void setDeliveryDelayTime(long deliveryDelayTime) {}

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
