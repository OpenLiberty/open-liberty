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
 * 184788          09/12/03 gareth   Add transaction state model
 * 188054.1        16/03/04 gareth   Enhanced JDBC Exception handling
 * SIB0003.ms.13   26/08/05 schofiel 1PC optimisation only works for data store not file store    
 * 341158          13/03/06 gareth   Make better use of LoggingTestCase       
 * 398385          24/10/06 gareth   Improve handling of 2PC commit retries
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistenceManager;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;

public class BrokenPersistenceManager implements PersistenceManager {
    public static final int NO_BREAK = 0;

    public static final int BREAK_AT_BEFORE_COMPLETION = 1;
    public static final int BREAK_AT_PREPARE = 2;
    public static final int BREAK_AT_COMMIT = 3;
    public static final int BREAK_AT_ROLLBACK = 4;

    public static final int SEVERE_AT_BEFORE_COMPLETION = 6;
    public static final int SEVERE_AT_PREPARE = 7;
    public static final int SEVERE_AT_COMMIT = 8;
    public static final int SEVERE_AT_ROLLBACK = 9;

    private int _breakPoint;
    private final MessageStoreTestCase _test;

    public BrokenPersistenceManager(MessageStoreTestCase test, int breakPoint) {
        _test = test;
        _breakPoint = breakPoint;
    }

    public void setBreakPoint(int breakPoint) {
        _breakPoint = breakPoint;
    }

    public void beforeCompletion(PersistentTransaction transaction) throws PersistenceException {
        switch (_breakPoint) {
            case BREAK_AT_BEFORE_COMPLETION:
                _test.print("* - Exception thrown in PM.beforeCompletion- SUCCESS *");
                throw new PersistenceException();

            case SEVERE_AT_BEFORE_COMPLETION:
                _test.print("* - Exception thrown in PM.beforeCompletion- SUCCESS *");
                throw new SeverePersistenceException();

            default:
                _test.print("* - PM called with beforeCompletion        - SUCCESS *");
        }
    }

    public void prepare(PersistentTransaction transaction) throws PersistenceException {
        switch (_breakPoint) {
            case BREAK_AT_PREPARE:
                _test.print("* - Exception thrown in PM.prepare         - SUCCESS *");
                throw new PersistenceException();

            case SEVERE_AT_PREPARE:
                _test.print("* - Exception thrown in PM.prepare         - SUCCESS *");
                throw new SeverePersistenceException();

            default:
                _test.print("* - PM called with prepare                 - SUCCESS *");
        }
    }

    public void commit(PersistentTransaction transaction, boolean onePhase) throws PersistenceException {
        switch (_breakPoint) {
            case BREAK_AT_COMMIT:
                _test.print("* - Exception thrown in PM.commit          - SUCCESS *");
                throw new PersistenceException();

            case SEVERE_AT_COMMIT:
                _test.print("* - Exception thrown in PM.commit          - SUCCESS *");
                throw new SeverePersistenceException();

            default:
                _test.print("* - PM called with commit                  - SUCCESS *");
        }
    }

    public void rollback(PersistentTransaction transaction) throws PersistenceException {
        switch (_breakPoint) {
            case BREAK_AT_ROLLBACK:
                _test.print("* - Exception thrown in PM.rollback        - SUCCESS *");
                throw new PersistenceException();

            case SEVERE_AT_ROLLBACK:
                _test.print("* - Exception thrown in PM.rollback        - SUCCESS *");
                throw new SeverePersistenceException();

            default:
                _test.print("* - PM called with rollback                - SUCCESS *");
        }
    }

    public void afterCompletion(PersistentTransaction transaction, boolean committed) {
        _test.print("* - PM called with afterCompletion         - SUCCESS *");
    }

    public boolean supports1PCOptimisation() {
        _test.print("* - PM called with supports1PCOptimisation - SUCCESS *");
        return true;
    }
}
