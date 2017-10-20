/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.feature.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.FeatureAuthorizationTableService;
import com.ibm.ws.security.authorization.RoleSet;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

import test.common.SharedOutputManager;

/**
 * This class tests the authorization table for a feature web bundle.
 */
public class FeatureAuthorizationTableTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final static String KEY_SECURITY_SERVICE = "securityService";
    private final static String KEY_CONFIG_ADMIN = "configurationAdmin";
    private final static String KEY_FEATURE_SECURITY_AUTHZ_SERVICE = "featureCollabAuthzTable";

    private final static String KEY_LDAP_REGISTRY = "(service.factoryPid=com.ibm.ws.security.registry.ldap.config)";
    private final static String KEY_IGNORE_CASE = "ignoreCase";
    private final static String CFG_KEY_NAME = "name";
    private final static String CFG_KEY_USER = "user";
    private final static String CFG_KEY_GROUP = "group";
    private final static String KEY_ROLE1 = "role1";
    private final static String KEY_ROLE2 = "role2";
    private final static String KEY_PID1 = "pid1";
    private final static String KEY_PID2 = "pid2";
    private final static String KEY_USER_PID1 = "userPid1";
    private final static String KEY_USER_PID2 = "userPid2";
    private final static String KEY_GROUP_PID1 = "groupPid1";
    private final static String KEY_GROUP_PID2 = "groupPid2";
    private final String[] roleDef = { KEY_PID1, KEY_PID2 };

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private FeatureAuthorizationTableTestDouble featureAuthzTable = null;
    private final Map<String, Object> configProps = new HashMap<String, Object>();
    private final String AUTHZ_ROLES_ID = "user.test.feature.roles";
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "secServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class);
    private final ServiceReference<ConfigurationAdmin> car = mock.mock(ServiceReference.class, "ConfigurationAdmin");
    private final ConfigurationAdmin ca = mock.mock(ConfigurationAdmin.class);
    private final ServiceReference<FeatureAuthorizationTableService> fatsr = mock.mock(ServiceReference.class, "FeatureAuthorizationTableService");
    private final FeatureAuthorizationTableService fats = mock.mock(FeatureAuthorizationTableService.class);
    private final UserRegistryService userRegistryService = mock.mock(UserRegistryService.class);
    private final UserRegistry userRegistry = mock.mock(UserRegistry.class);
    private final Configuration lrc = mock.mock(Configuration.class);
    private final Configuration lrcs[] = { lrc };
    private final Configuration cfg1 = mock.mock(Configuration.class, "cfg1");
    private final Configuration cfg2 = mock.mock(Configuration.class, "cfg2");
    private final Configuration userConfig1 = mock.mock(Configuration.class, "userConfig1");
    private final Configuration userConfig2 = mock.mock(Configuration.class, "userConfig2");
    private final Configuration groupConfig1 = mock.mock(Configuration.class, "groupConfig1");
    private final Configuration groupConfig2 = mock.mock(Configuration.class, "groupConfig2");
    private final BundleContext bc = mock.mock(BundleContext.class);

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr.trace("com.ibm.ws.security.*=all");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.trace("*=all=disabled");
    }

    class FeatureAuthorizationTableTestDouble extends FeatureAuthorizationTable {
        public FeatureAuthorizationTableTestDouble() {
            super();
        }

        @Override
        protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
            super.activate(cc);
            processConfigProps(props);
        }

        private void processConfigProps(Map<String, Object> props) {
            if (props == null || props.isEmpty())
                return;
            id = (String) props.get(CFG_KEY_ID);
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

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                will(returnValue(bc));
                allowing(bc).getBundle();
                allowing(cc).locateService(KEY_FEATURE_SECURITY_AUTHZ_SERVICE, fatsr);
                will(returnValue(fats));
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
//                allowing(fats).addAuthorizationTable(with(any(String.class)), with(any(OAuth20WebAppAuthorizationTable.class)));
            }
        });

        configProps.put(FeatureAuthorizationTable.CFG_KEY_ID, AUTHZ_ROLES_ID);
        configProps.put(FeatureAuthorizationTable.CFG_KEY_ROLE, null);

        featureAuthzTable = new FeatureAuthorizationTableTestDouble();
        featureAuthzTable.setFeatureCollabAuthzTable(fatsr);
        featureAuthzTable.setSecurityService(securityServiceRef);
        featureAuthzTable.setConfigurationAdmin(car);
        featureAuthzTable.activate(cc, configProps);
    }

    @After
    public void tearDown() {
//        featureAuthzTable.deactivate(cc);
        featureAuthzTable.unsetFeatureCollabAuthzTable(fatsr);
        featureAuthzTable.unsetSecurityService(securityServiceRef);
        featureAuthzTable.unsetConfigurationAdmin(car);
        featureAuthzTable = null;

        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.feature.FeatureAuthorizationTable#getFeatureAuthorizationId()}.
     *
     */
    @Test
    public void getFeatureAuthorizationId() {
        assertEquals("Incorrect feature authorization ID returned",
                     AUTHZ_ROLES_ID, featureAuthzTable.getFeatureAuthorizationId());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.feature.FeatureAuthorizationTable#getApplicationName()()}.
     *
     */
    @Test
    public void getApplicationName() {
        assertEquals("Incorrect application name returned",
                     AUTHZ_ROLES_ID, featureAuthzTable.getApplicationName());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_user_ignoreCase_True() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        final Dictionary<String, Object> roleProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> roleProps2 = new Hashtable<String, Object>();

        roleProps1.put(CFG_KEY_NAME, KEY_ROLE1);
        roleProps2.put(CFG_KEY_NAME, KEY_ROLE2);
        String[] userPids1 = { KEY_USER_PID1 };
        String[] userPids2 = { KEY_USER_PID2 };

        roleProps1.put(CFG_KEY_USER, userPids1);
        roleProps2.put(CFG_KEY_USER, userPids2);

        final Dictionary<String, Object> userProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> userProps2 = new Hashtable<String, Object>();
        userProps1.put("name", "user1");
        userProps2.put("name", "user2");
        userProps2.put("access-id", "user:mockRealm/user2");

        mock.checking(new Expectations() {
            {
                one(ca).getConfiguration(KEY_PID1, "");
                will(returnValue(cfg1));
                one(ca).getConfiguration(KEY_PID2, "");
                will(returnValue(cfg2));

                allowing(cfg1).getProperties();
                will(returnValue(roleProps1));
                allowing(cfg2).getProperties();
                will(returnValue(roleProps2));

                one(ca).getConfiguration(KEY_USER_PID1, "");
                will(returnValue(userConfig1));
                one(ca).getConfiguration(KEY_USER_PID2, "");
                will(returnValue(userConfig2));

                allowing(userConfig1).getProperties();
                will(returnValue(userProps1));
                allowing(userConfig2).getProperties();
                will(returnValue(userProps2));

                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(returnValue("USER1"));
                one(userRegistry).getUniqueUserId("user2");
                will(returnValue("USER2"));

            }
        });

        featureAuthzTable.setFeatureRoleConfiguration(roleDef, ca);

        // verify with name.
        RoleSet roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                              "user:mockRealm/UsEr1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role1", roles.contains(KEY_ROLE1));

        // verify with access-id
        roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                      "user:mockRealm/uSeR2");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role2", roles.contains(KEY_ROLE2));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_user_ignoreCase_False() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        final Dictionary<String, Object> roleProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> roleProps2 = new Hashtable<String, Object>();

        roleProps1.put(CFG_KEY_NAME, KEY_ROLE1);
        roleProps2.put(CFG_KEY_NAME, KEY_ROLE2);
        String[] userPids1 = { KEY_USER_PID1 };
        String[] userPids2 = { KEY_USER_PID2 };

        roleProps1.put(CFG_KEY_USER, userPids1);
        roleProps2.put(CFG_KEY_USER, userPids2);

        final Dictionary<String, Object> userProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> userProps2 = new Hashtable<String, Object>();
        userProps1.put("name", "user1");
        userProps2.put("name", "user2");
        userProps2.put("access-id", "user:mockRealm/user2");

        mock.checking(new Expectations() {
            {
                one(ca).getConfiguration(KEY_PID1, "");
                will(returnValue(cfg1));
                one(ca).getConfiguration(KEY_PID2, "");
                will(returnValue(cfg2));

                allowing(cfg1).getProperties();
                will(returnValue(roleProps1));
                allowing(cfg2).getProperties();
                will(returnValue(roleProps2));

                one(ca).getConfiguration(KEY_USER_PID1, "");
                will(returnValue(userConfig1));
                one(ca).getConfiguration(KEY_USER_PID2, "");
                will(returnValue(userConfig2));

                allowing(userConfig1).getProperties();
                will(returnValue(userProps1));
                allowing(userConfig2).getProperties();
                will(returnValue(userProps2));

                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueUserId("user1");
                will(returnValue("USER1"));
                one(userRegistry).getUniqueUserId("user2");
                will(returnValue("USER2"));

            }
        });

        featureAuthzTable.setFeatureRoleConfiguration(roleDef, ca);

        // verify that the identical access ID returns the valid role.
        RoleSet roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                              "user:mockRealm/user2");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role2", roles.contains(KEY_ROLE2));

        // verify that the identical name returns the valid role.
        roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                      "user:mockRealm/USER1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role1", roles.contains(KEY_ROLE1));

        // verify that case mismatch of user to accessId mapping doesn't return the role.
        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                           "user:mockRealm/UsEr1"));

        // verify that case mismatch of accessId mapping doesn't return the role.
        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                           "user:mockRealm/UsEr2"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_group_ignoreCase_True() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "true");

        final Dictionary<String, Object> roleProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> roleProps2 = new Hashtable<String, Object>();

        roleProps1.put(CFG_KEY_NAME, KEY_ROLE1);
        roleProps2.put(CFG_KEY_NAME, KEY_ROLE2);
        String[] groupPids1 = { KEY_GROUP_PID1 };
        String[] groupPids2 = { KEY_GROUP_PID2 };

        roleProps1.put(CFG_KEY_GROUP, groupPids1);
        roleProps2.put(CFG_KEY_GROUP, groupPids2);

        final Dictionary<String, Object> groupProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> groupProps2 = new Hashtable<String, Object>();
        groupProps1.put("name", "group1");
        groupProps2.put("name", "group2");
        groupProps2.put("access-id", "group:mockRealm/group2");

        mock.checking(new Expectations() {
            {
                one(ca).getConfiguration(KEY_PID1, "");
                will(returnValue(cfg1));
                one(ca).getConfiguration(KEY_PID2, "");
                will(returnValue(cfg2));

                allowing(cfg1).getProperties();
                will(returnValue(roleProps1));
                allowing(cfg2).getProperties();
                will(returnValue(roleProps2));

                one(ca).getConfiguration(KEY_GROUP_PID1, "");
                will(returnValue(groupConfig1));
                one(ca).getConfiguration(KEY_GROUP_PID2, "");
                will(returnValue(groupConfig2));

                allowing(groupConfig1).getProperties();
                will(returnValue(groupProps1));
                allowing(groupConfig2).getProperties();
                will(returnValue(groupProps2));

                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(returnValue("GROUP1"));
                one(userRegistry).getUniqueGroupId("group2");
                will(returnValue("GROUP2"));

            }
        });

        featureAuthzTable.setFeatureRoleConfiguration(roleDef, ca);

        // verify with name.
        RoleSet roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                              "group:mockRealm/GrOuP1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role1", roles.contains(KEY_ROLE1));

        // verify with access-id
        roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                      "group:mockRealm/gRoUp2");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role2", roles.contains(KEY_ROLE2));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.ManagementSecurityAuthorizationTable#getRolesForAccessId(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getRolesForAccessId_group_ignoreCase_False() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_IGNORE_CASE, "false");

        final Dictionary<String, Object> roleProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> roleProps2 = new Hashtable<String, Object>();

        roleProps1.put(CFG_KEY_NAME, KEY_ROLE1);
        roleProps2.put(CFG_KEY_NAME, KEY_ROLE2);
        String[] groupPids1 = { KEY_GROUP_PID1 };
        String[] groupPids2 = { KEY_GROUP_PID2 };

        roleProps1.put(CFG_KEY_GROUP, groupPids1);
        roleProps2.put(CFG_KEY_GROUP, groupPids2);

        final Dictionary<String, Object> groupProps1 = new Hashtable<String, Object>();
        final Dictionary<String, Object> groupProps2 = new Hashtable<String, Object>();
        groupProps1.put("name", "group1");
        groupProps2.put("name", "group2");
        groupProps2.put("access-id", "group:mockRealm/group2");

        mock.checking(new Expectations() {
            {
                one(ca).getConfiguration(KEY_PID1, "");
                will(returnValue(cfg1));
                one(ca).getConfiguration(KEY_PID2, "");
                will(returnValue(cfg2));

                allowing(cfg1).getProperties();
                will(returnValue(roleProps1));
                allowing(cfg2).getProperties();
                will(returnValue(roleProps2));

                one(ca).getConfiguration(KEY_GROUP_PID1, "");
                will(returnValue(groupConfig1));
                one(ca).getConfiguration(KEY_GROUP_PID2, "");
                will(returnValue(groupConfig2));

                allowing(groupConfig1).getProperties();
                will(returnValue(groupProps1));
                allowing(groupConfig2).getProperties();
                will(returnValue(groupProps2));

                one(userRegistryService).getUserRegistryType();
                will(returnValue("WIM"));
                one(ca).listConfigurations(KEY_LDAP_REGISTRY);
                will(returnValue(lrcs));
                one(lrc).getProperties();
                will(returnValue(props));
                allowing(userRegistry).getRealm();
                will(returnValue("mockRealm"));
                one(userRegistry).getUniqueGroupId("group1");
                will(returnValue("GROUP1"));
                one(userRegistry).getUniqueGroupId("group2");
                will(returnValue("GROUP2"));

            }
        });

        featureAuthzTable.setFeatureRoleConfiguration(roleDef, ca);

        // verify that the identical access ID returns the valid role.
        RoleSet roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                              "group:mockRealm/group2");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role2", roles.contains(KEY_ROLE2));

        // verify that the identical name returns the valid role.
        roles = featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                      "group:mockRealm/GROUP1");
        assertNotNull("Roles must not be null", roles);
        assertEquals("Must only have one role", 1, roles.size());
        assertTrue("Must have role1", roles.contains(KEY_ROLE1));

        // verify that case mismatch of user to accessId mapping doesn't return the role.
        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                           "group:mockRealm/GrOuP1"));

        // verify that case mismatch of accessId mapping doesn't return the role.
        assertEquals("role should be empty since ignoreCase is set as false",
                     RoleSet.EMPTY_ROLESET,
                     featureAuthzTable.getRolesForAccessId(AUTHZ_ROLES_ID,
                                                           "group:mockRealm/gRoUp2"));
    }

}
