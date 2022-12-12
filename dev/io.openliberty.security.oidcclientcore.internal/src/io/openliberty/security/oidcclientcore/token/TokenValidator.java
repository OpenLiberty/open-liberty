/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token;

import java.util.ArrayList;
import java.util.List;

import org.jose4j.jwt.NumericDate;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;

public class TokenValidator {

    private String issuer;
    private String issuerconfigured;
    private String subject = null;
    private List<String> audiences;
    private String azp = null;
    private NumericDate iat;
    private NumericDate exp;
    private NumericDate notBefore = null;
    protected OidcClientConfig oidcConfig;

    private final long clockSkewInSeconds = 120; // default value in seconds

    public TokenValidator(OidcClientConfig clientConfig) {
        this.oidcConfig = clientConfig;
    }

    public TokenValidator issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public TokenValidator subject(String subject) {
        this.subject = subject;
        return this;
    }

    public TokenValidator audiences(List<String> audience) {
        this.audiences = new ArrayList<String>(audience);
        return this;
    }

    public TokenValidator azp(String azp) {
        this.azp = azp;
        return this;
    }

    public TokenValidator iat(NumericDate iat) {
        this.iat = iat;
        return this;
    }

    public TokenValidator exp(NumericDate exp) {
        this.exp = exp;
        return this;
    }

    public TokenValidator nbf(NumericDate notBefore) {
        this.notBefore = notBefore;
        return this;
    }

    public void validate() throws TokenValidationException {
        validateIssuer();
        validateSubject();
        validateAudiences();
        validateAZP();
        validateExpiration();
        validateIssuedAt();
        validateNotBefore();

    }

    protected void validateNotBefore() throws TokenValidationException {
        if (this.notBefore != null) {
            long now = System.currentTimeMillis();
            if (now + (this.clockSkewInSeconds * 1000) < this.iat.getValueInMillis()) {
                throw new TokenValidationException(oidcConfig.getClientId(), "nbf claim must be in past");
            }
        }
    }

    protected void validateIssuedAt() throws TokenValidationException {
        long now = System.currentTimeMillis();
        if (now + (this.clockSkewInSeconds * 1000) < this.iat.getValueInMillis()) {
            throw new TokenValidationException(oidcConfig.getClientId(), "iat claim must be in past");
        }
    }

    protected void validateExpiration() throws TokenValidationException {
        long now = System.currentTimeMillis();
        if (now - (this.clockSkewInSeconds * 1000) > this.exp.getValueInMillis()) {
            throw new TokenValidationException(oidcConfig.getClientId(), "exp claim must be in future");
        }
    }

    protected void validateAZP() throws TokenValidationException {
        if (this.azp != null) {
            if (!(oidcConfig.getClientId().equals(this.azp))) {
                throw new TokenClaimMismatchException(oidcConfig.getClientId(), azp, "azp", oidcConfig.getClientId());
            }
        }
    }

    protected void validateAudiences() throws TokenValidationException {
        if (this.audiences != null && !(this.audiences.isEmpty())) {
            if (this.audiences.size() == 1) {
                // validate aud claim against client id
                if (!(oidcConfig.getClientId().equals(this.audiences.get(0)))) {
                    throw new TokenClaimMismatchException(oidcConfig.getClientId(), audiences.get(0), "aud", oidcConfig.getClientId());
                }
            } else if (this.azp == null) {
                // if more than one audience, then azp claim is a must
                throw new TokenValidationException(oidcConfig.getClientId(), "multiple audiences present [ " + this.audiences(audiences).toString()
                                                                             + " ], but required azp claim is missing");
            }
        }
    }

    protected void validateSubject() throws TokenValidationException {
        if (this.subject != null) {
            if (this.subject.isEmpty()) {
                throw new TokenValidationException(oidcConfig.getClientId(), "subject claim is present but empty");
            }
        }
    }

    protected void validateIssuer() throws TokenValidationException {
        if (!issuerconfigured.equals(this.issuer)) {
            throw new TokenClaimMismatchException(oidcConfig.getClientId(), issuer, "iss", issuerconfigured);
        }
    }

    public TokenValidator issuerconfigured(String issuerFromProviderMetadata) {
        this.issuerconfigured = issuerFromProviderMetadata;
        return this;
    }
}
