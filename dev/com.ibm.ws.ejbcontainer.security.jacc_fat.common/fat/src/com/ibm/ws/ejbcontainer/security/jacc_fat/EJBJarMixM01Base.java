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

import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Base class for EJBJarMixM01Test. This class is extended for EJB in WAR testing.
 */
public abstract class EJBJarMixM01Base extends EJBAnnTestBase {

    String getEJBString() {
        return "ejbm01";
    }

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level
     * <LI> and no specification at method level or in the ejb-jar.xml deployment descriptor.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role when DenyAll at class level and no ejb-jar.xml elements.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarM01_DenyAll_DenyAllWithNoXML_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at class level
     * <LI> and exclude-list specified for method in ejb-jar.xml deployment descriptor.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Employee role since method is in the exclude-list.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_DenyAll_DenyAllOverrideByExcludelist_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and unchecked specified for method in ejb-jar.xml deployment descriptor. Unchecked overrides.
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_PermitAll_DenyAllOverrideByUnchecked_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and unchecked specified for method in ejb-jar.xml deployment descriptor. Unchecked overrides.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid user in manager role is allowed access.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_PermitAllwithParam_DenyAllOverrideByUnchecked_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Manager method-permission for method in ejb-jar.xml results in Manager role allowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_Manager_DenyAllOverrideByManager_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Manager method-permission for method in ejb-jar.xml results in only Manager role allowed.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but method-permission requires Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_ManagerWithParam_DenyAllOverrideByManager_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Manager method-permission for method in ejb-jar.xml results in only Manager role allowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for valid user not in a role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_ManagerNoAuth_DenyAllOverrideByManager_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee method-permission for method in ejb-jar.xml results in only Employee role allowed.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_Employee_DenyAllOverrideByEmployee_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee method-permission for method in ejb-jar.xml results in only Employee role allowed.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeWithParam_DenyAllOverrideByEmployee_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee method-permission for method in ejb-jar.xml results in only Employee role allowed.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed for user in no role when Employee role is required.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeNoAuth_DenyAllOverrideByEmployee_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee and Manager method-permission for method in ejb-jar.xml results in Employee and Manager roles allowed.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Employee role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeAndManager_DenyAllOverrideByMethodPermissionEmployeeManager_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee and Manager method-permission for method in ejb-jar.xml results in Employee and Manager roles allowed.
     * <LI> This test covers invoking the EJB method employeeAndManager() with one String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeAndManagerwithParam_DenyAllOverrideByMethodPermissionEmployeeManager_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servletwhere DenyAll annotation at class level.
     * <LI> Ejb-jar.xml lists method with method-permission Employee and Manager, also as unchecked and on
     * <LI> exclude-list. Exclude-list should override all the method-permission and unchecked.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is denied to user in Employee role because exclude-list overrides method-permission
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeAndManagerwithInt_NoAnnExcludelistOverridesOtherXMLPermissionEmployeeManager_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DenyAll annotation at class level
     * <LI> and Employee and Manager method-permission for method in ejb-jar.xml results in Employee and Manager roles allowed.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is denied access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_EmployeeAndManagerAllWithParamsNoAuth_DenyAllOverrideByMethodPermissionEmployeeManager_DenyAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.NO_ROLE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where DeclareRoles annotation at class
     * <LI> level an no annotation at method level. There is no entry in ejb-jar.xml for declared role
     * <LI> which results in access as unchecked.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Because the method is unchecked isCallerInRole will always return false.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_DeclareRoles01_DeclareRolesNoXMLPermissions_PermitAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=declareRoles01";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE, Constants.IS_DECLARED_ROLE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with DenyAll and RunAs(Employee) annotation.
     * <LI> The runAsSpecified method has a method-permission of Manager role in the ejb-jar.xml. The ejb-jar.xml
     * <LI> contains no run-as element, so the annotation RunAs(Employee) should take effect. The server.xml specifies
     * <LI> the run-as user as user99 in the Employee role. The first EJB injected in the servlet
     * <LI> injects and invokes a second EJB (SecurityRunAsBean) which requires Employee role to access the employee method.
     * <LU> The ejb-jar.xml <run-as> results in the second EJB employee being invoked successfully with run-as user99.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB that requires Employee role with run-as user.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarM01_RunAsSpecified_RunAsEmployeewithMethodPermissionManagerNoXMLRunAs_InjectedEJBRunAsEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}