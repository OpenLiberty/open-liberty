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
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.wsspi.security.crypto.KeyStringResolver;

/**
 *
 */
public class AESKeyManager {
    private static final AtomicReference<KeyStringResolver> _resolver = new AtomicReference<KeyStringResolver>();

    public static enum KeyVersion {
        AES_V0("PBKDF2WithHmacSHA1", 84756, 128, new byte[] { -89, -94, -125, 57, 76, 90, -77, 79, 50, 21, 10, -98, 47, 23, 17, 56, -61, 46, 125, -128 }),
        AES_V1("PBKDF2WithHmacSHA512", 210000, 256, new byte[] { -89, -63, 22, 15, -121, 11, 102, 75, -91, 68, -94, -89, 96, 83, -21, -69, -45, 29, 26, 106, -18, 69, 60, -6,
                                                                 108, 73, 111, 122, 41, -19, -78, -79, -28, 102, 57, -10, 66, 48, 54, 111, 35, 92, 59, -121, 36, 15, 14, -63,
                                                                 -43, 107, 63, -18, 87, 43, -57, 74, 0, 107, -119, -2, -7, -7, -46, -95, -44, 36, -10, 86, -119, -80, -114,
                                                                 10, 85, 24, 24, -121, -30, 63, 59, 49, 52, -76, -122, 108, -84, 16, 4, -39, 58, 75, 9, -25, 126, 127, -96,
                                                                 122, -62, -94, 71, -8, -101, -33, 57, -44, -93, 86, 76, -115, 113, -124, 104, -40, -121, -9, 86, 121, -48,
                                                                 -57, -77, -58, 73, 7, 12, 4, 24, -81, -64, 107 });

        private final AtomicReference<KeyHolder> _key = new AtomicReference<KeyHolder>();

        private final String alg;
        private final int iterations;
        public final int keyLength;
        private final byte[] salt;

        private KeyVersion(String alg, int iterations, int keyLength, byte[] salt) {
            this.alg = alg;
            this.iterations = iterations;
            this.keyLength = keyLength;
            this.salt = salt;
        }

        private KeyHolder get(char[] keyChars) throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeyHolder holder = _key.get();
            if (holder == null || !!!holder.matches(keyChars)) {
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(alg);
                KeySpec aesKey = new PBEKeySpec(keyChars, salt, iterations, keyLength);
                byte[] data = keyFactory.generateSecret(aesKey).getEncoded();
                KeyHolder holder2 = new KeyHolder(keyChars, new SecretKeySpec(data, "AES"), new IvParameterSpec(data));
                _key.compareAndSet(holder, holder2);
                // Still use this holder for returns even if I do not end up caching it.
                holder = holder2;
            }
            return holder;
        }
    }

    private static class KeyHolder {
        private final char[] keyChars;
        private final Key key;
        private final IvParameterSpec iv;

        public KeyHolder(char[] kc, Key k, IvParameterSpec ivParameterSpec) {
            keyChars = kc;
            key = k;
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

    /**
     * @param holder
     * @param keyChars
     * @return
     */
    private static KeyHolder getHolder(KeyVersion version, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] keyChars = _resolver.get().getKey(key == null ? "${wlp.password.encryption.key}" : key);
        return version.get(keyChars);
    }

    /**
     * @param object
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
     * @param cryptoKey
     * @return
     */
    public static IvParameterSpec getIV(KeyVersion version, String cryptoKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (version == KeyVersion.AES_V0) {
            return getHolder(version, cryptoKey).getIv();
        } else {
            return null;
        }
    }

    /**
     * @param cryptoKey
     * @return
     */
    @Deprecated
    public static IvParameterSpec getIV(String cryptoKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getHolder(KeyVersion.AES_V0, cryptoKey).getIv();
    }
}