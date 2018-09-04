/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.token.impl.IdTokenImpl;

public class IDToken extends JWT {

    private static final TraceComponent tc = Tr.register(IDToken.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    Payload payload = null;
    String ath = null;

    public IDToken(JWSHeader header, Payload payload, @Sensitive Object key) {
        super(header, payload, key);
        this.payload = payload;
    }

    public IDToken(JWSHeader header, Payload payload) {
        super(header, payload);
        this.payload = payload;
    }

    public IDToken(JWSHeader header, Payload payload, @Sensitive Object Key, String accessToken) {
        super(header, payload, Key);
        this.payload = payload;
        // hash access token
        if (accessToken != null) {
            this.payload.setAccessTokenHash(accessTokenHash(accessToken));
        }
    }

    // Parse the signed IDToken string to construct token
    public IDToken(String tokenString, @Sensitive Object key, String clientId, String issuer, String signingAlgorithm) {
        super(tokenString, key, clientId, issuer, signingAlgorithm);
        payload = new Payload();
    }

    // Parse the plain text token string to construct token
    public IDToken(String tokenString, @Sensitive String clientId, String issuer, String signingAlgorithm) {
        super(tokenString, clientId, issuer, signingAlgorithm);
        payload = new Payload();
    }

    // Parse the signed IDToken with access token string to construct token
    public IDToken(String tokenString, @Sensitive Object key, String clientId, String issuer, String signingAlgorithm, String accessToken) {
        super(tokenString, key, clientId, issuer, signingAlgorithm);
        payload = new Payload();
        if (accessToken != null) {
            this.ath = accessTokenHash(accessToken);
        }
    }

    public String accessTokenHash(String accessToken) {
        return JsonTokenUtil.accessTokenHash(accessToken);
    }

    public boolean verifyAccessTokenHash(String accessTokenHash) {
        String decodedAccessTokenHash = JsonTokenUtil.decodeFromBase64String(accessTokenHash);
        String ourDecodedAccessTokenHash = JsonTokenUtil.decodeFromBase64String(this.ath);
        if (ourDecodedAccessTokenHash != null) {
            if (ourDecodedAccessTokenHash.equalsIgnoreCase(decodedAccessTokenHash))
                return true;
        }

        return false;
    }

    /**
     * Returns the payload.
     */
    @Override
    public Payload getPayload() {
        return payload;
    }

    @FFDCIgnore(IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    protected void addToPayloadFields(String key) {
        try {
            PayloadParameter param = PayloadParameter.valueOf(key.toUpperCase());
            //EXP, NBF, IAT, ISS, AUD, JTI, TYP, SUB, AUTH_TIME, AZP, NONCE, AT_HASH, ACR, AMR;
            switch (param) {
            case EXP:
                payload.setExpirationTimeSeconds((Long) payload.get(PayloadConstants.EXPIRATION_TIME_IN_SECS));
                break;
            case NBF:
                payload.setNotBeforeTimeSeconds((Long) payload.get(PayloadConstants.NOT_BEFORE_TIME_IN_SECS));
                break;
            case IAT:
                payload.setIssuedAtTimeSeconds((Long) payload.get(PayloadConstants.ISSUED_AT_TIME_IN_SECS));
                break;
            case ISS:
                payload.setIssuer((String) payload.get(PayloadConstants.ISSUER));
                break;
            case AUD:
                payload.setAudience(payload.get(PayloadConstants.AUDIENCE));
                break;
            case JTI:
                payload.setJwtId((String) payload.get(PayloadConstants.JWTID));
                break;
            case TYP:
                payload.setType((String) payload.get(PayloadConstants.TYPE));
                break;
            case SUB:
                payload.setSubject((String) payload.get(PayloadConstants.SUBJECT));
                break;
            case AUTH_TIME:
                payload.setAuthorizationTimeSeconds((Long) payload.get(PayloadConstants.AUTHZ_TIME_IN_SECS));
                break;
            case AZP:
                payload.setAuthorizedParty((String) payload.get(PayloadConstants.AUTHORIZED_PARTY));
                break;
            case NONCE:
                payload.setNonce((String) payload.get(PayloadConstants.NONCE));
                break;
            case AT_HASH:
                payload.setAccessTokenHash((String) payload.get(PayloadConstants.AT_HASH));
                break;
            case ACR:
                payload.setClassReference((String) payload.get(PayloadConstants.CLASS_REFERENCE));
                break;
            case AMR:
                payload.setMethodsReferences((List<String>) payload.get(PayloadConstants.METHODS_REFERENCE));
                break;
            default:
                break;
            }
        } catch (IllegalArgumentException e) {
            // Ignore since it is a custom claim
        }
    }

    @FFDCIgnore(IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    protected void addToPayloadFields(IdTokenImpl idTokenImpl, String key) {
        try {
            PayloadParameter param = PayloadParameter.valueOf(key.toUpperCase());
            //EXP, NBF, IAT, ISS, AUD, JTI, TYP, SUB, AUTH_TIME, AZP, NONCE, AT_HASH, ACR, AMR;
            switch (param) {
            case EXP:
                idTokenImpl.setExpirationTimeSeconds((Long) payload.get(PayloadConstants.EXPIRATION_TIME_IN_SECS));
                break;
            case NBF:
                idTokenImpl.setNotBeforeTimeSeconds((Long) payload.get(PayloadConstants.NOT_BEFORE_TIME_IN_SECS));
                break;
            case IAT:
                idTokenImpl.setIssuedAtTimeSeconds((Long) payload.get(PayloadConstants.ISSUED_AT_TIME_IN_SECS));
                break;
            case ISS:
                idTokenImpl.setIssuer((String) payload.get(PayloadConstants.ISSUER));
                break;
            case AUD:
                idTokenImpl.setAudience(payload.get(PayloadConstants.AUDIENCE));
                break;
            case JTI:
                idTokenImpl.setJwtId((String) payload.get(PayloadConstants.JWTID));
                break;
            case TYP:
                idTokenImpl.setType((String) payload.get(PayloadConstants.TYPE));
                break;
            case SUB:
                idTokenImpl.setSubject((String) payload.get(PayloadConstants.SUBJECT));
                break;
            case AUTH_TIME:
                idTokenImpl.setAuthorizationTimeSeconds((Long) payload.get(PayloadConstants.AUTHZ_TIME_IN_SECS));
                break;
            case AZP:
                idTokenImpl.setAuthorizedParty((String) payload.get(PayloadConstants.AUTHORIZED_PARTY));
                break;
            case NONCE:
                idTokenImpl.setNonce((String) payload.get(PayloadConstants.NONCE));
                break;
            case AT_HASH:
                idTokenImpl.setAccessTokenHash((String) payload.get(PayloadConstants.AT_HASH));
                break;
            case ACR:
                idTokenImpl.setClassReference((String) payload.get(PayloadConstants.CLASS_REFERENCE));
                break;
            case AMR:
                idTokenImpl.setMethodsReferences((List<String>) payload.get(PayloadConstants.METHODS_REFERENCE));
                break;
            default:
                idTokenImpl.setOtherClaims(key, payload.get(key));
                break;
            }
        } catch (IllegalArgumentException e) {
            // Ignore since it is a custom claim
            //We cannot ignore custom claims anymore because this method is used
            //by the new code (IDTokenImpl) to get all the claims.
            idTokenImpl.setOtherClaims(key, payload.get(key));
        }
    }

    protected void addToPayload() {
        Set<String> keys = super.getPayload().keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            addToPayloadFields(key);
        }
    }

    public void addToIdTokenImpl(IdTokenImpl idTokenImpl) {

        Set<String> keys = super.getPayload().keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String key = it.next();
            addToPayloadFields(idTokenImpl, key); // add k:v to idtokenimpl
        }
        if (getClientId() != null) {
            idTokenImpl.setClientId(getClientId());
        }
    }

    /**
     * Verify idtoken.
     *
     * @throws Exception
     */
    @Override
    public boolean verify() throws IDTokenValidationFailedException {
        return verify(JsonTokenUtil.DEFAULT_SKEW_IN_SECONDS);
    }

    @Override
    public boolean verify(long clockSkew)
            throws IDTokenValidationFailedException {
        return verify(clockSkew, getKey());
    }

    /**
     * Verify idtoken.
     *
     * @throws Exception
     */
    @FFDCIgnore({ IDTokenValidationFailedException.class })
    @Override
    public boolean verify(long clockSkew, Object key)
            throws IDTokenValidationFailedException {
        boolean verified = false;
        try {
            if (super.verify(clockSkew, key)) {
                payload.putAll(super.getPayload());
                addToPayload();
                // verify accesstoken hash if it exists
                if (payload.getAccessTokenHash() != null) {
                    String athash = payload.getAccessTokenHash();
                    if (this.ath == null) {
                        // internal error
                        Tr.error(tc, "OIDC_IDTOKEN_VERIFY_ATHASH_ERR", new Object[] { getClientId(), this.ath, athash });
                        throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_VERIFY_ATHASH_ERR", getClientId(), this.ath, athash);
                    }
                    if (verifyAccessTokenHash(athash)) {
                        verified = true;
                    } else {
                        Tr.error(tc, "OIDC_IDTOKEN_VERIFY_ATHASH_ERR", new Object[] { getClientId(), this.ath, athash });
                        throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_VERIFY_ATHASH_ERR", getClientId(), this.ath, athash);
                    }
                } else {
                    verified = true;
                }
            }
        } catch (IDTokenValidationFailedException e) {
            // Tr.error has been handled
            throw e;
        }
        return verified;
    }

}
