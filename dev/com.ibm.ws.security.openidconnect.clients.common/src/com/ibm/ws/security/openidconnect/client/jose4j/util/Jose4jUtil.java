/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.jose4j.util;

import java.security.AccessController;
import java.security.Key;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.common.web.WebSSOUtils;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.openidconnect.clients.common.AttributeToSubject;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.Constants;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.openidconnect.clients.common.JtiNonceCache;
import com.ibm.ws.security.openidconnect.clients.common.OIDCClientAuthenticatorUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionCache;
import com.ibm.ws.security.openidconnect.clients.common.OidcSessionInfo;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.clients.common.TraceConstants;
import com.ibm.ws.security.openidconnect.clients.common.UserInfoHelper;
import com.ibm.ws.security.openidconnect.jose4j.Jose4jValidator;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.common.jwt.JwtParsingUtils;

public class Jose4jUtil {

    private static final TraceComponent tc = Tr.register(Jose4jUtil.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String SIGNATURE_ALG_HS = "HS";
    private static final String SIGNATURE_ALG_RS = "RS";
    private static final String SIGNATURE_ALG_ES = "ES";
    private static final String SIGNATURE_ALG_NONE = "none";
    private final SSLSupport sslSupport;
    private static final JtiNonceCache jtiCache = new JtiNonceCache(); // Jose4jUil has only one instance

    WebSSOUtils webSsoUtils = new WebSSOUtils();

    private static boolean issuedBetaMessage = false;

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
        super();
        this.sslSupport = sslSupport;
    }

    private JwtClaims getClaimsFromAccessToken(String accessTokenStr) throws Exception {
        JwtClaims jwtClaims = null;
        String[] parts = accessTokenStr.split(Pattern.quote(".")); // split out the "parts" (header, payload and signature)

        if (parts.length > 1) {
            String claimsAsJsonString = new String(Base64.getDecoder().decode(parts[1]), "UTF-8");
            jwtClaims = JwtClaims.parse(claimsAsJsonString);
        } else {
            // do nothing
        }
        return jwtClaims;
    }

    private JwtClaims getClaimsFromIdToken(String tokenStr, ConvergedClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws Exception {
        String clientId = clientConfig.getClientId();
        JwtContext jwtContext = validateJwtStructureAndGetContext(tokenStr, clientConfig);
        JwtClaims jwtClaims = parseJwtWithValidation(clientConfig, jwtContext.getJwt(), jwtContext, oidcClientRequest);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "post jwtClaims: " + jwtClaims + " firstPass jwtClaims=" + jwtContext.getJwtClaims());
        }

        return jwtClaims;
    }

    private JwtClaims getClaimsFromUserInfo(String userInfoStr) throws Exception {
        return JwtClaims.parse(userInfoStr);
    }

    private OidcTokenImplBase getOidcToken(JwtClaims jwtClaims, String accessToken, String refreshToken, String clientId,
            String tokenTypeNoSpace) {
        return new OidcTokenImplBase(jwtClaims, accessToken, refreshToken, clientId, tokenTypeNoSpace);
    }

    // eliminate the FFDC since we will have the Tr.error and most of the Exception already handled by FFDC
    @FFDCIgnore({ Exception.class })
    public ProviderAuthenticationResult createResultWithJose4J(String responseState,
            Map<String, String> tokens,
            ConvergedClientConfig clientConfig,
            OidcClientRequest oidcClientRequest,
            SSLSocketFactory sslSocketFactory) {
        //oidcClientRequest.setTokenType(OidcClientRequest.TYPE_); // decided by the caller
        // This is for ID Token only at the writing time
        ProviderAuthenticationResult oidcResult = null;
        String idTokenStr = getIdToken(tokens, clientConfig);
        String originalIdTokenString = idTokenStr;
        String accessTokenStr = tokens.get(Constants.ACCESS_TOKEN);
        String refreshTokenStr = tokens.get(Constants.REFRESH_TOKEN);
        String clientId = clientConfig.getClientId();
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

        List<String> tokensOrderToFetchCallerClaims = clientConfig.getTokenOrderToFetchCallerClaims();
        try {          
            if ((idTokenStr == null || idTokenStr.isEmpty()) && tokensOrderToFetchCallerClaims.size() == 1) {
                // This is for ID Token only
                Tr.error(tc, "OIDC_CLIENT_IDTOKEN_REQUEST_FAILURE", new Object[] { clientId, clientConfig.getTokenEndpointUrl() });
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }
            
            Map<String, JwtClaims> tokenClaimsMap = new HashMap<String, JwtClaims>();

            JwtClaims idTokenClaims = getClaimsFromIdToken(idTokenStr, clientConfig, oidcClientRequest);
            OidcTokenImplBase idToken = getOidcToken(idTokenClaims, accessTokenStr, refreshTokenStr, clientId, Constants.TOKEN_TYPE_ID_TOKEN);
            String sub = idToken.getSubject();
            if (sub == null) {
                return new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
            }
            tokenClaimsMap.put(Constants.TOKEN_TYPE_ID_TOKEN, idTokenClaims);

            UserInfoHelper userInfoHelper = new UserInfoHelper(clientConfig, sslSupport);
            String userInfoStr = userInfoHelper.getUserInfoIfPossible(sub, accessTokenStr, sslSocketFactory, oidcClientRequest);
            JwtClaims userInfoClaims = null;
            if (userInfoStr != null) {
                try {
                    userInfoClaims = getClaimsFromUserInfo(userInfoStr);
                    
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid user info: " + userInfoStr);
                    }            
                }
            }

            if (tokensOrderToFetchCallerClaims.size() > 1 && tokensOrderToFetchCallerClaims.contains(Constants.TOKEN_TYPE_ACCESS_TOKEN)) {
                // access token
                JwtClaims accessTokenClaims = getClaimsFromAccessToken(accessTokenStr);
                tokenClaimsMap.put(Constants.TOKEN_TYPE_ACCESS_TOKEN, accessTokenClaims);
                if (userInfoStr != null) {
                    tokenClaimsMap.put(Constants.TOKEN_TYPE_USER_INFO, userInfoClaims);
                }       
            }

            String userName = this.getUserName(clientConfig, tokensOrderToFetchCallerClaims, tokenClaimsMap);
            if (userName == null || userName.isEmpty()) {
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
                props.put(Constants.ID_TOKEN, originalIdTokenString);
                props.put(Constants.ACCESS_TOKEN, accessTokenStr);
                if (refreshTokenStr != null) {
                    props.put(Constants.REFRESH_TOKEN, refreshTokenStr);
                }
                if (idToken != null) {
                    props.put(Constants.ID_TOKEN_OBJECT, idToken);
                }
                if (userInfoStr != null) {
                    props.put(Constants.USERINFO_STR, userInfoStr);
                }
                oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, null, null, props, null);
                if (isRunningBetaMode()) {
                    createWASOidcSession(oidcClientRequest, idToken.getJwtClaims(), clientConfig);
                }
                return oidcResult;
            }

            if (!clientConfig.isMapIdentityToRegistryUser()) {
                String realm = this.getRealmName(clientConfig, tokensOrderToFetchCallerClaims, tokenClaimsMap);
                if (realm != null && !realm.isEmpty()) {
                    customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
                }
                String uniqueSecurityName = this.getUniqueSecurityName(clientConfig, tokensOrderToFetchCallerClaims, tokenClaimsMap, userName);

                List<String> groups = getGroups(clientConfig, tokensOrderToFetchCallerClaims, tokenClaimsMap, realm);
                if (groups != null && !groups.isEmpty()) {
                    customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groups);
                }

                String uniqueID = new StringBuffer("user:").append(realm).append("/").append(uniqueSecurityName).toString();
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
            }


            if (clientConfig.isIncludeCustomCacheKeyInSubject() || clientConfig.isDisableLtpaCookie()) {
                //long storingTime = new Date().getTime();
                String customCacheKey = oidcClientRequest.getAndSetCustomCacheKeyValue(); //username + tokenStr.toString().hashCode();
                //customProperties.put(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS, Long.valueOf(storingTime));
                if (clientConfig.isIncludeCustomCacheKeyInSubject()) {
                    customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
                }
                customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE); // TODO checking?
            }
            Subject subject = null;
            if (clientConfig.isIncludeIdTokenInSubject()) {
                subject = new Subject();
                subject.getPrivateCredentials().add(idToken); // add the external IDToken
                customProperties.putAll(tokens); // add ALL tokens to props.
            } else {
                if (refreshTokenStr != null) {
                    customProperties.put(Constants.REFRESH_TOKEN, refreshTokenStr);
                }
                if (accessTokenStr != null) {
                    customProperties.put(Constants.ACCESS_TOKEN, accessTokenStr);
                }
            }
            if (idToken != null) {
                customProperties.put(Constants.ID_TOKEN_OBJECT, idToken); // pass back to authenticator
            }

            //addJWTTokenToSubject(customProperties, idToken, clientConfig);

            if (userInfoStr != null) {
                customProperties.put(Constants.USERINFO_STR, userInfoStr);
            }

            customProperties.put(ClientConstants.CREDENTIAL_STORING_TIME_MILLISECONDS, Long.valueOf(new Date().getTime())); // this is GMT/UTC time already

            //doIdAssertion(customProperties, payload, clientConfig);
            oidcResult = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, userName, subject, customProperties, null);
            if (oidcResult.getStatus() == AuthResult.SUCCESS && isRunningBetaMode()) {
                createWASOidcSession(oidcClientRequest, idToken.getJwtClaims(), clientConfig);
            }
        } catch (Exception e) {
            Tr.error(tc, "OIDC_CLIENT_IDTOKEN_VERIFY_ERR", new Object[] { e.getLocalizedMessage(), clientId });
            oidcResult = new ProviderAuthenticationResult(AuthResult.SEND_401, HttpServletResponse.SC_UNAUTHORIZED);
        }

        return oidcResult;
    }

    private void createWASOidcSession(OidcClientRequest oidcClientRequest, @Sensitive JwtClaims jwtClaims, ConvergedClientConfig clientConfig) throws MalformedClaimException {
        String configId = HashUtils.digest(clientConfig.getId());
        String iss = HashUtils.digest(jwtClaims.getIssuer());
        String sub = HashUtils.digest(jwtClaims.getSubject());
        String sid = HashUtils.digest(jwtClaims.getClaimValue("sid", String.class));
        String exp = String.valueOf(jwtClaims.getExpirationTime().getValueInMillis());

        OidcSessionInfo sessionInfo = new OidcSessionInfo(configId, iss, sub, sid, exp, clientConfig);

        OidcSessionCache oidcSessionCache = clientConfig.getOidcSessionCache();
        oidcSessionCache.insertSession(sessionInfo);

        String wasOidcSessionId = sessionInfo.getSessionId();
        Cookie cookie = webSsoUtils.createCookie(ClientConstants.WAS_OIDC_SESSION, wasOidcSessionId, oidcClientRequest.getRequest());
        cookie.setSecure(true);

        oidcClientRequest.getResponse().addCookie(cookie);
    }

    boolean isRunningBetaMode() {
        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
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

    public void checkJwtFormatAgainstConfigRequirements(String jwtString, ConvergedClientConfig clientConfig) throws JWTTokenValidationFailedException {
        if (JweHelper.isJwsRequired(clientConfig) && !JweHelper.isJws(jwtString)) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS", new Object[] { clientConfig.getId() });
            throw new JWTTokenValidationFailedException(errorMsg);
        }
        if (JweHelper.isJweRequired(clientConfig) && !JweHelper.isJwe(jwtString)) {
            String errorMsg = Tr.formatMessage(tc, "OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE", new Object[] { clientConfig.getId() });
            throw new JWTTokenValidationFailedException(errorMsg);
        }
    }

    @FFDCIgnore({ Exception.class })
    public JwtClaims parseJwtWithValidation(ConvergedClientConfig clientConfig,
            String jwtString,
            JwtContext jwtContext,
            OidcClientRequest oidcClientRequest) throws JWTTokenValidationFailedException, IllegalStateException, Exception {
        try {
            JsonWebStructure jsonStruct = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);

            Key key = getSignatureVerificationKeyFromJsonWebStructure(jsonStruct, clientConfig, oidcClientRequest);

            Jose4jValidator validator = new Jose4jValidator(key,
                    clientConfig.getClockSkewInSeconds(),
                    OIDCClientAuthenticatorUtil.getIssuerIdentifier(clientConfig),
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

    @FFDCIgnore({ Exception.class })
    public Key getSignatureVerificationKeyFromJsonWebStructure(JsonWebStructure jsonStruct, ConvergedClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws JWTTokenValidationFailedException {
        String kid = jsonStruct.getKeyIdHeaderValue();
        String x5t = jsonStruct.getX509CertSha1ThumbprintHeaderValue();
        Key key = null;
        Exception caughtException = null;
        try {
            key = getVerifyKey(clientConfig, kid, x5t);
        } catch (Exception e) {
            caughtException = e;
        }
        if (key == null && !SIGNATURE_ALG_NONE.equals(clientConfig.getSignatureAlgorithm())) {
            Object[] objs = new Object[] { clientConfig.getSignatureAlgorithm(), "" };
            if (caughtException != null) {
                objs = new Object[] { clientConfig.getSignatureAlgorithm(), caughtException.getLocalizedMessage() };
            }
            if (oidcClientRequest != null) {
                oidcClientRequest.setRsFailMsg(OidcClientRequest.NO_KEY, Tr.formatMessage(tc, "OIDC_CLIENT_NO_VERIFYING_KEY", objs));
                throw oidcClientRequest.error(true, tc, "OIDC_CLIENT_NO_VERIFYING_KEY", objs);
            } else {
                throw JWTTokenValidationFailedException.format(tc, "OIDC_CLIENT_NO_VERIFYING_KEY", objs);
            }
        }
        return key;
    }

    public Key getVerifyKey(ConvergedClientConfig clientConfig, String kid, String x5t) throws Exception {
        Key keyValue = null;
        String signatureAlgorithm = clientConfig.getSignatureAlgorithm();
        if (signatureAlgorithm == null) {
            return keyValue;
        }
        if (signatureAlgorithm.startsWith(SIGNATURE_ALG_HS)) {
            //keyValue = Base64Coder.getBytes(clientConfig.getSharedKey());
            keyValue = new HmacKey(clientConfig.getSharedKey().getBytes(ClientConstants.CHARSET));
        } else if (signatureAlgorithm.startsWith(SIGNATURE_ALG_RS) || signatureAlgorithm.startsWith(SIGNATURE_ALG_ES)) {
            if (clientConfig.getJwkEndpointUrl() != null || clientConfig.getJsonWebKey() != null) {
                JwKRetriever retriever = createJwkRetriever(clientConfig);
                keyValue = retriever.getPublicKeyFromJwk(kid, x5t, "sig", clientConfig.getUseSystemPropertiesForHttpClientConnections());
            } else {
                keyValue = clientConfig.getPublicKey();
            }
        } else if (SIGNATURE_ALG_NONE.equals(signatureAlgorithm)) {
            keyValue = null;
        }
        return keyValue;
    }

    public JwKRetriever createJwkRetriever(ConvergedClientConfig oidcClientConfig) {
        JwKRetriever retriever = null;
        if (oidcClientConfig != null) { // to support unittests, config cannot be null
            retriever = new JwKRetriever(oidcClientConfig.getId(), oidcClientConfig.getSslRef(), oidcClientConfig.getJwkEndpointUrl(), oidcClientConfig.getJwkSet(), this.sslSupport, oidcClientConfig.isHostNameVerificationEnabled(), oidcClientConfig.getJwkClientId(), oidcClientConfig.getJwkClientSecret(), oidcClientConfig.getSignatureAlgorithm());
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
            if (JweHelper.isJwe(jwtString)) {
                jwtString = JweHelper.extractJwsFromJweToken(jwtString, clientConfig, null);
            }
            JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(jwtString);
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
                if (clientConfig.isIncludeCustomCacheKeyInSubject()) {
                    customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, String.valueOf(accessToken.hashCode()));
                }
                customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            }
            customProperties.put(Constants.ACCESS_TOKEN_INFO, jwtClaims.getClaimsMap());

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

    /**
     * Verifies that the JWT is either a JWS or a JWE, depending on the client configuration, and does simple parsing of the
     * token to ensure it is formatted correctly. If the token is a JWE, this decrypts and extracts the JWS payload from the
     * token.
     */
    public JwtContext validateJwtStructureAndGetContext(String jwtString, ConvergedClientConfig clientConfig) throws Exception {
        checkJwtFormatAgainstConfigRequirements(jwtString, clientConfig);
        if (JweHelper.isJwe(jwtString)) {
            jwtString = JweHelper.extractJwsFromJweToken(jwtString, clientConfig, null);
        }
        return JwtParsingUtils.parseJwtWithoutValidation(jwtString);
    }

    public JwtClaims validateJwsSignature(JwtContext jwtContext, ConvergedClientConfig clientConfig) throws Exception {
        return validateJwsSignature(jwtContext, clientConfig, null);

    }

    public JwtClaims validateJwsSignature(JwtContext jwtContext, ConvergedClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws Exception {
        // TODO - update to use io.openliberty.security.common.jwt.jws.JwsVerificationKeyHelper and io.openliberty.security.common.jwt.jws.JwsSignatureVerifier
        JsonWebStructure jwStructure = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        Key key = getSignatureVerificationKeyFromJsonWebStructure(jwStructure, clientConfig, oidcClientRequest);

        // Clock skew and issuer aren't needed to validate the signature
        Jose4jValidator validator = new Jose4jValidator(key, 0L, null, clientConfig.getClientId(), clientConfig.getSignatureAlgorithm(), oidcClientRequest);
        return validator.validateJwsSignature((JsonWebSignature) jwStructure, jwtContext.getJwt());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Find the user name from one of the tokens.
     *
     * @param clientConfig
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @return
     * @throws MalformedClaimException
     */
    String getUserName(ConvergedClientConfig clientConfig, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap) throws MalformedClaimException {
        String userNameClaim = getUserNameClaim(clientConfig); 
        String userName = getClaimValueFromTokens(userNameClaim, String.class, tokensOrderToFetchCallerClaims, tokenClaimsMap);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "user name = '" + userName + "' and the user identifier = " + userNameClaim);
        }

        if (userName == null) {
            String attrUsedToCreateSubject = getUserNameAttribute(clientConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The " + attrUsedToCreateSubject + " config attribute is used");
                Tr.debug(tc, "There is no principal");
            }
            Tr.error(tc, "OIDC_CLIENT_JWT_MISSING_CLAIM", new Object[] { clientConfig.getClientId(), userNameClaim, attrUsedToCreateSubject });            
        }
        return userName;
    }
    
    String getUserNameAttribute(ConvergedClientConfig clientConfig) {
        String attrUsedToCreateSubject = clientConfig.isSocial() ? "userNameAttribute":"userIdentifier";
        if (clientConfig.getUserIdentifier() == null) {
            attrUsedToCreateSubject = clientConfig.isSocial() ? "userNameAttribute" : "userIdentityToCreateSubject"; 
        }
        return attrUsedToCreateSubject;
    }

    String getUserNameClaim(ConvergedClientConfig clientConfig) {
        String uid = clientConfig.getUserIdentifier();
        if (uid == null || uid.isEmpty()) {
            uid = clientConfig.getUserIdentityToCreateSubject();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "the user identifier = " + uid);
        }
        return uid;
    }

    /**
     * Find the realm name from one of the tokens.
     *
     * @param clientConfig
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @return
     * @throws MalformedClaimException
     */
    String getRealmName(ConvergedClientConfig clientConfig, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap) throws MalformedClaimException {
        String realm = clientConfig.getRealmName();
        if (realm == null) {
            for (String claim : getRealmNameClaim(clientConfig)) {
                realm = getClaimValueFromTokens(claim, String.class, tokensOrderToFetchCallerClaims, tokenClaimsMap);
                if (realm != null && !realm.isEmpty()) {
                    break;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "realm name = ", realm);
        }
        return realm;
    }

    /**
     * Find the claim(s) of the user name from the configuration.
     *
     * @param clientConfig
     * @return
     */
    List<String> getRealmNameClaim(ConvergedClientConfig clientConfig) {
        List<String> claims = new ArrayList<String>();
        String claim = clientConfig.getRealmIdentifier();
        if (claim != null && !claim.isEmpty()) {
            claims.add(claim);
        }
        claims.add(ClientConstants.ISS);
        return claims;
    }

    /**
     * Find the unique security name from one of the tokens.
     *
     * @param clientConfig
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @param userName
     * @return
     * @throws MalformedClaimException
     */
    String getUniqueSecurityName(ConvergedClientConfig clientConfig, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap, String userName) throws MalformedClaimException {
        String uniqueSecurityNameClaim = getUniqueSecurityNameClaim(clientConfig);
        String uniqueSecurityName = getClaimValueFromTokens(uniqueSecurityNameClaim, String.class, tokensOrderToFetchCallerClaims, tokenClaimsMap);

        if (uniqueSecurityName == null || uniqueSecurityName.isEmpty()) {
            uniqueSecurityName = userName;
        }
        return uniqueSecurityName;
    }

    /**
     * Find the claim of the unique security name from the configuration.
     *
     * @param clientConfig
     * @return
     */
    String getUniqueSecurityNameClaim(ConvergedClientConfig clientConfig) {
        return clientConfig.getUniqueUserIdentifier();
    }

    /**
     * Build the list of groups using the group id(s) previously found from one of the tokens.
     *
     * @param clientConfig
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @param realm
     * @return
     * @throws MalformedClaimException
     */
    List<String> getGroups(ConvergedClientConfig clientConfig, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap, String realm) throws MalformedClaimException {
        List<String> groups = new ArrayList<String>();
        List<String> groupIds = getGroupIds(clientConfig, tokensOrderToFetchCallerClaims, tokenClaimsMap);
        for (String gid : groupIds) {
            String group = new StringBuffer("group:").append(realm).append("/").append(gid).toString();
            groups.add(group);
        }
        return groups;
    }

    /**
     * Find the group id(s) from one of the tokens.
     *
     * @param clientConfig
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @return
     * @throws MalformedClaimException
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(MalformedClaimException.class)
    List<String> getGroupIds(ConvergedClientConfig clientConfig, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap) throws MalformedClaimException {
        String groupIdsClaim = getGroupIdsClaim(clientConfig);
        List<String> groupIds = null;
        try {
            groupIds = getClaimValueFromTokens(groupIdsClaim, List.class, tokensOrderToFetchCallerClaims, tokenClaimsMap);
        } catch (MalformedClaimException e) {
        } finally {
            if (groupIds == null) {
                groupIds = new ArrayList<String>();
                String groupIdsStr = getClaimValueFromTokens(groupIdsClaim, String.class, tokensOrderToFetchCallerClaims, tokenClaimsMap);
                if (groupIdsStr != null) {
                    groupIds.add(groupIdsStr);
                }
            }
        }
        if (groupIds.size() > 0 && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "groupIds=" + groupIds.toString() + " groups size = ", groupIds.size());
        }
        return groupIds;
    }

    /**
     * Find the claim of the group id(s) from the configuration.
     *
     * @param clientConfig
     * @return
     */
    String getGroupIdsClaim(ConvergedClientConfig clientConfig) {
        return clientConfig.getGroupIdentifier();
    }

    /**
     * Jakarta way of implementation: get claim value from one of tokens for a specific claim.
     *
     * @param <T>
     * @param claim
     * @param claimType
     * @param tokensOrderToFetchCallerClaims
     * @param tokenClaimsMap
     * @return
     * @throws MalformedClaimException
     */
    <T> T getClaimValueFromTokens(String claim, Class<T> claimType, List<String> tokensOrderToFetchCallerClaims, Map<String, JwtClaims> tokenClaimsMap) throws MalformedClaimException {
        if (claim == null || claim.isEmpty()) {
            return null;
        }
        T claimValue = null;
        for (String token : tokensOrderToFetchCallerClaims) {
            JwtClaims tokenClaims = tokenClaimsMap.get(token);
            if (tokenClaims != null) {
                claimValue = tokenClaims.getClaimValue(claim, claimType);
                if (valueExistsAndIsNotEmpty(claimValue, claimType))
                    break;
            }
        }
        return claimValue;
    }

    /**
     * Jakarta way of implementation: check whether value exists.
     *
     * @param <T>
     * @param claimValue
     * @param claimType
     * @return
     */
    @SuppressWarnings("rawtypes")
    <T> boolean valueExistsAndIsNotEmpty(T claimValue, Class<T> claimType) {
        if (claimValue == null) {
            return false;
        }
        if (claimType.equals(String.class) && ((String) claimValue).isEmpty()) {
            return false;
        }
        if (claimType.equals(Set.class) && ((Set) claimValue).isEmpty()) {
            return false;
        }
        if (claimType.equals(List.class) && ((List) claimValue).isEmpty()) {
            return false;
        }
        return true;
    }
}
