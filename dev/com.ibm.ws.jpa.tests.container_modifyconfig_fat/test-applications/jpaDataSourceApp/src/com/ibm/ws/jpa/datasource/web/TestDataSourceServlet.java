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

package com.ibm.ws.jpa.datasource.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.jpa.datasource.testlogic.DataSourceLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestDataSourceServlet")
public class TestDataSourceServlet extends JPATestServlet {

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "JPA_CMTS_DATASOURCE_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "JPA_AMJTA_DATASOURCE_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "JPA_AMRL_DATASOURCE_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = DataSourceLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testInsert Test
    public void insert_AMJTA() throws Exception {
        final String testName = "insert_AMJTA";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void insert_AMRL() throws Exception {
        final String testName = "insert_AMRL";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void insert_CMTS() throws Exception {
        final String testName = "insert_CMTS";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testFindExists Test
    public void exists_AMJTA() throws Exception {
        final String testName = "exists_AMJTA";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void exists_AMRL() throws Exception {
        final String testName = "exists_AMRL";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void exists_CMTS() throws Exception {
        final String testName = "exists_CMTS";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testFindExists Test
    public void notExists_AMJTA() throws Exception {
        final String testName = "notExists_AMJTA";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void notExists_AMRL() throws Exception {
        final String testName = "notExists_AMRL";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void notExists_CMTS() throws Exception {
        final String testName = "notExists_CMTS";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testRemove Test
    public void remove_AMJTA() throws Exception {
        final String testName = "remove_AMJTA";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void remove_AMRL() throws Exception {
        final String testName = "remove_AMRL";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void remove_CMTS() throws Exception {
        final String testName = "remove_CMTS";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
