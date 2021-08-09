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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class JWTPayload extends ConcurrentHashMap<String, Object> implements Cloneable {

    private static final long serialVersionUID = -9076728504407675716L;

    /*
     * Payload as specified in <a
     * href="http://tools.ietf.org/html/draft-ietf-oauth-json-web-token-08#section-4.1">Reserved Claim
     * Names</a>.
     */
    public JWTPayload() {
    }

    /**
     * Expiration time claim that identifies the expiration time (in seconds) on or after which the
     * token MUST NOT be accepted for processing or {@code null} for none.
     */
    //("exp")
    private Long expirationTimeSeconds;

    /**
     * Not before claim that identifies the time (in seconds) before which the token MUST NOT be
     * accepted for processing or {@code null} for none.
     */
    //("nbf")
    private Long notBeforeTimeSeconds;

    /**
     * Issued at claim that identifies the time (in seconds) at which the JWT was issued or {@code null} for none.
     */
    //("iat")
    private Long issuedAtTimeSeconds;

    /** Issuer claim that identifies the principal that issued the JWT or {@code null} for none. */
    //("iss")
    private String issuer;

    /**
     * Audience claim that identifies the audience that the JWT is intended for (should either be a {@code String} or a
     * {@code List<String>}) or {@code null} for none.
     */
    //("aud")
    private Object audience;

    /**
     * JWT ID claim that provides a unique identifier for the JWT or {@code null} for none.
     */
    //("jti")
    private String jwtId;

    /**
     * Type claim that is used to declare a type for the contents of this JWT Claims Set or {@code null} for none.
     */
    //("typ")
    private String type;

    /**
     * Subject claim identifying the principal that is the subject of the JWT or {@code null} for
     * none.
     */
    //("sub")
    private String subject;

    /**
     * Returns the expiration time (in seconds) claim that identifies the expiration time on or
     * after which the token MUST NOT be accepted for processing or {@code null} for none.
     */
    public final Long getExpirationTimeSeconds() {
        return expirationTimeSeconds;
    }

    /**
     * Sets the expiration time claim that identifies the expiration time (in seconds) on or after
     * which the token MUST NOT be accepted for processing or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setExpirationTimeSeconds(Long expirationTimeSeconds) {
        this.expirationTimeSeconds = expirationTimeSeconds;
        this.put(PayloadConstants.EXPIRATION_TIME_IN_SECS, expirationTimeSeconds);
        return this;
    }

    /**
     * Returns the not before claim that identifies the time (in seconds) before which the token
     * MUST NOT be accepted for processing or {@code null} for none.
     */
    public final Long getNotBeforeTimeSeconds() {
        return notBeforeTimeSeconds;
    }

    /**
     * Sets the not before claim that identifies the time (in seconds) before which the token MUST
     * NOT be accepted for processing or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setNotBeforeTimeSeconds(Long notBeforeTimeSeconds) {
        this.notBeforeTimeSeconds = notBeforeTimeSeconds;
        this.put(PayloadConstants.NOT_BEFORE_TIME_IN_SECS, notBeforeTimeSeconds);
        return this;
    }

    /**
     * Returns the issued at claim that identifies the time (in seconds) at which the JWT was issued
     * or {@code null} for none.
     */
    public final Long getIssuedAtTimeSeconds() {
        return issuedAtTimeSeconds;
    }

    /**
     * Sets the issued at claim that identifies the time (in seconds) at which the JWT was issued or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setIssuedAtTimeSeconds(Long issuedAtTimeSeconds) {
        this.issuedAtTimeSeconds = issuedAtTimeSeconds;
        this.put(PayloadConstants.ISSUED_AT_TIME_IN_SECS, issuedAtTimeSeconds);
        return this;
    }

    /**
     * Returns the issuer claim that identifies the principal that issued the JWT or {@code null} for none.
     */
    public final String getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer claim that identifies the principal that issued the JWT or {@code null} for
     * none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setIssuer(String issuer) {
        this.issuer = issuer;
        this.put(PayloadConstants.ISSUER, issuer);
        return this;
    }

    /**
     * Returns the audience claim that identifies the audience that the JWT is intended for (should
     * either be a {@code String} or a {@code List<String>}) or {@code null} for none.
     */
    public final Object getAudience() {
        return audience;
    }

    /**
     * Returns the list of audience claim that identifies the audience that the JWT is intended for
     * or empty for none.
     */
    @SuppressWarnings("unchecked")
    public final List<String> getAudienceAsList() {
        if (audience == null) {
            return Collections.emptyList();
        }
        if (audience instanceof String) {
            return Collections.singletonList((String) audience);
        }
        return (List<String>) audience;
    }

    /**
     * Sets the audience claim that identifies the audience that the JWT is intended for (should
     * either be a {@code String} or a {@code List<String>}) or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setAudience(Object audience) {
        this.audience = audience;
        this.put(PayloadConstants.AUDIENCE, audience);
        return this;
    }

    /**
     * Returns the JWT ID claim that provides a unique identifier for the JWT or {@code null} for
     * none.
     */
    public final String getJwtId() {
        return jwtId;
    }

    /**
     * Sets the JWT ID claim that provides a unique identifier for the JWT or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setJwtId(String jwtId) {
        this.jwtId = jwtId;
        this.put(PayloadConstants.JWTID, jwtId);
        return this;
    }

    /**
     * Returns the type claim that is used to declare a type for the contents of this JWT Claims Set
     * or {@code null} for none.
     */
    public final String getType() {
        return type;
    }

    /**
     * Sets the type claim that is used to declare a type for the contents of this JWT Claims Set or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setType(String type) {
        this.type = type;
        this.put(PayloadConstants.TYPE, type);
        return this;
    }

    /**
     * Returns the subject claim identifying the principal that is the subject of the JWT or {@code null} for none.
     */
    public final String getSubject() {
        return subject;
    }

    /**
     * Sets the subject claim identifying the principal that is the subject of the JWT or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload setSubject(String subject) {
        this.subject = subject;
        this.put(PayloadConstants.SUBJECT, subject);
        return this;
    }

    @Override
    public JWTPayload clone() throws CloneNotSupportedException {
        return (JWTPayload) super.clone();
    }
}
