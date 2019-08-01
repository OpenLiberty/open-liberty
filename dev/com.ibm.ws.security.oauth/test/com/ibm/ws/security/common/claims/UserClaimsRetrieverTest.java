/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.claims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;

public class UserClaimsRetrieverTest {

    private final Mockery mockery = new JUnit4Mockery();
    private final AuthCacheService authCache = mockery.mock(AuthCacheService.class);
    private final UserRegistry userRegistry = mockery.mock(UserRegistry.class);
    private final String user1 = "user1";
    private final String user1Realm = "realm1";
    private final String user2 = "user2";
    private final String user2Realm = "realm2";
    private final String groupIdentifier = "groups";

    private List<String> expectedGroupsForUser1;
    private List<String> expectedGroupsForUser2;

    @Before
    public void setUp() throws Exception {
        setupTestGroups();
    }

    private void setupTestGroups() {
        expectedGroupsForUser1 = new ArrayList<String>();
        expectedGroupsForUser1.add("group1");
        expectedGroupsForUser1.add("group2");

        expectedGroupsForUser2 = new ArrayList<String>();
        expectedGroupsForUser2.add("group3");
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void userClaims_fromCachedSubject() throws Exception {
        setExpecationsForCachedSubjectPath(user1Realm, user1, "group1", "group2");
        UserClaimsRetriever userClaimsRetriever = new UserClaimsRetriever(authCache, userRegistry);

        UserClaims userClaims = userClaimsRetriever.getUserClaims(user1, groupIdentifier);

        assertUser1Claims(userClaims);
        assertUserClaimsInMap(userClaims.asMap(), user1Realm, user1, expectedGroupsForUser1);
    }

    private void assertUser1Claims(UserClaims userClaims) {
        assertUserClaims(userClaims, user1Realm, user1, expectedGroupsForUser1);
    }

    private void assertUserClaims(UserClaims userClaims, String realmName, String uniqueSecurityName, List<String> groups) {
        assertEquals("There must be a uniqueSecurityName for the user.", uniqueSecurityName, userClaims.getUniqueSecurityName());
        assertEquals("There must be groups for the user.", groups, userClaims.getGroups());
        assertEquals("There must be a realmName for user.", realmName, userClaims.getRealmName());
    }

    private void assertUserClaimsInMap(Map<String, Object> userClaimsMap, String realmName, String uniqueSecurityName, List<String> groups) {
        assertEquals("There must be a uniqueSecurityName for the user.", uniqueSecurityName, userClaimsMap.get(UserClaims.USER_CLAIMS_UNIQUE_SECURITY_NAME));
        assertEquals("There must be groups for the user.", groups, userClaimsMap.get(groupIdentifier));
        assertEquals("There must be a realmName for user.", realmName, userClaimsMap.get(UserClaims.USER_CLAIMS_REALM_NAME));
    }

    @Test
    public void userClaims_fromCachedSubject_differentRealmUserAndGroups() throws Exception {
        setExpecationsForCachedSubjectPath(user2Realm, user2, "group3");
        UserClaimsRetriever userClaimsRetriever = new UserClaimsRetriever(authCache, userRegistry);

        UserClaims userClaims = userClaimsRetriever.getUserClaims(user2, groupIdentifier);

        assertUserClaims(userClaims, user2Realm, user2, expectedGroupsForUser2);
    }

    @Test
    public void userClaims_fromCachedSubject_noGroups() throws Exception {
        setExpecationsForCachedSubjectPath(user1Realm, user1);
        UserClaimsRetriever userClaimsRetriever = new UserClaimsRetriever(authCache, userRegistry);

        UserClaims userClaims = userClaimsRetriever.getUserClaims(user1, groupIdentifier);

        assertNull("There must not be groups for the user.", userClaims.getGroups());
    }

    @Test
    public void userClaims_fromRegistry() throws Exception {
        setUserRegistryExpectations(user1Realm);
        setExpectationsForRegistryPath();
        noAuthCacheSubjectExpected();
        UserClaimsRetriever userClaimsRetriever = new UserClaimsRetriever(authCache, userRegistry);

        UserClaims userClaims = userClaimsRetriever.getUserClaims(user1, groupIdentifier);

        assertUser1Claims(userClaims);
    }

    private void setExpectationsForRegistryPath() throws EntryNotFoundException, RegistryException {
        final List<String> groupsForUser = new ArrayList<String>();
        groupsForUser.add("group1");
        groupsForUser.add("group2");
        mockery.checking(new Expectations() {
            {
                one(userRegistry).getUniqueUserId(user1);
                will(returnValue(user1));
                one(userRegistry).getGroupsForUser(user1);
                will(returnValue(groupsForUser));
            }
        });
    }

    private void noAuthCacheSubjectExpected() {
        final String cacheKey = getCacheKey(user1Realm, user1);
        mockery.checking(new Expectations() {
            {
                one(authCache).getSubject(cacheKey);
                will(returnValue(null));
            }
        });
    }

    private void setExpecationsForCachedSubjectPath(String realm, String username, String... groups) throws Exception {
        setUserRegistryExpectations(realm);
        final Subject subject = createTestSubject(realm, username, groups);
        setAuthCacheExpectations(realm, username, subject);
    }

    private void setUserRegistryExpectations(final String realm) {
        mockery.checking(new Expectations() {
            {
                one(userRegistry).getRealm();
                will(returnValue(realm));
            }
        });
    }

    private Subject createTestSubject(String realm, String username, String... groups) throws Exception {
        Subject subject = new Subject();
        WSCredential credential = createCredential(realm, username, groups);
        subject.getPublicCredentials().add(credential);
        return subject;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private WSCredential createCredential(String realm, final String username, String... groups) throws Exception {
        final WSCredential credential = mockery.mock(WSCredential.class, username);
        final ArrayList groupIds = new ArrayList();
        for (String group : groups) {
            // WSCredential returns groupIds with format group:<realm>/<group>
            groupIds.add("group:" + realm + "/" + group);
        }

        mockery.checking(new Expectations() {
            {
                one(credential).getGroupIds();
                will(returnValue(groupIds));
                one(credential).getUniqueSecurityName();
                will(returnValue(username));
                allowing(credential).isUnauthenticated(); //
                will(returnValue(false)); //
            }
        });
        return credential;
    }

    private String getCacheKey(String realm, String username) {
        return realm + ":" + username;
    }

    private void setAuthCacheExpectations(String realm, String username, final Subject subject) {
        final String cacheKey = getCacheKey(realm, username);
        mockery.checking(new Expectations() {
            {
                one(authCache).getSubject(cacheKey);
                will(returnValue(subject));
            }
        });
    }

}
