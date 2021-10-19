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

package com.ibm.ws.jpa.beanvalidation.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.beanvalidation.testlogic.BeanValidationLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestBeanValidationServlet")
public class TestBeanValidationServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "BEANVALIDATION_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "BEANVALIDATION_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "BEANVALIDATION_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = BeanValidationLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testBeanValidationAnno Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno_AMRL_Web";
        final String testMethod = "testBeanValidationAnno";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno_CMTS_Web";
        final String testMethod = "testBeanValidationAnno";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML_AMJTA_Web";
        final String testMethod = "testBeanValidationXML";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML_AMRL_Web";
        final String testMethod = "testBeanValidationXML";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML_CMTS_Web";
        final String testMethod = "testBeanValidationXML";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
