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
    private static final MessageDigest md1JCE;
    private static final MessageDigest md2JCE;
    private static final Object lockObj1;
    private static final Object lockObj2;

    private final short version = 1;
    private byte signature[];
    private byte[] encryptedBytes = null;
    private UserData userData;
    private long expirationInMinutes;
    private long expirationInMilliseconds;
    private long lastUsedInMilliseconds;

    private final int refreshThreshold;
    //private double thresholdPercentage;
    private boolean triggleRefresh = false;

    private final long refreshLifetimeInMinutes;
    private long refreshLifetimeInMilliseconds;
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
     * @param tokenBytes       The byte representation of the LTPA2 token
     * @param sharedKey        The LTPA shared key
     * @param privateKey       The LTPA private key
     * @param publicKey        The LTPA public key
     * @param refreshLifetime  TODO
     * @param refreshThreshold TODO
     */
    public LTPAToken2(byte[] tokenBytes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, long expDiffAllowed,
                      long refreshLifetime, int refreshThreshold) throws InvalidTokenException {
        checkTokenBytes(tokenBytes);
        this.signature = null;
        this.encryptedBytes = tokenBytes.clone();
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.refreshLifetimeInMinutes = refreshLifetime;
        this.refreshThreshold = refreshThreshold;
        this.expirationInMilliseconds = 0;
        this.lastUsedInMilliseconds = 0;
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        this.expirationDifferenceAllowed = expDiffAllowed;
        decrypt();
    }

    /**
     * An LTPA2 token constructor.
     *
     * @param tokenBytes       The byte representation of the LTPA2 token
     * @param sharedKey        The LTPA shared key
     * @param privateKey       The LTPA private key
     * @param publicKey        The LTPA public key
     * @param refreshLifetime  TODO
     * @param refreshThreshold TODO
     * @param attributes       The list of attributes will be removed from the LTPA2 token
     */
    public LTPAToken2(byte[] tokenBytes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, long expDiffAllowed,
                      long refreshLifetime, int refreshThreshold, String... attributes) throws InvalidTokenException, TokenExpiredException {
        checkTokenBytes(tokenBytes);
        this.signature = null;
        this.encryptedBytes = tokenBytes.clone();
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.expirationInMilliseconds = 0;
        this.lastUsedInMilliseconds = 0;
        this.refreshLifetimeInMinutes = refreshLifetime;
        this.refreshThreshold = refreshThreshold;
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        this.expirationDifferenceAllowed = expDiffAllowed;
        decrypt();
        isValid();
        //setLastUsed();
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
     * @param accessID            The unique user identifier
     * @param expirationInMinutes Expiration limit of the LTPA2 token in minutes
     * @param sharedKey           The LTPA shared key
     * @param privateKey          The LTPA private key
     * @param publicKey           The LTPA public key
     * @param refreshLifetime     TODO
     * @param refreshThreshold    TODO
     */
    protected LTPAToken2(String accessID, long expirationInMinutes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, long refreshLifetime,
                         int refreshThreshold) {
        this.signature = null;
        this.encryptedBytes = null;
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.userData = new UserData(accessID);
        this.expirationInMinutes = expirationInMinutes;
        this.refreshThreshold = refreshThreshold;
        this.refreshLifetimeInMinutes = refreshLifetime;
        setExpiration(expirationInMinutes);
        setRefreshLifetime(refreshLifetimeInMinutes);
        setLastUsed();
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
    }

    /**
     * An LTPA2 token constructor (Used for the clone).
     *
     * @param expirationInMinutes Expiration limit of the LTPA2 token in minutes
     * @param sharedKey           The LTPA shared key
     * @param privateKey          The LTPA private key
     * @param publicKey           The LTPA public key
     * @param userdata            The UserData
     * @param refreshLifetime     TODO
     * @param refreshThreshold    TODO
     */
    protected LTPAToken2(long expirationInMinutes, @Sensitive byte[] sharedKey, LTPAPrivateKey privateKey, LTPAPublicKey publicKey, UserData userdata, long refreshLifetime,
                         int refreshThreshold) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "<UTLE> LTPAToken2 call by the clone() method, userData: " + userdata);
        }
        this.signature = null;
        this.encryptedBytes = null;
        this.sharedKey = sharedKey.clone();
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.userData = userdata;
        this.expirationInMinutes = expirationInMinutes;
        this.refreshLifetimeInMinutes = refreshLifetime;
        this.refreshThreshold = refreshThreshold;
        setExpiration(expirationInMinutes);
        setRefreshLifetime(refreshLifetimeInMinutes);
        setLastUsed();
        this.cipher = CryptoUtils.AES_CBC_CIPHER;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.exit(this, tc, "<UTLE>LTPAToken2 call by the clone() method,  userData: " + this.userData);
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
                Tr.debug(this, tc, "expiration: " + new Date(expirationInMilliseconds));
            }
            String[] lastUsedArray = userData.getAttributes(AttributeNameConstants.WSTOKEN_LAST_USED);
            if (lastUsedArray != null && lastUsedArray[lastUsedArray.length - 1] != null) {
                lastUsedInMilliseconds = Long.parseLong(lastUsedArray[lastUsedArray.length - 1]);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "lastUsed: " + new Date(lastUsedInMilliseconds));
                }
            }
            //setLastUsed();
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
     * Checks if the token has expired.
     *
     * @throws TokenExpiredException
     */
    /*
     * public final void validateExpiration() throws TokenExpiredException {
     * Date lastUsedD = new Date(getLastUsed());
     * Date expD = new Date(getExpiration());
     * long currentTimeMillis = System.currentTimeMillis();
     * long idleMillis = currentTimeMillis - lastUsedInMilliseconds;
     * long idleTimeInMillis = lastUsedInMilliseconds + idleMillis;
     * Date idleD = new Date(idleTimeInMillis);
     * Date refreshD = new Date(refreshLifetimeInMilliseconds);
     *
     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
     * Tr.debug(this, tc, "last used time = " + lastUsedInMilliseconds + " idle time (mls) = " + idleMillis + " current time = " + currentTimeMillis + " expiration time = "
     * + expirationInMilliseconds + " refresh time = " + refreshLifetimeInMilliseconds);
     * Tr.debug(this, tc, "last used date = " + lastUsedD + " idle date = " + idleD + " expiration date = " + expD + " refresh time = " + refreshD);
     * }
     * boolean expired = idleTimeInMillis > expirationInMilliseconds;
     * boolean refreshAble = currentTimeMillis <= refreshLifetimeInMilliseconds;
     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
     * Tr.debug(this, tc, "expired = " + expired);
     * Tr.debug(this, tc, "refreshAble = " + refreshAble);
     * }
     * //boolean expired = idleD.after(expD);
     * if (expired) {
     * String msg = "The token has expired: last used time = \"" + lastUsedD + "\", idle time = \"" + idleD + "\", expire time = \"" + expD + "\"";
     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
     * Tr.debug(this, tc, msg);
     * }
     * throw new TokenExpiredException(expirationInMilliseconds, msg);
     * } else {
     *
     * }
     * }
     */
    public final void validateExpiration() throws TokenExpiredException {
        Date d = new Date();
        Date expD = new Date(getExpiration());
        boolean expired = d.after(expD);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Current time = " + d + ", expiration time = " + expD);
        }
        if (expired) {
            String msg = "The token has expired: current time = \"" + d + "\", expire time = \"" + expD + "\"";
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, msg);
            }
            throw new TokenExpiredException(expirationInMilliseconds, msg);
        }

        isRefreshNeed();
    }

    /**
     *
     */
    private void isRefreshNeed() {
        triggleRefresh = false;
        long expireTime = getExpiration();
        long currentTime = System.currentTimeMillis();
        double timeRemaining = expireTime - currentTime;
//        double expirationInMils = expirationInMinutes * 60 * 1000;
        double expirationInMils = 4 * 60 * 1000; //hard code expiration 4 mins - need to pass in expirationInMinutes for all new LTPAToken2()
        double remainPercentage = timeRemaining / expirationInMils;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "<UTLE> expire time: " + new Date(expireTime) + " expiration: " + expirationInMils / 1000 + " secons and remaining: "
                               + (timeRemaining / 1000 + " seconds"));
            Tr.debug(this, tc, "<UTLE> remaining percentage: " + remainPercentage + " remaining threshold percentage: " + getRefreshThresholdPercentage());
        }
        if (remainPercentage <= getRefreshThresholdPercentage()) {
            triggleRefresh = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "LTPA token refresh = " + triggleRefresh);
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

    /**
     * {@inheritDoc}
     *
     * @param refreshLifetimeInMilliseconds
     */
    public final long getRefreshLifetime() {
        return refreshLifetimeInMilliseconds;
    }

    /** {@inheritDoc} */
    @Override
    public final long getLastUsed() {
        return lastUsedInMilliseconds;
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
     * Make a deep copy of the LTPA2 token when necessary
     *
     * @return Object A new copy of the LTPA2 token
     */
    @Override
    public final Object clone() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.entry(this, tc, "<UTLE> clone(): userData: " + userData);
        }
        userData.removeAttributes("expire");
        userData.removeAttributes("lastUsed");
        //setExpiration(ex);
        //setLastUsed();
        UserData ud = (UserData) userData.clone();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(this, tc, "<UTLE> clone(): ud: " + ud);
        }

        return new LTPAToken2(expirationInMinutes, sharedKey, privateKey, publicKey, ud, refreshLifetimeInMinutes, refreshThreshold);
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
     * Set expiration limit of the LTPA2 token
     *
     * @param expirationInMinutes the expiration limit of the LTPA2 token in minutes
     */
    private final void setExpiration(long expirationInMinutes) {
        //TODO: UTLE - need the caller to pass in the refreshLifetimeInMilliseconds
        // if expiration time after the refreshLifetime, then set the expiration to refreshLifetime

        /*
         * Date expD = new Date(getExpiration());
         * Date refreshLifetimeD = new Date(refreshLifetimeInMilliseconds);
         *
         * if (expD.after(refreshLifetimeD)) {
         * expirationInMilliseconds = refreshLifetimeInMilliseconds;
         * } else {
         * expirationInMilliseconds = System.currentTimeMillis() + expirationInMinutes * 60 * 1000;
         * }
         */
        expirationInMilliseconds = System.currentTimeMillis() + expirationInMinutes * 60 * 1000;
        signature = null;
        if (userData != null) {
            encryptedBytes = null;
            userData.addAttribute("expire", Long.toString(expirationInMilliseconds));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Date expiredD = new Date(expirationInMilliseconds);
                Tr.exit(this, tc, "<UTLE> expired: " + expiredD);
            }
        } else {
            encryptedBytes = null;
        }
    }

    /**
     * Set last used to the current time
     */
    private final void setLastUsed() {
        lastUsedInMilliseconds = System.currentTimeMillis();
        signature = null;
        if (userData != null) {
            encryptedBytes = null;
            //userData.removeAttributes("lastUsed");
            userData.addAttribute("lastUsed", Long.toString(lastUsedInMilliseconds));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Date lastUsedD = new Date(lastUsedInMilliseconds);
                Tr.exit(this, tc, "<UTLE> lastUsed:  " + lastUsedD);
            }
        } else {
            encryptedBytes = null;
        }
    }

    private final void setRefreshLifetime(long refreshLifetimeInMinutes) {
        refreshLifetimeInMilliseconds = System.currentTimeMillis() + refreshLifetimeInMinutes * 60 * 1000;
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

    private double getRefreshThresholdPercentage() {
        return (double) refreshThreshold / 100;
    }

    @Override
    public boolean shouldRefreshToken() {
        return triggleRefresh;
    }

}
