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
package com.ibm.ws.security.registry.basic.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

/**
 *
 */
public class BasicUserTest {

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_null() {
        BasicUser user = new BasicUser("user", "password");
        assertFalse(user.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_BasicGroup() {
        BasicUser user = new BasicUser("user", "password");
        assertFalse(user.equals(new BasicGroup("group", new HashSet<String>())));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_differentName() {
        BasicUser user = new BasicUser("user", "password");
        BasicUser userX = new BasicUser("userX", "password");
        assertFalse(user.equals(userX));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_sameNameDifferentPassword() {
        BasicUser user = new BasicUser("user", "password");
        BasicUser userX = new BasicUser("user", "passwordX");
        assertTrue(user.equals(userX));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals_self() {
        BasicUser user = new BasicUser("user", "password");
        assertTrue(user.equals(user));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#hashCode()}.
     */
    @Test
    public void testHashCode() {
        BasicUser user = new BasicUser("user", "password");
        assertEquals("Hash should be the hash of the name",
                     "user".hashCode(), user.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicUser#toString()}.
     */
    @Test
    public void testToString() {
        BasicUser user = new BasicUser("user", "password");
        assertEquals("toString should be the name",
                     "user", user.toString());
    }

    /**
     * Test method for testing constructor of BasicUser which takes BasicPassword as a parameter.
     */
    @Test
    public void testHashCodeWithBasicPassword() {
        BasicPassword bp = new BasicPassword("{hash}ATAAAAAIoqigxEbYwRVAAAAAQF/Vpz32XoxhnHFzFIc3j/vT8XCTDs3HHUolj7bTyN87pAI497KD5TxS0XeVLdkKTb3GIBf4mQQDZ+DxYBXB+Vk=", true);
        BasicUser user = new BasicUser("user", bp);
        assertEquals("Hash should be the hash of the name",
                     "user".hashCode(), user.hashCode());
    }

    /**
     * Test method for getPassword.
     */
    @Test
    public void testGetPasswordWithBasicPassword() {
        BasicPassword bp = new BasicPassword("{hash}ATAAAAAIoqigxEbYwRVAAAAAQF/Vpz32XoxhnHFzFIc3j/vT8XCTDs3HHUolj7bTyN87pAI497KD5TxS0XeVLdkKTb3GIBf4mQQDZ+DxYBXB+Vk=", true);
        BasicUser user = new BasicUser("user", bp);
        assertEquals("getPassword should be the same as the original one",
                     user.getPassword(), bp);
    }

}
