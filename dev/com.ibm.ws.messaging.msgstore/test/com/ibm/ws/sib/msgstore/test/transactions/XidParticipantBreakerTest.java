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
 * Reason          Date    Origin   Description
 * ------------- -------- -------- --------------------------------------------
 * 184788        10/12/03  gareth   Implement transaction state model
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 316887        03/02/06  gareth   Modify exception handling 
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * PK81848.1     18/06/09  pbroad   Alter unit tests that checked we'd left txns hanging in committing (rather than cleaning up)
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.ws.sib.msgstore.transactions.impl.XidParticipant;

public class XidParticipantBreakerTest extends MessageStoreTestCase {

    public XidParticipantBreakerTest(String name) {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite() {
        return new TestSuite(XidParticipantBreakerTest.class);
    }

    public void testXidParticipantBreaker() {
        print("************** XidParticipantBreaker *****************");
        print("*                                                    *");

        XidParticipant tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_PRE_COMMIT));

            // Succeeded as expected.
            print("* Register BrokenWorkList(PRE_COMMIT)      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register BrokenWorkList(PRE_COMMIT)      - FAILED  *");
            fail("Register of BrokenWorkList(PRE_COMMIT) failed!");
        }

        // Attempt to commit with 1PC, this should Succeed.
        try {
            print("* Commit Transaction with 1PC:                       *");

            tran.commit(true);

            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Commit of transaction failed to throw exception!");
        } catch (RollbackException rbe) {
            // failed as expected
            print("* Commit Transaction with 1PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction                       - FAILED  *");
            fail("Commit of LocalTransaction failed!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        TransactionState state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLEDBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_PRE_COMMIT) failed!");
        }

        tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a BrokenWorkList, this should SUCCEED.
        try {
            tran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_PRE_COMMIT));

            // Succeeded as expected.
            print("* Register BrokenWorkList(PRE_COMMIT)      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register BrokenWorkList(PRE_COMMIT)      - FAILED  *");
            fail("Register of BrokenWorkList(PRE_COMMIT) failed!");
        }

        // Attempt to prepare for 2PC, this should FAIL.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Prepare of XidParticipant failed to throw exception!");
        } catch (RollbackException rbe) {
            // failed as expected.
            print("* Prepare Transaction                      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Unexpected exception thrown during Prepare of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLEDBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_PRE_COMMIT) failed!");
        }

        tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_COMMIT));

            // Succeeded as expected.
            print("* Register BrokenWorkList(COMMIT)          - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register BrokenWorkList(COMMIT)          - FAILED  *");
            fail("Register of BrokenWorkList(COMMIT) failed!");
        }

        // Attempt to commit with 1PC, this should Succeed.
        try {
            print("* Commit Transaction with 1PC:                       *");

            tran.commit(true);

            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Commit of transaction failed to throw exception!");
        } catch (TransactionException te) {
            // failed as expected
            print("* Commit Transaction with 1PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction                       - FAILED  *");
            fail("Commit of LocalTransaction failed!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        // PK81848.1 We shouldn't leave the transaction stuck in STATE_COMMITTING_1PC as previously checked
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLEDBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLEDBACK- SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLEDBACK- FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_COMMIT) failed!");
        }

        tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a BrokenWorkList, this should SUCCEED.
        try {
            tran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_COMMIT));

            // Succeeded as expected.
            print("* Register BrokenWorkList(COMMIT)          - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register BrokenWorkList(COMMIT)          - FAILED  *");
            fail("Register of BrokenWorkList(COMMIT) failed!");
        }

        // Attempt to prepare for 2PC, this should SUCCEED.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            print("* Prepare Transaction                      - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Unexpected exception thrown during Prepare of XidParticipant!");
        }

        // Attempt to commit for 2PC, this should FAIL.
        try {
            print("* Commit Transaction with 2PC:                       *");

            tran.commit(false);

            // If we get this far then we're broken.
            print("* Commit Transaction with 2PC              - FAILED  *");
            fail("Prepare of XidParticipant failed to throw exception!");
        } catch (TransactionException te) {
            // failed as expected.
            print("* Commit Transaction with 2PC              - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Commit Transaction with 2PC              - FAILED  *");
            fail("Unexpected exception thrown during Commit 2PC of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_PREPARED (so the txn manager
        // can retry the commit)
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_PREPARED) {
            // Succeeded as expected.
            print("* Transaction State is STATE_PREPARED - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_PREPARED - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_PRE_COMMIT) failed!");
        }

        tran = new XidParticipant(null, null, new BrokenPersistenceManager(this, BrokenPersistenceManager.BREAK_AT_PREPARE), 0);
        tran.registerCallback(new NullTransactionCallback(this));
        print("* Register Broken PM (BREAK_AT_PREPARE)    - SUCCESS *");

        // Attempt to register a BrokenWorkList, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Register WorkItem                        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register WorkItem                        - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to prepare for 2PC, this should FAIL.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Prepare of XidParticipant failed to throw exception!");
        } catch (RollbackException rbe) {
            // failed as expected.
            print("* Prepare Transaction                      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Unexpected exception thrown during Prepare of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLEDBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_PREPARE) failed!");
        }

        tran = new XidParticipant(null, null, new BrokenPersistenceManager(this, BrokenPersistenceManager.SEVERE_AT_PREPARE), 0);
        tran.registerCallback(new NullTransactionCallback(this));
        print("* Register Broken PM (SEVERE_AT_PREPARE)   - SUCCESS *");

        // Attempt to register a BrokenWorkList, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Register WorkItem                        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register WorkItem                        - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to prepare for 2PC, this should FAIL.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Prepare of XidParticipant failed to throw exception!");
        } catch (SeverePersistenceException spe) {
            // failed as expected.
            print("* Prepare Transaction                      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Unexpected exception thrown during Prepare of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_PREPARING) {
            // Succeeded as expected.
            print("* Transaction State is STATE_PREPARING     - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_PREPARING     - FAILED  *");
            fail("getTransactionState(AFTER_SEVERE_AT_PREPARE) failed!");
        }

        tran = new XidParticipant(null, null, new BrokenPersistenceManager(this, BrokenPersistenceManager.BREAK_AT_COMMIT), 0);
        tran.registerCallback(new NullTransactionCallback(this));
        print("* Register Broken PM (BREAK_AT_COMMIT_1PC) - SUCCESS *");

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Register WorkItem                        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register WorkItem                        - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to commit wih 1PC, this should FAIL.
        try {
            print("* Commit Transaction with 1PC:                       *");

            tran.commit(true);

            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Commit of XidParticipant failed to throw exception!");
        } catch (RollbackException rbe) {
            // Failed as expected.
            print("* Commit Transaction with 1PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Unexpected exception thrown during Commit of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_ROLLEDBACK
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLEDBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLEDBACK    - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLEDBACK    - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_COMMIT_1PC) failed!");
        }

        tran = new XidParticipant(null, null, new BrokenPersistenceManager(this, BrokenPersistenceManager.BREAK_AT_COMMIT), 0);
        tran.registerCallback(new NullTransactionCallback(this));
        print("* Register Broken PM (BREAK_AT_COMMIT_2PC) - SUCCESS *");

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Register WorkItem                        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register WorkItem                        - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to prepare for 2PC, this should SUCCEED.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            // succeeded as expected.
            print("* Prepare Transaction                      - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Unexpected exception thrown during Prepare of XidParticipant!");
        }

        // Attempt to commit wih 2PC, this should FAIL.
        try {
            print("* Commit Transaction with 2PC:                       *");

            tran.commit(false);

            // If we get this far then we're broken.
            print("* Commit Transaction with 2PC              - FAILED  *");
            fail("Commit of XidParticipant failed to throw exception!");
        } catch (PersistenceException pe) {
            // Failed as expected.
            print("* Commit Transaction with 2PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction with 2PC              - FAILED  *");
            e.printStackTrace(System.out);
            fail("Unexpected exception thrown during Commit of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_PREPARED
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_PREPARED) {
            // Succeeded as expected.
            print("* Transaction State is STATE_PREPARED      - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_PREPARED      - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_COMMIT_2PC) failed!");
        }

        tran = new XidParticipant(null, null, new BrokenPersistenceManager(this, BrokenPersistenceManager.SEVERE_AT_COMMIT), 0);
        tran.registerCallback(new NullTransactionCallback(this));
        print("* Register Broken PM (SEVERE_AT_COMMIT)    - SUCCESS *");

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Register WorkItem                        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register WorkItem                        - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to commit wih 1PC, this should FAIL.
        try {
            print("* Commit Transaction with 1PC:                       *");

            tran.commit(true);

            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Commit of XidParticipant failed to throw exception!");
        } catch (SeverePersistenceException spe) {
            // Failed as expected.
            print("* Commit Transaction with 1PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction with 1PC              - FAILED  *");
            fail("Unexpected exception thrown during Commit of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_COMMITTING_1PC
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_COMMITTING_1PC) {
            // Succeeded as expected.
            print("* Transaction State is STATE_COMMITTING_1PC- SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_COMMITTING_1PC- FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_COMMIT_1PC) failed!");
        }

        tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a WorkItem, this should SUCCEED.
        try {
            tran.addWork(new BrokenWorkItem(this, BrokenWorkItem.BREAK_AT_ROLLBACK));

            // Succeeded as expected.
            print("* Register BrokenWorkList(ROLLBACK)        - SUCCESS *");
        } catch (MessageStoreException e) {
            // If we get this far then we're broken.
            print("* Register BrokenWorkList(ROLLBACK)        - FAILED  *");
            fail("Register of BrokenWorkList(ROLLBACK) failed!");
        }

        // Attempt to rollback, this should Succeed.
        try {
            print("* Rollback Transaction:                              *");

            tran.rollback();

            // If we get this far then we're broken.
            print("* Rollback Transaction                     - FAILED  *");
            fail("Rollback of XidParticipant failed to throw exception!");
        } catch (TransactionException te) {
            // Failed as expected.
            print("* Rollback Transaction                     - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Rollback Transaction                     - FAILED  *");
            fail("Unexpected exception thrown during Rollback of XidParticipant!");
        }

        // Get the transaction state, this should be STATE_ROLLINGBACK
        state = ((PersistentTransaction) tran).getTransactionState();

        if (state == TransactionState.STATE_ROLLINGBACK) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ROLLINGBACK   - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ROLLINGBACK   - FAILED  *");
            fail("getTransactionState(AFTER_BREAK_AT_ROLLBACK) failed!");
        }

        print("*                                                    *");
        print("************** XidParticipantBreaker *****************");
    }
}
