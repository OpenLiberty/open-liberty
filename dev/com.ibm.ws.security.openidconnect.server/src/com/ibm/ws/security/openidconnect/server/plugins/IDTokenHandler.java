/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jose4j.keys.HmacKey;
import org.osgi.service.component.ComponentContext;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenHelper;
import com.ibm.oauth.core.internal.oauth20.tokentype.OAuth20TokenTypeHandler;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.common.token.propagation.TokenPropagationHelper;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.jose4j.JWTData;
import com.ibm.ws.security.oauth20.plugins.jose4j.JwtCreator;
import com.ibm.ws.security.oauth20.plugins.jose4j.OidcUserClaims;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.server.internal.HashUtils;
import com.ibm.ws.security.openidconnect.token.IDToken;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.security.openidconnect.token.Payload;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;
import com.ibm.wsspi.security.openidconnect.IDTokenMediator;

/*
 * This is a singleton instance.
 * Do not use it for multiple instance
 */
public class IDTokenHandler implements OAuth20TokenTypeHandler {

    private static final TraceComponent tc = Tr.register(IDTokenHandler.class);
    private static final String CFG_KEY_ISSUER_IDENTIFIER = "issuerIdentifier";
    private static final String JTI_CLAIM = "jti";
    private static final String SIGNATURE_ALG_NONE = "none";
    private static final String SIGNATURE_ALG_HS256 = "HS256";
    private static final String SIGNATURE_ALG_RS256 = "RS256";
    private static final String SHARED_KEY = "sharedKey";

    public static final String AT_HASH = "at_hash"; // idtoken

    private static final int IDTOKEN_LIFETIME_DEFAULT = 7200;
    private volatile SecurityService securityService;

    /** {@inheritDoc} */
    @Override
    public void init(OAuthComponentConfiguration config) {
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void deactivate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    /** {@inheritDoc} */
    @Override
    public String getTypeTokenType() {
        return Constants.ID_TOKEN;
    }

    /** {@inheritDoc} */
    @Override
    public OAuth20Token createToken(@Sensitive Map<String, String[]> tokenMap) {

        OAuth20Token token = null;
        String sharedKey = OAuth20Util.getValueFromMap(SHARED_KEY, tokenMap);
        String componentId = OAuth20Util.getValueFromMap(OAuth20Constants.COMPONENTID, tokenMap);
        String accessToken = OAuth20Util.getValueFromMap(OAuth20Constants.ACCESS_TOKEN, tokenMap);
        String clientId = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_ID, tokenMap);
        String username = OAuth20Util.getValueFromMap(OAuth20Constants.USERNAME, tokenMap);
        String redirectUri = OAuth20Util.getValueFromMap(OAuth20Constants.REDIRECT_URI, tokenMap);
        String stateId = getStateId(tokenMap);
        String[] scopes = tokenMap.get(OAuth20Constants.SCOPE);
        String grantType = OAuth20Util.getValueFromMap(OAuth20Constants.GRANT_TYPE, tokenMap);

        OidcServerConfig oidcServerConfig = OIDCProvidersConfig.getOidcServerConfigForOAuth20Provider(componentId);
        int lifetime = getLifetime(oidcServerConfig);
        String idTokenString = null;
        String signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();

        if ("none".equals(signatureAlgorithm)) {// no need to sign
            Payload payload = createPayload(tokenMap, oidcServerConfig);
            Object signingKey = getSigningKey(signatureAlgorithm, sharedKey, oidcServerConfig);
            idTokenString = createIdTokenAsString(payload, signatureAlgorithm, signingKey, accessToken);
        } else {
            String accessTokenHash = null;
            if (accessToken != null) {
                accessTokenHash = JsonTokenUtil.accessTokenHash(accessToken);
            }

            JWTData jwtData = new JWTData(sharedKey, oidcServerConfig, JWTData.TYPE_ID_TOKEN); // Let's get the key here to get the same error as old code
            boolean idSpi = isIDTokenMediatorSpi(); //check if spi is loaded
            String jsonFromSpi = null;
            if (idSpi) {
                synchronized (this) {
                    boolean subjectPushed = false;
                    Subject priorSubject = null;
                    try {
                        // 238871 - push authenticated subject onto thread for mediator to use in token creation.
                        priorSubject = TokenPropagationHelper.getRunAsSubject();
                        subjectPushed = TokenPropagationHelper.pushSubject(username);

                        //1. pass tokenMap to SPI as input parameter
                        //2. expect SPI to return the claims as Json String
                        jsonFromSpi = getIDTokenClaimsFromMediatorSpi(tokenMap);
                        // 238871 - now we're done, restore the thread.
                    } finally {
                        if (subjectPushed) {
                            TokenPropagationHelper.setRunAsSubject(priorSubject);
                        }
                    }
                }
                if (jsonFromSpi != null) {
                    //3. merge claims and create ID token
                    idTokenString = JwtCreator.createJwtAsStringForSpi(jsonFromSpi, oidcServerConfig, clientId, username, scopes, lifetime,
                                                                       tokenMap, grantType, accessTokenHash, jwtData);
                }

            }

            if (jsonFromSpi == null) {
                Map<String, Object> userClaims = getCustomClaims(tokenMap, oidcServerConfig);
                if (accessTokenHash != null) {
                    userClaims.put(AT_HASH, accessTokenHash);
                }
                OAuth20Provider oauth20Provider = ProvidersService.getOAuth20Provider(componentId);
                boolean useMicroProfileTokenFormat = false;//oauth20Provider == null? false: oauth20Provider.isMpJwt(); //Aruna TODO:
                // make the above false for now since it is not working with mpJwt feature
                idTokenString = JwtCreator.createJwtAsString(oidcServerConfig,
                                                             clientId,
                                                             username,
                                                             scopes,
                                                             lifetime,
                                                             tokenMap,
                                                             userClaims,
                                                             jwtData,
                                                             useMicroProfileTokenFormat);
            }

        }
        String tokenId = HashUtils.digest(idTokenString);
        Map<String, String[]> externalClaims = OAuth20TokenHelper.getExternalClaims(tokenMap);
        token = new IDTokenImpl(tokenId, idTokenString, componentId, clientId, username, redirectUri, stateId, scopes, lifetime, externalClaims, grantType);
        if (token != null) {
            ((OAuth20TokenImpl) token).setAccessTokenKey(accessToken);
        }
        return token;

    }

    /**
     * @return
     */
    private boolean isIDTokenMediatorSpi() {
        if (ConfigUtils.getIdTokenMediatorService().size() > 0) {
            return true;
        }
        return false;
    }

    protected String getIDTokenClaimsFromMediatorSpi(Map<String, String[]> tokenMap) {
        String idStr = null;
        Iterator<IDTokenMediator> idMediators = ConfigUtils.getIdTokenMediatorService().getServices();
        if (idMediators.hasNext()) {
            IDTokenMediator idMediator = idMediators.next();

            idStr = idMediator.mediateToken(tokenMap);
        }
        return idStr;
    }

    private int getLifetime(OidcServerConfig oidcServerConfig) {
        int lifetime = IDTOKEN_LIFETIME_DEFAULT;
        if (oidcServerConfig != null) {
            Long lifeValue = oidcServerConfig.getIdTokenLifetime();
            if (lifeValue < Integer.MAX_VALUE && lifeValue > 0) {
                lifetime = lifeValue.intValue();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The value of idTokenLifetime exceeds maximum number of integer: " + lifeValue);
                }
                throw new RuntimeException("The value of idTokenLifetime exceeds maximum number of integer: " + lifeValue);
            }
        }
        return lifetime;
    }

    private String getStateId(Map<String, String[]> tokenMap) {
        String stateId = OAuth20Util.getValueFromMap(OAuth20Constants.STATE_ID, tokenMap);
        if (stateId == null) {
            stateId = OAuth20Util.generateUUID();
        }
        return stateId;
    }

    private Payload createPayload(@Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        Payload payload = new Payload();
        addRequiredClaims(payload, tokenMap, oidcServerConfig);
        validateRequiredClaims(payload);
        addOptionalClaims(payload, tokenMap, oidcServerConfig);
        addCustomClaims(payload, tokenMap, oidcServerConfig);
        addExternalClaims(payload, tokenMap);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "debug:" + payload);
        }
        return payload;
    }

    /**
     * @param payload
     * @param tokenMap
     */
    private void addExternalClaims(Payload payload, Map<String, String[]> tokenMap) {

        Set<Map.Entry<String, String[]>> entries = tokenMap.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            if (key.startsWith(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX)) {
                String shortKey = key.substring(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX_LENGTH);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " longKey:" + key + " shortKey:" + shortKey);
                }
                //payload.put(shortKey, OAuth20Util.getValueFromMap(key, tokenMap));
                List<String> list = getListFromMap(key, tokenMap);
                if (!list.isEmpty()) {
                    if (list.size() > 1) {
                        payload.put(shortKey, list);
                    } else {
                        payload.put(shortKey, OAuth20Util.getValueFromMap(key, tokenMap));
                    }
                }

            }
        }
    }

    List<String> getListFromMap(String key, Map<String, String[]> m) {
        List<String> list = new ArrayList<String>();
        String values[] = m.get(key);
        if (values != null && values.length > 0) {
            int max = values.length;
            for (int i = 0; i < max; i++) {
                list.add(values[i]);
            }
        }
        return list;
    }

    private void addRequiredClaims(Payload payload, @Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        payload.setIssuer(getIssuerIdentifier(tokenMap, oidcServerConfig));
        payload.setSubject(OAuth20Util.getValueFromMap(OAuth20Constants.USERNAME, tokenMap));
        payload.setAudience(OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_ID, tokenMap));
        long issuedAtTimeInSeconds = getIssuedAtTimeInSeconds();
        payload.setIssuedAtTimeSeconds(issuedAtTimeInSeconds);
        payload.setExpirationTimeSeconds(issuedAtTimeInSeconds + getLifetime(oidcServerConfig));
        String nonce = OAuth20Util.getValueFromMap(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, tokenMap);
        if (nonce != null) {
            payload.setNonce(nonce);
        }
    }

    private String getIssuerIdentifier(@Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        String issuerIdentifier = oidcServerConfig.getIssuerIdentifier();
        if (issuerIdentifier == null || issuerIdentifier.isEmpty()) {
            issuerIdentifier = OAuth20Util.getValueFromMap(CFG_KEY_ISSUER_IDENTIFIER, tokenMap);
        }
        return issuerIdentifier;
    }

    private long getIssuedAtTimeInSeconds() {
        return new Date().getTime() / 1000;
    }

    /**
     * This method should never throw the exception since the validation
     * performed by OAuth20ComponentImpl and the type handlers guarantees
     * that there is an issuer, a subject, and audience. Our code also
     * explicitly sets the time values and they are guaranteed to exist.
     * If this exception is ever thrown it means that the code was broken
     * since the values for the required claims must always exist.
     *
     * @param payload
     */
    protected void validateRequiredClaims(Payload payload) {
        String[] requiredClaims = new String[] { "iss", "sub", "aud", "exp", "iat" };
        StringBuffer sb = new StringBuffer("ID Token is missing required claims:");
        boolean valid = true;

        for (String requiredClaim : requiredClaims) {
            if (payload.get(requiredClaim) == null) {
                sb.append(" ");
                sb.append(requiredClaim);
                valid = false;
            }
        }

        if (valid != true) {
            throw new RuntimeException(sb.toString());
        }
    }

    private void addOptionalClaims(Payload payload, @Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        if (oidcServerConfig.isJTIClaimEnabled()) {
            payload.put(JTI_CLAIM, OAuthUtil.getRandom(16));
        }
    }

    private void addCustomClaims(Payload payload, @Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        Map<String, Object> userClaims = getCustomClaims(tokenMap, oidcServerConfig);
        if (userClaims != null) {
            payload.putAll(userClaims);
        }
    }

    private Map<String, Object> getCustomClaims(@Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        if (oidcServerConfig.isCustomClaimsEnabled()) {
            UserClaimsRetrieverService userClaimsRetrieverService = ConfigUtils.getUserClaimsRetrieverService();
            if (userClaimsRetrieverService != null) {
                String username = OAuth20Util.getValueFromMap(OAuth20Constants.USERNAME, tokenMap);
                String groupIdentifier = oidcServerConfig.getGroupIdentifier();
                UserClaims oauthUserClaims = userClaimsRetrieverService.getUserClaims(username, groupIdentifier);
                if (oauthUserClaims != null) { // userName != null
                    if (oauthUserClaims.isEnabled()) {
                        OidcUserClaims oidcUserClaims = new OidcUserClaims(oauthUserClaims);
                        oidcUserClaims.addExtraClaims(oidcServerConfig);
                        return oidcUserClaims.asMap();
                    } else {
                        return oauthUserClaims.asMap();
                    }
                }
            }
        }
        return new HashMap<String, Object>();
    }

    @Sensitive
    private Object getSigningKey(String signatureAlgorithm, @Sensitive String sharedKey, OidcServerConfig oidcServerConfig) {
        Object keyValue = null;
        if (oidcServerConfig.isJwkEnabled() && SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
            keyValue = oidcServerConfig.getJSONWebKey();
        } else {
            if (SIGNATURE_ALG_HS256.equals(signatureAlgorithm)) {
                keyValue = Base64Coder.getBytes(sharedKey);
            } else if (SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                keyValue = getRSAPrivateKey(oidcServerConfig);
            }
        }
        return keyValue;
    }

    /*
     * Handle the signingKey here to get the same error messages
     */
    @FFDCIgnore(Exception.class)
    @Sensitive
    public static JWTData getSigningKey(@Sensitive String sharedKey, OidcServerConfig oidcServerConfig) {
        Key keyValue = null;
        String keyId = null;
        String signatureAlgorithm = oidcServerConfig.getSignatureAlgorithm();
        if (oidcServerConfig.isJwkEnabled() && SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
            JSONWebKey jwk = oidcServerConfig.getJSONWebKey();
            keyValue = jwk.getPrivateKey();
            keyId = jwk.getKeyID();
        } else {
            if (SIGNATURE_ALG_HS256.equals(signatureAlgorithm)) {
                try {
                    keyValue = new HmacKey(sharedKey.getBytes("UTF-8"));;
                } catch (UnsupportedEncodingException e) {
                    // TODO This won't happen since we hardcode as UTF-8
                }

            } else if (SIGNATURE_ALG_RS256.equals(signatureAlgorithm)) {
                try {
                    keyValue = oidcServerConfig.getPrivateKey();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RSAPrivateKey: " + (keyValue instanceof RSAPrivateKey));
                    }
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception obtaining the private key: " + e);
                    }
                    throw new RuntimeException("Unable to create signed ID token due to exception: " + e);
                }
            }
        }
        return new JWTData(keyValue, keyId);
    }

    private String createIdTokenAsString(Payload payload, String signatureAlgorithm, @Sensitive Object signingKey, @Sensitive String accessToken) {
        String jwtString = null;
        try {
            if (requiresSigning(signatureAlgorithm)) {
                jwtString = createSignedIdToken(payload, signatureAlgorithm, signingKey, accessToken);
            } else {
                jwtString = createPlainTextIdToken(payload);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating the id_token string: " + e);
            }
            throw new RuntimeException("Exception creating the id_token string: " + e);
        }
        return jwtString;
    }

    private boolean requiresSigning(String signatureAlgorithm) {
        return SIGNATURE_ALG_NONE.equals(signatureAlgorithm) == false;
    }

    private String createSignedIdToken(Payload payload, String signatureAlgorithm, @Sensitive Object signingKey,
                                       @Sensitive String accessToken) throws InvalidKeyException, SignatureException {
        JWSHeader jwsHeader = new JWSHeader();
        jwsHeader.setAlgorithm(signatureAlgorithm);
        if (signingKey instanceof JWK) {
            JWK jwk = (JWK) signingKey;
            signingKey = jwk.getPrivateKey();
            jwsHeader.setKeyId(jwk.getKeyID());
        }
        IDToken idToken = new IDToken(jwsHeader, payload, signingKey, accessToken);
        return idToken.getSignedJWTString();
    }

    @FFDCIgnore(Exception.class)
    @Sensitive
    private Object getRSAPrivateKey(OidcServerConfig oidcServerConfig) {
        PrivateKey rsaPrivateKey = null;
        try {
            rsaPrivateKey = oidcServerConfig.getPrivateKey();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "RSAPrivateKey: " + (rsaPrivateKey instanceof RSAPrivateKey));
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception obtaining the private key: " + e);
            }
            throw new RuntimeException("Unable to create signed ID token due to exception: " + e);
        }
        return rsaPrivateKey;
    }

    private String createPlainTextIdToken(Payload payload) {
        JWSHeader jwsHeader = new JWSHeader();
        IDToken idToken = new IDToken(jwsHeader, payload);
        return idToken.getJWTString();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getKeysTokenType(AttributeList attributeList) throws OAuthException {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void validateRequestTokenType(AttributeList attributeList, List<OAuth20Token> tokens) throws OAuthException {
        // Nothing to validate since the id_token is not sent back in a resource request by the client
    }

    /** {@inheritDoc} */
    @Override
    public void buildResponseTokenType(AttributeList attributeList, List<OAuth20Token> tokens) {
        // Nothing to add to response since the id_token is not sent back in a resource request by the client
    }

}
