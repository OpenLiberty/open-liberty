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

package com.ibm.ws.jpa.beanvalidation20.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.beanvalidation20.testlogic.BeanValidation20Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestBeanValidation20_EJB_SF_Servlet")
public class TestBeanValidation20_EJB_SF_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = BeanValidation20Logic.class.getName();
        ejbJNDIName = "ejb/BeanValidation20SFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/BeanValidation20_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/BeanValidation20_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/BeanValidation20_CMTS"));
    }

    // testBeanValidationAnno001 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno001_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationAnno001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML001 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML001_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML001_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationXML001";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno002 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno002_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationAnno002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML002 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML002_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML002_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML002_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML002_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationXML002";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno003 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno003_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationAnno003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML003 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML003_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML003_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML003_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML003_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationXML003";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno004 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno004_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationAnno004";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML004 Test
    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML004_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML004_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationXML004";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationAnno005 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno005_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationAnno005";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML005 Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML005_EJB_SF_AMJTA_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_EJB_SF_AMJTA_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_beanvalidation_testBeanValidationXML005_EJB_SF_AMRL_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_EJB_SF_AMRL_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML005_EJB_SF_CMTS_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML005_EJB_SF_CMTS_Web";
        final String testMethod = "testBeanValidationXML005";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
