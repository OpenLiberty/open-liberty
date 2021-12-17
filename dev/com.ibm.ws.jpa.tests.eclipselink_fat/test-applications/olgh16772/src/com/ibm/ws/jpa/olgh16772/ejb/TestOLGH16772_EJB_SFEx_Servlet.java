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

package com.ibm.ws.jpa.olgh16772.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16772.testlogic.JPATestOLGH16772Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16772_EJB_SFEx_Servlet")
public class TestOLGH16772_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16772Logic.class.getName();
        ejbJNDIName = "ejb/OLGH16772SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16772_CMEX"));
    }

    // testWhereTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testWhereTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testWhereLeftTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereLeftTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereLeftTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testWhereLeftTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testWhereRightTrim

    @Test
    public void jpa_eclipselink_olgh16772_testWhereRightTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testWhereRightTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testWhereRightTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSelectTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectLeftTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectLeftTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectLeftTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSelectLeftTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testSelectRightTrim

    @Test
    public void jpa_eclipselink_olgh16772_testSelectRightTrim_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16772_testSelectRightTrim_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSelectRightTrim";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
