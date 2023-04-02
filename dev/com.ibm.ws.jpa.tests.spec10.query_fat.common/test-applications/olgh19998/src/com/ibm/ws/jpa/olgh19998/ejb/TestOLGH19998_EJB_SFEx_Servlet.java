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

package com.ibm.ws.jpa.olgh19998.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh19998.testlogic.JPATestOLGH19998Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH19998_EJB_SFEx_Servlet")
public class TestOLGH19998_EJB_SFEx_Servlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH19998Logic.class.getName();
        ejbJNDIName = "ejb/OLGH19998SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH19998_CMEX"));
    }

    // testUpdateAllQueryWithTimestampLocking

    @Test
    public void jpa_spec10_query_olgh19998_testUpdateAllQueryWithTimestampLocking_EJB_SFEx_CMTS_Web() throws Exception {
        if (isDB2ForZOS())
            return; // Requires an IBM JDK with EBCDIC codepage support, which OpenJDK/Semeru does not have.

        final String testName = "jpa10_query_olgh19998_testUpdateAllQueryWithTimestampLocking_EJB_SFEx_CMTS_Web";
        final String testMethod = "testUpdateAllQueryWithTimestampLocking";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testTimestampLockingUpdateWithUpdateAllQuery

    @Test
    public void jpa_spec10_query_olgh19998_testTimestampLockingUpdateWithUpdateAllQuery_EJB_SFEx_CMTS_Web() throws Exception {
        if (isDB2ForZOS())
            return; // Requires an IBM JDK with EBCDIC codepage support, which OpenJDK/Semeru does not have.

        final String testName = "jpa10_query_olgh19998_testTimestampLockingUpdateWithUpdateAllQuery_EJB_SFEx_CMTS_Web";
        final String testMethod = "testTimestampLockingUpdateWithUpdateAllQuery";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
