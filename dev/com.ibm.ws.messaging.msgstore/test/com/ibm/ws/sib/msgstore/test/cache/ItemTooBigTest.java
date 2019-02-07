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
/*
 * 
 *
 * Change activity:
 *
 * Reason          Date        Origin       Description
 * --------------- ----------  -----------  --------------------------------------------
 *                 27/10/2003  van Leersum  Original
 * 270103          27/04/2005  schofiel     sib.msgstore.OutOfCacheSpace exceptions before mediation
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test.cache;

import com.ibm.ws.sib.admin.JsHealthState;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.CacheStatistics;
import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * Test the behaviour when adding a new item to a full cache.
 * The existing items are all avaialable
 * 
 * @author DrPhill
 * 
 */
public class ItemTooBigTest extends MessageStoreTestCase implements MessageStoreConstants
{
    public ItemTooBigTest(String arg0)
    {
        super(arg0);
    }

    public void testItemTooBig()
    {
        // set maximum in cache
        String originalStored = System.getProperty(STANDARD_PROPERTY_PREFIX + PROP_STORED_CACHE_SIZE);
        String originalUnstored = System.getProperty(STANDARD_PROPERTY_PREFIX + PROP_UNSTORED_CACHE_SIZE);

        System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_STORED_CACHE_SIZE, "5000");
        System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_UNSTORED_CACHE_SIZE, "5000");
        // set trace
        //        String trace = "com.ibm.ws.sib.msgstore.cache.ref.*=all=enabled";
        //        configureTrace(trace);
        //        turnOnTrace();

        MessageStore messageStore = null;
        try
        {
            messageStore = MessageStore.createInstance();
            Configuration configuration = Configuration.createBasicConfiguration();
            configuration.setObjectManagerLogDirectory("build");
            configuration.setObjectManagerPermanentStoreDirectory("build");
            configuration.setObjectManagerTemporaryStoreDirectory("build");
            configuration.setCleanPersistenceOnStart(true);
            messageStore.initialize(configuration);
            messageStore.start();

            JsHealthState state = messageStore.getHealthState();
            if (!state.isOK())
            {
                fail("Failed to start message store. Health State: " + state);
            }

            testMe(messageStore);

        } catch (Exception e)
        {
            e.printStackTrace(System.err);
            fail("exception: " + e);
        } finally
        {
            if (null == originalStored)
            {
                System.getProperties().remove(STANDARD_PROPERTY_PREFIX + PROP_STORED_CACHE_SIZE);
            }
            else
            {
                System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_STORED_CACHE_SIZE, originalStored);
            }
            if (null == originalStored)
            {
                System.getProperties().remove(STANDARD_PROPERTY_PREFIX + PROP_UNSTORED_CACHE_SIZE);
            }
            else
            {
                System.setProperty(STANDARD_PROPERTY_PREFIX + PROP_UNSTORED_CACHE_SIZE, originalUnstored);
            }
            if (null != messageStore)
            {
                messageStore.stop(0);
            }
        }
    }

    public void testMe(MessageStore messageStore) throws Exception {
        Transaction transaction = messageStore.getTransactionFactory().createAutoCommitTransaction();
        ItemStream itemStream = new PersistentItemStream();
        messageStore.add(itemStream, transaction);

        CacheStatistics storedCacheStatistics = messageStore.getStoredCacheStatistics();
        CacheStatistics nonStoredCacheStatistics = messageStore.getNonStoredCacheStatistics();
        storedCacheStatistics.resetTotals();
        nonStoredCacheStatistics.resetTotals();

        Statistics listStatistics = itemStream.getStatistics();

        assertEquals(0, storedCacheStatistics.getTotalDiscardCount());
        assertEquals(0, storedCacheStatistics.getTotalRefusalCount());
        assertEquals(0, storedCacheStatistics.getCurrentCount());
        assertEquals(0, listStatistics.getTotalItemCount());

        assertEquals(0, nonStoredCacheStatistics.getTotalDiscardCount());
        assertEquals(0, nonStoredCacheStatistics.getTotalRefusalCount());
        assertEquals(0, nonStoredCacheStatistics.getCurrentCount());
        assertEquals(0, listStatistics.getTotalItemCount());

        Item item = new Item(AbstractItem.STORE_NEVER, Integer.parseInt(PROP_STORED_CACHE_MAXIMUM_ITEM_SIZE_DEFAULT) + 1);
        itemStream.addItem(item, transaction);
        assertEquals(1, nonStoredCacheStatistics.getTotalRefusalCount());
    }

}
