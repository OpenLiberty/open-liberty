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
 * 186657.1        24/05/04 gareth   Per-work-item error checking.
 * 206970          03/06/04 schofiel Modified definition of Persistable interface
 * 214205          07/07/04 schofiel Clean up size calculations for tasks
 * 223636.2        26/08/04 corrigk  Consolidate dump
 * 247513          14/01/05 schofiel Improve spilling performance
 * 321392          05/12/05 schofiel Complete removal of tick value
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase
 * SIB0112b.ms.1   07/08/06 gareth   Large message support.
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * 538096          25/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class BrokenWorkItem extends Task {
    private Persistable _persistable;

    public static final int BREAK_AT_PRE_COMMIT = 1;
    public static final int BREAK_AT_COMMIT = 2;
    public static final int BREAK_AT_ROLLBACK = 3;

    private final int _breakPoint;
    private final MessageStoreTestCase _test;

    public BrokenWorkItem(MessageStoreTestCase test, int breakPoint) throws SevereMessageStoreException {
        super(null);

        _test = test;
        _breakPoint = breakPoint;
    }

    @Override
    public void preCommit(final PersistentTransaction transaction) {
        if (_breakPoint == BREAK_AT_PRE_COMMIT) {
            _test.print("* - Exception thrown in WorkItem.preCommit - SUCCESS *");
            throw new MessageStoreRuntimeException();
        } else {
            _test.print("* - WorkItem called to PreCommit           - SUCCESS *");
        }
    }

    @Override
    public void commitInternal(final PersistentTransaction transaction) {
        if (_breakPoint == BREAK_AT_COMMIT) {
            _test.print("* - Exception thrown in WorkItem.Commit    - SUCCESS *");
            throw new MessageStoreRuntimeException();
        } else {
            _test.print("* - WorkItem called to commitInternal      - SUCCESS *");
        }
    }

    @Override
    public void commitExternal(final PersistentTransaction transaction) {
        _test.print("* - WorkItem called to commitExternal      - SUCCESS *");
    }

    @Override
    public void postCommit(final PersistentTransaction transaction) {
        _test.print("* - WorkItem called to postCommit          - SUCCESS *");
    }

    @Override
    public void abort(final PersistentTransaction transaction) {
        if (_breakPoint == BREAK_AT_ROLLBACK) {
            _test.print("* - Exception thrown in WorkItem.Abort     - SUCCESS *");
            throw new MessageStoreRuntimeException();
        } else {
            _test.print("* - WorkItem called to abort               - SUCCESS *");
        }
    }

    @Override
    public void postAbort(final PersistentTransaction transaction) {
        _test.print("* - WorkItem called to postAbort           - SUCCESS *");
    }

    public void persist(BatchingContext bc, TransactionState tranState) {}

    public int getPersistableInMemorySizeApproximation(TransactionState transtate) {
        return 0;
    }

    @Override
    public Persistable getPersistable() {
        if (_persistable == null) {
            _persistable = new NullPersistable();
        }
        return _persistable;
    }
}
