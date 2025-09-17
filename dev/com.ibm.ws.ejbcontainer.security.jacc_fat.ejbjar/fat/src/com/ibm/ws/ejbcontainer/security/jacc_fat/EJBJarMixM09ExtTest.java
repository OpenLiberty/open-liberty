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
 * Performs testing of EJB with the extensions file ibm-ejb-jar-ext.xml along with
 * an ejb-jar.xml file. There are no security annotations.
 *
 * The ibm-ejb-jar-ext.xml contains method level permissions for manager and employee methods requiring Manager
 * and Employee roles, exclude-list for denyAll method and run-as role of Employee for the bean.
 *
 * Permissions are set up to test that ibm-ejb-jar-ext.xml extensions
 * for run-as will override those in the ejb-jar.xml when there are conflicts.
 * The run-as settings in ibm-ejb-jar-ext.xml take effect at the specified
 * method level such that the methods listed in the extensions file
 * will use the caller as specified in the extensions file when calling those EJB methods.
 *
 * The ibm-ejb-jar-ext.xml tests method level run-as settings for CALLER_IDENTITY, SPECIFIED_IDENTITY,
 * and SYSTEM_IDENTITY (not supported).
 *
 * The test also verifies that method-permission security constraints in ejb-jar.xml are still enforced when
 * an extensions file is present.
 *
 * This test invokes Singleton SecurityEJBM09Bean methods with a variety of method signatures to insure that
 * ibm-ejb-jar-ext.xml method level run-as settings are processed correctly with methods of the same name and different signature.
 * The SecurityEJBM09Bean invokes the SecurityEJBRunAsExtBean methods from within its methods based on the run-as user specified.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarMixM09ExtTest extends EJBAnnTestBase {

    protected static Class<?> logClass = EJBJarMixM09ExtTest.class;

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
     * <LI> Attempt to access an EJB method injected into a servlet where the denyAll is on exclude-list in ejb-jar.
     * <LI> Permission should be denied to the denyAll method even though the extensions files specifies
     * <LI> that this method is to call others with run-as-mode CALLER_IDENTITY.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarExtM09_DenyAll_ExcludeListWithExtRunAsManager_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> When ejb-jar and no entry in ibm-ejb-jar-ext.xml the ejb-jar run-as specification should take effect to invoke
     * <LI> the second EJBs employee method as Employee role run-as user -- user99.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> No role user allowed access to first EJB method which successfully invokes second EJB method run-as Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_PermitAll_EjbJarRunAsInEffectWhenNoExt_PermitAccessRunAsEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> The ejb-jar run-as does not take effect because he employee method requires Employee role and Manager user invokes it.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role denied access to first EJB so second EJB is not called with run-as user.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_Employee_EJBJarEmployeeRolePermission_DenyAccessManagerRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> The extensions file has run-as SPECIFIED_IDENTITY with same run-as role Employee as ejb-jar. Employee user role
     * <LI> should be allowed access to first EJB to invoke the second EJBs employee method as Employee run-as user.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY (employee)so access
     * <LI> is permitted to second EJB by employee method which requires Employee role by run-as user99 .
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_Employee_EjbJarRunAsOverrideByExtRunAsEmployeeRole_PermitAccessEmployeeRunAsRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> The extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file overrides the ejb-jar run-as
     * <LI> so EJB invokes the second EJB employee method with caller employee user1 and access is permitted.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY (user1 in employee)so access
     * <LI> is granted to second EJB method which requires Employee role. Rather than being invoked as run-as user99, the
     * <LI> second EJB will be invoked with employee user1.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_EmployeeWithParam_EjbJarRunAsEmployeeOverrideByExtCallerIdentityEmployeeUser_PermitAccessEmployeeRoleUser() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> The extensions file overrides the ejb-jar run-as Employee with run-as-mode SPECIFIED_IDENTITY (manager) so
     * <LI> the second EJBs manager method, which requires Manager role, will be invoked with manager user and access allowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB RunAs SPECIFED_IDENTITY so access
     * <LI> is allowed to second EJB method which requires Manager role on manager method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_Manager_EjbJarRunAsEmployeeOverrideByExtSpecifiedIdentity_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml run-as Employee and no annotations.
     * <LI> The extensions file has run-as-mode mode=CALLER_IDENTITY specified for this method signature it overrides the ejb-jar run-as.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY manager
     * <LI> and access is permitted to second EJB method mananger.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_ManagerWithParam_EjbJarRunAsEmployeeOverridedByExtRunAsCaller_PermitAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar run-as Employee and no annotations. The
     * <LI> extensions file has run-as-mode mode=CALLER_IDENTITY specified for this method signature it overrides the ejb-jar.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY declaredRole
     * <LI> and access is denied to second EJB method which requires manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM09_ManagerWithParam_EjbJarRunAsEmployeeOverridedByExtRunAsCallerDeclaredRole_DenyAccessDeclaredRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                       Constants.DECLARED_ROLE_USER,
                                       Constants.MANAGER_METHOD);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar run-as Employee and no annotations.
     * <LI> The extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB employee method with specified user5 in Declared role and access is granted.
     * <LI> This test covers invoking the EJB method employeeAndManager with no parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employeeAndManager method run-as manager CALLER_IDENTITY
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_EmployeeAndManager_EjbJarRunAsEmployeeOverrideByExtCallerIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar run-as Employee and no annotations.
     * <LI> The extensions file have no run-as specifications. The ejb-jar should take effect to invoke
     * <LI> the second EJBs employeeAndManager method, which requires any declared role, to be invoked with Employee run-as user.
     * <LI> This test covers invoking the EJB method employeeAndManager(String) with single string parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB RunAs Employee run-as user99 so access
     * <LI> is denied to second EJB method which requires Declared role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_employeeAndManagerwithParam_EjbJarRunAsEmployeeAnnInEffectWhenNoExt_DenyAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar run-as Employee and no annotations.
     * <LI> The extensions file has run-as-mode mode=SPECIFIED_IDENTITY. The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB employee method with specified declared user role and access is granted.
     * <LI> This test covers invoking the EJB method employeeAndManager with two String parameters as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employee method run-as declared SPECIFIED_IDENTITY
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_EmployeeAndManagerwithParams_EjbJarRunAsEmployeeOverrideByExtSpecifiedIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar run-as Employee and no annotations.
     * <LI> The extensions file has run-as-mode mode=SYSTEM_IDENTITY (not supported). The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB employee method with specified system role and access is denied.
     * <LI> This test covers invoking the EJB method employeeAndManager with single int parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which invokes second EJB employee method run-as SYSTEM_IDENTITY.
     * <LI> Access is denied since SYSTEM_IDENTITY is not supported. An exception message should be received.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM09_EmployeeAndManagerwithInt_EjbJarRunAsEmployeeOverrideByExtSystemIdentity_DenyAccessSystemIdentityNotSupported() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm09&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithMethod(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.AUTH_DENIED_SYSTEM_IDENTITY_NOT_SUPPORTED, "employeeAndManager");
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}