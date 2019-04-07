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
 * Reason          Date        Origin       Description
 * --------------- ------      --------     --------------------------------------------
 *                 Jun 26, 2003 Mar 21, 2003 van Leersum  Original
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;

public class UnlockCountTest extends MessageStoreTestCase
{
    public UnlockCountTest(String str)
    {
        super(str);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        UnlockCountTest test = new UnlockCountTest("testUnlockCount");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testUnlockCount()
    {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream itemStream = createPersistentRootItemStream(messageStore);
        try
        {

            Item item = new Item();
            itemStream.addItem(item, messageStore.getTransactionFactory().createAutoCommitTransaction());
            assertEquals("unlock count should be zero: ", 0, item.guessUnlockCount());

            LockingCursor cursor = itemStream.newLockingItemCursor(null);
            AbstractItem lockedItem = cursor.next();
            assertNotNull("should have got one", lockedItem);
            assertEquals("Should have got the one we put. ", item, lockedItem);

            assertEquals("unlock count incorrect: ", 0, item.guessUnlockCount());

            item.unlock(item.getLockID());
            assertEquals("unlock count incorrect: ", 1, item.guessUnlockCount());

            cursor = itemStream.newLockingItemCursor(null);
            lockedItem = cursor.next();
            assertNotNull("should have got one", lockedItem);
            assertEquals("Should have got the one we put. ", item, lockedItem);

            assertEquals("unlock count incorrect: ", 1, item.guessUnlockCount());

            item.unlock(item.getLockID());
            assertEquals("unlock count incorrect: ", 2, item.guessUnlockCount());

        }
        catch (Exception e)
        {
            fail(e.toString());
        }
        stopMessageStore(messageStore);
    }

}
