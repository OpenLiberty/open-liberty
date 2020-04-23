/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh11795.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh11795.testlogic.JPATestOLGH11795Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH11795_EJB_SFEx_Servlet")
public class TestOLGH11795_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH11795Logic.class.getName();
        ejbJNDIName = "ejb/OLGH11795SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH11795_CMEX"));
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_EJB_SFEx_CMTS_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-cmex";

        // TODO: Disable this test for JPA 2.2 testing. But will be fixed when ECL 2.7 is updated
        if (isUsingJPA22Feature()) {
            return;
        }

        executeTest(testName, testMethod, testResource);
    }
}
