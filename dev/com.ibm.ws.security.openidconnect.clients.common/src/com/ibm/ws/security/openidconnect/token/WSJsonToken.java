/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import org.joda.time.Duration;

import com.google.gson.JsonObject;

/**
 * refactored from net.oauth.jsontoken.JsonToken
 */
public class WSJsonToken {
    private final JsonObject header;
    private final JsonObject payload;
    private final String tokenString;
    private final Duration skew; //TODO: why is this present?  seems wrong.
    public static final String ISSUED_AT = "iat";
    public static final String EXPIRATION = "exp";
    private long issuedAt; // seconds
    private long expiration; // seconds

    /**
     * @return the issuedAt
     */
    public long getIssuedAt() {
        return issuedAt;
    }

    public String getIssuer() {
        return payload.get("iss") != null ? payload.get("iss").getAsString() : null;
    }

    /**
     * @param issuedAt
     *            the issuedAt to set in seconds
     */
    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
        if (payload != null) {
            payload.addProperty(ISSUED_AT, issuedAt);
        }
    }

    /**
     * @return the expiration
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * @param expiration
     *            the expiration to set in seconds
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
        if (payload != null) {
            payload.addProperty(EXPIRATION, expiration);
        }
    }

    WSJsonToken(JsonObject header, JsonObject payload, Duration skew, String tokenString) {
        this.header = header;
        this.payload = payload;
        this.skew = skew;
        this.tokenString = tokenString;
        setIatExp(payload);
    }

    WSJsonToken() {
        this.header = new JsonObject();
        this.payload = new JsonObject();
        this.skew = null;
        this.tokenString = null;
    }

    WSJsonToken(JsonObject header, JsonObject payload) {
        this.header = header;
        this.payload = payload;
        this.skew = null;
        this.tokenString = null;
        setIatExp(payload);
    }

    private void setIatExp(JsonObject payload) {
        this.issuedAt = payload.getAsJsonPrimitive(ISSUED_AT) != null ? payload.getAsJsonPrimitive(ISSUED_AT).getAsLong() : 0;
        this.expiration = payload.getAsJsonPrimitive(EXPIRATION) != null ? payload.getAsJsonPrimitive(EXPIRATION).getAsLong() : 0;
    }

    /**
     * @return the header
     */
    public JsonObject getHeader() {
        return header;
    }

    /**
     * @return the payload
     */
    public JsonObject getPayload() {
        return payload;
    }

    /**
     * @return the tokenString
     */
    public String getTokenString() {
        return tokenString;
    }

    /**
     * @return the skew
     */
    public Duration getSkew() {
        return skew;
    }

}
