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

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.TransactionSynchronization;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationDDEJBSFExSyncTestServlet")
public class TxSynchronizationDDEJBSFExSyncTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.TxSynchronizationTestLogic";
        ejbJNDIName = "ejb/TxSyncBMTSFEXSyncEJB";

        jpaPctxMap.put("test-jpa-resource-txsync",
                       new JPAPersistenceContext("test-jpa-resource-txsync", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.FIELD, "emCMEXTxSync", TransactionSynchronization.SYNCHRONIZED));
//        jpaPctxMap.put("test-jpa-resource-txunsync",
//                       new JPAPersistenceContext("test-jpa-resource-txunsync", com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "emCMTSTxUnsync", TransactionSynchronization.UNSYNCHRONIZED));

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
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testIsJoinedToTransaction_001_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testIsJoinedToTransaction_001_EJB_CMEX_SF";
        final String testMethod = "testIsJoinedToTransaction001";
        final String testResource = "test-jpa-resource-txsync";
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_001_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_001_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock001";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_002_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_002_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock002";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_003_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_003_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock003";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_004_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_004_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock004";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.find() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 4 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_005_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_005_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock005";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.find() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 4 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_006_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_006_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock006";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: READ
     *
     * Verify that em.find() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 4 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_007_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_007_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock007";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.find() with Lock Type: WRITE
     *
     * Verify that em.find() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     *
     * Points: 4 points for synchronized entity managers, 9 points for unsynchronized entity managers.
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_008_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testFindWithLock_008_Sync_EJB_CMEX_SF";
        final String testMethod = "testFindWithLock008";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: NONE
     *
     * Verify that em.lock() with lock type NONE requires an active JTA transaction and that the
     * EntityManager is joined to the transaction.
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_001_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_001_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock001";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC
     *
     * Verify that em.lock() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_002_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_002_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock002";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_003_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_003_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock003";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_004_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_004_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock004";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.lock() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_005_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_005_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock005";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.lock() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_006_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_006_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock006";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: READ
     *
     * Verify that em.lock() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_007_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_007_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock007";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

    /**
     * Test em.lock() with Lock Type: WRITE
     *
     * Verify that em.lock() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_DD_testEmLock_008_Sync_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_DD_testEmLock_008_Sync_EJB_CMEX_SF";
        final String testMethod = "testEmLock008";
        final String testResource = "test-jpa-resource-txsync";
        executeTest(testName, testMethod, testResource);
    }

}
