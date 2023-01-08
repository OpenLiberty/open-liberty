/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.ejbinwar_javacomp.web.rrinwar;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

/**
 *
 */
public class JPAEjbInWarJavaCompTestServlet extends EJBDBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.ejbinwar_javacomp.testlogic.JPAEjbInWarJavaCompTestLogic";
    private final PersistenceInjectionType piType = PersistenceInjectionType.FIELD;

    private final static String ejbJNDIName = "ejbinwar/javaCompEnv/JPAEjbInWarJavaCompEJB";

    @PostConstruct
    private void initFAT() {

    }

    @Test
    public void testEJBInWarWithResRefInWebXML() throws Exception {
        final String testName = "testEJBInWarWithResRefInWebXML";
        final String resource = "emf";
        final String testMethod = "testCRUDOperations";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);
        final com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType pcType = com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType.APPLICATION_MANAGED_JTA;
        final JPAPersistenceContext jpaPCtx = new JPAPersistenceContext("test-jpa-resource", pcType, piType, resource);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPCtx);
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
