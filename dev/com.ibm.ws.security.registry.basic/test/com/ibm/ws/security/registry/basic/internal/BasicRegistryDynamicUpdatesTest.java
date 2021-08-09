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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.security.registry.UserRegistry;

/**
 * No modified method, no updates.
 */
public class BasicRegistryDynamicUpdatesTest extends BasicRegistryConfigAdminMock {
    private static Map<String, Object> originalConfigProps = new HashMap<String, Object>();
    private static Map<String, Object> modifiedConfigProps = new HashMap<String, Object>();
    private UserRegistry config;

    @Test
    public void testNothing() {};

//    /**
//     * Perform some setup once as we'll reuse this.
//     */
//    @BeforeClass
//    public static void setUpMaps() {
//        String[] origUsers = { "user_instance1" };
//        String[] origGroups = { "group_instance1" };
//        originalConfigProps.put(BasicRegistry.CFG_KEY_ID, "basic");
//        originalConfigProps.put(DynamicBasicRegistry.CFG_KEY_REALM, "originalRealm");
//        originalConfigProps.put(DynamicBasicRegistry.CFG_KEY_USER, origUsers);
//        originalConfigProps.put(DynamicBasicRegistry.CFG_KEY_GROUP, origGroups);
//
//        String[] modifiedUsers = { "user_instance1", "user_instance2" };
//        String[] modifiedGroups = { "group_instance1", "group_instance2" };
//        modifiedConfigProps.put(DynamicBasicRegistry.CFG_KEY_ID, "basic");
//        modifiedConfigProps.put(DynamicBasicRegistry.CFG_KEY_REALM, "modifiedRealm");
//        modifiedConfigProps.put(DynamicBasicRegistry.CFG_KEY_USER, modifiedUsers);
//        modifiedConfigProps.put(DynamicBasicRegistry.CFG_KEY_GROUP, modifiedGroups);
//    }
//
//    @Override
//    @Before
//    public void setUp() throws Exception {
//        super.setUp();
//
//        config = new UserRegistryConfiguration();
//        config.activate(originalConfigProps);
//    }
//
//    /**
//     * Perform some cleanup to simulate the service stopping.
//     */
//    @After
//    public void tearDown() {
//        mock.assertIsSatisfied();
//
//        config.deactivate(null);
//        config = null;
//    }
//
//    /**
//     * @return
//     * @throws RegistryException
//     */
//    private UserRegistry getUserRegistry() throws RegistryException {
//        BasicRegistryFactory factory = new BasicRegistryFactory();
//        factory.setConfigurationAdmin(configAdminRef);
//        factory.activate(cc);
//        return config.getUserRegistry(factory);
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getRealm()}.
//     * getRealm() shall return modified realm name after a configuration change
//     */
//    @Test
//    public void getRealm_default() throws Exception {
//        assertEquals("Realm name sould be originalRealm", "originalRealm", getUserRegistry().getRealm());
//
//        config.modify(modifiedConfigProps);
//
//        assertEquals("Realm name should be modifiedRealm", "modifiedRealm", getUserRegistry().getRealm());
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#checkPassword(java.lang.String, java.lang.String)}.
//     */
//    @Test
//    public void checkPasswordWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("user1 should pass checkPassword",
//                     "user1", reg.checkPassword("user1", "pass1"));
//        assertNull("user1 should not pass checkPassword (not yet defined)",
//                   reg.checkPassword("user 2", "pass 2"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("user1 should pass checkPassword",
//                     "user1", reg.checkPassword("user1", "pass1"));
//        assertEquals("'user 2' should pass checkPassword",
//                     "user 2", reg.checkPassword("user 2", "pass 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#isValidUser(String)}.
//     */
//    @Test
//    public void isValidUserWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertTrue("user1 should be valid", reg.isValidUser("user1"));
//        assertFalse("'user 2' should not be valid", reg.isValidUser("user 2"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertTrue("user1 should be valid", reg.isValidUser("user1"));
//        assertTrue("'user 2' should be valid", reg.isValidUser("user 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
//     */
//    @Test
//    public void getUsersWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//
//        SearchResult result = reg.getUsers(".*", 0);
//        assertNotNull("SearchResult must never be NULL", result);
//        assertEquals(1, result.getList().size());
//        assertFalse(result.hasMore());
//        assertTrue(result.getList().contains("user1"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        result = reg.getUsers(".*", 0);
//        assertNotNull("SearchResult must never be NULL", result);
//        assertEquals(2, result.getList().size());
//        assertFalse(result.hasMore());
//        assertTrue(result.getList().contains("user1"));
//        assertTrue(result.getList().contains("user 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserDisplayName(String)}.
//     */
//    @Test
//    public void getUserDisplayNameWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("user1", reg.getUserDisplayName("user1"));
//        try {
//            reg.getUserDisplayName("user 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("user1", reg.getUserDisplayName("user1"));
//        assertEquals("user 2", reg.getUserDisplayName("user 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueUserId(String)}.
//     */
//    @Test
//    public void getUniqueUserIdWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("user1", reg.getUniqueUserId("user1"));
//        try {
//            reg.getUniqueUserId("user 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("user1", reg.getUniqueUserId("user1"));
//        assertEquals("user 2", reg.getUniqueUserId("user 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserSecurityName(String)}.
//     */
//    @Test
//    public void getUserSecurityNameWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("user1", reg.getUserSecurityName("user1"));
//        try {
//            reg.getUserSecurityName("user 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("user1", reg.getUserSecurityName("user1"));
//        assertEquals("user 2", reg.getUserSecurityName("user 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#isValidGroup(String)}.
//     */
//    @Test
//    public void isValidGroupWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertTrue(reg.isValidGroup("group1"));
//        assertFalse(reg.isValidGroup("my group 2"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertTrue(reg.isValidGroup("group1"));
//        assertTrue(reg.isValidGroup("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
//     * A negative limit results in a default SearchResult object.
//     */
//    @Test
//    public void getGroupsWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//
//        SearchResult result = reg.getGroups(".*", 0);
//        assertNotNull("SearchResult must never be NULL", result);
//        assertEquals(1, result.getList().size());
//        assertFalse(result.hasMore());
//        assertTrue(result.getList().contains("group1"));
//        assertFalse(result.getList().contains("my group 2"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        result = reg.getGroups(".*", 0);
//        assertNotNull("SearchResult must never be NULL", result);
//        assertEquals(2, result.getList().size());
//        assertFalse(result.hasMore());
//        assertTrue(result.getList().contains("group1"));
//        assertTrue(result.getList().contains("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupDisplayName(String)}.
//     */
//    @Test
//    public void getGroupDisplayNameWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("group1", reg.getGroupDisplayName("group1"));
//        try {
//            reg.getGroupDisplayName("my group 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("group1", reg.getGroupDisplayName("group1"));
//        assertEquals("my group 2", reg.getGroupDisplayName("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupId(String)}.
//     */
//    @Test
//    public void getUniqueGroupIdWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("group1", reg.getUniqueGroupId("group1"));
//        try {
//            reg.getUniqueGroupId("my group 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("group1", reg.getUniqueGroupId("group1"));
//        assertEquals("my group 2", reg.getUniqueGroupId("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupSecurityName(String)}.
//     */
//    @Test
//    public void getGroupSecurityNameWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        assertEquals("group1", reg.getGroupSecurityName("group1"));
//        try {
//            reg.getGroupSecurityName("my group 2");
//        } catch (EntryNotFoundException enfe) {
//            // expected
//        }
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        assertEquals("group1", reg.getGroupSecurityName("group1"));
//        assertEquals("my group 2", reg.getGroupSecurityName("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupIdsForUser(String)}.
//     */
//    @Test
//    public void getUniqueGroupIdsForUserWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        List<String> groups = reg.getUniqueGroupIdsForUser("user1");
//        assertNotNull(groups);
//        assertEquals(1, groups.size());
//        assertTrue(groups.contains("group1"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        groups = reg.getUniqueGroupIdsForUser("user1");
//        assertNotNull(groups);
//        assertEquals(2, groups.size());
//        assertTrue(groups.contains("group1"));
//        assertTrue(groups.contains("my group 2"));
//    }
//
//    /**
//     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupsForUser(String)}.
//     */
//    @Test
//    public void getGroupsForUserWithDynamicUpdate() throws Exception {
//        UserRegistry reg = getUserRegistry();
//        List<String> groups = reg.getUniqueGroupIdsForUser("user1");
//        assertNotNull(groups);
//        assertEquals(1, groups.size());
//        assertTrue(groups.contains("group1"));
//
//        config.modify(modifiedConfigProps);
//
//        reg = getUserRegistry();
//        groups = reg.getUniqueGroupIdsForUser("user1");
//        assertNotNull(groups);
//        assertEquals(2, groups.size());
//        assertTrue(groups.contains("group1"));
//        assertTrue(groups.contains("my group 2"));
//    }
}
