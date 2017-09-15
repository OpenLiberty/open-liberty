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
package com.ibm.ws.security.quickstart.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 *
 */
public class QuickStartSecurityRegistryTest {
    private static final String DEFAULT_ADMIN_USER = "bob";
    private static final String DEFAULT_ADMIN_PASSWORD = "bobpwd";
    private static final ProtectedString DEFAULT_ADMIN_PASSWORD_PROTECTED = new ProtectedString(DEFAULT_ADMIN_PASSWORD.toCharArray());
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private static UserRegistry reg;

    @BeforeClass
    public static void setUp() {
        reg = new QuickStartSecurityRegistry(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD_PROTECTED);
    }

    /**
     * Constructor does not support a null user value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_userCanNotBeNull() throws Exception {
        new QuickStartSecurityRegistry(null, DEFAULT_ADMIN_PASSWORD_PROTECTED);
    }

    /**
     * Constructor does not support an empty user value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_userCanNotBeEmpty() throws Exception {
        new QuickStartSecurityRegistry("", DEFAULT_ADMIN_PASSWORD_PROTECTED);
    }

    /**
     * Constructor does not support an empty user value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_userCanNotBeOnlyWhiteSpace() throws Exception {
        new QuickStartSecurityRegistry("  ", DEFAULT_ADMIN_PASSWORD_PROTECTED);
    }

    /**
     * Constructor does not support a null password value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_passwordCanNotBeNull() throws Exception {
        new QuickStartSecurityRegistry(DEFAULT_ADMIN_USER, null);
    }

    /**
     * Constructor does not support a null groups value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_passwordCanNotBeEmpty() throws Exception {
        new QuickStartSecurityRegistry(DEFAULT_ADMIN_USER, new ProtectedString("".toCharArray()));
    }

    /**
     * Constructor does not support a null groups value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_passwordCanNotBeOnlyWhiteSpace() throws Exception {
        new QuickStartSecurityRegistry(DEFAULT_ADMIN_USER, new ProtectedString("  ".toCharArray()));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getRealm()}.
     * getRealm() shall return the default realm name.
     */
    @Test
    public void getRealm() throws Exception {
        assertEquals(QuickStartSecurityRegistry.REALM_NAME, reg.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#checkPassword(String, String)}.
     * checkPassword() shall return null if the specified username/password
     * is not valid.
     */
    @Test
    public void checkPassword_invalidCredentials() throws Exception {
        assertNull(reg.checkPassword(DEFAULT_ADMIN_USER, "badPassword"));
        assertNull(reg.checkPassword("badUser", DEFAULT_ADMIN_PASSWORD));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#checkPassword(String, String)}.
     * checkPassword() shall return true if the default username/password
     * is provided for a default configuration.
     */
    @Test
    public void checkPassword_validCredentials() throws Exception {
        assertEquals(DEFAULT_ADMIN_USER, reg.checkPassword(DEFAULT_ADMIN_USER, DEFAULT_ADMIN_PASSWORD));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#checkPassword(String, String)}.
     * checkPassword() shall return true if the specified username/password
     * match the configured credentials.
     */
    @Test
    public void checkPassword_cornerCases() throws Exception {
        assertNull(reg.checkPassword(DEFAULT_ADMIN_USER, " "));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_userDoesntExist() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("cn=iDontExist");
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        reg.mapCertificate(cert);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_noCN() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("o=nocn");
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        reg.mapCertificate(cert);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test
    public void mapCertificate() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("CN=" + DEFAULT_ADMIN_USER);
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        assertEquals(DEFAULT_ADMIN_USER, reg.mapCertificate(cert));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#isValidUser(String)}.
     */
    @Test
    public void isValidUser() throws Exception {
        assertFalse(reg.isValidUser("user"));
        assertTrue(reg.isValidUser(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_emptyList() throws Exception {
        SearchResult result = reg.getUsers("idontexist.*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getUsers_negativeLimit() throws Exception {
        SearchResult result = reg.getUsers(".*", -1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_noResultBounded() throws Exception {
        SearchResult result = reg.getUsers("abc*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_noResultUnbounded() throws Exception {
        SearchResult result = reg.getUsers("abc*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultOverBound() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER, 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultBounded() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER, 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultUnbound() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER, 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultUnderBound() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER + ".*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultOverBound() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER + ".*", 3);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultBounded() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER + ".*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultUnbounded() throws Exception {
        SearchResult result = reg.getUsers(DEFAULT_ADMIN_USER + ".*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresUnderBound() throws Exception {
        SearchResult result = reg.getUsers(".*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresOverBound() throws Exception {
        SearchResult result = reg.getUsers(".*", 5);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresBounded() throws Exception {
        SearchResult result = reg.getUsers(".*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should only be 1 entry", 1, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain " + DEFAULT_ADMIN_USER, result.getList().contains(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresUnbounded() throws Exception {
        SearchResult result = reg.getUsers(".*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should only be 1 entries", 1, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain " + DEFAULT_ADMIN_USER, result.getList().contains(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUserDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_doesNotExist() throws Exception {
        reg.getUserDisplayName("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUserDisplayName(String)}.
     */
    @Test
    public void getUserDisplayName_exists() throws Exception {
        assertEquals(DEFAULT_ADMIN_USER, reg.getUserDisplayName(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUniqueUserId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_doesNotExist() throws Exception {
        reg.getUniqueUserId("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUniqueUserId(String)}.
     */
    @Test
    public void getUniqueUserId_exists() throws Exception {
        assertEquals(DEFAULT_ADMIN_USER, reg.getUniqueUserId(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUserSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_doesNotExist() throws Exception {
        reg.getUserSecurityName("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUserSecurityName(String)}.
     */
    @Test
    public void getUserSecurityName_exists() throws Exception {
        assertEquals(DEFAULT_ADMIN_USER, reg.getUserSecurityName(DEFAULT_ADMIN_USER));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#isValidGroup(String)}.
     */
    @Test
    public void isValidGroup() throws Exception {
        assertFalse(reg.isValidGroup("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getGroups_emptyList() throws Exception {
        SearchResult result = reg.getGroups("*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getGroups_negativeLimit() throws Exception {
        SearchResult result = reg.getGroups("*", -1);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_noResultBounded() throws Exception {
        SearchResult result = reg.getGroups("abc*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_noResultUnbounded() throws Exception {
        SearchResult result = reg.getGroups("abc*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultOverBounded() throws Exception {
        SearchResult result = reg.getGroups("group1*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultBounded() throws Exception {
        SearchResult result = reg.getGroups("group1*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultUnbounded() throws Exception {
        SearchResult result = reg.getGroups("group1*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_multipleResultUnderBounded() throws Exception {
        SearchResult result = reg.getGroups("group.*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_multipleResultBonded() throws Exception {
        SearchResult result = reg.getGroups("group.*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_allEntiresBounded() throws Exception {
        SearchResult result = reg.getGroups(".*", 3);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_allEntiresUnbounded() throws Exception {
        SearchResult result = reg.getGroups(".*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertTrue(result.getList().isEmpty());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroupDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_doesNotExist() throws Exception {
        reg.getGroupDisplayName("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUniqueGroupId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_doesNotExist() throws Exception {
        reg.getUniqueGroupId("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroupSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_doesNotExist() throws Exception {
        reg.getGroupSecurityName("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdsForUser_noSuchUser() throws Exception {
        reg.getUniqueGroupIdsForUser("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_userWithNoGroups() throws Exception {
        List<String> groups = reg.getUniqueGroupIdsForUser(DEFAULT_ADMIN_USER);
        assertNotNull(groups);
        assertEquals(0, groups.size());
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroupsForUser(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_noSuchUser() throws Exception {
        reg.getGroupsForUser("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.QuickStartSecurityRegistry#getGroupsForUser(String)}.
     */
    @Test
    public void getGroupsForUser_userWithNoGroups() throws Exception {
        List<String> groups = reg.getGroupsForUser(DEFAULT_ADMIN_USER);
        assertNotNull(groups);
        assertEquals(0, groups.size());
    }

}
