/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Base class for PureAnnA05Test and extending other tests.
 */
public abstract class PureAnnA05Base extends EJBAnnTestBase {

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method denyAll() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testPureA05_DenyAll_MethodOverRidesClass_DenyAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_DenyAllWithParam_MethodOverRidesClass_DenyAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where PermitAll
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method permitAll() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_PermitAll_MethodOverRidesClass_PermitAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where PermitAll
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in no role is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_PermitAllwithParam_MethodOverRidesClass_PermitAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method manager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_Manager_MethodOverridesClass_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_ManagerWithParam_MethodOverridesClass_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method manager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user not in a role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_ManagerNoAuth_MethodOverridesClass_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. There RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method manager() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user in Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_ManagerNoAuth_MethodOverridesClass_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employee() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_Employee_MethodOverridesClass_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeWithParam_MethodOverridesClass_PermitAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employee() without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in no role when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeNoAuth_NoClassAnnMethodUsed_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeeAndManager()without parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeAndManager_MethodOverridesClass_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeAndManagerWithInt_MethodOverridesClass_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeAndManagerAllWithParam_MethodOverridesClass_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in no role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeAndManagerwithParamsNoAuth_MethodOverridesClass_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user in declared role attempts to access EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_EmployeeAndManagerNoAuth_MethodOverridesClass_DenyAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                       Constants.DECLARED_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in DeclaredRole is allowed access to EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA05_DeclareRoles01_PermitAccessDeclaredUserRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=declareRoles01";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE, Constants.IS_DECLARED_ROLE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where there is class level RunAs (Employee).
     * <LI> The injected EJB SecurityEJBA01Bean method RunAsClient is invoked by a user in Manager
     * <LI> role. SecurityEJBA01Bean injects and invokes a second EJB SecurityEJBRunAsBean. This second EJB requires Manager role
     * <LI> to invoke the manager method and therefore does not allow invocation when RunAs (Employee).
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is denied to second EJB because it is run as Employee role when Manager role is required to access manager method.
     * <LI>
     * </OL>
     */

    @Test
    public void testPureA05_RunAsClient_DenyAccess() throws Exception {

        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=runAsClient";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.MANAGER_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with a class level RunAs (Employee) and RolesAllowed (Manager). There is no
     * <LI> annotation at method level in the invoked EJB (SecurityEJBA01Bean) so it is invoked as Manager role. This first
     * <LI> EJB injects and invokes a second EJB (SecurityRunAsBean) which requires Manager role to access the manager method.
     * <LI> The RunAs (Employee) annotation results in the second EJB being invoked with employee role and therefore access to
     * <LI> the manager method is denied. This test invokes SecurityEJBA05Bean methods with a variety of method signatures to insure that
     * <LI> annotations are processed correctly with methods of the same name and different signature.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB with RunAs (Employee) so access to
     * <LI> is denied to second EJB method which requires Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testPureA05_RunAsSpecified_DenyAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.MANAGER_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}