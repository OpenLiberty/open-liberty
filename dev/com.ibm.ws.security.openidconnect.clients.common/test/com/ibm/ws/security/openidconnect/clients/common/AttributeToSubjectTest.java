/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.security.token.AttributeNameConstants;

import test.common.SharedOutputManager;
import test.common.junit.rules.MaximumJavaLevelRule;

public class AttributeToSubjectTest extends CommonTestClass {

    // Cap this unit test to Java 8 because it relies on legacy cglib which is not supported post JDK 8
    @ClassRule
    public static TestRule maxJavaLevel = new MaximumJavaLevelRule(8);

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.*=all=enabled");

    public interface MockAttributeToSubjectInterface {
        public String getTheRealmName();

        public String getTheUniqueSecurityName();
    }

    private final ConvergedClientConfig convClientConfig = mockery.mock(ConvergedClientConfig.class);
    private final Payload payload = mockery.mock(Payload.class);
    private final MockAttributeToSubjectInterface mockA2SInterface = mockery.mock(MockAttributeToSubjectInterface.class);

    private final String CLIENT_ID = "client_id";
    private final String UID_TOCREATE_SUBJECT = "uid_toCreate_subject";
    private final String UNIQUE_USERID = "unique_userID";
    private final String USER_NAME = "user_name";
    private final String REAL_NAME = "real_name";
    private final String GROUP_IDS = "group_ids";
    private final String GROUP1 = "group1";
    private final String SEGMENT2 = "segment2";
    private final String ACCESS_TOKEN = "segment1." + SEGMENT2 + ".segment3";
    private final String USER_IDENTIFIER = "user identifier";

    @BeforeClass
    public static void setUp() {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void after() {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * Tests:
     * - User identifier: String
     * - JSON object: Contains key that matches user identifier, but value is not a string
     */
    @Test
    public void testAttributeToSubject_UserIDIsNotString() throws IOException {
        final String USER_ID = "user_id";

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                allowing(convClientConfig).getUserIdentifier();
                will(returnValue(USER_ID));
            }
        });

        final String jsonString = "{\"user\":\"user1\" , \"" + USER_ID + "\":0101}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertNull("User name was expected to be null but was: [" + attrToSub.userName + "].", attrToSub.userName);
        assertNull("Custom cache key was expected to be null but was: [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey);
        assertNull("Realm was expected to be null but was: [" + attrToSub.realm + "].", attrToSub.realm);
        assertNull("Unique security name was expected to be null but was: [" + attrToSub.uniqueSecurityName + "].", attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    /**
     * Tests:
     * - User identifier: null (not configured)
     * - User identity to create subject: null (not configured)
     */
    @Test
    public void testAttributeToSubject_UidToCreateSubNotPresent() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(null));
                one(convClientConfig).getUserIdentityToCreateSubject();
                will(returnValue(null));
            }
        });

        final String jsonString = "{\"user\":\"user1\"}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertNull("User name was expected to be null but was: [" + attrToSub.userName + "].", attrToSub.userName);
        assertNull("Custom cache key was expected to be null but was: [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey);
        assertNull("Realm was expected to be null but was: [" + attrToSub.realm + "].", attrToSub.realm);
        assertNull("Unique security name was expected to be null but was: [" + attrToSub.uniqueSecurityName + "].", attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    /**
     * Tests:
     * - User identifier: null (not configured)
     * - User identity to create subject: String
     * - JSON object: Contains key that matches user identity for subject, but value is not a string
     */
    @Test
    public void testAttributeToSubject_UidToCreateSubNotString() throws IOException {

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(null));
                one(convClientConfig).getUserIdentityToCreateSubject();
                will(returnValue(UID_TOCREATE_SUBJECT));
            }
        });

        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":100}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertNull("User name was expected to be null but was: [" + attrToSub.userName + "].", attrToSub.userName);
        assertNull("Custom cache key was expected to be null but was: [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey);
        assertNull("Realm was expected to be null but was: [" + attrToSub.realm + "].", attrToSub.realm);
        assertNull("Unique security name was expected to be null but was: [" + attrToSub.uniqueSecurityName + "].", attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    /**
     * Tests:
     * - User identifier: null (not configured)
     * - User identity to create subject: String
     * - Realm name: String
     * - Unique user identifier: String
     * - Group identifier: String
     * - JSON object:
     * ---- Contains key that matches user identity for subject with valid value
     * ---- Contains key that matches group identifier with valid array value
     */
    @Test
    public void testAttributeToSubject_UidToCreateSubPresent() throws IOException {
        final String USERID = "userID";
        final Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(null));
                one(convClientConfig).getUserIdentityToCreateSubject();
                will(returnValue(UID_TOCREATE_SUBJECT));
                allowing(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(false));
                one(convClientConfig).getRealmName();
                will(returnValue(REAL_NAME));
                allowing(convClientConfig).getUniqueUserIdentifier();
                will(returnValue(UNIQUE_USERID));
                allowing(convClientConfig).getGroupIdentifier();
                will(returnValue(GROUP_IDS));
            }
        });

        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":\"" + USER_NAME + "\" , \"" + UNIQUE_USERID + "\":\"" + USERID + "\"}";
        JSONObject jobj = JSONObject.parse(jsonString);
        JSONArray groupIds = new JSONArray();
        groupIds.add(GROUP1);
        groupIds.add("group2");
        jobj.put(GROUP_IDS, groupIds);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertEquals("User name did not match expected value.", USER_NAME, attrToSub.userName);
        assertTrue("Expected that 'custom cache key' starts with:" + USER_NAME + " but it didn't. Cache key was [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey.startsWith(USER_NAME));
        assertEquals("Realm name did not match expected value.", REAL_NAME, attrToSub.realm);
        assertFalse("Expected a false value but received:" + attrToSub.checkForNullRealm() + ".", attrToSub.checkForNullRealm());
        assertEquals("Unique security name did not match expected value.", USERID, attrToSub.uniqueSecurityName);
        assertTrue("JSONArray should have contained:" + GROUP1 + " but it didn't. Group IDs were: " + attrToSub.groupIds, attrToSub.groupIds.contains(GROUP1));

        attrToSub.doMapping(customProperties, null);
        assertNotNull("Expected a valid value but received null for " + AttributeNameConstants.WSCREDENTIAL_UNIQUEID + " custom prop.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("Value for " + customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM) + " custom prop did not match expected value.",
                REAL_NAME, customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertNotNull("Expected a valid value but received null for " + AttributeNameConstants.WSCREDENTIAL_GROUPS + " custom prop.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS));
        assertNotNull("Expected a valid value but received null for " + ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS + " custom prop.", customProperties.get(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS));
    }

    /**
     * Tests:
     * - User identifier: null (not configured)
     * - User identity to create subject: String
     * - Realm name: null (not configured)
     * - Realm identifier: String
     * - Unique user identifier: String
     * - Group identifier: String
     * - JSON object:
     * ---- Contains key that matches realm identifier with valid value
     * ---- Contains key that matches user identifier for subject with valid value
     * ---- Contains key that matches group identifier with valid array value
     */
    @Test
    public void testAttributeToSubject_Groups() throws IOException {
        final String REAL_NAMEID = "real_nameID";
        final String UNIQUE_USERID = "unique_userID";

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(null));
                one(convClientConfig).getUserIdentityToCreateSubject();
                will(returnValue(UID_TOCREATE_SUBJECT));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(false));
                one(convClientConfig).getRealmName();
                will(returnValue(null));
                allowing(convClientConfig).getRealmIdentifier();
                will(returnValue(REAL_NAMEID));
                allowing(convClientConfig).getUniqueUserIdentifier();
                will(returnValue(UNIQUE_USERID));
                allowing(convClientConfig).getGroupIdentifier();
                will(returnValue(GROUP_IDS));
            }
        });

        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":\"" + USER_NAME + "\" , \"" + UNIQUE_USERID + "\":000 ,\"" + REAL_NAMEID + "\":\""
                + REAL_NAME + "\" , \"" + GROUP_IDS + "\":000}";
        JSONObject jobj = JSONObject.parse(jsonString);
        JSONArray groupIds = new JSONArray();
        groupIds.add(GROUP1);
        groupIds.add("group2");
        jobj.put(GROUP_IDS, groupIds);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertEquals("User name did not match expected value.", USER_NAME, attrToSub.userName);
        assertTrue("Expected that 'custom cache key' starts with:" + USER_NAME + " but it didn't. Cache key was [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey.startsWith(USER_NAME));
        assertEquals("Realm name did not match expected value.", REAL_NAME, attrToSub.realm);
        assertEquals("Unique security name did not match expected value.", USER_NAME, attrToSub.uniqueSecurityName);
        assertTrue("JSONArray should have contained:" + GROUP1 + " but it didn't. Group IDs were: " + attrToSub.groupIds, attrToSub.groupIds.contains(GROUP1));
    }

    /**
     * Tests:
     * - User identifier: String
     * - JSON object:
     * ---- Contains key that matches user identifier with valid value
     */
    @Test
    public void testAttributeToSubject_mapIdentityToRegistryUser() throws IOException {
        final String userName = "user1";
        final String jsonString = "{\"" + USER_IDENTIFIER + "\":\"" + userName + "\"}";

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(USER_IDENTIFIER));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });

        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        assertEquals("Client ID did not match expected value.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Token string did not match expected value.", ACCESS_TOKEN, attrToSub.tokenString);
        assertEquals("User name did not match expected value.", userName, attrToSub.userName);
        assertTrue("Expected that 'custom cache key' starts with:" + userName + " but it didn't. Cache key was [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey.startsWith(userName));
        assertNull("Realm was expected to be null but was: [" + attrToSub.realm + "].", attrToSub.realm);
        assertNull("Unique security name was expected to be null but was: [" + attrToSub.uniqueSecurityName + "].", attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    @Test
    public void testAttributeToSubject_UserIdentityToCreateSubjectIsNull_subIsNull() {
        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(null));
                one(convClientConfig).getUserIdentityToCreateSubject();
                will(returnValue(null));
                // Null userIdentifier and userIdentityToCreateSubject will default to "sub"
                one(payload).get(ClientConstants.SUB);
                will(returnValue(null));
            }
        });

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, payload, ACCESS_TOKEN);

        assertEquals("Did not get expected clientId.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Did not get expected access token string.", ACCESS_TOKEN, attrToSub.tokenString);
        assertNull("User name was expected to be null but was: [" + attrToSub.userName + "].", attrToSub.userName);
        assertNull("Custom cache key was expected to be null but was: [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey);
        assertNull("Realm was expected to be null but was: [" + attrToSub.realm + "].", attrToSub.realm);
        assertNull("Unique security name was expected to be null but was: [" + attrToSub.uniqueSecurityName + "].", attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    @Test
    public void testAttributeToSubject_UserNameIsNotNull_noGroups() {

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(USER_IDENTIFIER));
                one(payload).get(USER_IDENTIFIER);
                will(returnValue(USER_NAME));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(false));
                one(mockA2SInterface).getTheRealmName();
                will(returnValue(REAL_NAME));
                one(mockA2SInterface).getTheUniqueSecurityName();
                will(returnValue(USER_NAME));
                one(convClientConfig).getGroupIdentifier();
                will(returnValue(GROUP_IDS));
                one(payload).get(GROUP_IDS);
                will(returnValue(null));
            }
        });

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, payload, ACCESS_TOKEN) {
            @Override
            protected String getTheRealmName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {
                return mockA2SInterface.getTheRealmName();
            }

            @Override
            protected String getTheUniqueSecurityName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {
                return mockA2SInterface.getTheUniqueSecurityName();
            }
        };

        assertEquals("Did not get expected clientId.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Did not get expected access token string.", ACCESS_TOKEN, attrToSub.tokenString);
        assertEquals("Did not get expected userName.", USER_NAME, attrToSub.userName);
        assertTrue("Expected that 'custom cache key' starts with:" + USER_NAME + " but it didn't. Cache key was [" + attrToSub.customCacheKey + "].", attrToSub.customCacheKey.startsWith(USER_NAME));
        assertEquals("Did not get expected realm.", REAL_NAME, attrToSub.realm);
        assertEquals("Did not get expected uniqueSecurityName.", USER_NAME, attrToSub.uniqueSecurityName);
        assertNull("Group IDs were expected to be null but were: [" + attrToSub.groupIds + "].", attrToSub.groupIds);
    }

    @Test
    public void testAttributeToSubject_UserNameIsNotNull_withGroups() {
        final List<String> groups = new ArrayList<String>();
        groups.add(GROUP1);

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(USER_IDENTIFIER));
                one(payload).get(USER_IDENTIFIER);
                will(returnValue(USER_NAME));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(false));
                one(mockA2SInterface).getTheRealmName();
                will(returnValue(REAL_NAME));
                one(mockA2SInterface).getTheUniqueSecurityName();
                will(returnValue(USER_NAME));
                one(convClientConfig).getGroupIdentifier();
                will(returnValue(GROUP_IDS));
                one(payload).get(GROUP_IDS);
                will(returnValue(groups));
            }
        });

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, payload, ACCESS_TOKEN) {
            @Override
            protected String getTheRealmName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {
                return mockA2SInterface.getTheRealmName();
            }

            @Override
            protected String getTheUniqueSecurityName(ConvergedClientConfig clientConfig, JSONObject jobj, Payload payload) {
                return mockA2SInterface.getTheUniqueSecurityName();
            }
        };

        assertEquals("Did not get expected clientId.", CLIENT_ID, attrToSub.clientId);
        assertEquals("Did not get expected access token string.", ACCESS_TOKEN, attrToSub.tokenString);
        assertEquals("Did not get expected userName.", USER_NAME, attrToSub.userName);
        assertEquals("Did not get expected realm.", REAL_NAME, attrToSub.realm);
        assertEquals("Did not get expected uniqueSecurityName.", USER_NAME, attrToSub.uniqueSecurityName);
        assertTrue("JSONArray should have contained:" + GROUP1 + " but it didn't. Group IDs were: " + attrToSub.groupIds, attrToSub.groupIds.contains(GROUP1));
    }

    @Test
    public void testGetTheRealmName_NullArgs() throws IOException {
        getJsonObjConstructorExpectations();

        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":\"" + USER_NAME + "\"}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        String realmName = attrToSub.getTheRealmName(convClientConfig, null, null);
        assertNull("Realm was not null as expected but was [" + realmName + "].", realmName);
    }

    @Test
    public void testGetTheRealmName_JsonObjNonNullRealm() throws IOException {
        getJsonObjConstructorExpectations();

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getRealmName();
                will(returnValue(REAL_NAME));
            }
        });

        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":\"" + USER_NAME + "\"}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        String realmName = attrToSub.getTheRealmName(convClientConfig, jobj, null);
        assertEquals("Did not get expected realm.", REAL_NAME, realmName);
    }

    @Test
    public void testGetTheRealmName_JsonObjNullRealmNullRealmIdentifier() throws IOException {
        getJsonObjConstructorExpectations();

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getRealmName();
                will(returnValue(null));
                one(convClientConfig).getRealmIdentifier();
                will(returnValue(null));
            }
        });

        String iss = "client02";
        final String jsonString = "{\"user\":\"user1\" , \"" + UID_TOCREATE_SUBJECT + "\":\"" + USER_NAME + "\", \"" + ClientConstants.ISS + "\":\"" + iss + "\"}";
        JSONObject jobj = JSONObject.parse(jsonString);

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, jobj, ACCESS_TOKEN);

        String realmName = attrToSub.getTheRealmName(convClientConfig, jobj, null);
        assertEquals("Did not get expected realm.", iss, realmName);
    }

    @Test
    public void testGetTheRealmName_PayloadNonNullRealm() throws IOException {
        getPayloadConstructorExpectations();

        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getRealmName();
                will(returnValue(REAL_NAME));
            }
        });

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, payload, ACCESS_TOKEN);

        String realmName = attrToSub.getTheRealmName(convClientConfig, null, payload);
        assertEquals("Did not get expected realm.", REAL_NAME, realmName);
    }

    @Test
    public void testGetTheRealmName_PayloadNullRealmNullRealmIdentifier() throws IOException {

        getPayloadConstructorExpectations();

        final String iss = "client02";
        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getRealmName();
                will(returnValue(null));
                one(convClientConfig).getRealmIdentifier();
                will(returnValue(null));
                one(payload).get(null);
                will(returnValue(null));
                one(payload).get(ClientConstants.ISS);
                will(returnValue(iss));
            }
        });

        AttributeToSubject attrToSub = new AttributeToSubject(convClientConfig, payload, ACCESS_TOKEN);

        String realmName = attrToSub.getTheRealmName(convClientConfig, null, payload);
        assertEquals("Did not get expected realm.", iss, realmName);
    }

    private void getJsonObjConstructorExpectations() {
        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(UID_TOCREATE_SUBJECT));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });
    }

    private void getPayloadConstructorExpectations() {
        mockery.checking(new Expectations() {
            {
                one(convClientConfig).getClientId();
                will(returnValue(CLIENT_ID));
                one(convClientConfig).getUserIdentifier();
                will(returnValue(UID_TOCREATE_SUBJECT));
                one(payload).get(UID_TOCREATE_SUBJECT);
                will(returnValue(USER_NAME));
                one(convClientConfig).isMapIdentityToRegistryUser();
                will(returnValue(true));
            }
        });
    }
}