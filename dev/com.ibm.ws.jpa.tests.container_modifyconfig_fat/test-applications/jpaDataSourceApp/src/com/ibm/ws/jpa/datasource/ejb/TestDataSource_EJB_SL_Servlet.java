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

    // testInsert Test
    public void insert_SL_AMJTA() throws Exception {
        final String testName = "insert_SL_AMJTA";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void insert_SL_AMRL() throws Exception {
        final String testName = "insert_SL_AMRL";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void insert_SL_CMTS() throws Exception {
        final String testName = "insert_SL_CMTS";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testFindExists Test
    public void exists_SL_AMJTA() throws Exception {
        final String testName = "exists_SL_AMJTA";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void exists_SL_AMRL() throws Exception {
        final String testName = "exists_SL_AMRL";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void exists_SL_CMTS() throws Exception {
        final String testName = "exists_SL_CMTS";
        final String testMethod = "testFindExists";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testFindExists Test
    public void notExists_SL_AMJTA() throws Exception {
        final String testName = "notExists_SL_AMJTA";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void notExists_SL_AMRL() throws Exception {
        final String testName = "notExists_SL_AMRL";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void notExists_SL_CMTS() throws Exception {
        final String testName = "notExists_SL_CMTS";
        final String testMethod = "testFindNotExists";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testRemove Test
    public void remove_SL_AMJTA() throws Exception {
        final String testName = "remove_SL_AMJTA";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    public void remove_SL_AMRL() throws Exception {
        final String testName = "remove_SL_AMRL";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    public void remove_SL_CMTS() throws Exception {
        final String testName = "remove_SL_CMTS";
        final String testMethod = "testRemove";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
