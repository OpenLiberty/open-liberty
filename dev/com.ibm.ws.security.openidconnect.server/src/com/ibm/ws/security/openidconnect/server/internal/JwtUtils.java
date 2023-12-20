/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import java.security.Key;
import java.util.Arrays;
import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.jws.JwsSignatureVerifier;

/**
 *
 */
public class JwtUtils {

    private static final TraceComponent tc = Tr.register(JwtUtils.class);

    /**
     * construct JWT which is a super class of IDToken from IdTokenString.
     * JWT class is used in order to just perform signature validation.
     *
     * @param oauth20provider  extracted from the request
     * @param oidcServerConfig is the object of oidc server configurations
     * @throws OidcServerException
     *
     */
    public static JWT createJwt(String tokenString, OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig) throws Exception {
        String aud = null;
        String issuer = null;
        JWTPayload payload = JsonTokenUtil.getPayload(tokenString);
        if (payload != null) {
            aud = JsonTokenUtil.getAud(payload);
            issuer = JsonTokenUtil.getIss(payload);
        }
        Object key = getJwtVerificationKey(tokenString, oauth20provider, oidcServerConfig);
        String signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "clientId : " + aud + " key : " + ((key == null) ? "null" : "<removed>") + " issuer : " + issuer + " signatureAlgorithm : " + signatureAlgorithm);
        }

        return new JWT(tokenString, key, aud, issuer, signatureAlgorithm);
    }

    @Sensitive
    static Object getJwtVerificationKey(String tokenString, OAuth20Provider oauth20provider, OidcServerConfig oidcServerConfig) throws Exception {
        Object key = null;
        JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(tokenString);
        String signingAlgorithm = JwsSignatureVerifier.verifyJwsAlgHeaderOnly(jwtContext, Arrays.asList(oidcServerConfig.getIdTokenSigningAlgValuesSupported()));
        if (signingAlgorithm.equals("none")) {
            key = null;
        } else if (signingAlgorithm.startsWith("HS")) {
            key = getSharedKey(jwtContext, oauth20provider);
        } else {
            // TODO - remove beta guard when ready
            if (ProductInfo.getBetaEdition()) {
                JsonWebStructure jsonStruct = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
                key = getPublicKeyFromJsonWebStructure(jsonStruct, oidcServerConfig);
            }
        }
        return key;
    }

    @Sensitive
    static Object getSharedKey(JwtContext jwtContext, OAuth20Provider oauth20provider) throws OidcServerException, MalformedClaimException {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        List<String> audiences = jwtClaims.getAudience();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Audiences from JWT: " + audiences);
        }
        if (audiences == null || audiences.isEmpty() || audiences.size() > 1) {
            // TODO
            return null;
        }
        String clientId = audiences.get(0);
        return getSharedKey(oauth20provider, clientId);
    }

    /**
     * get Shared key
     *
     * @param oauth20provider extracted from the request
     * @param clientId
     * @throws OidcServerException
     *
     */
    @Sensitive
    static Object getSharedKey(OAuth20Provider oauth20provider, String clientId) throws OidcServerException {
        String sharedKey = null;
        OidcOAuth20ClientProvider clientProvider = oauth20provider.getClientProvider();
        OidcOAuth20Client oauth20Client = clientProvider.get(clientId);
        if (oauth20Client instanceof BaseClient) {
            BaseClient baseClient = (BaseClient) oauth20Client;
            sharedKey = baseClient.getClientSecret();
        }
        return sharedKey;
    }

    static Key getPublicKeyFromJsonWebStructure(JsonWebStructure jsonStruct, OidcServerConfig oidcServerConfig) {
        String alg = jsonStruct.getAlgorithmHeaderValue();
        String kid = jsonStruct.getKeyIdHeaderValue();
        String x5t = jsonStruct.getX509CertSha1ThumbprintHeaderValue();

        Key publicKey = null;
        JSONWebKey jwk = oidcServerConfig.getJSONWebKey();
        if (jwk != null && alg.equals(jwk.getAlgorithm())) {
            if (kid != null && kid.equals(jwk.getKeyID())) {
                publicKey = jwk.getPublicKey();
            } else if (x5t != null && x5t.equals(jwk.getKeyX5t())) {
                publicKey = jwk.getPublicKey();
            } else if (kid == null && x5t == null) {
                publicKey = jwk.getPublicKey();
            }
        }
        return publicKey;
    }

}
