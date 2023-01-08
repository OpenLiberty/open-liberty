/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.derivedidentity.tests.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.derivedidentity.testlogic.DerivedIdentityTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@WebServlet(urlPatterns = "/DerivedIdentityWebTestServlet")
public class DerivedIdentityWebTestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "DerivedIdentity_JEE")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "DerivedIdentity_JEE")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "DerivedIdentity_JEE_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = DerivedIdentityTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa20_derivedidentity_001_AMJTA_Web() throws Exception {
        final String testName = "jpa20_derivedidentity_001_AMJTA_Web";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-amjta";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_DERIVEDIDENTITY_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_derivedidentity_001_AMRL_Web() throws Exception {
        final String testName = "jpa20_derivedidentity_001_AMRL_Web";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_DERIVEDIDENTITY_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_derivedidentity_001_CMTS_Web() throws Exception {
        final String testName = "jpa20_derivedidentity_001_CMTS_Web";
        final String testMethod = "testScenario01";
        final String testResource = "test-jpa-resource-cmts";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_DERIVEDIDENTITY_DEFAULT_DELETE_${dbvendor}.ddl");
    }

}
