/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.Transaction;

/**
 * Test the behaviour when adding a new item to a full cache. The existing items
 * are all avaialable
 *
 * @author DrPhill
 *
 */
public class ItemTooBigTest extends MessageStoreTestCase implements MessageStoreConstants {
    private static final class PropertySetter implements AutoCloseable {
        private final String key;
        private final String originalValue;

        PropertySetter(String key, String value) {
            this.key = key;
            this.originalValue = System.getProperty(key);
            System.setProperty(key, value);
        }

        public void close() {
            if (null == originalValue) System.clearProperty(key);
            else System.setProperty(key, originalValue);
        }
    }

    public ItemTooBigTest(String arg0) {
        super(arg0);
    }

    public void testItemTooBig() throws Exception {
        // set maximum in cache
        final String keyForStored = STANDARD_PROPERTY_PREFIX + PROP_STORED_CACHE_SIZE;
        final String keyForUnstored = STANDARD_PROPERTY_PREFIX + PROP_UNSTORED_CACHE_SIZE;

        try (
                PropertySetter s1 = new PropertySetter(keyForStored, "5000");
                PropertySetter s2 = new PropertySetter(keyForUnstored, "5000")) {
            Configuration configuration = Configuration.createBasicConfiguration();
            configuration.setObjectManagerLogDirectory("build");
            configuration.setObjectManagerPermanentStoreDirectory("build");
            configuration.setObjectManagerTemporaryStoreDirectory("build");
            configuration.setCleanPersistenceOnStart(true);

            final MessageStore messageStore = MessageStoreImpl.createForTesting();
            messageStore.initialize(configuration);
            messageStore.start();
            try {
                JsHealthState state = messageStore.getHealthState();
                if (!state.isOK()) {
                    fail("Failed to start message store. Health State: " + state);
                }

                testMe(messageStore);
            } finally {
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
