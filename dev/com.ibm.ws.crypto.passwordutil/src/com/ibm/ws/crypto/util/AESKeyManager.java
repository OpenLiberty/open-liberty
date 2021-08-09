/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 *
 */
public class AESKeyManager {
    private static final AtomicReference<KeyHolder> _key = new AtomicReference<KeyHolder>();
    private static final AtomicReference<KeyStringResolver> _resolver = new AtomicReference<KeyStringResolver>();

    public static interface KeyStringResolver {
        public char[] getKey(String val);
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

    public static Key getKey(String key) {

        KeyHolder holder = getHolder(key);

        return holder.getKey();
    }

    /**
     * @param holder
     * @param keyChars
     * @return
     */
    private static KeyHolder getHolder(String key) {
        char[] keyChars = _resolver.get().getKey(key == null ? "${wlp.password.encryption.key}" : key);
        KeyHolder holder = _key.get();
        if (holder == null || !!!holder.matches(keyChars)) {
            try {
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec aesKey = new PBEKeySpec(keyChars, new byte[] { -89, -94, -125, 57, 76, 90, -77, 79, 50, 21, 10, -98, 47, 23, 17, 56, -61, 46, 125, -128 }, 84756, 128);
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
    public static IvParameterSpec getIV(String cryptoKey) {
        return getHolder(cryptoKey).getIv();
    }
}