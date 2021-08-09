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
 * Reason        Date     Origin    Description
 * ------------- -------- --------- -------------------------------------------
 * 168081        01/09/03  gareth   GlobalTransaction Support (Local Clients)
 * 182347        11/11/03  gareth   Add Persistent Transaction ID
 * 181930        17/11/03  gareth   XA Recovery Support
 * 186657.1      24/05/04  gareth   Per-work-item error checking.
 * 186657.4      11/06/04  gareth   Add isAlive() method
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.XidStillAssociatedException;
import com.ibm.ws.sib.msgstore.XidUnknownException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionParticipant;
import com.ibm.ws.sib.msgstore.transactions.impl.XidManager;
import com.ibm.ws.sib.transactions.PersistentTranId;

public class XidManagerBlackBoxTest extends MessageStoreTestCase {
    public XidManagerBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        XidManagerBlackBoxTest test = new XidManagerBlackBoxTest("testXidManagerBlackBox");
        test.setPersistence(persistence);

        suite.addTest(test);

        return suite;
    }

    public void testXidManagerBlackBox() {
        print("***************** XidManagerBlackBox *****************");
        print("*                                                    *");

        if (PERSISTENCE != null) {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStoreImpl MS = null;
        XidManager _manager = null;

        // Create XID's to use in following tests
        Xid xid1 = new NullXid("One");
        Xid xid2 = new NullXid("Two");

        try {
            print("* Create XidManager:                                 *");

            MS = (MessageStoreImpl) createAndStartMessageStore(true, PERSISTENCE);
            _manager = MS.getXidManager();

            print("* Create XidManager                        - SUCCESS *");
        } catch (Exception e) {
            print("* Create XidManager                        - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XidManager create: " + e.getMessage());
        }

        // Start two threads off, this should SUCCEED
        try {
            print("* Start two worker threads:                          *");

            XidThread thread1 = new XidThread(this, xid1, _manager);
            thread1.setName("One");
            thread1.start();
            print("* - Start worker thread 1                  - SUCCESS *");

            XidThread thread2 = new XidThread(this, xid2, _manager);
            thread2.setName("Two");
            thread2.start();
            print("* - Start worker thread 2                  - SUCCESS *");

            thread1.join();
            thread2.join();
            print("* Start two worker threads                 - SUCCESS *");
        } catch (Exception e) {
            print("* Start two worker threads                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during thread creation!");
        }

        // Prepare xid1, this should SUCCEED
        try {
            print("* Prepare of XID1:                                   *");

            _manager.prepare(new PersistentTranId(xid1));

            print("* Prepare of XID1                          - SUCCESS *");
        } catch (Exception e) {
            print("* Prepare of XID1                          - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during prepare of XID1!");
        }

        // output the contents of the XidManager in XML
        try {
            print("* Output XidManager in XML:                          *");

            print(_manager.toXmlString());

            print("* Output XidManager in XML                 - SUCCESS *");
        } catch (Exception e) {
            print("* Output XidManager in XML                 - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during toXmlString()!");
        }

        // Call isTranIdKnown(), this should return TRUE
        if (_manager.isTranIdKnown(new PersistentTranId(xid1))) {
            print("* Call isTranIdKnown(XID1), expect TRUE    - SUCCESS *");
        } else {
            print("* Call isTranIdKnown(XID1), expect TRUE    - FAILED  *");
            fail("TranId is not known before tran is complete!");
        }

        // Commit xid1, this should SUCCEED
        try {
            print("* Commit of XID1 (2PC):                              *");

            _manager.commit(new PersistentTranId(xid1), false);

            print("* Commit of XID1 (2PC)                     - SUCCESS *");
        } catch (Exception e) {
            print("* Commit of XID1 (2PC)                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during commit of XID1!");
        }

        // Commit xid2, this should SUCCEED
        try {
            print("* Commit of XID2 (1PC):                              *");

            _manager.commit(new PersistentTranId(xid2), true);

            print("* Commit of XID2 (1PC)                     - SUCCESS *");
        } catch (Exception e) {
            print("* Commit of XID2 (1PC)                     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during commit of XID2!");
        }

        // Call isTranIdKnown(), this should return FALSE
        if (!_manager.isTranIdKnown(new PersistentTranId(xid2))) {
            print("* Call isTranIdKnown(XID2), expect FALSE   - SUCCESS *");
        } else {
            print("* Call isTranIdKnown(XID2), expect FALSE   - FAILED  *");
            fail("TranId is still known after tran is complete!");
        }

        // Rollback xid2, this should FAIL
        try {
            _manager.rollback(new PersistentTranId(xid2));

            print("* Exception handling on rollback of XID2   - FAILED  *");
            fail("XidManager failed to throw Exception during rollback!");
        } catch (XidUnknownException xue) {
            print("* Exception handling on rollback of XID2   - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on rollback of XID2   - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during rollback of XID2!");
        }

        // Start a new association - this should SUCCEED
        try {
            TransactionParticipant part = new NullParticipant(this, "One");

            _manager.start(new PersistentTranId(xid1), part);

            print("* Start new association                    - SUCCESS *");
        } catch (Exception e) {
            print("* Start new association                    - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during XidManager.start()!");
        }

        // Prepare xid1, this should FAIL
        try {
            _manager.prepare(new PersistentTranId(xid1));

            print("* Exception handling on prepare of XID1    - FAILED  *");
            fail("Prepare failed to throw exception!");
        } catch (XidStillAssociatedException xsae) {
            print("* Exception handling on prepare of XID1    - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on prepare of XID1    - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during prepare of XID1!");
        }

        // Commit xid1, this should FAIL
        try {
            _manager.commit(new PersistentTranId(xid1), true);

            print("* Exception handling on commit of XID1     - FAILED  *");
            fail("Commit failed to throw exception!");
        } catch (XidStillAssociatedException xsae) {
            print("* Exception handling on commit of XID1     - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on commit of XID1     - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during commit of XID1!");
        }

        // Rollback xid1, this should FAIL
        try {
            _manager.rollback(new PersistentTranId(xid1));

            print("* Exception handling on rollback of XID1   - FAILED  *");
            fail("Rollback failed to throw exception!");
        } catch (XidStillAssociatedException xsae) {
            print("* Exception handling on rollback of XID1   - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on rollback of XID1   - FAILED  *");
            e.printStackTrace(System.err);
            fail("Unexpected exception thrown during rollback of XID1!");
        }

        if (MS != null) {
            stopMessageStore(MS);
        }

        print("*                                                    *");
        print("***************** XidManagerBlackBox *****************");
    }

    private class XidThread extends Thread {
        private final MessageStoreTestCase _test;

        private final Xid _xid;
        private final XidManager _manager;

        public XidThread(MessageStoreTestCase test, Xid xid, XidManager manager) {
            _test = test;
            _xid = xid;
            _manager = manager;
        }

        @Override
        public void run() {
            // Start a new association - this should SUCCEED
            try {
                TransactionParticipant tran = new NullParticipant(_test, getName());

                _manager.start(new PersistentTranId(_xid), tran);

                _test.print("* (" + getName() + ")Start new association               - SUCCESS *");
            } catch (Exception e) {
                _test.print("* (" + getName() + ")Start new association               - FAILED  *");
                e.printStackTrace(System.err);
                _test.fail("Exception thrown during XidManager.start()!");
            }

            // End the association - this should SUCCEED
            try {
                _manager.end(new PersistentTranId(_xid), XAResource.TMSUCCESS);

                _test.print("* (" + getName() + ")End association                     - SUCCESS *");
            } catch (Exception e) {
                _test.print("* (" + getName() + ")End association                     - FAILED  *");
                e.printStackTrace(System.err);
                _test.fail("Exception thrown during XidManager.end()!");
            }
        }
    }
}
