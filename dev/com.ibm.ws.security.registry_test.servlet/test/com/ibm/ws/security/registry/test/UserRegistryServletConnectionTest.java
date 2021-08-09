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

package com.ibm.ws.security.registry.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;

public class UserRegistryServletConnectionTest {

    private class FakeUserRegistryServletConnection extends UserRegistryServletConnection {
        private String fakeResponse;
        private String expectedMethodName;

        public FakeUserRegistryServletConnection(String host, int port) {
            super(host, port);
        }

        void setExpectedMethodName(String methodName) {
            expectedMethodName = methodName;
        }

        void setFakeResponse(String response) {
            fakeResponse = response;
        }

        @Override
        protected String makeServletMethodCall(String methodName, String servletRequest) {
            if (!methodName.equals(expectedMethodName)) {
                throw new IllegalArgumentException("expected method name [" + expectedMethodName + "] but saw [" + methodName + "]");
            }
            return fakeResponse;
        }
    }

    private FakeUserRegistryServletConnection servlet;

    @Before
    public void setUp() {
        servlet = new FakeUserRegistryServletConnection("localhost", 80);
    }

    @After
    public void tearDown() {
        servlet = null;
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#UserRegistryServletConnection(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctorNullHost() {
        new UserRegistryServletConnection(null, 80);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#UserRegistryServletConnection(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctorZeroPort() {
        new UserRegistryServletConnection("localhost", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#makeServletMethodCallWithException(java.lang.String, java.lang.String)}.
     */
    @Test
    public void makeServletMethodCallWithException() {
        servlet.setExpectedMethodName("ignored");

        try {
            servlet.setFakeResponse("com.ibm.ws.security.registry.EntryNotFoundException: msg");
            servlet.makeServletMethodCallWithException("ignored", "ignored");
        } catch (RegistryException re) {
            fail("Should not have thrown an exception on this case");
        }

        try {
            servlet.setFakeResponse("com.ibm.ws.security.registry.RegistryException: msg");
            servlet.makeServletMethodCallWithException("ignored", "ignored");
            fail("Should have thrown a RegistryException");
        } catch (RegistryException re) {
            assertEquals("msg", re.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#makeServletMethodCallWithExceptions(java.lang.String, java.lang.String)}.
     */
    @Test
    public void makeServletMethodCallWithExceptions() {
        servlet.setExpectedMethodName("ignored");

        try {
            servlet.setFakeResponse("nonException response");
            servlet.makeServletMethodCallWithExceptions("ignored", "ignored");
        } catch (Exception e) {
            fail("Should not have thrown an exception on this case");
        }

        try {
            servlet.setFakeResponse("com.ibm.ws.security.registry.EntryNotFoundException: msg");
            servlet.makeServletMethodCallWithExceptions("ignored", "ignored");
        } catch (RegistryException re) {
            fail("Should not have thrown a RegistryException on this case");
        } catch (EntryNotFoundException enof) {
            assertEquals("msg", enof.getMessage());
        }

        try {
            servlet.setFakeResponse("com.ibm.ws.security.registry.RegistryException: msg");
            servlet.makeServletMethodCallWithExceptions("ignored", "ignored");
            fail("Should have thrown a RegistryException");
        } catch (RegistryException re) {
            assertEquals("msg", re.getMessage());
        } catch (EntryNotFoundException enof) {
            fail("Should not have thrown an EntryNotFoundException on this case");
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getRealm()}.
     */
    @Test
    public void getRealm() throws Exception {
        servlet.setExpectedMethodName("getRealm");

        servlet.setFakeResponse("MyRealm");
        assertEquals("MyRealm", servlet.getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void checkPasswordRegistryException() throws Exception {
        servlet.setExpectedMethodName("checkPassword");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.checkPassword("admin", "password");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test
    public void checkPasswordValidResponse() throws Exception {
        servlet.setExpectedMethodName("checkPassword");

        servlet.setFakeResponse("admin");
        assertEquals("admin", servlet.checkPassword("admin", "password"));

        servlet.setFakeResponse("null");
        assertNull(servlet.checkPassword("admin", "password"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapNotSupportedException.class)
    public void mapCertificate() throws Exception {
        servlet.setExpectedMethodName("mapCertificate");
        servlet.mapCertificate(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void isValidUserRegistryException() throws Exception {
        servlet.setExpectedMethodName("isValidUser");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.isValidUser("admin");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidUser(java.lang.String)}.
     */
    @Test
    public void isValidUserValidResponse() throws Exception {
        servlet.setExpectedMethodName("isValidUser");

        servlet.setFakeResponse("true");
        assertTrue(servlet.isValidUser("admin"));

        servlet.setFakeResponse("false");
        assertFalse(servlet.isValidUser("admin"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidUser(java.lang.String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void isValidUserErrorResponse() throws Exception {
        servlet.setExpectedMethodName("isValidUser");

        servlet.setFakeResponse("ERROR");
        servlet.isValidUser("admin");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUsers(java.lang.String, int)}.
     */
    @Test(expected = RegistryException.class)
    public void getUsersRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUsers");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getUsers("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUsers(java.lang.String, int)}.
     */
    @Test
    public void getUsersValidResponse() throws Exception {
        servlet.setExpectedMethodName("getUsers");

        SearchResult expected;
        SearchResult result;
        List<String> list;

        expected = new SearchResult();
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getUsers("*", 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());

        list = new ArrayList<String>();
        list.add("user1");
        expected = new SearchResult(list, true);
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getUsers("*", 1);
        assertTrue(result.hasMore());
        assertFalse(result.getList().isEmpty());
        assertTrue(result.getList().contains("user1"));

        list = new ArrayList<String>();
        list.add("user1");
        list.add("user2");
        expected = new SearchResult(list, false);
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getUsers("*", 0);
        assertFalse(result.hasMore());
        assertFalse(result.getList().isEmpty());
        assertTrue(result.getList().contains("user1"));
        assertTrue(result.getList().contains("user2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUsers(java.lang.String, int)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getUsersErrorResponse() throws Exception {
        servlet.setExpectedMethodName("getUsers");

        servlet.setFakeResponse("ERROR");
        servlet.getUsers("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayNameEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getUserDisplayName");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getUserDisplayName("user1");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getUserDisplayNameRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUserDisplayName");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getUserDisplayName("user1");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserDisplayName(java.lang.String)}.
     */
    @Test
    public void getUserDisplayName() throws Exception {
        servlet.setExpectedMethodName("getUserDisplayName");

        servlet.setFakeResponse("user1");
        assertEquals("user1", servlet.getUserDisplayName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserIdEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getUniqueUserId");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUniqueUserId("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getUniqueUserIdRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUniqueUserId");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUniqueUserId("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueUserId(java.lang.String)}.
     */
    @Test
    public void getUniqueUserId() throws Exception {
        servlet.setExpectedMethodName("getUniqueUserId");

        servlet.setFakeResponse("user1");
        assertEquals("user1", servlet.getUniqueUserId("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityNameEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getUserSecurityName");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUserSecurityName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getUserSecurityNameRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUserSecurityName");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUserSecurityName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUserSecurityName(java.lang.String)}.
     */
    @Test
    public void getUserSecurityName() throws Exception {
        servlet.setExpectedMethodName("getUserSecurityName");

        servlet.setFakeResponse("user1");
        assertEquals("user1", servlet.getUserSecurityName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidGroup(java.lang.String)}.
     */
    @Test
    public void isValidGroupValidResponse() throws Exception {
        servlet.setExpectedMethodName("isValidGroup");

        servlet.setFakeResponse("true");
        assertTrue(servlet.isValidGroup("admin"));

        servlet.setFakeResponse("false");
        assertFalse(servlet.isValidGroup("admin"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidGroup(java.lang.String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void isValidGroupErrorResponse() throws Exception {
        servlet.setExpectedMethodName("isValidGroup");

        servlet.setFakeResponse("ERROR");
        servlet.isValidGroup("admin");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#isValidGroup(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void isValidGroupRegistryException() throws Exception {
        servlet.setExpectedMethodName("isValidGroup");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.isValidGroup("admin");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroups(java.lang.String, int)}.
     */
    @Test
    public void getGroupsValidResponse() throws Exception {
        servlet.setExpectedMethodName("getGroups");

        SearchResult expected;
        SearchResult result;
        List<String> list;

        expected = new SearchResult();
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getGroups("*", 0);
        assertFalse(result.hasMore());
        assertTrue(result.getList().isEmpty());

        list = new ArrayList<String>();
        list.add("group1");
        expected = new SearchResult(list, true);
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getGroups("*", 1);
        assertTrue(result.hasMore());
        assertFalse(result.getList().isEmpty());
        assertTrue(result.getList().contains("group1"));

        list = new ArrayList<String>();
        list.add("group1");
        list.add("group2");
        expected = new SearchResult(list, false);
        servlet.setFakeResponse(convertFromSR(expected));
        result = servlet.getGroups("*", 0);
        assertFalse(result.hasMore());
        assertFalse(result.getList().isEmpty());
        assertTrue(result.getList().contains("group1"));
        assertTrue(result.getList().contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroups(java.lang.String, int)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getGroupsErrorResponse() throws Exception {
        servlet.setExpectedMethodName("getGroups");

        servlet.setFakeResponse("ERROR");
        servlet.getGroups("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroups(java.lang.String, int)}.
     */
    @Test(expected = RegistryException.class)
    public void getGroupsRegistryException() throws Exception {
        servlet.setExpectedMethodName("getGroups");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getGroups("*", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayNameEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getGroupDisplayName");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getGroupDisplayName("group1");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getGroupDisplayNameRegistryException() throws Exception {
        servlet.setExpectedMethodName("getGroupDisplayName");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        servlet.getGroupDisplayName("group1");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupDisplayName(java.lang.String)}.
     */
    @Test
    public void getGroupDisplayName() throws Exception {
        servlet.setExpectedMethodName("getGroupDisplayName");

        servlet.setFakeResponse("group1");
        assertEquals("group1", servlet.getGroupDisplayName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupId");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("group1", servlet.getUniqueGroupId("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getUniqueGroupIdRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupId");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("group1", servlet.getUniqueGroupId("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupId(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupId() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupId");

        servlet.setFakeResponse("group1");
        assertEquals("group1", servlet.getUniqueGroupId("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityNameEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getGroupSecurityName");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("group1", servlet.getGroupSecurityName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getGroupSecurityNameRegistryException() throws Exception {
        servlet.setExpectedMethodName("getGroupSecurityName");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("group1", servlet.getGroupSecurityName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupSecurityName(java.lang.String)}.
     */
    @Test
    public void getGroupSecurityName() throws Exception {
        servlet.setExpectedMethodName("getGroupSecurityName");

        servlet.setFakeResponse("group1");
        assertEquals("group1", servlet.getGroupSecurityName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdsForUserEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupIdsForUser");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUniqueGroupIdsForUser("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getUniqueGroupIdsForUserRegistryException() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupIdsForUser");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getUniqueGroupIdsForUser("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test
    public void getUniqueGroupIdsForUserValidResponse() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupIdsForUser");

        List<String> expected;
        List<String> result;

        expected = new ArrayList<String>();
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getUniqueGroupIdsForUser("user1");
        assertTrue(result.isEmpty());

        expected = new ArrayList<String>();
        expected.add("group1");
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getUniqueGroupIdsForUser("user1");
        assertFalse(result.isEmpty());
        assertTrue(result.contains("group1"));

        expected = new ArrayList<String>();
        expected.add("group1");
        expected.add("group2");
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getUniqueGroupIdsForUser("user1");
        assertFalse(result.isEmpty());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getUniqueGroupIdsForUserErrorResponse() throws Exception {
        servlet.setExpectedMethodName("getUniqueGroupIdsForUser");

        servlet.setFakeResponse("ERROR");
        servlet.getUniqueGroupIdsForUser("user1");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUserEntryNotFoundException() throws Exception {
        servlet.setExpectedMethodName("getGroupsForUser");

        EntryNotFoundException expected = new EntryNotFoundException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getGroupsForUser("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = RegistryException.class)
    public void getGroupsForUserRegistryException() throws Exception {
        servlet.setExpectedMethodName("getGroupsForUser");

        RegistryException expected = new RegistryException("expected");
        servlet.setFakeResponse(expected.toString());
        assertEquals("user1", servlet.getGroupsForUser("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupsForUser(java.lang.String)}.
     */
    @Test
    public void getGroupsForUserValidResponse() throws Exception {
        servlet.setExpectedMethodName("getGroupsForUser");

        List<String> expected;
        List<String> result;

        expected = new ArrayList<String>();
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getGroupsForUser("user1");
        assertTrue(result.isEmpty());

        expected = new ArrayList<String>();
        expected.add("group1");
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getGroupsForUser("user1");
        assertFalse(result.isEmpty());
        assertTrue(result.contains("group1"));

        expected = new ArrayList<String>();
        expected.add("group1");
        expected.add("group2");
        servlet.setFakeResponse(convertFromList(expected));
        result = servlet.getGroupsForUser("user1");
        assertFalse(result.isEmpty());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.test.UserRegistryServletConnection#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getGroupsForUserErrorResponse() throws Exception {
        servlet.setExpectedMethodName("getGroupsForUser");

        servlet.setFakeResponse("ERROR");
        servlet.getGroupsForUser("user1");
    }

    // Copied from UserRegistryServlet class.
    private static String convertFromList(List<?> results) {

        if (results.isEmpty()) {
            return results.toString();
        }

        /*
         * Something unlikely to occur in a DN, yet still readable. If this value changes
         * remember to update UserRegistryServletConnection#convertToList() as well.
         */
        final String delimiter = " :: ";

        StringBuffer sb = new StringBuffer();

        sb.append('[');
        for (int idx = 0; idx < results.size(); idx++) {
            sb.append(results.get(idx));
            if (idx < (results.size() - 1)) {
                sb.append(delimiter);
            }
        }
        sb.append(']');

        return sb.toString();
    }

    // Copied from UserRegistryServlet class.
    private static String convertFromSR(SearchResult results) {
        /*
         * Originated from SearchResult.toString(). We make DN safe string representation of the list
         * here instead of calling List.toString().
         */
        return "SearchResult hasMore=" + results.hasMore() + " " + convertFromList(results.getList());
    }
}
