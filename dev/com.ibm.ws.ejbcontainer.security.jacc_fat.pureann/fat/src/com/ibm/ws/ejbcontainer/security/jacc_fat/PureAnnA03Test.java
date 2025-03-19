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
 * Performs testing of EJB pure annotations (without xml deployment descriptor) with a Stateless bean.
 * This tests the class level RolesAllowed ({"Employee", "Manager"}) with a variety of
 * method level annotations. This test invokes SecurityEJBA03Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA03Test extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnA03Test.class;

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
    public void testPureA03_DenyAll_MethodOverridesClassRolesAllowed_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level overrides the class level RolesAllowed annotation.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_DenyAllWithParam_MethodOverridesClassRolesAllowed_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at method level overrides the class level RolesAllowed (Employee,Manager).
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_PermitAll_MethodOverridesClassRolesAllowed_PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponse(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.NO_ROLE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation method level overrides class level RolesAllowed.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in Manager is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_PermitAllwithParam_MethodOverridesClassRolesAllowed_PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed (Manager)
     * <LI> annotation at method level overrides the class level RolesAllowed(Employee,Manager).
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_Manager_MethodOverridesClassRolesAllowed_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed (Manager)
     * <LI> annotation at method level overrides the class level RolesAllowed (Employee, Manager).
     * <LI> This test covers invoking the EJB method manager(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but RolesAllowed requires Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_ManagerWithParam_MethodOverridesClassRolesAllowed_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method overrides the class level RolesAllowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user not in a role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_ManagerNoAuth_MethodOverridesClassRolesAllowed_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed (Employee)
     * <LI> annotation at method level overrides the class level RolesAllowed (Employee,Manager) annotation.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_Employee_MethodOverridesClassRolesAllowed_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level RolesAllowed(Employee,Manager)
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in Manager when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_Employee_MethodOverridesClassRolesAllowed_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level RolesAllowed(Employee,Manager).
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeWithParam_MethodOverridesClassRolesAllowed_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level RolesAllowed(Employee,Manager).
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in no role when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeNoAuth_MethodOverridesClassRolesAllowed_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level RolesAllowed(Employee,Manager).
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in Manager role when Employee role is required by method level annotation.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeNoAuth_MethodOverridesClassRolesAllowed_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed annotation
     * <LI> at class level controls access when no method level annotation.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeAndManager_NoMethodAnnClassUsed_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed annotation
     * <LI> at class level controls access when no method level annotation.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeAndManagerAllWithParam_NoMethodAnnClassUsed_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed annotation
     * <LI> at class level controls access when no method level annotation.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeAndManagerWithInt_NoMethodAnnClassUsed_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed annotation
     * <LI> at class level controls access when no method level annotation.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in no role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA03_EmployeeAndManagerwithParamsNoAuth_NoMethodAnnClassUsed_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb03&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA03_DeclareRoles01_NoMethodAnnClassUsed_DenyAccessDeclaredRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA03_RunAsClient() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA03_RunAsSpecified() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}