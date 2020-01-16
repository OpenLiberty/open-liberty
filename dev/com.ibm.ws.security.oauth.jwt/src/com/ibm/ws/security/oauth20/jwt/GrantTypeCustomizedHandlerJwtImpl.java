/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.jwt;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.oauth.core.internal.oauth20.granttype.OAuth20GrantTypeHandler;
import com.ibm.oauth.core.internal.oauth20.token.OAuth20TokenFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.plugins.JwtGrantTypeHandlerFactory;
import com.ibm.ws.security.oauth20.util.BoundedCommonCache;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.common.cl.BuildResponseTypeUtil;
import com.ibm.ws.security.openidconnect.common.cl.JWTVerifier;
import com.ibm.ws.security.openidconnect.server.plugins.OIDCProvidersConfig;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

public class GrantTypeCustomizedHandlerJwtImpl implements OAuth20GrantTypeHandler, JwtGrantTypeHandlerFactory {
    protected static final TraceComponent tc = Tr.register(GrantTypeCustomizedHandlerJwtImpl.class,
                                                           TraceConstants.TRACE_GROUP,
                                                           TraceConstants.MESSAGE_BUNDLE);

    JwtGrantTypeHandlerConfig _jwtGrantTypeHandlerConfig = null;
    final static ArrayList<String> _emptyList = new ArrayList<String>();

    @Override
    public List<String> getKeysGrantType(@Sensitive AttributeList attributeList)
                    throws OAuthException {
        // The JWT does not have cached data, such as: code in authorization_code
        // we won't provide a cached key to get the cached data
        return _emptyList; // 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FFDCIgnore({ InvalidGrantException.class })
    public void validateRequestGrantType(@Sensitive AttributeList attributeList,
                                         List<OAuth20Token> tokens) throws OAuthException {
        // let's debug what attributes we get
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            List<Attribute> attributes = attributeList.getAllAttributes();
            for (Attribute attribute : attributes) {
                // String attribName = attribute.getName();
                Tr.debug(tc, "attrib: " + attribute.getName() + " :" + attribute.toString());
            }
        }

        // verification
        String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String client_secret = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_SECRET);
        OidcServerConfig oidcServerConfig = getOidcServerConfig(attributeList);

        String tokenString = getTokenString(attributeList);

        // get the allow skew seconds
        long lSkewSeconds = _jwtGrantTypeHandlerConfig.getJwtClockSkew();

        // verify the JWT Token signature and basic requirements with open source 
        // This get its payload as well
        JWTVerifier jwtVerifier = null;
        try {
            jwtVerifier = getJwtVerifier(clientId, client_secret, tokenString, lSkewSeconds, oidcServerConfig);
            jwtVerifier.verifySignature();
        } catch (InvalidGrantException e) {
            throw e;
        } catch (Exception e) {
            // when verify and de-serialize failed, it should throw an Exception 
            String message = e.getMessage();
            if (message == null || message.isEmpty()) {
                message = e.toString();
            }
            // Tr.error and ffdc had been handled by JWTVerifier or its sub-routines
            throw new InvalidGrantException(message, e);
        }

        // Doing further verifications on the JWT token
        // They ought to throw OAuth20Exception only        
        verifyJwtContentAndAddAttrib(attributeList,
                                     jwtVerifier.getPayload(),
                                     clientId,
                                     oidcServerConfig,
                                     lSkewSeconds);
    }

    /**
     * Example of data in the attributeList:
     * {name: assertion type: urn:ibm:names:body:param values: [jwt-header.jwt.payload.signature]}
     * {name: grant_type type: urn:ibm:names:body:param values: [urn:ietf:params:oauth:grant-type:jwt-bearer]}
     * 
     */
    @Override
    public List<OAuth20Token> buildTokensGrantType(AttributeList attributeList,
                                                   OAuth20TokenFactory tokenFactory,
                                                   List<OAuth20Token> tokens) {

        List<OAuth20Token> tokenList = new ArrayList<OAuth20Token>();
        // generate access_token and id_token, but no refresh_token
        // token lifetime=accesToken LifeTime 

        String clientId = attributeList.getAttributeValueByName(OAuth20Constants.CLIENT_ID);
        String[] redirectUris = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.REDIRECT_URI,
                                                                              OAuth20Constants.ATTRTYPE_PARAM_BODY);
        // It's handled in verifyAndAddAttrib. But it may be an empty string[] instead of null, if none defined in JWT token 
        String[] scopes = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.CLAIM_NAME_SCOPE, OAuth20Constants.ATTRTYPE_PARAM_JWT);

        String username = attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLAIM_NAME_SUB, OAuth20Constants.ATTRTYPE_PARAM_JWT);

        String stateId = null;
        String redirectUri = redirectUris == null ? null : (redirectUris.length > 0 ? redirectUris[0] : null);
        Map<String, String[]> accessTokenMap = tokenFactory.buildTokenMap(clientId, username, redirectUri,
                                                                          stateId, scopes, (OAuth20Token) null, OAuth20Constants.GRANT_TYPE_JWT);

        OAuth20Util.populateJwtAccessTokenData(attributeList, accessTokenMap);
        //for spi
        String proxy = attributeList
                        .getAttributeValueByName(OAuth20Constants.PROXY_HOST);
        accessTokenMap.put(OAuth20Constants.PROXY_HOST, new String[] { proxy });

        OAuth20Token access = tokenFactory.createAccessToken(accessTokenMap);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "access token is " + access);
        }
        if (access != null)
            tokenList.add(access);

        return tokenList;
    }

    @Override
    public void buildResponseGrantType(AttributeList attributeList, List<OAuth20Token> tokens) {
        BuildResponseTypeUtil.buildResponseGrantType(attributeList, tokens);
    }

    // subroutines for verifying the incoming JWT token
    // OAuthException ought to handle a Tr.error and FFDC before throw out
    protected void verifyJwtContentAndAddAttrib(@Sensitive AttributeList attributeList, JWTPayload payload, String clientId, OidcServerConfig oidcServerConfig,
                                                long lSkewSeconds) throws OAuthException {

        String issuerIdentifier = attributeList.getAttributeValueByName(OIDCConstants.ISSUER_IDENTIFIER);
        String[] clientRedirectUris = attributeList.getAttributeValuesByNameAndType(OIDCConstants.CLIENT_REDIRECT_URI,
                                                                                    OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
        // 
        String iss = getString(payload.get(OIDCConstants.PAYLOAD_ISSUER));
        verifyJwtIssuer(iss, clientRedirectUris, clientId);
        attributeList.setAttribute(OAuth20Constants.CLAIM_NAME_ISS, OAuth20Constants.ATTRTYPE_PARAM_JWT, new String[] { (String) iss });

        String sub = getString(payload.get(OIDCConstants.PAYLOAD_SUBJECT));
        verifyJwtSub(sub);
        attributeList.setAttribute(OAuth20Constants.CLAIM_NAME_SUB, OAuth20Constants.ATTRTYPE_PARAM_JWT, new String[] { (String) sub });

        // audience could be an String[]
        String[] aud = getStrings(payload.get(OIDCConstants.PAYLOAD_AUDIENCE));
        String strOidcConfigIssuerIdentifier = null;
        if (oidcServerConfig != null) {
            strOidcConfigIssuerIdentifier = oidcServerConfig.getIssuerIdentifier();
        }

        if (strOidcConfigIssuerIdentifier != null && !strOidcConfigIssuerIdentifier.isEmpty()) {
            verifyJwtAudience(aud, strOidcConfigIssuerIdentifier, (String) null);
        } else {
            // TODO make sure we can convert back to token endpoint in this way
            String strTokenEndpoint = issuerIdentifier + "/token";
            verifyJwtAudience(aud, issuerIdentifier, strTokenEndpoint);
        }

        attributeList.setAttribute(OAuth20Constants.CLAIM_NAME_AUD, OAuth20Constants.ATTRTYPE_PARAM_JWT, aud);

        Date currentDate = new Date();

        Long exp = getLong(payload, OIDCConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS); // exp
        verifyExpiredTime(exp, currentDate, lSkewSeconds);
        attributeList.setAttribute(OAuth20Constants.CLAIM_NAME_EXP, OAuth20Constants.ATTRTYPE_PARAM_JWT, new String[] { ((Long) exp).toString() });

        Long iat = getLong(payload, OIDCConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS); // iat
        checkIatTime(iat, currentDate, lSkewSeconds);

        Long nbf = getLong(payload, OIDCConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS); // nbf
        checkNotBeforeTime((Long) nbf, currentDate, (long) lSkewSeconds); // 
        Object jti = payload.get("jti");
        checkJti(jti, iss);

        String[] scopes = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.SCOPE,
                                                                        OAuth20Constants.ATTRTYPE_PARAM_OAUTH_REQUEST);
        if (scopes != null) {
            attributeList.setAttribute(OAuth20Constants.CLAIM_NAME_SCOPE, OAuth20Constants.ATTRTYPE_PARAM_JWT, scopes);
        }

    }

    /**
     * @param scopes
     * @return
     */
    protected String convertToString(String[] scopes) {
        StringBuffer st = new StringBuffer("");
        boolean bFirst = true;
        for (String scope : scopes) {
            if (!bFirst) {
                st.append(" ");
            }
            st.append(scope);
            bFirst = false;
        }
        return st.toString();
    }

    /**
     * @param jti
     */
    protected void checkJti(Object jti, String iss) throws OAuth20Exception {
        if (jti == null) {
            return; // jti is optional
        }

        String strJti = getString(jti);

        // jtiCache is per plugin for now
        BoundedCommonCache<String> jtiCache = _jwtGrantTypeHandlerConfig.getJtiCache();
        String jtiKey = iss + " - " + strJti;

        // if a duplicated jti key already exist in the same plugin
        if (jtiCache.contains(jtiKey)) {
            Tr.error(tc, "JWT_TOKEN_DUP_JTI_ERR",
                     new Object[] { iss, jti });
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_DUP_JTI_ERR", iss, strJti),
                            //"Another JWT Token with same 'iss':" + iss +
                            // " and 'jti':" + strJti + " had been submitted already",
                            (Throwable) null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "adding jti key: " + jtiKey);
        }
        jtiCache.put(jtiKey);
    }

    /**
     * @param nbf
     * @param currentDate
     * @param string
     */
    protected void checkNotBeforeTime(Long nbfSeconds, Date currentDate, long lSkew) throws OAuth20Exception {
        if (nbfSeconds == null) {
            return; // nbf is optional
        }
        long lNbf = nbfSeconds.longValue() * 1000; // MilliSecond
        Date nbfDate = new Date(lNbf);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT token NBF : " + nbfDate);
        }

        // Not-before means current time can not before the nbf.
        // If nbf is 12:00, then current time 11:59 or 11:55 is not valid
        // but consider the clock skew, current time allow to  + clock skew....
        // so, 11:55:01 is still OK ( 11:55:01 + 5 minutes clock skew = 12:00:01) 
        Date allowDate = new Date(currentDate.getTime() + (lSkew * 1000));
        if (nbfDate.after(allowDate)) { // does not accept when the current date time is after nbf
            Tr.error(tc, "JWT_TOKEN_TOKEN_BEFORE_NBF_ERR", allowDate.toString(), nbfDate.toString());
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_BEFORE_ERR", nbfDate.toString()),
                            (Throwable) null);
        }
    }

    /**
     * @param iat
     * @param currentDate
     * @param lMaxJwtLifetimeAllowed
     */
    protected void checkIatTime(Long iatSeconds, Date currentDate, long lSkewSeconds) throws OAuth20Exception {
        boolean bJwtIatRequired = _jwtGrantTypeHandlerConfig.isJwtIatRequired();
        if (iatSeconds == null) {
            if (!bJwtIatRequired)
                return; // IAT is optional
            // If JWT has specified the maxJwtLifetimeMinutesAllowed attribute 
            // then the jwt token has to provided iat claim 
            // This is not in the spec.
            Tr.error(tc, "JWT_TOKEN_IAT_NEEDED_ERR");
            // no exp claim in the jwt token
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_IAT_NEEDED_ERR"), null);
        }
        // get max jwt lifetime minutes
        long lMaxJwtLifetimeSecondAllowed = _jwtGrantTypeHandlerConfig.getJwtTokenMaxLifetime();

        long lIat = iatSeconds.longValue() * 1000; // MilliSecond
        Date iatDate = new Date(lIat);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT token iat : " + iatDate);
        }

        // If iat is specified, we will check it against the MaxJwtLifetimeMinutesAllowed
        // The default values on the MaxJwtLifetimeMinutesAllowed is 
        Date earlierDate = new Date(currentDate.getTime() - (lMaxJwtLifetimeSecondAllowed * 1000));
        if (iatDate.before(earlierDate)) { // the issue-at-time should not be earilier than (currenttime minus MaxLifetime)
            Tr.error(tc, "JWT_TOKEN_MAX_LIFETIME_ERR", iatDate.toString(), lMaxJwtLifetimeSecondAllowed);
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_MAX_LIFETIME_ERR", iatDate.toString(), lMaxJwtLifetimeSecondAllowed),
                            (Throwable) null);
        }

        // Let's check if the token is issued in a future time(iat)
        // If so, let's fail it
        Date currentSkewDate = new Date(currentDate.getTime() + (lSkewSeconds * 1000));
        if (iatDate.after(currentSkewDate)) {// iat at future time
            Tr.error(tc, "JWT_TOKEN_IAT_FUTURE_ERR", iatDate.toString(), currentSkewDate.toString());
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_FUTURE_TOKEN_ERR", iatDate.toString()),
                            (Throwable) null);
        }
    }

    /**
     * @param exp
     * @param currentDate
     * @param lSkew
     */
    protected void verifyExpiredTime(Long expSeconds, Date currentDate, long lSkew) throws OAuth20Exception {
        if (expSeconds == null) {
            Tr.error(tc, "JWT_TOKEN_MISSING_REQUIRED_CLAIM_ERR",
                     new Object[] { OIDCConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS });
            // no exp claim in the jwt token
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", OIDCConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS), null);
        }
        long lExp = expSeconds.longValue() * 1000; // MilliSecond
        Date expDate = new Date(lExp);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT token exp: " + expDate + " currentDate:" + currentDate);
        }
        // 

        Date allowDate = new Date(currentDate.getTime() - (lSkew * 1000));
        if (expDate.before(allowDate)) { // if expiration time is before current time... expired
            Tr.error(tc, "JWT_TOKEN_EXPIRE_ERR", expDate.toString(), allowDate.toString());
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_EXPIRED_ERR", expDate.toString()),
                            (Throwable) null);
        }

    }

    /**
     * @param user
     */
    protected void verifyJwtSub(String user) throws OAuth20Exception {
        if (user == null) { // required
            Tr.error(tc, "JWT_TOKEN_MISSING_REQUIRED_CLAIM_ERR",
                     new Object[] { OIDCConstants.PAYLOAD_SUBJECT });
            // no exp claim in the jwt token
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", OIDCConstants.PAYLOAD_SUBJECT), null);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT token sub : " + user);
        }
        // verify if the sub is in userRegistry
        // If it's not in the user registry, throw Exception
        if (!isInUserRegistry(user)) {
            Tr.error(tc, "JWT_TOKEN_SUB_NOT_FOUND_ERR", user);
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_SUB_NOT_FOUND_ERR", user), null);
        }
    }

    /**
     * @param iss
     * @param issuerIdentifier
     */
    protected void verifyJwtIssuer(String strIssuer, String[] redirectUris, String clientId) throws OAuth20Exception {
        if (strIssuer == null) {
            Tr.error(tc, "JWT_TOKEN_MISSING_REQUIRED_CLAIM_ERR",
                     new Object[] { OIDCConstants.PAYLOAD_ISSUER });
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", OIDCConstants.PAYLOAD_ISSUER), null);
        }
        String strRedirectUri = ""; // in case no
        boolean bOkIssuer = strIssuer.equals(clientId);
        if ((!bOkIssuer) && (redirectUris != null)) {
            for (String redirectUri : redirectUris) {
                strRedirectUri = redirectUri;
                if (bOkIssuer = strIssuer.equals(redirectUri)) {
                    break;
                }
            }
        }
        if (!bOkIssuer) {
            if (strRedirectUri == null)
                strRedirectUri = "NONE";
            Tr.error(tc, "JWT_TOKEN_INVALID_ISS_ERR", strIssuer, strRedirectUri, clientId);
            String msg = formatMessage("JWT_TOKEN_ISS_MISMATCH_ERR", strIssuer);
            throw new InvalidGrantException(msg, null);
        }
    }

    /**
     * @param iss
     * @param issuerIdentifier
     * @param strTokenEndpoint. This is a null in case the issuerIdentifier is defined at openidConnectProvider in configuration
     */
    protected void verifyJwtAudience(String[] audiences, String issuerIdentifier, String strTokenEndpoint) throws OAuth20Exception {
        if (audiences == null) {
            Tr.error(tc, "JWT_TOKEN_MISSING_REQUIRED_CLAIM_ERR",
                     new Object[] { OIDCConstants.PAYLOAD_AUDIENCE });
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", OIDCConstants.PAYLOAD_AUDIENCE), null);
        }

        boolean bOk = false;
        String errAudience = null;

        for (String strAudience : audiences) {
            if (strAudience.equalsIgnoreCase(issuerIdentifier) ||
                strAudience.equalsIgnoreCase(strTokenEndpoint)) { // strTokenEndpoint could be null but audience won't be null at this point 
                bOk = true;
                break;
            }
        }
        if (!bOk) {
            errAudience = convertToString(audiences);

            if (strTokenEndpoint != null) {
                Tr.error(tc, "JWT_TOKEN_INVALID_AUD_ERR", errAudience, issuerIdentifier, strTokenEndpoint);
            } else {
                // The issuerIdentifier is defined in the issuerIdentifier of openidConnectProvider in configuration
                Tr.error(tc, "JWT_TOKEN_INVALID_AUD_IDENTIFIER_ERR", errAudience, issuerIdentifier);
            }

            throw new InvalidGrantException(formatMessage("JWT_TOKEN_BAD_AUD_ERR"), null);
        }
    }

    protected boolean isInUserRegistry(String user) throws OAuth20Exception {
        boolean isValid = false;
        try {
            SecurityService securityService = _jwtGrantTypeHandlerConfig.getSecurityService();
            UserRegistryService userRegistryService = securityService.getUserRegistryService();
            UserRegistry userRegistry = userRegistryService.getUserRegistry();
            //String realm = userRegistry.getRealm();
            isValid = userRegistry.isValidUser(user);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "User " + user + " is valid " + isValid);
            }
        } catch (RegistryException e) {
            Tr.error(tc, "JWT_TOKEN_REGISTRY_EXCEPTION_ERR", user, e);
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_BAD_SUB_EXTERNAL_ERR", user), null);
        }
        return isValid;
    }

    public String getString(Object obj) {
        if (obj == null)
            return (String) null;
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof String[]) {
            String[] strings = (String[]) obj;
            if (strings.length > 0)
                return strings[0];
            else
                return (String) null;
        }
        return obj.toString();
    }

    // implements PluginGrantTypeHandler
    /**
     * set the configuration information into the new plugin Grant Type Handler Factory
     * 
     * @param pluginGrantTypeHandlerFactory
     */
    public void setHandlerInfo(String providerId, OAuth20Provider config) {
        _jwtGrantTypeHandlerConfig = new JwtGrantTypeHandlerConfig(providerId, config);
    }

    /**
     * get an OAuth20GrantTypeHandlerFactory instance
     * 
     * @return
     */
    public OAuth20GrantTypeHandler getHandlerInstance() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "GrantTypeCustomizedHandlerJwtImpl handler:" + this + " JwtGrantTypeHandlerConfig" + _jwtGrantTypeHandlerConfig);
        }
        return (OAuth20GrantTypeHandler) this;
    }

    protected Long getLong(JWTPayload payload, String strClaim) throws OAuthException {
        Object objLong = payload.get(strClaim);
        if (objLong == null || objLong instanceof Long) {
            return (Long) objLong;
        }
        if (objLong instanceof Integer) {
            Integer integer1 = (Integer) objLong;
            return Long.valueOf((long) integer1.intValue());
        }

        Long lResult = null;
        try {
            String strLong = null;
            if (objLong instanceof String[]) {
                strLong = ((String[]) objLong)[0];
            } else {
                strLong = (String) objLong;
            }
            lResult = Long.valueOf(strLong);
        } catch (Exception e) {
            Tr.error(tc, "JWT_TOKEN_BAD_NUMBER_ERR", new Object[] { strClaim, objLong });
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_BAD_NUMBER_ERR", strClaim, objLong), e);
        }
        return lResult;
    }

    protected OidcServerConfig getOidcServerConfig(AttributeList attributeList) throws OAuthException {
        OidcServerConfig oidcServerConfig = null;

        String requestType = attributeList.getAttributeValueByName(OAuth20Constants.REQUEST_FEATURE);
        if (OAuth20Constants.REQUEST_FEATURE_OAUTH2.equals(requestType)) {
            // oauth2 request should not get the oidcServerConfig
            return null;
        }

        try {
            String oauth20ProviderName = _jwtGrantTypeHandlerConfig.getProviderId();
            // TODO: an oidc request ought to have a proper oidc id in the request. This must have been checked, right?
            oidcServerConfig = OIDCProvidersConfig.getOidcServerConfigForOAuth20Provider(oauth20ProviderName);
        } catch (Exception e) {
            Tr.error(tc, "JWT_TOKEN_UNEXPECTED_EXCEPTION", "getOidcServerConfig()", e);
            throw new InvalidGrantException(formatMessage("JWT_UNEXPECTED_EXCEPTION_ERR", "getOidcServerConfig()", e.getMessage()), null);
        }
        return oidcServerConfig;
    }

    /**
     * @param clientId
     * @param client_secret
     * @param tokenString
     * @param lSkewSeconds
     * @return
     */
    // @FFDCIgnore({InvalidGrantException.class})
    public JWTVerifier getJwtVerifier(String clientId,
                                      String client_secret,
                                      String tokenString,
                                      long lSkewSeconds,
                                      OidcServerConfig oidcServerConfig)
                    throws OAuthException {
        JWTVerifier jwtVerifier = null;

        JWTVerifier tmpJwtVerifier = new JWTVerifier(tokenString);
        String signatureAlgorithm = tmpJwtVerifier.getAlgHeader();

        // default to HS256
        String jwtSignatureAlgorithm = OAuth20Constants.SIGNATURE_ALGORITHM_HS256;
        if (oidcServerConfig != null) {
            String alg = oidcServerConfig.getSignatureAlgorithm();
            if (OAuth20Constants.SIGNATURE_ALGORITHM_RS256.equals(alg)) {
                jwtSignatureAlgorithm = alg;
            } else {
                if (TraceComponent.isAnyTracingEnabled() &&
                    tc.isDebugEnabled() &&
                    OAuth20Constants.SIGNATURE_ALGORITHM_NONE.equals(alg)) {
                    Tr.debug(tc, "The algorithm in openidConnectProvider configuration is : " + alg +
                                 " reset it to default value:" + OAuth20Constants.SIGNATURE_ALGORITHM_HS256);
                }
            }
        } else {
            // oauth2 endpoint does not support RS256
            if (OAuth20Constants.SIGNATURE_ALGORITHM_RS256.equals(signatureAlgorithm)) {
                Tr.error(tc, "JWT_TOKEN_OAUTH_RS256_NOT_SUPPORTED_ERR");
                throw new InvalidGrantException(formatMessage("JWT_TOKEN_OAUTH_RS256_NOT_SUPPORTED_ERR"), null);
            }
        }
        // getting the jwtVerifier. 
        // signatureAlgorithm should not be "none" at this point. 
        if (jwtSignatureAlgorithm.equals(OAuth20Constants.SIGNATURE_ALGORITHM_HS256)) {
            jwtVerifier = new JWTVerifier(clientId, (Object) client_secret,
                            jwtSignatureAlgorithm, tokenString,
                            lSkewSeconds);
        } else if (jwtSignatureAlgorithm.equals(OAuth20Constants.SIGNATURE_ALGORITHM_RS256)) {
            // get the alias from the tokenstring first
            String alias = tmpJwtVerifier.getIssFromPayload();
            if (alias == null) {
                Tr.error(tc, "JWT_TOKEN_MISSING_REQUIRED_CLAIM_ERR",
                         new Object[] { OIDCConstants.PAYLOAD_ISSUER });
                throw new InvalidGrantException(formatMessage("JWT_TOKEN_MISS_REQUIRED_CLAIM_ERR", OIDCConstants.PAYLOAD_ISSUER), null);
            }

            // get the public key
            PublicKey publicKey = null;
            try {
                publicKey = oidcServerConfig.getPublicKey(alias);
            } catch (Exception e) {
                Tr.error(tc, "JWT_TOKEN_NO_PUBLIC_KEY_DETAIL_ERR",
                         e, jwtSignatureAlgorithm,
                         oidcServerConfig.getTrustStoreRef(), alias);
                throw new InvalidGrantException(GrantTypeCustomizedHandlerJwtImpl.formatMessage("JWT_TOKEN_NO_PUBLIC_KEY_ERR", jwtSignatureAlgorithm),
                                null);
            }
            if (publicKey != null) {
                jwtVerifier = new JWTVerifier(clientId, (Object) publicKey,
                                jwtSignatureAlgorithm, tokenString,
                                lSkewSeconds);
            }
        } // else{} ought to be limited by the metatype.xml 

        if (jwtVerifier == null) { // this should not happen
            Tr.error(tc, "JWT_UNEXPECTED_ERR", "can not get a JWTVerifier to verify the JWT Token. Signature algorithm is " + jwtSignatureAlgorithm);
        }
        return jwtVerifier;
    }

    protected String getTokenString(AttributeList attributeList) throws OAuthException {
        String tokenString = null;
        // get the jwt token
        String[] tokenStrings = attributeList.getAttributeValuesByName(OIDCConstants.ASSERTION);
        if (tokenStrings == null || tokenStrings.length < 1 || tokenStrings[0].isEmpty()) {
            Tr.error(tc, "JWT_TOKEN_NO_TOKEN_ERR");
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_NO_TOKEN_EXTERNAL_ERR"), null);
        } else if (tokenStrings.length > 1) {
            Tr.error(tc, "JWT_TOKEN_TOO_MANY_TOKENS_ERR");
            throw new InvalidGrantException(formatMessage("JWT_TOKEN_TOO_MANY_TOKENS_ERR"), null);
        } else {
            tokenString = tokenStrings[0];
        }
        return tokenString;
    }

    protected String[] getStrings(Object obj) {
        if (obj == null || obj instanceof String[]) {
            return (String[]) obj;
        }
        if (obj instanceof String) {
            return new String[] { (String) obj };
        }
        if (obj instanceof List) {
            List list = (List) obj;
            String[] result = new String[list.size()];
            int iCnt = 0;
            for (Object aObj : list) {
                result[iCnt++] = aObj.toString();
            }
            return result;
        }
        if (obj instanceof Object) {
            return new String[] { obj.toString() };
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Can not convert obj from " + obj.getClass().getName() + " to string[]");
        }
        return new String[] {};
    }

    static protected String formatMessage(String msgKey, Object... objs) {
        return Tr.formatMessage(tc, msgKey, objs);
    }

}
