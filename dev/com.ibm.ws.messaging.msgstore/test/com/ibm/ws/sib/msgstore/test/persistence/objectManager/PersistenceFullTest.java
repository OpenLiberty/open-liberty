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
package com.ibm.ws.sib.msgstore.test.persistence.objectManager;
/*
 * Change activity:
 *
 *  Reason         Date    Origin    Description
 * ------------- -------- --------- -------------------------------------------
 * 327709        06/12/05  gareth   Output NLS messages when OM files are full
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.admin.JsHealthState;

import com.ibm.ws.sib.msgstore.*;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.ExternalAutoCommitTransaction;

import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;

public class PersistenceFullTest extends MessageStoreTestCase
{
    public PersistenceFullTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    /**
     * This is a Object Manager specific test so no persistence
     * choice is provided.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(PersistenceFullTest.class);

        return suite;
    }

    public void testPersistenceFull()
    {
        print("**************** testPersistenceFull *****************");
        print("*                                                    *");

        MessageStore MS = null;

        try
        {
            // Start the MS to give us a handle to use, this should SUCCEED
            try
            {
                MS = createMessageStore(true, OBJECTMANAGER_PERSISTENCE);
    
                print("* Create a MessageStore                    - SUCCESS *");
            }
            catch (Exception e)
            {
                print("* Create a MessageStore                    - FAILED  *");
                fail("Exception thrown during MessageStore create!");
            }

            // Set the log file sizes and start the MessageStore.
            try
            {
/*                ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                         +MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_SIZE
                                                         ,"8388608");
                print("* Set Log file size to 8MB                 - SUCCESS *");

                ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                         +MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE
                                                         ,"10485760");
    
                ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                         +MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE
                                                         ,"10485760");
                print("* Set Permanent store size to 10MB         - SUCCESS *");
    
                ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                         +MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE
                                                         ,"10485760");
    
                ((MessageStoreImpl)MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                         +MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE
                                                         ,"10485760");
                print("* Set Temporary store size to 10MB         - SUCCESS *");
*/
                MS.start();
    
                JsHealthState state = MS.getHealthState();
                if (!state.isOK())
                {
                    print("* Start the MessageStore                   - FAILED  *");
                    fail("Failed to start message store. Health State: " + state);
                }

                print("* Start the MessageStore                   - SUCCESS *");

            }
            catch(Exception e)
            {
                print("* Start the MessageStore                   - FAILED  *");
                fail("Unexpected exception thrown during MessageStore start!");
            }

            long before, after = 0;
            ItemStream root = null;
            // Try to create an ItemStream, this should SUCCEED
            try
            {
                print("* Create Root ItemStream:                            *");
    
                root = createPersistentRootItemStream(MS);
    
                before = root.getStatistics().getTotalItemCount();
    
                print("* - Root ItemStream size: " + before + "                          *");
                print("* Create Root ItemStream                   - SUCCESS *");
            }
            catch (Exception e)
            {
                print("* Create Root ItemStream                   - FAILED  *");
                fail("Exception thrown during root ItemStream create!");
            }

            // Try to create an AutoCommitTransaction, this should SUCCEED
            ExternalAutoCommitTransaction tran = MS.getTransactionFactory().createAutoCommitTransaction();
            print("* Create AutoCommitTransaction             - SUCCESS *");

            boolean persistenceFilled = false;
            // Try to add an Item to the ItemStream, this should SUCCEED until we fill the log/os
            for (int i=0; i<15; i++)
            {
                try
                {
                    print("* Item Put:                                          *");
        
                    Item putItem = new OneMegItem();
                    StringBuffer out = new StringBuffer(((Object) putItem).toString());
                    int length = out.length();
        
                    print("* - " + out.substring(length - 48) + " *");
        
                    root.addItem(putItem, tran);
                    after = root.getStatistics().getTotalItemCount();

                    if (after < 10)
                    {
                        print("* - Root ItemStream size: " + after + "                          *");
                    }
                    else
                    {
                        print("* - Root ItemStream size: " + after + "                         *");
                    }
                    print("* Item Put                                 - SUCCESS *");
                }
                catch(RollbackException rbe)
                {
                    if (rbe.getCause() != null)
                    {
                        Throwable ex = rbe.getCause();
                        if (ex instanceof PersistenceFullException)
                        {
                            print("* Item Put failed due to PersistenceFull   - SUCCESS *");
                            persistenceFilled = true;
                            break;
                        }
                        else
                        {
                            print("* Item Put                                 - FAILED  *");
                            fail("Unexpected exception thrown whilst adding an Item!");
                        }
                    }
                    else
                    {
                        print("* Item Put                                 - FAILED  *");
                        fail("Unexpected exception thrown whilst adding an Item!");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    print("* Item Put                                 - FAILED  *");
                    fail("Unexpected exception thrown whilst adding an Item!");
                }
            }

            if (!persistenceFilled)
            {
                print("* Item Put failed due to PersistenceFull   - FAILED  *");
                fail("Persistence layer failed to fill up and thrown exception!");
            }
        }
        finally
        {
            if (MS != null)
            {
                stopMessageStore(MS);

                print("* Stop MessageStore                        - SUCCESS *");
            }

            print("*                                                    *");
            print("**************** testPersistenceFull *****************");
        }
    }
}
