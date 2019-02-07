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
 * ------------- -------- -------- ---------------------------------------
 * 168081        03/09/03  gareth   GlobalTransaction Support (Local Clients)
 * 181930        17/11/03  gareth   XA Recovery Support
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.MSDelegatingXAResource;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

public class MSDelegatingXAResourceBlackBoxTest extends MessageStoreTestCase {
    public MSDelegatingXAResourceBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        MSDelegatingXAResourceBlackBoxTest test = new MSDelegatingXAResourceBlackBoxTest("testMSDelegatingXAResourceBlackBox");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testMSDelegatingXAResourceBlackBox() {
        print("*********** MSDelegatingXAResourceBlackBox ***********");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStoreImpl MS = null;
        MSDelegatingXAResource xaRes = null;
        NullXid xid1 = new NullXid("XID1");

        // Try to create an ItemStream, this should SUCCEED
        try {
            print("* Create MessageStore:                               *");

            MS = (MessageStoreImpl) createAndStartMessageStore(true, PERSISTENCE);

            print("* Create MessageStore                      - SUCCESS *");
        } catch (Exception e) {
            print("* Create MessageStore                      - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during MessageStore create: " + e.getMessage());
        }

        // Try to create an XAResource, this should SUCCEED
        print("* Create MSDelegatingXAResource:                     *");

        xaRes = new MSDelegatingXAResource(MS, new NullPersistenceManager(this), 0);

        if (xaRes != null) {
            StringBuffer out2 = new StringBuffer(xaRes.toString());
            int length2 = out2.length();

            print("* - " + out2.substring(length2 - 48) + " *");

            print("* Create MSDelegatingXAResource            - SUCCESS *");
        } else {
            print("* Create MSDelegatingXAResource            - FAILED  *");
            fail("XAResource create failed!");
        }

        // Commit the XAResource, this should FAIL
        try {
            xaRes.commit(xid1, false);

            print("* Exception handling on Commit             - FAILED  *");
            fail("Commit succeeded before end or prepare were called!");
        } catch (XAException xae) {
            print("* Exception handling on Commit             - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on Commit             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during XAResource commit: " + e.getMessage());
        }

        // Rollback the XAResource, this should FAIL
        try {
            xaRes.rollback(xid1);

            print("* Exception handling on Rollback           - FAILED  *");
            fail("Rollback succeeded before end was called!");
        } catch (XAException xae) {
            print("* Exception handling on Rollback           - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on Rollback           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during XAResource Rollback: " + e.getMessage());
        }

        // Get the transaction state, this should be STATE_NONE
        TransactionState state = ((PersistentTransaction) xaRes).getTransactionState();

        if (state == TransactionState.STATE_NONE) {
            // Succeeded as expected.
            print("* Transaction State is STATE_NONE          - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_NONE          - FAILED  *");
            fail("getTransactionState(BEFORE_START) failed!");
        }

        // Associate the XAResource, this should SUCCEED
        try {
            xaRes.start(xid1, XAResource.TMNOFLAGS);
            xaRes.addWork(new NullWorkItem(this));
            xaRes.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource                     - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during association of XAResource: " + e.getMessage());
        }

        // Get the transaction state, this should be STATE_ACTIVE
        state = ((PersistentTransaction) xaRes).getTransactionState();

        if (state == TransactionState.STATE_ACTIVE) {
            // Succeeded as expected.
            print("* Transaction State is STATE_ACTIVE        - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_ACTIVE        - FAILED  *");
            fail("getTransactionState(STARTED) failed!");
        }

        // Prepare the XAResource, this should FAIL
        try {
            xaRes.prepare(xid1);

            print("* Exception handling on Prepare            - FAILED  *");
            fail("Commit succeeded before end or prepare were called!");
        } catch (XAException xae) {
            print("* Exception handling on Prepare            - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on Prepare            - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during XAResource prepare: " + e.getMessage());
        }

        // Commit the XAResource, this should FAIL
        try {
            xaRes.commit(xid1, false);

            print("* Exception handling on Commit             - FAILED  *");
            fail("Commit succeeded before end or prepare were called!");
        } catch (XAException xae) {
            print("* Exception handling on Commit             - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on Commit             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during XAResource commit: " + e.getMessage());
        }

        // Rollback the XAResource, this should FAIL
        try {
            xaRes.rollback(xid1);

            print("* Exception handling on Rollback           - FAILED  *");
            fail("Rollback succeeded before end was called!");
        } catch (XAException xae) {
            print("* Exception handling on Rollback           - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on Rollback           - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during XAResource Rollback: " + e.getMessage());
        }

        // Are we enlisted, this should return TRUE
        if (xaRes.isEnlisted()) {
            print("* Is XAResource Enlisted (True)            - SUCCESS *");
        } else {
            print("* Is XAResource Enlisted (True)            - FAILED  *");
            fail("XAResource.isEnlisted() returned FALSE.");
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        StringBuffer line1 = new StringBuffer("* - Id :                                             *");
        String idStr1 = xaRes.getPersistentTranId().toString();
        line1.replace(9, 9 + idStr1.length(), idStr1);

        print(line1.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        // Disassociate the XAResource, this should SUCCEED
        try {
            xaRes.end(xid1, XAResource.TMSUCCESS);

            print("* Disassociate XAResource                  - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Get the transaction state, this should be STATE_ACTIVE
        state = ((PersistentTransaction) xaRes).getTransactionState();

        if (state == TransactionState.STATE_NONE) {
            // Succeeded as expected.
            print("* Transaction State is STATE_NONE          - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction State is STATE_NONE          - FAILED  *");
            fail("getTransactionState(ENDED) failed!");
        }

        // Commit the XAResource, this should SUCCEED
        try {
            print("* Commit XAResource (1PC):                           *");

            xaRes.commit(xid1, true);

            print("* Commit XAResource (1PC)                  - SUCCESS *");
        } catch (Exception e) {
            print("* Commit XAResource (1PC)                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit (1PC): " + e.getMessage());
        }

        // Associate the XAResource, this should SUCCEED
        try {
            xaRes.start(xid1, XAResource.TMNOFLAGS);
            xaRes.addWork(new NullWorkItem(this));
            xaRes.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource                     - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during association of XAResource: " + e.getMessage());
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        line1 = new StringBuffer("* - Id :                                             *");
        idStr1 = xaRes.getPersistentTranId().toString();
        line1.replace(9, 9 + idStr1.length(), idStr1);

        print(line1.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        // Disassociate the XAResource, this should SUCCEED
        try {
            xaRes.end(xid1, XAResource.TMSUCCESS);

            print("* Disassociate XAResource                  - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Prepare the XAResource, this should SUCCEED
        try {
            print("* Prepare XAResource:                                *");

            xaRes.prepare(xid1);

            print("* Prepare XAResource                       - SUCCESS *");
        } catch (Exception e) {
            print("* Prepare XAResource                       - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Commit the XAResource, this should SUCCEED
        try {
            print("* Commit XAResource (2PC):                           *");

            xaRes.commit(xid1, false);

            print("* Commit XAResource (2PC)                  - SUCCESS *");
        } catch (Exception e) {
            print("* Commit XAResource (2PC)                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource commit (2PC): " + e.getMessage());
        }

        // Associate the XAResource, this should SUCCEED
        try {
            xaRes.start(xid1, XAResource.TMNOFLAGS);
            xaRes.addWork(new NullWorkItem(this));
            xaRes.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource                     - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during association of XAResource: " + e.getMessage());
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        line1 = new StringBuffer("* - Id :                                             *");
        idStr1 = xaRes.getPersistentTranId().toString();
        line1.replace(9, 9 + idStr1.length(), idStr1);

        print(line1.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        // Disassociate the XAResource, this should SUCCEED
        try {
            xaRes.end(xid1, XAResource.TMSUCCESS);

            print("* Disassociate XAResource                  - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Rollback the XAResource, this should SUCCEED
        try {
            print("* Rollback XAResource:                               *");

            xaRes.rollback(xid1);

            print("* Rollback XAResource                      - SUCCESS *");
        } catch (Exception e) {
            print("* Rollback XAResource                      - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource rollback: " + e.getMessage());
        }

        // Associate the XAResource, this should SUCCEED
        try {
            xaRes.start(xid1, XAResource.TMNOFLAGS);
            xaRes.addWork(new NullWorkItem(this));
            xaRes.registerCallback(new NullTransactionCallback(this));

            print("* Associate XAResource                     - SUCCESS *");
        } catch (Exception e) {
            print("* Associate XAResource                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during association of XAResource: " + e.getMessage());
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        line1 = new StringBuffer("* - Id :                                             *");
        idStr1 = xaRes.getPersistentTranId().toString();
        line1.replace(9, 9 + idStr1.length(), idStr1);

        print(line1.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        // Disassociate the XAResource, this should SUCCEED
        try {
            xaRes.end(xid1, XAResource.TMSUCCESS);

            print("* Disassociate XAResource                  - SUCCESS *");
        } catch (Exception e) {
            print("* Disassociate XAResource                  - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Prepare the XAResource, this should SUCCEED
        try {
            print("* Prepare XAResource:                                *");

            xaRes.prepare(xid1);

            print("* Prepare XAResource                       - SUCCESS *");
        } catch (Exception e) {
            print("* Prepare XAResource                       - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during disassociation of XAResource: " + e.getMessage());
        }

        // Rollback the XAResource, this should SUCCEED
        try {
            print("* Rollback XAResource:                               *");

            xaRes.rollback(xid1);

            print("* Rollback XAResource                      - SUCCESS *");
        } catch (Exception e) {
            print("* Rollback XAResource                      - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XAResource rollback: " + e.getMessage());
        }

        // Get the transaction type, this should be TX_GLOBAL
        int type = xaRes.getTransactionType();

        if (type == PersistentTransaction.TX_GLOBAL) {
            // Succeeded as expected.
            print("* Transaction Type is TX_GLOBAL            - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction Type is TX_GLOBAL            - FAILED  *");
            fail("getTransactionType failed!");
        }

        // Are we enlisted, this should return FALSE
        if (xaRes.isEnlisted()) {
            print("* Is XAResource Enlisted (False)           - FAILED  *");
            fail("XAResource.isEnlisted() returned TRUE.");
        } else {
            print("* Is XAResource Enlisted (False)           - SUCCESS *");
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("*********** MSDelegatingXAResourceBlackBox ***********");
    }
}
