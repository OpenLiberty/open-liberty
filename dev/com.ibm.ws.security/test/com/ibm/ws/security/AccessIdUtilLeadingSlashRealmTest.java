/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.internal.SecurityServiceImpl;

/**
 * Unit tests for {@link AccessIdUtil} with slash-containing realm values.
 * Covers leading-slash, internal-slash, trailing-slash, multi-realm, and bootstrap paths.
 */
@SuppressWarnings("unchecked")
public class AccessIdUtilLeadingSlashRealmTest {

    private static final String LEADING_SLASH_REALM = "/testRealm";
    private static final String USER_ACCESS_ID = "user:/testRealm/my_user";
    private static final String GROUP_ACCESS_ID = "group:/testRealm/groupA";

    private final Mockery mock = new JUnit4Mockery();
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "leadingSlashRealmRef");

    private AccessIdUtil accessIdUtil;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(securityServiceRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { LEADING_SLASH_REALM }));
            }
        });
        accessIdUtil = new AccessIdUtil();
        accessIdUtil.setSecurityService(securityServiceRef);
    }

    @After
    public void tearDown() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        mock.assertIsSatisfied();
    }

    @Test
    public void isUserAccessId_leadingSlashRealm_returnsTrue() {
        assertTrue(AccessIdUtil.isUserAccessId(USER_ACCESS_ID));
    }

    @Test
    public void isGroupAccessId_leadingSlashRealm_returnsTrue() {
        assertTrue(AccessIdUtil.isGroupAccessId(GROUP_ACCESS_ID));
    }

    @Test
    public void getEntityType_leadingSlashRealm_returnsUser() {
        assertEquals("user", AccessIdUtil.getEntityType(USER_ACCESS_ID));
    }

    @Test
    public void getEntityType_leadingSlashRealm_returnsGroup() {
        assertEquals("group", AccessIdUtil.getEntityType(GROUP_ACCESS_ID));
    }

    @Test
    public void getRealm_leadingSlashRealm_preservesLeadingSlash() {
        assertEquals(LEADING_SLASH_REALM, AccessIdUtil.getRealm(USER_ACCESS_ID));
    }

    @Test
    public void getUniqueId_leadingSlashRealm_returnsUniqueId() {
        assertEquals("my_user", AccessIdUtil.getUniqueId(USER_ACCESS_ID));
    }

    @Test
    public void getUniqueId_leadingSlashRealm_groupReturnsUniqueId() {
        assertEquals("groupA", AccessIdUtil.getUniqueId(GROUP_ACCESS_ID));
    }

    // "//realm" is invalid — ps requires exactly one leading slash.
    @Test
    public void isAccessId_doubleLeadingSlashRealm_returnsFalse() {
        assertFalse(AccessIdUtil.isAccessId("user://testRealm/my_user"));
    }

    // "///" has no realm segment; no pattern matches.
    @Test
    public void isAccessId_slashOnlyRealm_returnsFalse() {
        assertFalse(AccessIdUtil.isAccessId("user:///my_user"));
    }

    @Test
    public void getUniqueIdWithRealm_leadingSlashRealm_returnsUniqueId() {
        assertEquals("my_user", AccessIdUtil.getUniqueId(USER_ACCESS_ID, LEADING_SLASH_REALM));
    }

    @Test
    public void createAccessId_leadingSlashRealm_roundTrip() {
        String created = AccessIdUtil.createAccessId("user", LEADING_SLASH_REALM, "my_user");
        assertEquals(USER_ACCESS_ID, created);
        assertTrue(AccessIdUtil.isUserAccessId(created));
        assertEquals("user", AccessIdUtil.getEntityType(created));
        assertEquals(LEADING_SLASH_REALM, AccessIdUtil.getRealm(created));
        assertEquals("my_user", AccessIdUtil.getUniqueId(created));
    }

    // Regression: plain realm still works when a leading-slash realm is registered.
    @Test
    public void isUserAccessId_normalRealm_stillTrue() {
        assertTrue(AccessIdUtil.isUserAccessId("user:BasicRealm/my_user"));
    }

    @Test
    public void getRealm_normalRealm_noLeadingSlash() {
        assertEquals("BasicRealm", AccessIdUtil.getRealm("user:BasicRealm/my_user"));
    }

    @Test
    public void getUniqueId_normalRealm_returnsUniqueId() {
        assertEquals("my_user", AccessIdUtil.getUniqueId("user:BasicRealm/my_user"));
    }

    // Internal-slash realm: Pattern.quote treats the '/' as literal.
    @Test
    public void internalSlashRealm_isUserAccessId_returnsTrue() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "internalSlashRealmRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "my/realm" }));
            }
        });

        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:my/realm/bob"));
            assertEquals("my/realm", AccessIdUtil.getRealm("user:my/realm/bob"));
            assertEquals("bob", AccessIdUtil.getUniqueId("user:my/realm/bob"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }

    // Trailing-slash realm: accessId format is "user:myRealm//bob".
    @Test
    public void trailingSlashRealm_isUserAccessId_returnsTrue() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "trailingSlashRealmRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "myRealm/" }));
            }
        });

        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:myRealm//bob"));
            assertEquals("myRealm/", AccessIdUtil.getRealm("user:myRealm//bob"));
            assertEquals("bob", AccessIdUtil.getUniqueId("user:myRealm//bob"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }

    // Multiple realms including a leading-slash realm: both entries match.
    @Test
    public void multiRealm_withLeadingSlashRealm_bothMatch() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "multiRealmWithSlashRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "/testRealm", "BasicRealm" }));
            }
        });

        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:/testRealm/my_user"));
            assertEquals("/testRealm", AccessIdUtil.getRealm("user:/testRealm/my_user"));
            assertTrue(AccessIdUtil.isUserAccessId("user:BasicRealm/bob"));
            assertEquals("BasicRealm", AccessIdUtil.getRealm("user:BasicRealm/bob"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }

    // Multiple plain realms: both entries match independently.
    @Test
    public void multiRealm_twoNormalRealms_bothMatch() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "twoNormalRealmsRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "RealmA", "RealmB" }));
            }
        });

        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:RealmA/alice"));
            assertTrue(AccessIdUtil.isUserAccessId("user:RealmB/bob"));
            assertEquals("RealmA", AccessIdUtil.getRealm("user:RealmA/alice"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }

    // Without a realmHolder, ph/ps/p are the fallbacks.
    // Plain and single-leading-slash realms match; double-slash does not.
    @Test
    public void bootstrapFallback_noHolder_plainAndLeadingSlashMatch_doubleSlashDoesNot() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:BasicRealm/my_user"));
            assertTrue(AccessIdUtil.isUserAccessId("user:/testRealm/my_user"));
            assertFalse(AccessIdUtil.isUserAccessId("user://testRealm/my_user"));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // --- Gap fix: multi-segment unregistered leading-slash realms ---
    // ps was "(/[^/]+)/(.+)" which only matched ONE segment after the leading slash.
    // The new ps "(/.*)/([^/]+)" is greedy on the realm and treats the LAST slash as
    // the separator, so "/a/b" is the realm and "alice" is the uniqueId — correct.

    // Two-segment unregistered leading-slash realm resolves with last segment as uniqueId.
    @Test
    public void unregistered_twoSegmentLeadingSlashRealm_parsedCorrectly() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:/a/b/alice"));
            assertEquals("/a/b", AccessIdUtil.getRealm("user:/a/b/alice"));
            assertEquals("alice", AccessIdUtil.getUniqueId("user:/a/b/alice"));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // Three-segment unregistered leading-slash realm: last segment is always the uniqueId.
    @Test
    public void unregistered_threeSegmentLeadingSlashRealm_parsedCorrectly() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:/a/b/c/alice"));
            assertEquals("/a/b/c", AccessIdUtil.getRealm("user:/a/b/c/alice"));
            assertEquals("alice", AccessIdUtil.getUniqueId("user:/a/b/c/alice"));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // Single-segment case is unchanged: /myrealm still works as before.
    @Test
    public void unregistered_singleSegmentLeadingSlashRealm_unchanged() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            assertTrue(AccessIdUtil.isUserAccessId("user:/myrealm/testuser"));
            assertEquals("/myrealm", AccessIdUtil.getRealm("user:/myrealm/testuser"));
            assertEquals("testuser", AccessIdUtil.getUniqueId("user:/myrealm/testuser"));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // Empty uniqueId must still be rejected — the new ps requires at least one non-slash char
    // after the last slash, so "user:/a/b/" has no match.
    @Test
    public void unregistered_multiSegmentLeadingSlashRealm_emptyUniqueId_returnsFalse() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            assertFalse(AccessIdUtil.isAccessId("user:/a/b/"));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // Round-trip: createAccessId + parse for a multi-segment leading-slash realm.
    @Test
    public void unregistered_multiSegmentLeadingSlashRealm_roundTrip() {
        accessIdUtil.unsetSecurityService(securityServiceRef);
        try {
            String created = AccessIdUtil.createAccessId("user", "/a/b", "alice");
            assertEquals("user:/a/b/alice", created);
            assertTrue(AccessIdUtil.isUserAccessId(created));
            assertEquals("/a/b", AccessIdUtil.getRealm(created));
            assertEquals("alice", AccessIdUtil.getUniqueId(created));
        } finally {
            accessIdUtil.setSecurityService(securityServiceRef);
        }
    }

    // getUniqueId(accessId, realm) with an unregistered multi-segment realm uses the
    // on-demand compiled pattern and must not fall back to ps.
    @Test
    public void unregistered_multiSegmentLeadingSlashRealm_getUniqueIdWithRealm() {
        assertEquals("alice", AccessIdUtil.getUniqueId("user:/a/b/alice", "/a/b"));
    }

    // Multi-segment slash realm registered in the holder resolves correctly.
    @Test
    public void subpathRealm_matcherNonNull_and_partsCorrect() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "subpathRealmRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "/realm/sub" }));
            }
        });

        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            org.junit.Assert.assertNotNull(
                "matcher() must not return null for a subpath realm accessId",
                AccessIdUtil.matcher("user:/realm/sub/testuser"));
            assertEquals("/realm/sub", AccessIdUtil.getRealm("user:/realm/sub/testuser"));
            assertEquals("testuser", AccessIdUtil.getUniqueId("user:/realm/sub/testuser"));
            assertEquals("user", AccessIdUtil.getEntityType("user:/realm/sub/testuser"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }

    // A leading-slash realm not in the holder triggers on-demand pattern compilation.
    @Test
    public void getUniqueIdWithRealm_leadingSlashRealmNotInHolder_fallsBackToCompiledPattern() {
        String accessId = "user:/unknown/alice";
        assertEquals("alice", AccessIdUtil.getUniqueId(accessId, "/unknown"));
    }

    // Multi-realm holder: an access ID whose uniqueId is empty must still be rejected.
    // Guards the early-exit path in matcher() — the realmPatterns loop now applies to
    // ALL registered realms, not just single-realm holders.
    @Test
    public void multiRealm_incompleteAccessId_returnsFalse() {
        Mockery localMock = new JUnit4Mockery();
        ServiceReference<SecurityService> localRef =
                localMock.mock(ServiceReference.class, "multiRealmIncompleteRef");
        localMock.checking(new Expectations() {
            {
                allowing(localRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(new String[] { "/testRealm", "BasicRealm" }));
            }
        });
        accessIdUtil.unsetSecurityService(securityServiceRef);
        accessIdUtil.setSecurityService(localRef);
        try {
            // Realm matches realmPatterns[0] but uniqueId segment is empty — must return false.
            assertFalse(AccessIdUtil.isAccessId("user:/testRealm/"));
            // Realm matches realmPatterns[1] but uniqueId segment is empty — must return false.
            assertFalse(AccessIdUtil.isAccessId("user:BasicRealm/"));
        } finally {
            accessIdUtil.unsetSecurityService(localRef);
            accessIdUtil.setSecurityService(securityServiceRef);
            localMock.assertIsSatisfied();
        }
    }
}
