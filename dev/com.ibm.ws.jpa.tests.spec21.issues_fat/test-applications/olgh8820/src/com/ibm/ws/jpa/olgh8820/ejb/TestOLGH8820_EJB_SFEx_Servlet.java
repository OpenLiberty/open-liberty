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

package com.ibm.ws.jpa.olgh8820.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh8820.testlogic.JPATestOLGH8820Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH8820_EJB_SFEx_Servlet")
public class TestOLGH8820_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8820Logic.class.getName();
        ejbJNDIName = "ejb/OLGH8820SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8820_CMEX"));
    }

    //Indexed Parameters
    @Test
    public void jpa_spec21_olgh8820_testStoredProcedureOrderWithIndexParameter_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa21_olgh8820_testStoredProcedureOrderWithIndexParameter_EJB_SFEx_CMTS_Web";
        final String testMethod = "testStoredProcedureOrderWithIndexParameter";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //Named Parameters
    @Test
    public void jpa_spec21_olgh8820_testStoredProcedureOrderWithNamedParameter_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa21_olgh8820_testStoredProcedureOrderWithNamedParameter_EJB_SFEx_CMTS_Web";
        final String testMethod = "testStoredProcedureOrderWithNamedParameter";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
