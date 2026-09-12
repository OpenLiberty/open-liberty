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
package com.ibm.ws.crypto.ltpakeyutil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Unit tests for {@link AesLTPAKeyEncryptor}.
 */
public class AesLTPAKeyEncryptorTest {

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
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(key);

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
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(key);

        byte[] plaintext = "SomeKeyMaterial123456".getBytes("UTF-8");
        byte[] ciphertext = encryptor.encrypt(plaintext);

        assertFalse("Ciphertext must differ from plaintext",
                    Arrays.equals(plaintext, ciphertext));
    }

    /**
     * Two separate {@link AesLTPAKeyEncryptor} instances built from the same key
     * must be able to decrypt each other's output (same fixed IV, same key).
     */
    @Test
    public void crossInstanceDecrypt() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesLTPAKeyEncryptor enc1 = new AesLTPAKeyEncryptor(key);
        AesLTPAKeyEncryptor enc2 = new AesLTPAKeyEncryptor(key);

        byte[] plaintext = "CrossInstanceTest".getBytes("UTF-8");
        byte[] ciphertext = enc1.encrypt(plaintext);
        byte[] recovered = enc2.decrypt(ciphertext);

        assertArrayEquals("A second encryptor with the same key must decrypt ciphertext from the first",
                          plaintext, recovered);
    }

    /**
     * Constructing an {@link AesLTPAKeyEncryptor} from a key whose {@code getEncoded()} returns
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
        new AesLTPAKeyEncryptor(nullEncodedKey);
    }

    /**
     * The output of {@link AesLTPAKeyEncryptor#encrypt} must be at least
     * {@code IV_LENGTH + 1} bytes long: a 16-byte IV prefix followed by at least
     * one block of ciphertext.
     */
    @Test
    public void encrypt_prependsIV() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(key);

        byte[] plaintext = "SomeKeyMaterial".getBytes("UTF-8");
        byte[] ciphertext = encryptor.encrypt(plaintext);

        // IV is 16 bytes; AES/CBC/PKCS5Padding pads to a 16-byte block boundary,
        // so the minimum ciphertext length after the IV is 16 bytes.
        assertTrue("Encrypted output must be longer than IV_LENGTH (16) bytes",
                   ciphertext.length > 16);
    }

    /**
     * Two independent encryptions of the same plaintext with the same key must produce
     * different ciphertexts because a fresh random IV is generated on every call.
     */
    @Test
    public void twoEncryptionsProduceDifferentCiphertexts() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(key);

        byte[] plaintext = "SameInputEveryTime".getBytes("UTF-8");
        byte[] ct1 = encryptor.encrypt(plaintext);
        byte[] ct2 = encryptor.encrypt(plaintext);

        assertFalse("Two encryptions of the same plaintext must produce different ciphertexts (random IV)",
                    Arrays.equals(ct1, ct2));
    }

    /**
     * Feeding fewer than {@code IV_LENGTH} (16) bytes to
     * {@link AesLTPAKeyEncryptor#decrypt} must throw an exception, not silently
     * return garbage.
     */
    @Test
    public void decrypt_truncatedInput_throwsException() throws Exception {
        Key key = makeAesKey(VALID_KEY_B64);
        AesLTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(key);

        // 8 bytes — shorter than the 16-byte IV prefix that decrypt expects.
        byte[] truncated = new byte[8];

        try {
            encryptor.decrypt(truncated);
            fail("Expected an exception when decrypting input shorter than IV_LENGTH bytes");
        } catch (Exception e) {
            // Any exception (ArrayIndexOutOfBoundsException, IllegalArgumentException,
            // BadPaddingException, etc.) is acceptable — the point is it must not succeed.
        }
    }
}
