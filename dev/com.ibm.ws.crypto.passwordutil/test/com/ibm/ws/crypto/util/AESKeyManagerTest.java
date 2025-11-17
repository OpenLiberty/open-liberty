/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.junit.Test;

import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;

/**
 *
 */
public class AESKeyManagerTest {

    @Test
    public void testBase64KeyDecodeNullKey() {
        try {
            KeyVersion.AES_V2.decodeAesBase64Key(null);
            fail("decodeAeSBase64Key should throw an InvalidKeySpecException if a null value is passed in.");
        } catch (InvalidKeySpecException e) {
            assertEquals("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET exception not caught.", e.getMessage(), MessageUtils.getMessage("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET"));
            // intentionally empty, we should check for an exception but we don't have a message ID and can't resolve the message from within unit tests.
        }
    }

    @Test
    public void testBase64KeyDecodeDefaultKey() {
        try {
            KeyVersion.AES_V2.decodeAesBase64Key(AESKeyManager.PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY.toCharArray());
            fail("decodeAeSBase64Key should throw an InvalidKeySpecException if a ${wlp.aes.encryption.key} value is passed in.");
        } catch (InvalidKeySpecException e) {
            assertEquals("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET exception not caught.", e.getMessage(), MessageUtils.getMessage("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET"));
            // intentionally empty, we should check for an exception but we don't have a message ID and can't resolve the message from within unit tests.
        }
    }

    @Test
    public void testBase64KeyDecodeBadKey() {
        try {
            KeyVersion.AES_V2.decodeAesBase64Key("notbase64".toCharArray());
            fail("decodeAeSBase64Key should throw an InvalidKeySpecException if an invalid key is passed in.");
        } catch (InvalidKeySpecException e) {
            assertEquals("AESKEYMANAGER_NOT_BASE64_EXCEPTION exception not caught.", e.getMessage(), MessageUtils.getMessage("AESKEYMANAGER_NOT_BASE64_EXCEPTION"));
            // intentionally empty, we should check for an exception but we don't have a message ID and can't resolve the message from within unit tests.
        }
    }

    @Test
    public void testBase64KeyDecodeKeyTooSmall() {
        try {
            KeyVersion.AES_V2.decodeAesBase64Key("MTIzNDU2Nzg5MDEyMzQ1Ngo=".toCharArray());
            fail("decodeAeSBase64Key should throw an InvalidKeySpecException if key isn't 256-bit.");
        } catch (InvalidKeySpecException e) {
            assertEquals("AESKEYMANAGER_INVALID_KEYLENGTH_EXCEPTION exception not caught.", e.getMessage(), MessageUtils.getMessage("AESKEYMANAGER_INVALID_KEYLENGTH_EXCEPTION"));
            // intentionally empty, we should check for an exception but we don't have a message ID and can't resolve the message from within unit tests.
        }
    }

    @Test
    public void testBase64KeyValidKey() throws InvalidKeySpecException {
        KeyVersion.AES_V2.decodeAesBase64Key("pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=".toCharArray());
    }

    @Test
    public void testPbkDf2() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String key = new String(Base64.getEncoder().encode(KeyVersion.AES_V1.buildAesKeyWithPbkdf2("testString".toCharArray())));
        assertEquals("PBKDF2 derived key invalid.", "eqZ+0lybrSgztOfXg8D3flMtykcH3M/wOtRKaQYcNMA=", key);
    }

}
