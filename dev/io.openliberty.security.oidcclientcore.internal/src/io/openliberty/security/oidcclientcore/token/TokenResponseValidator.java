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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JWKSet;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.config.OidcMetadataService;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils.TokenSignatureValidationBuilder;

public class TokenResponseValidator {

    public static final TraceComponent tc = Tr.register(TokenResponseValidator.class);

    static boolean initialized = false;

    OidcClientConfig clientConfig;
    CommonJose4jUtils jose4jutil = new CommonJose4jUtils();
    JWKSet jwkset;
    private HttpServletRequest request;
    private HttpServletResponse response;
    JSONObject discoveredProviderMetadata = null;
    private Storage storage;

    public TokenResponseValidator(OidcClientConfig oidcClientConfig) {
        this.clientConfig = oidcClientConfig;
    }

    @FFDCIgnore(Exception.class)
    public JwtClaims validate(TokenResponse tokenResponse) throws TokenValidationException {
        String idtoken = null;

        if (tokenResponse != null) {
            idtoken = tokenResponse.getIdTokenString();
        }

        if (idtoken == null) {
            return new JwtClaims();
        }

        JwtContext jwtcontext = null;
        try {
            jwtcontext = JwtParsingUtils.parseJwtWithoutValidation(idtoken);

        } catch (Exception e) {
            String error = e.getMessage() != null ? e.getMessage() : "not a valid id token";
            throw new TokenValidationException(this.clientConfig.getClientId(), error);
        }

        JwtClaims jwtClaims = null;
        if (jwtcontext != null && jwtcontext.getJwtClaims() != null) {
            jwtClaims = jwtcontext.getJwtClaims();
            String clientSecret = null;
            ProtectedString clientSecretProtectedString = clientConfig.getClientSecret();
            if (clientSecretProtectedString != null) {
                clientSecret = new String(clientSecretProtectedString.getChars());
            }
            String issuerconfigured = null;
            // must have claims - iat and exp
            try {
                if (jwtClaims.getIssuedAt() == null) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "token is missing required iat claim");
                }
                if (jwtClaims.getExpirationTime() == null) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "token is missing required exp claim");
                }
                TokenValidator tokenValidator = new IdTokenValidator(clientConfig);
                issuerconfigured = MetadataUtils.getIssuer(clientConfig);
                tokenValidator.issuer(jwtClaims.getIssuer()).subject(jwtClaims.getSubject()).audiences(jwtClaims.getAudience()).azp(((String) jwtClaims.getClaimValue("azp"))).iat(jwtClaims.getIssuedAt()).exp(jwtClaims.getExpirationTime()).nbf(jwtClaims.getNotBefore()).issuerconfigured(issuerconfigured);
                tokenValidator.validate();
                if (this.clientConfig.isUseNonce()) {
                    ((IdTokenValidator) tokenValidator).nonce(((String) jwtClaims.getClaimValue("nonce")));
                    ((IdTokenValidator) tokenValidator).state(getStateParameter());
                    ((IdTokenValidator) tokenValidator).secret(clientSecret);
                    storage = StorageFactory.instantiateStorage(request, response, clientConfig.isUseSession());
                    ((IdTokenValidator) tokenValidator).storage(storage);

                    ((IdTokenValidator) tokenValidator).validateNonce();
                }
            } catch (Exception e) {
                throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
            }

            try {
                JsonWebStructure jsonStruct = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtcontext);
                if (jsonStruct == null || !(jsonStruct instanceof JsonWebSignature)) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "jsonwebsignature error");
                }
                TokenSignatureValidationBuilder tokenSignatureValidationBuilder = jose4jutil.signaturevalidationbuilder();
                String jwksUri = MetadataUtils.getJwksUri(clientConfig);
                tokenSignatureValidationBuilder.signature(jsonStruct).sslsupport(OidcMetadataService.getSSLSupport()).jwkuri(jwksUri).issuer(issuerconfigured).clientid(clientConfig.getClientId());

                tokenSignatureValidationBuilder.clientsecret(clientSecret);
                tokenSignatureValidationBuilder.jwkset(jwkset);
                tokenSignatureValidationBuilder.jwksConnectTimeout(clientConfig.getJwksConnectTimeout());
                tokenSignatureValidationBuilder.jwksReadTimeout(clientConfig.getJwksReadTimeout());

                return tokenSignatureValidationBuilder.parseJwtWithValidation(idtoken);
            } catch (Exception e) {
                throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
            }
        }
        throw new TokenValidationException(this.clientConfig.getClientId(), "not a valid token to continue the flow");
    }

    public String getStateParameter() {
        return request.getParameter(AuthorizationRequestParameters.STATE);
        //TODO: maybe throw an exception right here if this state is not valid
    }

    /**
     * @param jwkSet
     */
    public void setJwkSet(JWKSet jwkSet) {
        this.jwkset = jwkSet;
    }

    /**
     * @param request
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * @param response
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

}
