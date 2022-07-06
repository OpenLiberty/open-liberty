/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.encoding.EncodingUtils;

public class EncryptionUtils {

    public static final TraceComponent tc = Tr.register(EncryptionUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String TRANSFORMATION_RSA = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION_AES = "AES/CBC/PKCS5Padding";

    private static final EncodingUtils encodingUtils = new EncodingUtils();

    @Trivial
    public String rsaEncrypt(PublicKey publicKey, @Sensitive String plaintext) throws Exception {
        String ciphertext = null;
        if (plaintext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Plaintext is null");
            }
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_RSA);
        if (publicKey != null) {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = getBytes(cipher, plaintext.getBytes("UTF-8"), 53); // RSA takes 53 bytes max for encrypting.
            ciphertext = encodingUtils.bytesToHexString(encryptedBytes);
        }
        return ciphertext;
    }

    @Trivial
    public String rsaDecrypt(PrivateKey privateKey, @Sensitive String ciphertext) throws Exception {
        if (ciphertext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Ciphertext is null");
            }
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_RSA);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = getBytes(cipher, hexStringToBytes(ciphertext), 64); // RSA takes 64 bytes max for decrypting
        return new String(decryptedBytes, "UTF-8");
    }

    @Trivial
    protected byte[] getBytes(Cipher cipher, byte[] inputBytes, int algMaxInputLength) throws Exception {
        if (inputBytes == null) {
            return null;
        }
        if (algMaxInputLength <= 0) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Algorithm output offset length was not positive");
            }
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int remaining = inputBytes.length;
        int inputLen = algMaxInputLength; // 53;
        int i = 0;

        while (i < inputBytes.length) {
            if (remaining < inputLen) {
                inputLen = remaining;
            }

            byte[] part = cipher.doFinal(inputBytes, i, inputLen);

            if (part != null) {
                baos.write(part);
            }
            i = i + inputLen;
            remaining = remaining - inputLen;
        }
        return baos.toByteArray();
    }

    public String aesEncrypt(String secret, @Sensitive String plaintext) throws Exception {
        String ciphertext = null;
        if (plaintext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Plaintext is null");
            }
            return null;
        }
        Key secretKey = getSecretKey(secret);
        if (secretKey != null) {
            IvParameterSpec ivSpec = getIvSpec(secret);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
            ciphertext = encodingUtils.bytesToHexString(encryptedBytes);
        }
        return ciphertext;
    }

    public String aesDecrypt(String secret, @Sensitive String ciphertext) throws Exception {
        if (ciphertext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Ciphertext is null");
            }
            return null;
        }
        Key secretKey = getSecretKey(secret);
        IvParameterSpec ivSpec = getIvSpec(secret);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(hexStringToBytes(ciphertext));
        return new String(decryptedBytes, "UTF-8");
    }

    Key getSecretKey(String clientSecret) throws Exception {
        byte[] clientSecretHash = getClientSecretHash(clientSecret);
        return AESKeyManager.getKey(encodingUtils.bytesToHexString(clientSecretHash));
    }

    IvParameterSpec getIvSpec(String clientSecret) throws Exception {
        byte[] clientSecretHash = getClientSecretHash(clientSecret);
        return AESKeyManager.getIV(encodingUtils.bytesToHexString(clientSecretHash));
    }

    byte[] getClientSecretHash(@Sensitive String clientSecret) {
        if (clientSecret == null) {
            return null;
        }
        MessageDigest md = getMessageDigest("SHA-256");
        if (md == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The secret key and initialization vector couldn't be initialized because a MessageDigest could not be created");
            }
            return null;
        }
        return md.digest(clientSecret.getBytes(Charset.forName("UTF-8")));
    }

    @FFDCIgnore(Exception.class)
    MessageDigest getMessageDigest(String algorithm) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A MessageDigest object failed to be acquired: " + e);
            }
        }
        return md;
    }

    @Trivial
    protected String bytesToHexString(byte[] bytes) {
        return encodingUtils.bytesToHexString(bytes);
    }

    @Trivial
    protected byte[] hexStringToBytes(String string) throws NumberFormatException {
        try {
            return encodingUtils.hexStringToBytes(string);
        } catch (NumberFormatException e) {
            String errorMsg = Tr.formatMessage(tc, "VALUE_NOT_HEXADECIMAL");
            throw new NumberFormatException(errorMsg);
        }
    }

}
