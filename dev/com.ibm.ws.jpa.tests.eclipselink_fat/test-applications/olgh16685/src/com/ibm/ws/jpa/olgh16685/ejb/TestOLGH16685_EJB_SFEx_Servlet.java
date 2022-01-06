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

package com.ibm.ws.jpa.olgh16685.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16685.testlogic.JPATestOLGH16685Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16685_EJB_SFEx_Servlet")
public class TestOLGH16685_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16685Logic.class.getName();
        ejbJNDIName = "ejb/OLGH16685SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16685_CMEX"));
    }

    // testJPQLCriteriaSelectClauseParameters
    @Test
    public void jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16685_testJPQLCriteriaSelectClauseParameters_EJB_SFEx_CMTS_Web";
        final String testMethod = "testJPQLCriteriaSelectClauseParameters";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
