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
package com.ibm.ws.security.registry;

import org.junit.Test;

/**
 * Parent class for UserRegistry API compliance tests.
 * 
 * Verifies that a UserRegistry will throw IllegalArgumentException
 * if methods are invoked with null or empty parameters.
 */
public abstract class UserRegistryIllegalArgumentTemplate {
    public UserRegistry reg;

    /**
     * Constructs the test class.
     * 
     * @param reg an initialized UserRegistry instance
     */
    public UserRegistryIllegalArgumentTemplate(UserRegistry reg) {
        this.reg = reg;
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_nullUsername() throws Exception {
        reg.checkPassword(null, "password");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_emptyUsername() throws Exception {
        reg.checkPassword("", "password");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_nullPassword() throws Exception {
        reg.checkPassword("user", null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#checkPassword(java.lang.String, java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_emptyPassword() throws Exception {
        reg.checkPassword("user", "");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void mapCertificate() throws Exception {
        reg.mapCertificate(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#isValidUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidUser_null() throws Exception {
        reg.isValidUser(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#isValidUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidUser_empty() throws Exception {
        reg.isValidUser("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUsers(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUsers_null() throws Exception {
        reg.getUsers(null, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUsers(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUsers_empty() throws Exception {
        reg.getUsers("", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUserDisplayName_null() throws Exception {
        reg.getUserDisplayName(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUserDisplayName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUserDisplayName_empty() throws Exception {
        reg.getUserDisplayName("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueUserId_null() throws Exception {
        reg.getUniqueUserId(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueUserId(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueUserId_empty() throws Exception {
        reg.getUniqueUserId("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUserSecurityName_null() throws Exception {
        reg.getUserSecurityName(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUserSecurityName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUserSecurityName_empty() throws Exception {
        reg.getUserSecurityName("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#isValidGroup(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidGroup_null() throws Exception {
        reg.isValidGroup(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#isValidGroup(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isValidGroup_empty() throws Exception {
        reg.isValidGroup("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroups(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroups_null() throws Exception {
        reg.getGroups(null, 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroups(java.lang.String, int)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroups_empty() throws Exception {
        reg.getGroups("", 0);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupDisplayName_null() throws Exception {
        reg.getGroupDisplayName(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupDisplayName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupDisplayName_empty() throws Exception {
        reg.getGroupDisplayName("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueGroupId_null() throws Exception {
        reg.getUniqueGroupId(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueGroupId(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueGroupId_empty() throws Exception {
        reg.getUniqueGroupId("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupSecurityName_null() throws Exception {
        reg.getGroupSecurityName(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupSecurityName(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupSecurityName_empty() throws Exception {
        reg.getGroupSecurityName("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueGroupIdsForUser_null() throws Exception {
        reg.getUniqueGroupIdsForUser(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getUniqueGroupIdsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUniqueGroupIdsForUser_empty() throws Exception {
        reg.getUniqueGroupIdsForUser("");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupsForUser_null() throws Exception {
        reg.getGroupsForUser(null);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.UserRegistry#getGroupsForUser(java.lang.String)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getGroupsForUser_empty() throws Exception {
        reg.getGroupsForUser("");
    }

}
