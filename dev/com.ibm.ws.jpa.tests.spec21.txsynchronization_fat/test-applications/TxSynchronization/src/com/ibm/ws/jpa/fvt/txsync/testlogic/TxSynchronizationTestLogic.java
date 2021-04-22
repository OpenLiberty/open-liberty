/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.txsync.testlogic;

import java.io.Serializable;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * Tests common to all flavors of @PersistenceContext (Transaction Scoped and Extended Scoped)
 *
 * @author jgrassel
 *
 */
public class TxSynchronizationTestLogic extends AbstractTxSyncTestLogic {
    private final static Random rand = new Random();

    public TxSynchronizationTestLogic() {

    }

    /**
     * Test EntityManager.isJoinedToTransaction() contract with PersistenceContexts configured with
     * synchronization=SynchronizationType.SYNCHRONIZED. A SynchronizationType of SYNCHRONIZED
     * means the application does not need to invoke EntityManager.joinTransaction() for it
     * to enlist to the active transaction.
     *
     * Tests JPA 2.1 Specification Contract:
     *
     * 3.3.1 Synchronization with the Current Transaction
     * By default, a container-managed persistence context is of SynchronizationType.SYNCHRONIZED and is
     * automatically joined to the current transaction. A persistence context of SynchronizationType.UNSYNCHRONIZED
     * will not be enlisted in the current transaction, unless the EntityManager joinTransaction method is invoked.
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     *
     *                                   Points: 10
     */
    public void testIsJoinedToTransaction001(
                                             TestExecutionContext testExecCtx,
                                             TestExecutionResources testExecResources,
                                             Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testIsJoinedToTransaction001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }
        if (!isSynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Synchronized (is \"" +
                        getSynctype(testExecResources, "test-jpa-resource") +
                        "\" .)  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testIsJoinedToTransaction001(): Begin");

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            // Clean up any active transaction
            if (tj.isTransactionActive()) {
                System.out.println("Rolling back a pre-existing User Transaction...");
                tj.rollbackTransaction();
            }

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            System.out.println("Beginning a User Transaction...");
            tj.beginTransaction();

            Assert.assertTrue("Assert that there is an active transaction.", tj.isTransactionActive());
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            System.out.println("Committing a User Transaction...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            System.out.println("Beginning a User Transaction...");
            tj.beginTransaction();

            Assert.assertTrue("Assert that there is an active transaction.", tj.isTransactionActive());
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            System.out.println("Rolling back a User Transaction...");
            tj.rollbackTransaction();

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testIsJoinedToTransaction001(): End");
        }
    }

    /**
     * Test EntityManager.isJoinedToTransaction() contract with PersistenceContexts configured with
     * synchronization=SynchronizationType.UNSYNCHRONIZED. A SynchronizationType of UNSYNCHRONIZED
     * means the application does need to invoke EntityManager.joinTransaction() for it
     * to enlist to the active transaction.
     *
     * Tests JPA 2.1 Specification Contract:
     *
     * 3.3.1 Synchronization with the Current Transaction
     * By default, a container-managed persistence context is of SynchronizationType.SYNCHRONIZED and is
     * automatically joined to the current transaction. A persistence context of SynchronizationType.UNSYNCHRONIZED
     * will not be enlisted in the current transaction, unless the EntityManager joinTransaction method is invoked.
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     *
     *                                   Points: 12
     */
    public void testIsJoinedToTransaction002(
                                             TestExecutionContext testExecCtx,
                                             TestExecutionResources testExecResources,
                                             Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testIsJoinedToTransaction002: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }
        if (!isUnsynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Unsynchronized (is \"" +
                        getSynctype(testExecResources, "test-jpa-resource") +
                        "\" .)  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testIsJoinedToTransaction002(): Begin");

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            // Clean up any active transaction
            if (tj.isTransactionActive()) {
                System.out.println("Rolling back a pre-existing User Transaction...");
                tj.rollbackTransaction();
            }

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            System.out.println("Beginning a User Transaction...");
            tj.beginTransaction();

            Assert.assertTrue("Assert that there is an active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            System.out.println("Joining to User Transaction...");
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            System.out.println("Committing a User Transaction...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            System.out.println("Beginning a User Transaction...");
            tj.beginTransaction();

            Assert.assertTrue("Assert that there is an active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            System.out.println("Joining to User Transaction...");
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            System.out.println("Rolling back a User Transaction...");
            tj.rollbackTransaction();

            Assert.assertFalse("Assert that there is no active transaction.", tj.isTransactionActive());
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testIsJoinedToTransaction002(): End");
        }
    }

    /**
     * Simple CRUD Test #001
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=SYNCHRONIZED
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     *
     *                                   Points: 5
     */
    public void testCRUD001(
                            TestExecutionContext testExecCtx,
                            TestExecutionResources testExecResources,
                            Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCRUD001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }
        if (!isSynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Synchronized (is \"" +
                        getSynctype(testExecResources, "test-jpa-resource") +
                        "\" .)  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testCRUD001(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            jpaResource.getEm().clear();

            int identity = rand.nextInt();

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
            jpaResource.getTj().beginTransaction();

            System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
            jpaResource.getEm().persist(newEntity);

            Assert.assertTrue("Assert new entity is managed.", jpaResource.getEm().contains(newEntity));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 findEntity = jpaResource.getEm().find(SimpleVersionedEntity10.class, identity);

            Assert.assertNotNull("Assert find did not return null.", findEntity);
            if (findEntity == null)
                return;

            Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
            Assert.assertEquals("Assert the identity is " + identity, identity, findEntity.getId());
            Assert.assertEquals("Assert the String payload is correct.", "Simple String", findEntity.getStrData());

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testCRUD001(): End");
        }
    }

    /**
     * Simple CRUD Test #001B
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=UNSYNCHRONIZED
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     *
     *                                   Points: 5
     */
    public void testCRUD001B(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCRUD001B: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testCRUD001B(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            jpaResource.getEm().clear();

            int identity = rand.nextInt();

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            System.out.println("Beginning transaction ...");
            jpaResource.getTj().beginTransaction();

            System.out.println("Joining transaction...");
            jpaResource.getEm().joinTransaction();

            System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
            jpaResource.getEm().persist(newEntity);

            Assert.assertTrue("Assert new entity is managed.", jpaResource.getEm().contains(newEntity));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 findEntity = jpaResource.getEm().find(SimpleVersionedEntity10.class, identity);

            Assert.assertNotNull("Assert find did not return null.", findEntity);
            if (findEntity == null)
                return;

            Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
            Assert.assertEquals("Assert the identity is " + identity, identity, findEntity.getId());
            Assert.assertEquals("Assert the String payload is correct.", "Simple String", findEntity.getStrData());

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testCRUD001B(): End");
        }
    }

    /**
     * Simple CRUD Test #002
     *
     * Verify that CRUD operations are not persisted to the database with
     *
     * @PersistenceContext(transaction=UNSYNCHRONIZED) when em.joinTransaction is not invoked.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     * @param log
     *
     *                                   Points: 3
     */
    public void testCRUD002(
                            TestExecutionContext testExecCtx,
                            TestExecutionResources testExecResources,
                            Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCRUD002: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }
        if (!isUnsynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Unsynchronized.  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testCRUD002(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            jpaResource.getEm().clear();

            int identity = rand.nextInt();

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
            jpaResource.getTj().beginTransaction();

            System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
            try {
                // There is a JTA tran going, so persist() should operate even if it is not joined to the tx.
                jpaResource.getEm().persist(newEntity);

                Assert.assertTrue("Assert new entity is managed.", jpaResource.getEm().contains(newEntity));
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            }

            System.out.println("Committing transaction (nothing should be saved to DB) ...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Clearing persistence context ...");
            jpaResource.getEm().clear();

            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 findEntity = jpaResource.getEm().find(SimpleVersionedEntity10.class, identity);

            Assert.assertNull("Assert find did return null (data should not be saved to db.", findEntity);

            Assert.assertFalse("Assert new entity is not managed.", jpaResource.getEm().contains(newEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testCRUD002(): End");
        }
    }

    /**
     * Test Flush #001
     *
     * Verify that the em.flush() operation requires an active JTA transaction and that unsynchronized
     * entitymanagers must be joined to the transaction.
     *
     * Points: 8
     */
    public void testFlush001(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFlush001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }
        if (!isUnsynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Unsynchronized.  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFlush001(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            jpaResource.getEm().clear();

            int identity = rand.nextInt();

            // Part A (3 points)
            {
                System.out.println("Part A: Test em.flush() with an unsynchronized em with an active but unjoined JTA tran.");

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                newEntity.setId(identity);
                newEntity.setStrData("Simple String");

                Assert.assertFalse("Assert entitymanager is not joined to the JTA tran.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                try {
                    // There is a JTA tran going, so persist() should operate even if it is not joined to the tx.
                    jpaResource.getEm().persist(newEntity);

                    Assert.assertTrue("Assert new entity is managed.", jpaResource.getEm().contains(newEntity));
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Catch any Exceptions thrown by the test case for proper error logging.
                    Assert.fail("Caught an unexpected Exception during test execution." + t);
                }

                System.out.println("Invoking em.flush() (TransactionRequiredException should be thrown.");
                try {
                    jpaResource.getEm().flush();
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                System.out.println("Rolling back transaction ...");
                jpaResource.getTj().rollbackTransaction();

                jpaResource.getEm().clear();
            }

            // Part B (4 points)
            {
                System.out.println("Part B: Test em.flush() with an unsynchronized em with an active and joined JTA tran.");

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                newEntity.setId(identity);
                newEntity.setStrData("Simple String");

                Assert.assertFalse("Assert entitymanager is not joined to the JTA tran.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Beginning transaction (invoking em.joinTransaction()) ...");
                jpaResource.getTj().beginTransaction();
                jpaResource.getEm().joinTransaction();

                Assert.assertTrue("Assert entitymanager is joined to the JTA tran.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                try {
                    // There is a JTA tran going, so persist() should operate even if it is not joined to the tx.
                    jpaResource.getEm().persist(newEntity);

                    Assert.assertTrue("Assert new entity is managed.", jpaResource.getEm().contains(newEntity));
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Catch any Exceptions thrown by the test case for proper error logging.
                    Assert.fail("Caught an unexpected Exception during test execution." + t);
                }

                System.out.println("Invoking em.flush() (No Exception should be thrown.)");
                try {
                    jpaResource.getEm().flush();

                    System.out.println("Committing transaction ...");
                    jpaResource.getTj().commitTransaction();
                } catch (Exception e) {
                    jpaResource.getTj().rollbackTransaction();
                    Assert.fail("Caught an unexpected Exception during test execution." + e);
                }

                jpaResource.getEm().clear();
            }

            // Part C (1 point)
            {
                System.out.println("Part C: Verify that the entity was saved to the database.");

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = jpaResource.getEm().find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null (data should be saved to db.)", findEntity);

                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFlush001(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     *
     *
     * Points: 9 points for unsynchronized entity managers, 6 points for synchronized entity managers.
     */
    public void testFindWithLock001(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock001(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.NONE) does not require a JTA tran to work, regardless of
            // Tx synchronicity. (2 points)
            {
                System.out.println("Point B: Verify that em.find() with LockType.NONE does not require a JTA tran");

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType.NONE ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, LockModeType.NONE);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
//                if (isCMTS) {
//                    Assert.assertFalse("Assert entity was detached from persistence context.", jpaResource.getEm().contains(findEntity));
//                } else {
//                    Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));
//                }
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                em.clear();
            }

            // Verify that em.find(LockTypeMode.NONE) operates within the bounds of a JTA transaction, regardless of
            // Tx synchronicity (unsynchronized entity managers not joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType.NONE can run within a JTA tran boundary");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType.NONE ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, LockModeType.NONE);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));
                if (isSynchronized) {
                    Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                } else {
                    Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                }

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.NONE) with an unsynchronized entity manager operates
            // within the bounds of a JTA transaction when joined to the transaction (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType.NONE can run within a JTA tran boundary");

                System.out.println("Beginning transaction (invoking em.joinTransaction()) ...");
                tj.beginTransaction();
                em.joinTransaction();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType.NONE ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, LockModeType.NONE);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock001(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock002(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.OPTIMISTIC;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock002(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.OPTIMISTIC) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.OPTIMISTIC) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock002(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock003(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.OPTIMISTIC_FORCE_INCREMENT;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock003(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock003(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock004(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_FORCE_INCREMENT;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock004(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock004(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.find() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock005(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_READ;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock005(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.PESSIMISTIC_READ) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_READ) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_READ) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock005(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.find() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock006(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_WRITE;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock006(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.PESSIMISTIC_WRITE) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_WRITE) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.PESSIMISTIC_WRITE) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock006(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: READ
     *
     * Verify that em.find() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock007(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.READ;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock007(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.READ) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.READ) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.READ) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock007(): End");
        }
    }

    /**
     * Test em.find() with Lock Type: WRITE
     *
     * Verify that em.find() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testFindWithLock008(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testFindWithLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.WRITE;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock008(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.find(LockTypeMode.WRITE) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.find() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX and LockType." + lmType + " ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.find(LockTypeMode.WRITE) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.find() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX and LockType." + lmType + " ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.find(LockTypeMode.WRITE) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.find() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testFindWithLock008(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: NONE
     *
     * Verify that em.lock() with lock type NONE requires an active JTA transaction and that the
     * EntityManager is joined to the transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock001(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.NONE;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock001(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.NONE) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.NONE) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.NONE) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock001(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC
     *
     * Verify that em.lock() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock002(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.OPTIMISTIC;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock002(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.OPTIMISTIC) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.OPTIMISTIC) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.OPTIMISTIC) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock002(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock003(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.OPTIMISTIC_FORCE_INCREMENT;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock003(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.OPTIMISTIC_FORCE_INCREMENT) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock003(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock004(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_FORCE_INCREMENT;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock004(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_FORCE_INCREMENT) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock004(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.lock() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock005(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_READ;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock005(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_READ) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_READ) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_READ) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock005(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.lock() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock006(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.PESSIMISTIC_WRITE;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock006(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_WRITE) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_WRITE) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.PESSIMISTIC_WRITE) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock006(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: READ
     *
     * Verify that em.lock() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock007(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.READ;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock007(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.READ) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.READ) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.READ) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock007(): End");
        }
    }

    /**
     * Test em.lock() with Lock Type: WRITE
     *
     * Verify that em.lock() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 5 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    public void testEmLock008(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testEmLock001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        boolean isCMTS = isCMTS(testExecResources, "test-jpa-resource");
        boolean isSynchronized = isSynchronized(testExecResources, "test-jpa-resource");

        LockModeType lmType = LockModeType.WRITE;

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testEmLock008(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate Database");
            TestWorkRequest twrPopulate = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                        newEntity.setId(identity);
                        newEntity.setStrData("Simple String");

                        em.persist(newEntity);
                        Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork(twrPopulate) on " + managedComponentObject + " ...");
                    }

                    return null;
                }

            };
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            jpaResource.getEm().clear();

            // Verify that em.lock(LockTypeMode.WRITE) does require a JTA tran to work, regardless of
            // Tx synchronicity. (1 point for synchronized, 2 points for unsynchronized)
            {
                System.out.println("Point B: Verify that em.lock() with LockType." + lmType + " does require a JTA tran");

                if (!isSynchronized) {
                    Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());
                }
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                try {
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.WRITE) operates within the bounds of a JTA transaction,
            // (unsynchronized entity managers are joined to the tx). (3 points)
            {
                System.out.println("Point C: Verify that em.lock() with LockType." + lmType + " can run within a JTA tran boundary");

                System.out.println("Beginning transaction ...");
                tj.beginTransaction();

                if (!isSynchronized) {
                    System.out.println("Joining transaction ...");
                    em.joinTransaction();
                }
                Assert.assertTrue("Assert that the em is joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", jpaResource.getEm().contains(findEntity));

                System.out.println("Invoking em.lock() with LockType." + lmType + " ...");
                em.lock(findEntity, lmType);

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            // Verify that em.lock(LockTypeMode.WRITE) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate (3 points, unsynch em only)
            if (!isSynchronized) {
                System.out.println("Point D: Verify that unsynchronized (tx joined) em.lock() with LockType." +
                                   lmType + " can run within a JTA tran boundary but must also be joined to the JTA tran.");

                System.out.println("Beginning transaction (not invoking em.joinTransaction()) ...");
                tj.beginTransaction();

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    em.lock(findEntity, lmType);
                    Assert.fail("No Exception was thrown.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }

                Assert.assertFalse("Assert that the em is not joined to the transaction.", em.isJoinedToTransaction());

                System.out.println("Rolling back transaction ...");
                tj.rollbackTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testEmLock008(): End");
        }
    }

    public void testTemplate(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("TxSynchronizationTestLogic.testTemplate(): Begin");
//            cleanupDatabase(jpaCleanupResource);

            System.out.println("Ending test.");
        } finally {
            System.out.println("TxSynchronizationTestLogic.testTemplate(): End");
        }
    }
}
