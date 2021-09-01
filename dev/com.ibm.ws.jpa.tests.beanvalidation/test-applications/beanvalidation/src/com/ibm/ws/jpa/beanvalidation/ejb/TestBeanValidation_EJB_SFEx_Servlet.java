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

package com.ibm.ws.jpa.beanvalidation.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.beanvalidation.testlogic.BeanValidationLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestBeanValidation_EJB_SFEx_Servlet")
public class TestBeanValidation_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = BeanValidationLogic.class.getName();
        ejbJNDIName = "ejb/BeanValidationSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/BeanValidation_CMEX"));
    }

    // testBeanValidationAnno Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationAnno_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationAnno_EJB_SFEx_CMEX_Web";
        final String testMethod = "testBeanValidationAnno";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testBeanValidationXML Test
    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa_beanvalidation_testBeanValidationXML_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_beanvalidation_testBeanValidationXML_EJB_SFEx_CMEX_Web";
        final String testMethod = "testBeanValidationXML";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
