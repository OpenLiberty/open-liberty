/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.management.security.ManagementRole;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class ManagementSecurityAuthorizationTableTest {
    private static SharedOutputManager outputMgr;

    private final static String KEY_SECURITY_SERVICE = "securityService";
    private final static String KEY_CONFIG_ADMIN = "configurationAdmin";
    private final static String KEY_LDAP_REGISTRY = "(service.factoryPid=com.ibm.ws.security.registry.ldap.config)";
    private final static String KEY_IGNORE_CASE = "ignoreCase";

    static class ManagementRoleDouble implements ManagementRole {
        private final String roleName;
        private Set<String> users = new HashSet<String>();
        private final Set<String> userAccessIds = new HashSet<String>();
        private Set<String> groups = new HashSet<String>();
        private final Set<String> groupAccessIds = new HashSet<String>();

        ManagementRoleDouble(String roleName) {
            this.roleName = roleName;
        }

        /** {@inheritDoc} */
        @Override
        public String getRoleName() {
            return roleName;
        }

        /** {@inheritDoc} */
        @Override
        public Set<String> getUsers() {
            return users;
        }

        public void setUsers(Set<String> users) {
            this.users = users;
        }

        /** {@inheritDoc} */
        @Override
        public Set<String> getGroups() {
            return groups;
        }

        public void setGroups(Set<String> groups) {
            this.groups = groups;
        }

    }

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "secServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final ServiceReference<ConfigurationAdmin> car = mock.mock(ServiceReference.class, "ConfigurationAdmin");
    private final ConfigurationAdmin ca = mock.mock(ConfigurationAdmin.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final ServiceReference<ManagementRole> role1Ref = mock.mock(ServiceReference.class, "role1");
    private final ManagementRoleDouble role1 = new ManagementRoleDouble("Role1");
    private final ServiceReference<ManagementRole> role2Ref = mock.mock(ServiceReference.class, "role2");
    private final ManagementRoleDouble role2 = new ManagementRoleDouble("Role2");
    private final Configuration lrc = mock.mock(Configuration.class);
    private ManagementSecurityAuthorizationTableDouble table;
    private final Configuration lrcs[] = { lrc };

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        outputMgr.trace("com.ibm.ws.management.security.*=all:com.ibm.ws.security.*=all");
    }

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(ManagementSecurityAuthorizationTable.KEY_MANAGEMENT_ROLE, role1Ref);
                will(returnValue(role1));
                allowing(role1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(123L));
                allowing(role1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService(ManagementSecurityAuthorizationTable.KEY_MANAGEMENT_ROLE, role2Ref);
                will(returnValue(role2));
                allowing(role2Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(456L));
                allowing(role2Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService(KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));
                allowing(cc).locateService(KEY_CONFIG_ADMIN, car);
                will(returnValue(ca));
                allowing(securityService).getUserRegistryService();
                will(returnValue(userRegistryService));
                allowing(userRegistryService).getUserRegistry();
                will(returnValue(userRegistry));
                allowing(userRegistryService).isUserRegistryConfigured();
                will(returnValue(true));
            }
        });

        table = new ManagementSecurityAuthorizationTableDouble();
        table.setSecurityService(securityServiceRef);
        table.setConfigurationAdmin(car);
        table.activate(cc);
    }

    @After
    public void tearDown() {
        table.deactivate(cc);
        table.unsetSecurityService(securityServiceRef);
        table.unsetConfigurationAdmin(car);
        table = null;

        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#setManagementRole(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setManagementRole_noRoles() {
        table.setManagementRole(role1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#setManagementRole(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setManagementRole_oneRole() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#setManagementRole(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void setManagementRole_twoRole() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#updateManagementRole(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void updateManagementRole() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);

        table.updatedManagementRole(role1Ref);
        table.updatedManagementRole(role2Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#unsetManagementRole(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void unsetManagementRole() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);

        table.unsetManagementRole(role2Ref);
        table.unsetManagementRole(role1Ref);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activate() throws Exception {
        table = new ManagementSecurityAuthorizationTableDouble();

        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);

        table.setSecurityService(securityServiceRef);
        table.activate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#deactivate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void deactivate() throws Exception {
        ManagementSecurityAuthorizationTableDouble localTable = new ManagementSecurityAuthorizationTableDouble();

        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        localTable.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        localTable.setManagementRole(role2Ref);

        localTable.setSecurityService(securityServiceRef);
        localTable.activate(cc);

        localTable.deactivate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForSpecialSubject(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForSpecialSubject_unknownResource() throws Exception {
        assertNull("The resource is not known and null should be returned",
                   table.getRolesForSpecialSubject("unknown", AuthorizationTableService.EVERYONE));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForSpecialSubject(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForSpecialSubject_adminResource() throws Exception {
        assertEquals("No special subjects are supported",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForSpecialSubject(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, AuthorizationTableService.EVERYONE));
        assertTrue("Role allAuthenticatedUsers not found for special subject ALL_AUTHENTICATED_USERS",
                   table.getRolesForSpecialSubject(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                   AuthorizationTableService.ALL_AUTHENTICATED_USERS).contains(ManagementSecurityConstants.ALL_AUTHENTICATED_USERS_ROLE_NAME));
        assertEquals("No special subjects are supported",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForSpecialSubject(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, AuthorizationTableService.ALL_AUTHENTICATED_IN_TRUSTED_REALMS));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_unknownResource() throws Exception {
        assertNull("The resource is not known and null should be returned",
                   table.getRolesForAccessId("unknown", "user:realm/id"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_unknownAccessId() throws Exception {
        assertEquals("An unknown access ID should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, "user:realm/id"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getRolesForAccessId_invalidAccessId() throws Exception {
        assertNull("The resource is not known and null should be returned",
                   table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, "user:/id"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_unknownTypeAccessId() throws Exception {
        assertEquals("A non-user, non-group access ID should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, "unknown:realm/id"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_serverAccessId() throws Exception {
        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME, "server:realm/id");
        assertEquals("A server access ID should have the Administrator role",
                     1, roles.size());
        assertTrue("A server access ID should have the Administrator role",
                   roles.contains(ManagementSecurityConstants.ADMINISTRATOR_ROLE_NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_singleBinding() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");
        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(returnValue("user1"));
            }
        });

        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                  "user:mockRealm/user1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));

        // 2nd retrievable should be from the cache
        roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                          "user:mockRealm/user1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_userNotInBinding() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");
        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(returnValue("user1"));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "user:mockRealm/user2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_userEntryNotFound() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");
        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(throwException(new EntryNotFoundException("Expected test exception")));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "user:mockRealm/user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_userRegistryException() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");
        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(throwException(new RegistryException("Expected test exception")));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "user:mockRealm/user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_groupNotInBinding() throws Exception {
        Set<String> groups = new HashSet<String>();
        groups.add("group1");
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(returnValue("group1"));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "group:mockRealm/group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_groupEntryNotFound() throws Exception {
        Set<String> groups = new HashSet<String>();
        groups.add("group1");
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(throwException(new EntryNotFoundException("Expected test exception")));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "group:mockRealm/group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_groupRegistryException() throws Exception {
        Set<String> groups = new HashSet<String>();
        groups.add("group1");
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(throwException(new RegistryException("Expected test exception")));
            }
        });

        assertEquals("An access ID that can not be computed should be empty",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "group:mockRealm/group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_multipleUserBindings() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(returnValue("user1"));
            }
        });

        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                  "user:mockRealm/user1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have two roles", 2, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));
        assertTrue("Must have Role2", roles.contains("Role2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_multipleGroupBindings() throws Exception {
        Set<String> users = new HashSet<String>();
        users.add("user1");

        Set<String> groups = new HashSet<String>();
        groups.add("group1");

        role1.setUsers(users);
        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        role2.setUsers(users);
        role2.setGroups(groups);
        table.setManagementRole(role2Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("Basic"));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(returnValue("group1"));
            }
        });

        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                  "group:mockRealm/group1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have two roles", 2, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));
        assertTrue("Must have Role2", roles.contains("Role2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_group_ignoreCase_True() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        Set<String> groups = new HashSet<String>();
        groups.add("group3");

        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group3");
                will(returnValue("GROUP3"));
            }
        });

        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                  "group:mockRealm/group3");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_group_ignoreCase_False() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        Set<String> groups = new HashSet<String>();
        groups.add("group4");

        role1.setGroups(groups);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group4");
                will(returnValue("GROUP4"));
            }
        });

        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "group:mockRealm/group4"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_user_ignoreCase_True() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        Set<String> users = new HashSet<String>();
        users.add("user3");

        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user3");
                will(returnValue("USER3"));
            }
        });

        RoleSet roles = table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                  "user:mockRealm/user3");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have Role1", roles.contains("Role1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_user_ignoreCase_False() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        Set<String> users = new HashSet<String>();
        users.add("user4");

        role1.setUsers(users);
        table.setManagementRole(role1Ref);

        mock.checking(new Expectations() {
            {
                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                one(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user4");
                will(returnValue("USER4"));
            }
        });

        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     table.getRolesForAccessId(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                               "user:mockRealm/user4"));
    }

    class ManagementSecurityAuthorizationTableDouble extends ManagementSecurityAuthorizationTable {
        public ManagementSecurityAuthorizationTableDouble() {
            super();
        }

        @Override
        protected void setSecurityService(ServiceReference<SecurityService> reference) {
            super.setSecurityService(reference);
        }

        @Override
        protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
            super.unsetSecurityService(reference);
        }

        @Override
        protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
            super.setConfigurationAdmin(reference);
        }

        @Override
        protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> reference) {
            super.unsetConfigurationAdmin(reference);
        }
    }
}
