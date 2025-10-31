/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import com.ibm.ws.common.crypto.CryptoUtils;

public final class UsernameTokenUtil {
    public static final int DEFAULT_ITERATION = 1000;

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(UsernameTokenUtil.class);

    private UsernameTokenUtil() {
        // complete
    }

    /**
     * This static method generates a derived key as defined in WSS Username
     * Token Profile.
     *
     * @param password The password to include in the key generation
     * @param salt The Salt value
     * @param iteration The Iteration value. If zero (0) is given the method uses the
     *                  default value
     * @return Returns the derived key a byte array
     * @throws WSSecurityException
     */
    public static byte[] generateDerivedKey(
        byte[] password,
        byte[] salt,
        int iteration
    ) throws WSSecurityException {
        byte[] pwSalt = new byte[salt.length + password.length];
        System.arraycopy(password, 0, pwSalt, 0, password.length);
        System.arraycopy(salt, 0, pwSalt, password.length, salt.length);

        MessageDigest sha = null;
        try {
            // Liberty Change Start: FIPS default to SHA256
            sha = CryptoUtils.isFips140_3EnabledWithBetaGuard() 
                ? MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256) 
                : MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA1);
            // Liberty Change End
        } catch (NoSuchAlgorithmException e) {
            LOG.debug(e.getMessage(), e);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e, "decoding.general"
            );
        }
        //
        // Make the first hash round with start value
        //
        byte[] k = sha.digest(pwSalt);
        //
        // Perform the 1st up to iteration-1 hash rounds
        //
        int iter = iteration;
        if (iter <= 0) {
            iter = DEFAULT_ITERATION;
        }
        for (int i = 1; i < iter; i++) {
            k = sha.digest(k);
        }
        return k;
    }

    /**
     * This static method generates a derived key as defined in WSS Username
     * Token Profile.
     *
     * @param password The password to include in the key generation
     * @param salt The Salt value
     * @param iteration The Iteration value. If zero (0) is given the method uses the
     *                  default value
     * @return Returns the derived key a byte array
     * @throws WSSecurityException
     */
    public static byte[] generateDerivedKey(
        String password,
        byte[] salt,
        int iteration
    ) throws WSSecurityException {
        return generateDerivedKey(password.getBytes(StandardCharsets.UTF_8), salt, iteration);
    }

    /**
     * This static method generates a 128 bit salt value as defined in WSS
     * Username Token Profile.
     *
     * @param useForMac If <code>true</code> define the Salt for use in a MAC
     * @return Returns the 128 bit salt value as byte array
     */
    public static byte[] generateSalt(boolean useForMac) {
        byte[] saltValue = null;
        try {
            saltValue = generateNonce(16);
        } catch (WSSecurityException ex) {
            LOG.debug(ex.getMessage(), ex);
            return new byte[0]; // Liberty Change: Backport 4.x
        }
        if (useForMac) {
            saltValue[0] = 0x01;
        } else {
            saltValue[0] = 0x02;
        }
        return saltValue;
    }

    /**
     * Generate a nonce of the given length using a secure random algorithm. The SecureRandom
     * instance that backs this method is cached for efficiency.
     *
     * @return a nonce of the given length
     * @throws WSSecurityException
     */
    public static byte[] generateNonce(int length) throws WSSecurityException { // Liberty Change: Backport 4.x
        try {
            return XMLSecurityConstants.generateBytes(length);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                    "empty", new Object[] {"Error in generating nonce of length " + length}
            );
        }
    }

    public static String doPasswordDigest(byte[] nonce, String created, String password) throws WSSecurityException {
        return doPasswordDigest(nonce, created, password.getBytes(StandardCharsets.UTF_8));
    }

    public static String doPasswordDigest(byte[] nonce, String created, byte[] password) throws WSSecurityException {
        byte[] digestBytes = doRawPasswordDigest(nonce, created, password);
        return org.apache.xml.security.utils.XMLUtils.encodeToString(digestBytes);
    }

    public static byte[] doRawPasswordDigest(byte[] nonce, String created, byte[] password) throws WSSecurityException {
        try {
            byte[] b1 = nonce != null ? nonce : new byte[0];
            byte[] b2 = created != null ? created.getBytes(StandardCharsets.UTF_8) : new byte[0];
            byte[] b3 = password;
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;

            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;

            System.arraycopy(b3, 0, b4, offset, b3.length);

            return KeyUtils.generateDigest(b4);
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "decoding.general");
        }
    }

    /**
     * Get the raw (plain text) password used to compute secret key.
     */
    public static String getRawPassword(CallbackHandler callbackHandler, String username,
                                        String password, String passwordType) throws WSSecurityException {
        if (callbackHandler == null) {
            LOG.debug("CallbackHandler is null");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        WSPasswordCallback pwCb =
            new WSPasswordCallback(
                username, password, passwordType, WSPasswordCallback.USERNAME_TOKEN
            );
        try {
            callbackHandler.handle(new Callback[]{pwCb});
        } catch (IOException | UnsupportedCallbackException e) {
            LOG.debug(e.getMessage(), e);
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e
            );
        }
        return pwCb.getPassword();
    }
}
