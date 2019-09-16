/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.ws.security.internal.SecurityServiceImpl;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 * Drives most test cases with two inputs: hard-coded and constructed.
 * This verifies all possible permutations of input are covered.
 */
@SuppressWarnings("unchecked")
public class AccessIdUtilTest {
    protected final Mockery mock = new JUnit4Mockery();
    protected final ComponentContext cc = mock.mock(ComponentContext.class);
    protected final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class);
    protected final SecurityService securityService = mock.mock(SecurityService.class);
    protected final UserRegistryService urService = mock.mock(UserRegistryService.class);
    protected final UserRegistry ur = mock.mock(UserRegistry.class);
    protected final String KEY_SECURITY_SERVICE = "securityService";

    protected String defaultRealm;
    protected String[] defaultRealms;
    protected AccessIdUtil accessIdUtil;

    private final String mismatchedRealm = "SomeOtherRealm";
    private final String ALL_SPECIAL_CHARS = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
    private final String COMPLEX_USER = "https://user.test-realm.com:9876/" + ALL_SPECIAL_CHARS + "/end/";
    private final String COMPLEX_USER_X509 = "CN=user,OU=ibm.com/attribute=testAttribute,C=US,O=User Name";

    @Before
    public void setUp() throws CustomRegistryException, RemoteException, RegistryException {
        setRealm();
        defaultRealms = new String[] { defaultRealm };

        mock.checking(new Expectations() {
            {

                allowing(securityServiceRef).getProperty(SecurityServiceImpl.KEY_USERREGISTRY);
                will(returnValue(defaultRealms));

            }
        });
        accessIdUtil = new AccessIdUtil();
        accessIdUtil.setSecurityService(securityServiceRef);
    }

    void setRealm() {
        defaultRealm = "BasicRealm";
    }

    @After
    public void tearDown() {
        accessIdUtil.unsetSecurityService(securityServiceRef);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_allNull() {
        AccessIdUtil.createAccessId(null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_allEmpty() {
        AccessIdUtil.createAccessId("", "", "");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_typeNull() {
        AccessIdUtil.createAccessId(null, defaultRealm, "uniqueId");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_typeEmpty() {
        AccessIdUtil.createAccessId("", defaultRealm, "uniqueId");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_realmNull() {
        AccessIdUtil.createAccessId("type", null, "uniqueId");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_realmEmpty() {
        AccessIdUtil.createAccessId("type", "", "uniqueId");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_uniqueIdNull() {
        AccessIdUtil.createAccessId("type", defaultRealm, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_uniqueIdEmpty() {
        AccessIdUtil.createAccessId("type", defaultRealm, "");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test(expected = IllegalArgumentException.class)
    public void createAccessId_onlyUniqueId() {
        AccessIdUtil.createAccessId(null, null, "uniqueId");
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test
    public void createAccessId_allFieldsValid() {
        String accessId = AccessIdUtil.createAccessId("type", defaultRealm, "uniqueId");
        assertEquals("All fields are required for an accessId to be valid",
                     "type:" + defaultRealm + "/uniqueId", accessId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test
    public void createAccessId_allFieldsValid_complexUser() {
        String accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER);
        assertEquals("All fields are required for an accessId to be valid",
                     "type:" + defaultRealm + "/" + COMPLEX_USER, accessId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#createAccessId(java.lang.String, java.lang.String, java.lang.String)} .
     */
    @Test
    public void createAccessId_allFieldsValid_complexUserX509() {
        String accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER_X509);
        assertEquals("All fields are required for an accessId to be valid",
                     "type:" + defaultRealm + "/" + COMPLEX_USER_X509, accessId);
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_null() {
        assertNull("A null input should return null",
                   AccessIdUtil.getEntityType(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_empty() {
        assertNull("An empty input should return null",
                   AccessIdUtil.getEntityType(""));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_simpleString() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType("simpleString"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_onlyType() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType("type:"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_onlyRealm() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType(defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_onlyUniqueId() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType("uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_notSet() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType(defaultRealm + "/uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_partialAccessIdWithUniqueId() {
        String accessId = "type:uniqueId";
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_partialAccessIdWithRealm() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getEntityType("type:" + defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_fullAccessId() {
        String accessId = "type:" + defaultRealm + "/uniqueId";
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, "uniqueId");
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_fullAccessId_complexUser() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER;
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER);
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_fullAccessId_complexUserX509() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER_X509;
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER_X509);
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getEntityType(java.lang.String)}.
     */
    @Test
    public void getEntityType_mismatchedRealm() {
        String accessId = "type:" + mismatchedRealm + "/uniqueId";
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));

        accessId = AccessIdUtil.createAccessId("type", mismatchedRealm, "uniqueId");
        assertEquals("Should return 'type' when defined",
                     "type", AccessIdUtil.getEntityType(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_null() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_empty() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm(""));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_simpleString() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm("simpleString"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_onlyType() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm("type:"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_onlyRealm() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm(defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_onlyUniqueId() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm("uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_notSet() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm("type:uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_partialAccessIdWithType() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm("type:" + defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_partialAccessIdWithUniqueId() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getRealm(defaultRealm + "/uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_fullAccessId() {
        String accessId = "type:" + defaultRealm + "/uniqueId";
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, "uniqueId");
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_fullAccessId_complexUser() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER;
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER);
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_fullAccessId_complexUserX509() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER_X509;
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER_X509);
        assertEquals("Should return '" + defaultRealm + "' when defined",
                     defaultRealm, AccessIdUtil.getRealm(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getRealm(java.lang.String)}.
     */
    @Test
    public void getRealm_mismatchedRealm() {
        String accessId = "type:" + mismatchedRealm + "/uniqueId";
        assertEquals("Should return '" + mismatchedRealm + "' when defined",
                     mismatchedRealm, AccessIdUtil.getRealm(accessId));

        accessId = AccessIdUtil.createAccessId("type", mismatchedRealm, "uniqueId");
        assertEquals("Should return '" + mismatchedRealm + "' when defined",
                     mismatchedRealm, AccessIdUtil.getRealm(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_null() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_empty() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId(""));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_simpleString() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId("simpleString"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_onlyType() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId("type:"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_onlyRealm() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId(defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_onlyUniqueId() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId("uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_notSet() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId("type:" + defaultRealm + "/"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_partialAccessIdWithRealm() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId(defaultRealm + "/uniqueId"));

    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_partialAccessIdWithType() {
        assertNull("An incomplete accessId will return null",
                   AccessIdUtil.getUniqueId("type:uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_fullAccessId() {
        String accessId = "type:" + defaultRealm + "/uniqueId";
        assertEquals("Return the uniqueId when the accessId is valid",
                     "uniqueId", AccessIdUtil.getUniqueId(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, "uniqueId");
        assertEquals("Return the uniqueId when the accessId is valid",
                     "uniqueId", AccessIdUtil.getUniqueId(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_fullAccessId_complexUser() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER;
        assertEquals("Return the uniqueId when the accessId is valid",
                     COMPLEX_USER, AccessIdUtil.getUniqueId(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER);
        assertEquals("Return the uniqueId when the accessId is valid",
                     COMPLEX_USER, AccessIdUtil.getUniqueId(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_fullAccessId_complexUserX509() {
        String accessId = "type:" + defaultRealm + "/" + COMPLEX_USER_X509;
        assertEquals("Return the uniqueId when the accessId is valid",
                     COMPLEX_USER_X509, AccessIdUtil.getUniqueId(accessId));

        accessId = AccessIdUtil.createAccessId("type", defaultRealm, COMPLEX_USER_X509);
        assertEquals("Return the uniqueId when the accessId is valid",
                     COMPLEX_USER_X509, AccessIdUtil.getUniqueId(accessId));
    }

    /**
     * Test method for {@link com.ibm.ws.security.AccessIdUtil#getUniqueId(java.lang.String)}.
     */
    @Test
    public void getUniqueId_mismatchedRealm() {
        String accessId = "type:" + mismatchedRealm + "/uniqueId";
        assertEquals("Return the uniqueId when the accessId is valid",
                     "uniqueId", AccessIdUtil.getUniqueId(accessId));

        accessId = AccessIdUtil.createAccessId("type", mismatchedRealm, "uniqueId");
        assertEquals("Return the uniqueId when the accessId is valid",
                     "uniqueId", AccessIdUtil.getUniqueId(accessId));
    }

    @Test
    public void isAccessId_incompleteUniqueId() {
        assertFalse(AccessIdUtil.isAccessId("user:" + defaultRealm + "/"));
    }

    @Test
    public void isAccessId_incompleteRealm() {
        assertFalse(AccessIdUtil.isAccessId("user:/uniqueId"));
    }

    @Test
    public void isAccessId_incompleteType() {
        assertFalse(AccessIdUtil.isAccessId(":" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isAccessId_badPattern() {
        assertFalse(AccessIdUtil.isAccessId("type/" + defaultRealm + ":uniqueId"));
    }

    @Test
    public void isAccessId_complete() {
        assertTrue(AccessIdUtil.isAccessId("user:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isAccessId_complete_complexUser() {
        assertTrue(AccessIdUtil.isAccessId("user:" + defaultRealm + "/" + COMPLEX_USER));
    }

    @Test
    public void isAccessId_complete_complexUserX509() {
        assertTrue(AccessIdUtil.isAccessId("user:" + defaultRealm + "/" + COMPLEX_USER_X509));
    }

    @Test
    public void isAccessId_complete_mismatchedRealm() {
        assertTrue(AccessIdUtil.isAccessId("user:" + mismatchedRealm + "/uniqueId"));
    }

    @Test
    public void isServerAccessId_notValid() {
        assertFalse(AccessIdUtil.isServerAccessId("notValid"));
    }

    @Test
    public void isServerAccessId_false() {
        assertFalse(AccessIdUtil.isServerAccessId("group:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isSeverAccessId_true() {
        assertTrue(AccessIdUtil.isServerAccessId("server:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isSeverAccessId_true_complexUser() {
        assertTrue(AccessIdUtil.isServerAccessId("server:" + defaultRealm + "/" + COMPLEX_USER));
    }

    @Test
    public void isSeverAccessId_true_complexUserX509() {
        assertTrue(AccessIdUtil.isServerAccessId("server:" + defaultRealm + "/" + COMPLEX_USER_X509));
    }

    @Test
    public void isSeverAccessId_true_mismatchedRealm() {
        assertTrue(AccessIdUtil.isServerAccessId("server:" + mismatchedRealm + "/" + COMPLEX_USER_X509));
    }

    @Test
    public void isUserAccessId_notValid() {
        assertFalse(AccessIdUtil.isUserAccessId("notValid"));
    }

    @Test
    public void isUserAccessId_false() {
        assertFalse(AccessIdUtil.isUserAccessId("group:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isUserAccessId_true() {
        assertTrue(AccessIdUtil.isUserAccessId("user:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isUserAccessId_true_complexUser() {
        assertTrue(AccessIdUtil.isUserAccessId("user:" + defaultRealm + "/" + COMPLEX_USER));
    }

    @Test
    public void isUserAccessId_true_complexUserX509() {
        assertTrue(AccessIdUtil.isUserAccessId("user:" + defaultRealm + "/" + COMPLEX_USER_X509));
    }

    @Test
    public void isUserAccessId_true_mismatchedRealm() {
        assertTrue(AccessIdUtil.isUserAccessId("user:" + mismatchedRealm + "/uniqueId"));
    }

    @Test
    public void isGroupAccessId_notValid() {
        assertFalse(AccessIdUtil.isGroupAccessId("notValid"));
    }

    @Test
    public void isGroupAccessId_false() {
        assertFalse(AccessIdUtil.isGroupAccessId("user:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isGroupAccessId_true() {
        assertTrue(AccessIdUtil.isGroupAccessId("group:" + defaultRealm + "/uniqueId"));
    }

    @Test
    public void isGroupAccessId_true_complexUser() {
        assertTrue(AccessIdUtil.isGroupAccessId("group:" + defaultRealm + "/" + COMPLEX_USER));
    }

    @Test
    public void isGroupAccessId_true_complexUserX509() {
        assertTrue(AccessIdUtil.isGroupAccessId("group:" + defaultRealm + "/" + COMPLEX_USER_X509));
    }

    @Test
    public void isGroupAccessId_true_mismatchedRealm() {
        assertTrue(AccessIdUtil.isGroupAccessId("group:" + mismatchedRealm + "/uniqueId"));
    }

}
