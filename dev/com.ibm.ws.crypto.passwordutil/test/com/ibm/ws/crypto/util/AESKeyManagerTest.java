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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.wsspi.security.crypto.KeyStringResolver;
import com.ibm.wsspi.security.crypto.SecretKeyResolver;

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

    // -----------------------------------------------------------------------
    // isKeyConfigured tests
    // -----------------------------------------------------------------------

    /** Restore default (no-op) resolver and clear any SecretKeyResolver after each test. */
    @After
    public void resetResolvers() {
        AESKeyManager.setKeyStringResolver(null);
        AESKeyManager.setSecretKeyResolver(null);
    }

    /**
     * V1 not configured: the no-op KeyStringResolver returns the literal placeholder,
     * so isKeyConfigured must return false.
     */
    @Test
    public void testIsKeyConfigured_V1_unconfigured() {
        // Default resolver returns key string as-is → resolves to the placeholder literal.
        assertFalse("V1 should not be configured when resolver returns the placeholder",
                    AESKeyManager.isKeyConfigured(KeyVersion.AES_V1));
    }

    /**
     * V1 configured: the KeyStringResolver returns a real passphrase instead of the
     * placeholder, so isKeyConfigured must return true.
     */
    @Test
    public void testIsKeyConfigured_V1_configured() {
        AESKeyManager.setKeyStringResolver(new KeyStringResolver() {
            @Override
            public char[] getKey(String key) {
                if (AESKeyManager.PROPERTY_WLP_PASSWORD_ENCRYPTION_KEY.equals(key)) {
                    return "mySecretPassphrase".toCharArray();
                }
                return key.toCharArray();
            }
        });
        assertTrue("V1 should be configured when resolver returns a real passphrase",
                   AESKeyManager.isKeyConfigured(KeyVersion.AES_V1));
    }

    /**
     * V2 not configured: no SecretKeyResolver registered, and the default no-op resolver
     * returns the placeholder literal, so isKeyConfigured must return false.
     */
    @Test
    public void testIsKeyConfigured_V2_unconfigured() {
        assertFalse("V2 should not be configured when resolver returns the placeholder",
                    AESKeyManager.isKeyConfigured(KeyVersion.AES_V2));
    }

    /**
     * V2 configured via system property: the KeyStringResolver returns a valid base64
     * 32-byte key, so isKeyConfigured must return true.
     */
    @Test
    public void testIsKeyConfigured_V2_configured() {
        // A valid 32-byte key encoded in base64.
        final String validBase64Key = "pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=";
        AESKeyManager.setKeyStringResolver(new KeyStringResolver() {
            @Override
            public char[] getKey(String key) {
                if (AESKeyManager.PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY.equals(key)) {
                    return validBase64Key.toCharArray();
                }
                return key.toCharArray();
            }
        });
        assertTrue("V2 should be configured when resolver returns a real base64 key",
                   AESKeyManager.isKeyConfigured(KeyVersion.AES_V2));
    }

    /**
     * V2 with a hardware SecretKeyResolver registered: isKeyConfigured must return true
     * regardless of what the KeyStringResolver returns.
     */
    @Test
    public void testIsKeyConfigured_V2_hardwareResolver() {
        AESKeyManager.setSecretKeyResolver(new SecretKeyResolver() {
            @Override
            public Key getKey() {
                throw new UnsupportedOperationException("hardware key — never decoded");
            }
        });
        assertTrue("V2 should be configured when a hardware SecretKeyResolver is registered",
                   AESKeyManager.isKeyConfigured(KeyVersion.AES_V2));
    }

    // -----------------------------------------------------------------------
    // getKeyViaResolver tests
    // -----------------------------------------------------------------------

    /**
     * getKeyViaResolver for V1 configured: resolver returns a real key.
     */
    @Test
    public void testGetKeyViaResolver_V1_configured() throws Exception {
        AESKeyManager.setKeyStringResolver(new KeyStringResolver() {
            @Override
            public char[] getKey(String key) {
                if (AESKeyManager.PROPERTY_WLP_PASSWORD_ENCRYPTION_KEY.equals(key)) {
                    return "mySecretPassphrase".toCharArray();
                }
                return key.toCharArray();
            }
        });
        Key k = AESKeyManager.getKeyViaResolver(KeyVersion.AES_V1);
        assertTrue("getKeyViaResolver(V1) must return a non-null Key", k != null);
        assertEquals("Algorithm must be AES", "AES", k.getAlgorithm());
    }

    /**
     * getKeyViaResolver for V2 via hardware SecretKeyResolver: returns the Key from
     * the resolver without exposing any raw bytes.
     */
    @Test
    public void testGetKeyViaResolver_V2_hardwareResolver() throws Exception {
        final Key sentinel = new java.security.Key() {
            @Override public String getAlgorithm() { return "AES"; }
            @Override public String getFormat() { return "NONE"; }
            @Override public byte[] getEncoded() { return null; }
            private static final long serialVersionUID = 1L;
        };
        AESKeyManager.setSecretKeyResolver(new SecretKeyResolver() {
            @Override
            public Key getKey() {
                return sentinel;
            }
        });
        Key k = AESKeyManager.getKeyViaResolver(KeyVersion.AES_V2);
        assertSame("getKeyViaResolver(V2) with hardware resolver must return the resolver's Key", sentinel, k);
    }

}
