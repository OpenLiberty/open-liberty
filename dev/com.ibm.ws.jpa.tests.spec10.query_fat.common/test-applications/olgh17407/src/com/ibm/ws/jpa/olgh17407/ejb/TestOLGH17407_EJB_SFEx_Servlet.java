/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh17407.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh17407.testlogic.JPATestOLGH17407Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH17407_EJB_SFEx_Servlet")
public class TestOLGH17407_EJB_SFEx_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH17407Logic.class.getName();
        ejbJNDIName = "ejb/OLGH17407SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH17407_CMEX"));
    }

    // testQueryHavingLiterals1
    @Test
    public void jpa_spec10_query_olgh17407_testQueryHavingLiterals1_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17407_testQueryHavingLiterals1_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryHavingLiterals1";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testQueryHavingParameters1
    @Test
    public void jpa_spec10_query_olgh17407_testQueryHavingParameters1_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa10_query_olgh17407_testQueryHavingParameters1_EJB_SFEx_CMTS_Web";
        final String testMethod = "testQueryHavingParameters1";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
