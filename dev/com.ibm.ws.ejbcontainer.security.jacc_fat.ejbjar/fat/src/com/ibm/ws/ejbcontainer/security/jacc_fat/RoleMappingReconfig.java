/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
@Mode(TestMode.FULL)
public class RoleMappingReconfig extends EJBAnnTestBase {
    protected static Class<?> logClass = RoleMappingReconfig.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_JACC_DYNAMIC,
                    Constants.APPLICATION_SECURITY_EJB_IN_WAR, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB_IN_WAR);

    }

    /**
     * The beforeTest method will call the changeRoleMappingProps to make sure that before we start to
     * run a new test, we are using the default roleMappings.props file.
     */
    @Before
    public void beforeTest() throws Exception {
        testHelper.changeRoleMappingProps(Constants.ALL_ROLE_MAPPING_PROPS, getName().getMethodName(), null, Constants.DO_NOT_RESTART_SERVER);
    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>That after removing all the roles from the roleMappings.props, the user
     * <LI>is getting and access denied error.
     * <LI>
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 403 should be thrown since the user is not authorized to access any resource.
     * <LI>
     * </OL>
     */

    @Test
    public void testRoleMappingProps_NoRolesAssigned() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";

        testHelper.changeRoleMappingProps(Constants.NO_ROLES_ROLE_MAPPING_PROPS, getName().getMethodName(), null, Constants.DO_NOT_RESTART_SERVER);
        generateAccessDeniedResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> That after removing roles that are NOT related to the app, the user is still able to
     * <LI> access his application..
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testRoleMappingProps_AppRolesOnlyAssigned() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());
        testHelper.changeRoleMappingProps(Constants.APP_ROLE_ONLY_ROLE_MAPPING_PROPS, getName().getMethodName(), null, Constants.DO_NOT_RESTART_SERVER);

        String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>That after removing the roles that are specific to the app in the roleMappings.props, the user
     * <LI>is getting and access denied error.
     * <LI>
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> 403 should be thrown since the user is not authorized to access any resource.
     * <LI>
     * </OL>
     */

    @Test
    public void testRoleMappingProps_NoRolesAssigned_to_App() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb01&testMethod=denyAll";

        testHelper.changeRoleMappingProps(Constants.NO_APP_ROLE_ROLE_MAPPING_PROPS, getName().getMethodName(), null, Constants.DO_NOT_RESTART_SERVER);
        generateAccessDeniedResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}
