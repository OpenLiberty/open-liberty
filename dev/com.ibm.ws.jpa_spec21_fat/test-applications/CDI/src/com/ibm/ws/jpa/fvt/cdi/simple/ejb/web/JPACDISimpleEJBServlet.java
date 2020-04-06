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

package com.ibm.ws.jpa.fvt.cdi.simple.ejb.web;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.cdi.simple.CDISimpleTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = { "/JPACDISimpleEJBServlet" })
public class JPACDISimpleEJBServlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = -3888645553607380940L;
    private static final String CLASS_NAME = JPACDISimpleEJBServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @PostConstruct
    private void initFAT() {
        testClassName = CDISimpleTestLogic.class.getName();
        ejbJNDIName = "ejb/JPACDISimpleSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEM"));
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_AMJTA() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_AMJTA";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amjta";

        ejbJNDIName = "ejb/JPACDISimpleSLEJB";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_AMRL() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_AMRL";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amrl";

        ejbJNDIName = "ejb/JPACDISimpleSLEJB";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_CMTS() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SL_CMTS";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-cmts";

        ejbJNDIName = "ejb/JPACDISimpleSLEJB";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_AMJTA() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_AMJTA";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amjta";

        ejbJNDIName = "ejb/JPACDISimpleSFEJB";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_AMRL() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_AMRL";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amrl";

        ejbJNDIName = "ejb/JPACDISimpleSFEJB";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_CMTS() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_EJB_SF_CMTS";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-cmts";

        ejbJNDIName = "ejb/JPACDISimpleSFEJB";
        executeTest(testName, testMethod, testResource);
    }
}
