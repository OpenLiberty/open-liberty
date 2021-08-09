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
package com.ibm.ws.security.authentication.principals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.security.Principal;

import org.junit.Test;

/**
 *
 */
public class WSPrincipalTest {

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_nulls() {
        new WSPrincipal(null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullSecurityName() {
        String accessId = "user:realm/uniqueId";
        new WSPrincipal(null, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void ctor_nullAuthMethod() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        new WSPrincipal(securityName, accessId, null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#getName()}.
     */
    @Test
    public void getName() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        Principal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals("getName() should be the securityName specified in the constructor",
                     securityName, principal.getName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#getAccessId()}.
     */
    @Test
    public void getAccessId() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        WSPrincipal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals("getAccessId() should be the accessId specified in the constructor",
                     accessId, principal.getAccessId());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#getAccessId()}.
     */
    @Test
    public void getAccessId_Null() {
        String securityName = "securityName";
        String accessId = null;
        WSPrincipal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals("getAccessId() should be the accessId specified in the constructor",
                     accessId, principal.getAccessId());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#getAccessId()}.
     */
    @Test
    public void getAuthenticationMethod() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        WSPrincipal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals("getAuthenticationMethod() should be the authMethod specified in the constructor",
                     WSPrincipal.AUTH_METHOD_PASSWORD, principal.getAuthenticationMethod());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_null() {
        Principal principal = new WSPrincipal("securityName", "user:realm/uniqueId", WSPrincipal.AUTH_METHOD_PASSWORD);
        assertFalse("Null does not equal a WSPrincipal",
                    principal.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_StringSameSecurityName() {
        Principal principal = new WSPrincipal("securityName", "user:realm/uniqueId", WSPrincipal.AUTH_METHOD_PASSWORD);
        assertFalse("A String with the same accessId does not equal a WSPrincipal",
                    principal.equals("securityName"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_StringSameAccessId() {
        Principal principal = new WSPrincipal("securityName", "user:realm/uniqueId", WSPrincipal.AUTH_METHOD_PASSWORD);
        assertFalse("A String with the same accessId does not equal a WSPrincipal",
                    principal.equals("user:realm/uniqueId"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_WSPrincipalDifferentSecurityName() {
        Principal principal1 = new WSPrincipal("securityName", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);
        Principal principal2 = new WSPrincipal("securityNameX", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);
        assertFalse("A WSPrincipal with a different securityName is not equal",
                    principal1.equals(principal2));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_WSPrincipalDifferentAccessId() {
        Principal principal1 = new WSPrincipal("securityName", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);
        Principal principal2 = new WSPrincipal("securityName", "user:realm/nameX", WSPrincipal.AUTH_METHOD_PASSWORD);
        assertFalse("A WSPrincipal with a different accessId is not equal",
                    principal1.equals(principal2));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_WSPrincipalSameAccessId() {
        Principal principal1 = new WSPrincipal("securityName", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);
        Principal principal2 = new WSPrincipal("securityName", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);

        assertEquals("Only a WSPrincipal with the same accessId is equivalent",
                     principal1, principal2);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_WSPrincipalOneNullAccessId() {
        Principal principal1 = new WSPrincipal("securityName", null, WSPrincipal.AUTH_METHOD_PASSWORD);
        Principal principal2 = new WSPrincipal("securityName", "user:realm/name", WSPrincipal.AUTH_METHOD_PASSWORD);

        assertFalse("Only a WSPrincipal with the same accessId is equivalent",
                    principal1.equals(principal2));
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#equals(java.lang.Object)}.
     */
    @Test
    public void equalsObject_WSPrincipalNullAccessIds() {
        Principal principal1 = new WSPrincipal("securityName", null, WSPrincipal.AUTH_METHOD_PASSWORD);
        Principal principal2 = new WSPrincipal("securityName", null, WSPrincipal.AUTH_METHOD_PASSWORD);

        assertEquals("Only a WSPrincipal with the same accessId is equivalent", principal1, principal2);
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#hashCode()}.
     */
    @Test
    public void test_hashCode() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        Principal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals((principal.toString() + accessId).hashCode(), principal.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.authentication.principals.WSPrincipal#toString()}.
     */
    @Test
    public void test_toString() {
        String securityName = "securityName";
        String accessId = "user:realm/uniqueId";
        Principal principal = new WSPrincipal(securityName, accessId, WSPrincipal.AUTH_METHOD_PASSWORD);
        assertEquals("The String version of WSPrincipal should be the accessId",
                     "WSPrincipal:" + securityName, principal.toString());
    }

}
