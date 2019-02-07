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
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase
 * 515543.2        08/07/08 gareth   Change runtime exceptions to caught exception
 * 538096          25/07/08 susana   Use getInMemorySize for spilling & persistence
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSAutoCommitTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class AddDuringPreCommitWorkItem extends Task {
    private final MessageStoreTestCase _test;
    private Persistable _persistable;

    public AddDuringPreCommitWorkItem(MessageStoreTestCase test) throws SevereMessageStoreException {
        super(null);

        _test = test;
    }

    @Override
    public void preCommit(final PersistentTransaction transaction) {
        _test.print("* - WorkItem called to PreCommit:                    *");

        try {
            (transaction).addWork(new NullWorkItem(_test));

            // If we are using an AutoCommitTransaction
            // then adding more work at this point shouldn't
            // have succeeded.
            if (transaction instanceof MSAutoCommitTransaction) {
                _test.print("*   - Item added to AutoCommitTransaction! - FAILED  *");
                _test.fail("Add of further work was allowed by AutoCommitTransaction!");
            } else {
                _test.print("*   - Item added to Transaction            - SUCCESS *");
            }
        } catch (Exception e) {
            // If we are using an AutoCommitTransaction
            // then we should expect to fail.
            if (transaction instanceof MSAutoCommitTransaction) {
                _test.print("*   - Item add failed on AutoCommitTran    - SUCCESS *");
            } else {
                _test.print("*   - Item added to Transaction            - FAILED  *");
                e.printStackTrace(System.err);
                _test.fail("Add of further work failed! " + e.getMessage());
            }
        }

        _test.print("* - WorkItem called to PreCommit           - SUCCESS *");
    }

    @Override
    public void commitInternal(final PersistentTransaction transaction) {
        _test.print("* - WorkItem called to commitInternal      - SUCCESS *");
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
        _test.print("* - WorkItem called to abort               - SUCCESS *");
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
