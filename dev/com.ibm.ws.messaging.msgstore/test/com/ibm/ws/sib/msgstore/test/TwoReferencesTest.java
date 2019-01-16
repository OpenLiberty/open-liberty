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

import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

public class TwoReferencesTest extends MessageStoreTestCase
{
    public TwoReferencesTest(String arg0)
    {
        super(arg0);
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        TwoReferencesTest test = new TwoReferencesTest("testTwoReferences");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testTwoReferences()
    {
        MessageStore messageStore = createAndStartMessageStore(true, PERSISTENCE);
        ItemStream itemStream = createNonPersistentRootItemStream(messageStore);
        try
        {
            Transaction autoTran = messageStore.getTransactionFactory().createAutoCommitTransaction();
            ReferenceStream referenceStream1 = new ReferenceStream();
            ReferenceStream referenceStream2 = new ReferenceStream();
            itemStream.addReferenceStream(referenceStream1, autoTran);
            itemStream.addReferenceStream(referenceStream2, autoTran);


            ExternalLocalTransaction localtran = messageStore.getTransactionFactory().createLocalTransaction();

            Item item = new Item();
            itemStream.addItem(item, localtran);

            ItemReference reference1 = new RefTestRef(item);
            ItemReference reference2 = new RefTestRef(item);

            referenceStream1.add(reference1, localtran);
            referenceStream2.add(reference2, localtran);
            localtran.commit();
            assertEquals("reference count incorrect",2, item.getReferenceCount());
        }
        catch (Exception e)
        {
            fail(e.toString());
        }
        finally
        {
            stopMessageStore(messageStore);
        }
    }

    public static class RefTestItem extends Item
    {
        public RefTestItem()
        {
            super();
        }
    }

    public static class RefTestRef extends ItemReference
    {
        public RefTestRef()
        {
            super();
        }
        public RefTestRef(Item item)
        {
            super(item);
        }

    }
}
