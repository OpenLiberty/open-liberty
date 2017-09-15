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
package com.ibm.ws.security.intfc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.NotImplementedException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.internal.UserRegistryWrapper;

import test.common.SharedOutputManager;

/**
 *
 */
public class UserRegistryWrapperTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String REALM = "name";
    private static final String NAME = "name";
    static final String PWD = "pwd";
    private static final X509Certificate CERT = null;

    private final Mockery mock = new JUnit4Mockery();
    private final com.ibm.ws.security.registry.UserRegistry wrappedUr = mock.mock(com.ibm.ws.security.registry.UserRegistry.class);
    private UserRegistryWrapper wrapper;

    @Before
    public void setUp() {
        wrapper = new UserRegistryWrapper(wrappedUr);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#initialize(java.util.Properties)}.
     */
    @Test
    public void initialize() throws Exception {
        mock.checking(new Expectations() {
            {
                never(wrappedUr);
            }
        });
        wrapper.initialize(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test
    public void checkPassword() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).checkPassword(NAME, PWD);
                will(returnValue(NAME));
            }
        });
        assertEquals("checkPassword did not return expected String",
                     NAME, wrapper.checkPassword(NAME, PWD));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = PasswordCheckFailedException.class)
    public void checkPassword_failed() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).checkPassword(NAME, PWD);
                will(returnValue(null));
            }
        });
        wrapper.checkPassword(NAME, PWD);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void checkPassword_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).checkPassword(NAME, PWD);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.checkPassword(NAME, PWD);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = PasswordCheckFailedException.class)
    public void checkPassword_IllegalArgumentException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).checkPassword(NAME, PWD);
                will(throwException(new IllegalArgumentException("expected")));
            }
        });
        wrapper.checkPassword(NAME, PWD);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate[])}.
     */
    @Test
    public void mapCertificate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).mapCertificate(CERT);
                will(returnValue(NAME));
            }
        });

        X509Certificate[] certs = new X509Certificate[] { CERT };
        assertEquals("checkPassword did not return expected String",
                     NAME, wrapper.mapCertificate(certs));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate[])}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_failed() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).mapCertificate(CERT);
                will(throwException(new com.ibm.ws.security.registry.CertificateMapFailedException("expected")));
            }
        });

        X509Certificate[] certs = new X509Certificate[] { CERT };
        wrapper.mapCertificate(certs);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate[])}.
     */
    @Test(expected = CustomRegistryException.class)
    public void mapCertificate_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).mapCertificate(CERT);
                will(throwException(new com.ibm.ws.security.registry.RegistryException("expected")));
            }
        });

        X509Certificate[] certs = new X509Certificate[] { CERT };
        wrapper.mapCertificate(certs);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#mapCertificate(java.security.cert.X509Certificate[])}.
     */
    @Test(expected = CertificateMapNotSupportedException.class)
    public void mapCertificate_CertificateMapNotSupportedException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).mapCertificate(CERT);
                will(throwException(new com.ibm.ws.security.registry.CertificateMapNotSupportedException("expected")));
            }
        });

        X509Certificate[] certs = new X509Certificate[] { CERT };
        wrapper.mapCertificate(certs);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getRealm()}.
     */
    @Test
    public void getRealm() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getRealm();
                will(returnValue(REALM));
            }
        });
        assertEquals("getRealm did not return expected String",
                     REALM, wrapper.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test
    public void getUsers_hasMoreTrue() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        final SearchResult lResult = new SearchResult(expectedList, true);

        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUsers("*", 0);
                will(returnValue(lResult));
            }
        });

        Result result = wrapper.getUsers("*", 0);
        assertTrue("hasMore should be true as it's true in SearchResult",
                   result.hasMore());
        assertSame("Did not get back the expected list from SearchResult",
                   expectedList, result.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test
    public void getUsers_hasMoreFalse() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        final SearchResult lResult = new SearchResult(expectedList, false);

        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUsers("*", 0);
                will(returnValue(lResult));
            }
        });

        Result result = wrapper.getUsers("*", 0);
        assertFalse("hasMore should be false as it's false in SearchResult",
                    result.hasMore());
        assertSame("Did not get back the expected list from SearchResult",
                   expectedList, result.getList());
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUsers(java.lang.String, int)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUsers_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUsers("*", 0);
                will(throwException(new RegistryException("expected")));
            }
        });

        wrapper.getUsers("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserDisplayName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getUserDisplayName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUserDisplayName_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserDisplayName(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getUserDisplayName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserDisplayName(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getUserDisplayName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueUserId(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getUniqueUserId(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUniqueUserId_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueUserId(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getUniqueUserId(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueUserId(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getUniqueUserId(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserSecurityName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getUserSecurityName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUserSecurityName_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserSecurityName(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getUserSecurityName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUserSecurityName(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getUserSecurityName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidUser(java.lang.String)}.
     */
    @Test
    public void isValidUser() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidUser(NAME);
                will(returnValue(true));
            }
        });
        assertTrue("Did not get back expected true",
                   wrapper.isValidUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidUser(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void isValidUser_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidUser(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.isValidUser(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidUser(java.lang.String)}.
     */
    @Test
    public void isValidUser_IllegalArgumentException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidUser(NAME);
                will(throwException(new IllegalArgumentException("expected")));
            }
        });
        assertFalse(wrapper.isValidUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.websphere.security.impl.GroupRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test
    public void getGroups_hasMoreTrue() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        final SearchResult lResult = new SearchResult(expectedList, true);

        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroups("*", 0);
                will(returnValue(lResult));
            }
        });

        Result result = wrapper.getGroups("*", 0);
        assertTrue("hasMore should be true as it's true in SearchResult",
                   result.hasMore());
        assertSame("Did not get back the expected list from SearchResult",
                   expectedList, result.getList());
    }

    /**
     * Test method for {@link com.ibm.websphere.security.impl.GroupRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test
    public void getGroups_hasMoreFalse() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        final SearchResult lResult = new SearchResult(expectedList, false);

        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroups("*", 0);
                will(returnValue(lResult));
            }
        });

        Result result = wrapper.getGroups("*", 0);
        assertFalse("hasMore should be false as it's false in SearchResult",
                    result.hasMore());
        assertSame("Did not get back the expected list from SearchResult",
                   expectedList, result.getList());
    }

    /**
     * Test method for {@link com.ibm.websphere.security.impl.GroupRegistryWrapper#getGroups(java.lang.String, int)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getGroups_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroups("*", 0);
                will(throwException(new RegistryException("expected")));
            }
        });

        wrapper.getGroups("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupDisplayName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getGroupDisplayName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getGroupDisplayName_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupDisplayName(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getGroupDisplayName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupDisplayName(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getGroupDisplayName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupId(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getUniqueGroupId(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUniqueGroupId_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupId(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getUniqueGroupId(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupId(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getUniqueGroupId(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupIds(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupIdsForUser(NAME);
                will(returnValue(expectedList));
            }
        });
        assertSame("Did not get back the expected list",
                   expectedList, wrapper.getUniqueGroupIds(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupIds(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getUniqueGroupIds_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupIdsForUser(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getUniqueGroupIds(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUniqueGroupIds(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIds_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUniqueGroupIdsForUser(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getUniqueGroupIds(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupSecurityName(NAME);
                will(returnValue(NAME));
            }
        });
        assertEquals("Expected name was not returned",
                     NAME, wrapper.getGroupSecurityName(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getGroupSecurityName_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupSecurityName(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getGroupSecurityName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupSecurityName(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getGroupSecurityName(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidGroup(java.lang.String)}.
     */
    @Test
    public void isValidGroup() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidGroup(NAME);
                will(returnValue(true));
            }
        });
        assertTrue("Did not get back expected true",
                   wrapper.isValidGroup(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidGroup(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void isValidGroup_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidGroup(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.isValidGroup(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#isValidGroup(java.lang.String)}.
     */
    @Test
    public void isValidGroup_IllegalArgumentException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).isValidGroup(NAME);
                will(throwException(new IllegalArgumentException("expected")));
            }
        });
        assertFalse(wrapper.isValidGroup(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        final List<String> expectedList = new ArrayList<String>();
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupsForUser(NAME);
                will(returnValue(expectedList));
            }
        });
        assertSame("Did not get back the expected list",
                   expectedList, wrapper.getGroupsForUser(NAME));
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = CustomRegistryException.class)
    public void getGroupsForUser_RegistryException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupsForUser(NAME);
                will(throwException(new RegistryException("expected")));
            }
        });
        wrapper.getGroupsForUser(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_EntryNotFoundException() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getGroupsForUser(NAME);
                will(throwException(new com.ibm.ws.security.registry.EntryNotFoundException("expected")));
            }
        });
        wrapper.getGroupsForUser(NAME);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#getUsersForGroup(java.lang.String, int)}.
     */
    @Test
    public void getUsersForGroup() throws Exception {
        mock.checking(new Expectations() {
            {
                one(wrappedUr).getUsersForGroup(NAME, 0);
            }
        });

        wrapper.getUsersForGroup(NAME, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.intfc.internal.UserRegistryWrapper#createCredential(java.lang.String)}.
     */
    @Test(expected = NotImplementedException.class)
    public void createCredential() throws Exception {
        // No implementation is provided, exception is expected
        wrapper.createCredential(NAME);
    }

}
