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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

/**
 * This test covers nested LDAP groups and roles based authorization decisions based on the nested groups.
 * This test assumes that the nested groups have already been defined in the LDAP registry as follows:
 *
 * Top level group: nested_g1 (Mapped to Employee role)
 * User in top group: topng_user1
 *
 * SubGroup: embedded_group1 (Mapped to Manager role)
 * User: ng_user1
 * User: ng_user3
 *
 * SubGroup: embedded_group2 (not mapped to a role)
 * User: ng_user2
 * User: ng_user4
 *
 * The server.xml file maps nested_g1 to Employee and subgroup, embedded_group1, to Manager role.
 * The subgroup embedded_group1 should have both Employee and Manager role as it is part of top group.
 * The subgroup embedded_group2 does not have a role mapping and should inherit Employee Role from top group.
 *
 */
@RunWith(FATRunner.class)
public class LDAPNestedGroupsBase {
    // Keys to help readability of the test
    private final boolean IS_MANAGER_ROLE = true;
    private final boolean NOT_MANAGER_ROLE = false;
    private final boolean IS_EMPLOYEE_ROLE = true;
    private final boolean NOT_EMPLOYEE_ROLE = false;

    private final String employeeTopGroupUser = "topng_user1";
    private final String employeeTopGroupPassword = "testuserpwd";
    private final String group1EmbeddedUser = "ng_user1";
    private final String group1EmbeddedPassword = "security";
    private final String group2EmbeddedUser = "ng_user2";
    private final String group2EmbeddedPassword = "security";

    protected LibertyServer myServer;
    protected Class<?> logClass;
    protected BasicAuthClient myClient;

    public LDAPNestedGroupsBase(LibertyServer server, Class<?> clazz, BasicAuthClient client) {
        myServer = server;
        logClass = clazz;
        myClient = client;
    }

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(logClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(logClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in top level group where group has Employee role to
     * <LI> verify that the user has Employee access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access allowed to user in top level group which is assigned Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testUserInTopGroupEmployeeRole_AccessPermittedEmployeeRoleServlet() throws Exception {
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER) {
            String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, employeeTopGroupUser, employeeTopGroupPassword);
            assertTrue("Verification of programmatic APIs failed",
                       myClient.verifyResponse(response, employeeTopGroupUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        }
    }

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in top level group where group has Employee role to
     * <LI> verify that the user does not have Manager access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access denied to user in top level group which is assigned Employee role when Manager role required.
     * <LI>
     * </OL>
     */
    @Test
    public void testUserInTopGroupEmployeeRole_AccessDeniedManagerRoleServlet() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MANAGER_ROLE, employeeTopGroupUser, employeeTopGroupPassword));
    }

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in subgroup group with Manager role to
     * <LI> verify that the user can access servlet because top group is mapped to Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access allowed to user in top level group which is assigned Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testUserInEmbeddedGroup1ManagerAndEmployeeRole_AccessPermittedEmployeeRoleServlet() throws Exception {
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER) {
            String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, group1EmbeddedUser, group1EmbeddedPassword);
            assertTrue("Verification of programmatic APIs failed",
                       myClient.verifyResponse(response, group1EmbeddedUser, IS_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        }
    }

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in subgroup group with Manager role to
     * <LI> verify that the user can access servlet. User is also in top group is mapped to Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access allowed to user in embedded group with Manager role.
     * <LI>
     * </OL>
     */
    @Test
    public void testUserInEmbeddedGroup1ManagerAndEmployeeRole_AccessPermittedManagerRoleServlet() throws Exception {
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER) {
            String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_MANAGER_ROLE, group1EmbeddedUser, group1EmbeddedPassword);
            assertTrue("Verification of programmatic APIs failed",
                       myClient.verifyResponse(response, group1EmbeddedUser, IS_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
        }
    }

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in subgroup group with Manager role to
     * <LI> verify that the user can access servlet because top group is mapped to Employee role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access allowed to user in top level group which is assigned Employee role.
     * <LI>
     * </OL>
     */
    @Test
    public void testUserInEmbeddedGroup2InheritsEmployeeRoleFromTopGroup_AccessPermittedEmployeeRoleServlet() throws Exception {
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER) {
            String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, group2EmbeddedUser, group2EmbeddedPassword);
            assertTrue("Verification of programmatic APIs failed",
                       myClient.verifyResponse(response, group2EmbeddedUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
        }
    }

    /**
     * <P> Verify the following:
     * <OL>
     * <LI> Access the protected servlet using valid LDAP user in embedded group where group has no role and
     * <LI> inherits Employee role from top group.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Access denied to user in embedded group which inherits Employee role from top group but servlet
     * <LI> requires Manager role.
     * </OL>
     */
    @Test
    public void testUserInEmbeddedGroup2EmployeeRole_AccessDeniedManagerRoleServlet() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_MANAGER_ROLE, group2EmbeddedUser, group2EmbeddedPassword));
    }

}
