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

package com.ibm.ws.jpa.olgh19342.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh19342.testlogic.JPATestOLGH19342Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH19342_EJB_SFEx_Servlet")
public class TestOLGH19342_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH19342Logic.class.getName();
        ejbJNDIName = "ejb/OLGH19342SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19342_CMEX"));
    }

    // testCaseExpressionOperatorConcurrency
    @Test
    public void jpa_spec10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh19342_testCaseExpressionOperatorConcurrency_EJB_SFEx_CMTS_Web";
        final String testMethod = "testCaseExpressionOperatorConcurrency";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
