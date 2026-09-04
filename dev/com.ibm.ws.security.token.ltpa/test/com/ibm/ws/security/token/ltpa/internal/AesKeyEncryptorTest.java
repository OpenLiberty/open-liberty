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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Unit tests for {@link AesKeyEncryptor}.
 */
public class AesKeyEncryptorTest {

    /** A valid 32-byte (256-bit) AES key encoded as base64. */
    private static final String VALID_KEY_B64 = "pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=";

    private static Key makeAesKey(String base64) {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypt then decrypt must recover the original plaintext.
     */
    @Test
    public void roundTrip_softwareKey() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesKeyEncryptor encryptor = new AesKeyEncryptor(key);

        byte[] plaintext = "Hello LTPA key bytes!".getBytes("UTF-8");
        byte[] ciphertext = encryptor.encrypt(plaintext);
        byte[] recovered = encryptor.decrypt(ciphertext);

        assertArrayEquals("Decrypted bytes must equal original plaintext", plaintext, recovered);
    }

    /**
     * Ciphertext must differ from plaintext (encryption actually changes the data).
     */
    @Test
    public void encryptProducesDifferentBytes() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesKeyEncryptor encryptor = new AesKeyEncryptor(key);

        byte[] plaintext = "SomeKeyMaterial123456".getBytes("UTF-8");
        byte[] ciphertext = encryptor.encrypt(plaintext);

        assertFalse("Ciphertext must differ from plaintext",
                    Arrays.equals(plaintext, ciphertext));
    }

    /**
     * Two separate {@link AesKeyEncryptor} instances built from the same key
     * must be able to decrypt each other's output (same fixed IV, same key).
     */
    @Test
    public void crossInstanceDecrypt() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesKeyEncryptor enc1 = new AesKeyEncryptor(key);
        AesKeyEncryptor enc2 = new AesKeyEncryptor(key);

        byte[] plaintext = "CrossInstanceTest".getBytes("UTF-8");
        byte[] ciphertext = enc1.encrypt(plaintext);
        byte[] recovered = enc2.decrypt(ciphertext);

        assertArrayEquals("A second encryptor with the same key must decrypt ciphertext from the first",
                          plaintext, recovered);
    }

    /**
     * Constructing an {@link AesKeyEncryptor} from a key whose {@code getEncoded()} returns
     * {@code null} (e.g. a hardware-backed key) must not throw a NullPointerException.
     * The constructor must silently fall back to the fixed zero IV path.
     */
    @Test
    public void constructor_nullEncodedKey_doesNotThrow() {
        Key nullEncodedKey = new Key() {
            @Override public String getAlgorithm() { return "AES"; }
            @Override public String getFormat() { return "NONE"; }
            @Override public byte[] getEncoded() { return null; }
            private static final long serialVersionUID = 1L;
        };
        // Must not throw
        new AesKeyEncryptor(nullEncodedKey);
    }
}
