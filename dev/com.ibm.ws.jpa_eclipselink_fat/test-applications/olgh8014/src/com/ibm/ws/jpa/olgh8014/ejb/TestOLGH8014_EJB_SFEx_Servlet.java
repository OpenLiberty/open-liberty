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

package com.ibm.ws.jpa.olgh8014.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh8014.testlogic.JPATestOLGH8014Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH8014_EJB_SFEx_Servlet")
public class TestOLGH8014_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH8014Logic.class.getName();
        ejbJNDIName = "ejb/OLGH8014SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH8014_CMEX"));
    }

    // testEmptyAggregateFunctionsWithPrimitives
    @Test
    public void jpa_eclipselink_olgh8014_testEmptyAggregateFunctionsWithPrimitives_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh8014_testEmptyAggregateFunctionsWithPrimitives_EJB_SFEx_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testEmptyAggregateFunctionsWithWrappers
    @Test
    public void jpa_eclipselink_olgh8014_testEmptyAggregateFunctionsWithWrappers_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh8014_testEmptyAggregateFunctionsWithWrappers_EJB_SFEx_CMTS_Web";
        final String testMethod = "testEmptyAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testAggregateFunctionsWithPrimitives
    @Test
    public void jpa_eclipselink_olgh8014_testAggregateFunctionsWithPrimitives_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh8014_testAggregateFunctionsWithPrimitives_EJB_SFEx_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithPrimitives";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testAggregateFunctionsWithWrappers
    @Test
    public void jpa_eclipselink_olgh8014_testAggregateFunctionsWithWrappers_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh8014_testAggregateFunctionsWithWrappers_EJB_SFEx_CMTS_Web";
        final String testMethod = "testAggregateFunctionsWithWrappers";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
