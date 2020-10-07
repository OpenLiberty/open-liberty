/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.jose4j;

import java.util.Map;
import java.util.Set;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.claims.UserClaims;
import com.ibm.ws.security.common.claims.UserClaimsRetrieverService;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.util.ConfigUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

/**
 *
 */
public class JwtCreator {
    private static TraceComponent tc = Tr.register(JwtCreator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static final String CFG_KEY_ISSUER_IDENTIFIER = "issuerIdentifier";

    private static final String JTI_CLAIM = "jti"; // idtoken
    public static final String AT_HASH = "at_hash"; // idtoken
    public static final String AZP = "azp"; // 227327
    private static final String UPN_CLAIM = "upn"; // mpJWT token
    private static final String GROUP_CLAIM = "groups";

    /**
     * create String representation of ID Token, or a JWT token, or the MpJWT flavor of JWT token.
     * The sequence in which claims are added or overwritten has turned out to be critical,
     * so MpJWT was added in such a way to preserve the sequence of events as much as possible.
     * @param oidcServerConfig
     * @param clientId
     * @param username
     * @param scope
     * @param lifetimeSeconds
     * @param tokenMap
     * @param userClaims
     * @param jwtData - it's isJwt method will be true if JWT or mpJWT token requested.
     * @param mpJwt - if making JWT token string, use mpJWT token format
     * @return
     */
    @FFDCIgnore({ Exception.class })
    public static String createJwtAsString(OidcServerConfig oidcServerConfig,
            String clientId,
            String username,
            String[] scope,
            int lifetimeSeconds,
            Map<String, String[]> tokenMap,
            Map<String, Object> userClaims,
            JWTData jwtData,
            boolean mpJwt) {

        boolean bJwt = jwtData.isJwt(); // this token is not an id token.
        String jwt = null;
        try {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PI78760, PI80166 are present, OidcServerConfig is: " + oidcServerConfig);
            }
            String[] audiences = null;

            // Create the Claims, which will be the content of the JWT
            JwtClaims claims = new JwtClaims();
            // PI78760 - Since the mediatorSpi might have changed it,
            // setSubject here so it can be overridden
            claims.setSubject(username); // the subject/principal is whom the token is about

            if (bJwt || mpJwt) { // for JWT or MpJWT token used by RS
                audiences = tokenMap.get(OAuth20Constants.RESOURCE); //
                claims.setClaim("token_type", "Bearer"); // JWT only
                if (scope != null && scope.length > 0) { // JWT only
                    claims.setStringListClaim("scope", scope);
                }
                if (!mpJwt) {
                    claims.setClaim(AZP, clientId);// 227327, authorizingParty
                }

            } else { // this is for an ID token used by RP
                // handle nonce (Required claim first)
                String nonce = OAuth20Util.getValueFromMap(OIDCConstants.OIDC_AUTHZ_PARAM_NONCE, tokenMap);
                if (nonce != null && !nonce.isEmpty()) { // for IDToken but not for JWT
                    claims.setClaim("nonce", nonce);
                } // ID Token the audience has to be the cientId only
                audiences = new String[] { clientId };
                // PI80166 moved to later: addExternalClaims(claims, tokenMap);
                if (userClaims != null) {
                    for (Map.Entry<String, Object> e : userClaims.entrySet())
                        claims.setClaim(e.getKey(), e.getValue());
                }
            }
            if (oidcServerConfig.isJTIClaimEnabled() || mpJwt) { // addOptionalClaims for IDToken and jwt too 224198
                claims.setClaim(JTI_CLAIM, OAuthUtil.getRandom(16));
            }

            claims.setIssuer(getIssuerIdentifier(tokenMap, oidcServerConfig)); // who creates the token and signs it
            if (audiences != null && audiences.length > 0)
                claims.setAudience(audiences); // to whom the token is intended to be sent
            long timeInMilliSeconds = System.currentTimeMillis();

            NumericDate ndIat = NumericDate.fromMilliseconds(timeInMilliSeconds);
            NumericDate ndExp = NumericDate.fromMilliseconds(timeInMilliSeconds + (lifetimeSeconds * 1000L));
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting iat to [" + ndIat.toString() + "]");
                Tr.debug(tc, "Setting exp to [" + ndExp.toString() + "]");
            }
            claims.setExpirationTime(ndExp);
            claims.setIssuedAt(ndIat);
            // PI78760 moved to earlier: claims.setSubject(username);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "intermediate: audiences claims", audiences, claims);
            }

            if (bJwt || mpJwt) { // JWT or MpJWT token
                addFunctionalUserAndGroupIDs(claims, tokenMap, oidcServerConfig);
                addCustomClaims(claims, tokenMap, oidcServerConfig); // groups gets set here.
            } else { // Id token, pi80166
                addExternalClaims(claims, tokenMap); // mostly odd custom things
            }

            if (mpJwt) {
                // if mpJwt flavor of JWT, store any groups under the specified key name, "groups"
                String groupIdKey = oidcServerConfig.getGroupIdentifier();
                Object groups = claims.getClaimValue(groupIdKey);
                if (groups != null) {
                    claims.unsetClaim(groupIdKey);
                    claims.setClaim(GROUP_CLAIM, groups);
                }
                // translate uniqueSecurityName to upn, remove realmName if present
                Object user = claims.getClaimValue(UserClaims.USER_CLAIMS_UNIQUE_SECURITY_NAME);
                if (user != null) {
                    claims.setClaim(UPN_CLAIM, user);
                    claims.unsetClaim(UserClaims.USER_CLAIMS_UNIQUE_SECURITY_NAME);
                }
                claims.unsetClaim(UserClaims.USER_CLAIMS_REALM_NAME);
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "final: audiences claims", audiences, claims);
            }

            // A JWT is a JWS and/or a JWE with JSON claims as the payload.
            // In this example it is a JWS so we create a JsonWebSignature object.
            jwt = JwsSigner.getSignedJwt(claims, oidcServerConfig, jwtData);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception", e);
            }
            Object[] objs = new Object[] { oidcServerConfig.getProviderId(), e.getLocalizedMessage() };
            Tr.error(tc, "JWT_CANNOT_GENERATE_JWT", objs);
            throw new RuntimeException(Tr.formatMessage(tc, "JWT_CANNOT_GENERATE_JWT", objs));
        }
        return jwt;
    }

    private static String getIssuerIdentifier(@Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        String issuerIdentifier = oidcServerConfig.getIssuerIdentifier();
        if (issuerIdentifier == null || issuerIdentifier.isEmpty() || issuerIdentifier.equalsIgnoreCase("null")) {
            issuerIdentifier = OAuth20Util.getValueFromMap(CFG_KEY_ISSUER_IDENTIFIER, tokenMap);
        }
        return issuerIdentifier;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addCustomClaims(JwtClaims claims, @Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        if (oidcServerConfig.isCustomClaimsEnabled()) {
            UserClaimsRetrieverService userClaimsRetrieverService = ConfigUtils.getUserClaimsRetrieverService();
            if (userClaimsRetrieverService != null) {
                String username = OAuth20Util.getValueFromMap(OAuth20Constants.USERNAME, tokenMap);
                String groupIdentifier = oidcServerConfig.getGroupIdentifier();
                UserClaims oauthUserClaims = userClaimsRetrieverService.getUserClaims(username, groupIdentifier);
                if (oauthUserClaims != null) { // userName != null
                    Map<String, Object> customMap = null;
                    if (oauthUserClaims.isEnabled()) {
                        OidcUserClaims oidcUserClaims = new OidcUserClaims(oauthUserClaims);
                        oidcUserClaims.addExtraClaims(oidcServerConfig);
                        customMap = oidcUserClaims.asMap();
                    } else {
                        customMap = oauthUserClaims.asMap();
                    }
                    Set<Map.Entry<String, Object>> entries = customMap.entrySet();
                    for (Map.Entry<String, Object> entry : entries) {
                        claims.setClaim(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    private static void addFunctionalUserAndGroupIDs(JwtClaims claims, @Sensitive Map<String, String[]> tokenMap, OidcServerConfig oidcServerConfig) {
        String[] funcUsers = tokenMap.get(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USERID);
        String funcUser = null;
        if (funcUsers != null && funcUsers.length > 0) {
            funcUser = funcUsers[0];
            claims.setClaim(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USERID, funcUser);
        }
        String[] funcGroups = tokenMap.get(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USER_GROUPIDS);
        if (funcGroups != null && funcGroups.length > 0) {
            claims.setClaim(Constants.INTROSPECT_CLAIM_FUNCTIONAL_USER_GROUPIDS, funcGroups);
        }
    }

    /**
     * @param payload
     * @param tokenMap
     */
    private static void addExternalClaims(JwtClaims claims, Map<String, String[]> tokenMap) {

        Set<Map.Entry<String, String[]>> entries = tokenMap.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            if (key.startsWith(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX)) {
                String shortKey = key.substring(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX_LENGTH);
                // payload.put(shortKey, OAuth20Util.getValueFromMap(key, tokenMap));
                String[] values = tokenMap.get(key);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " longKey:" + key +
                            " shortKey:" + shortKey +
                            " values length:" + (values == null ? 0 : values.length));
                }
                if (values != null && values.length > 0) {
                    if (values.length == 1) {
                        claims.setClaim(shortKey, values[0]);
                    } else {
                        claims.setClaim(shortKey, values);
                    }
                }
            }
        }
    }

    public static String createJwtAsStringForSpi(String jsonFromSpi,
            OidcServerConfig oidcServerConfig,
            String clientId,
            String username,
            String[] scope,
            int lifetimeSeconds,
            Map<String, String[]> tokenMap,
            String grantType,
            String accessTokenHash,
            JWTData jwtData) {
        String jwt = null;
        try {
            JwtClaims claims = JwtClaims.parse(jsonFromSpi);
            // Now override and preserve System required claims
            if (claims.getIssuer() == null) {
                claims.setIssuer(getIssuerIdentifier(tokenMap, oidcServerConfig));
            }
            if (claims.getSubject() == null) {
                claims.setSubject(username);
            }
            if (claims.getExpirationTime() == null) {
                claims.setExpirationTimeMinutesInTheFuture(((float) lifetimeSeconds) / 60); // time when the token will expire (10 minutes from now)
            }
            // claim specific to id token
            if (accessTokenHash != null) {
                claims.setClaim(AT_HASH, accessTokenHash);
            }

            // String sharedKey = OAuth20Util.getValueFromMap(OAuth20Constants.CLIENT_SECRET, tokenMap);
            // JWTData jwtData = new JWTData(sharedKey, oidcServerConfig, JWTData.TYPE_ID_TOKEN);
            jwt = JwsSigner.getSignedJwt(claims, oidcServerConfig, jwtData);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception", e);
            }
            Object[] objs = new Object[] { oidcServerConfig.getProviderId(), e.getLocalizedMessage() };
            Tr.error(tc, "JWT_CANNOT_GENERATE_JWT", objs);
            throw new RuntimeException(Tr.formatMessage(tc, "JWT_CANNOT_GENERATE_JWT", objs));
        }

        return jwt;
    }

}
