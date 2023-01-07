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

package com.ibm.ws.jpa.fvt.util.tests.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.util.testlogic.UtilTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@WebServlet(urlPatterns = "/UtilWebTestServlet")
public class UtilWebTestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "Util_JEE")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "Util_JEE")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "Util_JEE_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = UtilTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa20_util_basic_001_AMJTA_Web() throws Exception {
        final String testName = "jpa20_util_basic_001_AMJTA_Web";
        final String testMethod = "testUtilBasic";
        final String testResource = "test-jpa-resource-amjta";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_util_1x1_001_AMJTA_Web() throws Exception {
        final String testName = "jpa20_util_1x1_001_AMJTA_Web";
        final String testMethod = "testUtil1x1";
        final String testResource = "test-jpa-resource-amjta";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    // Disabled until difference from WS-CD run is determined.
    //@Test
    public void jpa20_util_embeddable_001_AMRL_Web() throws Exception {
        final String testName = "jpa20_util_embeddable_001_AMRL_Web";
        final String testMethod = "testUtilEmbeddable";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_util_1xM_001_AMJTA_Web() throws Exception {
        final String testName = "jpa20_util_1xM_001_AMJTA_Web";
        final String testMethod = "testUtil1xm";
        final String testResource = "test-jpa-resource-amjta";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_util_basic_001_AMRL_Web() throws Exception {
        final String testName = "jpa20_util_basic_001_AMRL_Web";
        final String testMethod = "testUtilBasic";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_util_1x1_001_AMRL_Web() throws Exception {
        final String testName = "jpa20_util_1x1_001_AMRL_Web";
        final String testMethod = "testUtil1x1";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }

    @Test
    public void jpa20_util_1xM_001_AMRL_Web() throws Exception {
        final String testName = "jpa20_util_1xM_001_AMRL_Web";
        final String testMethod = "testUtil1xm";
        final String testResource = "test-jpa-resource-amrl";

        executeTest(testName, testMethod, testResource);
        executeDDL("JPA20_UTIL_DEFAULT_DELETE_${dbvendor}.ddl");
    }
}
