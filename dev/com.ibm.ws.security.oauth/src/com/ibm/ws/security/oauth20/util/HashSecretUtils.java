/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Utility class to generate hash code for the client secret stored in the database or custom store
 */
public class HashSecretUtils {
    private static final TraceComponent tc = Tr.register(HashSecretUtils.class);

    /*
     * Hash information for client secret
     */
    public final static String PBKDF2WithHmacSHA512 = "PBKDF2WithHmacSHA512";
    public final static String DEFAULT_HASH = PBKDF2WithHmacSHA512;

    public static final int DEFAULT_SALTSIZE = 32;
    public static final int DEFAULT_ITERATIONS = 2048;
    public static final int DEFAULT_KEYSIZE = 32;

    private static final int generateSaltSize = DEFAULT_SALTSIZE;

    /**
     * Hash the client secret, if the client secret is encoded (XOR) and updateXORtoHash is true, decode and hash.
     *
     * @param clientSecret
     * @param clientId
     * @return
     */
    @Sensitive
    public static String hashSecret(@Sensitive String clientSecret, String clientId, boolean updateXORtoHash, @Sensitive JsonObject clientMetadataAsJson) {
        return hashSecret(clientSecret, clientId, updateXORtoHash, clientMetadataAsJson.get(OAuth20Constants.SALT).getAsString(), clientMetadataAsJson.get(OAuth20Constants.HASH_ALGORITHM).getAsString(), clientMetadataAsJson.get(OAuth20Constants.HASH_ITERATIONS).getAsInt(), clientMetadataAsJson.get(OAuth20Constants.HASH_LENGTH).getAsInt());
    }

    /**
     * Hash the client secret, if the client secret is encoded (XOR) and updateXORtoHash is true, decode and hash.
     *
     * @param clientSecret
     * @param clientId
     * @return
     */
    @Sensitive
    public static String hashSecret(@Sensitive String clientSecret, String clientId, boolean updateXORtoHash, @Sensitive String salt, @Sensitive String algorithm, int iteration, int length) {
        if (clientSecret != null && !clientSecret.isEmpty()) {
            if (salt == null) {
                // todo -- what exception/error to do here?
                throw new IllegalArgumentException("A null salt was provided for clientId " + clientId + ". Cannot hash secret.");
            }
            String secretToHash = clientSecret;

            /*
             * Check if the current secret is an XOR/encoded string. If it is and we should
             * update secrets to hash, decode the secret so we can hash the original string.
             *
             * If we're not updating XOR to hash, return the secret as it was passed in.
             *
             * If we hit an exception, return the secret as it was passed in so we're not
             * hashing an XOR string and creating a bad secret.
             */
            String secretType = PasswordUtil.getCryptoAlgorithm(clientSecret);

            if (secretType != null && secretType.equals(OAuth20Constants.XOR)) {
                if (updateXORtoHash) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Client secret for " + clientId + " is stored as XOR, convert to hash");
                    }
                    secretToHash = PasswordUtil.passwordDecode(clientSecret);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Client secret for " + clientId + " is stored as XOR, not converting to hash");
                    }
                    return clientSecret;
                }
            }

            if (secretType == null || !secretType.equals(OAuth20Constants.HASH)) {
                Map<String, String> hashProps = new HashMap<String, String>();
                hashProps.put(PasswordUtil.PROPERTY_HASH_ALGORITHM, algorithm == null ? DEFAULT_HASH : algorithm);
                hashProps.put(PasswordUtil.PROPERTY_HASH_SALT, salt);
                hashProps.put(PasswordUtil.PROPERTY_HASH_ITERATION, iteration == 0 ? String.valueOf(DEFAULT_ITERATIONS) : String.valueOf(iteration));
                hashProps.put(PasswordUtil.PROPERTY_HASH_LENGTH, length == 0 ? String.valueOf(DEFAULT_KEYSIZE) : String.valueOf(length));

                return PasswordUtil.encode_password(secretToHash, OAuth20Constants.HASH, hashProps);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Client secret for " + clientId + " is already hashed.");
                }
            }
        }
        return clientSecret;
    }

    /**
     * Hash the client secret if stored in the metadata field
     *
     * @param clientMetadataJson
     * @param clientSecretOnObject
     * @param clientId
     */
    public static void hashClientMetaTypeSecret(@Sensitive JsonObject clientMetadataAsJson, String clientId, boolean updateXORtoHash) {
        if (clientMetadataAsJson != null && clientMetadataAsJson.has(OAuth20Constants.CLIENT_SECRET)) {
            String clientSecret = clientMetadataAsJson.get(OAuth20Constants.CLIENT_SECRET).getAsString();
            if (clientSecret != null && !clientSecret.isEmpty()) {
                clientMetadataAsJson.addProperty(OAuth20Constants.CLIENT_SECRET, hashSecret(clientSecret, clientId, updateXORtoHash, clientMetadataAsJson.get(OAuth20Constants.SALT).getAsString(), clientMetadataAsJson.get(OAuth20Constants.HASH_ALGORITHM).getAsString(), clientMetadataAsJson.get(OAuth20Constants.HASH_ITERATIONS).getAsInt(), clientMetadataAsJson.get(OAuth20Constants.HASH_LENGTH).getAsInt()));
            }
        }
    }

    @Sensitive
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[generateSaltSize];
        random.nextBytes(salt);
        return Base64Coder.base64EncodeToString(salt);
    }

    @Sensitive
    public static JsonObject processMetatypeForHashInfo(@Sensitive JsonObject clientMetadataAsJson, String clientId, String algDefault, int itrDefault, int lengthDefault) {
        if (clientMetadataAsJson.get(OAuth20Constants.HASH_ALGORITHM) == null) {
            clientMetadataAsJson.addProperty(OAuth20Constants.HASH_ALGORITHM, algDefault);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added default hash algorithm for " + clientId + ": " + algDefault);
            }
        }

        if (clientMetadataAsJson.get(OAuth20Constants.SALT) == null) {
            clientMetadataAsJson.addProperty(OAuth20Constants.SALT, HashSecretUtils.generateSalt());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Salt added for " + clientId);
            }
        }

        if (clientMetadataAsJson.get(OAuth20Constants.HASH_ITERATIONS) == null || clientMetadataAsJson.get(OAuth20Constants.HASH_ITERATIONS).getAsInt() == 0) {
            clientMetadataAsJson.addProperty(OAuth20Constants.HASH_ITERATIONS, itrDefault);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added default hash iterations for " + clientId + ": " + itrDefault);
            }
        }

        if (clientMetadataAsJson.get(OAuth20Constants.HASH_LENGTH) == null || clientMetadataAsJson.get(OAuth20Constants.HASH_LENGTH).getAsInt() == 0) {
            clientMetadataAsJson.addProperty(OAuth20Constants.HASH_LENGTH, DEFAULT_KEYSIZE);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Added default hash key size for " + clientId + ": " + itrDefault);
            }
        }
        return clientMetadataAsJson;
    }
}
