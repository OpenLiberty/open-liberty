/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.audit.encryption;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;

final class AuditCrypto {

    private static TraceComponent tc = Tr.register(AuditCrypto.class, null, "com.ibm.ejs.resources.security");
    private static IvParameterSpec ivs16 = null;

    public AuditCrypto() {}

    static final byte[] generateSharedKey() {
        return CryptoUtils.generateRandomBytes(CryptoUtils.AES_256_KEY_LENGTH_BYTES);
    }

    static final byte[] encrypt(byte[] data, byte[] key) {
        return encrypt(data, key, CryptoUtils.AES_CBC_CIPHER);
    }

    static final byte[] encrypt(byte[] data, byte[] key, String cipher) {
        long start_time = 0;

        if (tc.isDebugEnabled()) {
            start_time = System.currentTimeMillis();
            Tr.debug(tc, "Cipher used to encrypt: " + cipher);
            Tr.debug(tc, "Data size: " + data.length);
            Tr.debug(tc, "Key size: " + key.length);
        }

        if (null == data) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "data Array was null");
            return null;
        }

        byte[] mesg = null;
        try {

            SecretKey sKey = constructSecretKey(key, cipher);

            Cipher ci = createCipher(Cipher.ENCRYPT_MODE, key, cipher, sKey);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "encrypt() Cipher.doFinal()\n   data: " + new String(data));
            mesg = ci.doFinal(data);

        } catch (java.security.NoSuchAlgorithmException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2256");
        } catch (java.security.InvalidKeyException e) {
            Tr.debug(tc, "Error: Key invalid");
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2264");
        } catch (java.security.spec.InvalidKeySpecException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2267");
        } catch (javax.crypto.NoSuchPaddingException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2270");
        } catch (javax.crypto.IllegalBlockSizeException e) {
            // we get this exception when validating other token types
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2273");
        } catch (javax.crypto.BadPaddingException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2276");
        } catch (java.security.InvalidAlgorithmParameterException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2279");
        } catch (NoSuchProviderException e) {
            Tr.error(tc, "security.ltpa.noprovider", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2282");
        }

        if (tc.isDebugEnabled()) {
            long end_time = System.currentTimeMillis();
            Tr.debug(tc, "Total encryption time: " + (end_time - start_time));
        }

        return mesg;
    }

    static final byte[] decrypt(byte[] mesg, byte[] key) {
        return decrypt(mesg, key, CryptoUtils.AES_CBC_CIPHER);
    }

    static final byte[] decrypt(byte[] mesg, byte[] key, String cipher) {

        long start_time = 0;

        if (tc.isDebugEnabled()) {
            start_time = System.currentTimeMillis();
            Tr.debug(tc, "Cipher used to decrypt: " + cipher);
            Tr.debug(tc, "key size: " + key.length);
        }

        byte[] tmpMesg = null;
        try {

            SecretKey sKey = constructSecretKey(key, cipher);

            Cipher ci = createCipher(Cipher.DECRYPT_MODE, key, cipher, sKey);

            tmpMesg = ci.doFinal(mesg);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "decrypt() Cipher.doFinal()\n   tmpMesg: " + new String(tmpMesg));

        } catch (java.security.NoSuchAlgorithmException e) {
            Tr.error(tc, "no such algorithm exception", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2385");
        } catch (java.security.InvalidKeyException e) {
            Tr.debug(tc, "Error: Key invalid");
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2393");
        } catch (java.security.spec.InvalidKeySpecException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2396");
        } catch (javax.crypto.NoSuchPaddingException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2399");
        } catch (javax.crypto.IllegalBlockSizeException e) {
            // we get this exception when validating other token types
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2402");
        } catch (javax.crypto.BadPaddingException e) {
            Tr.debug(tc, "BadPaddingException validating token, normal when token generated from other factory.", new Object[] { e.getMessage() });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2405");
        } catch (java.security.InvalidAlgorithmParameterException e) {
            Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.auditAuditCrypto", "2408");
        } catch (NoSuchProviderException e) {
            Tr.error(tc, "security.ltpa.noprovider", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.auditAuditCrypto", "2412");
        }

        if (tc.isDebugEnabled()) {
            long end_time = System.currentTimeMillis();
            Tr.debug(tc, "Total decryption time: " + (end_time - start_time));
        }

        return tmpMesg;
    }

    /**
     * @param key
     * @param cipher
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static SecretKey constructSecretKey(byte[] key, String cipher) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        return new SecretKeySpec(key, 0, CryptoUtils.AES_256_KEY_LENGTH_BYTES, CryptoUtils.ENCRYPT_ALGORITHM_AES);
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
    private static Cipher createCipher(int cipherMode, byte[] key, String cipher,
                                       SecretKey sKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Cipher ci = Cipher.getInstance(cipher);

        setIVS16(key);
        ci.init(cipherMode, sKey, ivs16);

        return ci;
    }

    /**
     * Get 16 byte initialization vector
     **/
    public static IvParameterSpec getIVS16() {
        return ivs16;
    }

    /**
     * Set 16 byte initialization vector
     **/
    public static synchronized void setIVS16(byte[] key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setIVS16");

        try {
            byte[] iv16 = new byte[16];
            for (int i = 0; i < 16; i++) {
                iv16[i] = key[i];
            }
            ivs16 = new IvParameterSpec(iv16);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setIVS16: ivs16 successfully set");
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "setIVS16 unxepected exception setting initialization vector", new Object[] { e });
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.LTPAToken2Factory.initialize", "2568");
        }
    }
}