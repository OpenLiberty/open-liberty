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

package com.ibm.ws.jpa.fvt.txsync.tests.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.TransactionSynchronization;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationCMTSSpecificEJBSFTestServlet")
public class TxSynchronizationCMTSSpecificEJBSFTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.cm.CMTSAndBMTSpecificTxSynchronizationTestLogic";
        ejbJNDIName = "ejb/TxSyncBMTSFEJB";

        jpaPctxMap.put("test-jpa-resource-txsync",
                       new JPAPersistenceContext("test-jpa-resource-txsync", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "emCMTSTxSync", TransactionSynchronization.SYNCHRONIZED));
        jpaPctxMap.put("test-jpa-resource-txunsync",
                       new JPAPersistenceContext("test-jpa-resource-txunsync", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "emCMTSTxUnsync", TransactionSynchronization.UNSYNCHRONIZED));

    }

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
     * • If an entity manager is then invoked from within the component:
     * • Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will result in use of a
     * new persistence context (as described in section 7.6.2).
     * • Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will result in the use of
     * the existing extended persistence context bound to that component.
     * • If the entity manager is invoked within a JTA transaction, the persistence context will be associated
     * with the JTA transaction.
     *
     * If a component is called and the JTA transaction is propagated into that component:
     * • If the component is a stateful session bean to which an extended persistence context has been bound and
     * there is a different persistence context associated with the JTA transaction, an EJBException is thrown by
     * the container.
     * • If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
     * transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
     * the IllegalStateException is thrown by the container.
     * • Otherwise, if there is a persistence context associated with the JTA transaction, that persistence context is
     * propagated and used.
     *
     * Note that a component with a persistence context of type Synchronization- Type.UNSYNCHRONIZED may be called by a
     * component propagating either a persistence context of type SynchronizationType.UNSYNCHRONIZED or a persistence
     * context of type SynchronizationType.SYNCHRONIZED into it.
     *
     * [84] Entitymanager instances obtained from different entitymanagerfactories never share the same persistence context.
     */

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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_001_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_001_Sync_EJB_CMTS_SL";
        final String testMethod = "testCMTSEntityManagerScope001";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSEntityManagerScope001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Sync_EJB_CMTS_SL";
        final String testMethod = "testCMTSEntityManagerScope002";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Unsync_NoJoinTx_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Unsync_NoJoinTx_EJB_CMTS_SL";
        final String testMethod = "testCMTSEntityManagerScope002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Unsync_JoinTx_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSEntityManagerScope_002_Unsync_JoinTx_EJB_CMTS_SL";
        final String testMethod = "testCMTSEntityManagerScope002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSPersistContract001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_002_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_002_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSPersistContract002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_003_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_003_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSPersistContract003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_004_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSPersistContract_004_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSPersistContract004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * Test Variant: EntityManager.merge()
     * em.merge() invoked before em.joinTransaction()
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSMergeContract_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSMergeContract_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSMergeContract001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * Test Variant: EntityManager.merge()
     * em.merge() invoked after em.joinTransaction()
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSMergeContract_002_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSMergeContract_002_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSMergeContract002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * Test Variant: EntityManager.remove()
     * em.remove() invoked before em.joinTransaction()
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRemoveContract_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRemoveContract_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSRemoveContract001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * Test Variant: EntityManager.remove()
     * em.remove() invoked after em.joinTransaction()
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRemoveContract_002_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRemoveContract_002_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSRemoveContract002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that with an Unsynchronized CMTS Persistence Context:
     *
     * The persistence context is created and then associated with the JTA transaction. This association of the
     * persistence context with the JTA transaction is independent of the synchronization type of the persistence
     * context and whether the persistence context has been joined to the transaction.
     *
     * Test Variant: EntityManager.refresh()
     * em.refresh() invoked without an active jta tran will throw TransactionRequiredException
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRefreshContract_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testCMTSRefreshContract_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testCMTSRefreshContract001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001A_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001A_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001A_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001A_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001B_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001B_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001B";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001B_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001B_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001C_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001C_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001C";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001C_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_001C_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation001C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation002";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002A_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002A_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation002A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002A_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_002A_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003A_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003B";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003B_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003C";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_003C_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation003C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagationNoPropagation_004A_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagationNoPropagation004A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagation001";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagation001A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_001A_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * • If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * • If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002A_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002A_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002A_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_002A_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * • If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_003_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_003_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagation003";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagation004";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_004_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * • If the component is a stateful session bean to which an extended persistence context has been
     * bound and there is a different persistence context associated with the JTA transaction, an
     * EJBException is thrown by the container.
     *
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with an
     * a transaction scope persistence context, and attempts to propagate that persistence context
     * to a stateful session bean with a extended persistence context associated with the same
     * persistence unit, that an EJBException is thrown.
     *
     * Tests SYNCHRONIZED EntityManagers
     *
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Sync_Sync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Sync_Sync_EJB_CMTS_SL";
        final String testMethod = "testPropagation005";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Sync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Sync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation005";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Sync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Sync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation005";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * • If the component is a stateful session bean to which an extended persistence context has been
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
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Sync_Unsync_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Sync_Unsync_EJB_CMTS_SL";
        final String testMethod = "testPropagation005A";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Unsync_Join_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Unsync_Join_EJB_CMTS_SL";
        final String testMethod = "testPropagation005A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Unsync_Nojoin_EJB_CMTS_SL() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMTSSpecific_testPropagation_005_Unsync_Unsync_Nojoin_EJB_CMTS_SL";
        final String testMethod = "testPropagation005A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }
}
