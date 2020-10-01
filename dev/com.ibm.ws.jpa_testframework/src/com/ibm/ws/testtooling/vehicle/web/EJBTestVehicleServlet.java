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

package com.ibm.ws.testtooling.vehicle.web;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.ejb.EJBTestVehicle;

public class EJBTestVehicleServlet extends JPATestServlet {
    private static final long serialVersionUID = 7626680108917278937L;
    protected String ejbJNDIName;

    protected void executeTestVehicle(TestExecutionContext ctx, String ejbJndiName) {
        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/" + ejbJndiName;

            System.out.println("Looking up \"" + jndiName + "\" ...");
            EJBTestVehicle ejb = (EJBTestVehicle) ic.lookup(jndiName);
            try {
                ejb.executeTestLogic(ctx);
            } finally {
                try {
                    ejb.release();
                } catch (Throwable t) {
                }
            }
        } catch (Throwable t) {
            logException(t, ctx);
        }
    }

    @Override
    protected void executeTest(String testName, String testMethod, String testResource) throws Exception {
        executeTest(testName, testMethod, testResource, null);
    }

    @Override
    protected void executeTest(String testName, String testMethod, String testResource, Map<String, java.io.Serializable> props) throws Exception {
        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        if (testResource != null)
            jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get(testResource));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("dbProductName", getDbProductName());
        properties.put("dbProductVersion", getDbProductVersion());
        properties.put("jdbcDriverVersion", getJdbcDriverVersion());

        if (props != null && !props.isEmpty()) {
            properties.putAll(props);
        }

        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
