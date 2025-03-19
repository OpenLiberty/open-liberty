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

package com.ibm.ws.ejbcontainer.security.jacc_fat.audit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.junit.ClassRule;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.FeatureReplacementAction;

import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ejbcontainer.security.jacc_fat.Constants;
import com.ibm.ws.ejbcontainer.security.jacc_fat.EJBAnnTestBase;
import com.ibm.ws.ejbcontainer.security.jacc_fat.MessageConstants;
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Performs testing of EJB with only the ejb-jar.xml deployment descriptor (metadata-complete=true) and no annotations.
 *
 * The ejb-jar.xml (version 3.1) for this test specifies the following in order to verify override behavior
 * 1) All methods are specified as unchecked with *
 * 2) Methods employee, manager and employeeAndmanager are also assigned <method-permission> which is overridden by unchecked
 * 3) The denyAll methods are listed under exclude-list which overrides unchecked and method-permission
 *
 * This test invokes SecurityEJBX03Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class EJBJarX03JACCAuditTest extends EJBAnnTestBase {

    /**
     * Need the first repeat to make sure that audit-2.0 from a previous repeat gets put back to audit-1.0
     */
    @ClassRule
    public static RepeatTests auditRepeat = RepeatTests.with(new FeatureReplacementAction("audit-2.0", "audit-1.0").forServers(Constants.SERVER_EJBJAR_AUDIT).fullFATOnly())
                    .andWith(new FeatureReplacementAction("audit-1.0", "audit-2.0").forServers(Constants.SERVER_EJBJAR_AUDIT));

    protected static Class<?> logClass = EJBJarX03JACCAuditTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJBJAR_AUDIT,
                    Constants.APPLICATION_SECURITY_EJB_JAR, Constants.SERVLET_SECURITY_EJBXML, Constants.CONTEXT_ROOT_SECURITY_EJB_JAR);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the denyAll
     * <LI> method is specified in the exclude-list in the ejb-jar.xml deployment descriptor.
     * <LI> This test covers invoking the EJB method denyAll with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Authorization failed exception for valid userId in Manager role since method is in the exclude-list.
     * <LI>
     * </OL>
     */

    @Test
    public void testEJBJarX03_DenyAll_excludelist_DenyAccessManagerForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=denyAll";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyException(response, MessageConstants.EJB_ACCESS_EXCEPTION, MessageConstants.JACC_AUTH_DENIED_USER_NOT_GRANTED_REQUIRED_ROLE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user2",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.FAILURE));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the permitAll
     * <LI> method-permission specifies unchecked in the ejb-jar.xml deployment descriptor.
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A valid userId in no role is allowed to access the EJB method.
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarX03_PermitAll_unchecked_PermitAccessNoRoleForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The manager
     * <LI> method is specified with a method-permission of Manager role in the ejb-jar.xml.
     * <LI> This test covers invoking the EJB method manager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> IsCaller in Role Should return false since all the methods are unchecked.
     * </OL>
     */
    @Test
    public void testEJBJarX03_Manager_methodPermissionManager_PermitAccessManagerForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=manager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user2",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. All methods are unchecked (*) which overrides the
     * <LI> method specified with a method-permission of Manager role in the ejb-jar.xml,
     * <LI> This test covers invoking the EJB method managerwithParam(String) with single String parameter.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> IsCaller in Role Should return false since all the methods are unchecked.
     * </OL>
     */
    @Test
    public void testEJBJarX03_ManagerWithParam_uncheckedOverridesMethodPermissionManager_PermitAccessEmployeeForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=managerwithParam";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The employee
     * <LI> method is specified with a method-permission of Employee role in the ejb-jar.xml.
     * <LI> This test covers invoking the EJB method employee() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> IsCaller in Role Should return false since all the methods are unchecked.
     * </OL>
     */
    @Test
    public void testEJBJarX03_Employee_methodPermissionEmployee_PermitAccessEmployeeForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=employee";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponseWithoutDeprecated(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The employeeAndManager
     * <LI> method is specified with a method-permission of Employee and Manager role in the ejb-jar.xml.
     * <LI> This test covers invoking the EJB method employeeAndManager() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> IsCaller in Role Should return false since all the methods are unchecked.
     * </OL>
     */
    @Test
    public void testEJBJarX03_EmployeeAndManager_methodPermissionEmployeeManager_PermitAccessManagerForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=employeeAndManager";
        String response = generateResponseFromServlet(queryString, Constants.MANAGER_USER, Constants.MANAGER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.MANAGER_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user2",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet. The employeeAndManager
     * <LI> method is specified with a method-permission of Employee and Manager role in the ejb-jar.xml, but
     * <LI> since the method is also unchecked, unchecked overrides method-permission allowing access to all.
     * <LI> This test covers invoking the EJB method employeeAndManagerwithParams(String,String) with two String parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access is permitted for user in no role because unchecked overrides method-permission in ejb-jar.xml
     * <LI>
     * </OL>
     */
    @Test
    public void testEJBJarX03_EmployeeAndManagerAllWithParams_uncheckedOverridesMethodPermissionEmployeeManager_PermitAccessNoRoleForJACCAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());
        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleXMLServlet?testInstance=ejbx03&testMethod=employeeAndManagerwithParams";
        String response = generateResponseFromServlet(queryString, Constants.NO_ROLE_USER, Constants.NO_ROLE_USER_PWD);
        verifyResponseWithoutDeprecated(response, Constants.NO_ROLE_USER_PRINCIPAL, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_FALSE);

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user3",
                                                 AuditEvent.TARGET_NAME + "=/securityejbjar/SimpleXMLServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}