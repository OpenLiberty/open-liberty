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
package com.ibm.ws.sib.msgstore.test;

/*
 * Change activity:
 *
 * Reason      Date    Origin   Description
 * ----------  ------  -------  -------------------------------------------
 * 218247      280704  corrigk  Add test for dump after changes to Expirer/alarm
 * 492055      270504  susana   Add tests for dumping persisted data
 * ============================================================================
 */
import java.io.FileWriter;

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

public class DumpTest extends MessageStoreTestCase {
    public DumpTest(String name) {
        super(name);
    }

    public static TestSuite suite(String persistence) {
        TestSuite suite = new TestSuite();

        DumpTest test = new DumpTest("testDump");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    //----------------------------------------------------------------------------
    // Create some items and then dump the Message Store. This is a manual test
    // to be run as required. Checking is done by examining the xml dump file.
    //----------------------------------------------------------------------------
    public void testDump() {
        // Create some items
        final int ITEMS = 10;
        MyItem item[] = new MyItem[ITEMS];
        for (int i = 0; i < ITEMS; i++) {
            item[i] = new MyItem("TestItem" + i, (i + 4) * 1000, i % 5);
        }

        MessageStore ms = null;
        try {
            ms = createAndStartMessageStore(true, PERSISTENCE);
            ExternalLocalTransaction uncotran = ms.getTransactionFactory().createLocalTransaction();

            PersistentItemStream is = new PersistentItemStream();

            // Add the stream to the store
            ms.add(is, uncotran);
            uncotran.commit();

            uncotran = ms.getTransactionFactory().createLocalTransaction();

            // Add the items to the stream
            for (int i = 0; i < ITEMS; i++) {
                is.addItem(item[i], uncotran);
            }

            assertEquals("Stream size incorrect", ITEMS, is.getStatistics().getTotalItemCount());
            uncotran.commit();

            LockingCursor cursor = is.newLockingItemCursor(null);

            // Wait for some expiry cycles to happen.
            Thread.sleep(6000);

            // Under JUnit, this will write the dump to a file named "default.xml"
            // Dump should show some items left in the expiry index.
            print("Dumping started");
            FormattedWriter fw = new FormattedWriter(new FileWriter("build/dump.xml"));
            ms.dump(fw, null);
            fw = new FormattedWriter(new FileWriter("build/dump-raw.xml"));
            ms.dump(fw, "raw");
            fw = new FormattedWriter(new FileWriter("build/dump-all.xml"));
            ms.dump(fw, "all");
            print("Dump complete");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        } finally {
            try {
                if (ms != null) {
                    stopMessageStore(ms);
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }
        }
    }
}
