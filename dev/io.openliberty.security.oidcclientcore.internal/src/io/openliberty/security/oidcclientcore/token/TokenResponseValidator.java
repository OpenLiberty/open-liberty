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

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils;

/**
 *
 */
public class TokenResponseValidator {

    OidcClientConfig clientConfig;
    CommonJose4jUtils jose4jutil = new CommonJose4jUtils();

    /**
     * @param oidcClientConfig
     */
    public TokenResponseValidator(OidcClientConfig oidcClientConfig) {
        this.clientConfig = oidcClientConfig;

    }

    /**
     * @param tokenResponse
     */
    public void validate(TokenResponse tokenResponse) throws TokenValidationException {
        String idtoken = null;

        if (tokenResponse != null) {
            idtoken = tokenResponse.getIdToken();
        }

        JwtContext jwtcontext = null;
        try {
            jwtcontext = CommonJose4jUtils.parseJwtWithoutValidation(idtoken);

        } catch (Exception e) {
            String error = e.getMessage() != null ? e.getMessage() : "not a valid id token";
            throw new TokenValidationException(this.clientConfig.getClientId(), error);
        }

        JwtClaims jwtClaims = null;
        if (jwtcontext != null && jwtcontext.getJwtClaims() != null) {
            jwtClaims = jwtcontext.getJwtClaims();
            // must have claims - iat and exp
            try {
                if (jwtClaims.getIssuedAt() == null) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "token is missing required iat claim");
                }
                if (jwtClaims.getExpirationTime() == null) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "token is missing required exp claim");
                }
                TokenValidator tokenValidator = new IdTokenValidator(clientConfig);
                tokenValidator.issuer(jwtClaims.getIssuer()).subject(jwtClaims.getSubject()).audiences(jwtClaims.getAudience()).azp(((String) jwtClaims.getClaimValue("azp"))).iat(jwtClaims.getIssuedAt()).exp(jwtClaims.getExpirationTime()).nbf(jwtClaims.getNotBefore());
                if (jwtClaims.hasClaim("nonce")) {
                    ((IdTokenValidator)tokenValidator).nonce(((String)jwtClaims.getClaimValue("nonce")));
                    ((IdTokenValidator)tokenValidator).validate();
                } else {
                    tokenValidator.validate();
                }
            } catch (MalformedClaimException e) {
                throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
            }

        }

        // TODO: get verification key
        // TODO: verify token signature

    }

}
