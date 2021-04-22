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

import componenttest.annotation.ExpectedFFDC;

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
 * recursively appliesÑindependently of whether transactions are active or not at the point of the creation of the
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
 *    ¥ If an entity manager is then invoked from within the component:
 *       ¥ Invocation of an entity manager defined with PersistenceContext-Type.TRANSACTION will result in use of a
 *         new persistence context (as described in section 7.6.2).
 *       ¥ Invocation of an entity manager defined with PersistenceContext-Type.EXTENDED will result in the use of
 *         the existing extended persistence context bound to that component.
 *       ¥ If the entity manager is invoked within a JTA transaction, the persistence context will be associated
 *         with the JTA transaction.
 *
 * If a component is called and the JTA transaction is propagated into that component:
 *    ¥ If the component is a stateful session bean to which an extended persistence context has been bound and
 *      there is a different persistence context associated with the JTA transaction, an EJBException is thrown by
 *      the container.
 *    ¥ If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
 *      transaction and the target component specifies a persistence context of type SynchronizationType.SYNCHRONIZED,
 *      the IllegalStateException is thrown by the container.
 *    ¥ Otherwise, if there is a persistence context associated with the JTA transaction, that persistence context is
 *      propagated and used.
 *
 * Note that a component with a persistence context of type Synchronization- Type.UNSYNCHRONIZED may be called by a
 * component propagating either a persistence context of type SynchronizationType.UNSYNCHRONIZED or a persistence
 * context of type SynchronizationType.SYNCHRONIZED into it.
 *
 * [84] Entitymanager instances obtained from different entitymanagerfactories never share the same persistence context.
 */

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationDDCMTCMEXSpecificEJBSLTestServlet")
public class TxSynchronizationDDCMTCMEXSpecificEJBSLTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.cm.CMEXAndCMTSpecificTxSynchronizationTestLogic";
        ejbJNDIName = "ejb/BasicRunnerSLEJB";
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1 - CMT
     * Bean2 - BMT
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance001_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance001_unsync";
        final String testMethod = "testExtendedScopeInheritance001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance001_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance001_sync";
        final String testMethod = "testExtendedScopeInheritance001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-notsupported)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance002_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance002_unsync";
        final String testMethod = "testExtendedScopeInheritance002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance002_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance002_sync";
        final String testMethod = "testExtendedScopeInheritance002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1 - CMT
     * Bean2 - BMT
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance003_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance003_unsync";
        final String testMethod = "testExtendedScopeInheritance003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance003_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance003_sync";
        final String testMethod = "testExtendedScopeInheritance003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-required)
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance004_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance004_unsync";
        final String testMethod = "testExtendedScopeInheritance004";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance004_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance004_sync";
        final String testMethod = "testExtendedScopeInheritance004";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1 - CMT
     * Bean2 - CMT (business method is tx-notsupported)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance005_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance005_unsync";
        final String testMethod = "testExtendedScopeInheritance005";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        props.put("test.bean2.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance005_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance005_sync";
        final String testMethod = "testExtendedScopeInheritance005";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        props.put("test.bean2.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1: CMT
     * Bean2: BMT
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance006_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance006_sync";
        final String testMethod = "testExtendedScopeInheritance006";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance006_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance006_sync";
        final String testMethod = "testExtendedScopeInheritance006";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1: CMT
     * Bean2: CMT
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance007_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance007_sync";
        final String testMethod = "testExtendedScopeInheritance007";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance007_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance007_sync";
        final String testMethod = "testExtendedScopeInheritance007";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1: CMT - will begin a transaction
     * Bean2: BMT
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance008_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance008_sync";
        final String testMethod = "testExtendedScopeInheritance008";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance008_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance008_sync";
        final String testMethod = "testExtendedScopeInheritance008";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     * Bean1: CMT - will begin a transaction
     * Bean2: CMT
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance009_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance009_sync";
        final String testMethod = "testExtendedScopeInheritance009";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance009_unsync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testExtendedScopeInheritance009_sync";
        final String testMethod = "testExtendedScopeInheritance009";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001_sync";
        final String testMethod = "testNoPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001A_sync";
        final String testMethod = "testNoPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation001B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation001B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation001B_sync";
        final String testMethod = "testNoPersistenceContextPropagation001B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002_sync";
        final String testMethod = "testNoPersistenceContextPropagation002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation002A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation002A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002A_sync";
        final String testMethod = "testNoPersistenceContextPropagation002A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation002B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation002B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation002B_sync";
        final String testMethod = "testNoPersistenceContextPropagation002B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003_sync";
        final String testMethod = "testNoPersistenceContextPropagation003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation003A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation003A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003A_sync";
        final String testMethod = "testNoPersistenceContextPropagation003A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation003B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation003B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "true");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation003B_sync";
        final String testMethod = "testNoPersistenceContextPropagation003B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation004";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation004";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004_sync";
        final String testMethod = "testNoPersistenceContextPropagation004";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation004A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation004A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004A_sync";
        final String testMethod = "testNoPersistenceContextPropagation004A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
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
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_unsync_nojoin";
        final String testMethod = "testNoPersistenceContextPropagation004B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_unsync_join";
        final String testMethod = "testNoPersistenceContextPropagation004B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testNoPersistenceContextPropagation004B_sync";
        final String testMethod = "testNoPersistenceContextPropagation004B";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
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
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_unsync_join";
        final String testMethod = "testPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001_sync";
        final String testMethod = "testPersistenceContextPropagation001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
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
     * Second Bean is SF CMT (CMTS) (uses transaction REQUIRED on business method)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_unsync_join";
        final String testMethod = "testPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "true");
        props.put("test.bean1.em.isunsynchronized", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_sync() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation001A_sync";
        final String testMethod = "testPersistenceContextPropagation001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        props.put("test.bean1.em.isunsynchronized", "false");
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
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002_unsync_join";
        final String testMethod = "testPersistenceContextPropagation002";
        final String testResource = null;
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
     * Second Bean is SF CMT (CM-TS) (uses transaction REQUIRED on business method)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002A_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation002A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");

        Map<String, java.io.Serializable> props = new HashMap<String, java.io.Serializable>();
        props.put("test.em.should.join.ex", "false");
        executeTest(testName, testMethod, testResource, props);
    }

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002A_unsync_join() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation002A_unsync_join";
        final String testMethod = "testPersistenceContextPropagation002A";
        final String testResource = null;
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
     * Verify the scenario that when a component begins a JTA transaction and invokes a JPA operation with a
     * synchronized CMEX #1, and then calls a second session bean which attempts to use an unsynchronized CMTS #2
     * and CMEX #1 and TS #2 utilize the same EntityManagerFactory (same persistence unit), then the second session
     * bean will use the CMTS persistence context propagated by the first session bean.
     *
     *
     * CMTEX #1 is synchronized,CMTS #2 is unsynchronized
     * Second Bean is SL CMT (uses transaction REQUIRED on business method)
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation003_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation003_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation003";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * If a component is called and the JTA transaction is propagated into that component:
     * ¥ If there is a persistence context of type SynchronizationType.UNSYNCHRONIZED associated with the JTA
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
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation003A_unsync_nojoin() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_CMT_CMEXSpecific_testPersistenceContextPropagation003A_unsync_nojoin";
        final String testMethod = "testPersistenceContextPropagation003A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
