/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.crypto.util.InvalidPasswordCipherException;
import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.wsspi.security.crypto.EncryptedInfo;
import com.ibm.wsspi.security.crypto.SecretKeyResolver;

/**
 * Unit tests for {@link AesCipher}.
 */
public class AesCipherTest {

    private static final byte[] PLAINTEXT = "mySecretPassword".getBytes(StandardCharsets.UTF_8);

    // A valid 256-bit AES key encoded in base64.
    private static final String VALID_BASE64_KEY = "pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0="; //pragma: allowlist secret

    @After
    public void resetResolvers() {
        AESKeyManager.setKeyStringResolver(null);
        AESKeyManager.setSecretKeyResolver(null);
    }

    // -----------------------------------------------------------------------
    // V0 round-trip (AES-CBC / PBKDF2-HMAC-SHA1)
    // -----------------------------------------------------------------------

    @Test
    public void testV0RoundTrip() throws Exception {
        // V0 uses the configured key string resolver; the default no-op resolver returns the
        // property placeholder as-is, which becomes the passphrase for PBKDF2.
        SecretKeyResolver v0Resolver = AESKeyManager.getResolverFor(KeyVersion.AES_V0);

        EncryptedInfo encrypted = AesCipher.forEncrypt(KeyVersion.AES_V0, v0Resolver).encrypt(PLAINTEXT);
        assertNotNull("Encrypted payload must not be null", encrypted);
        assertEquals("V0 wire byte must be 0", 0, encrypted.getEncryptedBytes()[0]);

        byte[] decrypted = AesCipher.forDecrypt(KeyVersion.AES_V0, v0Resolver).decrypt(encrypted.getEncryptedBytes());
        assertArrayEquals("V0 round-trip must recover the original plaintext", PLAINTEXT, decrypted);
    }

    // -----------------------------------------------------------------------
    // V1 round-trip (AES-GCM / PBKDF2-HMAC-SHA512)
    // -----------------------------------------------------------------------

    @Test
    public void testV1RoundTrip() throws Exception {
        SecretKeyResolver v1Resolver = AESKeyManager.getResolverFor(KeyVersion.AES_V1);

        EncryptedInfo encrypted = AesCipher.forEncrypt(KeyVersion.AES_V1, v1Resolver).encrypt(PLAINTEXT);
        assertNotNull("Encrypted payload must not be null", encrypted);
        assertEquals("V1 wire byte must be 1", 1, encrypted.getEncryptedBytes()[0]);

        byte[] decrypted = AesCipher.forDecrypt(KeyVersion.AES_V1, v1Resolver).decrypt(encrypted.getEncryptedBytes());
        assertArrayEquals("V1 round-trip must recover the original plaintext", PLAINTEXT, decrypted);
    }

    // -----------------------------------------------------------------------
    // V2 round-trip (AES-GCM / raw base64 key)
    // -----------------------------------------------------------------------

    @Test
    public void testV2RoundTripWithBase64Key() throws Exception {
        byte[] rawKey = Base64.getDecoder().decode(VALID_BASE64_KEY);
        final Key aesKey = new SecretKeySpec(rawKey, "AES");
        SecretKeyResolver v2Resolver = () -> aesKey;

        EncryptedInfo encrypted = AesCipher.forEncrypt(KeyVersion.AES_V2, v2Resolver).encrypt(PLAINTEXT);
        assertNotNull("Encrypted payload must not be null", encrypted);
        assertEquals("V2 wire byte must be 2", 2, encrypted.getEncryptedBytes()[0]);

        byte[] decrypted = AesCipher.forDecrypt(KeyVersion.AES_V2, v2Resolver).decrypt(encrypted.getEncryptedBytes());
        assertArrayEquals("V2 round-trip must recover the original plaintext", PLAINTEXT, decrypted);
    }

    // -----------------------------------------------------------------------
    // forDecrypt — production path via wire byte
    // -----------------------------------------------------------------------

    @Test
    public void testForDecryptDispatchesViaWireByte_V1() throws Exception {
        SecretKeyResolver v1Resolver = AESKeyManager.getResolverFor(KeyVersion.AES_V1);
        EncryptedInfo encrypted = AesCipher.forEncrypt(KeyVersion.AES_V1, v1Resolver).encrypt(PLAINTEXT);

        // Production forDecrypt reads the wire byte; should pick V1 automatically.
        byte[] decrypted = AesCipher.forDecrypt(encrypted.getEncryptedBytes()).decrypt(encrypted.getEncryptedBytes());
        assertArrayEquals("forDecrypt(byte[]) must recover V1 plaintext", PLAINTEXT, decrypted);
    }

    @Test
    public void testForDecryptDispatchesViaWireByte_V2() throws Exception {
        byte[] rawKey = Base64.getDecoder().decode(VALID_BASE64_KEY);
        final Key aesKey = new SecretKeySpec(rawKey, "AES");
        AESKeyManager.setSecretKeyResolver(() -> aesKey);

        EncryptedInfo encrypted = AesCipher.forEncrypt(KeyVersion.AES_V2, AESKeyManager.getResolverFor(KeyVersion.AES_V2)).encrypt(PLAINTEXT);

        byte[] decrypted = AesCipher.forDecrypt(encrypted.getEncryptedBytes()).decrypt(encrypted.getEncryptedBytes());
        assertArrayEquals("forDecrypt(byte[]) must recover V2 plaintext", PLAINTEXT, decrypted);
    }

    // -----------------------------------------------------------------------
    // fromWireByte — unknown byte
    // -----------------------------------------------------------------------

    @Test
    public void testUnknownWireByte() throws Exception {
        // Manufacture a payload with an unknown version byte (0x7F).
        // An unrecognised wire byte means the ciphertext is malformed (CWWKS1857E),
        // not that the algorithm is unsupported (CWWKS1856E).
        byte[] badPayload = new byte[] { 0x7F, 0x00 };
        try {
            AesCipher.forDecrypt(badPayload);
            fail("forDecrypt with unknown wire byte must throw InvalidPasswordCipherException");
        } catch (InvalidPasswordCipherException e) {
            // expected — maps to CWWKS1857E (cipher exception), not CWWKS1856E
        }
    }

    // -----------------------------------------------------------------------
    // wireByte constants on KeyVersion
    // -----------------------------------------------------------------------

    @Test
    public void testWireByteConstants() {
        assertEquals("AES_V0 wireByte", (byte) 0, KeyVersion.AES_V0.wireByte);
        assertEquals("AES_V1 wireByte", (byte) 1, KeyVersion.AES_V1.wireByte);
        assertEquals("AES_V2 wireByte", (byte) 2, KeyVersion.AES_V2.wireByte);
    }

    @Test
    public void testFromWireByte() throws Exception {
        assertEquals(KeyVersion.AES_V0, KeyVersion.fromWireByte((byte) 0));
        assertEquals(KeyVersion.AES_V1, KeyVersion.fromWireByte((byte) 1));
        assertEquals(KeyVersion.AES_V2, KeyVersion.fromWireByte((byte) 2));
    }
}
