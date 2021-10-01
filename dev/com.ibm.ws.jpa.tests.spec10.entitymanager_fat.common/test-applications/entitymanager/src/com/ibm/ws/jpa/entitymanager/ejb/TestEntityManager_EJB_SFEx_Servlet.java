/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.entitymanager.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.entitymanager.testlogic.EntityManagerLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEntityManager_EJB_SFEx_Servlet")
public class TestEntityManager_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EntityManagerLogic.class.getName();
        ejbJNDIName = "ejb/EntityManagerSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityManager_CMEX"));
    }

    // Remove001 Test
    @Test
    public void jpa_spec10_entitymanager_testRemove001_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove001_EJB_SFEx_CMEX_Web";
        final String testMethod = "testRemove001";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // Remove002 Test
    @Test
    @AllowedFFDC({ "javax.transaction.RollbackException", "java.lang.IllegalArgumentException" })
    public void jpa_spec10_entitymanager_testRemove002_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_entitymanager_testRemove002_EJB_SFEx_CMEX_Web";
        final String testMethod = "testRemove002";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
