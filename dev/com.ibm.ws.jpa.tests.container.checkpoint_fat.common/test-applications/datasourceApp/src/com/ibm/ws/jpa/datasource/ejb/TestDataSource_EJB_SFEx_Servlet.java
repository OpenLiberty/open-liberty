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
@WebServlet(urlPatterns = "/TestDataSource_EJB_SFEx_Servlet")
public class TestDataSource_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = DataSourceLogic.class.getName();
        ejbJNDIName = "ejb/DataSourceSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/CMEX_DATASOURCE_JTA"));
    }

    // testInsert

    public void jpa_checkpoint_datasource_testInsert_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_checkpoint_datasource_testInsert_EJB_SFEx_CMEX_Web";
        final String testMethod = "testInsert";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
