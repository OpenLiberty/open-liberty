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
 * The ibm-ejb-jar-ext.xml contains exclude-list for denyAll method and run-as role of Employee for method name=* to indicate all methods.
 *
 * Permissions are set up to test that ibm-ejb-jar-ext.xml extensions
 * for run-as will override the ejb-jar.xml use-caller-identity with SPECIFIED_IDENTITY Employee for all methods (*).
 *
 *
 * This test invokes Stateless SecurityEJBM10Bean methods with a variety of method signatures to insure that
 * ibm-ejb-jar-ext.xml settings are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarMixM10ExtTest extends EJBAnnTestBase {

    protected static Class<?> logClass = EJBJarMixM10ExtTest.class;

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
     * <LI> that this method is to call others with run-as-mode SPECIFIED_IDENTITY.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarExtM10_DenyAll_ExcludeListWithExtRunAsManager_DenyAccessManager() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm10&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml use-caller-identity and no annotations.
     * <LI> The extensions file has run-as SPECIFIED_IDENTITY with run-as role Employee. Employee user role
     * <LI> should be allowed access to first EJB to invoke the second EJBs employee method as Employee run-as user.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role allowed access to first EJB method which invokes second EJB run-as SPECIFIED_IDENTITY(employee)so access
     * <LI> is permitted to second EJB by employee method which requires Employee role by run-as user99 .
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM10_Employee_EjbJarRunAsCallerOverrideByEXTSameSpecifiedEmployeeRole_PermitAccessEmployeeRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm10&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml use-caller-identity and no annotations.
     * <LI> The extensions file has run-as-mode mode=SPECIFIED_IDENTITY. The extension file overrides the ejb-jar run-as
     * <LI> so EJB invokes the second EJB employee method with specified employee identity and access is permitted.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB run-as SPECIFIED_IDENTITY (employee) so access
     * <LI> is granted to second EJB method which requires Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM10_EmployeeWithParam_EjbJarRunAsCallerOverrideByExtSpecifiedIdentity_PermitAccessEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm10&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml use-caller-identity and no annotations.
     * <LI> The extensions file overrides the ejb-jar run-as with the SPECIFIED_IDENTITY Employee. When it invokes
     * <LI> the second EJBs manager method, which requires Manager role, permission will be denied using specified employee role.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB RunAs SPECIFED_IDENTITY Employee so access
     * <LI> is denied to second EJB method which requires Manager role on manager method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM10_Manager_EjbJarRunAsCallerOverrideByEXTSpecifiedIdentity_DenyAccessRunAsEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm10&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the ejb-jar.xml use-caller-identity and no annotations.
     * <LI> The extensions file overrides the ejb-jar run-as with the SPECIFIED_IDENTITY Employee. When it invokes
     * <LI> the second EJBs manager method, which requires Manager role, permission will be denied using specified employee role.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB RunAs SPECIFED_IDENTITY Employee so access
     * <LI> is denied to second EJB method which requires Manager role on manager method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM10_ManagerWithParam_EjbJarRunAsCallerOverridedByEXTSpecifiedIdentity_DenyAccessRunAsEmployee() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm10&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}