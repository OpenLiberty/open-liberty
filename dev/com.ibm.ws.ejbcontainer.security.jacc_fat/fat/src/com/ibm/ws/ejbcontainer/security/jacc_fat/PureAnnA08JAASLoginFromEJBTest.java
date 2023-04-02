/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import static org.junit.Assert.assertTrue;

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
 * Performs testing of a JAAS programmatic login from a statless bean with pure annotations. The bean issues a login() after
 * it is invoked by the servlet. The bean obtains the subject from the LoginContext and displays the subject so that it can be verified.
 *
 * This test invokes SecurityEJBA08Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 *
 * Note: This test invokes SecurityEJBA08Bean which issues a JAAS login with user1 and user3
 * (which are hard-coded users in SecurityEJBA08Bean.java). Therefore, this test requires the basic user
 * registry to be configured with user1 in group1 and user3 in group3.
 **/
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA08JAASLoginFromEJBTest extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnA08JAASLoginFromEJBTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at method level allows user1 to invoke the permitAll method. The permitAll
     * <LI> method then performs a JAAS programmatic login with the same user (user1) results in valid subject.
     * <LI>
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> After an EJB method is invoked with one user, a JAAS login with the same user results in
     * <LI> valid subject containing WSPrincipal with correct realm and group name.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA08JAASLogin_PermitAll_LoginWithSameUser_ValidSubjectWithWSPrincipalAndGroup() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb08&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        assertTrue("WSPrincipal:" + Constants.EMPLOYEE_USER + " not found in response", response.contains("WSPrincipal:" + Constants.EMPLOYEE_USER));
        assertTrue("group:" + Constants.USER_REGISTRY_REALM + "/" + Constants.EMPLOYEE_GROUP + " not found in response",
                   response.contains("group:" + Constants.USER_REGISTRY_REALM + "/"));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at method level allows user1 to invoke the permitAll(String) method. The permitAll
     * <LI> method then performs a JAAS programmatic login with a different user (user3) which results in valid subject.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> After an EJB method is invoked with one user, a JAAS login with a different user results in
     * <LI> valid subject containing WSPrincipal with correct realm and group name.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA08JAASLogin_PermitAllwithParam_LoginDifferentUser_ValidNewSubjectWithWSPrincipalAndGroup() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb08&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        assertTrue("WSPrincipal:" + Constants.NEW_JASS_LOGIN_USER + " not found in response", response.contains("WSPrincipal:" + Constants.NEW_JASS_LOGIN_USER));
        assertTrue("group:" + Constants.USER_REGISTRY_REALM + "/" + Constants.EMPLOYEE_GROUP + " not found in response",
                   response.contains("group:" + Constants.USER_REGISTRY_REALM + "/"));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }
}