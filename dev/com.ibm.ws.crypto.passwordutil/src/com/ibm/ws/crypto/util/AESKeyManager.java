/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
        PBKDF2_SHA1("PBKDF2WithHmacSHA1", 84756, 128, new byte[] { -89, -94, -125, 57, 76, 90, -77, 79, 50, 21, 10, -98, 47, 23, 17, 56, -61, 46, 125, -128 }),
        PBKDF2_SHA256("PBKDF2WithHmacSHA256", 84756, 128, new byte[] { 73, -125, -10, -15, 48, 90, -50, -73, -3, -25, -61, 14, -74, 48, -59, 122, -70, 34, 36, 52, 105, 48, -39, -80, -94, -46, 122, 109, -7, 59, 101, -105, 66, -58, 33, 6, -80, -128, 29, 50, 114, 104, 37, -119, -45, -8, -41, -123, 19, 108, -3, 21, 127, 48, 84, 62, 13, -89, 94, 2, -43, 101, -72, 15 });

        private final AtomicReference<KeyHolder> _key = new AtomicReference<KeyHolder>();

        private String alg;
        private byte[] salt;
        private int iterations;
        private int len;

        private KeyVersion(String a, int i, int l, byte[] s) {
            alg = a;
            salt = s;
            iterations = i;
            len = l;
        }

        private KeyHolder get(char[] keyChars) {
            KeyHolder holder = _key.get();
            if (holder == null || !!!holder.matches(keyChars)) {
                try {
                    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(alg);
                    KeySpec aesKey = new PBEKeySpec(keyChars, salt, iterations, len);
                    byte[] data = keyFactory.generateSecret(aesKey).getEncoded();
                    KeyHolder holder2 = new KeyHolder(keyChars, new SecretKeySpec(data, "AES"), new IvParameterSpec(data));
                    _key.compareAndSet(holder, holder2);
                    // Still use this holder for returns even if I do not end up caching it.
                    holder = holder2;
                } catch (InvalidKeySpecException e) {
                    return null;
                } catch (NoSuchAlgorithmException e) {
                    return null;
                }
    
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

    public static Key getKey(KeyVersion version, String key) {

        KeyHolder holder = getHolder(version, key);

        return holder.getKey();
    }

    @Deprecated
    public static Key getKey(String key) {

        KeyHolder holder = getHolder(KeyVersion.PBKDF2_SHA1, key);

        return holder.getKey();
    }

    /**
     * @param holder
     * @param keyChars
     * @return
     */
    private static KeyHolder getHolder(KeyVersion version, String key) {
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
    public static IvParameterSpec getIV(KeyVersion version, String cryptoKey) {
        if (version == KeyVersion.PBKDF2_SHA1) {
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
    public static IvParameterSpec getIV(String cryptoKey) {
        return getHolder(KeyVersion.PBKDF2_SHA1, cryptoKey).getIv();
    }
}