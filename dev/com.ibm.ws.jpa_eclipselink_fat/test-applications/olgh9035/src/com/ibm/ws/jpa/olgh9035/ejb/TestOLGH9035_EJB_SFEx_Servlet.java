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

package com.ibm.ws.jpa.olgh9035.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH9035_EJB_SFEx_Servlet")
public class TestOLGH9035_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = "com.ibm.ws.jpa.olgh9035.testlogic.JPATestOLGH9035Logic";
        ejbJNDIName = "ejb/OLGH9035SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH9035_CMEX"));
    }

    // DB2Z tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_DB2ZOS_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_DB2ZOS_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_DB2ZOS";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //DB2-i tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_DB2I_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_DB2I_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_DB2I";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //MySQL tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_MySQL_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_MySQL_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_MySQL";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //Derby tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_Derby_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_Derby_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_Derby";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //Oracle tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_Oracle_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_Oracle_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_Oracle";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    //Hana tests
    @Test
    public void jpa_eclipselink_olgh9035_testPlatformDetection_Hana_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh9035_testPlatformDetection_Hana_EJB_SFEx_CMTS_Web";
        final String testMethod = "testPlatformDetection_Hana";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
