/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class DynamicJACCFeatureTest extends EJBAnnTestBase {

    protected static Class<?> logClass = DynamicJACCFeatureTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        commonSetup(logClass, Constants.SERVER_JACC_DYNAMIC,
                    Constants.APPLICATION_SECURITY_EJB_IN_WAR, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB_IN_WAR);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> We start the test with no JACC Feature added on the server.xml.
     * <LI> We then proceed to hit the servlet and get the below expectation.
     * <LI> However we then stop the server and add the JACC Feature and config
     * <LI> and then we start the server and repeat the same test.
     * <LI>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level overrides the class level RolesAllowed annotation.
     * <LI> This test covers invoking the EJB method denyAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testDynamicFeatureUpdate_NoJACCFeature_Then_AddJaccFeature() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering Test: " + getName().getMethodName());

        testHelper.reconfigureServer(Constants.JACC_FEATURE_NOT_ENABLED, getName().getMethodName(), Constants.RESTART_SERVER);

        String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.AUTH_DENIED_METHOD_EXPLICITLY_EXCLUDED);
        client.resetClientState();
        testHelper.reconfigureServer(Constants.DEFAULT_CONFIG_FILE, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        String queryString2 = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";
        String response2 = generateResponseFromServlet(queryString2, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response2, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting Test: " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> We start the test with no JACC Feature added on the server.xml.
     * <LI> We then proceed to hit the servlet and get the below expectation.
     * <LI> However we then stop the server and add the JACC Feature and config
     * <LI> and then we start the server and repeat the same test.
     * <LI>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level overrides the class level RolesAllowed annotation.
     * <LI> This test covers invoking the EJB method denyAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testDynamicFeatureUpdate_AddJaccFeature_Then_NoJACCFeature() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering Test: " + getName().getMethodName());

        testHelper.reconfigureServer(Constants.DEFAULT_CONFIG_FILE, getName().getMethodName(), Constants.RESTART_SERVER);
        String queryString2 = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";
        String response2 = generateResponseFromServlet(queryString2, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response2, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        client.resetClientState();

        testHelper.reconfigureServer(Constants.JACC_FEATURE_NOT_ENABLED, getName().getMethodName(), Constants.DO_NOT_RESTART_SERVER);
        String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.AUTH_DENIED_METHOD_EXPLICITLY_EXCLUDED);

        Log.info(logClass, getName().getMethodName(), "**Exiting Test: " + getName().getMethodName());
    }
}
