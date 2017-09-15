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
import org.junit.Test;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;

/**
 *
 */
public class CustomUserRegistryWrapperTest {

    private static final String NAME = "name";
    private static final String PWD = "pwd";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "expected";
    private static final List<String> EXPECTED_LIST = new ArrayList<String>();

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final Result resultWithHasMoreTrue = new Result();
    private final Result resultWithHasMoreFalse = new Result();
    private final X509Certificate cert = mockery.mock(X509Certificate.class);
    private final X509Certificate[] certChain = new X509Certificate[] { cert };
    private final PasswordCheckFailedException pwdCheckFailedException = new PasswordCheckFailedException(EXPECTED_EXCEPTION_MESSAGE);
    private final CustomRegistryException customRegistryException = new CustomRegistryException(EXPECTED_EXCEPTION_MESSAGE);
    private final com.ibm.websphere.security.EntryNotFoundException entryNotFoundException = new com.ibm.websphere.security.EntryNotFoundException(EXPECTED_EXCEPTION_MESSAGE);
    private final com.ibm.websphere.security.UserRegistry customUserRegistry = mockery.mock(com.ibm.websphere.security.UserRegistry.class);
    private final CustomUserRegistryWrapper wrapper = new CustomUserRegistryWrapper(customUserRegistry);

    @Before
    public void setUp() {
        resultWithHasMoreTrue.setList(EXPECTED_LIST);
        resultWithHasMoreTrue.setHasMore();
        resultWithHasMoreFalse.setList(EXPECTED_LIST);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getRegistry()}.
     */
    @Test
    public void testGetRegistry() {
        assertEquals("The user registry must be the same as the custom user registry.", customUserRegistry, wrapper.getExternalUserRegistry());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testCheckPassword() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).checkPassword(NAME, PWD);
                will(returnValue(NAME));
            }
        });
        assertEquals("checkPassword did not return expected String.", NAME, wrapper.checkPassword(NAME, PWD));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    public void testCheckPassword_PasswordCheckFailedException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).checkPassword(NAME, PWD);
                will(throwException(pwdCheckFailedException));
            }
        });

        assertNull("The userSecurityName must be null for invalid user or password.", wrapper.checkPassword(NAME, PWD));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testCheckPassword_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).checkPassword(NAME, PWD);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.checkPassword(NAME, PWD);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test
    public void testGetGroupDisplayName() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupDisplayName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getGroupDisplayName did not return expected name.", NAME, wrapper.getGroupDisplayName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetGroupDisplayName_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupDisplayName(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getGroupDisplayName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetGroupDisplayName_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupDisplayName(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getGroupDisplayName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test
    public void testGetGroupSecurityName() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupSecurityName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getGroupSecurityName did not return expected name.", NAME, wrapper.getGroupSecurityName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetGroupSecurityName_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupSecurityName(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getGroupSecurityName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetGroupSecurityName_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupSecurityName(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getGroupSecurityName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test
    public void testGetGroups_hasMoreTrue() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroups("*", 0);
                will(returnValue(resultWithHasMoreTrue));
            }
        });
        SearchResult searchResult = wrapper.getGroups("*", 0);
        assertTrue("hasMore should be true as it's true in Result.", searchResult.hasMore());
        assertEquals("getGroups did not get back the expected list from Result.", EXPECTED_LIST, searchResult.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test
    public void testGetGroups_hasMoreFalse() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroups("*", 0);
                will(returnValue(resultWithHasMoreFalse));
            }
        });
        SearchResult searchResult = wrapper.getGroups("*", 0);
        assertFalse("hasMore should be false as it's false in Result.", searchResult.hasMore());
        assertEquals("getGroups did not get back the expected list from Result.", EXPECTED_LIST, searchResult.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetGroups_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroups("*", 0);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getGroups("*", 0);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void testGetGroupsForUser() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupsForUser(NAME);
                will(returnValue(EXPECTED_LIST));
            }
        });
        assertEquals("getGroupsForUser did not return expected list.", EXPECTED_LIST, wrapper.getGroupsForUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetGroupsForUser_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupsForUser(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getGroupsForUser(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetGroupsForUser_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getGroupsForUser(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getGroupsForUser(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getRealm()}.
     */
    @Test
    public void testGetRealm() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getRealm();
                will(returnValue(NAME));
            }
        });
        assertEquals("getRealm did not return expected realm.", NAME, wrapper.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getRealm()}.
     */
    @Test
    public void testGetRealmNullDefaultToCustomRealm() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getRealm();
                will(returnValue(null));
            }
        });
        assertEquals("getRealm did not return expected realm.", "customRealm", wrapper.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test
    public void testGetUniqueGroupId() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupId(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getUniqueGroupId did not return expected id.", NAME, wrapper.getUniqueGroupId(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetUniqueGroupId_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupId(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getUniqueGroupId(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUniqueGroupId_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupId(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUniqueGroupId(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void testGetUniqueGroupIdsForUser() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupIds(NAME);
                will(returnValue(EXPECTED_LIST));
            }
        });
        assertEquals("getUniqueGroupIdsForUser did not return expected list.", EXPECTED_LIST, wrapper.getUniqueGroupIdsForUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetUniqueGroupIdsForUser_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupIds(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getUniqueGroupIdsForUser(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUniqueGroupIdsForUser_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueGroupIds(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUniqueGroupIdsForUser(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test
    public void testGetUniqueUserId() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueUserId(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getUniqueUserId did not return expected id.", NAME, wrapper.getUniqueUserId(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetUniqueUserId_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueUserId(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getUniqueUserId(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUniqueUserId_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUniqueUserId(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUniqueUserId(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test
    public void testGetUserDisplayName() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserDisplayName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getUserDisplayName did not return expected name.", NAME, wrapper.getUserDisplayName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetUserDisplayName_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserDisplayName(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getUserDisplayName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUserDisplayName_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserDisplayName(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUserDisplayName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test
    public void testGetUserSecurityName() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserSecurityName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("getUserSecurityName did not return expected name.", NAME, wrapper.getUserSecurityName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void testGetUserSecurityName_EntryNotFoundException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserSecurityName(NAME);
                will(throwException(entryNotFoundException));
            }
        });

        try {
            wrapper.getUserSecurityName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(entryNotFoundException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUserSecurityName_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUserSecurityName(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUserSecurityName(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test
    public void testGetUsers_hasMoreTrue() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUsers("*", 0);
                will(returnValue(resultWithHasMoreTrue));
            }
        });
        SearchResult searchResult = wrapper.getUsers("*", 0);
        assertTrue("hasMore should be true as it's true in Result.", searchResult.hasMore());
        assertEquals("getUsers did not get back the expected list from Result.", EXPECTED_LIST, searchResult.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test
    public void testGetUsers_hasMoreFalse() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUsers("*", 0);
                will(returnValue(resultWithHasMoreFalse));
            }
        });
        SearchResult searchResult = wrapper.getUsers("*", 0);
        assertFalse("hasMore should be false as it's false in Result.", searchResult.hasMore());
        assertEquals("getUsers did not get back the expected list from Result.", EXPECTED_LIST, searchResult.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test(expected = RegistryException.class)
    public void testGetUsers_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).getUsers("*", 0);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.getUsers("*", 0);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#isValidGroup(java.lang.String)}.
     */
    @Test
    public void testIsValidGroup() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).isValidGroup(NAME);
                will(returnValue(true));
            }
        });
        assertTrue("isValidGroup did not return expected true.", wrapper.isValidGroup(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#isValidGroup(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testIsValidGroup_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).isValidGroup(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.isValidGroup(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#isValidUser(java.lang.String)}.
     */
    @Test
    public void testIsValidUser() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).isValidUser(NAME);
                will(returnValue(true));
            }
        });
        assertTrue("isValidUser did not return expected true.", wrapper.isValidUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#isValidUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void testIsValidUser_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).isValidUser(NAME);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.isValidUser(NAME);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test
    public void testMapCertificate() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).mapCertificate(certChain);
                will(returnValue(NAME));
            }
        });
        assertEquals("mapCertificate did not return expected name.", NAME, wrapper.mapCertificate(cert));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapNotSupportedException.class)
    public void testMapCertificate_CertificateMapNotSupportedException() throws Exception {
        final com.ibm.websphere.security.CertificateMapNotSupportedException certMapNotSupportedException = new com.ibm.websphere.security.CertificateMapNotSupportedException(EXPECTED_EXCEPTION_MESSAGE);
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).mapCertificate(certChain);
                will(throwException(certMapNotSupportedException));
            }
        });

        try {
            wrapper.mapCertificate(cert);
        } catch (Exception e) {
            assertExceptionMessage(e);
            throw e;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void testMapCertificate_CertificateMapFailedException() throws Exception {
        final com.ibm.websphere.security.CertificateMapFailedException certMapFailedException = new com.ibm.websphere.security.CertificateMapFailedException(EXPECTED_EXCEPTION_MESSAGE);
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).mapCertificate(certChain);
                will(throwException(certMapFailedException));
            }
        });

        try {
            wrapper.mapCertificate(cert);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(certMapFailedException, e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.CustomUserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = RegistryException.class)
    public void testMapCertificate_CustomRegistryException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(customUserRegistry).mapCertificate(certChain);
                will(throwException(customRegistryException));
            }
        });

        try {
            wrapper.mapCertificate(cert);
        } catch (Exception e) {
            assertExceptionContentsAndRethrow(customRegistryException, e);
        }
    }

    private void assertExceptionContentsAndRethrow(final Exception originalException, Exception thrownException) throws Exception {
        assertExceptionMessage(thrownException);
        assertEquals("The exception's cause must be set.", originalException, thrownException.getCause());
        throw thrownException;
    }

    private void assertExceptionMessage(Exception e) {
        assertEquals("The exception's message must be set.", EXPECTED_EXCEPTION_MESSAGE, e.getMessage());
    }

}
