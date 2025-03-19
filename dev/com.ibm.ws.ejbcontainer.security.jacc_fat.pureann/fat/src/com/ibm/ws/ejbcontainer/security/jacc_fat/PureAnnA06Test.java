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
 * This tests the class level DenyAll annotation with a variety of method level
 * annotations. This test invokes SecurityEJBA06Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA06Test extends EJBAnnTestBase {

    protected static Class<?> logClass = PureAnnA06Test.class;

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
     * <LI> annotation at class level and no annotation at method level.
     * <LI> This test covers invoking the EJB method denyAll() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testPureA06_DenyAll_NoMethodAnnClassUsed_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at class level is used because there is no method annotation.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in no role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_DenyAllWithParam_NoMethodAnnClassUsed_DenyAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where PermitAll
     * <LI> at method level overrides class level DenyAll.
     * <LI> This test covers invoking the EJB method permitAll() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_PermitAll_MethodOverridesClassDenyAll_PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponse(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.NO_ROLE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at method level overrides class level DenyAll.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in Manager role is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_PermitAllwithParam__MethodOverridesClassDenyAll__PermitAccess() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method manager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_Manager_MethodOverridesClassDenyAll_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_ManagerWithParam_MethodOverridesClassDenyAll_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method manager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user not in a role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_ManagerNoAuth_MethodOverridesClassDenyAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level overrides the class level DenyAll annotation.
     * <LI> This test covers invoking the EJB method employee() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_Employee_MethodOverridesClassDenyAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level DenyAll.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeWithParam_MethodOverridesClassDenyAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level (Employee) overrides class level DenyAll
     * <LI> This test covers invoking the EJB method employee() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in no role when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeNoAuth_MethodOverridesClassDenyAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method employeeAndManager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeAndManager_MethodOverridesClassDenyAll_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeAndManagerAllWithParam_MethodOverridesClassDenyAll_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in no role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeAndManagerWithIntNoAuth_MethodOverridesClassDenyAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level (Employee Manager) overrides the class level DenyAll.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in no role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_EmployeeAndManagerWithParamsNoAuth_MethodOverridesClassDenyAll_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                       Constants.DECLARED_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation
     * <LI> at method level overrides the class level DenyAll and allows access to user in DeclaredRole.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in DeclaredRole is allowed access to EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA06_DeclareRoles01() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb06&testMethod=declareRoles01";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE,
                       Constants.IS_DECLARED_ROLE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA06_RunAsClient() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    @Test
    public void testPureA06_RunAsSpecified() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "Method not implemented by design" + name.getMethodName());
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}