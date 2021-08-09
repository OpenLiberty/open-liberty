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
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * PK81848.2       08/07/09 gareth   Allow deferred rollback processing
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class PausableWorkItem extends Task {
    private Persistable _persistable;
    private final MessageStoreTestCase _test;
    private final int _pausePoint;
    private final Object _lock;

    public static final int PAUSE_IN_PRE_COMMIT = 0;
    public static final int PAUSE_IN_COMMIT = 1;
    public static final int PAUSE_IN_POST_COMMIT = 2;
    public static final int PAUSE_IN_ROLLBACK = 3;
    public static final int PAUSE_IN_POST_ROLLBACK = 4;

    public PausableWorkItem(MessageStoreTestCase test, int pausePoint, Object lock) throws SevereMessageStoreException {
        super(null);

        _test = test;
        _pausePoint = pausePoint;
        _lock = lock;
    }

    @Override
    public void preCommit(final PersistentTransaction transaction) {
        if (_pausePoint == PAUSE_IN_PRE_COMMIT) {
            _test.print("| - WorkItem pausing in PreCommit");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - WorkItem resumed in PreCommit");
        } else {
            _test.print("| - WorkItem called to PreCommit");
        }
    }

    @Override
    public void commitInternal(final PersistentTransaction transaction) {
        if (_pausePoint == PAUSE_IN_COMMIT) {
            _test.print("| - WorkItem pausing in Commit");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - WorkItem resumed in Commit");
        } else {
            _test.print("| - WorkItem called to Commit");
        }
    }

    @Override
    public void commitExternal(final PersistentTransaction transaction) {}

    @Override
    public void postCommit(final PersistentTransaction transaction) {
        if (_pausePoint == PAUSE_IN_POST_COMMIT) {
            _test.print("| - WorkItem pausing in postCommit");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - WorkItem resumed in postCommit");
        } else {
            _test.print("| - WorkItem called to postCommit");
        }
    }

    @Override
    public void abort(final PersistentTransaction transaction) {
        if (_pausePoint == PAUSE_IN_ROLLBACK) {
            _test.print("| - WorkItem pausing in rollback");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - WorkItem resumed in rollback");
        } else {
            _test.print("| - WorkItem called to rollback");
        }
    }

    @Override
    public void postAbort(final PersistentTransaction transaction) {
        if (_pausePoint == PAUSE_IN_POST_ROLLBACK) {
            _test.print("| - WorkItem pausing in postRollback");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - WorkItem resumed in postRollback");
        } else {
            _test.print("| - WorkItem called to postRollback");
        }
    }

    public void persist(BatchingContext bc, TransactionState tranState) {}

    public int getPersistableInMemorySizeApproximation(TransactionState tranState) {
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
