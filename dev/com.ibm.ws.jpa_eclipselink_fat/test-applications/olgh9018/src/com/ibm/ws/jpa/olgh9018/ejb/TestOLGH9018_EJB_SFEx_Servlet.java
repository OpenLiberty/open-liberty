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

package com.ibm.ws.jpa.olgh9018.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.jpa.olgh9018.testlogic.JPATestOLGH9018Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH9018_EJB_SFEx_Servlet")
public class TestOLGH9018_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH9018Logic.class.getName();
        ejbJNDIName = "ejb/OLGH9018SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH9018_CMEX"));
    }

    /*
     * TODO: This test fails on first assert. Need to investigate this behavior.
     */
//    @Test
//    public void jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SFEx_CMTS_Web() throws Exception {
//        final String testName = "jpa_eclipselink_olgh9018_testUnwrapExclusiveConnection_EJB_SFEx_CMTS_Web";
//        final String testMethod = "testUnwrapExclusiveConnection";
//        final String testResource = "test-jpa-resource-cmex";
//        executeTest(testName, testMethod, testResource);
//    }
}
