/*==============================================================================
 * Copyright (c) 2012,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *==============================================================================
 */
package com.ibm.ws.sib.msgstore.test.persistence.objectManager;

import com.ibm.ws.sib.admin.JsHealthState;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.PersistenceFullException;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;
import junit.framework.TestSuite;

public class PersistenceFullTest extends MessageStoreTestCase {
    public PersistenceFullTest(String name) {
        super(name);
        //turnOnTrace();
    }

    /**
     * This is a Object Manager specific test so no persistence
     * choice is provided.
     */
    public static TestSuite suite() {
        return new TestSuite(PersistenceFullTest.class);
    }

    public void testPersistenceFull() throws Exception {
        final String methodName = getName();
        printStarHead(methodName);
        printStarLine();

        // Start the MS to give us a handle to use, this should SUCCEED
        final MessageStore MS = createMessageStore(true, OBJECTMANAGER_PERSISTENCE);
        try {
            printSuccess("Create a MessageStore");

            // Set the log file sizes and start the MessageStore.
/*
            ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                     +MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_SIZE
                                                     ,"8388608");
            printSuccess("Set Log file size to 8MB");

            ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                     +MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE
                                                     ,"10485760");

            ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                     +MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE
                                                     ,"10485760");
            printSuccess("Set Permanent store size to 10MB");

            ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                     +MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE
                                                     ,"10485760");

            ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                     +MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE
                                                     ,"10485760");
            printSuccess("Set Temporary store size to 10MB");
*/
            MS.start();

            final JsHealthState state = MS.getHealthState();
            if (!state.isOK()) {
                printFailed("Start the MessageStore", state);
                fail("Failed to start message store. Health State: " + state);
            }

            printSuccess("Start the MessageStore");

            // Try to create an ItemStream, this should SUCCEED
            printStarLine("Create Root ItemStream:");

            final ItemStream root = createPersistentRootItemStream(MS);

            printStarLine("- Root ItemStream size: ", root.getStatistics().getTotalItemCount());
            printSuccess("Create Root ItemStream");

            // Try to create an AutoCommitTransaction, this should SUCCEED
            final ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();
            printSuccess("Create AutoCommitTransaction");

            // Try to add an Item to the ItemStream, this should SUCCEED until we fill the log/os
            for (int i=0; i<15; i++) {
                try {
                    printStarLine("Item Put:");
        
                    Item putItem = new OneMegItem();

                    printStarLine(putItem);
        
                    root.addItem(putItem, tran);

                    printStarLine("Root ItemString size: ", root.getStatistics().getTotalItemCount());
                    printSuccess("Item Put");
                } catch(RollbackException rbe) {
                    if (rbe.getCause() instanceof PersistenceFullException) {
                        printSuccess("Item Put failed due to PersistenceFull");
                        return;
                    }
                    throw new Exception("RollbackException occurred with unexpected causal exception", rbe);
                }
            }
        } finally {
            if (MS != null) {
                stopMessageStore(MS);
                printSuccess("Stop MessageStore");
            }

            printStarLine();
            printStarHead(methodName);
        }
    }
}
