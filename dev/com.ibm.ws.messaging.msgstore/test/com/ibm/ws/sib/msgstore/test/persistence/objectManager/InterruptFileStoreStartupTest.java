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
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * 549131          27/08/09 gareth    Make startup retry loop interruptable
 * 126016          19/03/14 romehla1  Test failure com.ibm.ws.sib.msgstore.test.persistence.objectManager.PersistenceFullTest.testPersistenceFull
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.test.MessageStoreTestCase;

public class InterruptFileStoreStartupTest extends MessageStoreTestCase {
    public InterruptFileStoreStartupTest(String name) {
        super(name);

        //turnOnTrace();
    }

    /**
     * This is a Object Manager specific test so no persistence
     * choice is provided.
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite(InterruptFileStoreStartupTest.class);

        return suite;
    }

    public void testInterruptFileStoreStartup() {
        print("|-----------------------------------------------------");
        print("| InterruptFileStoreStartup:");
        print("|---------------------------");
        print("|");

        MessageStore MS = null;

        Thread startupThread = null;

        try {
            // Start the MS to give us a handle to use, this should SUCCEED
            try {
                MS = createMessageStore(true, OBJECTMANAGER_PERSISTENCE);

                print("| Created a MessageStore");
            } catch (Exception e) {
                print("| Create a MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Exception thrown during MessageStore create!");
            }

            // Set the log file name and start the MessageStore.
            try {
                ((MessageStoreImpl) MS).setCustomProperty(MessageStoreConstants.STANDARD_PROPERTY_PREFIX
                                                          + MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_NAME
                                                          , ".:|?");
                print("| Set illegal Log file name");

                startupThread = new StartThread(MS);
                startupThread.start();

                print("| Started the MessageStore start thread");

            } catch (Exception e) {
                print("| Start the MessageStore start thread   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Unexpected exception thrown during MessageStore start!");
            }
            //defect 126016
            try {
                synchronized (this) {
                    wait(20000);
                }

                print("| Waited 20 seconds");
            } catch (Exception e) {
                print("| Wait 20 seconds   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Unexpected exception thrown waiting for 10 seconds!");
            }

            try {
                stopMessageStore(MS);
                MS = null;

                print("| Stop called on MessageStore");

            } catch (Exception e) {
                print("| Stop the MessageStore   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Unexpected exception thrown during MessageStore stop!");
            }

            try {
                // defect 126016
                startupThread.join(25000);

                print("| Joined the start thread");

                //defect 126016
                try {
                    synchronized (this) {
                        wait(10000);
                    }

                    print("| Waited 10 seconds");
                } catch (Exception e) {
                    print("| Wait 10 seconds   !!!FAILED!!!");
                    e.printStackTrace(System.err);
                    fail("Unexpected exception thrown waiting for 10 seconds!");
                }

            } catch (Exception e) {
                print("| Join the start thread   !!!FAILED!!!");
                e.printStackTrace(System.err);
                fail("Unexpected exception thrown joining start thread!");
            }

            if (startupThread.getState() != Thread.State.TERMINATED) {
                fail("| Start thread has not completed!");
            } else {
                print("| Start thread completed as expected");
            }
        } finally {
            if (MS != null) {
                stopMessageStore(MS);

                print("| Stopped MessageStore");
            }

            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    private class StartThread extends Thread {
        private final MessageStore _ms;

        public StartThread(MessageStore ms) {
            _ms = ms;
        }

        @Override
        public void run() {
            // Start the message store. This should enter its 
            // retry loop as a nonsense file path was supplied.
            try {
                _ms.start(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            print("| - Start of MessageStore finished");
        }
    }
}
