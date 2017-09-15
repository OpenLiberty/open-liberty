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
package com.ibm.ws.security.registry.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 *
 */
public class UserRegistryProxyTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String realm = "proxyRealm";
    final String userSecurityName = "user";
    final String password = "pwd";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final UserRegistry delegate1 = mock.mock(UserRegistry.class, "delegate1");
    private final UserRegistry delegate2 = mock.mock(UserRegistry.class, "delegate2");
    private UserRegistry proxy;

    @Before
    public void setUp() {
        List<UserRegistry> delegates = new ArrayList<UserRegistry>();
        delegates.add(delegate1);
        delegates.add(delegate2);
        proxy = new UserRegistryProxy(realm, delegates);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();

        proxy = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_emptyList() {
        new UserRegistryProxy(realm, new ArrayList<UserRegistry>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctor_oneElementList() {
        List<UserRegistry> delegates = new ArrayList<UserRegistry>();
        delegates.add(delegate1);
        new UserRegistryProxy(realm, delegates);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getRealm()}.
     * 
     * Shall return the realm name specified to the constructor.
     */
    @Test
    public void getRealm() {
        assertEquals("Should be the realm name specified to the constructor",
                     realm, proxy.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#checkPassword(java.lang.String, java.lang.String)}.
     * 
     * checkPassword shall return false if unable to validate username / password against
     * any delegate.
     */
    @Test
    public void checkPassword_inNeitherDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).checkPassword(userSecurityName, password);
                will(returnValue(null));
                allowing(delegate2).checkPassword(userSecurityName, password);
                will(returnValue(null));
            }
        });
        assertNull("If not defined in any delegate, return null",
                    proxy.checkPassword(userSecurityName, password));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#checkPassword(java.lang.String, java.lang.String)}.
     * 
     * checkPassword shall return true if unable to validate username / password against
     * any delegate.
     */
    @Test
    public void checkPassword_inOneDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).checkPassword(userSecurityName, password);
                will(returnValue(null));
                allowing(delegate2).checkPassword(userSecurityName, password);
                will(returnValue(userSecurityName));
            }
        });
        assertEquals("If defined in at least one delegate, return the name",
                     userSecurityName, proxy.checkPassword(userSecurityName, password));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#mapCertificate(java.security.cert.X509Certificate)}.
     * 
     * If no such mapping exists in any delegate, return a CertificateMapFailedException.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_inNeitherDelegate() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        mock.checking(new Expectations() {
            {
                allowing(delegate1).mapCertificate(cert);
                will(throwException(new CertificateMapFailedException("No such mapping")));
                allowing(delegate2).mapCertificate(cert);
                will(throwException(new CertificateMapFailedException("No such mapping")));
            }
        });

        proxy.mapCertificate(cert);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#mapCertificate(java.security.cert.X509Certificate)}.
     * 
     * If the delegates do not support a mapCertificate, return a CertificateMapFailedException.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_notSupported() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        mock.checking(new Expectations() {
            {
                allowing(delegate1).mapCertificate(cert);
                will(throwException(new CertificateMapNotSupportedException("Not supported")));
                allowing(delegate2).mapCertificate(cert);
                will(throwException(new CertificateMapNotSupportedException("Not supported")));
            }
        });

        proxy.mapCertificate(cert);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#mapCertificate(java.security.cert.X509Certificate)}.
     * 
     * If any delegate supports such a mapping, return the mapped name.
     */
    @Test
    public void mapCertificate_inOneDelegate() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        mock.checking(new Expectations() {
            {
                allowing(delegate1).mapCertificate(cert);
                will(throwException(new CertificateMapFailedException("No such mapping")));
                allowing(delegate2).mapCertificate(cert);
                will(returnValue(userSecurityName));
            }
        });

        assertEquals("If any delegate can handle the mapping, return the mapped user name",
                     userSecurityName, proxy.mapCertificate(cert));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#isValidUser(java.lang.String)}.
     * 
     * If the specified name is not valid in any delegate, return false.
     */
    @Test
    public void isValidUser_inNeitherDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).isValidUser(userSecurityName);
                will(returnValue(false));
                allowing(delegate2).isValidUser(userSecurityName);
                will(returnValue(false));
            }
        });
        assertFalse("If the specified name is not valid in any delegate, return false.",
                    proxy.isValidUser(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#isValidUser(java.lang.String)}.
     * 
     * If the specified name is valid in at least one delegate, return true.
     */
    @Test
    public void isValidUser_inOneDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).isValidUser(userSecurityName);
                will(returnValue(true));
                allowing(delegate2).isValidUser(userSecurityName);
                will(returnValue(false));
            }
        });
        assertTrue("If the specified name is valid in at least one delegate, return true.",
                   proxy.isValidUser(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUsers(java.lang.String, int)}.
     * 
     * If no matches are found, return an empty SearchResult.
     */
    @Test
    public void getUsers_noMatchesInDelegates() throws Exception {
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUsers(pattern, limit);
                will(returnValue(new SearchResult()));
                allowing(delegate2).getUsers(pattern, limit);
                will(returnValue(new SearchResult()));
            }
        });
        SearchResult result = proxy.getUsers(pattern, limit);
        assertFalse("Should not have more", result.hasMore());
        assertEquals("Should be an empty list", 0, result.getList().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUsers(java.lang.String, int)}.
     * 
     * If matches are found, return an merged SearchResult.
     */
    @Test
    public void getUsers_matchesInOneDelegate() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateUser1");
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUsers(pattern, limit);
                will(returnValue(new SearchResult(delegate1List, false)));
                allowing(delegate2).getUsers(pattern, limit);
                will(returnValue(new SearchResult()));
            }
        });
        SearchResult result = proxy.getUsers(pattern, limit);
        assertFalse("Should not have more", result.hasMore());
        assertEquals("Should be a list with 1 element", 1, result.getList().size());
        assertTrue("Should have delegateUser1", result.getList().contains("delegateUser1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUsers(java.lang.String, int)}.
     * 
     * If matches are found, return an merged SearchResult.
     */
    @Test
    public void getUsers_matchesInBothDelegate() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateUser1");
        final List<String> delegate2List = new ArrayList<String>();
        delegate1List.add("delegateUser2");
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUsers(pattern, limit);
                will(returnValue(new SearchResult(delegate1List, false)));
                allowing(delegate2).getUsers(pattern, limit);
                will(returnValue(new SearchResult(delegate2List, true)));
            }
        });
        SearchResult result = proxy.getUsers(pattern, limit);
        assertTrue("Should have more", result.hasMore());
        assertEquals("Should be a list with 2 elements", 2, result.getList().size());
        assertTrue("Should have delegateUser1", result.getList().contains("delegateUser1"));
        assertTrue("Should have delegateUser2", result.getList().contains("delegateUser2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUserDisplayName(java.lang.String)}.
     * 
     * If the specified name is not defined in any delegate, throw an EntryNotFoundException.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_inNeitherDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUserDisplayName(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUserDisplayName(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getUserDisplayName(userSecurityName);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUserDisplayName(java.lang.String)}.
     * 
     * If the specified name is defined in at least one delegate, return the name.
     */
    @Test
    public void getUserDisplayName_inOneDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUserDisplayName(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUserDisplayName(userSecurityName);
                will(returnValue(userSecurityName));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     userSecurityName, proxy.getUserDisplayName(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueUserId(java.lang.String)}.
     * 
     * If the specified name is not defined in any delegate, throw an EntryNotFoundException.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_inNeitherDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueUserId(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUniqueUserId(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getUniqueUserId(userSecurityName);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueUserId(java.lang.String)}.
     * 
     * If the specified name is defined in at least one delegate, return the name.
     */
    @Test
    public void getUniqueUserId_inOneDelegate() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueUserId(userSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUniqueUserId(userSecurityName);
                will(returnValue(userSecurityName));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     userSecurityName, proxy.getUniqueUserId(userSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_inNeitherDelegate() throws Exception {
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUserSecurityName(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUserSecurityName(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getUserSecurityName(uniqueUserId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUserSecurityName(java.lang.String)}.
     */
    @Test
    public void getUserSecurityName_inOneDelegate() throws Exception {
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUserSecurityName(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUserSecurityName(uniqueUserId);
                will(returnValue(uniqueUserId));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     uniqueUserId, proxy.getUserSecurityName(uniqueUserId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#isValidGroup(java.lang.String)}.
     * 
     * If the specified name is not valid in any delegate, return false.
     */
    @Test
    public void isValidGroup_inNeitherDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).isValidGroup(groupSecurityName);
                will(returnValue(false));
                allowing(delegate2).isValidGroup(groupSecurityName);
                will(returnValue(false));
            }
        });
        assertFalse("If the specified name is not valid in any delegate, return false.",
                    proxy.isValidGroup(groupSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#isValidGroup(java.lang.String)}.
     * 
     * If the specified name is valid in at least one delegate, return true.
     */
    @Test
    public void isValidGroup_inOneDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).isValidGroup(groupSecurityName);
                will(returnValue(false));
                allowing(delegate2).isValidGroup(groupSecurityName);
                will(returnValue(true));
            }
        });
        assertTrue("If the specified name is valid in at least one delegate, return true.",
                    proxy.isValidGroup(groupSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroups(java.lang.String, int)}.
     * 
     * If no matches are found, return an empty SearchResult.
     */
    @Test
    public void getGroups_noMatchesInDelegates() throws Exception {
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroups(pattern, limit);
                will(returnValue(new SearchResult()));
                allowing(delegate2).getGroups(pattern, limit);
                will(returnValue(new SearchResult()));
            }
        });
        SearchResult result = proxy.getGroups(pattern, limit);
        assertFalse("Should not have more", result.hasMore());
        assertEquals("Should be an empty list", 0, result.getList().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroups(java.lang.String, int)}.
     * 
     * If matches are found, return an merged SearchResult.
     */
    @Test
    public void getGroups_matchesInOneDelegate() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroups(pattern, limit);
                will(returnValue(new SearchResult(delegate1List, false)));
                allowing(delegate2).getGroups(pattern, limit);
                will(returnValue(new SearchResult()));
            }
        });
        SearchResult result = proxy.getGroups(pattern, limit);
        assertFalse("Should not have more", result.hasMore());
        assertEquals("Should be a list with 1 element", 1, result.getList().size());
        assertTrue("Should have delegateGroup1", result.getList().contains("delegateGroup1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroups(java.lang.String, int)}.
     * 
     * If matches are found, return an merged SearchResult.
     */
    @Test
    public void getGroups_matchesInBothDelegate() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final List<String> delegate2List = new ArrayList<String>();
        delegate2List.add("delegateGroup2");
        final String pattern = "*";
        final int limit = 1;
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroups(pattern, limit);
                will(returnValue(new SearchResult(delegate1List, false)));
                allowing(delegate2).getGroups(pattern, limit);
                will(returnValue(new SearchResult(delegate2List, true)));
            }
        });
        SearchResult result = proxy.getGroups(pattern, limit);
        assertTrue("Should have more", result.hasMore());
        assertEquals("Should be a list with 2 elements", 2, result.getList().size());
        assertTrue("Should have delegateGroup1", result.getList().contains("delegateGroup1"));
        assertTrue("Should have delegateGroup2", result.getList().contains("delegateGroup2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupDisplayName(java.lang.String)}.
     * 
     * If the specified name is not defined in any delegate, throw an EntryNotFoundException.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_inNeitherDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupDisplayName(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getGroupDisplayName(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getGroupDisplayName(groupSecurityName);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupDisplayName(java.lang.String)}.
     * 
     * If the specified name is defined in at least one delegate, return the name.
     */
    @Test
    public void getGroupDisplayName_inOneDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupDisplayName(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getGroupDisplayName(groupSecurityName);
                will(returnValue(groupSecurityName));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     groupSecurityName, proxy.getGroupDisplayName(groupSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupId(java.lang.String)}.
     * 
     * If the specified name is not defined in any delegate, throw an EntryNotFoundException.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_inNeitherDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupId(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUniqueGroupId(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getUniqueGroupId(groupSecurityName);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupId(java.lang.String)}.
     * 
     * If the specified name is defined in at least one delegate, return the name.
     */
    @Test
    public void getUniqueGroupId_inOneDelegate() throws Exception {
        final String groupSecurityName = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupId(groupSecurityName);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUniqueGroupId(groupSecurityName);
                will(returnValue(groupSecurityName));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     groupSecurityName, proxy.getUniqueGroupId(groupSecurityName));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupSecurityName(java.lang.String)}.
     * 
     * If the specified name is not defined in any delegate, throw an EntryNotFoundException.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_inNeitherDelegate() throws Exception {
        final String uniqueGroupId = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupSecurityName(uniqueGroupId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getGroupSecurityName(uniqueGroupId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getGroupSecurityName(uniqueGroupId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupSecurityName(java.lang.String)}.
     * 
     * If the specified name is defined in at least one delegate, return the name.
     */
    @Test
    public void getGroupSecurityName_inOneDelegate() throws Exception {
        final String uniqueGroupId = "group";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupSecurityName(uniqueGroupId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getGroupSecurityName(uniqueGroupId);
                will(returnValue(uniqueGroupId));
            }
        });
        assertEquals("If the specified name is defined in at least one delegate, return the name.",
                     uniqueGroupId, proxy.getGroupSecurityName(uniqueGroupId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdsForUser_noMatchesInDelegates() throws Exception {
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupIdsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getUniqueGroupIdsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getUniqueGroupIdsForUser(uniqueUserId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_noGroupsInDelegates() throws Exception {
        final List<String> emptyList = new ArrayList<String>();
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(emptyList));
                allowing(delegate2).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(emptyList));
            }
        });
        List<String> result = proxy.getUniqueGroupIdsForUser(uniqueUserId);
        assertEquals("Should be an empty list", 0, result.size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_missingInOneDelegates() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getUniqueGroupIdsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        List<String> result = proxy.getUniqueGroupIdsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_matchesInOneDelegates() throws Exception {
        final List<String> emptyList = new ArrayList<String>();
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(emptyList));
            }
        });
        List<String> result = proxy.getUniqueGroupIdsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_matchesInBothDelegates() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final List<String> delegate2List = new ArrayList<String>();
        delegate2List.add("delegateGroup2");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getUniqueGroupIdsForUser(uniqueUserId);
                will(returnValue(delegate2List));
            }
        });
        List<String> result = proxy.getUniqueGroupIdsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
        assertTrue("Should have delegateGroup2", result.contains("delegateGroup2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_noMatchesInDelegates() throws Exception {
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
                allowing(delegate2).getGroupsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        proxy.getGroupsForUser(uniqueUserId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUser_noGroupsInDelegates() throws Exception {
        final List<String> emptyList = new ArrayList<String>();
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupsForUser(uniqueUserId);
                will(returnValue(emptyList));
                allowing(delegate2).getGroupsForUser(uniqueUserId);
                will(returnValue(emptyList));
            }
        });
        List<String> result = proxy.getGroupsForUser(uniqueUserId);
        assertEquals("Should be an empty list", 0, result.size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUser_missingInOneDelegates() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getGroupsForUser(uniqueUserId);
                will(throwException(new EntryNotFoundException("Expected")));
            }
        });
        List<String> result = proxy.getGroupsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUser_matchesInOneDelegates() throws Exception {
        final List<String> emptyList = new ArrayList<String>();
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getGroupsForUser(uniqueUserId);
                will(returnValue(emptyList));
            }
        });
        List<String> result = proxy.getGroupsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryProxy#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUser_matchesInBothDelegates() throws Exception {
        final List<String> delegate1List = new ArrayList<String>();
        delegate1List.add("delegateGroup1");
        final List<String> delegate2List = new ArrayList<String>();
        delegate2List.add("delegateGroup2");
        final String uniqueUserId = "user";
        mock.checking(new Expectations() {
            {
                allowing(delegate1).getGroupsForUser(uniqueUserId);
                will(returnValue(delegate1List));
                allowing(delegate2).getGroupsForUser(uniqueUserId);
                will(returnValue(delegate2List));
            }
        });
        List<String> result = proxy.getGroupsForUser(uniqueUserId);
        assertTrue("Should have delegateGroup1", result.contains("delegateGroup1"));
        assertTrue("Should have delegateGroup2", result.contains("delegateGroup2"));
    }

}
