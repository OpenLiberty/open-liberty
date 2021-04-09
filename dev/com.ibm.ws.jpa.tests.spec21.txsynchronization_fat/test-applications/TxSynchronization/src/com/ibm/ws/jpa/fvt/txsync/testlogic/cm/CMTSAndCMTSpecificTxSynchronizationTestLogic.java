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

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSLBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.testlogic.AbstractTxSyncTestLogic;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/*
 * JPA 2.1 Specification Contracts of Interest:
 *

 */
/**
 * Tests examining CMTS-Specific Transaction Synchronicity using Container Managed Transactions.
 *
 *
 */
public class CMTSAndCMTSpecificTxSynchronizationTestLogic extends AbstractTxSyncTestLogic {
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

    private final static String testBucketName = CMTSAndCMTSpecificTxSynchronizationTestLogic.class.getName();

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * that the persistence context for CMTS #1 is propagated to a call to a second component when that
     * component continues the JTA transaction.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-required business method
     * calls a tx-required business method on SLSB#2, which also has a Synchronized CMTS EntityManager
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 4
     */
    public void testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001(
                                                                                 TestExecutionContext testExecCtx,
                                                                                 TestExecutionResources testExecResources,
                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001(
                                                                                 TestExecutionContext testExecCtx,
                                                                                 TestExecutionResources testExecResources,
                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001(
                                                                                 TestExecutionContext testExecCtx,
                                                                                 TestExecutionResources testExecResources,
                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001(
                                                                                 TestExecutionContext testExecCtx,
                                                                                 TestExecutionResources testExecResources,
                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

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
     * Scenario: SLSB#1 with Unsynchronized CMTS EntityManager using a tx-required business method
     * calls a tx-required business method on SLSB#2, which also has an Unsynchronized CMTS EntityManager
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Variants: Configuration property permits joining/not joining the transaction.
     *
     * Points: 4
     */
    public void testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001(
                                                                                     TestExecutionContext testExecCtx,
                                                                                     TestExecutionResources testExecResources,
                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 5 points, 6 if emShouldJoinTx==true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not
                    // yet be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            if (emShouldJoinTx) {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            }

                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001(
                                                                                     TestExecutionContext testExecCtx,
                                                                                     TestExecutionResources testExecResources,
                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 5 points, 6 if emShouldJoinTx==true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not
                    // yet be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            if (emShouldJoinTx) {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            }

                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Exception e) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001(
                                                                                     TestExecutionContext testExecCtx,
                                                                                     TestExecutionResources testExecResources,
                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 5 points, 6 if emShouldJoinTx==true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not
                    // yet be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            if (emShouldJoinTx) {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            }

                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001(
                                                                                     TestExecutionContext testExecCtx,
                                                                                     TestExecutionResources testExecResources,
                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 5 points, 6 if emShouldJoinTx==true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not
                    // yet be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            if (emShouldJoinTx) {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            }

                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Exception e) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

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
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-required business method
     * calls a tx-required business method on SLSB#2, which has an Unsynchronized CMTS EntityManager
     *
     *
     * CMTS #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 4
     */
    public void testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager should already
                    // be joined to the Transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // Test Logic to drive Bean #2 (2 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            Assert.assertTrue("Assert that the EntityManager thinks the same Entity Object is managed.",
                                              em.contains(newEntity));
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

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
     * Scenario: SLSB#1 with Unsynchronized CMTS EntityManager using a tx-required business method
     * calls a tx-required business method on SLSB#2, which has an Synchronized CMTS EntityManager.
     * Trying to propagate an Unsynchronized EntityManager to a Synchronized EntityManager should
     * result in an InvalidStateException being thrown.
     *
     * Variants: Configuration property permits joining/not joining the transaction.
     *
     * CMTS #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 4 (5 if emShouldJoinTx == true)
     */
    public void testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points, 5 if emShouldJoinTx == true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not already
                    // be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (1 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            try {
                                // Poke the EntityManager, it should throw an InvalidStateException
                                em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by invoking the Synchronized EntityManager...");
                            } catch (Exception e) {
                                assertExceptionIsInChain(IllegalStateException.class, e);
                            }
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points, 5 if emShouldJoinTx == true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not already
                    // be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (1 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            try {
                                // Poke the EntityManager, it should throw an InvalidStateException
                                em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by invoking the Synchronized EntityManager...");
                            } catch (Exception e) {
                                assertExceptionIsInChain(IllegalStateException.class, e);
                            }
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points, 5 if emShouldJoinTx == true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not already
                    // be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (1 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            try {
                                // Poke the EntityManager, it should throw an InvalidStateException
                                em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by invoking the Synchronized EntityManager...");
                            } catch (Exception e) {
                                assertExceptionIsInChain(IllegalStateException.class, e);
                            }
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001(
                                                                                   TestExecutionContext testExecCtx,
                                                                                   TestExecutionResources testExecResources,
                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 4 points, 5 if emShouldJoinTx == true)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Unsynchronized EntityManager should not already
                    // be joined to the Transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    // After the em invocation, make sure the EntityManager is still not joined.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    if (emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (1 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            try {
                                // Poke the EntityManager, it should throw an InvalidStateException
                                em.find(SimpleVersionedEntity10.class, identity);
                                Assert.fail("No Exception was thrown by invoking the Synchronized EntityManager...");
                            } catch (Exception e) {
                                assertExceptionIsInChain(IllegalStateException.class, e);
                            }
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with CMTS #1,
     * and a call is made to a second component, that while the persistence context is propagated, that
     * the second component's use of an EntityManager that is of a different presistence unit does not
     * have its EMInvocation overridden.
     *
     * Variants: Bean #1 and Bean #2 Synchronized/Unsynchronized EntityManager driven by properties
     * Variants: Configuration property permits joining/not joining the transaction.
     *
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 6 points, 7 if em1IsUnsynchronized && emShouldJoinTx
     */
    public void testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001(
                                                                              TestExecutionContext testExecCtx,
                                                                              TestExecutionResources testExecResources,
                                                                              Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        boolean em1IsUnsynchronized_pval = false;
        String em1IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean1.em.isunsynchronized");
        if (em1IsUnsynchronizedStr != null) {
            em1IsUnsynchronized_pval = Boolean.parseBoolean(em1IsUnsynchronizedStr);
        }
        final boolean em1IsUnsynchronized = em1IsUnsynchronized_pval;

        boolean em2IsUnsynchronized_pval = false;
        String em2IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean2.em.isunsynchronized");
        if (em2IsUnsynchronizedStr != null) {
            em2IsUnsynchronized_pval = Boolean.parseBoolean(em2IsUnsynchronizedStr);
        }
        final boolean em2IsUnsynchronized = em2IsUnsynchronized_pval;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 6 points, 7 if em1IsUnsynchronized && emShouldJoinTx)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    if (em1IsUnsynchronized && emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (3 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            SimpleVersionedEntity10 emFind = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNull("Assert that the em.find() operation on em with pu#2 returned null.", emFind);

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized == true ? TargetEntityManager.TXSYNC2_UNSYNCHRONIZED : TargetEntityManager.TXSYNC2_SYNCHRONIZED));
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver,
                                                            (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001(
                                                                              TestExecutionContext testExecCtx,
                                                                              TestExecutionResources testExecResources,
                                                                              Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        boolean em1IsUnsynchronized_pval = false;
        String em1IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean1.em.isunsynchronized");
        if (em1IsUnsynchronizedStr != null) {
            em1IsUnsynchronized_pval = Boolean.parseBoolean(em1IsUnsynchronizedStr);
        }
        final boolean em1IsUnsynchronized = em1IsUnsynchronized_pval;

        boolean em2IsUnsynchronized_pval = false;
        String em2IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean2.em.isunsynchronized");
        if (em2IsUnsynchronizedStr != null) {
            em2IsUnsynchronized_pval = Boolean.parseBoolean(em2IsUnsynchronizedStr);
        }
        final boolean em2IsUnsynchronized = em2IsUnsynchronized_pval;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 6 points, 7 if em1IsUnsynchronized && emShouldJoinTx)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    if (em1IsUnsynchronized && emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (3 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            SimpleVersionedEntity10 emFind = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNull("Assert that the em.find() operation on em with pu#2 returned null.", emFind);

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized == true ? TargetEntityManager.TXSYNC2_UNSYNCHRONIZED : TargetEntityManager.TXSYNC2_SYNCHRONIZED));
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001(
                                                                              TestExecutionContext testExecCtx,
                                                                              TestExecutionResources testExecResources,
                                                                              Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        boolean em1IsUnsynchronized_pval = false;
        String em1IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean1.em.isunsynchronized");
        if (em1IsUnsynchronizedStr != null) {
            em1IsUnsynchronized_pval = Boolean.parseBoolean(em1IsUnsynchronizedStr);
        }
        final boolean em1IsUnsynchronized = em1IsUnsynchronized_pval;

        boolean em2IsUnsynchronized_pval = false;
        String em2IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean2.em.isunsynchronized");
        if (em2IsUnsynchronizedStr != null) {
            em2IsUnsynchronized_pval = Boolean.parseBoolean(em2IsUnsynchronizedStr);
        }
        final boolean em2IsUnsynchronized = em2IsUnsynchronized_pval;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 6 points, 7 if em1IsUnsynchronized && emShouldJoinTx)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    if (em1IsUnsynchronized && emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (3 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            SimpleVersionedEntity10 emFind = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNull("Assert that the em.find() operation on em with pu#2 returned null.", emFind);

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized == true ? TargetEntityManager.TXSYNC2_UNSYNCHRONIZED : TargetEntityManager.TXSYNC2_SYNCHRONIZED));
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequired(bean1Driver,
                                                            (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001(
                                                                              TestExecutionContext testExecCtx,
                                                                              TestExecutionResources testExecResources,
                                                                              Object managedComponentObject) throws Throwable {
        final String testName = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";

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

        // Permit a test variable drive-able by the XML definition on whether the EntityManager should join the tx.
        // True or False the results should be the same.
        boolean emShouldJoinTx_pval = false;
        String shouldJoinTxStr = (String) testExecCtx.getProperties().get("test.em.should.join.ex");
        if (shouldJoinTxStr != null) {
            emShouldJoinTx_pval = Boolean.parseBoolean(shouldJoinTxStr);
        }
        final boolean emShouldJoinTx = emShouldJoinTx_pval;

        boolean em1IsUnsynchronized_pval = false;
        String em1IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean1.em.isunsynchronized");
        if (em1IsUnsynchronizedStr != null) {
            em1IsUnsynchronized_pval = Boolean.parseBoolean(em1IsUnsynchronizedStr);
        }
        final boolean em1IsUnsynchronized = em1IsUnsynchronized_pval;

        boolean em2IsUnsynchronized_pval = false;
        String em2IsUnsynchronizedStr = (String) testExecCtx.getProperties().get("test.bean2.em.isunsynchronized");
        if (em2IsUnsynchronizedStr != null) {
            em2IsUnsynchronized_pval = Boolean.parseBoolean(em2IsUnsynchronizedStr);
        }
        final boolean em2IsUnsynchronized = em2IsUnsynchronized_pval;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Test logic to drive Bean #1 (total 6 points, 7 if em1IsUnsynchronized && emShouldJoinTx)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
                    newEntity.setIntData(42);

                    System.out.println("Persisting new entity ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));

                    if (em1IsUnsynchronized) {
                        Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                    } else {
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    if (em1IsUnsynchronized && emShouldJoinTx) {
                        // Bonus point if emShouldJoinTx == true ...
                        System.out.println("Joining the EntityManager to the transaction ...");
                        em.joinTransaction();
                        Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                    }

                    // Test Logic to drive Bean #2 (3 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            SimpleVersionedEntity10 emFind = em.find(SimpleVersionedEntity10.class, identity);
                            Assert.assertNull("Assert that the em.find() operation on em with pu#2 returned null.", emFind);

                            if (em2IsUnsynchronized) {
                                Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());
                            } else {
                                Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());
                            }

                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized == true ? TargetEntityManager.TXSYNC2_UNSYNCHRONIZED : TargetEntityManager.TXSYNC2_SYNCHRONIZED));
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does not begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-notsupported business method
     * calls a tx-notsupported business method on SLSB#2, which also has a Synchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does not begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call.
     *
     * Scenario: SLSB#1 with Unynchronized CMTS EntityManager using a tx-notsupported business method
     * calls a tx-notsupported business method on SLSB#2, which also has an Unsynchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                       TestExecutionContext testExecCtx,
                                                                                                       TestExecutionResources testExecResources,
                                                                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                       TestExecutionContext testExecCtx,
                                                                                                       TestExecutionResources testExecResources,
                                                                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                       TestExecutionContext testExecCtx,
                                                                                                       TestExecutionResources testExecResources,
                                                                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                       TestExecutionContext testExecCtx,
                                                                                                       TestExecutionResources testExecResources,
                                                                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does not begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-notsupported business method
     * calls a tx-notsupported business method on SLSB#2, which has an Unsynchronized CMTS EntityManager
     * If the pctx were propagated this would result in an Exception. However, no propagation should result
     * with no exception
     *
     * CMTS #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call
     * when the second bean's business method is tx-notsupported.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-required business method
     * calls a tx-notsupported business method on SLSB#2, which also has a Synchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager be joined
                    // to any transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager be joined
                    // to any transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-REQUIRED business method, so the Synchronized EntityManager be joined
                    // to any transaction.
                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call
     * when the second bean's business method is tx-notsupported.
     *
     * Scenario: SLSB#1 with Unynchronized CMTS EntityManager using a tx-requiresnew business method
     * calls a tx-notsupported business method on SLSB#2, which also has an Unsynchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call
     * when the second bean's business method is tx-notsupported.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-requiresnew business method
     * calls a tx-notsupported business method on SLSB#2, which has an Unsynchronized CMTS EntityManager
     * If the pctx were propagated this would result in an Exception. However, no propagation should result
     * with no exception
     *
     * CMTS #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (uses transaction NONE on business method)
     *
     * Points: 8
     */
    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx(
                                                                                                   TestExecutionContext testExecCtx,
                                                                                                   TestExecutionResources testExecResources,
                                                                                                   Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 7 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (4 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-none, should be no tx to join to
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With no tx, the entity should not be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
//							Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity2));	// tripped on LCTs again...

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxNotSupported(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxRequiresNew(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does not begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-notsupported business method
     * calls a tx-requires business method on SLSB#2, which also has a Synchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 9
     */
    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required, should be a tx to join to
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx, the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required, should be a tx to join to
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx, the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required, should be a tx to join to
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx, the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                 TestExecutionContext testExecCtx,
                                                                                                 TestExecutionResources testExecResources,
                                                                                                 Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required, should be a tx to join to
                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertTrue("Assert that the EntityManager is joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx, the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component
     *
     * Verify the scenario that with a CMT session bean running a business method that does not begin a
     * transaction, that the persistence context is not propagated to a second bean's business method call.
     *
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-notsupported business method
     * calls a tx-requires business method on SLSB#2, which also has a Synchronized CMTS EntityManager
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     * Points: 9
     */
    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required but em is unsynchronized, should not be joined
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx (even unjoined), the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required but em is unsynchronized, should not be joined
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx (even unjoined), the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            getCMTSCMTSLBuddy().doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SLFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required but em is unsynchronized, should not be joined
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx (even unjoined), the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // No close() call needed for SLSB bean #2
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public void testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx(
                                                                                                     TestExecutionContext testExecCtx,
                                                                                                     TestExecutionResources testExecResources,
                                                                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";

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

        final int identity = rand.nextInt();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Populate the database (1 point)
            {
                System.out.println("Point A: Populate the Database");
                TestWorkRequest twrPopulate = TWR_createSimpleVersionedEntity10("twrPopulate", identity);
                getCMTSCMTSLBuddy().doWorkRequestWithTxRequiresNew(twrPopulate, TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            }

            // Test logic to drive Bean #1 (total 8 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = null;

                    // Running in TX-NONE business method, so the Synchronized EntityManager not be joined
                    // to any transaction.
                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    System.out.println("Creating new SimpleVersionedEntity10(id=" + identity + ") ...");
                    final SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);

                    Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                    // With no tx, the entity should not be managed
                    Assert.assertNotNull("Assert the find operation returned an object.", findEntity);
//					Assert.assertFalse("Assert that the new entity is not managed.", em.contains(findEntity));		// tripped on LCTs again...

                    // Test Logic to drive Bean #2 (5 points)
                    TestWorkRequest bean2Driver = new TestWorkRequest() {
                        @Override
                        public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                            // Since the bean method is tx-required but em is unsynchronized, should not be joined
                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            SimpleVersionedEntity10 findEntity2 = em.find(SimpleVersionedEntity10.class, identity);

                            Assert.assertFalse("Assert that the EntityManager is not joined to the tx.", em.isJoinedToTransaction());

                            //  With a tx (even unjoined), the entity should be managed
                            Assert.assertNotNull("Assert the find operation returned an object.", findEntity2);
                            Assert.assertTrue("Assert that the new entity is managed.", em.contains(findEntity2));

                            // Should be a different entity object instance, if persistence contexts are not propagated
                            Assert.assertNotSame("Assert entities found by both persistence contexts are not the same.",
                                                 findEntity, findEntity2);
                            return null;
                        }
                    };

                    try {
                        // Invoke SLSB Bean #2's TX-Required Business method to drive test logic.
                        ic = new InitialContext();
                        bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
                        bean2.doWorkRequestWithTxRequiresNew(bean2Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        try {
                            if (ic != null) {
                                ic.close();
                            }
                        } catch (Exception e) {
                            // Swallow
                        }

                        // Close() call needed for SLSB bean #2
                        if (bean2 != null) {
                            try {
                                bean2.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }
                    }

                    return null;
                }
            };

            TxSyncCMTSFBuddyLocal bean1 = getCMTSCMTSFBuddy();
            if (bean1 != null) {
                bean1.doWorkRequestWithTxNotSupported(bean1Driver, TargetEntityManager.TXSYNC1_UNSYNCHRONIZED);
                try {
                    bean1.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

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

    /*
     *
     */

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
