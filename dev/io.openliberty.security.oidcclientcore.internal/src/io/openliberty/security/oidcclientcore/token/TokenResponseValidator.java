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
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;
import io.openliberty.security.oidcclientcore.storage.CookieBasedStorage;
import io.openliberty.security.oidcclientcore.storage.SessionBasedStorage;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils;
import io.openliberty.security.oidcclientcore.utils.CommonJose4jUtils.TokenSignatureValidationBuilder;

/**
 *
 */
@Component(service = TokenResponseValidator.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class TokenResponseValidator {

    public static final TraceComponent tc = Tr.register(TokenResponseValidator.class);
    private static final String KEY_EP_REQUEST = "endpointRequest";
    public static volatile EndpointRequest eprequest;
    private static final String KEY_SSL_SUPPORT = "sslSupport";
    public static volatile SSLSupport sslSupport;
    static boolean initialized = false;

    OidcClientConfig clientConfig;
    CommonJose4jUtils jose4jutil = new CommonJose4jUtils();
    JWKSet jwkset;
    private HttpServletRequest request;
    private HttpServletResponse response;
    JSONObject discoveredProviderMetadata = null;
    private Storage storage;

    public TokenResponseValidator() {

    }

    /**
     * @param oidcClientConfig
     */
    public TokenResponseValidator(OidcClientConfig oidcClientConfig) {
        this.clientConfig = oidcClientConfig;
    }

    /**
     * @param oidcClientConfig
     */
    private void instantiateStorage(OidcClientConfig oidcClientConfig) {
        if (oidcClientConfig.isUseSession()) {
            this.storage = new SessionBasedStorage(this.request);
        } else {
            this.storage = new CookieBasedStorage(this.request, this.response);
        }

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
    @FFDCIgnore(Exception.class)
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
                issuerconfigured = getIssuer();
                tokenValidator.issuer(jwtClaims.getIssuer()).subject(jwtClaims.getSubject()).audiences(jwtClaims.getAudience()).azp(((String) jwtClaims.getClaimValue("azp"))).iat(jwtClaims.getIssuedAt()).exp(jwtClaims.getExpirationTime()).nbf(jwtClaims.getNotBefore()).issuerconfigured(issuerconfigured);
                tokenValidator.validate();
                if (this.clientConfig.isUseNonce()) {
                    ((IdTokenValidator) tokenValidator).nonce(((String) jwtClaims.getClaimValue("nonce")));
                    ((IdTokenValidator) tokenValidator).state(getStateParameter());
                    ((IdTokenValidator) tokenValidator).secret(clientSecret);
                    instantiateStorage(clientConfig);
                    ((IdTokenValidator) tokenValidator).storage(storage);
                    
                    ((IdTokenValidator) tokenValidator).validateNonce();
                } 
            } catch (MalformedClaimException e) {
                throw new TokenValidationException(this.clientConfig.getClientId(), e.getMessage());
            }

            try {
                JsonWebStructure jsonStruct = jose4jutil.getJsonWebStructureFromJwtContext(jwtcontext);
                if (jsonStruct == null || !(jsonStruct instanceof JsonWebSignature)) {
                    throw new TokenValidationException(this.clientConfig.getClientId(), "jsonwebsignature error");
                }
                TokenSignatureValidationBuilder tokenSignatureValidationBuilder = jose4jutil.signaturevalidationbuilder();
                String jwkuri = getJwkUri();
                tokenSignatureValidationBuilder.signature(jsonStruct).sslsupport(sslSupport).issuer(issuerconfigured).jwkuri(jwkuri).clientid(clientConfig.getClientId());

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

    /**
     * @param clientConfig
     * @return
     */
    private String getIssuer() {
        String issuer = null;
        issuer = clientConfig.getProviderMetadata().getIssuer();
        if ((issuer == null || issuer.isEmpty()) && getProviderDiscoveryMetadadata() != null) {
            return ((String) this.discoveredProviderMetadata.get("issuer"));
        }
        return issuer;
    }

    /**
     * @return
     */
    private JSONObject getProviderDiscoveryMetadadata() {
        if (this.discoveredProviderMetadata == null) {
            this.discoveredProviderMetadata = eprequest.getProviderDiscoveryMetadata(clientConfig);
        }
        return this.discoveredProviderMetadata; //TODO : throw TokenValidationException if we don't have valid discovered data
    }

    /**
     * @return
     */
    private String getJwkUri() {
        String jwkuri = null;
        jwkuri = clientConfig.getProviderMetadata().getJwksURI();
        if ((jwkuri == null || jwkuri.isEmpty()) && getProviderDiscoveryMetadadata() != null) {
            jwkuri = (String) (this.discoveredProviderMetadata.get("jwks_uri"));
        }
        return jwkuri;
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
