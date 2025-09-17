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
 * Performs testing of EJB with the ejb-jar.xml deployment descriptor mixed with annotations.
 * The annotations specified at class level is @DenyAll with a mix of overriding annotations at
 * method level and overriding specifications in the ejb-jar.xml.
 *
 * The ejb-jar.xml (version 3.1) for this test specifies a variety permissions to cover the following:
 *
 * 1) denyAll() - PermitAll method annotation with ejb-jar method-permission Employee, results in only Employee role allowed access.
 * 2) denyAllwithParam() - PermitAll method annotation with ejb-jar exclude-list results in exclude-list override to deny access to all.
 * 3) permitAll() -PermitAll method annotation with no ejb-jar specification overrides class level DenyAll to permit access.
 * 4) permitAllwithParam() - no method annotation with no ejb-jar specification results in class level DenyAll to deny access.
 * 5) manager() - RolesAllowed(Manager) method annotation with method-permission Employee in ejb-jar specification overrides to permit only Employee.
 * 6) managerwithParam() - RolesAllowed(Manager) method annotation with method-permission Employee,Manager in ejb-jar verrides to permit both Employee, Manager.
 * 7) employee() - no method annotation with Employee method-permission in ejb-jar permits only Employee.
 * 8) employeewithParam() - no method annotation with unchecked in ejb-jar permits any valid user.
 * 9) employeeAndManager() - RolesAllowed(Employee,Manager) annotation with DeclaredRole01 method permission in ejb-jar allows only DeclaredRole user.
 * 10) employeeAndManagerwithParam() - RolesAllowed(Employee,Manager) annotation with permission,unchecked and exclude-list in ejb-jar denies access to all.
 * 11) employeeAndManagerwithInt() - RolesAllowed(Employee,Manager) annotation with no ejb-jar allows Employee and Manager roles.
 * 12) employeeAndManagerwithIParams() - RolesAllowed(Employee,Manager) annotation with exclude-list in ejb-jar denies access to all.
 *
 * This test invokes Singleton SecurityEJBM05Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class EJBJarMixM05Test extends EJBAnnTestBase {

    protected static Class<?> logClass = EJBJarMixM05Test.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR,
                    Constants.APPLICATION_SECURITY_EJB_JAR, Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, PermitAll
     * <LI> at method level and method-permission Employee in ejb-jar specification results in only Employee role getting access.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role when only Employee role allowed.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarM05_DenyAll_DenyAllClassPermitAllMethodOverrideByEmployeePermission_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.DENY_ALL_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, PermitAll
     * <LI> at method level and method-permission Employee in ejb-jar specification results in only Employee role getting access.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role when only Employee permitted.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarM05_DenyAll_DenyAllClassPermitAllMethodOverrideByEmployeePermission_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, PermitAll
     * <LI> at method level and exclude-list in ejb-jar specification results in exclude-list taking effect and access denied.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role when exclude-list overrides.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_DenyAllwithParam_DenyAllClassPermitAllMethodOverrideByExcludelist_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, PermitAll
     * <LI> at method level and no ejb-jar specification results in PermitAll taking effect.
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_PermitAll_DenyAllClassPermitAllMethodWithNoXML_PermitAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, no annotation
     * <LI> at method level and no ejb-jar specification results in access denied.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_PermitAllwithParam_DenyAllClassNoMethodAnnWithNoXML_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager)
     * <LI> at method level and method-permission Employee in ejb-jar specification results in only Employee role allowed access.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_Manager_DenyAllClassRolesAllowedManagerMethodOverrideByMethodPermissionEmployee_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager)
     * <LI> at method level and method-permission Employee in ejb-jar specification results in only Employee role allowed access.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role because xml permission requires Employee.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_Manager_DenyAllClassRolesAllowedManagerMethodOverrideByMethodPermissionEmployee_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager)
     * <LI> at method level and method-permission Employee and Manager in ejb-jar specification results in both Employee and Manager role allowed access.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_ManagerWithParam_DenyAllClassRolesAllowedManagerMethodOverrideByMethodPermissionEmployeeManager_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager)
     * <LI> at method level and method-permission Employee and Manager in ejb-jar specification results in both Employee and Manager role allowed access.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_ManagerWithParam_DenyAllClassRolesAllowedManagerMethodOverrideByMethodPermissionEmployeeManager_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager)
     * <LI> at method level and method-permission Employee and Manager in ejb-jar specification results in both Employee and Manager role allowed access.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for user ID in no role because xml permission requires Employee or Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_ManagerWithParam_DenyAllClassRolesAllowedManagerMethodOverrideByMethodPermissionEmployeeManager_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, no annotation
     * <LI> at method level and method-permission Employee in ejb-jar specification results in Employee role allowed access.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_Employee_DenyAllClassNoMethodAnnOverrideByMethodPermissionEmployee_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, no annotation
     * <LI> at method level and method-permission Employee in ejb-jar specification results in Employee role allowed access.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for user ID in Manager role because xml permission requires Employee.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_Employee_DenyAllClassNoMethodAnnOverrideByMethodPermissionEmployee_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, no annotation
     * <LI> at method level and unchecked in ejb-jar specification results in all valid users allowed access.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeWithParam_DenyAllClassNoMethodAnnOverrideByUnchecked_PermitAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and method-permission Manager in ejb-jar specification results in only Manager role allowed access.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManager_DenyAllClassRolesAllowedEmployeeManagerOverrideByMethodPermissionDeclaredRole_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and method-permission DeclaredRole in ejb-jar specification results in only Manager role allowed access.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for user ID in Employee role because xml permission requires DeclaredRole.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManager_DenyAllClassRolesAllowedEmployeeManagerOverrideByMethodPermissionDeclaredRole_DenyAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and method-permission Manager + unchecked + exclude-list in ejb-jar specification results in all excluded.
     * <LI> This test covers invoking the EJB method employeeAndManager() with one String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role because exclude-list overrides.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManagerwithParam_DenyAllClassRolesAllowedEmployeeManagerOverrideByManagerUncheckedExcludeList_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and no ejb-jar specification results in both Manager and Employee roles allowed.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in Employee role is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManagerwithInt_DenyAllClassRolesAllowedEmployeeManagerWithNoXML_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and no ejb-jar specification results in both Manager and Employee roles allowed.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is denied to user in no role because Employee or Manager is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManagerwithInt_DenyAllClassRolesAllowedEmployeeManagerWithNoXML_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level, RolesAllowed(Manager,Employee)
     * <LI> at method level and exclude-list specification results in no users having access.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParms with mutliple string parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization fails for valid user in no role since exclude-list overrides.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM05_EmployeeAndManagerwithInt_DenyAllClassRolesAllowedEmployeeManagerOverrideByExcludelist_DenyAccessNoRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm05&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}