/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.beanvalidation20.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.beanvalidation20.testlogic.BeanValidation20Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestBeanValidation20Servlet")
public class TestBeanValidation20Servlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "BEANVALIDATION20_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "BEANVALIDATION20_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "BEANVALIDATION20_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = BeanValidation20Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testBeanValidationAnno001 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_AMRL_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_CMTS_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML001 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_AMJTA_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_AMRL_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_CMTS_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno002 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno002_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno002_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_AMRL_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno002_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_CMTS_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML002 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML002_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_AMJTA_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML002_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_AMRL_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML002_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_CMTS_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno003 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno003_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno003_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_AMRL_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno003_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_CMTS_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML003 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML003_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_AMJTA_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML003_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_AMRL_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML003_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_CMTS_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno004 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_AMRL_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_CMTS_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML004 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_AMJTA_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_AMRL_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_CMTS_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno005 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno005_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno005_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_AMRL_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno005_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_CMTS_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML005 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML005_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_AMJTA_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML005_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_AMRL_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML005_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_CMTS_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
