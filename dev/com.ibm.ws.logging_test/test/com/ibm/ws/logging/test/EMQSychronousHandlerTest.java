package com.ibm.ws.logging.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.DummyHandler;

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
    }

    @Before
    public void BeforeTest() throws Exception {
        testBufferManager = new BufferManagerImpl(10000, "testData");
        syncHandler = new DummyHandler();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        testBufferManager.removeHandler(syncHandler.getHandlerName());
    }

    @Test
    public void SendEMQMessagesToSyncHandler() {
        for (int i = 0; i < 50; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(50, syncHandler.getNumOfMessages());
    }

    @Test
    public void SendEMQMessagesToMultipleSyncHandlers() {
        DummyHandler syncHandler2 = new DummyHandler();
        System.out.println("asdasd");

        for (int i = 0; i < 50; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        testBufferManager.addSyncHandler(syncHandler2);
        assertEquals(50, syncHandler.getNumOfMessages());
        assertEquals(50, syncHandler2.getNumOfMessages());
        testBufferManager.removeSyncHandler(syncHandler2);
    }

    @Test
    public void EmptyEMQSentToSyncHandler() {
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(0, syncHandler.getNumOfMessages());
    }

    @Test
    public void FullEMQSentToSyncHandler() {
        for (int i = 0; i < 400; i++) {
            testBufferManager.add("Message" + i);
        }
        testBufferManager.addSyncHandler(syncHandler);
        assertEquals(400, syncHandler.getNumOfMessages());
    }

    @Test
    public void RemoveEMQ() {
        BufferManager.removeEMQTrigger();
        assertTrue(BufferManager.EMQRemovedFlag);
    }

}
