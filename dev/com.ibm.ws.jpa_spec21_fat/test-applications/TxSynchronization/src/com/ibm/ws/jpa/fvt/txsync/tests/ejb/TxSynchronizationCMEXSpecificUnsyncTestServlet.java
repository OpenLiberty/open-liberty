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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationCMEXSpecificUnsyncTestServlet")
public class TxSynchronizationCMEXSpecificUnsyncTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.cm.CMEXAndBMTSpecificTxSynchronizationTestLogic";
        ejbJNDIName = "ejb/TxSyncBMTSFEXUnsyncEJB";

        jpaPctxMap.put("test-jpa-resource-txunsync",
                       new JPAPersistenceContext("test-jpa-resource-txunsync", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.FIELD, "emCMEXTxUnsync", TransactionSynchronization.UNSYNCHRONIZED));

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
     * recursively applies—independently of whether transactions are active or not at the point of the creation of the
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
     * Verify that an entity fetched outside a JTA transaction remains managed by an Extended persistence context,
     * regardless of transaction synchronicity and.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_001_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_001_Unsync_EJB_CMEX_SF";
        final String testMethod = "testExtendedPersistence001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that an entity fetched within a JTA transaction remains managed by an Extended persistence context,
     * regardless of transaction synchronicity and, with unsynchronized EntityManagers, whether it joins the
     * transaction, after the JTA transaction commits.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_002_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_002_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testExtendedPersistence002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_002_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_002_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testExtendedPersistence002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * Verify that an entity fetched within a JTA transaction may be detached when that transaction rolls back.
     * Unsynchronized CMEX EntityManagers that did not join the transaction should not have its entities detached.
     * Synchronized and Unsynchronized (that joined the tx) CMEX EntityManagers should have its entities detached.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_003_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_003_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testExtendedPersistence003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_003_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedPersistence_003_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testExtendedPersistence003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that EntityManager.clear() can be invoked outside of a JTA transaction, and that the
     * entity pesisted becomes detached.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_001_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_001_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPersist001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the persist() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_002_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_002_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPersist002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.persist() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the persist method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_003_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_003_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPersist003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_004_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_004_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPersist004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_004_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPersist_004_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPersist004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_001_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_001_Unsync_EJB_CMEX_SF";
        final String testMethod = "testMerge001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the merge() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_002_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_002_Unsync_EJB_CMEX_SF";
        final String testMethod = "testMerge002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.merge() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the merge method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_003_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_003_Unsync_EJB_CMEX_SF";
        final String testMethod = "testMerge003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_004_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_004_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testMerge004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_004_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testMerge_004_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testMerge004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * Verify that EntityManager.remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the remove method invocation.
     *
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_001_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_001_Unsync_EJB_CMEX_SF";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.Remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the Remove method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, but because the Remove() operation occurred
     * before the JTA transaction was started, and no EntityManager operation (ie, flush, joinTransaction)
     * is invoked before it is committed, the data is not saved to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_002_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_002_Unsync_EJB_CMEX_SF";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.Remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the Remove method invocation.
     *
     * Verify that afterwards a JTA transaction can be started, and that the EntityManager can be enlisted
     * to the transaction with a joinTransaction() operation to save it to the database.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_003_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_003_Unsync_EJB_CMEX_SF";
        final String testMethod = "testRemove003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Verify that EntityManager.Remove() can be invoked outside of a JTA transaction, and that the
     * entity remains managed after the Remove method invocation.
     *
     * For SYNCHRONIZED EntityManagers, verify that the new entity was saved after the JTA tran commits.
     * For UNSYNCHRONIZED EntityManagers, if the JTA tran was joined then verify that the new entity was
     * saved after the JTA tran commits. If the JTA tran was not joined, then verify that the new entity
     * was not saved after the JTA tran commits.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_004_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_004_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testRemove004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_004_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testRemove_004_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testRemove004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * Verify that, when there is no JTA transaction active, that a persistence context is not propagated.
     *
     * Scenario: CMEX #1 loads an entity, and invokes a second bean that
     * injects a CMTS #2 with the same persistence unit. Verify that the entity loaded by #1 is not
     * managed by #2, thus verifying that the persistence context is not propagated.
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001A_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001A_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001B_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001B_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation001B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001C_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_001C_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation001C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_002_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_002_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_002A_Unsync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_002A_Unsync_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003A_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003A_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003A_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003A_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
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
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMTS #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SL CMT (CM-TS) (uses transaction REQUIRES_NEW to suspend JTA tran)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003B_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003B_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003B_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003B_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003B";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
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
     * Test supports both Synchronized and Unsynchronized CMEX persistence contexts.
     * Test supports both joining and not joining an Unsynchronized CMEX persistence context to the JTA transaction.
     *
     *
     * CMEX #1 and CMTS #2 use the same transaction synchronicity.
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRES_NEW to suspend JTA tran)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003C_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003C_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003C_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_003C_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation003C";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004A_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004A_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation004A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004A_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagationNoPropagation_004A_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagationNoPropagation004A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * Second Bean is SL CMT (CMTS) (uses transaction REQUIRED on business method)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagation001";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001A_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001A_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001A_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_001A_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagation001A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * ¥ If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagation002";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * ¥ If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002A_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002A_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002A_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_002A_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagation002A";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_004_Unsync_Nojoin_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_004_Unsync_Nojoin_EJB_CMEX_SF";
        final String testMethod = "testPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_004_Unsync_Join_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testPropagation_004_Unsync_Join_EJB_CMEX_SF";
        final String testMethod = "testPropagation004";
        final String testResource = "test-jpa-resource-txunsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        executeTest(testName, testMethod, testResource, props);
    }
}
