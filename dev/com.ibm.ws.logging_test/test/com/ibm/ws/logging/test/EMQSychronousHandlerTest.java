/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.collector.manager.buffer.BufferManagerEMQHelper;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;

import test.common.SharedOutputManager;

public class EMQSychronousHandlerTest {

    private static SharedOutputManager outputMgr;
    private static BufferManagerImpl testBufferManager;
    private static DummyHandler syncHandler;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("com.ibm.ws.collector.manager.*=all");
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
        System.out.println("@AGG order is: " + order);
    }

    public static String order = "";

    @Rule
    public TestName tn = new TestName();

    public void BeforeTest() throws Exception {
        testBufferManager = new BufferManagerImpl(10000, "testData");
        syncHandler = new DummyHandler();
        order += "  " + tn.getMethodName();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        testBufferManager.removeHandler(syncHandler.getHandlerName());
    }

    // Different JDKs will run unit tests in different orderings.  Since the tests in this class depend on test ordering,
    // we will simply combine all tests into one JUnit test to control the ordering
    @Test
    public void testAll() throws Exception {
        BeforeTest();
        SendEMQMessagesToSyncHandler();
        tearDown();

        BeforeTest();
        SendEMQMessagesToMultipleSyncHandlers();
        tearDown();

        BeforeTest();
        EmptyEMQSentToSyncHandler();
        tearDown();

        BeforeTest();
        FullEMQSentToSyncHandler();
        tearDown();

        BeforeTest();
        RemoveEMQThenAddSyncHandler();
        tearDown();

        BeforeTest();
        RemoveEMQ();
        tearDown();
    }

    //@Test
    public void SendEMQMessagesToSyncHandler() {
        for (int i = 0; i < 50; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(50, syncHandler.getNumOfMessages());
    }

    //@Test
    public void SendEMQMessagesToMultipleSyncHandlers() {
        DummyHandler syncHandler2 = new DummyHandler();

        for (int i = 0; i < 50; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        testBufferManager.addSyncHandler(syncHandler2);
        assertEquals(50, syncHandler.getNumOfMessages());
        assertEquals(50, syncHandler2.getNumOfMessages());
        testBufferManager.removeSyncHandler(syncHandler2);
    }

    //@Test
    public void EmptyEMQSentToSyncHandler() {
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(0, syncHandler.getNumOfMessages());
    }

    //@Test
    public void FullEMQSentToSyncHandler() {
        for (int i = 0; i < 401; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);

        //Make sure the messages that the sync handler received were the newest 400
        for (int i = 1; i < 401; i++) {
            assertEquals("Message" + i, syncHandler.messageHolder.get(i - 1));
        }
        assertEquals(400, syncHandler.getNumOfMessages());
    }

    //@Test
    public void RemoveEMQThenAddSyncHandler() {
        for (int i = 0; i < 200; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(200, syncHandler.getNumOfMessages());
        BufferManagerEMQHelper.removeEMQTrigger();

        DummyHandler syncHandler2 = new DummyHandler();
        testBufferManager.add(syncHandler2);
        assertEquals(0, syncHandler2.getNumOfMessages());
    }

    //@Test
    public void RemoveEMQ() {
        BufferManagerEMQHelper.removeEMQTrigger();
        assertTrue(BufferManagerEMQHelper.getEMQRemovedFlag());
    }

}
