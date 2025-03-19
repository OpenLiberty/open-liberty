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

public abstract class EJBJarMixMC06Base extends EJBAnnTestBase {

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml, annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, DenyAll
     * <LI> at method level and no ejb-jar specification results in ignoring DenyAll annotation and allows no role access.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method since annotations are ignored.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarMC06_DenyAll_PermitAllClassOverrideByMetadataCompleteWhenNoXML_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml, annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, DenyAll
     * <LI> at method level and Manager role method-permission in ejb-jar specification results in Manager only allowed.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_DenyAllwithParam_PermitAllClassDenyAllMethodOverrideByXMLManagerPermission_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, DenyAll
     * <LI> at method level and Manager role method-permission in ejb-jar specification results in Manager only allowed.
     * <LI> This test covers invoking the EJB method denyAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but method-permission requires Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_DenyAllwithParam_PermitAllClassDenyAllMethodOverrideByXMLManagerPermission_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=denyAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.DENY_ALL_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, no annotation
     * <LI> at method level and no method-permission in ejb-jar specification results in PermitAll.
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_PermitAll_PermitAllClassWithNoXMLOverrideByMetadataComplete_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, no annotation
     * <LI> at method level and exclude-list in ejb-jar specification results in access denied.
     * <LI> This test covers invoking the EJB method permitAllwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role exclude-list overrides PermitAll when metadata-complete.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_PermitAllwithParam_PermitAllClassOverrideByXMLExcludelist_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=permitAllwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, no annotation
     * <LI> at method level and method-permission Manager in ejb-jar specification results in Manager only allowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_Manager_PermitAllClassOverrideByXMLMethodPermissionManager_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, no annotation
     * <LI> at method level and method-permission Manager in ejb-jar specification results in Manager only allowed.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but method-permission requires Manager.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_ManagerNoAuth_PermitAllClassOverrideByXMLMethodPermissionManager_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, DenyAll annotation
     * <LI> at method level and unchecked in ejb-jar specification results in all allowed access- unchecked.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_ManagerWithParam_PermitAllClassDenyAllMethodOverrideByXMLUnchecked_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee)
     * <LI> at method level and no ejb-jar specification results ignoring RolesAllowed annotation and no checking for user in no role allowed access.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_Employee_PermitAllClassOverrideByRolesAllowedEmployeeOverrideByMetadataCompleteNoXML_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee)
     * <LI> at method level and no ejb-jar specification with metadata-complete results in ignoring annotation Employee role such that
     * <LI> a user in noRole is allowed access.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_Employee_PermitAllClassOverrideByRolesAllowedEmployeeNoXML_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee)
     * <LI> at method level and method-permission Manager in ejb-jar specification results in only Manager role allowed.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeWithParam_PermitAllClassRolesAllowedEmployeeOverrideByMethodPermissionManager_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee)
     * <LI> at method level and method-permission Manager in ejb-jar specification results in only Manager role allowed.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> EJBAccessException when user Employee but method-permission requires Manager
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeWithParam_PermitAllClassRolesAllowedEmployeeOverrideByXMLMethodPermissionManager_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.EMPLOYEE_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee,Manager)
     * <LI> at method level and unchecked in ejb-jar specification results in unchecked so all have access.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeAndManager_PermitAllClassRolesAllowedEmployeeManagerOverrideByUnchecked_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee,Manager)
     * <LI> at method level and exclude-list in ejb-jar specification results in all denied access.
     * <LI> This test covers invoking the EJB method employeeAndManager() with one String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in Manager role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeAndManagerwithParam_PermitAllClassRolesAllowedEmployeeManagerOverrideByXMLExcludelist_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee,Manager)
     * <LI> at method level and method-permission Employee only in ejb-jar specification results in only Employee role allowed.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is permitted to user in Employee role because because ejb-jar requires employee.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeAndManagerwithInt_PermitAllClassRolesAllowedEmployeeManagerOverrideByXMLMethodPermEmployee_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, RolesAllowed(Employee,Manager)
     * <LI> at method level and method-permission Employee only in ejb-jar specification results in only Employee role allowed.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithInt(int) with single integer parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is denied to user in Manager role because ejb-jar method permission requires Employee.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_EmployeeAndManagerwithInt_PermitAllClassRolesAllowedEmployeeManagerOverrideByXMLMethodPermEmployee_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the RunAs(Employee) annotation is overridden
     * <LI> by ejb-jar.xml use-caller-identity. No role user invokes the runAsClient method which invokes a second EJB (SecurityEJBMCRunAsBean). Since
     * <LI> use-caller-identity is specifed in ejb-jar.xml, the second EJB is invoked with no role user (caller identity) and access to
     * <LI> the employee method of second EJB is allowed because metadata-complete is true for this bean as well and annotation
     * <LI> requiring Employee role is ignored since method is unprotected in ejb-jar.xml.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization permitted since second bean is invoked using caller identity (no role user)and annotations are ignored
     * <LI> and all roles are allowed access since metadata-complete is true for the SecurityEJBMCRunAsBean.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_runAsClient_MetadataCompleteRunAsClientOverrideAnnotationRunAsEmployee_PermitAccessNoRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=runAsClient";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll annotation at class level, DenyAll
     * <LI> at method level are ignored and ejb-jar.xml does not specify a permission so all roles are allowed access to declareRole01 method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization permitted since annotations are overridden by ejb-jar.xml which does not specify a constraint and therefore is unchecked.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_DeclareRoles01_PermitAllClassDenyAllMethodOverrideWhenNoXML_PermitAccess() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=declareRoles01";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet with PermitAll and RunAs(Employee) annotations.
     * <LI> The ejb-jar.xml specifies to run as client identity. So when the runAsSpecified method is invoked with manager user
     * <LI> it should invoke the second EJB (SecurityEJBMCRunAsBean)as manager (client identity) since annotation that specifies RunAs (Employee) is
     * <LI> now ignored. In the SecurityEJBMCRunAsBean and its manager method requires Manager role to run it in ejb-jar.xml.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB. Access permitted since run as client
     * <LI> identity is used and second EJB requires Manager role (role of client identity) to access its manager method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarMC06_RunAsSpecified_RunAsEmployeeOverrideByXMLUseCallerIdentity_PermitAccessRunAsMethodByManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> With metadata-complete in ejb-jar.xml annotations should be ignored and only ejb-jar.xml used.
     * <LI> Attempt to access an EJB method injected into a servlet with PermitAll and RunAs(Employee) annotations.
     * <LI> The ejb-jar.xml specifies to run as client identity. So when the runAsSpecified method is invoked with Employee user,
     * <LI> it should invoke the second EJB (SecurityEJBMCRunAsBean) as client identity of employee user.
     * <LI> The ejb-jar.xml for the SecurityEJBMCRunAsBean requires Manager role to invoke manager method so should fail.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role allowed access to first EJB method (runAsSpecified) which invokes second EJB manager method that requires Manager
     * <LI> role in ejb-jar.xml. Therefore Employee role is not allowed access to SecurityEJBMCRunAsBean manager method.
     * <OL>
     * <LI> Access is denied since ejb-jar.xml specifies that the manager method of second EJB requires Manager role.
     * </OL>
     */
    @Test
    public void testEJBJarMC06_RunAsSpecified_RunAsEmployeeOverrideByXMLUseCallerIdentity_DenyAccessRunAsMethodByEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleMCServlet?testInstance=ejbmc06&testMethod=runAsSpecified";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.RUN_AS_SPECIFIED_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}