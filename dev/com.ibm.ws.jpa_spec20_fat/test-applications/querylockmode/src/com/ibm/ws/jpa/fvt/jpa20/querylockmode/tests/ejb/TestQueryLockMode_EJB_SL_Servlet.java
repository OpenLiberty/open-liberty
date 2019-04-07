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

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.LockModeType;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestQueryLockMode_EJB_SL_Servlet")
public class TestQueryLockMode_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.jpa20.querylockmode.testlogic.QueryLockModeTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/QueryLockModeTestSLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/QueryLockMode_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/QueryLockMode_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/QueryLockMode_CMTS"));
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/QueryLockMode_AMRL"));

    }

    @Test
    public void jpa20_QueryLockMode_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa20_QueryLockMode_@LOCKMODE@_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testScenario01";

        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testLogicClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
            jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx, ejbJNDIName);
        }
    }

    @Test
    public void jpa20_QueryLockMode_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa20_QueryLockMode_@LOCKMODE@_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testScenario01";

        for (LockModeType lockModeType : LockModeType.values()) {
            final TestExecutionContext testExecCtx = new TestExecutionContext(testName.replace("@LOCKMODE@", lockModeType.name()), testLogicClassName, testMethod);
            final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
            jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

            HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
            properties.put("LockModeType", lockModeType.name());

            executeDDL("JPA20_QUERYLOCKMODE_DELETE_${dbvendor}.ddl");
            executeDDL("JPA20_QUERYLOCKMODE_POPULATE_${dbvendor}.ddl");
            executeTestVehicle(testExecCtx, ejbJNDIName);
        }
    }

}
