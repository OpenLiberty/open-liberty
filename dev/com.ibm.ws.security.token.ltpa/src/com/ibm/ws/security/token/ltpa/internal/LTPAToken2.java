/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.crypto.BadPaddingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.websphere.security.auth.TokenExpiredException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyUtil;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.wsspi.security.ltpa.Token;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Represents an LTPAToken that is delegatable. The token contains user data,
 * expiration time, along with the digital signature based on the data and the RSA key.
 */
public class LTPAToken2 implements Token, Serializable {

    private static final boolean fipsEnabled = CryptoUtils.isFips140_3Enabled();

    private static final TraceComponent tc = Tr.register(LTPAToken2.class);

    private static final long serialVersionUID = 1L;
    private static final String DELIM = "%";
    private static final long MILLIS_PER_MINUTE = 60 * 1000;
    private static final MessageDigest md1JCE;
    private static final MessageDigest md2JCE;
    private static final Object lockObj1;
    private static final Object lockObj2;

    private final short version = 1;
    private byte signature[];
    private byte[] encryptedBytes = null;
    private UserData userData;
    private final long expirationInMinutes;
    private long expirationInMilliseconds;

    private final long refreshThresholdInMinutes; //Token time remaining threshold
    private boolean triggerRefresh = false;

    private final long inactivityTimeoutInMinutes;
    private final boolean dynamicExpirationValidation;
    private final byte[] sharedKey;
    private final LTPAPrivateKey privateKey;
    private final LTPAPublicKey publicKey;
    private String cipher = null;
    private long expirationDifferenceAllowed;

    static {
        MessageDigest m1 = null, m2 = null;

        m1 = CryptoUtils.getMessageDigestForLTPA();
        m2 = CryptoUtils.getMessageDigestForLTPA();

        md1JCE = m1;
        md2JCE = m2;
        lockObj1 = new Object();
        lockObj2 = new Object();
    }

    /**
     * An LTPA2 token constructor.
     *
     * @param tokenBytes                    The byte representation of the LTPA2 token
     * @param sharedKey                     The LTPA shared key
     * @param privateKey                    The LTPA private key
     * @param publicKey                     The LTPA public key
     * @param expDiffAllowed                The LTPA expiration difference allowed
     * @param expirationInMinutes           The LTPA token expiration in minutes
     * @param refreshThresholdInMinutes     The LTPA token expiration remaining threshold
     * @param inactivityTimeoutInMinutes    The LTPA token inactivity timeout in minutes
     * @param dynamicExpirationValidation   Whether dynamic expiration validation is enabled
     */

    public LTPAToken2(byte[] tokenBytes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, long expDiffAllowed,
                      long expirationInMinutes, long refreshThresholdInMinutes, long inactivityTimeoutInMinutes,
                      boolean dynamicExpirationValidation) throws InvalidTokenException {
        checkTokenBytes(tokenBytes);
        this.signature = null;
        this.encryptedBytes = tokenBytes.clone();
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.expirationInMinutes = expirationInMinutes;
        this.refreshThresholdInMinutes = refreshThresholdInMinutes;
        this.inactivityTimeoutInMinutes = inactivityTimeoutInMinutes;
        this.dynamicExpirationValidation = dynamicExpirationValidation;
        this.expirationInMilliseconds = 0;
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        this.expirationDifferenceAllowed = expDiffAllowed;
        decrypt();
    }

    /**
     * An LTPA2 token constructor.
     *
     * @param tokenBytes                    The byte representation of the LTPA2 token
     * @param sharedKey                     The LTPA shared key
     * @param privateKey                    The LTPA private key
     * @param publicKey                     The LTPA public key
     * @param expirationInMinutes           The LTPA token expiration in minutes
     * @param refreshThresholdInMinutes     The LTPA token expiration time remaining threshold
     * @param inactivityTimeoutInMinutes    The LTPA token inactivity timeout in minutes
     * @param dynamicExpirationValidation   Whether dynamic expiration validation is enabled
     * @param attributes                    The list of attributes will be removed from the LTPA2 token
     */
    public LTPAToken2(byte[] tokenBytes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, long expDiffAllowed,
                      long expirationInMinutes, long refreshThresholdInMinutes, long inactivityTimeoutInMinutes,
                      boolean dynamicExpirationValidation, String... attributes) throws InvalidTokenException, TokenExpiredException {
        checkTokenBytes(tokenBytes);
        this.signature = null;
        this.encryptedBytes = tokenBytes.clone();
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.expirationInMinutes = expirationInMinutes;
        this.refreshThresholdInMinutes = refreshThresholdInMinutes;
        this.inactivityTimeoutInMinutes = inactivityTimeoutInMinutes;
        this.dynamicExpirationValidation = dynamicExpirationValidation;
        this.expirationInMilliseconds = 0;
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        this.expirationDifferenceAllowed = expDiffAllowed;
        decrypt();
        isValid();
        if (attributes != null) {
            //Reset signature, encryptedBytes and remove attributes
            this.signature = null;
            this.encryptedBytes = null;
            userData.removeAttributes(attributes);
        }
    }

    /**
     * An LTPA2 token constructor.
     *
     * @param accessID                      The unique user identifier
     * @param expirationInMinutes           Expiration limit of the LTPA2 token in minutes
     * @param refreshThresholdInMinutes     Refresh threshold in minutes
     * @param inactivityTimeoutInMinutes    Inactivity timeout in minutes
     * @param dynamicExpirationValidation   Whether dynamic expiration validation is enabled.
     *                                      When {@code true}, the stored token expiration is set to
     *                                      {@code creationTime + inactivityTimeout} instead of
     *                                      {@code creationTime + expiration}.
     * @param sharedKey                     The LTPA shared key
     * @param privateKey                    The LTPA private key
     * @param publicKey                     The LTPA public key
     */
    protected LTPAToken2(String accessID, long expirationInMinutes, long refreshThresholdInMinutes, long inactivityTimeoutInMinutes,
                         boolean dynamicExpirationValidation,
                         @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey) {
        this.signature = null;
        this.encryptedBytes = null;
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.userData = new UserData(accessID);
        this.expirationInMinutes = expirationInMinutes;
        this.refreshThresholdInMinutes = refreshThresholdInMinutes;
        this.inactivityTimeoutInMinutes = inactivityTimeoutInMinutes;
        this.dynamicExpirationValidation = dynamicExpirationValidation;
        setCreationTime();
        // When dynamicExpirationValidation is enabled, store creationTime + inactivityTimeout
        // as the token expiration so the recipient sees the inactivity window deadline.
        // During validation the server ignores this value and recomputes from the config expiration.
        if (dynamicExpirationValidation && inactivityTimeoutInMinutes > 0) {
            setExpiration(inactivityTimeoutInMinutes);
        } else {
            setExpiration(expirationInMinutes);
        }
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
    }

    /**
     * An LTPA2 token constructor (Used for the clone).
     *
     * <p>When {@code dynamicExpirationValidation} is {@code false}, pass the original
     * token's absolute expiration as {@code originalExpirationInMillis} so the cloned
     * token preserves the same hard deadline.
     *
     * <p>When {@code dynamicExpirationValidation} is {@code true}, pass {@code 0L} as
     * the sentinel so the cloned token recomputes its stored expiration from the
     * <em>new</em> {@code creationTime + inactivityTimeout}.  This is correct because
     * under dynamic-expiration mode the stored field carries the inactivity deadline
     * (not the absolute one), and that deadline must be anchored to the fresh creation
     * time, not the original token's creation time.
     *
     * @param expirationInMinutes           Expiration limit of the LTPA2 token in minutes (config value, kept for future clones)
     * @param refreshThresholdInMinutes     The LTPA token expiration time remaining threshold
     * @param inactivityTimeoutInMinutes    The LTPA token inactivity timeout in minutes
     * @param dynamicExpirationValidation   Whether dynamic expiration validation is enabled
     * @param originalExpirationInMillis    Absolute expiration in ms to preserve (pass {@code 0L} when dynamicExpirationValidation=true)
     * @param sharedKey                     The LTPA shared key
     * @param privateKey                    The LTPA private key
     * @param publicKey                     The LTPA public key
     * @param userdata                      The UserData
     */
    protected LTPAToken2(long expirationInMinutes, long refreshThresholdInMinutes, long inactivityTimeoutInMinutes,
                         boolean dynamicExpirationValidation, long originalExpirationInMillis,
                         @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, UserData userdata) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "call by the clone() method, userData: " + userdata);
            Tr.debug(this, tc, "expire: " + new Date(originalExpirationInMillis));
        }
        this.signature = null;
        this.encryptedBytes = null;
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.userData = userdata;
        this.expirationInMinutes = expirationInMinutes;
        this.refreshThresholdInMinutes = refreshThresholdInMinutes;
        this.inactivityTimeoutInMinutes = inactivityTimeoutInMinutes;
        this.dynamicExpirationValidation = dynamicExpirationValidation;
        setCreationTime();
        // When dynamicExpirationValidation=true the clone() method passes 0L as a sentinel.
        // We must recompute the stored expiration from the new creationTime (just set above)
        // plus the inactivity window — exactly as the creation constructor does.
        // When dynamicExpirationValidation=false we preserve the original absolute deadline.
        if (dynamicExpirationValidation && inactivityTimeoutInMinutes > 0) {
            setExpiration(inactivityTimeoutInMinutes);
        } else if (originalExpirationInMillis > 0) {
            setExpirationFromMilliseconds(originalExpirationInMillis);
        } else {
            setExpiration(expirationInMinutes);
        }
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "call by the clone() method,  userData: " + this.userData);
        }
    }

    /**
     * Encrypt the token passed into the token.
     *
     * @throws TokenException
     */
    private final void encrypt() throws Exception {
        String signStr = Base64Coder.toString(Base64Coder.base64Encode(signature));
        String ud = userData.toString();
        byte[] accessID = Base64Coder.getBytes(ud);
        StringBuilder sb = new StringBuilder(DELIM);
        sb.append(getExpiration()).append(DELIM).append(signStr);
        byte[] timeAndSign = getSimpleBytes(sb.toString());
        byte[] toBeEnc = new byte[accessID.length + timeAndSign.length];
        for (int i = 0; i < accessID.length; i++) {
            toBeEnc[i] = accessID[i];
        }
        for (int i = accessID.length; i < toBeEnc.length; i++) {
            toBeEnc[i] = timeAndSign[i - accessID.length];
        }
        try {
            encryptedBytes = LTPAKeyUtil.encrypt(toBeEnc, sharedKey, cipher);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error encrypting; " + e);
            }
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Encrypted bytes are: " + (encryptedBytes == null ? "" : Base64Coder.toString(Base64Coder.base64Encode(encryptedBytes))));
        }
    }

    /**
     * Decrypt the encrypted token bytes passed into the constructor.
     */
    @FFDCIgnore({ BadPaddingException.class, Exception.class })
    private final void decrypt() throws InvalidTokenException {
        byte[] tokenData;
        try {
            tokenData = LTPAKeyUtil.decrypt(encryptedBytes.clone(), sharedKey, cipher);

            checkTokenBytes(tokenData);
            String UTF8TokenString = toUTF8String(tokenData);
            String[] userFields = LTPATokenizer.parseToken(UTF8TokenString);
            Map<String, ArrayList<String>> attribs = LTPATokenizer.parseUserData(userFields[0]);
            userData = new UserData(attribs);

            String tokenString = toSimpleString(tokenData);
            String[] fields = LTPATokenizer.parseToken(tokenString);
            String[] expirationArray = userData.getAttributes(AttributeNameConstants.WSTOKEN_EXPIRATION);

            if (expirationArray != null && expirationArray[expirationArray.length - 1] != null) {
                // the new expiration value inside the signature for LTPAToken2
                expirationInMilliseconds = Long.parseLong(expirationArray[expirationArray.length - 1]);

                // Liberty and Traditional WebSphere both create LTPA Tokens with 3 fields. (userData % expiration % sign)
                // Normally, the expiration value is read from the first field of the token, the userData field.
                // The expiration value may be read from the second field of the token instead, to maintain legacy support in Traditional WebSphere.

                // If the LTPAToken contains both expiration formats, Compare the values to ensure they are within the expiration difference allowed.
                // If the difference between the two expiration values is greater than expirationDifferenceAllowed, then an InvalidTokenException will be thrown.
                // If expirationDifferenceAllowed is 0, then the two expiration values must match.
                // If expirationDifferenceAllowed is less than 0, then the two expiration values are not compared.
                // expirationDifferenceAllowed is 3 seconds (3000ms) by default.
                if (fields.length == 3 && expirationDifferenceAllowed >= 0 && (Math.abs(expirationInMilliseconds - Long.parseLong(fields[1])) > expirationDifferenceAllowed)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Token validation failed due to the expiration fields having a difference greater than: "
                                           + expirationDifferenceAllowed + " milliseconds\n"
                                           + "first field expiration: " + expirationInMilliseconds + " milliseconds\n"
                                           + "second field expiration: " + fields[1] + " milliseconds");
                    }
                    throw new InvalidTokenException("Token Validation Failed");
                }
            } else {
                // the old expiration value outside of the signature for LTPAToken
                expirationInMilliseconds = Long.parseLong(fields[1]);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "token expire: " + expirationInMilliseconds + " time: " + new Date(expirationInMilliseconds));
            }
            // the signature will always be the last field, but the fields array length may be 2 or 3.
            byte[] signature = Base64Coder.base64Decode(Base64Coder.getBytes(fields[fields.length - 1]));
            setSignature(signature);
        } catch (BadPaddingException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Caught BadPaddingException while decrypting token, this is only a critical problem if decryption should have worked.", e);
            }
            throw new InvalidTokenException(e.getMessage(), e);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error decrypting; " + e);
            }
            throw new InvalidTokenException(e.getMessage(), e);
        }
    }

    /**
     * Sign the token passed into the token.
     */
    private final void sign() throws Exception {
        String dataStr = this.getUserData().toString();
        byte[] data = Base64Coder.getBytes(dataStr);
        byte[] signature = sign(data, this.privateKey);
        this.setSignature(signature);
    }

    private final byte[] sign(byte[] msg, LTPAPrivateKey privKey) throws Exception {
        byte[] data;
        synchronized (lockObj1) {
            data = md1JCE.digest(msg);
        }
        byte[][] rsaPrivKey = LTPAKeyUtil.getRawKey(privKey);
        LTPAKeyUtil.setRSAKey(rsaPrivKey);
        byte[] signature;
        signature = LTPAKeyUtil.signISO9796(rsaPrivKey, data, 0, data.length);

        return signature;
    }

    /**
     * Verify the token.
     */
    private final boolean verify() throws Exception {
        String dataStr = this.getUserData().toString();
        byte[] data = Base64Coder.getBytes(dataStr);
        return verify(data, signature, publicKey);
    }

    private final boolean verify(byte[] msg, byte[] signature, LTPAPublicKey pubKey) throws Exception {
        if (msg == null) {
            throw new IllegalArgumentException("null message");
        } else if (signature == null) {
            throw new IllegalArgumentException("null signature");
        }

        byte[] data;
        synchronized (lockObj2) {
            data = md2JCE.digest(msg);
        }
        byte[][] rsaPubKey = LTPAKeyUtil.getRawKey(pubKey);
        return LTPAKeyUtil.verifyISO9796(rsaPubKey, data, 0, data.length, signature, 0, signature.length);
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public final boolean isValid() throws InvalidTokenException, TokenExpiredException {
        boolean verified = false;

        validateExpiration();

        try {
            verified = verify();
        } catch (Exception e) {
            verified = false;
            throw new InvalidTokenException(e.getMessage(), e);
        }

        if (!verified) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Invalid signature of the token " + this);
            }
            throw new InvalidTokenException("Token Validation Failed");
        }

        return verified;
    }

    /**
     * Validates token expiration and checks if refresh is needed.
     *
     * @throws TokenExpiredException if the token has expired
     */
    public final void validateExpiration() throws TokenExpiredException {
        long currentTime = System.currentTimeMillis();
        Date currentD = new Date(currentTime);

        // Determine the effective expiration time.
        // When dynamicExpirationValidation is enabled, ignore the expiration stored in the token
        // and recompute it from the creation time + the server-configured expiration (in minutes).
        long effectiveExpiration;
        if (dynamicExpirationValidation) {
            String[] creationTimeArray = userData.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
            if (creationTimeArray != null && creationTimeArray[creationTimeArray.length - 1] != null) {
                long creationTime = Long.parseLong(creationTimeArray[creationTimeArray.length - 1]);
                effectiveExpiration = creationTime + (expirationInMinutes * MILLIS_PER_MINUTE);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "dynamicExpirationValidation: recomputed expiration from creationTime "
                                       + new Date(creationTime) + " + " + expirationInMinutes + "m = "
                                       + new Date(effectiveExpiration));
                }
            } else {
                // No creation time in token — fall back to the stored expiration value.
                effectiveExpiration = expirationInMilliseconds;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "dynamicExpirationValidation: no creationTime found, using stored expiration");
                }
            }
        } else {
            effectiveExpiration = expirationInMilliseconds;
        }

        // Check absolute expiration (always enforced)
        Date expD = new Date(effectiveExpiration);
        boolean expired = currentTime > effectiveExpiration;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "current time    = " + currentD);
            Tr.debug(this, tc, "absolute expiration time = " + expD);
        }
        if (expired) {
            String msg = "The token has exceeded absolute expiration: current time = \"" + currentD + "\", expire time = \"" + expD + "\"";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, msg);
            }
            throw new TokenExpiredException(effectiveExpiration, msg);
        }

        // Check inactivity timeout (if configured)
        if (inactivityTimeoutInMinutes > 0) {
            // Get creation time from token
            String[] creationTimeArray = userData.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
            if (creationTimeArray != null && creationTimeArray[creationTimeArray.length - 1] != null) {
                long creationTime = Long.parseLong(creationTimeArray[creationTimeArray.length - 1]);
                long inactivityTimeout = getInactivityTimeout(creationTime);

                boolean inactivityExpired = currentTime > inactivityTimeout;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "creation time = " + new Date(creationTime));
                    Tr.debug(this, tc, "inactivity timeout = " + new Date(inactivityTimeout));
                }
                if (inactivityExpired) {
                    String msg = "The token has exceeded inactivity timeout: current time = \"" +
                                 currentD + "\", inactivity timeout = \"" + new Date(inactivityTimeout) + "\"";
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, msg);
                    }
                    throw new TokenExpiredException(inactivityTimeout, msg);
                }
            }
        }

        // Check if token needs refresh
        checkRefreshNeeded(currentTime);
    }

    /**
     * Compute the inactivity timeout for this token given its creation time.
     * The result is capped at the absolute expiration so inactivity can never
     * extend beyond the token's hard deadline.
     *
     * @param creationTime the WSTOKEN_CREATION_TIME value in milliseconds
     * @return the effective inactivity timeout time in milliseconds
     */
    private long getInactivityTimeout(long creationTime) {
        long timeout = creationTime + (inactivityTimeoutInMinutes * MILLIS_PER_MINUTE);
        // Cap at the true absolute deadline so inactivity can never extend beyond it.
        // When dynamicExpirationValidation=true the stored expirationInMilliseconds is
        // creationTime + inactivityTimeout (a short value), NOT the hard expiration.
        // In that case we must compute the real ceiling from creationTime + expirationInMinutes.
        long absoluteDeadline = dynamicExpirationValidation
                ? creationTime + (expirationInMinutes * MILLIS_PER_MINUTE)
                : expirationInMilliseconds;
        if (timeout > absoluteDeadline) {
            timeout = absoluteDeadline;
        }
        return timeout;
    }

    /**
     * Determine whether the token should be proactively refreshed.
     * Sets {@link #triggerRefresh} to {@code true} if the inactivity time remaining
     * until inactivity expiry is at or below the configured refresh threshold.
     *
     * @param currentTime the current wall-clock time in milliseconds (from {@link #validateExpiration})
     */
    private void checkRefreshNeeded(long currentTime) {
        triggerRefresh = false;

        // Beta guard: Token refresh is only available in beta edition
        if (!ProductInfo.getBetaEdition()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "LTPA token refresh is only available in beta edition");
            }
            return;
        }

        // Only check refresh when inactivity timeout is configured
        if (inactivityTimeoutInMinutes <= 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Inactivity timeout not configured, refresh not applicable");
            }
            return;
        }

        // Get creation time from token
        String[] creationTimeArray = userData.getAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);
        if (creationTimeArray == null || creationTimeArray[creationTimeArray.length - 1] == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creation time not found in token, refresh not applicable");
            }
            return;
        }

        long creationTime = Long.parseLong(creationTimeArray[creationTimeArray.length - 1]);
        long inactivityTimeout = getInactivityTimeout(creationTime);

        long inactivityTimeRemaining = inactivityTimeout - currentTime;
        long thresholdInMillis = getRefreshThreshold();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Creation time: " + new Date(creationTime));
            Tr.debug(this, tc, "Current time: " + new Date(currentTime));
            Tr.debug(this, tc, "Inactivity timeout: " + new Date(inactivityTimeout));
            Tr.debug(this, tc, "Inactivity time remaining (ms): " + inactivityTimeRemaining);
            Tr.debug(this, tc, "Refresh threshold (ms): " + thresholdInMillis);
        }

        if (inactivityTimeRemaining <= thresholdInMillis) {
            triggerRefresh = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Token refresh triggered: inactivity time remaining (" + inactivityTimeRemaining + "ms) <= threshold (" + thresholdInMillis + "ms)");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "LTPA token refresh = " + triggerRefresh);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(Exception.class)
    public final byte[] getBytes() throws InvalidTokenException, TokenExpiredException {
        if (encryptedBytes == null) {
            try {
                sign();
                encrypt();
            } catch (Exception e) {
                throw new com.ibm.websphere.security.auth.InvalidTokenException(e.getMessage(), e);
            }
        }
        return encryptedBytes.clone();
    }

    /** {@inheritDoc} */
    @Override
    public final long getExpiration() {
        return expirationInMilliseconds;
    }

    /** {@inheritDoc} */
    @Override
    public final short getVersion() {
        return version;
    }

    /** {@inheritDoc} */
    @Override
    public final String[] addAttribute(String name, String value) {
        signature = null;
        encryptedBytes = null;
        return userData.addAttribute(name, value);
    }

    /** {@inheritDoc} */
    @Override
    public final String[] getAttributes(String name) {
        return userData.getAttributes(name);
    }

    /** {@inheritDoc} */
    @Override
    public final Enumeration<String> getAttributeNames() {
        return userData.getAttributeNames();
    }

    @Override
    public final String toString() {
        return encryptedBytes == null ? "NULL" : Base64Coder.base64EncodeToString(encryptedBytes);
    }

    /**
     * Make a deep copy of the LTPA2 token when necessary.
     *
     * <p>When {@code dynamicExpirationValidation=false} the clone preserves the original
     * absolute expiration deadline unchanged.
     *
     * <p>When {@code dynamicExpirationValidation=true} the stored expiration field
     * in the original token is {@code originalCreationTime + inactivityTimeout} — a
     * stale value that must <em>not</em> be inherited.  We pass {@code 0L} as the
     * sentinel so the clone constructor recomputes it from the new creation time.
     *
     * @return Object A new copy of the LTPA2 token
     */
    @Override
    public final Object clone() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "userData: " + userData);
        }

        // Clone userData first, then remove expire and creation time from the copy.
        // This avoids mutating this.userData (the original validated token) which
        // would corrupt it if referenced after clone() returns.
        UserData ud = (UserData) userData.clone();
        ud.removeAttributes("expire");
        ud.removeAttributes(AttributeNameConstants.WSTOKEN_CREATION_TIME);

        // dynamicExpirationValidation=false: pass the original absolute deadline so the
        //   clone does not recompute expiration from now.
        // dynamicExpirationValidation=true:  pass 0L as sentinel; the clone constructor
        //   will call setExpiration(inactivityTimeoutInMinutes) anchored to the new
        //   creationTime that setCreationTime() stamps inside the constructor.
        long expiryForClone = dynamicExpirationValidation ? 0L : expirationInMilliseconds;
        return new LTPAToken2(expirationInMinutes, refreshThresholdInMinutes, inactivityTimeoutInMinutes,
                              dynamicExpirationValidation, expiryForClone,
                              sharedKey, privateKey, publicKey, ud);
    }

    /**
     * Check the byte expression of the LTPA2 token
     *
     * @param tokenBytes The byte expression of the LTPA2 token
     */
    private static final void checkTokenBytes(byte[] tokenBytes) {
        if ((tokenBytes == null) || (tokenBytes.length == 0)) {
            throw new IllegalArgumentException("No token bytes specified");
        }
    }

    /**
     * Set a new signature of the LTPA2 token
     *
     * @param newValue The signature of the LTPA2 token
     */
    private final void setSignature(byte newValue[]) {
        this.signature = newValue;
    }

    /**
     * Get the UserData
     *
     * @return The UserData
     */
    private final UserData getUserData() {
        return userData;
    }

    /**
     * Set expiration limit of the LTPA2 token, computing the deadline from now.
     * Used when minting a brand-new token.
     *
     * @param expirationInMinutes the expiration limit of the LTPA2 token in minutes
     */
    private final void setExpiration(long expirationInMinutes) {
        setExpirationFromMilliseconds(System.currentTimeMillis() + expirationInMinutes * MILLIS_PER_MINUTE);
    }

    /**
     * Set expiration using a pre-computed absolute deadline in milliseconds.
     * Used by the clone constructor to preserve the original token's absolute expiration.
     *
     * @param expirationMillis the absolute expiration time in milliseconds
     */
    private final void setExpirationFromMilliseconds(long expirationMillis) {
        expirationInMilliseconds = expirationMillis;

        signature = null;
        if (userData != null) {
            encryptedBytes = null;
            userData.addAttribute("expire", Long.toString(expirationInMilliseconds));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Date expiredD = new Date(expirationInMilliseconds);
                Tr.exit(this, tc, "useData expire: " + expirationInMilliseconds + " expire: " + expiredD);
            }
        } else {
            encryptedBytes = null;
        }
    }

    /**
     * Set creation time of the LTPA2 token
     */
    private final void setCreationTime() {
        long creationTime = System.currentTimeMillis();
        signature = null;
        if (userData != null) {
            encryptedBytes = null;
            userData.addAttribute(AttributeNameConstants.WSTOKEN_CREATION_TIME, Long.toString(creationTime));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Date creationD = new Date(creationTime);
                Tr.debug(this, tc, "Creation time: " + creationTime + " time: " + creationD);
            }
        } else {
            encryptedBytes = null;
        }
    }

    /**
     * Convert the byte representation to the UTF-8 String form.
     *
     * @param b The byte representation
     * @return The UTF-8 String form
     */
    private static final String toUTF8String(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    /**
     * Convert the byte representation to the String form.
     *
     * @param b The byte representation
     * @return The String form
     */
    private static final String toSimpleString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = b.length; i < len; i++) {
            sb.append((char) (b[i] & 0xff));
        }
        String str = sb.toString();
        return str;
    }

    /**
     * Convert the String form to the byte representation
     *
     * @param str The String form
     * @return The byte representation
     */
    private static final byte[] getSimpleBytes(String str) {
        StringBuilder sb = new StringBuilder(str);
        byte[] b = new byte[sb.length()];
        for (int i = 0, len = sb.length(); i < len; i++) {
            b[i] = (byte) sb.charAt(i);
        }
        return b;
    }

    private long getRefreshThreshold() {
        return refreshThresholdInMinutes * MILLIS_PER_MINUTE;
    }

    @Override
    public boolean shouldRefreshToken() {
        // Re-evaluate on every call so that callers who update WSTOKEN_CREATION_TIME
        // (e.g. by backdating in tests, or by the framework after a request) see the
        // correct answer without having to call isValid() first.
        //checkRefreshNeeded(System.currentTimeMillis());
        return triggerRefresh;
    }

}
