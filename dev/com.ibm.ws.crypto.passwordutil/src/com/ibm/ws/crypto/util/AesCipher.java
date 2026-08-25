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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.wsspi.security.crypto.EncryptedInfo;
import com.ibm.wsspi.security.crypto.SecretKeyResolver;

/**
 * Value-object that encapsulates a single AES encrypt or decrypt operation.
 *
 * <p>Use the static factory methods to obtain an instance:
 * <ul>
 *   <li>{@link #forEncrypt(KeyVersion, SecretKeyResolver)} — for an encipher operation</li>
 *   <li>{@link #forDecrypt(byte[])} — reads the wire-format version byte and resolves the
 *       {@link KeyVersion} and {@link SecretKeyResolver} automatically</li>
 *   <li>{@link #forDecrypt(KeyVersion, SecretKeyResolver)} — testing overload; bypasses
 *       wire-byte dispatch and uses the supplied resolver directly</li>
 * </ul>
 *
 * <p>Wire format:
 * <ul>
 *   <li>V0 (AES-CBC): {@code [0x00, ciphertext]}</li>
 *   <li>V1/V2 (AES-GCM): {@code [wireByte, IV_len, IV_bytes..., ciphertext]}</li>
 * </ul>
 *
 * <p>The plaintext payload (before encryption) always uses the seed layout:
 * {@code [seedLen (1 byte), seed (seedLen bytes), original plaintext]}.
 */
public final class AesCipher {

    private static final Logger logger = Logger.getLogger(AesCipher.class.getCanonicalName(), MessageUtils.RB);

    private static final AtomicBoolean alreadyLoggedAESWeakPasswordAlgoWarning = new AtomicBoolean(false);
    private static final AtomicBoolean alreadyLoggedAESDefaultKeyWarning = new AtomicBoolean(false);

    private final KeyVersion version;
    private final SecretKeyResolver resolver;

    private AesCipher(KeyVersion version, SecretKeyResolver resolver) {
        this.version = version;
        this.resolver = resolver;
    }

    // -----------------------------------------------------------------------
    // Factory methods
    // -----------------------------------------------------------------------

    /**
     * Returns an {@link AesCipher} configured for encryption using the given version and resolver.
     *
     * @param version  the {@link KeyVersion} that defines the cipher algorithm and wire format
     * @param resolver the {@link SecretKeyResolver} that will supply the {@link Key}
     * @return a configured {@link AesCipher} ready to {@link #encrypt(byte[])}
     */
    public static AesCipher forEncrypt(KeyVersion version, SecretKeyResolver resolver) {
        return new AesCipher(version, resolver);
    }

    /**
     * Returns an {@link AesCipher} configured for decryption by reading the version byte from
     * the first byte of {@code encryptedBytes}, resolving the {@link KeyVersion} via
     * {@link KeyVersion#fromWireByte(byte)}, and obtaining the {@link SecretKeyResolver} from
     * {@link AESKeyManager#getResolverFor(KeyVersion)}.
     *
     * <p>FIPS 140-3 rejection of V0 and the weak-algorithm warning are applied here.
     *
     * @param encryptedBytes the full AES-encrypted payload (must be non-null and non-empty)
     * @return a configured {@link AesCipher} ready to {@link #decrypt(byte[])}
     * @throws InvalidPasswordCipherException      if FIPS rejects V0, the payload is malformed,
     *                                             or the version byte is not recognised
     * @throws UnsupportedCryptoAlgorithmException if the algorithm is unavailable at the JVM level
     */
    public static AesCipher forDecrypt(byte[] encryptedBytes) throws InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {
        byte versionByte = encryptedBytes[0];
        KeyVersion version = KeyVersion.fromWireByte(versionByte);

        if (version == KeyVersion.AES_V0) {
            if (CryptoUtils.isFips140_3Enabled()) {
                throw new InvalidPasswordCipherException("FIPS 140-3 cannot use AES-128");
            }
            if (alreadyLoggedAESWeakPasswordAlgoWarning.compareAndSet(false, true)) {
                logger.logp(Level.WARNING, PasswordUtil.class.getName(), "forDecrypt", "PASSWORDUTIL_WEAK_ALGORITHM_WARNING",
                            new Object[] { "{aes}", ": AES-" + KeyVersion.AES_V0.keyLength, ": AES-" + KeyVersion.AES_V1.keyLength });
            }
            checkAndLogDefaultKeyWarning(KeyVersion.AES_V0);
        } else if (version == KeyVersion.AES_V1) {
            checkAndLogDefaultKeyWarning(KeyVersion.AES_V1);
        }

        return new AesCipher(version, AESKeyManager.getResolverFor(version));
    }

    /**
     * Testing overload: returns an {@link AesCipher} configured for decryption using the
     * supplied version and resolver directly, bypassing wire-byte dispatch.
     *
     * @param version  the {@link KeyVersion} to use
     * @param resolver the {@link SecretKeyResolver} to use
     * @return a configured {@link AesCipher} ready to {@link #decrypt(byte[])}
     */
    public static AesCipher forDecrypt(KeyVersion version, SecretKeyResolver resolver) {
        return new AesCipher(version, resolver);
    }

    // -----------------------------------------------------------------------
    // Cipher operations
    // -----------------------------------------------------------------------

    /**
     * Encrypts {@code plainBytes} and returns the serialised wire-format payload wrapped in an
     * {@link EncryptedInfo}.
     *
     * @param plainBytes the plaintext password bytes
     * @return an {@link EncryptedInfo} wrapping the encrypted wire-format bytes
     * @throws InvalidKeySpecException            if the key material is invalid
     * @throws InvalidPasswordCipherException     if the cipher operation fails
     * @throws UnsupportedCryptoAlgorithmException if the algorithm is not available
     */
    public EncryptedInfo encrypt(byte[] plainBytes) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {
        byte[] seeded = addSeed(plainBytes);
        try {
            if (version == KeyVersion.AES_V0) {
                return encryptCbc(seeded);
            } else {
                return encryptGcm(seeded);
            }
        } catch (NoSuchAlgorithmException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    /**
     * Decrypts the wire-format payload in {@code encryptedBytes} and returns the original
     * plaintext bytes.
     *
     * @param encryptedBytes the full AES-encrypted payload including version byte
     * @return the decrypted plaintext bytes
     * @throws InvalidKeySpecException            if the key material is invalid
     * @throws InvalidPasswordCipherException     if the cipher operation fails
     * @throws UnsupportedCryptoAlgorithmException if the algorithm is not available
     */
    public byte[] decrypt(byte[] encryptedBytes) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {
        try {
            byte[] decrypted;
            if (version == KeyVersion.AES_V0) {
                decrypted = decryptCbc(encryptedBytes);
            } else {
                decrypted = decryptGcm(encryptedBytes);
            }
            return removeSeed(decrypted);
        } catch (NoSuchAlgorithmException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    // -----------------------------------------------------------------------
    // Private cipher helpers
    // -----------------------------------------------------------------------

    private EncryptedInfo encryptCbc(byte[] seeded) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException, NoSuchAlgorithmException {
        try {
            Key key = resolver.getKey();
            Cipher c = Cipher.getInstance(CryptoUtils.AES_CBC_CIPHER);
            c.init(Cipher.ENCRYPT_MODE, key, AESKeyManager.getIV(KeyVersion.AES_V0, null));
            byte[] ciphertext = c.doFinal(seeded);
            byte[] output = new byte[ciphertext.length + 1];
            output[0] = version.wireByte;
            System.arraycopy(ciphertext, 0, output, 1, ciphertext.length);
            return new EncryptedInfo(output, "");
        } catch (NoSuchPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidKeyException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (IllegalBlockSizeException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (BadPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    private EncryptedInfo encryptGcm(byte[] seeded) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException, NoSuchAlgorithmException {
        try {
            Cipher c = Cipher.getInstance(CryptoUtils.AES_GCM_CIPHER);
            // Use CryptoUtils.generateRandomBytes to avoid UnsupportedOperationException on providers that do not implement generateSeed.
            GCMParameterSpec ps = new GCMParameterSpec(CryptoUtils.GCM_TAG_LENGTH, CryptoUtils.generateRandomBytes(c.getBlockSize()));
            Key key = resolver.getKey();
            c.init(Cipher.ENCRYPT_MODE, key, ps);
            byte[] ciphertext = c.doFinal(seeded);
            byte[] ivBytes = ps.getIV();
            byte[] output = new byte[ivBytes.length + ciphertext.length + 2];
            output[0] = version.wireByte;
            output[1] = (byte) ivBytes.length;
            System.arraycopy(ivBytes, 0, output, 2, ivBytes.length);
            System.arraycopy(ciphertext, 0, output, ivBytes.length + 2, ciphertext.length);
            return new EncryptedInfo(output, "");
        } catch (NoSuchPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidKeyException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (IllegalBlockSizeException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (BadPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    private byte[] decryptCbc(byte[] encryptedBytes) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException, NoSuchAlgorithmException {
        try {
            Key key = resolver.getKey();
            Cipher c = Cipher.getInstance(CryptoUtils.AES_CBC_CIPHER);
            c.init(Cipher.DECRYPT_MODE, key, AESKeyManager.getIV(KeyVersion.AES_V0, null));
            return c.doFinal(encryptedBytes, 1, encryptedBytes.length - 1);
        } catch (NoSuchPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidKeyException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (IllegalBlockSizeException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (BadPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    private byte[] decryptGcm(byte[] encryptedBytes) throws InvalidKeySpecException, InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException, NoSuchAlgorithmException {
        try {
            int ivLen = encryptedBytes[1];
            int cipherStart = ivLen + 2;
            GCMParameterSpec iv = new GCMParameterSpec(CryptoUtils.GCM_TAG_LENGTH, encryptedBytes, 2, ivLen);
            Key key = resolver.getKey();
            Cipher c = Cipher.getInstance(CryptoUtils.AES_GCM_CIPHER);
            c.init(Cipher.DECRYPT_MODE, key, iv);
            return c.doFinal(encryptedBytes, cipherStart, encryptedBytes.length - cipherStart);
        } catch (NoSuchPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidKeyException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (IllegalBlockSizeException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (BadPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        }
    }

    // -----------------------------------------------------------------------
    // Seed helpers (moved from PasswordCipherUtil)
    // -----------------------------------------------------------------------

    private static byte[] addSeed(byte[] plainBytes) {
        byte seedSize = 64;
        byte[] seed = CryptoUtils.generateRandomBytes(seedSize);
        byte[] seeded = new byte[plainBytes.length + seedSize + 1];
        seeded[0] = seedSize;
        System.arraycopy(seed, 0, seeded, 1, seedSize);
        System.arraycopy(plainBytes, 0, seeded, seedSize + 1, plainBytes.length);
        return seeded;
    }

    private static byte[] removeSeed(byte[] decrypted) {
        if (decrypted == null) {
            return null;
        }
        int seedSize = decrypted[0];
        byte[] result = new byte[decrypted.length - seedSize - 1];
        System.arraycopy(decrypted, seedSize + 1, result, 0, result.length);
        return result;
    }

    // -----------------------------------------------------------------------
    // Warning helpers
    // -----------------------------------------------------------------------

    private static void checkAndLogDefaultKeyWarning(KeyVersion version) {
        if (!alreadyLoggedAESDefaultKeyWarning.get() && !AESKeyManager.isKeyConfigured(version)) {
            if (alreadyLoggedAESDefaultKeyWarning.compareAndSet(false, true)) {
                logger.logp(Level.WARNING, AesCipher.class.getName(), "forDecrypt",
                            "PASSWORDUTIL_DEFAULT_KEY_WARNING");
            }
        }
    }
}
