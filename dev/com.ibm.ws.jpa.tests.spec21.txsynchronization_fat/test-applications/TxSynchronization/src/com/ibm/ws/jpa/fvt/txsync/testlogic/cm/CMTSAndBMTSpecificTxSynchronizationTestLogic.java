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

package com.ibm.ws.jpa.fvt.txsync.testlogic.cm;

import java.io.Serializable;
import java.util.HashMap;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.TransactionRequiredException;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFEXSyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFEXUnsyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.testlogic.AbstractTxSyncTestLogic;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/*
 * JPA 2.1 Specification Contracts of Interest:
 *

 */
/**
 * Tests examining CMTS-Specific Transaction Synchronicity using Bean Managed Transactions (UserTransactions).
 *
 *
 */
public class CMTSAndBMTSpecificTxSynchronizationTestLogic extends AbstractTxSyncTestLogic {
/*
 * JPA Spec 2.1: 7.6.1 Persistence Context Synchronization Type
 *
 * By default, a container-managed persistence context is of type SynchronizationType.SYNCHRONIZED. Such a persistence
 * context is automatically joined to the current JTA transaction, and updates made to the persistence context are
 * propagated to the underlying resource manager.
 *
 * A container-managed persistence context may be specified to be of type SynchronizationType.UNSYNCHRONIZED. A
 * persistence context of type SynchronizationType.UNSYNCHRONIZED is not enlisted in any JTA transaction unless
 * explicitly joined to that transaction by the application. A persistence context of type
 * SynchronizationType.UNSYNCHRONIZED is enlisted in a JTA transaction and registered for subsequent transaction
 * notifications against that transaction by the invocation of the EntityManager joinTransaction method. The
 * persistence context remains joined to the transaction until the transaction commits or rolls back. After the
 * transaction commits or rolls back, the persistence context will not be joined to any subsequent transaction
 * unless the joinTransaction method is invoked in the scope of that subsequent transaction.
 *
 * A persistence context of type SynchronizationType.UNSYNCHRONIZED must not be flushed to the database unless it
 * is joined to a transaction. The application's use of queries with pessimistic locks, bulk update or delete queries,
 * etc. result in the provider throwing the TransactionRequiredException. After the persistence context has been
 * joined to the JTA transaction, these operations are again allowed.
 *
 * The application is permitted to invoke the persist, merge, remove, and refresh entity lifecycle operations on an
 * entity manager of type SynchronizationType.UNSYNCHRONIZED independent of whether the persistence context is joined
 * to the current transaction. After the persistence context has been joined to a transaction, changes in a
 * persistence context can be flushed to the database either explicitly by the application or by the provider. If the
 * flush method is not explicitly invoked, the persistence provider may defer flushing until commit time depending
 * on the operations invoked and the flush mode setting in effect.
 *
 * If an extended persistence context of type SynchronizationType.UNSYNCHRONIZED has not been joined to the current
 * JTA transaction, rollback of the JTA transaction will have no effect upon the persistence context. In general, it
 * is recommended that a non-JTA datasource be specified for use by the persistence provider for a persistence context
 * of type SynchronizationType.UNSYNCHRONIZED that has not been joined to a JTA transaction in order to alleviate
 * the risk of integrating uncommitted changes into the persistence context in the event that the transaction is
 * later rolled back.
 *
 * If a persistence context of type SynchronizationType.UNSYNCHRONIZED has been joined to the JTA transaction,
 * transaction rollback will cause the persistence context to be cleared and all pre-existing managed and removed
 * instances to become detached. (See section 3.3.3.)
 *
 * When a JTA transaction exists, a persistence context of type SynchronizationType.UNSYNCHRONIZED is propagated
 * with that transaction according to the rules in section 7.6.4.1 regardless of whether the persistence context has
 * been joined to that transaction.
 *
 *
 * JPA Spec 2.1: 7.6.2 Container-managed Transaction-scoped Persistence Context
 *
 * The application can obtain a container-managed entity manager with transaction-scoped persistence context
 * by injection or direct lookup in the JNDI namespace. The persistence context type for the entity manager
 * is defaulted or defined as PersistenceContextType.TRANSACTION.
 *
 * A new persistence context begins when the container-managed entity manager is invoked[82] in the scope of
 * an active JTA transaction, and there is no current persistence context already associated with the JTA transaction.
 * The persistence context is created and then associated with the JTA transaction. This association of the
 * persistence context with the JTA transaction is independent of the synchronization type of the persistence
 * context and whether the persistence context has been joined to the transaction.
 *
 * The persistence context ends when the associated JTA transaction commits or rolls back, and all entities that
 * were managed by the EntityManager become detached.[83]
 *
 * If the entity manager is invoked outside the scope of a transaction, any entities loaded from the database will
 * immediately become detached at the end of the method call.
 *
 * [82] Specifically, when one of the methods of the EntityManager interface is invoked.
 * [83] Note that this applies to a transaction-scoped persistence context of type
 * SynchronizationType.UNSYNCHRONIZED that has not been joined to the transaction as well.
 *
 *
 * JPA Spec 2.1: 7.6.4.1 Requirements for Persistence Context Propagation
 *
 * Persistence contexts are propagated by the container across component invocations as follows.
 *
 * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
 * context is not propagated.
 * * If an entity manager is then invoked from within the component:
 * * Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will result in use of a
 * new persistence context (as described in section 7.6.2).
 * * Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will result in the use of
 * the existing extended persistence context bound to that component.
 * * If the entity manager is invoked within a JTA transaction, the persistence context will be associated
 * with the JTA transaction.
 *
 * If a component is called and the JTA transaction is propagated into that component:
 * * If the component is a stateful session bean to which an extended persistence context has been bound and
 * there is a different persistence context associated with the JTA transaction, an EJBException is thrown by
 * the container.
 * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
 * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
 * the IllegalStateException is thrown by the container.
 * * Otherwise, if there is a persistence context associated with the JTA transaction, that persistence context is
 * propagated and used.
 *
 * Note that a component with a persistence context of type Synchronization- Type.UNSYNCHRONIZED may be called by a
 * component propagating either a persistence context of type SynchronizationType.UNSYNCHRONIZED or a persistence
 * context of type SynchronizationType.SYNCHRONIZED into it.
 *
 * [84] Entitymanager instances obtained from different entitymanagerfactories never share the same persistence context.
 */

    private final static String testBucketName = CMTSAndBMTSpecificTxSynchronizationTestLogic.class.getName();

    /**
     * (7.6.2)
     * A new persistence context begins when the container-managed entity manager is invoked[82] in the scope of
     * an active JTA transaction, and there is no current persistence context already associated with the JTA transaction.
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * The persistence context ends when the associated JTA transaction commits or rolls back, and all entities that
     * were managed by the EntityManager become detached.[83]
     *
     * If the entity manager is invoked outside the scope of a transaction, any entities loaded from the database will
     * immediately become detached at the end of the method call.
     *
     *
     * Verify that a new persistence context begins when the EntityManager is invoked within the scope of a
     * JTA transaction, and ends when that JTA transaction commits.
     *
     * Test will join an UNSYNCHRONIZED EntityManager to the JTA transaction.
     *
     * Tests: Both SYNCHRONIZED and UNSYNCHRONIZED CMTS EntityManagers
     *
     * Points: 7
     */
    public void testCMTSEntityManagerScope001(
                                              TestExecutionContext testExecCtx,
                                              TestExecutionResources testExecResources,
                                              Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSEntityManagerScope001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
////            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();
            SimpleVersionedEntity10 newEntity = null;

            // Begin a JTA tran, join the em to the tran if the EntityManager is unsynchronized,
            // create and persist a new SimpleVersionedEntity10, and verify that em.find() returns
            // the same instance of SimpleVersionedEntity10 -- this will verify that the same
            // persistence context is being used throughout the JTA transaction. (6 points)
            {
                System.out.println("Point A: Verify that the same persistence context is utilized through a JTA tran lifespan.");

                // Begin the Transaction
                System.out.println("Beginning Transaction...");
                jpaResource.getTj().beginTransaction();

                if (isUnsynchronised) {
                    System.out.println("Joining Transaction...");
                    jpaResource.getEm().joinTransaction();
                }
                Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                newEntity = new SimpleVersionedEntity10(identity);
                newEntity.setStrData("Simple String");

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                jpaResource.getEm().persist(newEntity);

                Assert.assertTrue("Assert entity is managed by persistence context.", em.contains(newEntity));

                // Validate that the entity persisted is the same returned by a find operation.
                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertSame("Assert that find did return the same entity.", newEntity, findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction...");
                jpaResource.getTj().commitTransaction();

                // Verify that entities are detached once the JTA transaction, and thus the persistence context, ends.
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertFalse("Assert entity was detached from persistence context.", jpaResource.getEm().contains(newEntity));
                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * (7.6.2)
     * A new persistence context begins when the container-managed entity manager is invoked[82] in the scope of
     * an active JTA transaction, and there is no current persistence context already associated with the JTA transaction.
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * The persistence context ends when the associated JTA transaction commits or rolls back, and all entities that
     * were managed by the EntityManager become detached.[83]
     *
     * If the entity manager is invoked outside the scope of a transaction, any entities loaded from the database will
     * immediately become detached at the end of the method call.
     *
     *
     * Verify that entities are detached from a persistence context when the JTA tran commits or rolls back.
     *
     * Tests: Both SYNCHRONIZED and UNSYNCHRONIZED CMTS EntityManagers
     *
     * Points: 14
     */
    public void testCMTSEntityManagerScope002(
                                              TestExecutionContext testExecCtx,
                                              TestExecutionResources testExecResources,
                                              Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSEntityManagerScope002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A: Populate the Database");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            final SimpleVersionedEntity10 newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate,
                                                                                                                                   TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            em.clear();

            // Verify that entities loaded by a CMTS EntityManager during a JTA transaction are detached
            // when the transaction commits. (6 points)
            {
                System.out.println("Point B: Verify that entities loaded by a CMTS EntityManager during a JTA transaction " +
                                   "are detached when the transaction commits.");

                // Begin the Transaction
                System.out.println("Beginning Transaction...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Joining Transaction...");
                        em.joinTransaction();

                        Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that the em has NOT joined the transaction.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", em.contains(findEntity));

                // Commit the Transaction (should cause everything to become detached)
                System.out.println("Committing the User Transaction...");
                tj.commitTransaction();

                Assert.assertFalse("Assert entity was detached from persistence context.", em.contains(findEntity));
                Assert.assertFalse("Assert that the em is not joined with a transaction.", em.isJoinedToTransaction());
            }

            // Verify that entities loaded by a CMTS EntityManager during a JTA transaction are detached
            // when the transaction rolls back. (6 points)
            {
                System.out.println("Point C: Verify that entities loaded by a CMTS EntityManager during a JTA transaction " +
                                   "are detached when the transaction rolls back.");

                // Begin the Transaction
                System.out.println("Beginning Transaction...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Joining Transaction...");
                        em.joinTransaction();

                        Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that the em has NOT joined the transaction.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
                Assert.assertTrue("Assert entity was not detached from persistence context.", em.contains(findEntity));

                // Rollback the Transaction (should cause everything to become detached)
                System.out.println("Rolling back the User Transaction...");
                tj.rollbackTransaction();

                Assert.assertFalse("Assert entity was detached from persistence context.", em.contains(findEntity));
                Assert.assertFalse("Assert that the em is not joined with a transaction.", em.isJoinedToTransaction());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.persist()
     *
     * Verifies: That an UNSYNCHRONIZED CMTS EntityManager will throw a TransactionRequiredException when persist()
     * is invoked outside of the scope of a JTA transaction.
     *
     * Points: 2
     */
    public void testCMTSPersistContract001(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSPersistContract001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Verify that the em.persist() contract will still throw a TransactionRequiredException when
            // a JTA transaction is not active. (1 point)
            try {
                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                newEntity.setId(identity);
                newEntity.setStrData("Simple String");

                System.out.println("Calling em.persist() on the new entity (should throw TransactionRequiredException) ...");
                em.persist(newEntity);
                Assert.fail("No Exception was thrown by em.persist() being called outside of a JTA transaction boundary.");
            } catch (Exception e) {
                assertExceptionIsInChain(TransactionRequiredException.class, e);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.persist()
     *
     * Verifies: That an UNSYNCHRONIZED CMTS EntityManager will not throw a TransactionRequiredException when
     * invoked within the scope of an active but unjoined JTA transaction, but the new entity will
     * also not be saved to the database after tx commit.
     *
     * Points: 10 points
     */
    public void testCMTSPersistContract002(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSPersistContract002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            SimpleVersionedEntity10 newEntity = null;

            // Exercise em.persist() within the bounds of a unjoined JTA transaction (6 points)
            {
                System.out.println("Point A: Exercise em.persist() within the bounds of a unjoined JTA transaction ...");

                // Begin the Transaction
                System.out.println("Beginning Transaction (not joining the EntityManager to the transaction) ...");
                tj.beginTransaction();
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                newEntity = new SimpleVersionedEntity10(identity);
                newEntity.setStrData("Simple String");

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") (should not throw Exception) ...");
                em.persist(newEntity);

                Assert.assertTrue("Assert entity is managed by persistence context.", em.contains(newEntity));
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertSame("Assert that find did return the same entity.", newEntity, findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                tj.commitTransaction();

                Assert.assertFalse("Assert entity has become detached.", em.contains(newEntity));
                em.clear();
            }

            // Verify that the entity was not saved to the database (3 points)
            {
                System.out.println("Point B: Verify that the entity was not saved to the database.");
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNull("Assert find did return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.persist()
     *
     * Verifies: That an UNSYNCHRONIZED CMTS EntityManager persist() operation will save to the database if
     * the EntityManager joins the JTA tran before the persist().
     *
     * Points: 12 points
     */
    public void testCMTSPersistContract003(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSPersistContract003";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            SimpleVersionedEntity10 newEntity = null;

            // Exercise em.persist() within the bounds of a unjoined JTA transaction (7 points)
            {
                System.out.println("Point A: Exercise em.persist() within the bounds of a unjoined JTA transaction ...");

                // Begin the Transaction
                System.out.println("Beginning Transaction (not joining the EntityManager to the transaction) ...");
                tj.beginTransaction();
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Joining transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                newEntity = new SimpleVersionedEntity10(identity);
                newEntity.setStrData("Simple String");

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") (should not throw Exception) ...");
                em.persist(newEntity);

                Assert.assertTrue("Assert entity is managed by persistence context.", em.contains(newEntity));
                Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertSame("Assert that find did return the same entity.", newEntity, findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                tj.commitTransaction();

                Assert.assertFalse("Assert entity has become detached.", em.contains(newEntity));
                em.clear();
            }

            // Verify that the entity was not saved to the database (4 points)
            {
                System.out.println("Point B: Verify that the entity was not saved to the database.");
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.persist()
     *
     * Verifies: That an UNSYNCHRONIZED CMTS EntityManager persist() operation will save to the database if
     * the EntityManager joins the JTA tran after the persist().
     *
     * Points: 12 points
     */
    public void testCMTSPersistContract004(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSPersistContract004";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            SimpleVersionedEntity10 newEntity = null;

            // Exercise em.persist() within the bounds of a unjoined JTA transaction (7 points)
            {
                System.out.println("Point A: Exercise em.persist() within the bounds of a unjoined JTA transaction ...");

                // Begin the Transaction
                System.out.println("Beginning Transaction (not joining the EntityManager to the transaction) ...");
                tj.beginTransaction();
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
                newEntity = new SimpleVersionedEntity10(identity);
                newEntity.setStrData("Simple String");

                System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") (should not throw Exception) ...");
                em.persist(newEntity);

                Assert.assertTrue("Assert entity is managed by persistence context.", em.contains(newEntity));
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertSame("Assert that find did return the same entity.", newEntity, findEntity);

                System.out.println("Joining transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em has joined the transaction.", em.isJoinedToTransaction());

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                tj.commitTransaction();

                Assert.assertFalse("Assert entity has become detached.", em.contains(newEntity));
                em.clear();
            }

            // Verify that the entity was not saved to the database (4 points)
            {
                System.out.println("Point B: Verify that the entity was not saved to the database.");
                Assert.assertFalse("Assert that the em has not joined the transaction.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") with no active TX ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertNotSame("Assert that find did not return the same entity.", newEntity, findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.merge()
     * em.merge() invoked before em.joinTransaction()
     *
     * Points: 33
     */
    public void testCMTSMergeContract001(
                                         TestExecutionContext testExecCtx,
                                         TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSMergeContract001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A:");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            // Verify that em.merge() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does not save the alteration to the database.
            // (15 points)
            {
                System.out.println("Point B:");

                // Fetch the entity and detach
                System.out.println("Point B-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Merge within a JTA tran but do not join the JTA tran (changes should not be saved)
                System.out.println("Point B-2:");

                System.out.println("Mutating the entity ...");
                findEntity.setStrData("Mutatated String");

                // Begin the Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Invoking em.merge() (expecting no Exception given active JTA transaction) ...");
                SimpleVersionedEntity10 mergedEntity = em.merge(findEntity);

                Assert.assertNotNull("Assert that the merge operation did not return null.", mergedEntity);
                Assert.assertNotSame("Assert that the merge operation did not return the same object.", findEntity, mergedEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the merged entity is managed.", em.contains(mergedEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction (merge should not be saved) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(mergedEntity));
                em.clear();

                // Verify that the change was not committed to the database
                System.out.println("Point B-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity2);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity2));
                Assert.assertEquals("Assert that the entity's strData is unchanged.", "Simple String", findEntity2.getStrData());

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            // Verify that em.merge() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does save the alteration to the database
            // if it joins the transaction before the commit. (16 points)
            {
                System.out.println("Point C:");

                // Fetch the entity and detach
                System.out.println("Point C-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Merge within a JTA tran but do not join the JTA tran
                System.out.println("Point C-2:");

                System.out.println("Mutating the entity ...");
                findEntity.setStrData("Mutatated String");

                // Begin the Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Invoking em.merge() (expecting no Exception given active JTA transaction) ...");
                SimpleVersionedEntity10 mergedEntity = em.merge(findEntity);

                Assert.assertNotNull("Assert that the merge operation did not return null.", mergedEntity);
                Assert.assertNotSame("Assert that the merge operation did not return the same object.", findEntity, mergedEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the merged entity is managed.", em.contains(mergedEntity));

                System.out.println("Joining the transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                // Commit the Transaction
                System.out.println("Committing a User Transaction (merge should be saved) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(mergedEntity));
                em.clear();

                // Verify that the change was not committed to the database
                System.out.println("Point C-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity2);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity2));
                Assert.assertEquals("Assert that the entity's strData is changed.", "Mutatated String", findEntity2.getStrData());

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.merge()
     * em.merge() invoked after em.joinTransaction()
     *
     * Points: 33
     */
    public void testCMTSMergeContract002(
                                         TestExecutionContext testExecCtx,
                                         TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSMergeContract002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A:");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            // Verify that em.merge() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does not save the alteration to the database.
            // (15 points)
            {
                System.out.println("Point B:");

                // Fetch the entity and detach
                System.out.println("Point B-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Merge within a JTA tran but do not join the JTA tran (changes should not be saved)
                System.out.println("Point B-2:");

                System.out.println("Mutating the entity ...");
                findEntity.setStrData("Mutatated String");

                // Begin the Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Invoking em.merge() (expecting no Exception given active JTA transaction) ...");
                SimpleVersionedEntity10 mergedEntity = em.merge(findEntity);

                Assert.assertNotNull("Assert that the merge operation did not return null.", mergedEntity);
                Assert.assertNotSame("Assert that the merge operation did not return the same object.", findEntity, mergedEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the merged entity is managed.", em.contains(mergedEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction (merge should not be saved) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(mergedEntity));
                em.clear();

                // Verify that the change was not committed to the database
                System.out.println("Point B-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity2);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity2));
                Assert.assertEquals("Assert that the entity's strData is unchanged.", "Simple String", findEntity2.getStrData());

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            // Verify that em.merge() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does save the alteration to the database
            // if it joins the transaction before the commit. (16 points)
            {
                System.out.println("Point C:");

                // Fetch the entity and detach
                System.out.println("Point C-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Merge within a JTA tran but do not join the JTA tran
                System.out.println("Point C-2:");

                System.out.println("Mutating the entity ...");
                findEntity.setStrData("Mutatated String");

                // Begin the Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Joining the transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Invoking em.merge() (expecting no Exception given active JTA transaction) ...");
                SimpleVersionedEntity10 mergedEntity = em.merge(findEntity);

                Assert.assertNotNull("Assert that the merge operation did not return null.", mergedEntity);
                Assert.assertNotSame("Assert that the merge operation did not return the same object.", findEntity, mergedEntity);
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the merged entity is managed.", em.contains(mergedEntity));

                // Commit the Transaction
                System.out.println("Committing a User Transaction (merge should be saved) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(mergedEntity));
                em.clear();

                // Verify that the change was not committed to the database
                System.out.println("Point C-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity2);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity2));
                Assert.assertEquals("Assert that the entity's strData is changed.", "Mutatated String", findEntity2.getStrData());

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.remove()
     * em.remove() invoked before em.joinTransaction()
     *
     * Points: 17
     */
    public void testCMTSRemoveContract001(
                                          TestExecutionContext testExecCtx,
                                          TestExecutionResources testExecResources,
                                          Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSRemoveContract001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A:");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            // Verify that em.remove() must be called in a JTA transaction boundary, regardless of
            // its synchronicity. (2 points)
            {
                em.clear();
                System.out.println("Point A-1:");

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);

                try {
                    System.out.println("Invoking em.remove() (should throw TransactionRequiredException) ...");
                    em.remove(findEntity);
                    Assert.fail("The em.remove() call outside of a JTA Tran boundary did not throw an Exception.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }
                em.clear();
            }

            // Verify that em.remove() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does not save the alteration to the database.
            // (6 points)
            {
                System.out.println("Point B:");
                System.out.println("Point B-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                System.out.println("Calling em.remove() on the entity returned by find() (should not throw Exception) ...");
                em.remove(findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction (the removal should not be saved to db) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Verify that the entity was not removed from the database
                System.out.println("Point B-2:");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                em.clear();
            }

            // Verify that em.remove() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does save the alteration to the database
            // if the entitymanager is joined to the transaction after the em.remove() call..
            // (7 points)
            {
                System.out.println("Point C:");
                System.out.println("Point C-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Issue the em.remove() on the entity again, but this time join the transaction after issuing the
                // remove operation.
                System.out.println("Point C-2:");

                System.out.println("Calling em.remove() on the entity returned by find() (should not throw Exception) ...");
                em.remove(findEntity);

                System.out.println("Joining the transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                // Commit the Transaction
                System.out.println("Committing a User Transaction (remove should be saved to db) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Verify that the change was committed to the database
                System.out.println("Point C-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNull("Assert find did return null (entity no longer exists.)", findEntity2);

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.remove()
     * em.remove() invoked after em.joinTransaction()
     *
     * Points: 17
     */
    public void testCMTSRemoveContract002(
                                          TestExecutionContext testExecCtx,
                                          TestExecutionResources testExecResources,
                                          Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSRemoveContract002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A:");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            // Verify that em.remove() must be called in a JTA transaction boundary, regardless of
            // its synchronicity. (2 points)
            {
                em.clear();
                System.out.println("Point A-1:");

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);

                try {
                    System.out.println("Invoking em.remove() (should throw TransactionRequiredException) ...");
                    em.remove(findEntity);
                    Assert.fail("The em.remove() call outside of a JTA Tran boundary did not throw an Exception.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }
                em.clear();
            }

            // Verify that em.remove() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does not save the alteration to the database.
            // (6 points)
            {
                System.out.println("Point B:");
                System.out.println("Point B-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                System.out.println("Calling em.remove() on the entity returned by find() (should not throw Exception) ...");
                em.remove(findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction (the removal should not be saved to db) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Verify that the entity was not removed from the database
                System.out.println("Point B-2:");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                em.clear();
            }

            // Verify that em.remove() within a JTA tran boundary but not joined to the JTA tran will
            // not throw a TransactionRequiredException but does save the alteration to the database
            // if the entitymanager is joined to the transaction before the em.remove() call..
            // (7 points)
            {
                System.out.println("Point C:");
                System.out.println("Point C-1:");

                // Begin Transaction
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the em is not joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Issue the em.remove() on the entity again, but this time join the transaction after issuing the
                // remove operation.
                System.out.println("Point C-2:");

                System.out.println("Joining the transaction ...");
                em.joinTransaction();
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Calling em.remove() on the entity returned by find() (should not throw Exception) ...");
                em.remove(findEntity);

                // Commit the Transaction
                System.out.println("Committing a User Transaction (remove should be saved to db) ...");
                jpaResource.getTj().commitTransaction();

                Assert.assertFalse("Assert that the entity is no longer managed.", em.contains(findEntity));
                em.clear();

                // Verify that the change was committed to the database
                System.out.println("Point C-3:");
                System.out.println("Beginning Transaction (em not joining the transaction) ...");
                jpaResource.getTj().beginTransaction();

                Assert.assertFalse("Assert that the em has not joined the transaction.", jpaResource.getEm().isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNull("Assert find did return null (entity no longer exists.)", findEntity2);

                // Commit the Transaction
                System.out.println("Committing a User Transaction ...");
                jpaResource.getTj().commitTransaction();

                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction. (7.6.2)
     *
     * Test Variant: EntityManager.refresh()
     * em.refresh() invoked without an active jta tran will throw TransactionRequiredException
     *
     * Points: 10
     */
    public void testCMTSRefreshContract001(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testCMTSRefreshContract001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testBucketName + "." + testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
            boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            final int identity = rand.nextInt();

            // Populate the database (1 point)
            System.out.println("Point A:");
            TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            // Verify that em.refresh() without a jta tran throws a TransactionRequiredException (2 points)
            {
                em.clear();
                System.out.println("Point A: Verify that em.refresh() without a jta tran throws a TransactionRequiredException");

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);

                // Dirty the Data
                findEntity.setStrData("Mutate Data");

                try {
                    System.out.println("Invoking em.refresh() (should throw TransactionRequiredException) ...");
                    em.refresh(findEntity);
                    Assert.fail("The em.refresh() call outside of a JTA Tran boundary did not throw an Exception.");
                } catch (Exception e) {
                    assertExceptionIsInChain(TransactionRequiredException.class, e);
                }
                em.clear();
            }

            // Verify that em.refresh() with an joined jta tran works (6 points)
            {
                em.clear();
                System.out.println("Point C: Verify that em.refresh() with a joined jta tran works.");

                // Begin Transaction
                System.out.println("Beginning Transaction ...");
                jpaResource.getTj().beginTransaction();

                System.out.println("Joining Transaction ...");
                em.joinTransaction();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert that the em is joined with a transaction.", jpaResource.getEm().isJoinedToTransaction());
                Assert.assertTrue("Assert that the entity is managed.", em.contains(findEntity));

                // Dirty the Data
                findEntity.setStrData("Mutate Data");
                Assert.assertEquals("Assert that the entity was mutated.", "Mutate Data", findEntity.getStrData());

                try {
                    System.out.println("Invoking em.refresh() (should throw TransactionRequiredException) ...");
                    em.refresh(findEntity);
                    Assert.assertEquals("Assert that the data mutation was undone.", "Simple String", findEntity.getStrData());
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Catch any Exceptions thrown by the test case for proper error logging.
                    Assert.fail("Caught an unexpected Exception during test execution." + t);
                } finally {
                    System.out.println("Rolling Back Transaction ...");
                    jpaResource.getTj().rollbackTransaction();
                }
                em.clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

/*
 * JPA Spec 2.1: 7.6.4.1 Requirements for Persistence Context Propagation
 *
 * Persistence contexts are propagated by the container across component invocations as follows.
 *
 * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
 * context is not propagated.
 * * If an entity manager is then invoked from within the component:
 * * Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will result in use of a
 * new persistence context (as described in section 7.6.2).
 * * Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will result in the use of
 * the existing extended persistence context bound to that component.
 * * If the entity manager is invoked within a JTA transaction, the persistence context will be associated
 * with the JTA transaction.
 *
 * If a component is called and the JTA transaction is propagated into that component:
 * * If the component is a stateful session bean to which an extended persistence context has been bound and
 * there is a different persistence context associated with the JTA transaction, an EJBException is thrown by
 * the container.
 * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
 * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
 * the IllegalStateException is thrown by the container.
 * * Otherwise, if there is a persistence context associated with the JTA transaction, that persistence context is
 * propagated and used.
 *
 * Note that a component with a persistence context of type Synchronization- Type.UNSYNCHRONIZED may be called by a
 * component propagating either a persistence context of type SynchronizationType.UNSYNCHRONIZED or a persistence
 * context of type SynchronizationType.SYNCHRONIZED into it.
 *
 * [84] Entitymanager instances obtained from different entitymanagerfactories never share the same persistence context.
 */

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation001(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxNever(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL BMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation001A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation001A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSBMTSLBuddy().doWorkRequest(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation001B(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation001B";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSFBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxNever(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF BMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation001C(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation001C";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSBMTSFBuddy().doWorkRequest(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 do not use the same transaction synchronicity.
     * Second Bean is SL CMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation002(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_SYNCHRONIZED : TargetEntityManager.TXSYNC1_UNSYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxNever(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMTS #1 loads an entity (and is still managed because of LTC), and invokes a second bean that
     * injects a CMTS #2 with the same EntityManagerFactory. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 do not use the same transaction synchronicity.
     * Second Bean is SL BMT
     *
     * 8 points
     */
    public void testPropagationNoPropagation002A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation002A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity without an active JTA transaction (3 points)
            {
                System.out.println("Point B: Load the test entity without an active JTA transaction");
                em.clear();

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
            }

            // Invoke Buddy BMT and instruct it to fetch the same entity.  Should be a different persistence context
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy BMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_SYNCHRONIZED : TargetEntityManager.TXSYNC1_UNSYNCHRONIZED;

                getCMTSBMTSLBuddy().doWorkRequest(twr, tEmType);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NOT_SUPPORTED to suspend JTA tran)
     *
     * Points: 9
     */
    public void testPropagationNoPropagation003(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation003";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (uses transaction NOT_SUPPORTED to suspend JTA tran)
     *
     * Points: 9
     */
    public void testPropagationNoPropagation003A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation003A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (3 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSFBuddy().doWorkRequestWithTxNotSupported(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRES_NEW to suspend JTA tran)
     *
     * Points: 10
     */
    public void testPropagationNoPropagation003B(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation003B";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final boolean ejb2_isUnsynchronised = isUnsynchronised;
                final boolean ejb2_emShouldJoinTx = emShouldJoinTx;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            if (ejb2_isUnsynchronised) {
                                if (ejb2_emShouldJoinTx) {
                                    System.out.println("Unsynchronized EntityManager joining transaction ...");
                                    em.joinTransaction();
                                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                                } else {
                                    Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                                }
                            } else {
                                Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                            }

                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            if (!ejb2_isUnsynchronised || ejb2_emShouldJoinTx)
                                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                            else
                                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (uses transaction REQUIRES_NEW to suspend JTA tran)
     *
     * Points: 10
     */
    public void testPropagationNoPropagation003C(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation003C";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final boolean ejb2_isUnsynchronised = isUnsynchronised;
                final boolean ejb2_emShouldJoinTx = emShouldJoinTx;

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            if (ejb2_isUnsynchronised) {
                                if (ejb2_emShouldJoinTx) {
                                    System.out.println("Unsynchronized EntityManager joining transaction ...");
                                    em.joinTransaction();
                                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                                } else {
                                    Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                                }
                            } else {
                                Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                            }

                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            if (!ejb2_isUnsynchronised || ejb2_emShouldJoinTx)
                                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                            else
                                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSFBuddy().doWorkRequestWithTxRequiresNew(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL BMT (invoking second BMT should suspend first BMT's JTA tran)
     *
     * Points: 10
     */
    public void testPropagationNoPropagation004(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation004";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final Object delegate = em.getDelegate();

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);

                            Assert.assertNotSame("Assert that the delegate EntityManager used in components 1 and 2 are not the same",
                                                 delegate,
                                                 em.getDelegate());
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSBMTSLBuddy().doWorkRequest(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and there is no JTA transaction or the JTA transaction is not propagated, the persistence
     * context is not propagated.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF BMT (invoking second BMT should suspend first BMT's JTA tran)
     *
     * Points: 10
     */
    public void testPropagationNoPropagation004A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagationNoPropagation004A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be a different persistence context
            // because the buddy CMT suspends the transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be a different persistence context since there is no propagation, " +
                                   "so a new entity object instance should be fetched.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final Object delegate = em.getDelegate();

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                            Assert.assertNotNull("Assert find did not return null.", findEntity);
                            if (findEntity == null)
                                return null;

                            Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);

                            Assert.assertNotSame("Assert that the delegate EntityManager used in components 1 and 2 are not the same",
                                                 delegate,
                                                 em.getDelegate());
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSBMTSFBuddy().doWorkRequest(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is propagated to a call to a second component when that
     * component continues the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 10
     */
    public void testPropagation001(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be the same persistence context
            // because the buddy CMT uses the same transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be the same persistence context since persistence context propagation is expected.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final boolean ejb2_isUnsynchronised = isUnsynchronised;
                final boolean ejb2_emShouldJoinTx = emShouldJoinTx;
                final Object delegate = em.getDelegate();

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNotNull("Assert find did not return null.", findEntity);

                            if (!ejb2_isUnsynchronised || ejb2_emShouldJoinTx)
                                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                            else
                                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                            Assert.assertSame("Assert that find did return the same entity.", entityPCtx1, findEntity);

                            Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                              delegate,
                                              em.getDelegate());
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is propagated to a call to a second component when that
     * component continues the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (uses transaction REQUIRED on business method)
     *
     * Points: 10
     */
    public void testPropagation001A(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation001A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be the same persistence context
            // because the buddy CMT uses the same transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be the same persistence context since persistence context propagation is expected.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final boolean ejb2_isUnsynchronised = isUnsynchronised;
                final boolean ejb2_emShouldJoinTx = emShouldJoinTx;
                final Object delegate = em.getDelegate();

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNotNull("Assert find did not return null.", findEntity);

                            if (!ejb2_isUnsynchronised || ejb2_emShouldJoinTx)
                                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                            else
                                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                            Assert.assertSame("Assert that find did return the same entity.", entityPCtx1, findEntity);

                            Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                              delegate,
                                              em.getDelegate());
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                TargetEntityManager tEmType = (isUnsynchronised) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED;

                getCMTSCMTSFBuddy().doWorkRequestWithTxRequired(twr, tEmType);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
     * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
     * the IllegalStateException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with an
     * unsynchronized CMTS #1, and then calls a second session bean which attempts to use synchronized CMTS #2
     * and CMTS #1 and #2 utilize the same EntityManagerFactory (same persistence unit), then a
     * IllegalStateException should be thrown by the container.
     *
     * Test supports both joining and not joining the Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 7
     */
    public void testPropagation002(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }
        if (!isUnsynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Unsynchronized (is \"" +
                        getSynctype(testExecResources, "test-jpa-resource") +
                        "\" .)  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity, while trying to use a synchronized
            // CMTS associated with the same EntityManagerFactory, then a IllegalStateException should be thrown.
            // (1 points)
            try {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity, while trying to use " +
                                   "a synchronized CMTS associated with the same EntityManagerFactory, then a IllegalStateException should be thrown.");

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") " +
                                               "(action should cause IllegalStateException to be thrown) ... ");
                            try {
                                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by the em.find() operation.");
                            } catch (Throwable t) {
                                assertExceptionIsInChain(IllegalStateException.class, t);
                            }
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } finally {
                try {
                    System.out.println("Rolling back the JTA transaction ...");
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
     * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
     * the IllegalStateException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with an
     * unsynchronized CMTS #1, and then calls a second session bean which attempts to use synchronized CMTS #2
     * and CMTS #1 and #2 utilize the same EntityManagerFactory (same persistence unit), then a
     * IllegalStateException should be thrown by the container.
     *
     * Test supports both joining and not joining the Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SF CMT (uses transaction REQUIRED on business method)
     *
     * Points: 7
     */
    public void testPropagation002A(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation002A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }
        if (!isUnsynchronized(testExecResources, "test-jpa-resource")) {
            Assert.fail("The JPAResource 'test-jpa-resource' must be of type Unsynchronized (is \"" +
                        getSynctype(testExecResources, "test-jpa-resource") +
                        "\" .)  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity, while trying to use a synchronized
            // CMTS associated with the same EntityManagerFactory, then a IllegalStateException should be thrown.
            // (1 points)
            try {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity, while trying to use " +
                                   "a synchronized CMTS associated with the same EntityManagerFactory, then a IllegalStateException should be thrown.");

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") " +
                                               "(action should cause IllegalStateException to be thrown) ... ");
                            try {
                                SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by the em.find() operation.");
                            } catch (Throwable t) {
                                assertExceptionIsInChain(IllegalStateException.class, t);
                            }
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                getCMTSCMTSFBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } finally {
                try {
                    System.out.println("Rolling back the JTA transaction ...");
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
     * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
     * the IllegalStateException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with a
     * synchronized CMTS #1, and then calls a second session bean which attempts to use an unsynchronized CMTS #2
     * and CMTS #1 and #2 utilize the same EntityManagerFactory (same persistence unit), then the second session
     * bean will use the CMTS persistence context propagated by the first session bean.
     *
     *
     * CMTS #1 is synchronized,CMTS #2 is unsynchronized
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 10
     */
    public void testPropagation003(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation003";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
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

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Should be the same persistence context
            // because the buddy CMT uses the same transaction
            // (4 points)
            {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  " +
                                   "Should be the same persistence context since persistence context propagation is expected.");

                final SimpleVersionedEntity10 entityPCtx1 = findEntity;
                final Object delegate = em.getDelegate();

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        try {
                            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                            SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNotNull("Assert find did not return null.", findEntity);

                            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                            Assert.assertSame("Assert that find did return the same entity.", entityPCtx1, findEntity);
                            Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                              delegate,
                                              em.getDelegate());
                        } finally {
                            System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                        }

                        return null;
                    }

                };

                getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
            }

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS a
     * persistence context of X, and invokes a second component (propagating X) that invokes a CMTS with
     * a persistence context of Y, that X and Y can coexist along side one another.
     *
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 18
     */
    public void testPropagation004(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation004";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity wit an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Create TestWorkRequest for multiple permutations of second component invocation
            // Valid Key/Value pairs for twrConfig map:
            // "is.synchronized" - (Boolean) whether the EntityManager is SYNCHRONIZED (TRUE) or UNSYNCHRONIZED (FALSE)
            // "should.join.tx" - (Boolean) If UNSYNCHRONIZED, whether the EntityManager should join the tx (TRUE) or not (FALSE)
            final HashMap<String, Object> twrConfig = new HashMap<String, Object>();
            final SimpleVersionedEntity10 entityPCtx1 = findEntity;
            final Object delegate = em.getDelegate();

            // An execution of this TestWorkRequest is worth 4 points.
            // Since this TWR is supposed to run with a different EntityManagerFactory than CMTS #1 the find() operation
            // should never return the same entity object instance.
            TestWorkRequest twr = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                    boolean ejb2_isUnsynchronised = Boolean.TRUE.equals(twrConfig.get("is.synchronized"));
                    boolean ejb2_emShouldJoinTx = Boolean.TRUE.equals(twrConfig.get("should.join.tx"));

                    try {
                        System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                        Assert.assertNotNull("Assert find did not return null.", findEntity);

                        if (!ejb2_isUnsynchronised || ejb2_emShouldJoinTx)
                            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                        else
                            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityPCtx1, findEntity);
                        Assert.assertNotSame("Assert that the delegate EntityManager used in components 1 and 2 are not the same",
                                             delegate,
                                             em.getDelegate());
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test with TXSYNC2:Synchronized on second component (4 points)
            twrConfig.clear();
            twrConfig.put("is.synchronized", Boolean.TRUE);
            twrConfig.put("should.join.tx", Boolean.TRUE);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC2_SYNCHRONIZED);

            // Test with TXSYNC2:Unsynchronized:nojoin on second component (4 points)
            twrConfig.clear();
            twrConfig.put("is.synchronized", Boolean.FALSE);
            twrConfig.put("should.join.tx", Boolean.FALSE);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC2_UNSYNCHRONIZED);

            // Test with TXSYNC2:Unsynchronized:join on second component (4 points)
            twrConfig.clear();
            twrConfig.put("is.synchronized", Boolean.FALSE);
            twrConfig.put("should.join.tx", Boolean.TRUE);
            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC2_UNSYNCHRONIZED);

            System.out.println("Rolling back the JTA transaction ...");
            tj.rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If the component is a stateful session bean to which an extended persistence context has been
     * bound and there is a different persistence context associated with the JTA transaction, an
     * EJBException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with an
     * a transaction scope persistence context, and attempts to propagate that persistence context
     * to a stateful session bean with a extended persistence context associated with the same
     * persistence unit, that an EJBException is thrown.
     *
     * Tests SYNCHRONIZED EntityManager on bean #1
     *
     * Points: 8 points
     */
    public void testPropagation005(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation005";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        InitialContext ic = null;
        TxSyncCMTSFEXSyncBuddyLocal bean2 = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Create an instance of TxSyncCMTSFEXSyncBuddyLocal
            ic = new InitialContext();
            bean2 = (TxSyncCMTSFEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
            Assert.assertNotNull("Assert that an instance of TxSyncCMTSFEXSyncBuddyLocal was created.", bean2);

            // Load the test entity with an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Because Bean #1 has a CM-TS associated
            // (but not necessarally joined) with the transaction of the same persistence unit that Bean #2 has
            // an CM-EX persistence context for, the operation should throw an EJBException. (1 point)
            try {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  Because Bean #1 has a CM-TS associated " +
                                   "(but not necessarally joined) with the transaction of the same persistence unit that Bean #2 has " +
                                   "an CM-EX persistence context for, the operation should throw an EJBException.");

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        // Poke the CM-EX ref
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                        Assert.fail("Execution entering here means an EJBException was not thrown.");

                        return null;
                    }

                };

                try {
                    bean2.doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    Assert.fail("No Exception was thrown by the call to bean #2.");
                } catch (Exception e) {
                    // Caught an Exception
                    assertExceptionIsInChain(EJBException.class, e);
                }
            } finally {
                try {
                    System.out.println("Rolling back the JTA transaction ...");
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Exception e) {
                    // swallow
                }
            }

            if (bean2 != null) {
                try {
                    bean2.close();
                } catch (Exception e) {
                    // swallow
                }
            }
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If the component is a stateful session bean to which an extended persistence context has been
     * bound and there is a different persistence context associated with the JTA transaction, an
     * EJBException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with an
     * a transaction scope persistence context, and attempts to propagate that persistence context
     * to a stateful session bean with a extended persistence context associated with the same
     * persistence unit, that an EJBException is thrown.
     *
     * Tests UNSYNCHRONIZED EntityManager on bean #2
     *
     * Points: 8 points
     */
    public void testPropagation005A(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation005A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();
        SimpleVersionedEntity10 findEntity = null;

        InitialContext ic = null;
        TxSyncCMTSFEXUnsyncBuddyLocal bean2 = null;

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Create an instance of TxSyncCMTSFEXSyncBuddyLocal
            ic = new InitialContext();
            bean2 = (TxSyncCMTSFEXUnsyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
            Assert.assertNotNull("Assert that an instance of TxSyncCMTSFEXUnsyncBuddyLocal was created.", bean2);

            // Load the test entity with an active JTA transaction (4 points)
            {
                System.out.println("Point B: Load the test entity with an active JTA transaction");
                em.clear();

                System.out.println("Beginning JTA transaction ...");
                tj.beginTransaction();

                if (isUnsynchronised) {
                    if (emShouldJoinTx) {
                        System.out.println("Unsynchronized EntityManager joining transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertFalse("Assert that EM has not joined tx.", em.isJoinedToTransaction());
                    }
                } else {
                    Assert.assertTrue("Assert that EM has joined tx.", em.isJoinedToTransaction());
                }

                System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                findEntity = em.find(SimpleVersionedEntity10.class, identity);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                if (!isUnsynchronised || emShouldJoinTx)
                    Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                else
                    Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
            }

            // Invoke Buddy CMT and instruct it to fetch the same entity.  Because Bean #1 has a CM-TS associated
            // (but not necessarally joined) with the transaction of the same persistence unit that Bean #2 has
            // an CM-EX persistence context for, the operation should throw an EJBException. (1 point)
            try {
                System.out.println("Point C: Invoke Buddy CMT and instruct it to fetch the same entity.  Because Bean #1 has a CM-TS associated " +
                                   "(but not necessarally joined) with the transaction of the same persistence unit that Bean #2 has " +
                                   "an CM-EX persistence context for, the operation should throw an EJBException.");

                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx, Object managedComponentObject) {
                        System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                        // Poke the CM-EX ref
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                        Assert.fail("Execution entering here means an EJBException was not thrown.");

                        return null;
                    }

                };

                try {
                    bean2.doWorkRequestWithTxRequired(twr, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    Assert.fail("No Exception was thrown by the call to bean #2.");
                } catch (Exception e) {
                    // Caught an Exception
                    assertExceptionIsInChain(EJBException.class, e);
                }
            } finally {
                try {
                    System.out.println("Rolling back the JTA transaction ...");
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Exception e) {
                    // swallow
                }
            }

            if (bean2 != null) {
                try {
                    bean2.close();
                } catch (Exception e) {
                    // swallow
                }
            }
            System.out.println(testBucketName + "." + testName + ": End");
        }

    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * * If the component is a stateful session bean to which an extended persistence context has been bound and
     * there is a different persistence context associated with the JTA transaction, an EJBException is thrown by
     * the container.
     * * If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
     * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
     * the IllegalStateException is thrown by the container.
     * * Otherwise, if there is a persistence context associated with the JTA transaction, that persistence context is
     * propagated and used.
     *
     * Note that a component with a persistence context of type Synchronization- Type.UNSYNCHRONIZED may be called by a
     * component propagating either a persistence context of type SynchronizationType.UNSYNCHRONIZED or a persistence
     * context of type SynchronizationType.SYNCHRONIZED into it.
     */

    /*
     * Utilities
     */

    /**
     * Creates a TestWorkRequest that will create a single SimpleVersionedEntity10 with an
     * identity as specified by the argument and a StrData payload of "Simple String".
     *
     * @param identifier
     * @param identity
     * @return
     */
    private TestWorkRequest TWR_createSimpleVersionedEntity10(String identifier, int identity) {
        final String e_identifier = identifier;
        final int e_identity = identity;

        return new TestWorkRequest() {
            private static final long serialVersionUID = -963281443402926776L;

            @Override
            public Serializable doTestWork(EntityManager em,
                                           UserTransaction tx,
                                           Object managedComponentObject) {
                final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";

                System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                try {
                    System.out.println("Creating SimpleVersionedEntity10(id=" + e_identity + ") ...");
                    SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10();
                    newEntity.setId(e_identity);
                    newEntity.setStrData("Simple String");

                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    return newEntity;
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Catch any Exceptions thrown by the test case for proper error logging.
                    Assert.fail("Caught an unexpected Exception during test execution." + t);
                    return null;
                } finally {
                    System.out.println("End " + uowName + " on " + managedComponentObject + " ...");
                }
            }
        };
    }

    public void testTemplate(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        final String testName = "testTemplate";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//           Assert.fail("Missing JPAResource 'cleanup'.  Cannot execute the test.");
        //return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource'.  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }
}
