/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * @see UserRegistryFactoryTemplate
 */
@SuppressWarnings("unchecked")
public class BasicRegistryFactoryTest extends BasicRegistryConfigAdminMock {
//    private BasicRegistry factory;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

//    @Override
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//
//        factory = new BasicRegistryFactory();
//        factory.setConfigurationAdmin(configAdminRef);
//        factory.activate(cc);
//    }
//
//    @After
//    public void tearDown() {
//        mock.assertIsSatisfied();
//
//        factory.deactivate(cc);
//        factory.unsetConfigurationAdmin(configAdminRef);
//        factory = null;
//    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Ensure that the warning to indicate there are no users defined
     * in the registry gets logged.
     *
     * @param id
     */
    private void checkNoUsersWarningWasLogged(String id) {
        assertTrue("Expected message was not logged",
                   outputMgr.checkForMessages("CWWKS3103W: There are no users defined for the BasicRegistry configuration of ID " + id));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
//    @Test
//    public void getUserRegistryMap_emptyConfiguration() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic1");
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//
//        checkNoUsersWarningWasLogged("basic1");
//    }

    /**
     * Ensure that the warning to indicate there are no users defined
     * in the registry gets logged.
     *
     * @param message
     */
    private void checkInvalidUserDefinitionErrorWasLogged(String message) {
        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardErr("CWWKS3100E: The user definition is not valid: " + message));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An IOException on loading user configuration data should result in a RegistryException.
     */
//    @Test
//    public void getUserRegistryMap_unableToAccessUserConfiguration() throws Exception {
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("nonAccessableInstance");
//                will(throwException(new IOException("Not able to access this config data")));
//
//                allowing(configAdmin).getConfiguration("nullPropertiesInstance");
//                will(returnValue(null));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic1");
//        String[] users = { "nonAccessableInstance", "nullPropertiesInstance" };
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//
//        checkInvalidUserDefinitionErrorWasLogged("nonAccessableInstance");
//        checkInvalidUserDefinitionErrorWasLogged("nullPropertiesInstance");
//        checkNoUsersWarningWasLogged("basic1");
//    }

    /**
     * Ensure that the warning to indicate there are no users defined
     * in the registry gets logged.
     *
     * @param id
     */
    private void checkInvalidGroupDefinitionErrorWasLogged(String id) {
        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardErr("CWWKS3101E: The group definition is not valid: " + id));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An IOException on loading group configuration data should result in a RegistryException.
     */
//    @Test
//    public void getUserRegistryMap_unableToAccessGroupConfiguration() throws Exception {
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("nonAccessableInstance");
//                will(throwException(new IOException("Not able to access this config data")));
//
//                allowing(configAdmin).getConfiguration("nullPropertiesInstance");
//                will(returnValue(null));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] groups = { "nonAccessableInstance", "nullPropertiesInstance" };
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//
//        checkInvalidGroupDefinitionErrorWasLogged("nonAccessableInstance");
//        checkInvalidGroupDefinitionErrorWasLogged("nullPropertiesInstance");
//    }

    /**
     * Ensure that the warning to indicate there are no users defined
     * in the registry gets logged.
     *
     * @param id
     */
    private void checkInvalidMemberDefinitionErrorWasLogged(String id) {
        assertTrue("Expected message was not logged",
                   outputMgr.checkForStandardErr("CWWKS3102E: The member definition is not valid: " + id));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An IOException on loading group configuration data should result in a RegistryException.
     */
//    @Test
//    public void getUserRegistryMap_unableToAccessMemberConfiguration() throws Exception {
//        final Configuration groupWithNonAccessableMembersConfig = mock.mock(Configuration.class, "groupWithNonAccessableMembers");
//        final Dictionary groupWithNonAccessableMembersConfigProps = new Hashtable();
//        groupWithNonAccessableMembersConfigProps.put(DynamicBasicRegistry.CFG_KEY_NAME, "group1");
//        groupWithNonAccessableMembersConfigProps.put(DynamicBasicRegistry.CFG_KEY_MEMBER, new String[] { "nonAccessableMember", "memberWithNullProperties" });
//
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("groupWithNonAccessableMembers");
//                will(returnValue(groupWithNonAccessableMembersConfig));
//                allowing(groupWithNonAccessableMembersConfig).getProperties();
//                will(returnValue(groupWithNonAccessableMembersConfigProps));
//
//                allowing(configAdmin).getConfiguration("nonAccessableMember");
//                will(throwException(new IOException("Not able to access this config data")));
//
//                allowing(configAdmin).getConfiguration("memberWithNullProperties");
//                will(returnValue(null));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] groups = { "groupWithNonAccessableMembers" };
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//
//        checkInvalidMemberDefinitionErrorWasLogged("nonAccessableMember");
//        checkInvalidMemberDefinitionErrorWasLogged("memberWithNullProperties");
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An IOException on loading group configuration data should result in a RegistryException.
     */
//    @Test
//    public void getUserRegistryMap_invalidUserConfiguration() throws Exception {
//        final Configuration userWithNoFieldsConfig = mock.mock(Configuration.class, "userWithNoFields");
//        final Configuration userWithNoNameConfig = mock.mock(Configuration.class, "userWithNoName");
//        final Dictionary userWithNoNameConfigProps = new Hashtable();
//        userWithNoNameConfigProps.put("pass", "pwd");
//        final Configuration userWithNoPasswordConfig = mock.mock(Configuration.class, "userWithNoPassword");
//        final Dictionary userWithNoPasswordConfigProps = new Hashtable();
//        userWithNoPasswordConfigProps.put("name", "user");
//
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("userWithNoFields");
//                will(returnValue(userWithNoFieldsConfig));
//                allowing(userWithNoFieldsConfig).getProperties();
//                will(returnValue(new Hashtable()));
//
//                allowing(configAdmin).getConfiguration("userWithNoName");
//                will(returnValue(userWithNoNameConfig));
//                allowing(userWithNoNameConfig).getProperties();
//                will(returnValue(userWithNoNameConfigProps));
//
//                allowing(configAdmin).getConfiguration("userWithNoPassword");
//                will(returnValue(userWithNoPasswordConfig));
//                allowing(userWithNoPasswordConfig).getProperties();
//                will(returnValue(userWithNoPasswordConfigProps));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic1");
//        String[] users = { "userWithNoFields", "userWithNoName", "userWithNoPassword" };
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//        assertEquals("Should contain no groups", 0, reg.getUsers(".*", 0).getList().size());
//
//        checkInvalidUserDefinitionErrorWasLogged("A user element must define a name.");
//        checkInvalidUserDefinitionErrorWasLogged("The user element with name 'user' must define a password.");
//        checkNoUsersWarningWasLogged("basic1");
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An invalid group definition is logged but does not halt creation.
     */
//    @Test
//    public void getUserRegistryMap_invalidGroupConfiguration() throws Exception {
//        final Configuration groupWithNoFieldsConfig = mock.mock(Configuration.class, "groupWithNoFields");
//        final Configuration groupWithNoNameConfig = mock.mock(Configuration.class, "groupWithNoName");
//        final Dictionary groupWithNoNameConfigProps = new Hashtable();
//
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("groupWithNoFields");
//                will(returnValue(groupWithNoFieldsConfig));
//                allowing(groupWithNoFieldsConfig).getProperties();
//                will(returnValue(new Hashtable()));
//
//                allowing(configAdmin).getConfiguration("groupWithNoName");
//                will(returnValue(groupWithNoNameConfig));
//                allowing(groupWithNoNameConfig).getProperties();
//                will(returnValue(groupWithNoNameConfigProps));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] groups = { "groupWithNoFields", "groupWithNoName" };
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//        assertEquals("Should contain no groups", 0, reg.getGroups(".*", 0).getList().size());
//
//        checkInvalidGroupDefinitionErrorWasLogged("A group element must define a name.");
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * An invalid member definition is logged but does not halt creation.
     */
//    @Test
//    public void getUserRegistryMap_invalidMemberConfiguration() throws Exception {
//        final Configuration groupWithInvalidMemberConfig = mock.mock(Configuration.class, "groupWithInvalidMember");
//        final Dictionary groupWithInvalidMemberConfigProps = new Hashtable();
//        groupWithInvalidMemberConfigProps.put(DynamicBasicRegistry.CFG_KEY_NAME, "group1");
//        groupWithInvalidMemberConfigProps.put(DynamicBasicRegistry.CFG_KEY_MEMBER, new String[] { "memberWithNoFields", "memberWithNoName" });
//
//        final Configuration memberWithNoFieldsConfig = mock.mock(Configuration.class, "memberWithNoFields");
//        final Configuration memberWithNoNameConfig = mock.mock(Configuration.class, "memberWithNoName");
//        final Dictionary memberWithNoNameConfigProps = new Hashtable();
//
//        mock.checking(new Expectations() {
//            {
//                allowing(configAdmin).getConfiguration("groupWithInvalidMember");
//                will(returnValue(groupWithInvalidMemberConfig));
//                allowing(groupWithInvalidMemberConfig).getProperties();
//                will(returnValue(groupWithInvalidMemberConfigProps));
//
//                allowing(configAdmin).getConfiguration("memberWithNoFields");
//                will(returnValue(memberWithNoFieldsConfig));
//                allowing(memberWithNoFieldsConfig).getProperties();
//                will(returnValue(new Hashtable()));
//
//                allowing(configAdmin).getConfiguration("memberWithNoName");
//                will(returnValue(memberWithNoNameConfig));
//                allowing(memberWithNoNameConfig).getProperties();
//                will(returnValue(memberWithNoNameConfigProps));
//            }
//        });
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] groups = { "groupWithInvalidMember" };
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);
//
//        UserRegistry reg = factory.getUserRegistry(map);
//        assertNotNull("Instance should be non-null", reg);
//        assertTrue("Instance should be a DynamicBasicRegistry", reg instanceof DynamicBasicRegistry);
//        assertEquals("Should contain one group", 1, reg.getGroups(".*", 0).getList().size());
//
//        checkInvalidMemberDefinitionErrorWasLogged("A member element must define a name.");
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
    @Test
    public void getUserRegistryMap_validConfiguration() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] users = { "user_instance0", "user_instance1", "user_instance2", "user_instance3" };
//        String[] groups = { "group_instance0", "group_instance1", "group_instance2" };
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        map.put(DynamicBasicRegistry.CFG_KEY_REALM, "BasicRealm");
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);

        BasicRegistry reg = new BasicRegistry();
        reg.activate(new BasicRegistryConfig() {

            @Override
            public User[] user() {
                return new User[] { user0, user1, user2, user3 };
            }

            @Override
            public String realm() {
                return "BasicRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public Group[] group() {
                return new Group[] { group0, group1, group2 };
            }

            @Override
            public String config_id() {
                return "basic";
            }
        });
        assertTrue("encodedUser should exist in the registry", reg.isValidUser("encodedUser"));
        assertTrue("user1 should exist in the registry", reg.isValidUser("user1"));
        assertTrue("'user 2' should exist in the registry", reg.isValidUser("user 2"));
        assertTrue("hashedUser should exist in the registry", reg.isValidUser("hashedUser"));
        assertTrue("group0 should exist in the registry", reg.isValidGroup("group0"));
        assertTrue("group1 should exist in the registry", reg.isValidGroup("group1"));
        assertTrue("'my group 2' should exist in the registry", reg.isValidGroup("my group 2"));
        assertEquals("user1 should have two groups", 2, reg.getGroupsForUser("user1").size());
        assertTrue("user1 should belong to group1", reg.getGroupsForUser("user1").contains("group1"));
        assertTrue("user1 should belong to 'my group 2'", reg.getGroupsForUser("user1").contains("my group 2"));

        assertEquals("Password should be decoded",
                     "encodedUser", reg.checkPassword("encodedUser", "pass3"));
        assertEquals("Hashed password should be decoded",
                     "hashedUser", reg.checkPassword("hashedUser", "password"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
    @Test
    public void getUserRegistryMap_configurationWithDuplicates() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] users = { "user_instance0", "user_instance1", "user_instance2", "user_instance1" };
//        String[] groups = { "group_instance0", "group_instance1", "group_instance2", "group_instance3", "group_instance1" };
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        map.put(DynamicBasicRegistry.CFG_KEY_REALM, "BasicRealm");
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);

        BasicRegistry reg = new BasicRegistry();
        reg.activate(new BasicRegistryConfig() {

            @Override
            public User[] user() {
                return new User[] { user0, user1, user2, user1 };
            }

            @Override
            public String realm() {
                return "BasicRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public Group[] group() {
                return new Group[] { group0, group1, group2, group3, group1 };
            }

            @Override
            public String config_id() {
                return "basic";
            }
        });
        assertTrue("encodedUser should exist in the registry", reg.isValidUser("encodedUser"));
        assertFalse("user1 should NOT exist in the registry (as its duplicated)", reg.isValidUser("user1"));
        assertTrue("'user 2' should exist in the registry", reg.isValidUser("user 2"));
        assertTrue("group0 should exist in the registry", reg.isValidGroup("group0"));
        assertFalse("group1 should NOT exist in the registry", reg.isValidGroup("group1"));
        assertTrue("'my group 2' should exist in the registry", reg.isValidGroup("my group 2"));

        assertEquals("Password should be decoded",
                     "encodedUser", reg.checkPassword("encodedUser", "pass3"));

        assertTrue("Expected duplicate user error message not found",
                   outputMgr.checkForStandardErr("CWWKS3104E: Multiple users with the name 'user1' are defined. The entries for this user will not be used."));

        assertTrue("Expected duplicate group error message not found",
                   outputMgr.checkForStandardErr("CWWKS3105E: Multiple groups with the name 'group1' are defined. The entries for this group will not be used."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
    @Test
    public void getUserRegistryMap_configurationWithTriplicates() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] users = { "user_instance1", "user_instance0", "user_instance1", "user_instance2", "user_instance1" };
//        String[] groups = { "group_instance0", "group_instance1", "group_instance2", "group_instance1", "group_instance3", "group_instance1" };
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        map.put(DynamicBasicRegistry.CFG_KEY_REALM, "BasicRealm");
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);

        BasicRegistry reg = new BasicRegistry();
        reg.activate(new BasicRegistryConfig() {

            @Override
            public User[] user() {
                return new User[] { user1, user0, user1, user2, user1 };
            }

            @Override
            public String realm() {
                return "BasicRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public Group[] group() {
                return new Group[] { group0, group1, group2, group1, group3, group1 };
            }

            @Override
            public String config_id() {
                return "basic";
            }
        });
        assertTrue("encodedUser should exist in the registry", reg.isValidUser("encodedUser"));
        assertFalse("user1 should NOT exist in the registry (as its duplicated)", reg.isValidUser("user1"));
        assertTrue("'user 2' should exist in the registry", reg.isValidUser("user 2"));
        assertTrue("group0 should exist in the registry", reg.isValidGroup("group0"));
        assertFalse("group1 should NOT exist in the registry", reg.isValidGroup("group1"));
        assertTrue("'my group 2' should exist in the registry", reg.isValidGroup("my group 2"));

        assertEquals("Password should be decoded",
                     "encodedUser", reg.checkPassword("encodedUser", "pass3"));

        assertTrue("Expected duplicate user error message not found",
                   outputMgr.checkForStandardErr("CWWKS3104E: Multiple users with the name 'user1' are defined. The entries for this user will not be used."));

        assertTrue("Expected duplicate group error message not found",
                   outputMgr.checkForStandardErr("CWWKS3105E: Multiple groups with the name 'group1' are defined. The entries for this group will not be used."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
    @Test
    public void getUserRegistryMap_configurationWithUnknownGroupMember() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] users = { "user_instance1" };
//        String[] groups = { "group_instance2" };
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        map.put(DynamicBasicRegistry.CFG_KEY_REALM, "BasicRealm");
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);

        BasicRegistry reg = new BasicRegistry();
        reg.activate(new BasicRegistryConfig() {

            @Override
            public User[] user() {
                return new User[] { user1 };
            }

            @Override
            public String realm() {
                return "BasicRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public Group[] group() {
                return new Group[] { group2 };
            }

            @Override
            public String config_id() {
                return "basic";
            }
        });
        assertTrue("'user1' should exist in the registry", reg.isValidUser("user1"));
        assertTrue("'my group 2' should exist in the registry", reg.isValidGroup("my group 2"));
        assertEquals("'user1' should have one group", 1, reg.getGroupsForUser("user1").size());

        assertTrue("Expected warning message about non-matching user member entry not found",
                   outputMgr.checkForMessages("CWWKS3107W: Member entry with the name 'user 2' for group 'my group 2' does not match a defined user."));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistryFactory##getUserRegistry(Map)}.
     *
     * A non-null, BasicRegistry instance shall be constructed.
     */
    @Test
    public void getUserRegistryMap_configurationWithDuplicateGroupMember() throws Exception {
//        Map<String, Object> map = new HashMap<String, Object>();
//        String[] users = { "user_instance2" };
//        String[] groups = { "group_instance3" };
//        map.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        map.put(DynamicBasicRegistry.CFG_KEY_REALM, "BasicRealm");
//        map.put(DynamicBasicRegistry.CFG_KEY_USER, users);
//        map.put(DynamicBasicRegistry.CFG_KEY_GROUP, groups);

        BasicRegistry reg = new BasicRegistry();
        reg.activate(new BasicRegistryConfig() {

            @Override
            public User[] user() {
                return new User[] { user2 };
            }

            @Override
            public String realm() {
                return "BasicRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public Group[] group() {
                return new Group[] { group3 };
            }

            @Override
            public String config_id() {
                return "basic";
            }
        });
        assertTrue("'user 2' should exist in the registry", reg.isValidUser("user 2"));
        assertTrue("multiGroup should exist in the registry", reg.isValidGroup("multiGroup"));
        assertEquals("'user 2' should have one group", 1, reg.getGroupsForUser("user 2").size());
        assertTrue("'user 2' should belong to multiGroup", reg.getGroupsForUser("user 2").contains("multiGroup"));

        assertTrue("Expected warning message about duplicate member entry not found",
                   outputMgr.checkForStandardOut("CWWKS3106W: Multiple member entries with the name 'user 2' are defined for the group 'multiGroup'."));

        assertFalse("Warning message about non-matching user member entry should not be logged",
                    outputMgr.checkForStandardOut("CWWKS3107W"));
    }
}
