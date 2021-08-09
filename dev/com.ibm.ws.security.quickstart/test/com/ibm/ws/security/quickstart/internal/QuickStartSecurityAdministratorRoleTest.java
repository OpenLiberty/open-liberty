/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.quickstart.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 *
 */
public class QuickStartSecurityAdministratorRoleTest {
    private static final String USER = "bob";
    private ManagementRole quickStartAdminRole;

    @Before
    public void setUp() {
        quickStartAdminRole = new QuickStartSecurityAdministratorRole(USER);
    }

    /**
     * Test method for {@link com.ibm.ws.security.quickstart.internal.QuickStartSecurityAdministratorRole#getRoleName()}.
     */
    @Test
    public void getRoleName() {
        assertEquals("Must be the Administrator role name",
                     ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     quickStartAdminRole.getRoleName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.quickstart.internal.QuickStartSecurityAdministratorRole#getUsers()}.
     */
    @Test
    public void testGetUsers() {
        Set<String> users = quickStartAdminRole.getUsers();
        assertEquals("Only one user should ever be mapped",
                     1, users.size());
        assertTrue("", users.contains(USER));
    }

    /**
     * Test method for {@link com.ibm.ws.security.quickstart.internal.QuickStartSecurityAdministratorRole#getGroups()}.
     */
    @Test
    public void getGroups() {
        assertTrue("No groups should ever be mapped",
                   quickStartAdminRole.getGroups().isEmpty());
    }

}
