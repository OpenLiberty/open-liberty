/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
import com.ibm.ws.security.openidconnect.clients.common.EncryptionUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;

public class TAIEncryptionUtils {

    public static final TraceComponent tc = Tr.register(TAIEncryptionUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String ALG_RSA = "RSA";
    private static final String ALG_AES = "AES";

    private final EncodingUtils encodingUtils = new EncodingUtils();
    private final EncryptionUtils encryptionUtils = new EncryptionUtils();

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
        return encryptionUtils.rsaEncrypt(clientConfig.getPublicKey(), accessToken);
    }

    @Trivial
    protected String rsaDecrypt(SocialLoginConfig clientConfig, @Sensitive String encryptedToken) throws Exception {
        try {
            return encryptionUtils.rsaDecrypt(clientConfig.getPrivateKey(), encryptedToken);
        } catch (NumberFormatException e) {
            throw new SocialLoginException("VALUE_NOT_HEXADECIMAL", e, new Object[0]);
        }
    }

    @Trivial
    protected String aesEncrypt(SocialLoginConfig clientConfig, @Sensitive String accessToken) throws Exception {
        return encryptionUtils.aesEncrypt(clientConfig.getClientSecret(), accessToken);
    }

    @Trivial
    protected String aesDecrypt(SocialLoginConfig clientConfig, String encryptedToken) throws Exception {
        try {
            return encryptionUtils.aesDecrypt(clientConfig.getClientSecret(), encryptedToken);
        } catch (NumberFormatException e) {
            throw new SocialLoginException("VALUE_NOT_HEXADECIMAL", e, new Object[0]);
        }
    }

}
