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
import java.util.ArrayList;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTEXSyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFEXBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFEXSyncBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSLBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.testlogic.AbstractTxSyncTestLogic;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TargetEntityManager;
import com.ibm.ws.jpa.fvt.txsync.testlogic.TestWorkRequest;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class CMEXAndCMTSpecificTxSynchronizationTestLogic extends AbstractTxSyncTestLogic {
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
    private final static String testBucketName = CMEXAndCMTSpecificTxSynchronizationTestLogic.class.getName();

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited without an active JTA transaction.
     * - Variants: SYNCHRONIZED or UNSYNCHRONIZED EntityManager (both beans are the same)
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
     * Bean1 - CMT
     * Bean2 - BMT
     *
     * 4 Points
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
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = TWR_testContains("VerifyCMEXInheritance:" + testName + ":Bean2", entityContainer, true);

            // Test logic to drive Bean #1 (total 3 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncBMTEXSyncBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                        }

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequest(bean2Driver,
                                            (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited without an active JTA transaction.
     * - Variants: SYNCHRONIZED or UNSYNCHRONIZED EntityManager (both beans are the same)
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
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-notsupported)
     *
     * 4 Points
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
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = TWR_testContains("VerifyCMEXInheritance:" + testName + ":Bean2", entityContainer, true);

            // Test logic to drive Bean #1 (total 3 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                        }

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited with an active JTA transaction.
     * - Variants: SYNCHRONIZED or UNSYNCHRONIZED EntityManager (both beans are the same)
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
     * Bean1 - CMT
     * Bean2 - BMT
     *
     * 4 Points
     */
    public void testExtendedScopeInheritance003(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance003";

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
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = TWR_testContains("VerifyCMEXInheritance:" + testName + ":Bean2", entityContainer, true);

            // Test logic to drive Bean #1 (total 3 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncBMTEXSyncBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-required so there should be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                        }

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequest(bean2Driver,
                                            (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited with an active JTA transaction.
     * - Variants: SYNCHRONIZED or UNSYNCHRONIZED EntityManager (both beans are the same)
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
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-required)
     *
     * 4 Points
     */
    public void testExtendedScopeInheritance004(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance004";

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
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = TWR_testContains("VerifyCMEXInheritance:" + testName + ":Bean2", entityContainer, true);

            // Test logic to drive Bean #1 (total 3 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-required so there should be an active tran

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                        }

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that the persistence context is inherited with an active JTA transaction.
     * - Variants: SYNCHRONIZED or UNSYNCHRONIZED EntityManager (both beans are the same)
     * - Verify with a business method call on bean#2 that is tx-notsupported
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
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-notsupported)
     *
     * 4 Points
     */
    public void testExtendedScopeInheritance005(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance005";

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
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = TWR_testContains("VerifyCMEXInheritance:" + testName + ":Bean2", entityContainer, true);

            // Test logic to drive Bean #1 (total 3 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-required so there should be an active tran

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                        }

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a (UN)SYNCHRONIZED version and SFSB#2 using the opposing sync type, that when
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
     * 3 Points
     *
     * Bean1: CMT
     * Bean2: BMT
     */
    public void testExtendedScopeInheritance006(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance006";

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

        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #1 (total 3 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncBMTEXSyncBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                        }

                        Assert.fail("No Exception was thrown by the bean create operation.");
                    } catch (Throwable t) {
                        //  Caught an Exception
                        assertExceptionIsInChain(EJBException.class, t);
                        return null;
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a (UN)SYNCHRONIZED version and SFSB#2 using the opposing sync type, that when
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
     * 3 Points
     *
     * Bean1: CMT
     * Bean2: CMT
     */
    public void testExtendedScopeInheritance007(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance007";

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

        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #1 (total 3 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                        }

                        Assert.fail("No Exception was thrown by the bean create operation.");
                    } catch (Throwable t) {
                        //  Caught an Exception
                        assertExceptionIsInChain(EJBException.class, t);
                        return null;
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a (UN)SYNCHRONIZED version and SFSB#2 using the opposing sync type, that when
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
     * 3 Points
     *
     * Bean1: CMT - will begin a transaction
     * Bean2: BMT
     */
    public void testExtendedScopeInheritance008(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance008";

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

        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #1 (total 3 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncBMTEXSyncBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-required so there should not be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncBMTSFEXSyncBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncBMTEXSyncBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFEXSyncBuddyEJB");
                        }

                        Assert.fail("No Exception was thrown by the bean create operation.");
                    } catch (Throwable t) {
                        //  Caught an Exception
                        assertExceptionIsInChain(EJBException.class, t);
                        return null;
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify Extended Persistence Context Inheritance.
     * - Verify that two SFSBs with an Ex-PC of the same Persistence Unit, with SFSB#1 using
     * a (UN)SYNCHRONIZED version and SFSB#2 using the opposing sync type, that when
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
     * 3 Points
     *
     * Bean1: CMT - will begin transaction
     * Bean2: CMT
     */
    public void testExtendedScopeInheritance009(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        final String testName = "testExtendedScopeInheritance009";

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

        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

        // Execute Test Case
        try {
            System.out.println(testBucketName + "." + testName + ": Begin");
//            cleanupDatabase(jpaCleanupResource);

            // Create Entity Object for test
            System.out.println("Creating SimpleVersionedEntity10(id=" + identity + ") ...");
            final SimpleVersionedEntity10 newEntity = new SimpleVersionedEntity10(identity);
            newEntity.setStrData("Simple String");
            entityContainer.add(newEntity);

            // Test logic to drive Bean #1 (total 3 points)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = null;

                    // Populate the database (2 points) to verify the usability of the CM-EX EntityManager
                    System.out.println("Persisting SimpleVersionedEntity10(id=" + identity + ") ...");
                    em.persist(newEntity);
                    Assert.assertTrue("Assert that the entity is managed.", em.contains(newEntity));

                    // Check tx join status, business method is tx-required so there should be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
                        if (em2IsUnsynchronized) {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                        } else {
                            bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                        }

                        Assert.fail("No Exception was thrown by the bean create operation.");
                    } catch (Throwable t) {
                        //  Caught an Exception
                        assertExceptionIsInChain(EJBException.class, t);
                        return null;
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                        // Cleanup
                        if (ic != null) {
                            try {
                                ic.close();
                            } catch (Throwable t) {
                                // Swallow
                            }
                        }

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

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
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
     * Second Bean is SL CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation001(
                                                       TestExecutionContext testExecCtx,
                                                       TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation001";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
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
     * Second Bean is SF CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation001A(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation001A";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
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
     * injects a CMEX #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMEX #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-EX)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation001B(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation001B";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFEXBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFEXBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFEXBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");

                if (em2IsUnsynchronized) {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
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
     * CMEX #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation002(
                                                       TestExecutionContext testExecCtx,
                                                       TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation002";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
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
     * CMEX #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SF CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation002A(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation002A";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
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
     * injects a CMEX #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMEX #2 use different transaction synchronicity.
     * Second Bean is SF CMT (CM-EX)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation002B(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation002B";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFEXBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFEXBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//					if (em1IsUnsynchronized) {
//						if (emShouldJoinTx) {
//							System.out.println("Joining Unsynchronized EntityManager to the transaction...");
//							em.joinTransaction();
//							Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//						} else {
//							Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
//						}
//					} else {
//						Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
//					}

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNever(bean2Driver,
                                                       (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFEXBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");

                if (em2IsUnsynchronized) {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxNever(bean1Driver,
                                               (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS) (is tx-notsupported)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation003(
                                                       TestExecutionContext testExecCtx,
                                                       TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation003";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation003A(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation003A";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMEX #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMEX #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-EX)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation003B(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation003B";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFEXBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFEXBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFEXBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");

                if (em2IsUnsynchronized) {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation004(
                                                       TestExecutionContext testExecCtx,
                                                       TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation004";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    // Check tx join status, business method is tx-never so there should not be an active tran
                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SF CMT (CM-TS)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation004A(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation004A";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    /**
     * Verify that, when there is a JTA transaction active but the transaction is suspended by
     * a tx-notsupported business method, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMEX #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMEX #1 and CMEX #2 use different transaction synchronicity.
     * Second Bean is SF CMT (CM-EX)
     *
     * - Variants: Transaction Synchronicity
     *
     * 8 points
     */
    public void testNoPersistenceContextPropagation004B(
                                                        TestExecutionContext testExecCtx,
                                                        TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = "testNoPersistenceContextPropagation004B";

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
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFEXBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFEXBuddyLocal>();

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

            // Test logic to drive Bean #2 (total 3 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

                        Assert.assertNotSame("Assert that find did not return the same entity.", entityContainer.get(0), findEntity);
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 7 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFEXBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

//						System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");
//						if (em2IsUnsynchronized) {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
//						} else {
//							bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
//						}
//
//                   	 	Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
//                   	 	if (bean2 == null) {
//                   	 		return null;
//                   	 	}

                        bean2.doWorkRequestWithTxNotSupported(bean2Driver,
                                                              (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean2);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFEXBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #1 ...");
                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFEXBuddyEJB #2 ...");

                if (em2IsUnsynchronized) {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean2 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

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
     * Points: 9
     */
    public void testPersistenceContextPropagation001(
                                                     TestExecutionContext testExecCtx,
                                                     TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation001";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 4 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                        Assert.assertNotNull("Assert find did not return null.", findEntity);

                        if (!em2IsUnsynchronized || emShouldJoinTx)
                            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                        else
                            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                        Assert.assertSame("Assert that find did return the same entity.", entityContainer.get(0), findEntity);

                        Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                          emContainer.get(0),
                                          em.getDelegate());
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 8 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
            }

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
     * Second Bean is SF CMT (CMTS) (uses transaction REQUIRED on business method)
     *
     * Points: 8
     */
    public void testPersistenceContextPropagation001A(
                                                      TestExecutionContext testExecCtx,
                                                      TestExecutionResources testExecResources,
                                                      Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation001A";

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
        final boolean em2IsUnsynchronized = em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 4 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                        Assert.assertNotNull("Assert find did not return null.", findEntity);

                        if (!em2IsUnsynchronized || emShouldJoinTx)
                            Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                        else
                            Assert.assertFalse("Assert that the EntityManager is not associated with a transaction.", em.isJoinedToTransaction());

                        Assert.assertSame("Assert that find did return the same entity.", entityContainer.get(0), findEntity);

                        Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                          emContainer.get(0),
                                          em.getDelegate());
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 8 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                      (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
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
     * Test supports both joining and not joining the Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SL CMT (CM-TS) (uses transaction REQUIRED on business method)
     *
     * Points: 6
     */
    public void testPersistenceContextPropagation002(
                                                     TestExecutionContext testExecCtx,
                                                     TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation002";

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

        final boolean em1IsUnsynchronized = true;
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

            // Test logic to drive Bean #1 (total 8 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
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
     * Test supports both joining and not joining the Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 is unsynchronized,CMTS #2 is synchronized
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRED on business method)
     *
     * Points: 6
     */
    public void testPersistenceContextPropagation002A(
                                                      TestExecutionContext testExecCtx,
                                                      TestExecutionResources testExecResources,
                                                      Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation002A";

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

        final boolean em1IsUnsynchronized = true;
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
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

            // Test logic to drive Bean #1 (total 5 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                      (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
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
     * Points: 9
     */
    public void testPersistenceContextPropagation003(
                                                     TestExecutionContext testExecCtx,
                                                     TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation003";

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

        final boolean em1IsUnsynchronized = false;
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 4 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                        Assert.assertNotNull("Assert find did not return null.", findEntity);

                        Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                        Assert.assertSame("Assert that find did return the same entity.", entityContainer.get(0), findEntity);
                        Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                          emContainer.get(0),
                                          em.getDelegate());
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 8 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSLBuddyLocal bean2 = null;

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    // Create Bean #2 (also a TxSyncBMTSFEXSyncBuddyEJB) and Verify that the persistence context is inherited.  (2 points)
                    try {
                        ic = new InitialContext();

                        System.out.println("Creating TxSyncCMTSLBuddyEJB #2 ...");
                        bean2 = (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");

                        Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                        if (bean2 == null) {
                            return null;
                        }

                        bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                          (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));

                    } catch (java.lang.AssertionError ae) {
                        throw ae;
                    } catch (Throwable t) {
                        // Catch any Exceptions thrown by the test case for proper error logging.
                        Assert.fail("Caught an unexpected Exception during test execution." + t);
                    } finally {
                        CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic);
                    }
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
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
     * Second Bean is SF CMT (uses transaction REQUIRED on business method)
     *
     * Points: 9
     */
    public void testPersistenceContextPropagation003A(
                                                      TestExecutionContext testExecCtx,
                                                      TestExecutionResources testExecResources,
                                                      Object managedComponentObject) throws Throwable {
        final String testName = "testPersistenceContextPropagation0023A";

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

        final boolean em1IsUnsynchronized = false;
        final boolean em2IsUnsynchronized = !em1IsUnsynchronized;

        System.out.println("emShouldJoinTx = " + emShouldJoinTx);
        System.out.println("em1IsUnsynchronized = " + em1IsUnsynchronized);
        System.out.println("em2IsUnsynchronized = " + em2IsUnsynchronized);

        final int identity = rand.nextInt();
        final ArrayList<SimpleVersionedEntity10> entityContainer = new ArrayList<SimpleVersionedEntity10>();
        final ArrayList<TxSyncCMTSFBuddyLocal> bean2Container = new ArrayList<TxSyncCMTSFBuddyLocal>();
        final ArrayList<Object> emContainer = new ArrayList<Object>();

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

            // Test logic to drive Bean #2 (total 1 points)
            final TestWorkRequest bean2Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em,
                                               UserTransaction tx, Object managedComponentObject) {
                    System.out.println("Begin Running TestWorkRequest.doTestWork(twr) on " + managedComponentObject + " ...");

                    try {
                        System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                        SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                        Assert.assertNotNull("Assert find did not return null.", findEntity);

                        Assert.assertTrue("Assert that the EntityManager is associated with a transaction.", em.isJoinedToTransaction());
                        Assert.assertSame("Assert that find did return the same entity.", entityContainer.get(0), findEntity);

                        Assert.assertSame("Assert that the delegate EntityManager used in components 1 and 2 are the same",
                                          emContainer.get(0),
                                          em.getDelegate());
                    } finally {
                        System.out.println("End Running TestWorkRequest.doTestWork() on " + managedComponentObject + " ...");
                    }

                    return null;
                }
            };

            // Test logic to drive Bean #1 (total 5 points - including bean2 work)
            TestWorkRequest bean1Driver = new TestWorkRequest() {
                @Override
                public Serializable doTestWork(EntityManager em, UserTransaction tx, Object managedComponentObject) {
                    InitialContext ic = null;
                    TxSyncCMTSFBuddyLocal bean2 = bean2Container.get(0);

                    System.out.println("Finding SimpleVersionedEntity10(id=" + identity + ") ...");
                    SimpleVersionedEntity10 findEntity = em.find(SimpleVersionedEntity10.class, identity);
                    Assert.assertNotNull("Assert find did not return null.", findEntity);
                    Assert.assertTrue("Assert that entity is managed because of LTC.", em.contains(findEntity));
                    entityContainer.add(findEntity);
                    emContainer.add(em.getDelegate());

                    if (em1IsUnsynchronized) {
                        if (emShouldJoinTx) {
                            System.out.println("Joining Unsynchronized EntityManager to the transaction...");
                            em.joinTransaction();
                            Assert.assertTrue("Assert that the Unsynchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                        } else {
                            Assert.assertFalse("Assert that the Unsynchronized EntityManager is not joined to a transaction.", em.isJoinedToTransaction());
                        }
                    } else {
                        Assert.assertTrue("Assert that the Synchronized EntityManager is joined to a transaction.", em.isJoinedToTransaction());
                    }

                    bean2.doWorkRequestWithTxRequired(bean2Driver,
                                                      (em2IsUnsynchronized ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED));
                    return null;
                }
            };

            InitialContext ic = null;
            TxSyncCMTSFEXBuddyLocal bean1 = null;
            TxSyncCMTSFBuddyLocal bean2 = null;
            try {
                ic = new InitialContext();

                if (em1IsUnsynchronized) {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXUnsyncBuddyEJB");
                } else {
                    bean1 = (TxSyncCMTSFEXBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFEXSyncBuddyEJB");
                }

                System.out.println("Creating TxSyncCMTSFBuddyEJB #2 ...");
                bean2 = (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");

                Assert.assertNotNull("Assert bean2 is not null (lookup & create succeeded).", bean2);
                if (bean2 == null) {
                    return;
                }
                bean2Container.add(bean2);

                bean1.doWorkRequestWithTxRequired(bean1Driver,
                                                  (em1IsUnsynchronized == true) ? TargetEntityManager.TXSYNC1_UNSYNCHRONIZED : TargetEntityManager.TXSYNC1_SYNCHRONIZED);
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Catch any Exceptions thrown by the test case for proper error logging.
                Assert.fail("Caught an unexpected Exception during test execution." + t);
            } finally {
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(ic, bean1);
                CMEXAndCMTSpecificTxSynchronizationTestLogic.cleanup(null, bean2);
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println(testBucketName + "." + testName + ": End");
        }
    }

    public static void cleanup(InitialContext ic) {
        if (ic != null) {
            try {
                ic.close();
            } catch (Throwable t) {
                // Swallow
            }
        }
    }

    public static void cleanup(InitialContext ic, TxSyncCMTSFBuddyLocal bean) {
        if (ic != null) {
            try {
                ic.close();
            } catch (Throwable t) {
                // Swallow
            }
        }

        if (bean != null) {
            try {
                bean.close();
            } catch (Throwable t) {
                // Swallow
            }
        }
    }

    public static void cleanup(InitialContext ic, TxSyncBMTEXSyncBuddyLocal bean) {
        if (ic != null) {
            try {
                ic.close();
            } catch (Throwable t) {
                // Swallow
            }
        }

        if (bean != null) {
            try {
                bean.close();
            } catch (Throwable t) {
                // Swallow
            }
        }
    }

    public static void cleanup(InitialContext ic, TxSyncCMTSFEXSyncBuddyLocal bean) {
        if (ic != null) {
            try {
                ic.close();
            } catch (Throwable t) {
                // Swallow
            }
        }

        if (bean != null) {
            try {
                bean.close();
            } catch (Throwable t) {
                // Swallow
            }
        }
    }

    public static void cleanup(InitialContext ic, TxSyncCMTSFEXBuddyLocal bean) {
        if (ic != null) {
            try {
                ic.close();
            } catch (Throwable t) {
                // Swallow
            }
        }

        if (bean != null) {
            try {
                bean.close();
            } catch (Throwable t) {
                // Swallow
            }
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

    /**
     * 1 Point
     *
     * @param identifier
     * @param entityContainer
     * @param expectContains
     * @return
     */
    private TestWorkRequest TWR_testContains(String identifier, ArrayList<SimpleVersionedEntity10> entityContainer, boolean expectContains) {
        if (entityContainer == null || entityContainer.isEmpty()) {
            return null;
        }

        SimpleVersionedEntity10 testEnt = entityContainer.get(0);
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
