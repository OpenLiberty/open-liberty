/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.Transient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CryptoUtilsTest {
    
    @Test
    public void testIsInsecureSHA() {
        assertTrue("SHA was not recognized as insecure", CryptoUtils.isAlgorithmInsecure("SHA"));
    }

    @Test
    public void testIsInsecureSHA1() {
        assertTrue("SHA1 was not recognized as insecure", CryptoUtils.isAlgorithmInsecure("SHA1"));
    }

    @Test
    public void testIsInsecureSHADash1() {
        assertTrue("SHA-1 was not recognized as insecure", CryptoUtils.isAlgorithmInsecure("SHA-1"));
    }

    @Test
    public void testIsInsecureSHA128() {
        assertTrue("SHA128 was not recognized as insecure", CryptoUtils.isAlgorithmInsecure("SHA128"));
    }

    @Test
    public void testIsInsecureMD5() {
        assertTrue("MD5 was not recognized as insecure", CryptoUtils.isAlgorithmInsecure("MD5"));
    }

    @Test
    public void testIsInsecureSHA256() {
        assertFalse("SHA256 was recognized as insecure", CryptoUtils.isAlgorithmInsecure("SHA256"));
    }

    @Test
    public void testGetSecureAlternative1() {
        assertEquals("SHA256 was recognized as insecure", "SHA256", CryptoUtils.getSecureAlternative("SHA1"));
    }

    @Test
    public void testGetSecureAlternativeNull() {
        assertEquals("SHA256 was recognized as insecure", null, CryptoUtils.getSecureAlternative("xxx"));
    }

    @Test
    public void testGetSecureAlternativeNull2() {
        assertEquals("SHA256 was recognized as insecure", null, CryptoUtils.getSecureAlternative(null));
    }
}
