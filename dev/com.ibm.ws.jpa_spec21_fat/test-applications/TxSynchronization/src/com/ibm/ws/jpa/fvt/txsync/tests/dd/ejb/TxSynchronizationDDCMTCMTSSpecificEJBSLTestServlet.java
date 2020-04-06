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

package com.ibm.ws.jpa.fvt.txsync.tests.dd.ejb;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationDDCMTCMTSSpecificEJBSLTestServlet")
public class TxSynchronizationDDCMTCMTSSpecificEJBSLTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.cm.CMTSAndCMTSpecificTxSynchronizationTestLogic";
        ejbJNDIName = "ejb/BasicRunnerSLEJB";
    }

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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__00() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__00";
        final String testMethod = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__00() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__00";
        final String testMethod = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__00() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__00";
        final String testMethod = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__00() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__00";
        final String testMethod = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
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
     * Scenario: SLSB#1 with Synchronized CMTS EntityManager using a tx-required business method
     * calls a tx-required business method on SLSB#2, which has an Unsynchronized CMTS EntityManager
     *
     *
     * CMTS #1 and CMTS #2 use different transaction synchronicity.
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testMethod = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testMethod = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testMethod = "testPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testMethod = "testPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_jointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_jointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_nojointx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001_nojointx";
        final String testMethod = "testPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_sync";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_sync_unsync_join";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001_unsync_sync_join";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSLSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_sync";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_join";
        final String testMethod = "testPropagation_CMT_SLSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_sync";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_nojoin";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_sync_unsync_join";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_sync_join";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "false");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001_unsync_unsync_join";
        final String testMethod = "testPropagation_CMT_SFSB_CMTS__To__CMT_SSFSB_CMTS_DIFFPU__001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1Tx_Bean2NoTx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SLSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSLSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SFSB_SYNC_CMTS__To__CMT_SSFSB_SYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SLSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSLSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMTSSpecific_testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testMethod = "testNoPropagation_CMT_SFSB_UNSYNC_CMTS__To__CMT_SSFSB_UNSYNC_CMTS__Bean1NoTx_Bean2Tx";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

}
