/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.entitymanager.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.entitymanager.testlogic.EntityManagerLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEntityManagerServlet")
public class TestEntityManagerServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "ENTITYMANAGER_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "ENTITYMANAGER_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "ENTITYMANAGER_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = EntityManagerLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // Remove001 Test
    @Test
    public void jpa_spec10_entitymanager_testRemove001_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_AMJTA_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_entitymanager_testRemove001_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_AMRL_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_entitymanager_testRemove001_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_CMTS_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // Remove002 Test
    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_AMJTA_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_AMRL_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_CMTS_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
