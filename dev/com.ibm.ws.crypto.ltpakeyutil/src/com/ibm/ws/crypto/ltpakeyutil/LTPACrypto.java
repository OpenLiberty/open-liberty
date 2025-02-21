/*******************************************************************************
 * Copyright (c) 1997, 2011, 2025 IBM Corporation and others.
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

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.crypto.CryptoUtils;

final class LTPACrypto {

    private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();

    private static final String provider = CryptoUtils.getProvider();

    private static final String signatureAlgorithm = CryptoUtils.getSignatureAlgorithm();

    private static int MAX_CACHE = 500; // has to be greater than 0 and a multiple of 5
    private static IvParameterSpec ivs8 = null;
    private static IvParameterSpec ivs16 = null;

    @Trivial
    private static class CachingKey {

        private boolean reused = false;
        private long successfulUses;
        private final byte[][] key;
        private final byte[] data;
        private final int off;
        private final int len;
        private int hashcode;
        private byte[] result;

        @Trivial
        private CachingKey(byte[][] key, byte[] data, int off, int len) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.successfulUses = 0;
            this.reused = false;

            this.hashcode = 0;
            if (key != null && key.length > 0) {
                if (key[0] != null && key[0].length > 0) {
                    hashcode += key[0][0];
                }
            }
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    hashcode += data[i];
                }
            }

            hashcode += off + len;
            if (off != 0) {
                hashcode *= off;
            }
            hashcode *= 2;
        }

        @Trivial
        @Override
        public boolean equals(Object to) {
            if (!(to instanceof CachingKey)) {
                return false;
            }

            CachingKey ck = (CachingKey) to;

            if (hashcode != ck.hashcode) {
                return false;
            }

            if (len != ck.len) {
                return false;
            }

            if (key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < key.length; i++) {
                    if (key[i] != null) {
                        if (ck.key[i] == null) {
                            return false;
                        } else {
                            if (key[i].length != ck.key[i].length) {
                                return false;
                            }
                        }
                        for (int o = 0; o < key[i].length; o++) {
                            if (key[i][o] != ck.key[i][o]) {
                                return false;
                            }
                        }
                    } else {
                        if (ck.key[i] != null) {
                            return false;
                        }
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (off != ck.off) {
                return false;
            }

            return true;
        }

        @Override
        @Trivial
        public int hashCode() {
            return hashcode;
        }

    }

    private static final ConcurrentHashMap<CachingKey, CachingKey> cryptoKeysMap = new ConcurrentHashMap<CachingKey, CachingKey>();

    /**
     * Sign the data.
     *
     * @param key  The key used to sign the data
     * @param data The byte representation of the data
     * @param off  The offset of the data
     * @param len  The length of the data
     * @return The signature of the data
     */
    @Trivial
    protected static final byte[] signISO9796(byte[][] key, byte[] data, int off, int len) throws Exception {
        CachingKey ck = new CachingKey(key, data, off, len);
        CachingKey result = cryptoKeysMap.get(ck);

        if (result != null) {
            result.successfulUses += 1;
            result.reused = true;
            return result.result;
        } else {
            if (cryptoKeysMap.size() >= MAX_CACHE) {
                try {
                    int cryptoKeysMapSize = cryptoKeysMap.size();
                    CachingKey[] keys = cryptoKeysMap.keySet().toArray(new CachingKey[cryptoKeysMapSize]);
                    Arrays.sort(keys, cachingKeyComparator);
                    if (cachingKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                        for (int i = 0; i < cryptoKeysMapSize / 5; i++) {
                            cryptoKeysMap.remove(keys[i]);
                            keys[i + 1 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 2 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 3 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[i + 4 * cryptoKeysMapSize / 5].successfulUses--;
                        }
                    } else { // TODO: consider removing bc this likely isn't used since we sort the keys above (except when all keys have the same num of uses)
                        for (int i = 0; i < cryptoKeysMapSize / 5; i++) {
                            cryptoKeysMap.remove(keys[keys.length - 1 - i]);
                            keys[keys.length - 1 - i - 1 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 2 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 3 * cryptoKeysMapSize / 5].successfulUses--;
                            keys[keys.length - 1 - i - 4 * cryptoKeysMapSize / 5].successfulUses--;
                        }
                    }
                } catch (Exception e) {
                    // do nothing. since this code is used for the command line
                    // utility, no log is
                    // taken.
                }

            }
        }

        /** Invoked by LTPADigSignature **/
        BigInteger n = new BigInteger(key[0]);
        BigInteger e = new BigInteger(key[2]);
        BigInteger p = new BigInteger(key[3]);
        BigInteger q = new BigInteger(key[4]);
        BigInteger d = e.modInverse((p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE)));
        KeyFactory kFact = null;

        kFact = (provider == null) ? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                : KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);

        BigInteger pep = new BigInteger(key[5]);
        BigInteger peq = new BigInteger(key[6]);
        BigInteger crtC = new BigInteger(key[7]);
        RSAPrivateCrtKeySpec privCrtKeySpec = new RSAPrivateCrtKeySpec(n, e, d, p, q, pep, peq, crtC);
        PrivateKey privKey = kFact.generatePrivate(privCrtKeySpec);

        Signature rsaSig = null;

        rsaSig = (provider == null) ? Signature.getInstance(signatureAlgorithm)
                : Signature.getInstance(signatureAlgorithm, provider);

        rsaSig.initSign(privKey);
        rsaSig.update(data, off, len);
        byte[] sig = rsaSig.sign();

        cryptoKeysMap.put(ck, ck);
        ck.result = sig;
        ck.successfulUses = 0;

        return sig;
    }

    private static final ConcurrentHashMap<CachingVerifyKey, CachingVerifyKey> verifyKeysMap = new ConcurrentHashMap<CachingVerifyKey, CachingVerifyKey>();

    @Trivial
    private static class CachingVerifyKey {

        private long successfulUses;
        private final byte[][] key;
        private final byte[] data;
        private final int off;
        private final int len;
        private final byte[] sig;
        private final int sigOff;
        private final int sigLen;
        private int hashcode;
        private boolean result;

        @Trivial
        private CachingVerifyKey(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.sig = sig;
            this.sigOff = sigOff;
            this.sigLen = sigLen;
            this.successfulUses = 0;

            this.hashcode = 0;
            if (key != null && key.length > 0) {
                if (key[0] != null && key[0].length > 0) {
                    this.hashcode += key[0][0];
                }
            }
            if (data != null) {
                for (int i = 0; i < data.length && i < 10; i++) {
                    this.hashcode += data[i];
                }
                for (int i = data.length - 1; i >= 0 && i > data.length - 10; i--) {
                    this.hashcode += data[i];
                }
            }

            this.hashcode += off;
            if (off != 0) {
                this.hashcode *= off;
            }
            this.hashcode *= 2;
        }

        @Override
        @Trivial
        public boolean equals(Object to) {
            if (!(to instanceof CachingVerifyKey)) {
                return false;
            }

            CachingVerifyKey ck = (CachingVerifyKey) to;

            if (this.hashcode != ck.hashcode) {
                return false;
            }

            if (this.len != ck.len) {
                return false;
            }

            if (this.key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (this.key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.key.length; i++) {
                    if (this.key[i] != null) {
                        if (ck.key[i] == null) {
                            return false;
                        } else {
                            if (this.key[i].length != ck.key[i].length) {
                                return false;
                            }
                        }
                        for (int o = 0; o < this.key[i].length; o++) {
                            if (this.key[i][o] != ck.key[i][o]) {
                                return false;
                            }
                        }
                    } else {
                        if (ck.key[i] != null) {
                            return false;
                        }
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (this.data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (this.data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.data.length; i++) {
                    if (this.data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (this.sig != null) {
                if (ck.sig == null) {
                    return false;
                } else {
                    if (this.sig.length != ck.sig.length) {
                        return false;
                    }
                }
                for (int i = 0; i < this.sig.length; i++) {
                    if (this.sig[i] != ck.sig[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.sig != null) {
                    return false;
                }
            }

            if (this.off != ck.off) {
                return false;
            }

            if (this.sigOff != ck.sigOff) {
                return false;
            }

            if (this.sigLen != ck.sigLen) {
                return false;
            }

            return true;
        }

        @Override
        @Trivial
        public int hashCode() {
            return this.hashcode;
        }

    }

    private static final Comparator<CachingVerifyKey> cachingVerifyKeyComparator = new Comparator<CachingVerifyKey>() {
        @Override
        @Trivial
        public int compare(CachingVerifyKey o1, CachingVerifyKey o2) {
            if (o1.successfulUses < o2.successfulUses) {
                return -1;
            } else if (o1.successfulUses == o2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };
    private static final Comparator<CachingKey> cachingKeyComparator = new Comparator<CachingKey>() {
        @Override
        @Trivial
        public int compare(CachingKey o1, CachingKey o2) {
            if (!o1.reused) {
                if (o2.reused) {
                    return -1;
                }
            } else {
                if (!o2.reused) {
                    return 1;
                }
            }
            if (o1.successfulUses < o2.successfulUses) {
                return -1;
            } else if (o1.successfulUses == o2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    /**
     * Verify if the signature of the data is correct.
     *
     * @param key  The key used to verify the data
     * @param data The byte representation of the data
     * @param off  The offset of the data
     * @param len  The length of the data
     * @param sig  The signature of the data
     * @param off  The offset of the signature
     * @param len  The length of the signature
     * @return True if the signature of the data is correct
     */
    @Trivial
    protected static final boolean verifyISO9796(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff,
            int sigLen) throws Exception {
        CachingVerifyKey ck = new CachingVerifyKey(key, data, off, len, sig, sigOff, sigLen);
        CachingVerifyKey result = verifyKeysMap.get(ck);

        if (result != null) {
            result.successfulUses += 1;
            return result.result;
        } else {
            if (verifyKeysMap.size() >= MAX_CACHE) {
                int verifyKeysMapSize = verifyKeysMap.size();
                CachingVerifyKey[] keys = verifyKeysMap.keySet().toArray(new CachingVerifyKey[verifyKeysMapSize]);
                Arrays.sort(keys, cachingVerifyKeyComparator);
                if (cachingVerifyKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                    for (int i = 0; i < verifyKeysMapSize / 5; i++) {
                        verifyKeysMap.remove(keys[i]);
                        keys[i + 1 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 2 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 3 * verifyKeysMapSize / 5].successfulUses--;
                        keys[i + 4 * verifyKeysMapSize / 5].successfulUses--;
                    }
                } else { // TODO: consider removing bc this likely isn't used since we sort the keys above (except when all keys have the same num of uses)
                    for (int i = 0; i < verifyKeysMapSize / 5; i++) {
                        verifyKeysMap.remove(keys[keys.length - 1 - i]);
                        keys[keys.length - 1 - i - 1 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 2 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 3 * verifyKeysMapSize / 5].successfulUses--;
                        keys[keys.length - 1 - i - 4 * verifyKeysMapSize / 5].successfulUses--;
                    }
                }
            }
        }

        boolean verified = false;

        BigInteger n = new BigInteger(key[0]);
        BigInteger e = new BigInteger(key[1]);

        KeyFactory kFact = null;
        Signature rsaSig = null;

        kFact = (provider == null) ? KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                : KeyFactory.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);

        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(n, e);
        PublicKey pubKey = kFact.generatePublic(pubKeySpec);

        rsaSig = (provider == null) ? Signature.getInstance(signatureAlgorithm)
                : Signature.getInstance(signatureAlgorithm, provider);

        rsaSig.initVerify(pubKey);
        rsaSig.update(data, off, len);
        verified = rsaSig.verify(sig);

        verifyKeysMap.put(ck, ck);
        ck.result = verified;
        ck.successfulUses = 0;

        return verified;
    }

    /**
     * Set the key for RSA algorithms.
     *
     * @param key The key
     */
    @Trivial
    protected static final void setRSAKey(byte[][] key) {
        BigInteger[] k = new BigInteger[8];
        for (int i = 0; i < 8; i++) {
            if (key[i] != null) {
                k[i] = new BigInteger(1, key[i]);
            }
        }

        if (k[3].compareTo(k[4]) < 0) {
            BigInteger tmp;
            tmp = k[3];
            k[3] = k[4];
            k[4] = tmp;
            tmp = k[5];
            k[5] = k[6];
            k[6] = tmp;
            k[7] = null;
        }
        if (k[7] == null) {
            k[7] = k[4].modInverse(k[3]);
        }
        if (k[0] == null) {
            k[0] = k[3].multiply(k[4]);
        }
        if (k[1] == null) {
            k[1] = k[2].modInverse(k[3].subtract(BigInteger.valueOf(1)).multiply(k[4].subtract(BigInteger.valueOf(1))));
        }
        if (k[5] == null) {
            k[5] = k[1].remainder(k[3].subtract(BigInteger.valueOf(1)));
        }
        if (k[6] == null) {
            k[6] = k[1].remainder(k[4].subtract(BigInteger.valueOf(1)));
        }
        for (int i = 0; i < 8; i++) {
            key[i] = k[i].toByteArray();
        }

    }

    /**
     * @param key
     * @param cipher
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Trivial
    private static SecretKey constructSecretKey(byte[] key, String cipher)
            throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        SecretKey sKey = null;
        if (cipher.indexOf(CryptoUtils.ENCRYPT_ALGORITHM_AES) != -1) {
            int keyLength = fipsEnabled ? CryptoUtils.AES_256_KEY_LENGTH_BYTES : CryptoUtils.AES_128_KEY_LENGTH_BYTES;
            sKey = new SecretKeySpec(key, 0, keyLength, CryptoUtils.ENCRYPT_ALGORITHM_AES);
        } else {
            DESedeKeySpec kSpec = new DESedeKeySpec(key);
            SecretKeyFactory kFact = null;

            kFact = (provider == null) ? SecretKeyFactory.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_DESEDE)
                    : SecretKeyFactory.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_DESEDE, provider);

            sKey = kFact.generateSecret(kSpec);
        }
        return sKey;
    }

    /**
     * @param key
     * @param cipher
     * @param sKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    @Trivial
    private static Cipher createCipher(int cipherMode, byte[] key, String cipher, SecretKey sKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, NoSuchProviderException {

        Cipher ci = null;
        ci = (provider == null) ? Cipher.getInstance(cipher) : Cipher.getInstance(cipher, provider);

        if (cipher.indexOf(CryptoUtils.ENCRYPT_MODE_ECB) == -1) {
            if (cipher.indexOf(CryptoUtils.ENCRYPT_ALGORITHM_AES) != -1) {
                setIVS16(key);
                ci.init(cipherMode, sKey, ivs16);
            } else {
                setIVS8(key);
                ci.init(cipherMode, sKey, ivs8);
            }
        } else {
            ci.init(cipherMode, sKey);
        }
        return ci;
    }

    /**
     * Encrypt the data.
     *
     * @param data   The byte representation of the data
     * @param key    The key used to encrypt the data
     * @param cipher The cipher algorithm
     * @return The encrypted data (ciphertext)
     */
    @Trivial
    protected static final byte[] encrypt(byte[] data, byte[] key, String cipher) throws Exception {
        SecretKey sKey = constructSecretKey(key, cipher);
        Cipher ci = createCipher(Cipher.ENCRYPT_MODE, key, cipher, sKey);
        return ci.doFinal(data);
    }

    /**
     * Decrypt the specified msg.
     *
     * @param msg    The byte representation of the data
     * @param key    The key used to decrypt the data
     * @param cipher The cipher algorithm
     * @return The decrypted data (plaintext)
     */
    @Trivial
    protected static final byte[] decrypt(byte[] msg, byte[] key, String cipher) throws Exception {
        SecretKey sKey = constructSecretKey(key, cipher);
        Cipher ci = createCipher(Cipher.DECRYPT_MODE, key, cipher, sKey);
        return ci.doFinal(msg);
    }

    /*
     * Set the maximam size of the cache. It is used only for the junit tests.
     *
     * @param maxCache The maximam size of the cache
     */
    @Trivial
    protected static void setMaxCache(int maxCache) {
        MAX_CACHE = maxCache;
    }

    /*
     * Set the 8-byte initialization vector.
     *
     * @param key The key
     */
    @Trivial
    private static final synchronized void setIVS8(byte[] key) {
        byte[] iv8 = new byte[8];
        for (int i = 0; i < 8; i++) {
            iv8[i] = key[i];
        }
        ivs8 = new IvParameterSpec(iv8);
    }

    /*
     * Set the 16-byte initialization vector.
     *
     * @param key The key
     */
    @Trivial
    private static final synchronized void setIVS16(byte[] key) {
        byte[] iv16 = new byte[16];
        for (int i = 0; i < 16; i++) {
            iv16[i] = key[i];
        }
        ivs16 = new IvParameterSpec(iv16);
    }

    @Trivial
    static final int lsbf(byte[] data, int i, int n) {
        int v = 0;
        do {
            v |= (data[i + (--n)] & 0xFF) << n * 8;
        } while (n > 0);
        return v;
    }

    @Trivial
    static final int lsbf4(byte[] data, int i) {
        return (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8) | ((data[i + 2] & 0xFF) << 16) | (data[i + 3] << 24);
    }

    @Trivial
    static final void lsbf4(int v, byte[] data, int i) {
        data[i] = (byte) v;
        data[i + 1] = (byte) (v >>> 8);
        data[i + 2] = (byte) (v >>> 16);
        data[i + 3] = (byte) (v >>> 24);
    }

    @Trivial
    static void lsbf2(int v, byte[] data, int i) {
        data[i] = (byte) v;
        data[i + 1] = (byte) (v >>> 8);
    }

    @Trivial
    private static final int FF(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += ((b & c) | (~b & d)) + x + ac) << l) | (a >>> r)) + b;
    }

    @Trivial
    private static final int GG(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += ((b & d) | (c & ~d)) + x + ac) << l) | (a >>> r)) + b;
    }

    @Trivial
    private static final int HH(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += (b ^ c ^ d) + x + ac) << l) | (a >>> r)) + b;
    }

    @Trivial
    private static final int II(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += (c ^ (b | ~d)) + x + ac) << l) | (a >>> r)) + b;
    }

    @Trivial
    static final void md5(int[] state, byte[] data, int off, int len, byte[] to, int pos) {
        int a, b, c, d;
        {
            a = 0x67452301;
            b = 0xefcdab89;
            c = 0x98badcfe;
            d = 0x10325476;
        }

        int W0, W1, W2, W3, W4, W5, W6, W7, W8, W9, W10, W11, W12, W13, W14, W15;
        int i, n = len / 4, a0, b0, c0, d0;
        final int[] W = new int[16];

        boolean done = false;
        boolean padded = false;

        do {

            for (i = 0; (i < 16) && (n > 0); n--, off += 4)
                W[i++] = lsbf4(data, off);

            if (i < 16) {

                if (!padded) {
                    W[i++] = ((a0 = len % 4) != 0) ? (lsbf(data, off, a0) | (0x80 << (a0 * 8))) : 0x80;
                    if (i == 15)
                        W[15] = 0;
                    padded = true;
                }
                if (i <= 14) {
                    while (i < 14)
                        W[i++] = 0;

                    if (state != null)
                        len += state[5];
                    W[14] = len << 3;
                    W[15] = len >>> 29;
                    done = true;
                }
            }

            a = FF((a0 = a), b, c, d, (W0 = W[0]), 7, 25, 0xd76aa478);
            d = FF((d0 = d), a, b, c, (W1 = W[1]), 12, 20, 0xe8c7b756);
            c = FF((c0 = c), d, a, b, (W2 = W[2]), 17, 15, 0x242070db);
            b = FF((b0 = b), c, d, a, (W3 = W[3]), 22, 10, 0xc1bdceee);
            a = FF(a, b, c, d, (W4 = W[4]), 7, 25, 0xf57c0faf);
            d = FF(d, a, b, c, (W5 = W[5]), 12, 20, 0x4787c62a);
            c = FF(c, d, a, b, (W6 = W[6]), 17, 15, 0xa8304613);
            b = FF(b, c, d, a, (W7 = W[7]), 22, 10, 0xfd469501);
            a = FF(a, b, c, d, (W8 = W[8]), 7, 25, 0x698098d8);
            d = FF(d, a, b, c, (W9 = W[9]), 12, 20, 0x8b44f7af);
            c = FF(c, d, a, b, (W10 = W[10]), 17, 15, 0xffff5bb1);
            b = FF(b, c, d, a, (W11 = W[11]), 22, 10, 0x895cd7be);
            a = FF(a, b, c, d, (W12 = W[12]), 7, 25, 0x6b901122);
            d = FF(d, a, b, c, (W13 = W[13]), 12, 20, 0xfd987193);
            c = FF(c, d, a, b, (W14 = W[14]), 17, 15, 0xa679438e);
            b = FF(b, c, d, a, (W15 = W[15]), 22, 10, 0x49b40821);

            a = GG(a, b, c, d, W1, 5, 27, 0xf61e2562);
            d = GG(d, a, b, c, W6, 9, 23, 0xc040b340);
            c = GG(c, d, a, b, W11, 14, 18, 0x265e5a51);
            b = GG(b, c, d, a, W0, 20, 12, 0xe9b6c7aa);
            a = GG(a, b, c, d, W5, 5, 27, 0xd62f105d);
            d = GG(d, a, b, c, W10, 9, 23, 0x2441453);
            c = GG(c, d, a, b, W15, 14, 18, 0xd8a1e681);
            b = GG(b, c, d, a, W4, 20, 12, 0xe7d3fbc8);
            a = GG(a, b, c, d, W9, 5, 27, 0x21e1cde6);
            d = GG(d, a, b, c, W14, 9, 23, 0xc33707d6);
            c = GG(c, d, a, b, W3, 14, 18, 0xf4d50d87);
            b = GG(b, c, d, a, W8, 20, 12, 0x455a14ed);
            a = GG(a, b, c, d, W13, 5, 27, 0xa9e3e905);
            d = GG(d, a, b, c, W2, 9, 23, 0xfcefa3f8);
            c = GG(c, d, a, b, W7, 14, 18, 0x676f02d9);
            b = GG(b, c, d, a, W12, 20, 12, 0x8d2a4c8a);

            a = HH(a, b, c, d, W5, 4, 28, 0xfffa3942);
            d = HH(d, a, b, c, W8, 11, 21, 0x8771f681);
            c = HH(c, d, a, b, W11, 16, 16, 0x6d9d6122);
            b = HH(b, c, d, a, W14, 23, 9, 0xfde5380c);
            a = HH(a, b, c, d, W1, 4, 28, 0xa4beea44);
            d = HH(d, a, b, c, W4, 11, 21, 0x4bdecfa9);
            c = HH(c, d, a, b, W7, 16, 16, 0xf6bb4b60);
            b = HH(b, c, d, a, W10, 23, 9, 0xbebfbc70);
            a = HH(a, b, c, d, W13, 4, 28, 0x289b7ec6);
            d = HH(d, a, b, c, W0, 11, 21, 0xeaa127fa);
            c = HH(c, d, a, b, W3, 16, 16, 0xd4ef3085);
            b = HH(b, c, d, a, W6, 23, 9, 0x4881d05);
            a = HH(a, b, c, d, W9, 4, 28, 0xd9d4d039);
            d = HH(d, a, b, c, W12, 11, 21, 0xe6db99e5);
            c = HH(c, d, a, b, W15, 16, 16, 0x1fa27cf8);
            b = HH(b, c, d, a, W2, 23, 9, 0xc4ac5665);

            a = II(a, b, c, d, W0, 6, 26, 0xf4292244);
            d = II(d, a, b, c, W7, 10, 22, 0x432aff97);
            c = II(c, d, a, b, W14, 15, 17, 0xab9423a7);
            b = II(b, c, d, a, W5, 21, 11, 0xfc93a039);
            a = II(a, b, c, d, W12, 6, 26, 0x655b59c3);
            d = II(d, a, b, c, W3, 10, 22, 0x8f0ccc92);
            c = II(c, d, a, b, W10, 15, 17, 0xffeff47d);
            b = II(b, c, d, a, W1, 21, 11, 0x85845dd1);
            a = II(a, b, c, d, W8, 6, 26, 0x6fa87e4f);
            d = II(d, a, b, c, W15, 10, 22, 0xfe2ce6e0);
            c = II(c, d, a, b, W6, 15, 17, 0xa3014314);
            b = II(b, c, d, a, W13, 21, 11, 0x4e0811a1);
            a = II(a, b, c, d, W4, 6, 26, 0xf7537e82);
            d = II(d, a, b, c, W11, 10, 22, 0xbd3af235);
            c = II(c, d, a, b, W2, 15, 17, 0x2ad7d2bb);
            b = II(b, c, d, a, W9, 21, 11, 0xeb86d391) + b0;

            a += a0;
            c += c0;
            d += d0;
        } while (!done);

        {
            lsbf4(a, to, pos);
            lsbf4(b, to, pos + 4);
            lsbf4(c, to, pos + 8);
            lsbf4(d, to, pos + 12);
        }
    }

    private static double[] ETB = new double[16];

    static {
        double d = ETB[0] = 0.001;
        double log2d = Math.log(2 * d);
        int i = 1;
        do {
            ETB[i] = Math.exp(log2d / ++i) / 2;
        } while (i < ETB.length);
    }

    private static int slot, channels;
    private static int[] samples = new int[56];
    private static int[] ones = new int[16];
    private static int[] block = new int[16];

    @Trivial
    static final void trng(byte[] to, int off, int len) {
        long accu = 0;
        int bits = 0, i, m, j;

        while (len-- > 0) {
            while (bits < 8) {

                int s = 0;
                do {
                    long t = System.currentTimeMillis();
                    while (System.currentTimeMillis() == t)
                        s++;
                } while (s == 0);

                int xor = samples[slot] ^ s;
                samples[slot] = s;

                i = 0;
                m = 1;
                do {

                    if ((xor & m) != 0) {
                        ones[i] += (((s & m) != 0) ? 1 : -1);
                        channels ^= m;
                    }

                    if (--block[i] == 0) {
                        accu = (accu << 1) | (((channels & m) != 0) ? 1 : 0);
                        bits++;
                    }
                    if (block[i] <= 0) {

                        for (j = 0; j < 16; j++) {
                            if (Math.abs(0.5 - (double) ones[i] / (double) 56) <= ETB[j])
                                break;
                        }

                        block[i] = (j == 16) ? -1 : j + 1;
                    }
                    m <<= 1;
                } while (++i < 16);
                slot = (slot + 1) % 56;
            }
            to[off++] = (byte) (accu >>> (bits -= 8));
        }
    }

    private static byte[] seed = new byte[32];
    private static int ri;
    private static boolean seedInitialized = false;

    static int trMix = 128;

    static String[][] rsaKeyMaterial = {

            { "4svq2jqtxo3zn2njenso9vwyg2bynvo08ekktj4d7sqwk9s3oz",
                    "4se994le3trmoep5f74ytxfupr2o0oi9dem4nzailb4k4g5e7j", "1ekh" },

            { "uk5febz1u9c5x7knn185refnb02syox36xqwae0lm30z9j9p03"
                    + "hyu175dyxbiczds3k1n6jiwqdeyetwgsy1qrvje8a7o40cmb5",
                    "ujsuw3e4k53dtzgbsm3tjpytf5h25i71r8cs8ijbigo607ceo5"
                            + "zy5toem0kp4oeb77tt86h7gkix5fjdq13sa7puya61b2ep82n",
                    "3" } };

    static String[][] dsaKeyMaterial = {

            { "otj4bi3e6pxy54h5tkjwpuzycvm3ta6jg9f6lj52mvygb9l72y1tkrs0ppuldns6kem6vzw3fbwhinhdhpqjvn284fc0dsaz39h",
                    "jpdh5mk2p667os7al4gmvbdfmar3bsv",
                    "cdybrmm4x665tomdaiedafq3d2wiajhlkbeql7iui72eeayleaa3ppn7lhfdbrh508kum7havwgb7otsnme3pc8r7kipf55hvio",
                    "lpb2xrb2yivmklm6i6pyzvagsu9qhdz",
                    "6d3ng23juhszoxet3kkzw2ei7y3hxo67c9oqvuf5d1dpev7qzwhzy11tcaikknfxtr62zyk96d9vvhli6zw2b2sxbrnlc3xkuzy" },

            { "10uj5jh4khn7t93eh41c1d7sfptfuqiycpiimudbj62leu8fwnnt3k5cdkzynrvbhlflm3qe6sfwsjs3bbvjm8j8ctzaljlothj"
                    + "tbujclhafng31uzf4zmj11qjni0z9ou77rap19wl7ps7v52fbuoycrgu6xohwoobiwfanlkh4t18wtw3kf1nsdxz7mwpu9ddu4cz",
                    "s6zmy3zi8dumvm43ofheresn52f9trj",
                    "z4asx4yhsha3vd0d0uhhnahzmtj1qg572k3frvtq46x9lrawlm4x70oc99d4qsplci9e8qjtaqt3sqf719tfojrwjnonkqbxm9o"
                            + "p3ck61fcxx2q6l4vg1rizk9kn74pi9859nqqctvn9174smwqzosvdrnd89eykgocc09ph343gpen9lgo0h6dk32a35gut5wb6w1",
                    "f8xedoxwqju60mngerxyt5jv7rl8wbg",
                    "egc8c7ptmx0hr5i4x2bzgeumx8kcmc9jokca88r8e4k1ih802bnz9flr08topo1v7kodqg9yab3xpf2j0lv9zmg8jhh38okgjfe"
                            + "ou1fb7xn6blo4t1m8fb64p849eaqa66f1c0ar7m1uwdwc9k57vr58frxezjd1w4sc4zp8s6wn89lmbzem0brt6phtukhg2qfgrn" } };

    static byte[][][] rsaKeys;
    static byte[][][] dsaKeys;

    @Trivial
    static final void random(byte[] to, int off, int n) {
        if (!seedInitialized) {
            trng(seed, 0, 32);
            md5(null, seed, 0, 32, seed, 0);

            rsaKeys = new byte[4][][];
            for (int i = 0, j = 0; i < rsaKeyMaterial.length; i++, j += 2) {
                rsaKeys[j] = new byte[8][];
                rsaKeys[j][2] = new BigInteger(rsaKeyMaterial[i][2], 36).toByteArray();
                rsaKeys[j][3] = new BigInteger(rsaKeyMaterial[i][0], 36).toByteArray();
                rsaKeys[j][4] = new BigInteger(rsaKeyMaterial[i][1], 36).toByteArray();
                setRSAKey(rsaKeys[j]);
                rsaKeys[j + 1] = new byte[][] { rsaKeys[j][0], rsaKeys[j][2] };
            }

            dsaKeys = new byte[4][4][];
            for (int i = 0, j = 0; i < dsaKeyMaterial.length; i++, j += 2) {

                for (int k = 0; k < 3; k++)
                    dsaKeys[j][k] = dsaKeys[j + 1][k] = new BigInteger(dsaKeyMaterial[i][k], 36).toByteArray();
                dsaKeys[j][3] = new BigInteger(dsaKeyMaterial[i][3], 36).toByteArray();
                dsaKeys[j + 1][3] = new BigInteger(dsaKeyMaterial[i][4], 36).toByteArray();
            }

            seedInitialized = true;
        }

        synchronized (seed) {
            for (int i = 0; i < n; i++) {
                int ri8 = ++ri % 8;

                if (ri % trMix == 0) {
                    byte b = seed[ri8];
                    trng(seed, ri8, 1);
                    seed[ri8] ^= b;
                }

                if (ri8 == 0)
                    md5(null, seed, 0, 32, seed, 0);

                to[off++] = seed[ri8];
            }
        }
    }

    @Trivial
    static final byte[] generateSharedKey() {
        return (fipsEnabled) ? CryptoUtils.generateRandomBytes(CryptoUtils.AES_256_KEY_LENGTH_BYTES)
                : CryptoUtils.generateRandomBytes(CryptoUtils.DESEDE_KEY_LENGTH_BYTES);
    }

    @Trivial
    static final byte[][] rsaKey(int len, boolean crt, boolean f4) {
        byte[][] key = new byte[crt ? 8 : 3][];
        KeyPair pair = null;
        KeyPairGenerator keyGen = null;
        try {

            keyGen = (provider == null) ? KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA)
                    : KeyPairGenerator.getInstance(CryptoUtils.CRYPTO_ALGORITHM_RSA, provider);

            keyGen.initialize(len * 8, new SecureRandom());
            pair = keyGen.generateKeyPair();
            RSAPublicKey rsaPubKey = (RSAPublicKey) pair.getPublic();
            RSAPrivateCrtKey rsaPrivKey = (RSAPrivateCrtKey) pair.getPrivate();

            BigInteger e = rsaPubKey.getPublicExponent();
            BigInteger n = rsaPubKey.getModulus();
            BigInteger pe = rsaPrivKey.getPrivateExponent();
            key[0] = n.toByteArray();
            key[1] = crt ? null : pe.toByteArray();
            key[2] = e.toByteArray();

            if (crt) {
                BigInteger p = rsaPrivKey.getPrimeP();
                BigInteger q = rsaPrivKey.getPrimeQ();
                BigInteger ep = rsaPrivKey.getPrimeExponentP();
                BigInteger eq = rsaPrivKey.getPrimeExponentQ();
                BigInteger c = rsaPrivKey.getCrtCoefficient();
                key[3] = p.toByteArray();
                key[4] = q.toByteArray();
                key[5] = ep.toByteArray();
                key[6] = eq.toByteArray();
                key[7] = c.toByteArray();
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            // instrumented ffdc
        } catch (java.security.NoSuchProviderException e) {
            // instrumented ffdc
        } catch (java.lang.UnsupportedOperationException uoe) {
            // This is when hard ware crypto provider is at the top of java.security
            // Using the different key creation routines.
            System.out.println(
                    "DEBUG: UnsupportedOperationException is caught!! Going back to the previous hardware crypto routine for evaluation.");
            BigInteger p, q, n, d;
            BigInteger e = BigInteger.valueOf(f4 ? 0x10001 : 3);
            BigInteger one = BigInteger.valueOf(1), two = BigInteger.valueOf(2);
            byte[] b = new byte[(len /= 2) + 1];

            for (p = null;;) {
                for (q = null;;) {
                    if (q == null) {
                        random(b, 1, len);
                        b[1] |= 0xC0;
                        b[len] |= 1;
                        q = new BigInteger(b);
                    } else {
                        q = q.add(two);
                        if (q.bitLength() > len * 8) {
                            q = null;
                            continue;
                        }
                    }

                    if (q.isProbablePrime(32) && e.gcd(q.subtract(one)).equals(one))
                        break;
                }

                if (p == null)
                    p = q;
                else {
                    n = p.multiply(q);
                    if (n.bitLength() == len * 2 * 8) {

                        d = e.modInverse((p.subtract(one)).multiply(q.subtract(one)));

                        if (((p.modPow(e, n)).modPow(d, n)).equals(p))
                            break;
                    }
                    p = null;
                }
            }

            key[0] = n.toByteArray(); // modulus
            key[1] = crt ? null : d.toByteArray(); // private exponent if a CRT key
            key[2] = e.toByteArray(); // public exponent

            if (crt) {
                if (p.compareTo(q) < 0) {
                    e = p;
                    p = q;
                    q = e;
                }
                key[3] = p.toByteArray(); // PrimeP
                key[4] = q.toByteArray(); // PrimeQ
                key[5] = d.remainder(p.subtract(one)).toByteArray(); // PrimeExponentP \
                key[6] = d.remainder(q.subtract(one)).toByteArray(); // PrimeExponentQ - looks like JCE sets these to
                                                                     // zero. You could calculate these if you want
                                                                     // to.
                key[7] = q.modInverse(p).toByteArray(); // getCrtCoefficient /
            }
        }

        return key;
    }
}
