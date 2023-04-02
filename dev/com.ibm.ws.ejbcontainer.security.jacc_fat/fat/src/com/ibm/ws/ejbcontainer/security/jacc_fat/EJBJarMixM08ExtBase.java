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
 * Base class for EJBJarMixM08Ext tests. This class is extended for EJB in WAR testing.
 */
public abstract class EJBJarMixM08ExtBase extends EJBAnnTestBase {

    String getEJBString() {
        return "ejbm08";
    }

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at method level.
     * <LI> Permission should be denied for the denyAll method even though the extensions files specifies
     * <LI> that this method is to call others with run-as-mode CALLER_IDENTITY.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarExtM08_DenyAll_DenyAllMethodWithExtRunAsManager_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation.
     * <LI> When no ejb-jar and no entry in ibm-ejb-jar-ext.xml theannotation should take effect to invoke
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
    public void testEJBJarExtM08_PermitAll_RunAsEmployeeAnnInEffectWhenNoEXT_PermitAccessRunAsEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponse(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.NO_ROLE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation. This
     * <LI> annotation does not take effect because the employee method requires Employee role and Manager user invokes.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role denied access to first EJB so second EJB is not called with run-as user.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM08_Employee_RunAsEmployeeNoEffectWhenAccessDenied_DenyAccessManagerRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation. There is
     * <LI> no ejb-jar.xml. The extensions file has run-as SPECIFIED_IDENTITY with same role Employee as annotation. Employee user role
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
    public void testEJBJarExtM08_Employee_RunAsEmployeeAnnOverrideByExtSameEmployeeRole_PermitAccessEmployeeRunAsUser() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.RUN_AS_USER_PRINCIPAL, Constants.RUN_AS_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override and no ejb-jar.xml.
     * <LI> The extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with caller employee user1 and access is permitted.
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
    public void testEJBJarExtM08_EmployeeWithParam_RunAsEmployeeOverrideByExtCallerIdentityEmployeeUser_PermitAccessEmployeeRoleUser() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and no
     * <LI> ejb-jar.xml. The extensions file overrides the run-as with run-as-mode SPECIFIED_IDENTITY (manager) so
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
    public void testEJBJarExtM08_Manager_RunAsEmployeeOverrideByExtSpecifiedIdentity_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and no ejb-jar.xml. The
     * <LI> extensions file has run-as-mode mode=CALLER_IDENTITY specified for this method signature it overrides the annotation.
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
    public void testEJBJarExtM08_ManagerWithParam_RunAsEmployeeOverridedByExtRunAsCaller_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and no ejb-jar.xml. The
     * <LI> extensions file has run-as-mode mode=CALLER_IDENTITY specified for this method signature it overrides the annotation.
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
    public void testEJBJarExtM08_ManagerWithParam_RunAsEmployeeOverridedByExtRunAsCallerDeclaredRole_DenyAccessDeclaredrole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                       Constants.DECLARED_ROLE_USER,
                                       Constants.MANAGER_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override. There is
     * <LI> no ejb-jar.xml file. The extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with specified user5 in Declared role and access is granted.
     * <LI> This test covers invoking the EJB method employeeAndManager with no parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employeeAndManager method run-as manager CALLER_IDENTITY
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM08_EmployeeAndManager_RunAsEmployeeOverrideByExtCallerIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.DECLARED_ROLE_USER, Constants.DECLARED_ROLE_USER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the EJB RunAs(Employee) annotation. There is no
     * <LI> ejb-jar.xml. The extensions file have no run-as specifications. The annotation should take effect to invoke
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
    public void testEJBJarExtM08_employeeAndManagerwithParam_RunAsEmployeeAnnInEffectWhenNoExt_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.RUN_AS_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override. The
     * <LI> ejb-jar.xml has no run-as specified and extensions file has run-as-mode mode=SPECIFIED_IDENTITY. The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with specified declared user role and access is granted.
     * <LI> This test covers invoking the EJB method employeeAndManager with two String parameters as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employee method run-as declared SPECIFIED_IDENTITY
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM08_EmployeeAndManagerwithParams_RunAsEmployeeOverrideByExtSpecifiedIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override. The
     * <LI> ejb-jar.xml has no run-as specified and extensions file has run-as-mode mode=SYSTEM_IDENTITY (not supported). The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with specified system role and access is denied.
     * <LI> This test covers invoking the EJB method employeeAndManager with single int parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which invokes second EJB employee method run-as SYSTEM_IDENTITY.
     * <LI> Access is denied since SYSTEM_IDENTITY is not supported and exception message should be received.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM08_EmployeeAndManagerwithInt_RunAsEmployeeOverrideByExtSystemIdentity_DenyAccessSystemIdentityNotSupported() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=" + getEJBString() + "&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithMethod(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.AUTH_DENIED_SYSTEM_IDENTITY_NOT_SUPPORTED, "employeeAndManager");
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

}