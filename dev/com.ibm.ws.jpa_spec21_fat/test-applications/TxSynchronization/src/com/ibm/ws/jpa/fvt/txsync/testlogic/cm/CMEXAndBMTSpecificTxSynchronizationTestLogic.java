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
import java.util.Map;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTSFEXSyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTSFEXUnsyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.testlogic.AbstractTxSyncTestLogic;
import com.ibm.ws.jpa.fvt.txsync.testlogic.BeanStore;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * Tests examining CMEX-Specific Transaction Synchronicity using Bean Managed Transactions (UserTransactions).
 *
 *
 */
public class CMEXAndBMTSpecificTxSynchronizationTestLogic extends AbstractTxSyncTestLogic {
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
 * JPA Spec 2.1: 7.6.3 Container-managed Extended Persistence Context
 *
 * A container-managed extended persistence context can only be initiated within the scope of a stateful session bean.
 * It exists from the point at which the stateful session bean that declares a dependency on an entity manager of type
 * PersistenceContextType.EXTENDED is created, and is said to be bound to the stateful session bean. The dependency on
 * the extended persistence context is declared by means of the PersistenceContext annotation or
 * persistence-context-ref deployment descriptor element. The association of the extended persistence context with the
 * JTA transaction is independent of the synchronization type of the persistence context and whether the persistence
 * context has been joined to the transaction.
 *
 * The persistence context is closed by the container when the @Remove method of the stateful session bean completes
 * (or the stateful session bean instance is otherwise destroyed).
 *
 *
 *
 * JPA Spec 2.1: 7.6.3.1 Inheritance of Extended Persistence Context
 *
 * If a stateful session bean instantiates a stateful session bean (executing in the same EJB container instance) which
 * also has such an extended persistence context with the same synchronization type, the extended persistence context
 * of the first stateful session bean is inherited by the second stateful session bean and bound to it, and this rule
 * recursively applies independently of whether transactions are active or not at the point of the creation of the
 * stateful session beans. If the stateful session beans differ in declared synchronization type, the EJBException is
 * thrown by the container.
 *
 * If the persistence context has been inherited by any stateful session beans, the container does not close the
 * persistence context until all such stateful session beans have been removed or otherwise destroyed.
 *
 * JPA Spec 2.1: 7.6.4 Persistence Context Propagation
 *
 * As described in section 7.1, a single persistence context may correspond to one or more JTA entity manager instances
 * (all associated with the same entity manager factory[84]).
 *
 * The persistence context is propagated across the entity manager instances as the JTA transaction is propagated.
 * A persistence context of type SynchronizationType.UNSYNCHRONIZED is propagated with the JTA transaction regardless
 * of whether it has been joined to the transaction.
 *
 * Propagation of persistence contexts only applies within a local environment. Persistence contexts are not propagated
 * to remote tiers.
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
    private final static String testBucketName = CMEXAndBMTSpecificTxSynchronizationTestLogic.class.getName();

    /**
     * Verify that an entity fetched outside a JTA transaction remains managed by an Extended persistence context,
     * regardless of transaction synchronicity.
     *
     * Points: 6
     */
    public void testExtendedPersistence001(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedPersistence001";

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

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity with an active JTA transaction (x points)
            System.out.println("Point B: Load the test entity with an active JTA transaction");
            em.clear();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
            findEntity = em.find(SimpleVersionedEntity10.class, identity);
            Assert.assertNotNull("Assert find did not return null.", findEntity);

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that an entity fetched within a JTA transaction remains managed by an Extended persistence context,
     * regardless of transaction synchronicity and, with unsynchronized EntityManagers, whether it joins the
     * transaction, after the JTA transaction commits.
     *
     * Points: 8
     */
    public void testExtendedPersistence002(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedPersistence002";

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

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity with an active JTA transaction (x points)
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

            // Commit the JTA transaction, entities should remain managed by the persistence context.
            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            // JTA tran is done, should not be associated with it any longer.
            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Entity should still be managed.
            Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that an entity fetched within a JTA transaction may be detached when that transaction rolls back.
     * Unsynchronized CMEX EntityManagers that did not join the transaction should not have its entities detached.
     * Synchronized and Unsynchronized (that joined the tx) CMEX EntityManagers should have its entities detached.
     *
     * Points: 8
     */
    public void testExtendedPersistence003(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedPersistence003";

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
        // Here, the difference matters.
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

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Load the test entity with an active JTA transaction (x points)
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

            // Rollback the JTA transaction, entities should remain managed by the persistence context.
            System.out.println("Rolling back JTA transaction ...");
            tj.rollbackTransaction();

            // JTA tran is done, should not be associated with it any longer.
            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Entity should still be managed if and only if it is an Unsynchronized EntityManager that did not
            // join the tx
            if (!isUnsynchronised || emShouldJoinTx)
                Assert.assertFalse("Assert that entity is not managed.", em.contains(findEntity));
            else
                Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that EntityManager.clear() can be invoked outside of a JTA transaction, and that the
     * entity persisted becomes detached.
     *
     * Points: 4
     */
    public void testPersist001(
                               TestExecutionContext testExecCtx,
                               TestExecutionResources testExecResources,
                               Object managedComponentObject) throws Throwable {
        final String testName = "testPersist001";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Str Data");

            Assert.assertFalse("Assert the new entity is not yet managed.", em.contains(newEntity));

            System.out.println("Persisting new entity ...");
            em.persist(newEntity);

            Assert.assertTrue("Assert the new entity is now managed.", em.contains(newEntity));

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(newEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the persist() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     * Points: 7
     */
    public void testPersist002(
                               TestExecutionContext testExecCtx,
                               TestExecutionResources testExecResources,
                               Object managedComponentObject) throws Throwable {
        final String testName = "testPersist002";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Str Data");

            Assert.assertFalse("Assert the new entity is not yet managed.", em.contains(newEntity));

            System.out.println("Persisting new entity ...");
            em.persist(newEntity);

            Assert.assertTrue("Assert the new entity is now managed.", em.contains(newEntity));

            System.out.println("Beginning new JTA transaction ...");
            tj.beginTransaction();

            System.out.println("Committing JTA transaction without joining/invoking EntityManager");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
            Assert.assertTrue("Assert the new entity is still managed.", em.contains(newEntity));

            System.out.println("Verify that the entity was never added to the database.");
            TestWorkRequest twrPopulate = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate,
                                                                                                                              TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            Assert.assertNull("Assert that the find operation in a non-propagated CMTS PCtx returned null.", findEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(newEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     * Points: 8
     */
    public void testPersist003(
                               TestExecutionContext testExecCtx,
                               TestExecutionResources testExecResources,
                               Object managedComponentObject) throws Throwable {
        final String testName = "testPersist003";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Str Data");

            Assert.assertFalse("Assert the new entity is not yet managed.", em.contains(newEntity));

            System.out.println("Persisting new entity ...");
            em.persist(newEntity);

            Assert.assertTrue("Assert the new entity is now managed.", em.contains(newEntity));

            System.out.println("Beginning new JTA transaction ...");
            tj.beginTransaction();

            System.out.println("Joining the transaction ...");
            em.joinTransaction();

            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
            Assert.assertTrue("Assert the new entity is still managed.", em.contains(newEntity));

            System.out.println("Verify that the entity was added to the database.");
            TestWorkRequest twrPopulate = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate,
                                                                                                                              TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            Assert.assertNotNull("Assert that the find operation in a non-propagated CMTS PCtx did not return null.", findEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(newEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.persist() can be invoked inside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * For SYNCHRONIZED EntityManagers, verify that the new entity was saved after the JTA tran commits.
     * For UNSYNCHRONIZED EntityManagers, if the JTA tran was joined then verify that the new entity was
     * saved after the JTA tran commits. If the JTA tran was not joined, then verify that the new entity
     * was not saved after the JTA tran commits.
     *
     *
     * Points: 9
     */
    public void testPersist004(
                               TestExecutionContext testExecCtx,
                               TestExecutionResources testExecResources,
                               Object managedComponentObject) throws Throwable {
        final String testName = "testPersist004";

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
        // Here, the difference matters.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

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

            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Str Data");

            Assert.assertFalse("Assert the new entity is not yet managed.", em.contains(newEntity));

            System.out.println("Persisting new entity ...");
            em.persist(newEntity);

            Assert.assertTrue("Assert the new entity is now managed.", em.contains(newEntity));
            if (!isUnsynchronised || emShouldJoinTx)
                Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
            else
                Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
            Assert.assertTrue("Assert the new entity is still managed.", em.contains(newEntity));

            System.out.println("Check if the entity was added to the database.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            if (!isUnsynchronised || emShouldJoinTx)
                Assert.assertNotNull("Assert that the find operation in a non-propagated CMTS PCtx did not return null.", findEntity);
            else
                Assert.assertNull("Assert that the find operation in a non-propagated CMTS PCtx did return null.", findEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(newEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     *
     * Points: 11
     */
    public void testMerge001(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        final String testName = "testMerge001";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Mutating entity ...");
            newEntity.setStrData("Mutant Data");

            System.out.println("Merging entity ...");
            SimpleVersionedEntity10 mergedEntity = em.merge(newEntity);
            Assert.assertNotNull("Assert merge operation did not return null.", mergedEntity);
            Assert.assertNotSame("Assert the merge operation did not return the same entity object.", newEntity, mergedEntity);
            Assert.assertEquals("Assert that the merged entity's data is \"Mutant Data\"", "Mutant Data", mergedEntity.getStrData());
            Assert.assertTrue("Assert the merged entity is now managed.", em.contains(mergedEntity));

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(mergedEntity));

            // Assert that the database has not been modified
            System.out.println("Verify that the database has not been modified.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNotNull("Assert find did not return null.", findEntity);
            Assert.assertEquals("Assert that the new entity's data is still \"Simple String\"", "Simple String", findEntity.getStrData());

            System.out.println("Ending test.");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the merge() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     * Points: 14
     */
    public void testMerge002(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        final String testName = "testMerge002";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Mutating entity ...");
            newEntity.setStrData("Mutant Data");

            System.out.println("Merging entity ...");
            SimpleVersionedEntity10 mergedEntity = em.merge(newEntity);
            Assert.assertNotNull("Assert merge operation did not return null.", mergedEntity);
            Assert.assertNotSame("Assert the merge operation did not return the same entity object.", newEntity, mergedEntity);
            Assert.assertEquals("Assert that the merged entity's data is \"Mutant Data\"", "Mutant Data", mergedEntity.getStrData());
            Assert.assertTrue("Assert the merged entity is now managed.", em.contains(mergedEntity));

            // Assert that the database has not been modified
            System.out.println("Verify that the database has not been modified.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNotNull("Assert find did not return null.", findEntity);
            Assert.assertEquals("Assert that the new entity's data is still \"Simple String\"", "Simple String", findEntity.getStrData());

            System.out.println("Beginning new JTA transaction ...");
            tj.beginTransaction();

            System.out.println("Committing JTA transaction without joining/invoking EntityManager");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Assert that the database has still not been modified
            System.out.println("Verify that the database has still not been modified.");
            SimpleVersionedEntity10 findEntity2 = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNotNull("Assert find did not return null.", findEntity2);
            Assert.assertEquals("Assert that the new entity's data is still \"Simple String\"", "Simple String", findEntity2.getStrData());

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(mergedEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     * Points: 14
     */
    public void testMerge003(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        final String testName = "testMerge003";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Mutating entity ...");
            newEntity.setStrData("Mutant Data");

            System.out.println("Merging entity ...");
            SimpleVersionedEntity10 mergedEntity = em.merge(newEntity);
            Assert.assertNotNull("Assert merge operation did not return null.", mergedEntity);
            Assert.assertNotSame("Assert the merge operation did not return the same entity object.", newEntity, mergedEntity);
            Assert.assertEquals("Assert that the merged entity's data is \"Mutant Data\"", "Mutant Data", mergedEntity.getStrData());
            Assert.assertTrue("Assert the merged entity is now managed.", em.contains(mergedEntity));

            System.out.println("Beginning new JTA transaction ...");
            tj.beginTransaction();

            System.out.println("Joining the transaction ...");
            em.joinTransaction();

            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
            Assert.assertTrue("Assert the new entity is still managed.", em.contains(mergedEntity));

            // Assert that the database has been modified
            System.out.println("Verify that the database has been modified.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNotNull("Assert find did not return null.", findEntity);
            Assert.assertEquals("Assert that the new entity's data is now \"Mutant Data\"", "Mutant Data", findEntity.getStrData());

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(mergedEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * For SYNCHRONIZED EntityManagers, verify that the new entity was saved after the JTA tran commits.
     * For UNSYNCHRONIZED EntityManagers, if the JTA tran was joined then verify that the new entity was
     * saved after the JTA tran commits. If the JTA tran was not joined, then verify that the new entity
     * was not saved after the JTA tran commits.
     *
     * Points: 14
     */
    public void testMerge004(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        final String testName = "testMerge004";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // Here, the difference matters.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Mutating entity ...");
            newEntity.setStrData("Mutant Data");

            System.out.println("Merging entity ...");
            SimpleVersionedEntity10 mergedEntity = em.merge(newEntity);
            Assert.assertNotNull("Assert merge operation did not return null.", mergedEntity);
            Assert.assertNotSame("Assert the merge operation did not return the same entity object.", newEntity, mergedEntity);
            Assert.assertEquals("Assert that the merged entity's data is \"Mutant Data\"", "Mutant Data", mergedEntity.getStrData());
            Assert.assertTrue("Assert the merged entity is now managed.", em.contains(mergedEntity));

            System.out.println("Beginning new JTA transaction ...");
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

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());
            Assert.assertTrue("Assert the new entity is still managed.", em.contains(mergedEntity));

            // Assert that the database has been modified (if synchronized/tx joined) (2 points)
            if (!isUnsynchronised || emShouldJoinTx) {
                System.out.println("Verify that the database has been modified.");
                TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
                SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind,
                                                                                                                                  TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                Assert.assertNotNull("Assert find did not return null.", twrFind);
                Assert.assertEquals("Assert that the new entity's data is now \"Mutant Data\"", "Mutant Data", findEntity.getStrData());
            } else {
                System.out.println("Verify that the database has not been modified.");
                TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
                SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind,
                                                                                                                                  TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
                Assert.assertEquals("Assert that the new entity's data is still \"Simple String\"", "Simple String", findEntity.getStrData());
            }

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(mergedEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the remove method invocation.
     *
     *
     * Points: 7
     */
    public void testRemove001(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = "testRemove001";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Finding entity with CMEX persistence context ...");
            SimpleVersionedEntity10 targetEntity = em.find(SimpleVersionedEntity10.class, identity);
            Assert.assertNotNull("Assert find did not return null.", targetEntity);
            Assert.assertTrue("Assert that the entity is managed.", em.contains(targetEntity));

            System.out.println("Removing entity ...");
            em.remove(targetEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(targetEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the remove method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the remove() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     * Points: 8
     */
    public void testRemove002(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = "testRemove002";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Finding entity with CMEX persistence context ...");
            SimpleVersionedEntity10 targetEntity = em.find(SimpleVersionedEntity10.class, identity);
            Assert.assertNotNull("Assert find did not return null.", targetEntity);
            Assert.assertTrue("Assert that the entity is managed.", em.contains(targetEntity));

            System.out.println("Removing entity ...");
            em.remove(targetEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(targetEntity));

            // Assert that the database has not been modified
            System.out.println("Verify that the database has not been modified.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNotNull("Assert find did not return null.", findEntity);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the remove method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     * Points: 10
     */
    public void testRemove003(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = "testRemove003";

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

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Finding entity with CMEX persistence context ...");
            SimpleVersionedEntity10 targetEntity = em.find(SimpleVersionedEntity10.class, identity);
            Assert.assertNotNull("Assert find did not return null.", targetEntity);
            Assert.assertTrue("Assert that the entity is managed.", em.contains(targetEntity));

            System.out.println("Removing entity ...");
            em.remove(targetEntity);

            System.out.println("Beginning new JTA transaction ...");
            tj.beginTransaction();

            System.out.println("Joining the transaction ...");
            em.joinTransaction();

            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Assert that the database has been modified
            System.out.println("Verify that the database has been modified.");
            TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
            SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            Assert.assertNull("Assert find did not return null.", findEntity);

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(targetEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * For SYNCHRONIZED EntityManagers, verify that the new entity was saved after the JTA tran commits.
     * For UNSYNCHRONIZED EntityManagers, if the JTA tran was joined then verify that the new entity was
     * saved after the JTA tran commits. If the JTA tran was not joined, then verify that the new entity
     * was not saved after the JTA tran commits.
     *
     * Points: 10
     */
    public void testRemove004(
                              TestExecutionContext testExecCtx,
                              TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = "testRemove004";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // Here, the difference matters.
        boolean emShouldJoinTx = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx = Boolean.parseBoolean(shouldJoinTxStr);
        }

        System.out.println("EntityManager Synchronization Type: " + getSynctype(testExecResources, "test-jpa-resource"));
        boolean isUnsynchronised = isUnsynchronized(testExecResources, "test-jpa-resource");

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            em.clear(); // Ensure the extended persistence context is clear before testing.

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Populate the database (1 point)
            SimpleVersionedEntity10 newEntity = null;
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                newEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            Assert.assertFalse("Assert that the entity created by the buddy session bean invocation is not managed.",
                               em.contains(newEntity));
            Assert.assertEquals("Assert that the new entity's data is \"Simple String\"", "Simple String", newEntity.getStrData());

            System.out.println("Finding entity with CMEX persistence context ...");
            SimpleVersionedEntity10 targetEntity = em.find(SimpleVersionedEntity10.class, identity);
            Assert.assertNotNull("Assert find did not return null.", targetEntity);
            Assert.assertTrue("Assert that the entity is managed.", em.contains(targetEntity));

            System.out.println("Removing entity ...");
            em.remove(targetEntity);

            System.out.println("Beginning new JTA transaction ...");
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

            System.out.println("Committing JTA transaction ...");
            tj.commitTransaction();

            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

            // Assert that the database has been modified (if synchronized/tx joined) (1 point)
            if (!isUnsynchronised || emShouldJoinTx) {
                System.out.println("Verify that the database has been modified.");
                TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
                SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind,
                                                                                                                                  TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                Assert.assertNull("Assert find did return null.", findEntity);
            } else {
                System.out.println("Verify that the database has not been modified.");
                TestWorkRequest twrFind = TWR_findSimpleVersionedEntity10("twrFind", identity, false);
                SimpleVersionedEntity10 findEntity = (SimpleVersionedEntity10) getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrFind,
                                                                                                                                  TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                Assert.assertNotNull("Assert find did not return null.", findEntity);
            }

            System.out.println("Clearing persistence context ...");
            em.clear();

            Assert.assertFalse("Assert the new entity is no longer managed.", em.contains(targetEntity));

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited without an active JTA transaction.
     * - Verify with SYNCHRONIZED EntityManager
     *
     * JPA Spec 2.1 Section 7.6.3.1 Inheritance of Extended Persistence Context:
     *
     * If a stateful session bean instantiates a stateful session bean (executing in the same EJB
     * container instance) which also has such an extended persistence context with the same
     * synchronization type, the extended persistence context of the first stateful session bean is
     * inherited by the second stateful session bean and bound to it, and this rule recursively
     * applies-independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * 5 Points
     */
    public void testExtendedScopeInheritance001(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance001";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");

            InitialContext ic = new InitialContext();
            UserTransaction tx = (UserTransaction) ic.lookup("java:comp/UserTransaction");

            TxSyncBMTSFEXSyncBuddyLocal sfBean1 = null;
            try {
                // Create BMT Bean #1 (TxSyncBMTSFEXSyncBuddyEJB)
                System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #1 ...");
                sfBean1 = (TxSyncBMTSFEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                Assert.assertNotNull("Assert sfBean1 is not null (lookup & create succeeded).", sfBean1);
                if (sfBean1 == null) {
                    return;
                }

                // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                SimpleVersionedEntity10 newEntity = null;
                {
                    System.out.println("Point A: Populate the Database");
                    TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);

                    System.out.println("Beginning User Transaction ...");
                    tx.begin();

                    newEntity = (SimpleVersionedEntity10) sfBean1.doWorkRequest(twrPopulate,
                                                                                TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    Assert.assertNotNull("Assert that a new Entity object instance was created.", newEntity);
                    tx.commit();
                }

                // Instruct Bean #1 to create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and
                // Verify that the persistence context is inherited.
                // (2 points)
                final SimpleVersionedEntity10 targetEntity = newEntity;
                final String e_identifier = "VerifyCMEXInheritance:" + testName;
                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx,
                                                   Object managedComponentObject) {
                        final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";
                        System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                        BeanStore thisEJB = (BeanStore) managedComponentObject;
                        Map<String, Object> beanStore = thisEJB.getBeanStore();

                        TxSyncBMTSFEXSyncBuddyLocal sfBean2 = null;
                        InitialContext ic = null;

                        try {
                            ic = new InitialContext();

                            // Create BMT Bean #1 (TxSyncBMTSFEXSyncBuddyEJB)
                            System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                            sfBean2 = (TxSyncBMTSFEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                            Assert.assertNotNull("Assert sfBean2 is not null (lookup & create succeeded).", sfBean2);
                            if (sfBean2 == null) {
                                return null;
                            }

                            // Bean #2 successfully created, verify that the persistence context was
                            // inherited by confirming that newEntity is also managed by it.
                            TestWorkRequest containsTWR = TWR_testContains(e_identifier + ":Bean2", targetEntity, true);
                            sfBean2.doWorkRequest(containsTWR, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

                            return null;
                        } catch (java.lang.AssertionError ae) {
                            throw ae;
                        } catch (Throwable t) {
                            // Catch any Exceptions thrown by the test case for proper error logging.
                            Assert.fail("Caught an unexpected Exception during test execution." + t);
                            return null;
                        } finally {
                            System.out.println("End " + uowName + " on " + managedComponentObject + " ...");
                            if (ic != null) {
                                try {
                                    ic.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }

                            if (sfBean2 != null) {
                                try {
                                    sfBean2.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }
                        }
                    }
                };

                // Execute Work Request
                System.out.println("Executing Work Requiest on Bean #1 ...");
                sfBean1.doWorkRequest(twr, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            } finally {
                // Test Cleanup
                if (ic != null) {
                    try {
                        ic.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                if (sfBean1 != null) {
                    try {
                        sfBean1.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited without an active JTA transaction.
     * - Verify with UNSYNCHRONIZED EntityManager
     *
     * JPA Spec 2.1 Section 7.6.3.1 Inheritance of Extended Persistence Context:
     *
     * If a stateful session bean instantiates a stateful session bean (executing in the same EJB
     * container instance) which also has such an extended persistence context with the same
     * synchronization type, the extended persistence context of the first stateful session bean is
     * inherited by the second stateful session bean and bound to it, and this rule recursively
     * applies-independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * 5 Points
     */
    public void testExtendedScopeInheritance001A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance001A";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");

            InitialContext ic = new InitialContext();
            UserTransaction tx = (UserTransaction) ic.lookup("java:comp/UserTransaction");

            TxSyncBMTSFEXUnsyncBuddyLocal sfBean1 = null;
            try {
                // Create BMT Bean #1 (TxSyncBMTSFEXUnsyncBuddyEJB)
                System.out.println("Creating TxSyncBMTSFEXUnsyncBuddyEJB #1 ...");
                sfBean1 = (TxSyncBMTSFEXUnsyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                Assert.assertNotNull("Assert sfBean1 is not null (lookup & create succeeded).", sfBean1);
                if (sfBean1 == null) {
                    return;
                }

                // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                SimpleVersionedEntity10 newEntity = null;
                {
                    System.out.println("Point A: Populate the Database");
                    TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);

                    System.out.println("Beginning User Transaction ...");
                    tx.begin();

                    newEntity = (SimpleVersionedEntity10) sfBean1.doWorkRequest(twrPopulate,
                                                                                TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    Assert.assertNotNull("Assert that a new Entity object instance was created.", newEntity);
                    tx.commit();
                }

                // Instruct Bean #1 to create Bean #2 (also a TxSyncBMTSFEXUnsyncBuddyEJB) and
                // Verify that the persistence context is inherited.
                // (2 points)
                final SimpleVersionedEntity10 targetEntity = newEntity;
                final String e_identifier = "VerifyCMEXInheritance:" + testName;
                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx,
                                                   Object managedComponentObject) {
                        final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";
                        System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                        BeanStore thisEJB = (BeanStore) managedComponentObject;
                        Map<String, Object> beanStore = thisEJB.getBeanStore();

                        TxSyncBMTSFEXUnsyncBuddyLocal sfBean2 = null;
                        InitialContext ic = null;

                        try {
                            ic = new InitialContext();

                            // Create BMT Bean #1 (TxSyncBMTSFEXUnsyncBuddyEJB)
                            System.out.println("Creating TxSyncBMTSFEXUnsyncBuddyEJB #2 ...");
                            sfBean2 = (TxSyncBMTSFEXUnsyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                            Assert.assertNotNull("Assert sfBean2 is not null (lookup & create succeeded).", sfBean2);
                            if (sfBean2 == null) {
                                return null;
                            }

                            // Bean #2 successfully created, verify that the persistence context was
                            // inherited by confirming that newEntity is also managed by it.
                            TestWorkRequest containsTWR = TWR_testContains(e_identifier + ":Bean2", targetEntity, true);
                            sfBean2.doWorkRequest(containsTWR, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

                            return null;
                        } catch (java.lang.AssertionError ae) {
                            throw ae;
                        } catch (Throwable t) {
                            // Catch any Exceptions thrown by the test case for proper error logging.
                            Assert.fail("Caught an unexpected Exception during test execution." + t);
                            return null;
                        } finally {
                            System.out.println("End " + uowName + " on " + managedComponentObject + " ...");
                            if (ic != null) {
                                try {
                                    ic.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }

                            if (sfBean2 != null) {
                                try {
                                    sfBean2.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }
                        }
                    }
                };

                // Execute Work Request
                System.out.println("Executing Work Requiest on Bean #1 ...");
                sfBean1.doWorkRequest(twr, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            } finally {
                // Test Cleanup
                if (ic != null) {
                    try {
                        ic.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                if (sfBean1 != null) {
                    try {
                        sfBean1.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a SYNCHRONIZED version and SFSB#2 using an UNSYNCHRONIZED version, that when
     * SFSB#1 attempts to create SFSB#2, this conflict will be detected and an
     * EJBException thrown.
     *
     * JPA Spec 2.1 Section 7.6.3.1 Inheritance of Extended Persistence Context:
     *
     * If a stateful session bean instantiates a stateful session bean (executing in the same EJB
     * container instance) which also has such an extended persistence context with the same
     * synchronization type, the extended persistence context of the first stateful session bean is
     * inherited by the second stateful session bean and bound to it, and this rule recursively
     * applies-independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * 2 Points
     */
    public void testExtendedScopeInheritance002(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");

            InitialContext ic = new InitialContext();
            UserTransaction tx = (UserTransaction) ic.lookup("java:comp/UserTransaction");

            TxSyncBMTSFEXSyncBuddyLocal sfBean1 = null;
            try {
                // Create BMT Bean #1 (TxSyncBMTSFEXSyncBuddyEJB)
                System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #1 ...");
                sfBean1 = (TxSyncBMTSFEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                Assert.assertNotNull("Assert sfBean1 is not null (lookup & create succeeded).", sfBean1);
                if (sfBean1 == null) {
                    return;
                }

                // Instruct Bean #1 to create Bean #2 (a TxSyncBMTSFEXUnsyncBuddyEJB) and
                // Verify that an EJBException is thrown.
                // (1 point)
                final String e_identifier = "VerifyCMEXInheritance:" + testName;
                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx,
                                                   Object managedComponentObject) {
                        final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";
                        System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                        TxSyncBMTSFEXUnsyncBuddyLocal sfBean2 = null;
                        InitialContext ic = null;

                        try {
                            ic = new InitialContext();

                            // Create BMT Bean #1 (TxSyncBMTSFEXUnsyncBuddyEJB)
                            // This may throw an EJBException if the appserver is proactive
                            // regarding creating JPA Resources.
                            System.out.println("Creating TxSyncBMTSFEXUnsyncBuddyEJB #2 ...");
                            try {
                                sfBean2 = (TxSyncBMTSFEXUnsyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");

                                Assert.fail("No Exception was thrown.");
                            } catch (Exception e) {
                                // Caught an Exception
                                assertExceptionIsInChain(EJBException.class, e);
                                return null;
                            }

                            return null;
                        } catch (java.lang.AssertionError ae) {
                            throw ae;
                        } catch (Throwable t) {
                            // Catch any Exceptions thrown by the test case for proper error logging.
                            Assert.fail("Caught an unexpected Exception during test execution." + t);
                            return null;
                        } finally {
                            System.out.println("End " + uowName + " on " + managedComponentObject + " ...");
                            if (ic != null) {
                                try {
                                    ic.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }

                            if (sfBean2 != null) {
                                try {
                                    sfBean2.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }
                        }
                    }
                };

                // Execute Work Request
                System.out.println("Executing Work Request on Bean #1 ...");
                sfBean1.doWorkRequest(twr, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            } finally {
                // Test Cleanup
                if (ic != null) {
                    try {
                        ic.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                if (sfBean1 != null) {
                    try {
                        sfBean1.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a UNSYNCHRONIZED version and SFSB#2 using an SYNCHRONIZED version, that when
     * SFSB#1 attempts to create SFSB#2, this conflict will be detected and an
     * EJBException thrown.
     *
     * JPA Spec 2.1 Section 7.6.3.1 Inheritance of Extended Persistence Context:
     *
     * If a stateful session bean instantiates a stateful session bean (executing in the same EJB
     * container instance) which also has such an extended persistence context with the same
     * synchronization type, the extended persistence context of the first stateful session bean is
     * inherited by the second stateful session bean and bound to it, and this rule recursively
     * applies-independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * 2 Points
     */
    public void testExtendedScopeInheritance002A(
                                                 TestExecutionContext testExecCtx,
                                                 TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance002";

        // Verify parameters

        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testTemplate: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");

            InitialContext ic = new InitialContext();
            UserTransaction tx = (UserTransaction) ic.lookup("java:comp/UserTransaction");

            TxSyncBMTSFEXUnsyncBuddyLocal sfBean1 = null;
            try {
                // Create BMT Bean #1 (TxSyncBMTSFEXSyncBuddyEJB)
                System.out.println("Creating TxSyncBMTSFEXUnsyncBuddyEJB #1 ...");
                sfBean1 = (TxSyncBMTSFEXUnsyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                Assert.assertNotNull("Assert sfBean1 is not null (lookup & create succeeded).", sfBean1);
                if (sfBean1 == null) {
                    return;
                }

                // Instruct Bean #1 to create Bean #2 (a TxSyncBMTSFEXSyncBuddyEJB) and
                // Verify that an EJBException is thrown.
                // (1 point)
                final String e_identifier = "VerifyCMEXInheritance:" + testName;
                TestWorkRequest twr = new TestWorkRequest() {
                    @Override
                    public Serializable doTestWork(EntityManager em,
                                                   UserTransaction tx,
                                                   Object managedComponentObject) {
                        final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";
                        System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                        TxSyncBMTSFEXSyncBuddyLocal sfBean2 = null;
                        InitialContext ic = null;

                        try {
                            ic = new InitialContext();

                            // Create BMT Bean #1 (TxSyncBMTSFEXSyncBuddyEJB)
                            // This may throw an EJBException if the appserver is proactive
                            // regarding creating JPA Resources.
                            System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                            try {
                                sfBean2 = (TxSyncBMTSFEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");

                                Assert.fail("No Exception was thrown.");
                            } catch (Exception e) {
                                // Caught an Exception
                                assertExceptionIsInChain(EJBException.class, e);
                                return null;
                            }

                            return null;
                        } catch (java.lang.AssertionError ae) {
                            throw ae;
                        } catch (Throwable t) {
                            // Catch any Exceptions thrown by the test case for proper error logging.
                            Assert.fail("Caught an unexpected Exception during test execution." + t);
                            return null;
                        } finally {
                            System.out.println("End " + uowName + " on " + managedComponentObject + " ...");
                            if (ic != null) {
                                try {
                                    ic.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }

                            if (sfBean2 != null) {
                                try {
                                    sfBean2.close();
                                } catch (Throwable t) {
                                    // Swallow
                                }
                            }
                        }
                    }
                };

                // Execute Work Request
                System.out.println("Executing Work Request on Bean #1 ...");
                sfBean1.doWorkRequest(twr, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            } finally {
                // Test Cleanup
                if (ic != null) {
                    try {
                        ic.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }

                if (sfBean1 != null) {
                    try {
                        sfBean1.close();
                    } catch (Throwable t) {
                        // Swallow
                    }
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

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

    private TestWorkRequest TWR_findSimpleVersionedEntity10(String identifier, int identity, boolean joinTx) {
        final String e_identifier = identifier;
        final int e_identity = identity;
        final boolean e_joinTx = joinTx;

        return new TestWorkRequest() {

            @Override
            public Serializable doTestWork(EntityManager em,
                                           UserTransaction tx,
                                           Object managedComponentObject) {
                final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";

                System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                try {
                    System.out.println("Finding SimpleVersionedEntity10(id=" + e_identity + ") ...");
                    if (e_joinTx) {
                        System.out.println("Joining transaction ...");
                        em.joinTransaction();
                    }
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, e_identity);
                    return findEntity;
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

    private TestWorkRequest TWR_testContains(String identifier, SimpleVersionedEntity10 testEnt, boolean expectContains) {
        final String e_identifier = identifier;
        final SimpleVersionedEntity10 e_testEnt = testEnt;
        final boolean e_expectContains = expectContains;

        return new TestWorkRequest() {
            private static final long serialVersionUID = -6004181378426388352L;

            @Override
            public Serializable doTestWork(EntityManager em,
                                           UserTransaction tx,
                                           Object managedComponentObject) {
                final String uowName = "TestWorkRequest.doTestWork(" + e_identifier + ")";

                System.out.println("Begin " + uowName + " on " + managedComponentObject + " ...");

                try {
                    boolean containResult = em.contains(e_testEnt);
                    if (e_expectContains) {
                        Assert.assertTrue("Assert that the entity " + e_testEnt + " is managed.", containResult);
                    } else {
                        Assert.assertFalse("Assert that the entity " + e_testEnt + " is not managed.", containResult);
                    }

                    return containResult;
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

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS)
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
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL BMT (CM-TS)
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
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS)
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
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF BMT (CM-TS)
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
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
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
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 do not use the same transaction synchronicity.
     * Second Bean is SL BMT (CM-TS)
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
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMEX #1,
     * that the persistence context for CMEX #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS) (uses transaction NOT_SUPPORTED to suspend JTA tran)
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
     * that the persistence context for CMEX #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMTS persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS) (uses transaction NOT_SUPPORTED to suspend JTA tran)
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
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS) (uses transaction REQUIRES_NEW to suspend JTA tran)
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
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRES_NEW to suspend JTA tran)
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
     * that the persistence context for CMEX #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL BMT (CM-TS) (invoking second BMT should suspend first BMT's JTA tran)
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
     * that the persistence context for CMEX #1 is not propagated to a call to a second component when that
     * component suspends the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF BMT (CM-TS) (invoking second BMT should suspend first BMT's JTA tran)
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

    /*
     * Propagation Tests: CM-EX propagating to a bean that uses a CM-TS associated with the same
     * persistence context. The CM-TS EntityManager reference should use the CM-EX persistence
     * context if propagation is expected.
     */

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMEX #1,
     * that the persistence context for CMEX #1 is propagated to a call to a second component when that
     * component continues the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CMTS) (uses transaction REQUIRED on business method)
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
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMEX #1,
     * that the persistence context for CMEX #1 is propagated to a call to a second component when that
     * component continues the JTA transaction.
     *
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRED on business method)
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
     * unsynchronized CMEX #1, and then calls a second session bean which attempts to use synchronized CMTS #2
     * and CMEX #1 and TS #2 utilize the same EntityManagerFactory (same persistence unit), then a
     * IllegalStateException should be thrown by the container.
     *
     * Test supports both joining and not joining the Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SL CMT (CM-TS) (uses transaction REQUIRED on business method)
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
     * unsynchronized CMEX #1, and then calls a second session bean which attempts to use synchronized CMTS #2
     * and CMEX #1 and TS #2 utilize the same EntityManagerFactory (same persistence unit), then a
     * IllegalStateException should be thrown by the container.
     *
     * Test supports both joining and not joining the Unsynchronized CMTS persistence context to the JTA transaction.
     *
     *
     * CMEX #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRED on business method)
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
     * synchronized CMEX #1, and then calls a second session bean which attempts to use an unsynchronized CMTS #2
     * and CMEX #1 and TS #2 utilize the same EntityManagerFactory (same persistence unit), then the second session
     * bean will use the CMTS persistence context propagated by the first session bean.
     *
     *
     * CMTEX #1 is synchronized,CMTS #2 is unsynchronized
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
     * persistence context of X, and invokes a second component (propagating X) that invokes a CMEX with
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
}
