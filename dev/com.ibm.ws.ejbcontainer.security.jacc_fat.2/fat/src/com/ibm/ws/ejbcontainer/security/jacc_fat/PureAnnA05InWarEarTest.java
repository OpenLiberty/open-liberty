/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This test variation covers packaging of EJB in WAR with all packaged in an EAR file such
 * that the servlet classes are in WEB-INF/classes and EJB jar files are in WEB-INF/lib.
 *
 * Performs testing of EJB pure annotations (without xml deployment descriptor) with a Singleton bean.
 * This test has class level annotations for RunAs("Employee") and DeeclareRoles("DeclaredRole01") with a
 * variety of method level annotations.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA05InWarEarTest extends PureAnnA05Base {

    protected static Class<?> logClass = PureAnnA05InWarEarTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB,
                    Constants.APPLICATION_SECURITY_EJB_IN_WAR, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB_IN_WAR);

    }

    @Override
    protected TestName getName() {
        return name;
    }

//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method denyAll() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Authorization failed exception for valid userId in Manager role.
//     * <LI>
//     * </OL>
//     */
//
//    @Override
//    @Test
//    public void testPureA05_DenyAll_MethodOverRidesClass_DenyAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=denyAll";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyException(response, EJBExceptionMessage, AuthDeniedMethodExplicitlyExcluded);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Authorization failed exception for valid userId in Employee role.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_DenyAllWithParam_MethodOverRidesClass_DenyAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=denyAllwithParam";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyException(response, EJBExceptionMessage, AuthDeniedMethodExplicitlyExcluded);
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where PermitAll
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method permitAll() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Employee role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_PermitAll_MethodOverRidesClass_PermitAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=permitAll";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyResponseWithoutDeprecated(response, employeeUserPrincipal, isManagerFalse, isEmployeeTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where PermitAll
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid user in no role is allowed access.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_PermitAllwithParam_MethodOverRidesClass_PermitAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=permitAllwithParam";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, noRoleUser, noRolePassword);
//        verifyResponseWithoutDeprecated(response, noRoleUserPrincipal, isManagerFalse, isEmployeeFalse);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method manager() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Manager role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_Manager_MethodOverridesClass_PermitAccessManager() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyResponseWithoutDeprecated(response, managerUserPrincipal, isManagerTrue, isEmployeeFalse);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Manager role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_ManagerWithParam_MethodOverridesClass_PermitAccessManager() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=managerwithParam";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyResponseWithoutDeprecated(response, managerUserPrincipal, isManagerTrue, isEmployeeFalse);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method manager() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Authorization failed for valid user not in a role.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_ManagerNoAuth_MethodOverridesClass_DenyAccessNoRole() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, noRoleUser, noRolePassword);
//        verifyExceptionWithUserAndRole(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, noRoleUser, managerRole);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. There RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method manager() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Authorization failed for valid user in Employee role.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_ManagerNoAuth_MethodOverridesClass_DenyAccessEmployee() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=manager";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyExceptionWithUserAndRole(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, employeeUser, managerRole);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employee() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Employee role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_Employee_MethodOverridesClass_PermitAccessEmployee() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employee";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyResponseWithoutDeprecated(response, employeeUserPrincipal, isManagerFalse, isEmployeeTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Employee role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeWithParam_MethodOverridesClass_PermitAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeewithParam";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyResponseWithoutDeprecated(response, employeeUserPrincipal, isManagerFalse, isEmployeeTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employee() without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Authorization failed for user in no role when Employee role is required.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeNoAuth_NoClassAnnMethodUsed_DenyAccessNoRole() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employee";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, noRoleUser, noRolePassword);
//        verifyExceptionWithUserAndRole(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, noRoleUser, employeeRole);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeeAndManager()without parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Employee role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeAndManager_MethodOverridesClass_PermitAccessEmployee() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManager";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyResponseWithoutDeprecated(response, employeeUserPrincipal, isManagerFalse, isEmployeeTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Employee role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeAndManagerWithInt_MethodOverridesClass_PermitAccessEmployee() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithInt";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
//        verifyResponseWithoutDeprecated(response, employeeUserPrincipal, isManagerFalse, isEmployeeTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeeAndManagerwithParam(String) with single String parameter.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid userId in Manager role is allowed to access the EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeAndManagerAllWithParam_MethodOverridesClass_PermitAccessManager() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithParam";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyResponseWithoutDeprecated(response, managerUserPrincipal, isManagerTrue, isEmployeeFalse);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> EJBAccessException when user in no role attempts to access EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeAndManagerwithParamsNoAuth_MethodOverridesClass_DenyAccessNoRole() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManagerwithParams";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, noRoleUser, noRolePassword);
//        verifyExceptionWithUserAndRoles(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, noRoleUser, employeeRole, managerRole);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet. The RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> EJBAccessException when user in declared role attempts to access EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_EmployeeAndManagerNoAuth_MethodOverridesClass_DenyAccessDeclaredRole() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=employeeAndManager";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, declaredRoleUser, declaredPassword);
//        verifyExceptionWithUserAndRoles(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, declaredRoleUser, employeeRole, managerRole);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where the RolesAllowed
//     * <LI> annotation at method level controls access when RunAs and DeclareRoles at class level.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> A valid user in DeclaredRole is allowed access to EJB method.
//     * <LI>
//     * </OL>
//     */
//    @Override
//    @Test
//    public void testPureA05_DeclareRoles01_PermitAccessDeclaredUserRole() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=declareRoles01";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, declaredRoleUser, declaredPassword);
//        verifyResponseWithoutDeprecated(response, declaredRoleUserPrincipal, isManagerFalse, isEmployeeFalse, isDeclaredRoleTrue);
//
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet where there is class level RunAs (Employee).
//     * <LI> The injected EJB SecurityEJBA01Bean method RunAsClient is invoked by a user in Manager
//     * <LI> role. SecurityEJBA01Bean injects and invokes a second EJB SecurityEJBRunAsBean. This second EJB requires Manager role
//     * <LI> to invoke the manager method and therefore does not allow invocation when RunAs (Employee).
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Access is denied to second EJB because it is run as Employee role when Manager role is required to access manager method.
//     * <LI>
//     * </OL>
//     */
//
//    @Override
//    @Test
//    public void testPureA05_RunAsClient_DenyAccess() throws Exception {
//
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=runAsClient";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyExceptionWithUserAndRole(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, runAsUser, managerRole);
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }
//
//    /**
//     * Verify the following:
//     * <OL>
//     * <LI> Attempt to access an EJB method injected into a servlet with a class level RunAs (Employee) and RolesAllowed (Manager). There is no
//     * <LI> annotation at method level in the invoked EJB (SecurityEJBA01Bean) so it is invoked as Manager role. This first
//     * <LI> EJB injects and invokes a second EJB (SecurityRunAsBean) which requires Manager role to access the manager method.
//     * <LI> The RunAs (Employee) annotation results in the second EJB being invoked with employee role and therefore access to
//     * <LI> the manager method is denied. This test invokes SecurityEJBA05Bean methods with a variety of method signatures to insure that
//     * <LI> annotations are processed correctly with methods of the same name and different signature.
//     * </OL>
//     * <P> Expected Results:
//     * <OL>
//     * <LI> Manager role allowed access to first EJB method which invokes second EJB with RunAs (Employee) so access to
//     * <LI> is denied to second EJB method which requires Manager role.
//     * <LI>
//     * </OL>
//     */
//
//    @Override
//    @Test
//    public void testPureA05_RunAsSpecified_DenyAccess() throws Exception {
//        Log.info(thisClass, name.getMethodName(), "Entering " + name.getMethodName());
//
//        String queryString = "/SimpleServlet?testInstance=ejb05&testMethod=runAsSpecified";
//        String response = myClient.accessProtectedServletWithAuthorizedCredentials(queryString, managerUser, managerPassword);
//        verifyExceptionWithUserAndRole(response, EJBExceptionMessage, AuthDeniedUserNotGrantedAccessToRole, runAsUser, managerRole);
//        Log.info(thisClass, name.getMethodName(), "Exiting " + name.getMethodName());
//    }

}