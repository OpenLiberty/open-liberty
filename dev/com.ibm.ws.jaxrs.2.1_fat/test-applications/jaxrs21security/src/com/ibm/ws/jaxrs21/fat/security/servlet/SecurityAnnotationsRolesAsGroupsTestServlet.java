/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.fat.security.servlet;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;

@WebServlet(urlPatterns = "/SecurityAnnotationsRolesAsGroupsTestServlet")
public class SecurityAnnotationsRolesAsGroupsTestServlet extends SecurityAnnotationsParentTestServlet {

    private static final long serialVersionUID = 4563456788769868446L;
    /**
     * tests RolesAllowed at class level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_withWebXml_roleSameAsGroup() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, false);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testClassLevelRolesAllowedUserInRole_noWebXml_roleSameAsGroup() throws Exception {
        testClassLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, false);
    }

    /**
     * tests RolesAllowed at method level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_withWebXml_roleSameAsGroup() throws Exception {
        testMethodLevelRolesAllowedUserInRole(SECANNO_BASE_TEST_URI, false);
    }

    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRole_noWebXml_roleSameAsGroup() throws Exception {
        testMethodLevelRolesAllowedUserInRole(SECANNO_NOWEBXML_BASE_TEST_URI, false);
    }

    /**
     * tests RolesAllowed at method level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRoleMultipleRequests_withWebXml_roleSameAsGroup() throws Exception {
        testMethodLevelRolesAllowedUserInRoleMultipleRequests(SECANNO_BASE_TEST_URI, false);
    }

    /**
     * tests RolesAllowed at method level -- user in role same as user's group name
     */
    @Test
    @AllowedFFDC("com.ibm.ws.security.registry.RegistryException")
    public void testMethodLevelRolesAllowedUserInRoleMultipleRequests_roleSameAsGroup() throws Exception {
        testMethodLevelRolesAllowedUserInRoleMultipleRequests(SECANNO_BASE_TEST_URI, false);
    }

}