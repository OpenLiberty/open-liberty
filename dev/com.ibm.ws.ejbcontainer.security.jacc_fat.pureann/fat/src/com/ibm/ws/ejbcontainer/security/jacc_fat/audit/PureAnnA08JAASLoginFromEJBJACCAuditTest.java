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

import static org.junit.Assert.assertTrue;

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
import com.ibm.ws.security.audit.fat.common.tooling.AuditAsserts;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;
import com.ibm.ws.security.audit.fat.common.tooling.RecentAuditFileStream;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Performs testing of a JAAS programmatic login from a statless bean with pure annotations. The bean issues a login() after
 * it is invoked by the servlet. The bean obtains the subject from the LoginContext and displays the subject so that it can be verified.
 *
 * This test invokes SecurityEJBA08Bean methods with a variety of method signatures to insure that
 * annotations are processed correctly with methods of the same name and different signature.
 *
 * Note: This test invokes SecurityEJBA08Bean which issues a JAAS login with user1 and user3
 * (which are hard-coded users in SecurityEJBA08Bean.java). Therefore, this test requires the basic user
 * registry to be configured with user1 in group1 and user3 in group3.
 **/
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class PureAnnA08JAASLoginFromEJBJACCAuditTest extends EJBAnnTestBase {

    @ClassRule
    public static RepeatTests auditRepeat = RepeatTests.with(new FeatureReplacementAction("audit-2.0", "audit-1.0").forServers(Constants.SERVER_EJB_AUDIT).fullFATOnly())
                    .andWith(new FeatureReplacementAction("audit-1.0", "audit-2.0").forServers(Constants.SERVER_EJB_AUDIT));

    protected static Class<?> logClass = PureAnnA08JAASLoginFromEJBJACCAuditTest.class;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(logClass, "setUp", "Starting the server....");
        commonSetup(logClass, Constants.SERVER_EJB_AUDIT,
                    Constants.APPLICATION_SECURITY_EJB, Constants.SERVLET_SECURITY_EJB, Constants.CONTEXT_ROOT_SECURITY_EJB);

    }

    protected TestName getName() {
        return name;
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an EJB method injected into a servlet where the PermitAll
     * <LI> annotation at method level allows user1 to invoke the permitAll method. The permitAll
     * <LI> method then performs a JAAS programmatic login with the same user (user1) results in valid subject.
     * <LI>
     * <LI> This test covers invoking the EJB method permitAll() with no parameters.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> After an EJB method is invoked with one user, a JAAS login with the same user results in
     * <LI> valid subject containing WSPrincipal with correct realm and group name.
     * <LI>
     * </OL>
     */
    @Test
    public void testPureA08JAASLogin_PermitAll_LoginWithSameUser_ValidSubjectWithWSPrincipalAndGroupForJaccAudit() throws Exception {
        Log.info(logClass, name.getMethodName(), "**Entering " + name.getMethodName());

        RecentAuditFileStream recent = new RecentAuditFileStream(server.getLogsRoot() + "audit.log");
        AuditAsserts asserts = new AuditAsserts(logClass, recent);

        String queryString = "/SimpleServlet?testInstance=ejb08&testMethod=permitAll";
        String response = generateResponseFromServlet(queryString, Constants.EMPLOYEE_USER, Constants.EMPLOYEE_PWD);
        verifyResponse(response, Constants.EMPLOYEE_USER_PRINCIPAL, Constants.EMPLOYEE_USER_IDENTITY, Constants.IS_MANAGER_FALSE, Constants.IS_EMPLOYEE_TRUE);
        assertTrue("WSPrincipal:" + Constants.EMPLOYEE_USER + " not found in response", response.contains("WSPrincipal:" + Constants.EMPLOYEE_USER));
        assertTrue("group:" + Constants.USER_REGISTRY_REALM + "/" + Constants.EMPLOYEE_GROUP + " not found in response",
                   response.contains("group:" + Constants.USER_REGISTRY_REALM + "/"));

        // Assert that all of the test patterns specified below are found in the audit data. The records must be found
        // in the order specified below. Extra records in between are ignored.
        asserts.assertFoundInData(asserts.asJson(AuditConstants.EVENT_NAME + "=" + AuditConstants.SECURITY_AUTHZ,
                                                 AuditEvent.TARGET_CREDENTIAL_TYPE + "=" + AuditEvent.CRED_TYPE_BASIC,
                                                 AuditEvent.TARGET_REALM + "=" + AuditCommonTest.REGISTRY_BASICREALM,
                                                 AuditEvent.TARGET_CREDENTIAL_TOKEN + "=user1",
                                                 AuditEvent.TARGET_NAME + "=/securityejb/SimpleServlet",
                                                 AuditEvent.REASON_TYPE + "=EJB",
                                                 AuditEvent.TARGET_JACC_CONTAINER + "=ejb",
                                                 AuditEvent.TARGET_JACC_PERMISSIONS + "=GET",
                                                 AuditEvent.TARGET_ROLE_NAMES + "=[AllAuthenticated]",
                                                 AuditConstants.OUTCOME + "=" + AuditConstants.SUCCESS));

        Log.info(logClass, name.getMethodName(), "**Exiting " + name.getMethodName());
    }

}