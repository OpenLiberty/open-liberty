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

package com.ibm.ws.jpa.olgh16588.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh16588.testlogic.JPATestOLGH16588Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH16588_EJB_SFEx_Servlet")
public class TestOLGH16588_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH16588Logic.class.getName();
        ejbJNDIName = "ejb/OLGH16588SFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OLGH16588_CMEX"));
    }

    // testJPQLAggregateCollection
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLAggregateCollection_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLAggregateCollection_EJB_SFEx_CMTS_Web";
        final String testMethod = "testJPQLAggregateCollection";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQLJoin
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLJoin_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLJoin_EJB_SFEx_CMTS_Web";
        final String testMethod = "testJPQLJoin";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testJPQLNestedEmbeddable
    @Test
    public void jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_EJB_SFEx_CMTS_Web() throws Exception {
        final String testName = "jpa_eclipselink_olgh16588_testJPQLNestedEmbeddable_EJB_SFEx_CMTS_Web";
        final String testMethod = "testJPQLNestedEmbeddable";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
