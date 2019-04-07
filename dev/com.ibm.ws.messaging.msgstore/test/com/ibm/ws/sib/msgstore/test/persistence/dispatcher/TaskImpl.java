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
package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;

/*
 * Change activity:
 *
 * Reason          Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 184390.1.1      26/01/04 schofiel Revised Reliability Qualities of Service - MS - Tests for spill
 * 184390.1.3      27/02/04 schofiel Revised Reliability Qualities of Service - MS - PersistentDispatcher
 * 188052.1        16/03/04 schofiel Remove deprecated persist() method
 * 214274          06/07/04 schofiel Problems in PersistentDispatcherQueueTest
 * 214539          07/07/04 schofiel Problems with PersistentDispatcherTest
 * 253157          03/02/05 schofiel Extra code added to test feature 247513 better
 * 258476          01/03/05 schofiel Implemented getPersistableSizeApproximation() for spill dispatcher test 
 * 327709          14/12/05 gareth   Output NLS messages when OM files are full
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * 538096          25/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import java.util.HashSet;
import java.util.Iterator;

import junit.framework.TestCase;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.task.NullTask;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.ws.sib.utils.ras.SibTr;

public class TaskImpl extends NullTask {
    private static TraceComponent tc = SibTr.register(TaskImpl.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private final PersistableEventDispatchListener _listener;
    private final Persistable _persistable;
    private int _numWriteErrorsBeforeRecovery;
    private int _numPersistenceFullErrorsBeforeRecovery;
    private final HashSet _checkPersist;
    private final HashSet _hasPersisted;
    private boolean _isCreateOfPersistentRepresentation;
    private boolean _isDeleteOfPersistentRepresentation;

    TaskImpl(int id) throws SevereMessageStoreException {
        this(null, new PersistableImpl(id), 0, 0);
    }

    TaskImpl(int id, PersistableEventDispatchListener listener) throws SevereMessageStoreException {
        this(listener, new PersistableImpl(id, listener), 0, 0);
    }

    TaskImpl(int id, PersistableEventDispatchListener listener, int numWriteErrorsBeforeRecovery) throws SevereMessageStoreException {
        this(listener, new PersistableImpl(id, listener), numWriteErrorsBeforeRecovery, 0);
    }

    TaskImpl(PersistableEventDispatchListener listener) throws SevereMessageStoreException {
        this(listener, new PersistableImpl(listener), 0, 0);
    }

    TaskImpl(PersistableEventDispatchListener listener, int numWriteErrorsBeforeRecovery) throws SevereMessageStoreException {
        this(listener, new PersistableImpl(listener), numWriteErrorsBeforeRecovery, 0);
    }

    TaskImpl(PersistableEventDispatchListener listener, Persistable persistable) throws SevereMessageStoreException {
        this(listener, persistable, 0, 0);
    }

    TaskImpl(PersistableEventDispatchListener listener, Persistable persistable, int numWriteErrorsBeforeRecovery) throws SevereMessageStoreException {
        this(listener, persistable, numWriteErrorsBeforeRecovery, 0);
    }

    TaskImpl(PersistableEventDispatchListener listener, Persistable persistable, int numWriteErrorsBeforeRecovery, int numPersistenceFullErrorsBeforeRecovery) throws SevereMessageStoreException {
        super(null);

        if (tc.isEntryEnabled())
            SibTr.entry(tc, "<init>", new Object[] { "Listener=" + listener, "Persistable=" + persistable,
                                                                        "WriteErrors=" + numWriteErrorsBeforeRecovery,
                                                                        "PersistenceFullErors=" + numPersistenceFullErrorsBeforeRecovery });
        _listener = listener;
        _persistable = persistable;
        _numWriteErrorsBeforeRecovery = numWriteErrorsBeforeRecovery;
        _numPersistenceFullErrorsBeforeRecovery = numPersistenceFullErrorsBeforeRecovery;
        _checkPersist = new HashSet();
        _hasPersisted = new HashSet();
        _isCreateOfPersistentRepresentation = false;
        _isDeleteOfPersistentRepresentation = false;

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    @Override
    public synchronized void persist(BatchingContext bc, TransactionState transtate) {
        junit.framework.Assert.assertTrue("persist not permitted in transtate " + transtate, _checkPersist.isEmpty() || _checkPersist.contains(transtate));
        junit.framework.Assert.assertFalse("persist repeated", _hasPersisted.contains(transtate));

        bc.insert(null);

        if (_numWriteErrorsBeforeRecovery > 0) {
            _numWriteErrorsBeforeRecovery--;
            ((BatchingContextImpl) bc).setExecuteFail(true);
        } else if (_numPersistenceFullErrorsBeforeRecovery > 0) {
            _numPersistenceFullErrorsBeforeRecovery--;
            ((BatchingContextImpl) bc).setExecuteFailAsFull(true);
        } else {
            _hasPersisted.add(transtate);
        }
    }

    @Override
    public Persistable getPersistable() {
        return _persistable;
    }

    @Override
    public int getPersistableInMemorySizeApproximation(TransactionState tranState) {
        return _persistable.getInMemoryByteSize();
    }

    public void setCheckPersist(TransactionState transtate) {
        _checkPersist.add(transtate);
    }

    public boolean isPersistOutstanding() {
        Iterator it = _checkPersist.iterator();
        while (it.hasNext()) {
            TransactionState state = (TransactionState) it.next();
            if (!_hasPersisted.contains(state)) {
                TestCase.fail("Expected persistence for transtate " + state + " did not occur");
            }
        }

        return false;
    }

    public TaskImpl setCreateOfPersistentRepresentation() {
        // Can't both be true :-)
        _isCreateOfPersistentRepresentation = true;
        _isDeleteOfPersistentRepresentation = false;
        return this;
    }

    @Override
    public boolean isCreateOfPersistentRepresentation() {
        return _isCreateOfPersistentRepresentation;
    }

    public TaskImpl setDeleteOfPersistentRepresentation() {
        // Can't both be true :-)
        _isCreateOfPersistentRepresentation = false;
        _isDeleteOfPersistentRepresentation = true;
        return this;
    }

    @Override
    public boolean isDeleteOfPersistentRepresentation() {
        return _isDeleteOfPersistentRepresentation;
    }

    @Override
    public Task.Type getTaskType() {
        if (_isCreateOfPersistentRepresentation) {
            return Task.Type.ADD;
        } else if (_isDeleteOfPersistentRepresentation) {
            return Task.Type.REMOVE;
        }

        return Task.Type.UNKNOWN;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("TaskImpl");
        buffer.append("@");
        buffer.append(this.hashCode());

        return buffer.toString();
    }

    public void abort(Transaction transaction) {}

    public void commitExternal(Transaction transaction) {}

    public void commitInternal(Transaction transaction) {}

    public void postAbort(Transaction transaction) {}

    public void postCommit(Transaction transaction) {}

    public void preCommit(Transaction transaction) {}
}
