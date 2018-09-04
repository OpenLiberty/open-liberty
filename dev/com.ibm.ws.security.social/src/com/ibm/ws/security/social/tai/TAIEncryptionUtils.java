/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

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
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

public class TAIEncryptionUtils {

    public static final TraceComponent tc = Tr.register(TAIEncryptionUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String TRANSFORMATION_RSA = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION_AES = "AES/CBC/PKCS5Padding";
    private static final String ALG_RSA = "RSA";
    private static final String ALG_AES = "AES";

    private final EncodingUtils encodingUtils = new EncodingUtils();

    protected TAIEncryptionUtils() {
    }

    @Sensitive
    public String getEncryptedAccessToken(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
        if (accessToken == null) {
            throw new SocialLoginException("ACCESS_TOKEN_TO_ENCRYPT_IS_NULL", null, new Object[0]);
        }
        return getEncryptedAccessTokenUsingAlgorithm(clientConfig, accessToken, clientConfig.getAlgorithm());
    }

    String getEncryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String accessToken, String algorithm) throws SocialLoginException {
        String encryptedAccessToken = null;
        if (ALG_RSA.equals(algorithm)) {
            return encryptAccessTokenUsingRsa(clientConfig, accessToken);
        } else if (ALG_AES.equals(algorithm)) {
            return encryptAccessTokenUsingAes(clientConfig, accessToken);
        }
        return encryptedAccessToken;
    }

    @FFDCIgnore(SocialLoginException.class)
    String encryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
        try {
            return rsaEncrypt(clientConfig, accessToken);
        } catch (SocialLoginException e) {
            throw new SocialLoginException("ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_RSA", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_RSA", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    @FFDCIgnore(SocialLoginException.class)
    String encryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws SocialLoginException {
        try {
            return aesEncrypt(clientConfig, accessToken);
        } catch (SocialLoginException e) {
            throw new SocialLoginException("ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_AES", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN_AES", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    @Sensitive
    protected String getDecryptedAccessToken(SocialLoginConfig clientConfig, @Sensitive String encryptedToken) throws Exception {
        if (encryptedToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Provided token is null");
            }
            return null;
        }
        return getDecryptedAccessTokenUsingAlgorithm(clientConfig, encryptedToken, clientConfig.getAlgorithm());
    }

    String getDecryptedAccessTokenUsingAlgorithm(SocialLoginConfig clientConfig, @Sensitive String encryptedToken, String algorithm) throws SocialLoginException {
        String decryptedAccessToken = null;
        if (ALG_RSA.equals(algorithm)) {
            return decryptAccessTokenUsingRsa(clientConfig, encryptedToken);
        } else if (ALG_AES.equals(algorithm)) {
            return decryptAccessTokenUsingAes(clientConfig, encryptedToken);
        }
        return decryptedAccessToken;
    }

    @FFDCIgnore(SocialLoginException.class)
    String decryptAccessTokenUsingRsa(SocialLoginConfig clientConfig, @Sensitive String encryptedToken) throws SocialLoginException {
        try {
            return rsaDecrypt(clientConfig, encryptedToken);
        } catch (SocialLoginException e) {
            throw new SocialLoginException("ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_RSA", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_RSA", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    @FFDCIgnore(SocialLoginException.class)
    String decryptAccessTokenUsingAes(SocialLoginConfig clientConfig, @Sensitive String encryptedToken) throws SocialLoginException {
        try {
            return aesDecrypt(clientConfig, encryptedToken);
        } catch (SocialLoginException e) {
            throw new SocialLoginException("ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_AES", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        } catch (Exception e) {
            throw new SocialLoginException("ERROR_GETTING_DECRYPTED_ACCESS_TOKEN_AES", e, new Object[] { clientConfig.getUniqueId(), e.getLocalizedMessage() });
        }
    }

    @Trivial
    protected String rsaEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
        String encryptedAccessToken = null;
        if (accessToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Access token is null");
            }
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_RSA);
        PublicKey publicKey = clientConfig.getPublicKey();
        if (publicKey != null) {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = getBytes(cipher, accessToken.getBytes("UTF-8"), 53); // RSA takes 53 bytes max for encrypting.
            encryptedAccessToken = encodingUtils.bytesToHexString(encryptedBytes);
        }
        return encryptedAccessToken;
    }

    @Trivial
    protected String rsaDecrypt(SocialLoginConfig clientConfig, @Sensitive String encryptedToken) throws Exception {
        if (encryptedToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encrypted token is null");
            }
            return null;
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_RSA);
        PrivateKey privateKey = clientConfig.getPrivateKey();
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = getBytes(cipher, hexStringToBytes(encryptedToken), 64); // RSA takes 64 bytes max for decrypting
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

    @Trivial
    protected String aesEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
        String encryptedAccessToken = null;
        if (accessToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Access token is null");
            }
            return null;
        }
        Key secretKey = getSecretKey(clientConfig);
        if (secretKey != null) {
            IvParameterSpec ivSpec = getIvSpec(clientConfig);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(accessToken.getBytes("UTF-8"));
            encryptedAccessToken = encodingUtils.bytesToHexString(encryptedBytes);
        }
        return encryptedAccessToken;
    }

    @Trivial
    protected String aesDecrypt(SocialLoginConfig clientConfig, String encryptedToken) throws Exception {
        if (encryptedToken == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encrypted token is null");
            }
            return null;
        }
        Key secretKey = getSecretKey(clientConfig);
        IvParameterSpec ivSpec = getIvSpec(clientConfig);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(hexStringToBytes(encryptedToken));
        return new String(decryptedBytes, "UTF-8");
    }

    Key getSecretKey(SocialLoginConfig config) throws Exception {
        byte[] clientSecretHash = getClientSecretHash(config.getClientSecret());
        return AESKeyManager.getKey(encodingUtils.bytesToHexString(clientSecretHash));
    }

    IvParameterSpec getIvSpec(SocialLoginConfig config) throws Exception {
        byte[] clientSecretHash = getClientSecretHash(config.getClientSecret());
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
    protected byte[] hexStringToBytes(String string) throws SocialLoginException {
        try {
            return encodingUtils.hexStringToBytes(string);
        } catch (NumberFormatException e) {
            // The provided value is not in hexadecimal format.
            throw new SocialLoginException("VALUE_NOT_HEXADECIMAL", e, new Object[0]);
        }
    }

}
