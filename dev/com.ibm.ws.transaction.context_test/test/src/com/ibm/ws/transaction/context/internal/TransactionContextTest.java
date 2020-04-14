package com.ibm.ws.transaction.context.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableUserTransactionImpl;
import com.ibm.tx.ltc.impl.LocalTranCoordImpl;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereUserTransaction;
import com.ibm.wsspi.threadcontext.ThreadContext;

import test.common.SharedOutputManager;

/**
 * Unit tests for TransactionContextImpl.
 */
public class TransactionContextTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Tests that we can access the embeddable local transaction current manager.
     *
     * @throws Exception
     */
    @Test
    public void testAccessToEmbeddableLocalTranMgr() throws Exception {
        LocalTransactionCurrent ltcTm = EmbeddableTransactionManagerFactory.getLocalTransactionCurrent();
        assertTrue(ltcTm != null);
    }

    /**
     * Tests that we can get a UserTransaction reference.
     *
     * @throws Exception
     */
    @Test
    public void testAccessToUserTransaction() throws Exception {
        EmbeddableWebSphereUserTransaction userTx = EmbeddableUserTransactionImpl.instance();
        assertTrue(userTx != null);
    }

    /**
     * Tests that the context provider creates a context when getContext is invoked.
     *
     * @throws Exception
     */
    @Test
    public void testCtxProviderGetContext() throws Exception {
        assertTrue(getContext() != null);
    }

    /**
     * Tests taskStarting and taskStopping. Normal completion expected.
     *
     * @throws Exception
     */
    @Test
    public void testTxContextTaskStartingStoppingNormalCompletion() throws Exception {

        // Make sure there is no transaction on the thread.
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        UOWCoordinator uowc1 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc1 == null);

        // Start a global transaction.
        EmbeddableWebSphereUserTransaction ut = EmbeddableUserTransactionImpl.instance();
        ut.begin();

        // Make sure there is a global transaction on the thread.
        UOWCoordinator uowc2 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc2 != null && (uowc2.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));

        // Call taskStarting.
        ThreadContext context = getContext();
        context.taskStarting();

        // Make sure that a local transaction was put on the thread.
        UOWCoordinator uowc3 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc3 != null && (uowc3.getTxType() == UOWCoordinator.TXTYPE_LOCAL));
        assertTrue(uowc3 != uowc1);

        // Call taskStopping.
        context.taskStopping();

        // Make sure the global transaction is back on the thread.
        UOWCoordinator uowc4 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc4 != null && (uowc4.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));
        assertTrue(uowc2 == uowc4);

        // Commit the global transaction and make sure there is nothing left on the thread.
        ut.commit();
        UOWCoordinator uowc5 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc5 == null);
    }

    /**
     * Test taskStarting and taskStopping.
     * Exception expected on taskStopping due to dangling global transaction.
     *
     * @throws Exception
     */
    @Test
    public void testTaskStartingStoppingFailureDueToDanglingGlobalTx() throws Exception {

        // Make sure there is no transaction on the thread.
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        UOWCoordinator uowc1 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc1 == null);

        // Start a global transaction.
        EmbeddableWebSphereUserTransaction ut = EmbeddableUserTransactionImpl.instance();
        ut.begin();

        // Make sure there is a global transaction on the thread.
        UOWCoordinator uowc2 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc2 != null && (uowc2.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));

        // Call taskStarting.
        ThreadContext context = getContext();
        context.taskStarting();

        // Make sure that a local transaction was put on the thread.
        UOWCoordinator uowc3 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc3 != null && (uowc3.getTxType() == UOWCoordinator.TXTYPE_LOCAL));
        assertTrue(uowc3 != uowc1);

        // end the local transaction to start a global transaction next.
        LocalTranCoordImpl ltCoord = (LocalTranCoordImpl) uowc3;
        ltCoord.end(LocalTranCoordImpl.EndModeRollBack);

        // Begin dangler global transaction.
        ut.begin();

        // Call taskStopping. Exception is expected. Dangling global transaction should have been rolled back.
        context.taskStopping();

        // Make sure the global transaction is back on the thread.
        UOWCoordinator uowc4 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc4 != null && (uowc4.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));
        assertTrue(uowc2 == uowc4);

        // Commit the global transaction and make sure there is nothing left on the thread.
        ut.commit();
        UOWCoordinator uowc5 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc5 == null);
    }

    /**
     * Test (taskStarting (taskStarting task stopping) taskStopping). Normal completion expected.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleTaskStartingStopping() throws Exception {

        // Make sure there is no transaction on the thread.
        EmbeddableWebSphereTransactionManager tm = EmbeddableTransactionManagerFactory.getTransactionManager();
        UOWCoordinator uowc1 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc1 == null);

        // Start a global transaction 1.
        EmbeddableWebSphereUserTransaction ut = EmbeddableUserTransactionImpl.instance();
        ut.begin();

        // Make sure global transaction 1 is on the thread.
        UOWCoordinator uowc2 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc2 != null && (uowc2.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));

        // Call taskStarting 1
        ThreadContext context = getContext();
        context.taskStarting();

        // Make sure that a local transaction was put on the thread.
        UOWCoordinator uowc3 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc3 != null && (uowc3.getTxType() == UOWCoordinator.TXTYPE_LOCAL));
        assertTrue(uowc3 != uowc1);

        // end the local transaction to start a global transaction next.
        LocalTranCoordImpl ltCoord = (LocalTranCoordImpl) uowc3;
        ltCoord.end(LocalTranCoordImpl.EndModeRollBack);

        // Begin global transaction 2.
        ut.begin();

        // Make sure global transaction 2 is on the thread.
        UOWCoordinator uowc22 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc22 != null && (uowc22.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));

        // Call taskStarting 2.
        ThreadContext context2 = context.clone();
        context2.taskStarting();

        // Make sure that a local transaction was put on the thread.
        UOWCoordinator uowc33 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc33 != null && (uowc33.getTxType() == UOWCoordinator.TXTYPE_LOCAL));
        assertTrue(uowc33 != uowc1);

        // Call taskStopping 2.
        context2.taskStopping();

        // Make sure the global transaction 2 is back on the thread.
        UOWCoordinator uowc44 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc44 != null && (uowc44.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));
        assertTrue(uowc22 == uowc44);

        // Commit global transaction 2.
        ut.commit();
        UOWCoordinator uowc55 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc55 == null);

        // Call taskStopping 1.
        context.taskStopping();

        // Make sure global transaction 1 is back on the thread.
        UOWCoordinator uowc4 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc4 != null && (uowc4.getTxType() == UOWCoordinator.TXTYPE_INTEROP_GLOBAL));
        assertTrue(uowc2 == uowc4);

        // Commit global transaction 1.
        ut.commit();

        // Validate that there are no transactions left on the thread.
        UOWCoordinator uowc5 = ((UOWCurrent) tm).getUOWCoord();
        assertTrue(uowc5 == null);
    }

    /**
     * Gets a context object reference.
     *
     * @return The transaction context.
     */
    private ThreadContext getContext() {
        TransactionContextProviderImpl txCtxProvider = new TransactionContextProviderImpl();
        ThreadContext context = txCtxProvider.captureThreadContext(Collections.<String, String> emptyMap(), null);
        return context;
    }

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        System.setProperty("com.ibm.tx.config.ConfigurationProviderClassName", "com.ibm.tx.jta.config.DefaultConfigurationProvider");
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }
}
