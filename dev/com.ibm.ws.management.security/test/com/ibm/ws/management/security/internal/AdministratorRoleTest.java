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
package com.ibm.ws.management.security.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

import com.ibm.ws.management.security.ManagementSecurityConstants;

/**
 *
 */
public class AdministratorRoleTest {
    private static SharedOutputManager outputMgr;

    @Rule
    public TestName testName = new TestName();

    private AdministratorRole adminRole;

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("com.ibm.ws.management.security.*=info");
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        outputMgr.resetStreams();
        adminRole = new AdministratorRole();
    }

    @After
    public void tearDown() {
        adminRole = null;
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Check output for duplicate entry error messages.
     */
    private void checkForDuplicateRoleEntry(String entityType, String name) {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForMessages("CWWKX0002E: A duplicate entry in the Administrator role binding was detected. The duplicate " + entityType
                                              + " entry is " + name + "."));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_emptyProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getUsers().isEmpty());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getGroups().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_emptyValuesProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "", " " });
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "", " " });
        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getUsers().isEmpty());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getGroups().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_justUsers() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2" });

        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 2 users",
                     2, adminRole.getUsers().size());
        assertTrue("User bindings should contain user1",
                   adminRole.getUsers().contains("user1"));
        assertTrue("User bindings should contain user2",
                   adminRole.getUsers().contains("user2"));

        assertTrue("Group bindings should be empty with no groups in config",
                   adminRole.getGroups().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_duplicateUsers() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2", "user1", "user3", "user1", "user3" });

            adminRole.activate(props);
            assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                         adminRole.getRoleName());

            assertEquals("Expecting only 1 user as there were duplicates",
                         1, adminRole.getUsers().size());
            assertTrue("User bindings should contain user2",
                       adminRole.getUsers().contains("user2"));
            assertTrue("Group bindings should be empty with no groups in config",
                       adminRole.getGroups().isEmpty());

            checkForDuplicateRoleEntry("user", "user1");
            checkForDuplicateRoleEntry("user", "user3");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_justGroups() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2" });

        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertTrue("User bindings should be empty with no users in config",
                   adminRole.getUsers().isEmpty());

        assertEquals("Expecting 2 groups",
                     2, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group1",
                   adminRole.getGroups().contains("group1"));
        assertTrue("Group bindings should contain group2",
                   adminRole.getGroups().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_duplicateGroups() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2", "group1", "group3", "group1", "group3" });

            adminRole.activate(props);
            assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                         adminRole.getRoleName());

            assertTrue("User bindings should be empty with no users in config",
                       adminRole.getUsers().isEmpty());

            assertEquals("Expecting only 1 group as there were duplicates",
                         1, adminRole.getGroups().size());
            assertTrue("Group bindings should contain group2",
                       adminRole.getGroups().contains("group2"));

            checkForDuplicateRoleEntry("group", "group1");
            checkForDuplicateRoleEntry("group", "group3");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#activate(org.osgi.service.component.ComponentContext, java.util.Map)}.
     */
    @Test
    public void activate_fullBinding() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2" });
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2" });

        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 2 users",
                     2, adminRole.getUsers().size());
        assertTrue("User bindings should contain user1",
                   adminRole.getUsers().contains("user1"));
        assertTrue("User bindings should contain user2",
                   adminRole.getUsers().contains("user2"));

        assertEquals("Expecting 2 groups",
                     2, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group1",
                   adminRole.getGroups().contains("group1"));
        assertTrue("Group bindings should contain group2",
                   adminRole.getGroups().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_emptyProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        adminRole.modify(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getUsers().isEmpty());
        assertTrue("Bindings should be empty after activation with no properties",
                   adminRole.getGroups().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_justUsers() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2" });

        adminRole.modify(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 2 users",
                     2, adminRole.getUsers().size());
        assertTrue("User bindings should contain user1",
                   adminRole.getUsers().contains("user1"));
        assertTrue("User bindings should contain user2",
                   adminRole.getUsers().contains("user2"));

        assertTrue("Group bindings should be empty with no groups in config",
                   adminRole.getGroups().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_duplicateUsers() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2", "user1", "user3", "user1", "user3" });

            adminRole.modify(props);
            assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                         adminRole.getRoleName());

            assertEquals("Expecting only 1 user as there were duplicates",
                         1, adminRole.getUsers().size());
            assertTrue("User bindings should contain user2",
                       adminRole.getUsers().contains("user2"));

            assertTrue("Group bindings should be empty with no groups in config",
                       adminRole.getGroups().isEmpty());

            checkForDuplicateRoleEntry("user", "user1");
            checkForDuplicateRoleEntry("user", "user3");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_justGroups() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2" });

        adminRole.modify(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertTrue("User bindings should be empty with no users in config",
                   adminRole.getUsers().isEmpty());

        assertEquals("Expecting 2 groups",
                     2, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group1",
                   adminRole.getGroups().contains("group1"));
        assertTrue("Group bindings should contain group2",
                   adminRole.getGroups().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_duplicateGroups() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2", "group1", "group3", "group1", "group3" });

            adminRole.modify(props);
            assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                         adminRole.getRoleName());

            assertTrue("User bindings should be empty with no users in config",
                       adminRole.getUsers().isEmpty());

            assertEquals("Expecting only 1 group as there were duplicates",
                         1, adminRole.getGroups().size());
            assertTrue("Group bindings should contain group2",
                       adminRole.getGroups().contains("group2"));

            checkForDuplicateRoleEntry("group", "group1");
            checkForDuplicateRoleEntry("group", "group3");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_fullBindings() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1", "user2" });
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1", "group2" });

        adminRole.modify(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 2 users",
                     2, adminRole.getUsers().size());
        assertTrue("User bindings should contain user1",
                   adminRole.getUsers().contains("user1"));
        assertTrue("User bindings should contain user2",
                   adminRole.getUsers().contains("user2"));

        assertEquals("Expecting 2 groups",
                     2, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group1",
                   adminRole.getGroups().contains("group1"));
        assertTrue("Group bindings should contain group2",
                   adminRole.getGroups().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#modify(java.util.Map)}.
     */
    @Test
    public void modify_afterActivate() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user1" });
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group1" });

        adminRole.activate(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 1 user",
                     1, adminRole.getUsers().size());
        assertTrue("User bindings should contain user1",
                   adminRole.getUsers().contains("user1"));

        assertEquals("Expecting 1 group",
                     1, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group1",
                   adminRole.getGroups().contains("group1"));

        props = new HashMap<String, Object>();
        props.put(AdministratorRole.CFG_KEY_USER, new String[] { "user2" });
        props.put(AdministratorRole.CFG_KEY_GROUP, new String[] { "group2" });

        adminRole.modify(props);
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());

        assertEquals("Expecting 2 user",
                     1, adminRole.getUsers().size());
        assertTrue("User bindings should contain user2",
                   adminRole.getUsers().contains("user2"));

        assertEquals("Expecting 1 group",
                     1, adminRole.getGroups().size());
        assertTrue("Group bindings should contain group2",
                   adminRole.getGroups().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.AdministratorRole#deactivate(java.util.Map)}.
     */
    @Test
    public void deactivate() {
        adminRole.deactivate();
        assertEquals(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME,
                     adminRole.getRoleName());
        assertTrue("Bindings should be empty after deactivation",
                   adminRole.getUsers().isEmpty());
        assertTrue("Bindings should be empty after deactivation",
                   adminRole.getGroups().isEmpty());
    }

}
