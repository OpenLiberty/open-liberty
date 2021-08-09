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
 * Reason            Date    Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 168081          23/07/03  gareth   GlobalTransaction Support (Local Clients) 
 * 181930          17/11/03  gareth   XA Recovery Support
 * 186657.1        24/05/04  gareth   Per-work-item error checking.
 * 341158          13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.XidParticipant;
import com.ibm.ws.sib.transactions.PersistentTranId;

public class XidParticipantBlackBoxTest extends MessageStoreTestCase {
    public XidParticipantBlackBoxTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(XidParticipantBlackBoxTest.class);
    }

    public void testXidParticipantBlackBox() {
        print("************** XidParticipantBlackBox ****************");
        print("*                                                    *");

        XidParticipant tran = new XidParticipant(null, new PersistentTranId(new NullXid("Xid1")), new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a null WorkList, this should FAIL.
        try {
            tran.addWork(null);

            // If we get this far then we're broken.
            print("* Exception handling on addWork(null)      - FAILED  *");
            fail("Register of null failed to throw Exception!");
        } catch (ProtocolException pe) {
            // Failed as expected.
            print("* Exception handling on addWork(null)      - SUCCESS *");
        } catch (Exception e) {
            print("* Exception handling on addWork(null)      - FAILED  *");
            fail("Register of null threw wrong Exception!");
        }

        // Attempt to register a WorkList, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Registration of WorkItem                 - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Registration of WorkItem                 - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to commit with 1PC, this should Succeed.
        try {
            print("* Commit Transaction with 1PC:                       *");

            tran.commit(true);

            // Succeeded as expected.
            print("* Commit Transaction with 1PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction                       - FAILED  *");
            fail("Commit of transaction failed!");
        }

        // Attempt to prepare after completion, this should FAIL.
        try {
            tran.prepare();

            // If we get this far then we're broken.
            print("* Exception handling on prepare()          - FAILED  *");
            fail("Prepare on completed transaction failed to throw Exception!");
        } catch (Exception e) {
            // Failed as expected.
            print("* Exception handling on prepare()          - SUCCESS *");
        }

        // Attempt to commit after completion, this should FAIL.
        try {
            tran.commit(true);

            // If we get this far then we're broken.
            print("* Exception handling on commit(1PC)        - FAILED  *");
            fail("Commit on completed transaction failed to throw Exception!");
        } catch (Exception e) {
            // Failed as expected.
            print("* Exception handling on commit()           - SUCCESS *");
        }

        try {
            tran.commit(false);

            // If we get this far then we're broken.
            print("* Exception handling on commit(2PC)        - FAILED  *");
            fail("Commit on completed transaction failed to throw Exception!");
        } catch (Exception e) {
            // Failed as expected.
            print("* Exception handling on commit()           - SUCCESS *");
        }

        // Attempt to rollback, this should FAIL.
        try {
            tran.rollback();

            // If we get this far then we're broken.
            print("* Exception handling on rollback()         - FAILED  *");
            fail("Rollback on completed transaction failed to throw Exception!");
        } catch (Exception e) {
            // Failed as expected.
            print("* Exception handling on rollback()         - SUCCESS *");
        }

        // Attempt to register a WorkList, this should FAIL.
        try {
            tran.addWork(new NullWorkItem(this));

            // If we get this far then we're broken.
            print("* Exception handling on addWork()          - FAILED  *");
            fail("addWork failed to throw Exception!");
        } catch (Exception e) {
            // Failed as expected.
            print("* Exception handling on addWork()          - SUCCESS *");
        }

        tran = new XidParticipant(null, null, new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a WorkList, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Registration of WorkItem                 - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Registration of WorkItem                 - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // Attempt to prepare for 2PC, this should SUCCEED.
        try {
            print("* Prepare Transaction:                               *");

            tran.prepare();

            // Succeeded as expected.
            print("* Prepare Transaction                      - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Prepare Transaction                      - FAILED  *");
            fail("Prepare of XidParticipant failed!");
        }

        // Attempt to commit wih 2PC, this should SUCCEED.
        try {
            print("* Commit Transaction with 2PC:                       *");

            tran.commit(false);

            // Succeeded as expected.
            print("* Commit Transaction with 2PC              - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Commit Transaction with 2PC              - FAILED  *");
            fail("Commit of XidParticipant failed!");
        }

        tran = new XidParticipant(null, new PersistentTranId(new NullXid("Xid2")), new NullPersistenceManager(this), 0);
        tran.registerCallback(new NullTransactionCallback(this));

        // Attempt to register a WorkList, this should SUCCEED.
        try {
            tran.addWork(new NullWorkItem(this));

            // Succeeded as expected.
            print("* Registration of WorkItem                 - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Registration of WorkItem                 - FAILED  *");
            fail("Register of WorkItem failed!");
        }

        // output the contents of the XidParticipant in XML
        try {
            print("* Output XidParticipant in XML:                      *");

            print(tran.toXmlString());

            print("* Output XidParticipant in XML             - SUCCESS *");
        } catch (Exception e) {
            print("* Output XidParticipant in XML             - FAILED  *");
            e.printStackTrace(System.err);
            fail("Exception thrown during toXmlString()!");
        }

        // Attempt to rollback, this should Succeed.
        try {
            print("* Rollback Transaction:                              *");

            tran.rollback();

            // Succeeded as expected.
            print("* Rollback Transaction                     - SUCCESS *");
        } catch (Exception e) {
            // If we get this far then we're broken.
            print("* Rollback Transaction                     - FAILED  *");
            fail("Rollback of transaction failed!");
        }

        // Get the transaction type, this should be TX_LOCAL
        int type = ((PersistentTransaction) tran).getTransactionType();

        if (type == PersistentTransaction.TX_GLOBAL) {
            // Succeeded as expected.
            print("* Transaction Type is TX_GLOBAL            - SUCCESS *");
        } else {
            // If we get this far then we're broken.
            print("* Transaction Type is TX_GLOBAL            - FAILED  *");
            fail("getTransactionType failed!");
        }

        // Get the transaction id.
        print("* Get Transaction Id                                 *");

        StringBuffer line = new StringBuffer("* - Transaction Id is :                              *");
        String idStr = tran.getPersistentTranId().toString();
        line.replace(24, 24 + idStr.length(), idStr);

        print(line.toString());
        print("* Get Transaction Id                       - SUCCESS *");

        print("*                                                    *");
        print("************** XidParticipantBlackBox ****************");
    }
}
