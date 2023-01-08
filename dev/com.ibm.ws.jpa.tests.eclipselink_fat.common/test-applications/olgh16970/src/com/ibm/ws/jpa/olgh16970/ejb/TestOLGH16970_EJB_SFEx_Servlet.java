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

package com.ibm.ws.jpa.olgh16970.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16970.testlogic.JPATestOLGH16970Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16970_EJB_SFEx_Servlet")
public class TestOLGH16970_EJB_SFEx_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16970Logic.class.getName();
        ejbJNDIName = "ejb/OLGH16970SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16970_CMEX"));
    }

    // testSimpleQuery1
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery1_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery1_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSimpleQuery1";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testSimpleQuery2
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery2_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery2_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSimpleQuery2";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testSimpleQuery3
    @Test
    public void jpa_eclipselink_olgh16970_testSimpleQuery3_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16970_testSimpleQuery3_EJB_SFEx_CMTS_Web";
        final String testMethod = "testSimpleQuery3";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
