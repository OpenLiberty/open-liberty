/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.jpa.datasource.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.jpa.datasource.testlogic.DataSourceLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestDataSource_EJB_SL_Servlet")
public class TestDataSource_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = DataSourceLogic.class.getName();
        ejbJNDIName = "ejb/DataSourceSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AMJTA_DATASOURCE_JTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AMRL_DATASOURCE_RL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/CMTS_DATASOURCE_JTA"));
    }

    // testInsert

    public void jpa_checkpoint_datasource_testInsert_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_checkpoint_datasource_testInsert_EJB_SL_AMJTA_Web";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void jpa_checkpoint_datasource_testInsert_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_checkpoint_datasource_testInsert_EJB_SL_AMRL_Web";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void jpa_checkpoint_datasource_testInsert_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_checkpoint_datasource_testInsert_EJB_SL_CMTS_Web";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
