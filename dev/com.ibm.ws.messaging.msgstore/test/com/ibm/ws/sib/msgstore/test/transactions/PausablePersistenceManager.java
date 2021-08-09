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

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistenceManager;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

public class PausablePersistenceManager implements PersistenceManager {
    private boolean _supports1PCOptimization = false;

    private final MessageStoreTestCase _test;
    private final int _pausePoint;
    private final Object _lock;

    public static final int PAUSE_IN_BEFORE_COMPLETION = 0;
    public static final int PAUSE_IN_PREPARE = 1;
    public static final int PAUSE_IN_COMMIT = 2;
    public static final int PAUSE_IN_ROLLBACK = 3;
    public static final int PAUSE_IN_AFTER_COMPLETION = 4;

    public PausablePersistenceManager(MessageStoreTestCase test, Object lock, int pausePoint) {
        _test = test;
        _lock = lock;
        _pausePoint = pausePoint;
    }

    public PausablePersistenceManager(MessageStoreTestCase test, Object lock, int pausePoint, boolean supports1PCOptimization) {
        _test = test;
        _lock = lock;
        _pausePoint = pausePoint;
        _supports1PCOptimization = supports1PCOptimization;
    }

    public void beforeCompletion(PersistentTransaction transaction) throws PersistenceException {
        if (_pausePoint == PAUSE_IN_BEFORE_COMPLETION) {
            _test.print("| - PM pausing in beforeCompletion");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - PM resumed in beforeCompletion");
        } else {
            _test.print("| - PM called with beforeCompletion");
        }
    }

    public void prepare(PersistentTransaction transaction) throws PersistenceException {
        if (_pausePoint == PAUSE_IN_PREPARE) {
            _test.print("| - PM pausing in prepare");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - PM resumed in prepare");
        } else {
            _test.print("| - PM called with prepare");
        }
    }

    public void commit(PersistentTransaction transaction, boolean onePhase) throws PersistenceException {
        if (_pausePoint == PAUSE_IN_COMMIT) {
            _test.print("| - PM pausing in commit");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - PM resumed in commit");
        } else {
            _test.print("| - PM called with commit");
        }
    }

    public void rollback(PersistentTransaction transaction) throws PersistenceException {
        if (_pausePoint == PAUSE_IN_ROLLBACK) {
            _test.print("| - PM pausing in rollback");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - PM resumed in rollback");
        } else {
            _test.print("| - PM called with rollback");
        }
    }

    public void afterCompletion(PersistentTransaction transaction, boolean committed) {
        if (_pausePoint == PAUSE_IN_AFTER_COMPLETION) {
            _test.print("| - PM pausing in afterCompletion");

            synchronized (_lock) {
                try {
                    _lock.wait();
                } catch (InterruptedException ie) {
                }
            }

            _test.print("| - PM resumed in afterCompletion");
        } else {
            _test.print("| - PM called with afterCompletion");
        }
    }

    public boolean supports1PCOptimisation() {
        _test.print("| - PM called with supports1PCOptimisation");
        return _supports1PCOptimization;
    }
}
