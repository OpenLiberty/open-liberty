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

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSynchronizationCMEXSpecificRunnerTestServlet")
public class TxSynchronizationCMEXSpecificRunnerTestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.fvt.txsync.testlogic.cm.CMEXAndBMTSpecificTxSynchronizationTestLogic";
        ejbJNDIName = "ejb/BasicRunnerSLEJB";
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     */

    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance001_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance001_EJB_CMEX_SF";
        final String testMethod = "testExtendedScopeInheritance001";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     */
    @Test
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance001A_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance001A_EJB_CMEX_SF";
        final String testMethod = "testExtendedScopeInheritance001A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance002_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance002_EJB_CMEX_SF";
        final String testMethod = "testExtendedScopeInheritance002";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
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
     * applies—independently of whether transactions are active or not at the point of the creation
     * of the stateful session beans. If the stateful session beans differ in declared synchronization
     * type, the EJBException is thrown by the container.
     *
     */

    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.wsspi.injectionengine.InjectionException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance002A_EJB_CMEX_SF() throws Exception {
        final String testName = "jpa_jpa21_TxSynchronizationTest_CMEXSpecific_testExtendedScopeInheritance002A_EJB_CMEX_SF";
        final String testMethod = "testExtendedScopeInheritance002A";
        final String testResource = null;
        executeDDL("JPA21_TXSYNC_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

}
