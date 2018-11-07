/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import java.security.AccessController;
import java.security.Key;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.openidconnect.clients.common.AttributeToSubject;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.JtiNonceCache;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.clients.common.TraceConstants;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.jose4j.Jose4jValidator;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;

public class Jose4jUtil {

    private static final TraceComponent tc = Tr.register(Jose4jUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SIGNATURE_ALG_NONE = "none";
    private final SSLSupport sslSupport;
    private static final JtiNonceCache jtiCache = new JtiNonceCache(); // Jose4jUil has only one instance

    // set org.jose4j.jws.default-allow-none to true to behave the same as old jwt
    // allow signatureAlgorithme as none
    static {
        AccessController
                .doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return System.setProperty("org.jose4j.jws.default-allow-none", "true");
                    }
                });
    };

    public Jose4jUtil(SSLSupport sslSupport) {
        this.sslSupport = sslSupport;
    }

    // eliminate the FFDC since we will have the Tr.error and most of the Exception already handled by FFDC
    @FFDCIgnore({ Exception.class })
    public ProviderAuthenticationResult createResultWithJose4J(String responseState,
            Map<String, String> tokens,
            ConvergedClientConfig clientConfig,
            OidcClientRequest oidcClientRequest) {
        //oidcClientRequest.setTokenType(OidcClientRequest.TYPE_); // decided by the caller
        // This is for ID Token only at the writing time
        ProviderAuthenticationResult oidcResult = null;
        String tokenStr = getIdToken(tokens, clientConfig);
        String accessToken = tokens.get(Constants.ACCESS_TOKEN);
        String refreshToken = tokens.get(Constants.REFRESH_TOKEN);
        String clientId = clientConfig.getClientId();
        try {
            if (tokenStr == null) {
                // This is for ID Token only
                Tr.error(tc, "OIDC_CLIENT_IDTOKEN_REQUEST_FAILURE", new Object[] { clientId, clientConfig.getTokenEndpointUrl() });
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }

            JwtContext jwtContext = parseJwtWithoutValidation(tokenStr);
            JwtClaims jwtClaims = parseJwtWithValidation(clientConfig, tokenStr, jwtContext, oidcClientRequest);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "post jwtClaims: " + jwtClaims + " firstPass jwtClaims=" + jwtContext.getJwtClaims());
            }
            OidcTokenImplBase idToken = new OidcTokenImplBase(jwtClaims, accessToken, refreshToken, clientId, oidcClientRequest.getTokenTypeNoSpace());

            if (idToken.getSubject() == null) {
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }
            AttributeToSubject attributeToSubject = new AttributeToSubject(clientConfig, idToken);
            if (attributeToSubject.checkUserNameForNull()) {
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }

            boolean isImplicit = Constants.IMPLICIT.equals(clientConfig.getGrantType());
            // verify nonce when nonce is enabled
            // this id for ID Token only
            if (clientConfig.isNonceEnabled() || isImplicit) {
                String nonceInIDToken = idToken.getNonce();
                boolean bNonceVerified = OidcUtil.verifyNonce(oidcClientRequest, nonceInIDToken, clientConfig, responseState);
                if (!bNonceVerified) {
                    // Error handling
                    Tr.error(tc, "OIDC_CLIENT_REQUEST_NONCE_FAILED", new Object[] { clientId, nonceInIDToken });
                    return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
                }
            }

            // if social login flow, put tokens and anything else social might want
            // into PAR props here and return it. Social will build the subject.
            if (clientConfig.isSocial()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "social login flow, storing id token in result");
                }
                Hashtable<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.ID_TOKEN, tokenStr);
                props.put(Constants.ACCESS_TOKEN, accessToken);
                if (idToken != null) {
                    props.put(Constants.ID_TOKEN_OBJECT, idToken);
                }
                oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, null, props, null);
                return oidcResult;
            }

            Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
            if (clientConfig.isIncludeCustomCacheKeyInSubject() || clientConfig.isDisableLtpaCookie()) {
                long storingTime = new Date().getTime();
                String customCacheKey = oidcClientRequest.getAndSetCustomCacheKeyValue(); //username + tokenStr.toString().hashCode();
                customProperties.put(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS, Long.valueOf(storingTime));
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
                customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE); // TODO checking?
            }
            Subject subject = null;
            if (clientConfig.isIncludeIdTokenInSubject()) {
                subject = new Subject();
                subject.getPrivateCredentials().add(idToken); // add the external IDToken
                customProperties.putAll(tokens); // add ALL tokens to props.
            } else {
                if (refreshToken != null) {
                    customProperties.put(Constants.REFRESH_TOKEN, refreshToken);
                }
                if (accessToken != null) {
                    customProperties.put(Constants.ACCESS_TOKEN, accessToken);
                }
            }
            if (idToken != null) {
                customProperties.put(Constants.ID_TOKEN_OBJECT, idToken); // pass back to authenticator
            }

            //addJWTTokenToSubject(customProperties, idToken, clientConfig);

            //doIdAssertion(customProperties, payload, clientConfig);
            oidcResult = attributeToSubject.doMapping(customProperties, subject);
            //oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, username, subject, customProperties, null);

        } catch (Exception e) {
            Tr.error(tc, "OIDC_CLIENT_IDTOKEN_VERIFY_ERR", new Object[] { e.getLocalizedMessage(), clientId });
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return oidcResult;
    }

    String getIdToken(Map<String, String> tokens, ConvergedClientConfig clientConfig) {
        if (tokens == null) {
            return null;
        }
        String idToken = tokens.get(Constants.ID_TOKEN);
        if (idToken == null && useAccessTokenAsIdToken(clientConfig)) {
            idToken = tokens.get(Constants.ACCESS_TOKEN);
        }
        return idToken;
    }

    boolean useAccessTokenAsIdToken(ConvergedClientConfig clientConfig) {
        return clientConfig.getUseAccessTokenAsIdToken();
    }

    /**
     * @param customProperties
     * @param accessToken
     * @param idToken
     * @param clientConfig
     * @throws Exception
     */
    //    private void addJWTTokenToSubject(Hashtable<String, Object> customProperties, OidcTokenImpl idToken, OidcClientConfig clientConfig) throws Exception {
    //        //
    //        if (clientConfig.jwtRef() != null && idToken != null) {
    //            String[] claimsToCopy = clientConfig.getJwtClaims();
    //            Map<String, Object> claimsToCopyMap = new HashMap<String, Object>();
    //            //claimsToCopyMap.put(ClientConstants.SUB, idToken.getSubject()); //always copy this
    //            if (claimsToCopy != null && claimsToCopy.length > 0) {
    //                for (String claim : claimsToCopy) {
    //                    Object v;
    //                    if ((v = idToken.getClaim(claim)) != null) {
    //                        claimsToCopyMap.put(claim, v);
    //                    }
    //                }
    //            } else {
    //                String subToAdd = null;
    //                String sub = getSubjectClaim(clientConfig);
    //                if (sub == null) {
    //                    sub = ClientConstants.SUB;
    //                }
    //                try {
    //                    subToAdd = (String) idToken.getClaim(sub);
    //
    //                } catch (ClassCastException cce) {
    //                    subToAdd = null;
    //                }
    //                if (subToAdd != null) {
    //                    claimsToCopyMap.put(sub, subToAdd);
    //                }
    //            }
    //            buildJWTTokenAndAddToSubject(clientConfig, claimsToCopyMap, customProperties);
    //        }
    //    }
    //
    //    protected String getSubjectClaim(OidcClientConfig clientConfig) {
    //        String sub = null;
    //
    //        sub = clientConfig.getUserIdentifier();
    //        if (sub == null) {
    //            sub = clientConfig.getUserIdentityToCreateSubject(); //default is "sub"
    //        }
    //        return sub;
    //    }
    //
    //    protected String getSubClaimFromIdToken(OidcClientConfig clientConfig, OidcTokenImpl idToken) {
    //        String sub = null;
    //        String subToAdd = null;
    //
    //        sub = clientConfig.getUserIdentifier();
    //        if (sub == null) {
    //            sub = clientConfig.getUserIdentityToCreateSubject(); //default is "sub"
    //        }
    //        if (sub != null) {
    //            try {
    //                subToAdd = (String) idToken.getClaim(sub);
    //
    //            } catch (ClassCastException cce) {
    //                subToAdd = null;
    //            }
    //        }
    //        return subToAdd;
    //    }

    /**
     * @param clientConfig
     * @param claimsFromAnother
     * @param customProperties
     * @throws Exception
     */
    //    private void buildJWTTokenAndAddToSubject(OidcClientConfig clientConfig, Map claimsFromAnother, Hashtable<String, Object> customProperties) throws Exception {
    //
    //        JwtToken token = JwtBuilder.create(clientConfig.jwtRef()).claim(claimsFromAnother).buildJwt();
    //        String jwt = token.compact();//JwtBuilder.create(clientConfig.jwtRef()).claim(idToken.getAllClaims()).buildJwt().compact();
    //        if (jwt != null) {
    //            customProperties.put(ClientConstants.ISSUED_JWT_TOKEN, jwt);
    //        }
    //
    //    }

    //Just parse without validation for now
    protected static JwtContext parseJwtWithoutValidation(String jwtString) throws Exception {
        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtContext jwtContext = firstPassJwtConsumer.process(jwtString);

        return jwtContext;

    }

    @FFDCIgnore({ Exception.class })
    protected JwtClaims parseJwtWithValidation(ConvergedClientConfig clientConfig,
            String jwtString,
            JwtContext jwtContext,
            OidcClientRequest oidcClientRequest) throws JWTTokenValidationFailedException, IllegalStateException, Exception {
        try {
            List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();
            if (jsonStructures == null || jsonStructures.isEmpty()) {
                throw new Exception("Invalid JsonWebStructure");
            }
            JsonWebStructure jsonStruct = jsonStructures.get(0);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "JsonWebStructure class: " + jsonStruct.getClass().getName() + " data:" + jsonStruct);
                if (jsonStruct instanceof JsonWebSignature) {
                    JsonWebSignature signature = (JsonWebSignature) jsonStruct;
                    Tr.debug(tc, "JsonWebSignature alg: " + signature.getAlgorithmHeaderValue() + " 3rd:'" + signature.getEncodedSignature() + "'");
                }
            }

            String kid = jsonStruct.getKeyIdHeaderValue();
            String x5t = jsonStruct.getX509CertSha1ThumbprintHeaderValue();
            Key key = null;
            Exception caughtException = null;
            try {
                key = getVerifyKey(clientConfig, kid, x5t);
            } catch (Exception e) {
                caughtException = e;
            }

            if (key == null) {
                Object[] objs = new Object[] { clientConfig.getSignatureAlgorithm(), "" };
                if (caughtException != null) {
                    objs = new Object[] { clientConfig.getSignatureAlgorithm(), caughtException.getLocalizedMessage() };
                }
                oidcClientRequest.setRsFailMsg(OidcClientRequest.NO_KEY, Tr.formatMessage(tc, "OIDC_CLIENT_NO_VERIFYING_KEY", objs));
                throw oidcClientRequest.error(true, tc, "OIDC_CLIENT_NO_VERIFYING_KEY", objs);
            }

            Jose4jValidator validator = new Jose4jValidator(key,
                    clientConfig.getClockSkewInSeconds(),
                    new OIDCClientAuthenticatorUtil().getIssuerIdentifier(clientConfig),
                    clientConfig.getClientId(),
                    clientConfig.getSignatureAlgorithm(),
                    oidcClientRequest);

            return validator.parseJwtWithValidation(jwtString, jwtContext, (JsonWebSignature) jsonStruct);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an unexpected exception.", e);
            }
            throw e;
        }
    }

    protected Key getVerifyKey(ConvergedClientConfig clientConfig, String kid, String x5t) throws Exception {
        Key keyValue = null;
        String signatureAlgorithm = clientConfig.getSignatureAlgorithm();
        if (SIGNATURE_ALG_HS256.equals(signatureAlgorithm)) {
            //keyValue = Base64Coder.getBytes(clientConfig.getSharedKey());
            keyValue = new HmacKey(clientConfig.getSharedKey().getBytes(ClientConstants.CHARSET));
        } else if (SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
            if (clientConfig.getJwkEndpointUrl() != null || clientConfig.getJsonWebKey() != null) {
                JwKRetriever retriever = createJwkRetriever(clientConfig);
                keyValue = retriever.getPublicKeyFromJwk(kid, x5t, "sig", clientConfig.getUseSystemPropertiesForHttpClientConnections());
            } else {
                keyValue = clientConfig.getPublicKey();
            }
        } else if (SIGNATURE_ALG_NONE.equals(signatureAlgorithm)) {
            keyValue = new HmacKey(clientConfig.getSharedKey().getBytes(ClientConstants.CHARSET)); // TODO: need to look at the token to figure out which key to get from config
            // TODO : getAlgFromToken(tokenStr);
        }
        return keyValue;
    }

    public JwKRetriever createJwkRetriever(ConvergedClientConfig oidcClientConfig) {
        JwKRetriever retriever = null;
        if (oidcClientConfig != null) { // to support unittests, config cannot be null
            retriever = new JwKRetriever(oidcClientConfig.getId(), oidcClientConfig.getSslRef(), oidcClientConfig.getJwkEndpointUrl(), oidcClientConfig.getJwkSet(), this.sslSupport, oidcClientConfig.isHostNameVerificationEnabled(), oidcClientConfig.getJwkClientId(), oidcClientConfig.getJwkClientSecret());
        }
        return retriever;
    }

    protected ProviderAuthenticationResult createProviderAuthenticationResult(JSONObject jobj, ConvergedClientConfig clientConfig, String accessToken) {

        AttributeToSubject attributeToSubject = new AttributeToSubject(clientConfig, jobj, accessToken);
        if (attributeToSubject.checkUserNameForNull())/* || attributeToSubject.checkForNullRealm()) */ { //TODO enable this null realm checking once userinfo code is fixed to emit "iss"
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        Hashtable<String, Object> customProperties = attributeToSubject.handleCustomProperties();
        if (accessToken != null) {
            customProperties.put(Constants.ACCESS_TOKEN, accessToken);
        }

        ProviderAuthenticationResult oidcResult = null;
        // The doMapping() will save the current time. ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS
        // In this RS scenario, the access_token is considered as expired. It should not be reused.
        // Since there are no expires_in attribute in the customProperties, it indicates the access_token is expired
        // **Unless we change the design. But this needs to think much further, such as: the customized WASOidcClient_ token... etc
        oidcResult = attributeToSubject.doMapping(customProperties, new Subject());

        return oidcResult;
    }

    @FFDCIgnore({ Exception.class })
    public ProviderAuthenticationResult createResultWithJose4JForJwt(
            String jwtString,
            ConvergedClientConfig clientConfig,
            OidcClientRequest oidcClientRequest) {
        // This is for JWT only at the writing time. caller set it already.
        // oidcClientRequest.setTokenType(OidcClientRequest.TYPE_JWT_TOKEN);
        ProviderAuthenticationResult oidcResult = null;

        String accessToken = jwtString;
        String refreshToken = null;
        String clientId = clientConfig.getClientId();
        try {

            JwtContext jwtContext = parseJwtWithoutValidation(jwtString);
            JwtClaims jwtClaims = parseJwtWithValidation(clientConfig, jwtString, jwtContext, oidcClientRequest);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "jwtClaims: " + jwtClaims);
            }
            OidcTokenImplBase idToken = new OidcTokenImplBase(jwtClaims, accessToken, refreshToken, clientId, oidcClientRequest.getTokenTypeNoSpace());
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "jwt token(idToken):" + idToken.toString());
            }

            oidcResult = checkForReusedJwt(clientConfig, idToken);
            if (oidcResult != null) {
                return oidcResult;
            }

            AttributeToSubject attributeToSubject = new AttributeToSubject(clientConfig, idToken);
            if (attributeToSubject.checkUserNameForNull()) {
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }

            Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

            Subject subject = null;
            if (clientConfig.isIncludeIdTokenInSubject()) {
                subject = new Subject();
                subject.getPrivateCredentials().add(idToken); // add the external IDToken
            }
            if (accessToken != null) {
                customProperties.put(Constants.ACCESS_TOKEN, accessToken);
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, accessToken.hashCode());
                customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            }

            //addJWTTokenToSubject(customProperties, idToken, clientConfig);

            //doIdAssertion(customProperties, payload, clientConfig);
            oidcResult = attributeToSubject.doMapping(customProperties, subject);
            //oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, username, subject, customProperties, null);

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Get exception", e);
            }
            // This is for JWT only at the writing time
            Object[] objs = new Object[] { e.getLocalizedMessage(), clientId };
            Tr.error(tc, "OIDC_CLIENT_JWT_VERIFY_ERR", objs);
            oidcClientRequest.setRsFailMsg(null, Tr.formatMessage(tc, "OIDC_CLIENT_JWT_VERIFY_ERR", objs));
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return oidcResult;
    }

    ProviderAuthenticationResult checkForReusedJwt(ConvergedClientConfig clientConfig, OidcTokenImplBase idToken) {
        if (clientConfig.getTokenReuse()) {
            // Tokens are allowed to be reused, so don't bother checking any further
            return null;
        }
        if (jtiCache.contain(idToken)) { // this has effect of adding token to cache if not already present.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Jwt token can only be submitted once. The issuer is " + idToken.getIssuer() + ", and JTI is " + idToken.getJwtId());
            }
            String errorMsg = Tr.formatMessage(tc, "JWT_DUP_JTI_ERR", new Object[] { idToken.getIssuer(), idToken.getJwtId() });
            Tr.error(tc, errorMsg); //add following default message
            return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }
        return null;
    }

}
