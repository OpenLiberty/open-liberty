/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.wsspi.security.crypto.KeyStringResolver;
import com.ibm.wsspi.security.crypto.SecretKeyResolver;

/**
 *
 */
public class AESKeyManager {

    public static final String NAME_WLP_PASSWORD_ENCRYPTION_KEY = "wlp.password.encryption.key";
    public static final String NAME_WLP_BASE64_AES_ENCRYPTION_KEY = "wlp.aes.encryption.key";
    public static final String PROPERTY_WLP_PASSWORD_ENCRYPTION_KEY = "${" + NAME_WLP_PASSWORD_ENCRYPTION_KEY + "}";
    public static final String PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY = "${" + NAME_WLP_BASE64_AES_ENCRYPTION_KEY + "}";

    private static final AtomicReference<KeyStringResolver> _resolver = new AtomicReference<KeyStringResolver>();

    public enum KeyVersion {

        // FIPS 140-3: Algorithm assessment complete; no changes required.
        // AES_V0 is only used for backward compatibility, newly created passwords will use AES_V1. If FIPS is enabled, AES_V0 will not be tolerated
        // and users must recreate their password
        // AES_V2 allows users to specify a raw AES-256 key to encrypt / decrypt. This is specified in base64 format in property PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY.
        AES_V0(CryptoUtils.PBKDF2_WITH_HMAC_SHA1, CryptoUtils.PBKDF2HMACSHA1_ITERATIONS, CryptoUtils.AES_128_KEY_LENGTH_BITS, CryptoUtils.AES_V0_SALT, PROPERTY_WLP_PASSWORD_ENCRYPTION_KEY),
        AES_V1(CryptoUtils.PBKDF2_WITH_HMAC_SHA512, CryptoUtils.PBKDF2HMACSHA512_ITERATIONS, CryptoUtils.AES_256_KEY_LENGTH_BITS, CryptoUtils.AES_V1_SALT, PROPERTY_WLP_PASSWORD_ENCRYPTION_KEY),
        // AES_v2 omits salt and iterations since it doesn't use PBKDF for key derivation, unlike V0 and V1.
        AES_V2(CryptoUtils.ENCRYPT_ALGORITHM_AES, 0, CryptoUtils.AES_256_KEY_LENGTH_BITS, null, PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY);

        private final AtomicReference<KeyHolder> _key = new AtomicReference<KeyHolder>();

        private final String alg;
        private final int iterations;
        public final int keyLength;
        private final byte[] salt;
        public final String resolverProperty;
        final AtomicReference<SecretKeyResolver> resolver;

        private KeyVersion(String alg, int iterations, int keyLength, byte[] salt, String resolverProperty) {
            this.alg = alg;
            this.iterations = iterations;
            this.keyLength = keyLength;
            this.salt = salt;
            this.resolverProperty = resolverProperty;
            this.resolver = new AtomicReference<>(new DefaultSecretKeyResolver(this));
        }

        /**
         * If key is already in memory, return the holder containing the key. Otherwise, build the key and update the holder.
         *
         * @param keyChars an array containing either a raw key in Base64 if alg == ENCRYPT_ALGORITHM_AES, a keyphrase used to derive a key otherwise.
         * @return a KeyHolder object containing the AES key specified in keyChars
         * @throws NoSuchAlgorithmException
         * @throws InvalidKeySpecException
         */
        private KeyHolder get(char[] keyChars) throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeyHolder holder = _key.get();
            if (holder == null || !!!holder.matches(keyChars)) {
                byte[] data;
                if (CryptoUtils.ENCRYPT_ALGORITHM_AES.equals(alg)) {
                    data = decodeAesBase64Key(keyChars);
                } else {
                    data = buildAesKeyWithPbkdf2(keyChars);
                }
                byte[] iv = Arrays.copyOfRange(data, 0, CryptoUtils.AES_128_KEY_LENGTH_BYTES);
                KeyHolder holder2 = new KeyHolder(keyChars, new SecretKeySpec(data, CryptoUtils.ENCRYPT_ALGORITHM_AES), new IvParameterSpec(iv));
                _key.compareAndSet(holder, holder2);
                // Still use this holder for returns even if I do not end up caching it.
                holder = holder2;
            }
            return holder;
        }

        /**
         * @param keyChars a char array to be base64 decoded into a byte array
         * @return keyChars as a base64 decoded byte array.
         * @throws InvalidKeySpecException if keyChars does not represent a valid base64 value.
         */
        public byte[] decodeAesBase64Key(char[] keyChars) throws InvalidKeySpecException {
            if (keyChars == null) {
                throw new InvalidKeySpecException(MessageUtils.getMessage("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET"));
            }
            String keyString = new String(keyChars);
            if (PROPERTY_WLP_BASE64_AES_ENCRYPTION_KEY.equals(keyString)) {
                throw new InvalidKeySpecException(MessageUtils.getMessage("AESKEYMANAGER_BASE64_VARIABLE_NOT_SET"));
            }
            byte[] data;
            try {
                data = Base64.getDecoder().decode(keyString);
            } catch (IllegalArgumentException iae) {
                throw new InvalidKeySpecException(MessageUtils.getMessage("AESKEYMANAGER_NOT_BASE64_EXCEPTION"), iae);
            }
            int keyBitLength = data.length * 8;
            if (keyBitLength != this.keyLength) {
                throw new InvalidKeySpecException(MessageUtils.getMessage("AESKEYMANAGER_INVALID_KEYLENGTH_EXCEPTION"));
            }
            return data;
        }

        public byte[] buildAesKeyWithPbkdf2(char[] keyChars) throws NoSuchAlgorithmException, InvalidKeySpecException {

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(alg);
            KeySpec aesKey = new PBEKeySpec(keyChars, salt, iterations, keyLength);
            return keyFactory.generateSecret(aesKey).getEncoded();
        }
    }

    private static class KeyHolder {
        private final char[] keyChars;
        private final Key key;
        private final IvParameterSpec iv;

        public KeyHolder(char[] keyChars, Key key, IvParameterSpec ivParameterSpec) {
            this.keyChars = keyChars;
            this.key = key;
            iv = ivParameterSpec;
        }

        public boolean matches(char[] k) {
            if (k == keyChars)
                return true;
            return Arrays.equals(k, keyChars);
        }

        public Key getKey() {
            return key;
        }

        public IvParameterSpec getIv() {
            return iv;
        }
    }

    static {
        setKeyStringResolver(null);
    }

    public static Key getKey(KeyVersion version, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyHolder holder = getHolder(version, key);
        return holder.getKey();
    }

    @Deprecated
    public static Key getKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeyHolder holder = getHolder(KeyVersion.AES_V0, key);

        return holder.getKey();
    }

    private static KeyHolder getHolder(KeyVersion version, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] keyChars = getKeyCharsUsingResolver(version, key);
        return version.get(keyChars);
    }

    /**
     * @param version the KeyVersion for 'key'
     * @param key     the provided key, if null the KeyVersion.resolveProperty is used to resolve the key.
     * @return the resolved Key as char[]
     */
    public static char[] getKeyCharsUsingResolver(KeyVersion version, String key) {
        return _resolver.get().getKey(key == null ? version.resolverProperty : key);
    }

    /**
     * Sets a hardware-backed {@link SecretKeyResolver} (e.g. ICSF/CKDS via IBMJCECCA) directly
     * on {@link KeyVersion#AES_V2}. When non-null, all AES_V2 encrypt and decrypt operations will
     * use the supplied resolver. Pass {@code null} to revert to the software base64-key path.
     *
     * @param resolver the resolver to install, or null to revert to the standard char[]-based path
     */
    public static void setSecretKeyResolver(SecretKeyResolver resolver) {
        SecretKeyResolver effective = (resolver != null) ? resolver : new DefaultSecretKeyResolver(KeyVersion.AES_V2);
        KeyVersion.AES_V2.resolver.set(effective);
        // Invalidate any cached AES_V2 key so the next encrypt/decrypt starts clean
        KeyVersion.AES_V2._key.set(null);
    }

    /**
     * Returns the active hardware-backed {@link SecretKeyResolver} if one is installed,
     * or {@code null} if AES_V2 is currently using the default software path.
     *
     * @return the active hardware SecretKeyResolver, or null
     */
    public static SecretKeyResolver getSecretKeyResolver() {
        SecretKeyResolver skr = KeyVersion.AES_V2.resolver.get();
        return (skr instanceof DefaultSecretKeyResolver) ? null : skr;
    }

    /**
     * Sets the {@link KeyStringResolver} used to look up key strings by name (e.g. from
     * server configuration variables). Pass {@code null} to restore the default no-op resolver
     * that returns the key string as-is.
     *
     * @param resolver the resolver to install, or {@code null} to revert to the default
     */
    public static void setKeyStringResolver(KeyStringResolver resolver) {
        if (resolver == null) {
            resolver = new KeyStringResolver() {

                @Override
                public char[] getKey(String key) {
                    return key.toCharArray();
                }
            };
        }
        _resolver.set(resolver);
    }

    /**
     * Returns the {@link IvParameterSpec} for the given key version and raw key string.
     *
     * @param version   the key version to use
     * @param cryptoKey the raw key string, or {@code null} to use the version's configured property
     * @return the IV derived from the resolved key
     */
    public static IvParameterSpec getIV(KeyVersion version, String cryptoKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getHolder(version, cryptoKey).getIv();
    }

    /**
     * @deprecated Use {@link #getIV(KeyVersion, String)} with {@link KeyVersion#AES_V0}.
     */
    @Deprecated
    public static IvParameterSpec getIV(String cryptoKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getHolder(KeyVersion.AES_V0, cryptoKey).getIv();
    }

    /**
     * Returns {@code true} if the given key version has a real key configured — i.e.
     * the backing system property or {@link SecretKeyResolver} holds an actual value
     * rather than the unresolved placeholder string.
     *
     * <p>For {@link KeyVersion#AES_V2}: also returns {@code true} when a non-default
     * hardware-backed {@link SecretKeyResolver} is registered via
     * {@link #setSecretKeyResolver(SecretKeyResolver)}.
     *
     * @param version the key version to test
     * @return {@code true} if the version is configured with a real key
     */
    public static boolean isKeyConfigured(KeyVersion version) {
        if (version == KeyVersion.AES_V2 && getSecretKeyResolver() != null) {
            return true;
        }
        char[] keyChars = getKeyCharsUsingResolver(version, null);
        String keyString = new String(keyChars);
        return !version.resolverProperty.equals(keyString);
    }

    /**
     * Returns the {@link java.security.Key} for the given version by calling through the
     * version's own {@link SecretKeyResolver}. For {@link KeyVersion#AES_V2} this honours a
     * hardware-backed resolver (e.g. ICSF/CKDS) so the raw key bytes are never exposed. For
     * V1/V0 it delegates to the default software resolver, which derives the key via PBKDF2.
     *
     * <p>This is the preferred call site for obtaining the key when the caller wants a
     * {@link java.security.Key} object — for example when constructing an
     * {@code AesKeyEncryptor} — because it keeps the Key opaque and avoids calling
     * {@code getEncoded()} on hardware keys.
     *
     * @param version the key version whose resolver should be invoked
     * @return the resolved {@link java.security.Key}
     * @throws NoSuchAlgorithmException if the required algorithm is not available
     * @throws InvalidKeySpecException  if the key material is invalid or improperly encoded
     */
    public static java.security.Key getKeyViaResolver(KeyVersion version) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return version.resolver.get().getKey();
    }

}