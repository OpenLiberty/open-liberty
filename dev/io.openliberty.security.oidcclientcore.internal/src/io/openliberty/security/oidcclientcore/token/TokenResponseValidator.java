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

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils.TokenSignatureValidationBuilder;

/**
 *
 */
@Component(service = TokenResponseValidator.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class TokenResponseValidator {

    private static final String KEY_EP_REQUEST = "endpointRequest";
    private static volatile EndpointRequest eprequest;
    private static final String KEY_SSL_SUPPORT = "sslSupport";
    private static volatile SSLSupport sslSupport;

    OidcClientConfig clientConfig;
    CommonJose4jUtils jose4jutil = new CommonJose4jUtils();

    public TokenResponseValidator() {

    }

    /**
     * @param oidcClientConfig
     */
    public TokenResponseValidator(OidcClientConfig oidcClientConfig) {
        this.clientConfig = oidcClientConfig;

    }

    @Reference(name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC)
    protected void setSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = sslSupportSvc;
    }

    protected void unsetSslSupport(SSLSupport sslSupportSvc) {
        sslSupport = null;
    }

    @Reference(name = KEY_EP_REQUEST, policy = ReferencePolicy.DYNAMIC)
    protected void setEndpointRequest(EndpointRequest eprequestService) {
        eprequest = eprequestService;
    }

    protected void unsetEndpointRequest(EndpointRequest eprequestService) {
        eprequest = null;
    }

    /**
     * @param tokenResponse
     */
    public JwtClaims validate(TokenResponse tokenResponse) throws TokenValidationException {
        String idtoken = null;

        if (tokenResponse != null) {
            idtoken = tokenResponse.getIdTokenString();
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
                    ((IdTokenValidator) tokenValidator).nonce(((String) jwtClaims.getClaimValue("nonce")));
                    ((IdTokenValidator) tokenValidator).validate();
                } else {
                    tokenValidator.validate();
                }
            } catch (MalformedClaimException e) {
                throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
            }

        }

        try {
            JsonWebStructure jsonStruct = jose4jutil.getJsonWebStructureFromJwtContext(jwtcontext);
            if (jsonStruct == null || !(jsonStruct instanceof JsonWebSignature)) {
                throw new TokenValidationException(this.clientConfig.getClientId(), "jsonwebsignature error");
            }
            TokenSignatureValidationBuilder tokenSignatureValidationBuilder = jose4jutil.signaturevalidationbuilder();

            tokenSignatureValidationBuilder.signature(jsonStruct).sslsupport(sslSupport).issuer(TokenValidator.getIssuer(clientConfig)).jwkuri(clientConfig.getProviderMetadata().getJwksURI()) //TODO : use discover data if needed
                            .clientid(clientConfig.getClientId());
            String clientSecret = null;
            ProtectedString clientSecretProtectedString = clientConfig.getClientSecret();
            if (clientSecretProtectedString != null) {
                clientSecret = new String(clientSecretProtectedString.getChars());
            }
            tokenSignatureValidationBuilder.clientsecret(clientSecret);
            return tokenSignatureValidationBuilder.parseJwtWithValidation(idtoken);
        } catch (Exception e) {
            throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
        }
    }

}
