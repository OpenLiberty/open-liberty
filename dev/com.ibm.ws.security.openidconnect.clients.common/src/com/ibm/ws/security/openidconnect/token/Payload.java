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

import java.util.List;

/**
 * ID token payload.
 */
public class Payload extends JWTPayload {

    private static final long serialVersionUID = -8360926274023790345L;

    /** Time (in seconds) of end-user authorization or {@code null} for none. */
    //("auth_time")
    private Long authorizationTimeSeconds;

    /** Authorized party or {@code null} for none. */
    //("azp")
    private String authorizedParty;

    /** Value used to associate a client session with an ID token or {@code null} for none. */
    private String nonce;

    /** Access token hash value or {@code null} for none. */
    //("at_hash")
    private String accessTokenHash;

    /** Authentication context class reference or {@code null} for none. */
    //("acr")
    private String classReference;

    /** Authentication methods references or {@code null} for none. */
    //("amr")
    private List<String> methodsReferences;

    /** Returns the time (in seconds) of end-user authorization or {@code null} for none. */
    public final Long getAuthorizationTimeSeconds() {
        return authorizationTimeSeconds;
    }

    /**
     * Sets the time (in seconds) of end-user authorization or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setAuthorizationTimeSeconds(Long authorizationTimeSeconds) {
        this.authorizationTimeSeconds = authorizationTimeSeconds;
        this.put(PayloadConstants.AUTHZ_TIME_IN_SECS, authorizationTimeSeconds);
        return this;
    }

    /**
     * Returns the authorized party or {@code null} for none.
     * 
     * <p>
     * Returns String
     * </p>
     */
    public final String getAuthorizedParty() {
        return authorizedParty;
    }

    /**
     * Sets the authorized party or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setAuthorizedParty(String authorizedParty) {
        this.authorizedParty = authorizedParty;
        this.put(PayloadConstants.AUTHORIZED_PARTY, authorizedParty);
        return this;
    }

    /**
     * Returns the value used to associate a client session with an ID token or {@code null} for
     * none.
     */
    public final String getNonce() {
        return nonce;
    }

    /**
     * Sets the value used to associate a client session with an ID token or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setNonce(String nonce) {
        this.nonce = nonce;
        this.put(PayloadConstants.NONCE, nonce);
        return this;
    }

    /**
     * Returns the access token hash value or {@code null} for none.
     */
    public final String getAccessTokenHash() {
        return accessTokenHash;
    }

    /**
     * Sets the access token hash value or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setAccessTokenHash(String accessTokenHash) {
        this.accessTokenHash = accessTokenHash;
        this.put(PayloadConstants.AT_HASH, accessTokenHash);
        return this;
    }

    /**
     * Returns the authentication context class reference or {@code null} for none.
     */
    public final String getClassReference() {
        return classReference;
    }

    /**
     * Sets the authentication context class reference or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setClassReference(String classReference) {
        this.classReference = classReference;
        this.put(PayloadConstants.CLASS_REFERENCE, classReference);
        return this;
    }

    /**
     * Returns the authentication methods references or {@code null} for none.
     */
    public final List<String> getMethodsReferences() {
        return methodsReferences;
    }

    /**
     * Sets the authentication methods references or {@code null} for none.
     * 
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public Payload setMethodsReferences(List<String> methodsReferences) {
        this.methodsReferences = methodsReferences;
        this.put(PayloadConstants.METHODS_REFERENCE, methodsReferences);
        return this;
    }

    @Override
    public Payload setExpirationTimeSeconds(Long expirationTimeSeconds) {
        return (Payload) super.setExpirationTimeSeconds(expirationTimeSeconds);
    }

    @Override
    public Payload setNotBeforeTimeSeconds(Long notBeforeTimeSeconds) {
        return (Payload) super.setNotBeforeTimeSeconds(notBeforeTimeSeconds);
    }

    @Override
    public Payload setIssuedAtTimeSeconds(Long issuedAtTimeSeconds) {
        return (Payload) super.setIssuedAtTimeSeconds(issuedAtTimeSeconds);
    }

    @Override
    public Payload setIssuer(String issuer) {
        return (Payload) super.setIssuer(issuer);
    }

    @Override
    public Payload setAudience(Object audience) {
        return (Payload) super.setAudience(audience);
    }

    @Override
    public Payload setJwtId(String jwtId) {
        return (Payload) super.setJwtId(jwtId);
    }

    @Override
    public Payload setType(String type) {
        return (Payload) super.setType(type);
    }

    @Override
    public Payload setSubject(String subject) {
        return (Payload) super.setSubject(subject);
    }

}
