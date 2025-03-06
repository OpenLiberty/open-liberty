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
 * Base class for EJBJarMixM07Ext tests. This class is extended for EJB in WAR testing.
 */

public abstract class EJBJarMixM07ExtBase extends EJBAnnTestBase {

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the DenyAll annotation at method level and no
     * <LI> ejb-jar.xml method permission. Permission should be denied even though the extensions files specifies
     * <LI> that this method is to call others with run-as-mode SPECIFIED_IDENTITY.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarExtM07_DenyAll_DenyAllAnnWithExtRunAsManager_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());
        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with annotations @PermitAll class level and ejb-jar.xml lists
     * <LI> denyAll method in excludeList. Permission should be denied even though the extensions files specifies
     * <LI> that this method is to call others with run-as-mode SPECIFIED_IDENTITY. This shows that having an extension file
     * <LI> does not affect the normal annotations and ejb-jar.xml processing.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role since method is in excludeList.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarExtM07_DenyAll_XMLExcludeListWithExtRunAsManager_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ejb-jar run-as-caller.
     * <LI> The extensions file has no run-as specifications, so the ejb-jar run-as-caller should take effect to invoke
     * <LI> the second EJBs employee method as caller in employee role (user1).
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which successfully invokes second EJB method run-as caller in employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_Employee_EjbJarRunAsCallerInEffectWhenNoExt_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ejb-jar run-as-caller.
     * <LI> The extensions file has no run-as specifications, so the ejb-jar use-caller-identity should take effect to invoke
     * <LI> the second EJBs employee method as caller in employee role (user1).
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which is denied access to second EJB which requires employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_Employee_EjbJarRunAsCallerInEffectWhenNoExt_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation. The
     * <LI> ejb-jar.xml has use-caller-identity and extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file
     * <LI> and ejb-jar are the same so EJB invokes the second EJB employee method with caller user1 in Employee role and access is granted.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role user allowed access to first EJB method which successfully invokes second EJB employee method run-as CALLER_IDENTITY (user1)
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_EmployeeWithParam_EjbJarAndExtCallerIdentity_PermitAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation with ejb-jar use-caller-identity
     * <LI> and ext override. The extensions file has run-as-mode mode=CALLER_IDENTITY. The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB employee method with specified user in Manager role and access is granted.
     * <LI> This test covers invoking the EJB method employeewithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY so access
     * <LI> is denied to second EJB method which requires Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_EmployeeWithParam_EjbJarAndExtCallerIdentity_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeewithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation. The ejb-jar.xml has
     * <LI> use-caller-identity and extensions file has no run-as specifications, so the ejb-jar should take effect to invoke
     * <LI> the second EJBs manager method, which requires Manager role, as caller identity manager user and access permitted.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB with caller identity manager so access
     * <LI> is allowed to second EJB method which requires Manager role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_Manager_UseCallerIdentityInEffectWhenNoEXT_PermitAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponse(response, Constants.MANAGER_USER_PRINCIPAL, Constants.MANAGER_USER_IDENTITY, Constants.IS_MANAGER_TRUE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override. The
     * <LI> ejb-jar.xml user-caller-identity and extensions file has run-as-mode mode=SPECIFIED_IDENTITY with DeclaredRole01. The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB manager method with specified user5 in DeclaredRole01 role and access is denied.
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role allowed access to first EJB method which invokes second EJB run-as CALLER_IDENTITY so access
     * <LI> is denied to second EJB method which requires Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_ManagerWithParam_UserCallerIdentityOverrideByExtSpecifiedIdentity_DenyAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE,
                                       Constants.DECLARED_ROLE_USER,
                                       Constants.MANAGER_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation and ext override. The
     * <LI> ejb-jar.xml has use-caller-identity and extensions file has run-as-mode mode=SPECIFIED_IDENTITY declaredRole.
     * <LI> The first EJB invokes the second EJB employeeAndManager method with specified user5 in Declared role and access is granted.
     * <LI> This test covers invoking the EJB method employeeAndManager with no parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employee method run-as declared role CALLER_IDENTITY
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_EmployeeAndManager_UseCallerIdentityOverrideBySpecifiedIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the EJB RunAs(Employee) annotation. The
     * <LI> ejb-jar.xml has use-caller-identity extensions file has no run-as specifications. The ejb-jar should take effect to invoke
     * <LI> the second EJBs employeeAndManager method, which requires declared role, with Employee run-as user so permission is denied
     * <LI> This test covers invoking the EJB method employeeAndManager(String) with single string parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Employee role allowed access to first EJB method which invokes second EJB as caller identity employee so access
     * <LI> is denied to second EJB method which requires Declared role.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_employeeAndManagerwithParam_UseCallerIdentityEmployeeInEffectWhenNoEXT_DenyAccessEmployee() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeeAndManagerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.EMPLOYEE_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);
        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation, ejb-jar use-caller-identity and ext override. The
     * <LI> ejb-jar.xml has use-caller-identity and extensions file has run-as-mode mode=SPECIFIED_IDENTITY Manager. The extension file
     * <LI> overrides the ejb-jar so EJB invokes the second EJB employee method with specified manager user and access is denied.
     * <LI> This test covers invoking the EJB method employeeAndManager with two String parameters as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employee method run-as manager SPECIFIED_IDENTITY
     * <LI> and access is denied because declared role is required for second EJB.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_EmployeeAndManagerwithParams_UseCallerIdentityOverrideByExtSpecifiedIdentity_DenyAccessManager() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithUserAndRole(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE, Constants.MANAGER_USER,
                                       Constants.EMPLOYEE_AND_MANAGER_METHOD);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation, ejb-jar use-caller-identity and ext override. The
     * <LI> ejb-jar.xml has use-caller-identity and extensions file has run-as-mode mode=SYSTEM_IDENTITY (not supported). The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with specified system role and access is denied
     * <LI> This test covers invoking the EJB method employeeAndManager with single int parameter as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which invokes second EJB employee method run-as SYSTEM_IDENTITY
     * <LI> and access is denied because SYSTEM_IDENTITY is not supported. An excpetion message should be received.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_EmployeeAndManagerwithInt_UseCallerIdentityOverrideByExtSystemIdentity_DenyAccessSystemIdentityNotSupported() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=employeeAndManagerwithInt";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyExceptionWithMethod(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.AUTH_DENIED_SYSTEM_IDENTITY_NOT_SUPPORTED, "employee");

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet with the RunAs(Employee) annotation, ejb-jar use-caller-identity and ext override. The
     * <LI> ejb-jar.xml has use-caller-identity and extensions file has run-as-mode mode=SPECIFIED_IDENTITY. The extension file
     * <LI> overrides the annotation so EJB invokes the second EJB employee method with specified declared run-as role and access is granted.
     * <LI> This test covers invoking the EJB method declaredRole01 as named in the extensions file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Manager role user allowed access to first EJB method which successfully invokes second EJB employee method run-as SYSTEM_IDENTITY.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarExtM07_declaredRoles01_UseCallerIdentityOverrideByExtSpecifiedIdentity_PermitAccessDeclaredRole() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        String queryString = "/SimpleXMLServlet?testInstance=ejbm07&testMethod=declareRoles01";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.DECLARED_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }
}