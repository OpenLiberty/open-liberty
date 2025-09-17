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
 * Performs testing of EJB pure annotations (without xml deployment descriptor) with a Singleton bean.
 * This tests the class level PermitAll annotation with a variety of method level
 * annotations. This test invokes SecurityEJBA02Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class PureAnnA02Test extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnA02Test.class;

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
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level overrides the class level PermitAll annotation.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testPureA02_DenyAll_MethodOverridesClassPermitAll_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level overrides the class level PermitAll annotation.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_DenyAllWithParam_MethodOverridesClassPermitAll_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at class level controls access when no annotation at method level.
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_PermitAll_NoMethodAnnClassUsed_PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at class level controls access when no annotation at method level.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in no role is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_PermitAllwithParam_NoMethodAnnClassUsed_PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_Manager_MethodOverridesClassPermitAll_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but RolesAllowed requires Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_ManagerWithParam_MethodOverridesClassPermitAll_DenyAccessNonManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user not in a role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_ManagerNoAuth_MethodOverridesClassPermitAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level PermitAll annotation.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_Employee_MethodOverridesClassPermitAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level PermitAll.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeWithParam_MethodOverridesClassPermitAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level PermitAll.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in no role when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeNoAuth_MethodOverridesClassPermitAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeAndManager_MethodOverridesClassPermitAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeAndManagerwithInt_MethodOverridesClassPermitAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeAndManagerAllWithParams_MethodOverridesClassPermitAll_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level PermitAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in no role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA02_EmployeeAndManagerwithParamNoAuth_MethodOverridesClassPermitAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb02&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA02_DeclareRoles01() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA02_RunAsClient() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA02_RunAsSpecified() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}