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
package com.ibm.ws.security.credentials.wscred;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;


import test.common.SharedOutputManager;

public class WSCredentialImplTest {

    private static SharedOutputManager outputMgr;
    private final String realmName = "BasicRealm";
    private final String securityName = "user1";
    private final String uniqueSecurityName = "user1";
    private final String primaryGroupId = "group1";
    private final String accessId = "user:BasicRealm:user1";
    private final List<String> roles = new ArrayList<String>();
    private final List<String> groupIds = new ArrayList<String>();

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructorWithRegistryData() {
        WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
        assertNotNull("There must be a credentials service.", credential);
    }

    @Test
    public void testConstructorWithNullGroups() {
        WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, null);
        assertNotNull("There must be a credentials service.", credential);
    }

    @Test
    public void testGetRealmName() {
        final String methodName = "testGetRealm";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String realmNameRetrieved = credential.getRealmName();
            assertEquals("The realm name retrieved must be equals to the realm name set.", realmName, realmNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSecurityName() {
        final String methodName = "testGetSecurityName";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String securityNameRetrieved = credential.getSecurityName();
            assertEquals("The security name retrieved must be equals to the security name set.", securityName, securityNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRealmSecurityName() {
        final String methodName = "testGetRealmSecurityName";
        try {
            String expectedRealmSecurityName = realmName + "/" + securityName;
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String realmSecurityNameRetrieved = credential.getRealmSecurityName();
            assertEquals("The realm security name retrieved must be equals to the realm prefixed to the security name.", expectedRealmSecurityName, realmSecurityNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetUniqueSecurityName() {
        final String methodName = "testGetUniqueSecurityName";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String uniqueSecurityNameRetrieved = credential.getUniqueSecurityName();
            assertEquals("The unique security name retrieved must be equals to the unique security name set.", uniqueSecurityName, uniqueSecurityNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetRealmUniqueSecurityName() {
        final String methodName = "testGetRealmUniqueSecurityName";
        try {
            String expectedRealmUniqueSecurityName = realmName + "/" + uniqueSecurityName;
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String realmUniqueSecurityNameRetrieved = credential.getRealmUniqueSecurityName();
            assertEquals("The realm unique security name retrieved must be equals to the realm prefixed to the unique security name.", expectedRealmUniqueSecurityName,
                         realmUniqueSecurityNameRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPrimaryGroupId() {
        final String methodName = "testGetPrimaryGroupId";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String primaryGroupIdRetrieved = credential.getPrimaryGroupId();
            assertEquals("The primary group id retrieved must be equals to the primary group id set.", primaryGroupId, primaryGroupIdRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAccessId() {
        final String methodName = "testGetAccessId";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String accessIdRetrieved = credential.getAccessId();
            assertEquals("The access id retrieved must be equals to the access id set.", accessId, accessIdRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetGroupIds() {
        final String methodName = "testGetGroupIds";
        try {
            groupIds.add("group1");
            groupIds.add("group2");
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            ArrayList<String> groupIdsRetrieved = credential.getGroupIds();
            assertEquals("The group ids retrieved must be equals to the group ids set.", groupIds, groupIdsRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetGroupIdsReturnsAClone() {
        final String methodName = "testGetGroupIdsReturnsAClone";
        try {
            groupIds.add("group1");
            groupIds.add("group2");
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            ArrayList<String> groupIdsRetrieved = credential.getGroupIds();
            assertTrue("The group ids retrieved must be a clone of the group ids set.", groupIds != groupIdsRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSet() {
        final String methodName = "testSet";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String key = "key1";
            Object value = "value1";
            String previousValue = (String) credential.set(key, value);
            assertNull("The value must not previously exist for a new credential.", previousValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGet() {
        final String methodName = "testGet";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String key = "key1";
            Object value = "value1";
            credential.set(key, value);
            String valueRetrieved = (String) credential.get(key);
            assertEquals("The value retrieved must be equals to the value set.", value, valueRetrieved);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsUnauthenticated() {
        final String methodName = "testIsUnauthenticated";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            boolean unauthenticated = credential.isUnauthenticated();
            assertFalse("The credential must not be unauthenticated.", unauthenticated);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsUnauthenticatedWithUNAUTHENTICATEDReturnsTrue() {
        final String methodName = "testIsUnauthenticatedWithUNAUTHENTICATEDReturnsTrue";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, "UNAUTHENTICATED", uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            boolean unauthenticated = credential.isUnauthenticated();
            assertTrue("The credential must be unauthenticated.", unauthenticated);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testBasicAuthCredential() {
        final String methodName = "testBasicAuthCredential";
        try {
            String password = "user1pwd";
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, password);
            byte[] credentialToken = credential.getCredentialToken();
            String securityNameRetrieved = credential.getSecurityName();

            assertTrue("The WSCredential must be of type basic auth.", credential.isBasicAuth());
            assertEquals("The security name retrieved must be equals to the security name set.", securityName, securityNameRetrieved);
            assertEquals("The credential token must be the password.", password, new String(credentialToken, "UTF-8"));
            assertTrue("The WSCredential must be forwardable.", credential.isForwardable());
            assertEquals("The credential OID must be GSSUP's OID.", "oid:2.23.130.1.1.1", credential.getOID());
            assertEquals("The credential expiration must not be set.", 0, credential.getExpiration());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testToString() {
        final String methodName = "testToString";
        try {
            WSCredentialImpl credential = new WSCredentialImpl(realmName, securityName, uniqueSecurityName, "UNAUTHENTICATED", primaryGroupId, accessId, roles, groupIds);
            String credentialToStringMessage = credential.toString();
            List<String> expectedFields = getExpectedToStringFields();
            boolean validMessage = true;
            for (String expectedField : expectedFields) {
                if (credentialToStringMessage.contains(expectedField) == false) {
                    validMessage = false;
                    break;
                }
            }
            assertTrue("The credential toString message must contain expected fields.", validMessage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private List<String> getExpectedToStringFields() {
        List<String> expectedFields = new ArrayList<String>();
        expectedFields.add("realmName");
        expectedFields.add("securityName");
        expectedFields.add("realmSecurityName");
        expectedFields.add("uniqueSecurityName");
        expectedFields.add("primaryGroupId");
        expectedFields.add("accessId");
        expectedFields.add("groupIds");
        return expectedFields;
    }
}
