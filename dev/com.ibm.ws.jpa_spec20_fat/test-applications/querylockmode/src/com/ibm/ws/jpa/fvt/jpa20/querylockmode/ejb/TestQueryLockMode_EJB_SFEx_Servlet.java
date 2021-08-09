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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.LockModeType;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.jpa.fvt.jpa20.querylockmode.testlogic.QueryLockModeTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestQueryLockMode_EJB_SFEx_Servlet")
public class TestQueryLockMode_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = QueryLockModeTestLogic.class.getName();
        ejbJNDIName = "ejb/QueryLockModeTestSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/QueryLockMode_CMEX"));
    }

    /*
     * TODO: Running this test with Container Managed EntityManager requires a transaction on em.refresh().
     * Investigate changing the test to allow cmts.
     *
     * @Test
     * public void jpa_spec20_querylockmode_testScenario01_CMTS_EJB_SFEx() throws Exception {
     * final String testName = "jpa20_querylockmode_@LOCKMODE@_testScenario01_CMTS_EJB_SFEx";
     * final String testMethod = "testScenario01";
     * final String testResource = "test-jpa-resource-cmex";
     * executeTest(testName, testMethod, testResource);
     * }
     */

    @Override
    protected void executeTest(String testName, String testMethod, String testResource) throws Exception {
        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get(testResource));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("dbProductName", getDbProductName());
            properties.put("dbProductVersion", getDbProductVersion());
            properties.put("jdbcDriverVersion", getJdbcDriverVersion());
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx, ejbJNDIName);
        }
    }
}
