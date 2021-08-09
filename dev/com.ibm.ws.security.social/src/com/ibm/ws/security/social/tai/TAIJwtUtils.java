/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;

public class TAIJwtUtils {

    public static final TraceComponent tc = Tr.register(TAIJwtUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    protected TAIJwtUtils() {
    }

    @FFDCIgnore(SocialLoginException.class)
    public JwtToken createJwtToken(OAuthClientUtil clientUtil, @Sensitive String idToken, SocialLoginConfig clientConfig, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        JwtToken jwtToken = null;
        if (idToken == null) {
            UserApiConfig[] userinfoCfg = clientConfig.getUserApis();
            if (userinfoCfg == null || userinfoCfg.length == 0) {
                throw new SocialLoginException("NO_USER_API_CONFIGS_PRESENT", null, new Object[] { clientConfig.getUniqueId() });
            }
            String userinfoApi = userinfoCfg[0].getApi();
            // TODO: Call as many user info APIs as configured
            try {
                jwtToken = clientUtil.getUserApiAsJwtToken(userinfoApi, accessToken, sslSocketFactory, false, clientConfig);
            } catch (SocialLoginException e) {
                throw new SocialLoginException("FAILED_TO_CREATE_JWT_FROM_USER_API", e, new Object[] { userinfoApi, e.getMessage() });
            } catch (Exception e) {
                throw new SocialLoginException("FAILED_TO_CREATE_JWT_FROM_USER_API", e, new Object[] { userinfoApi, e.getMessage() });
            }
        } else {
            jwtToken = createJwtTokenFromIdToken(idToken, clientConfig.getUniqueId());
        }
        return jwtToken;
    }

    public JwtToken createJwtTokenFromIdToken(@Sensitive String idToken, String jwtConfigId) throws SocialLoginException {
        // TODO this is for googleLogin now. May need to extends to other social configuration
        // Using the social googleLogin as the jwt consumer config to handle the ID Token
        try {
            return JwtConsumer.create(jwtConfigId).createJwt(idToken);
        } catch (Exception e) {
            throw new SocialLoginException("FAILED_TO_CREATE_JWT_FROM_ID_TOKEN", e, new Object[] { jwtConfigId, e.getMessage() });
        }
    }

    public JwtToken createJwtTokenFromJson(String userApiResponse, SocialLoginConfig clientConfig, boolean isOidc) throws Exception {
        Map<String, Object> claims = handleJwtClaims(userApiResponse, clientConfig, isOidc);
        JwtBuilder jwtBuilder = JwtBuilder.create(clientConfig.getJwtRef()).claim(claims);
        return jwtBuilder.buildJwt();
    }

    public Map<String, Object> handleJwtClaims(String jsonoridtoken, SocialLoginConfig clientConfig, boolean isOidc) throws Exception {
        Map claimsMap = null;
        if (isOidc) {
            //id token - base64 encoded
            String payload = JsonUtils.getPayload(jsonoridtoken);
            if (payload != null) {
                String decodedPayload = JsonUtils.decodeFromBase64String(payload);
                if (decodedPayload != null) {
                    claimsMap = JsonUtils.claimsFromJsonObject(decodedPayload);
                }
            }
        } else {
            //oauth user api response
            claimsMap = JsonUtils.claimsFromJsonObject(jsonoridtoken);
        }

        return handleJwtClaimsMap(claimsMap, clientConfig, isOidc);

    }

    /**
     * @param claimsMap
     * @param clientConfig
     * @return
     */
    protected Map<String, Object> handleJwtClaimsMap(Map claimsMap, SocialLoginConfig clientConfig, boolean isOidc) {

        Map<String, Object> claimsToCopyMap = new HashMap<String, Object>();

        if (claimsMap != null && !claimsMap.isEmpty()) {
            String[] claimsToCopy = clientConfig.getJwtClaims();
            //claimsToCopyMap.put(ClientConstants.SUB, idToken.getSubject()); //always copy this
            if (claimsToCopy != null && claimsToCopy.length > 0) {
                for (String claim : claimsToCopy) {
                    Object v;
                    if ((v = claimsMap.get(claim)) != null) {
                        claimsToCopyMap.put(claim, v);
                    }
                }
            } else {
                if (isOidc) {
                    String subClaim = clientConfig.getUserNameAttribute();
                    Object subClaimValue = claimsMap.get(subClaim);
                    if (subClaim != null && subClaimValue != null) {
                        claimsToCopyMap.put(subClaim, subClaimValue);
                    }
                } else {
                    // usually we should not have "iss" claim in the userapi response. But our own OP is including it. So we need to handle.
                    claimsMap = handleIssuerClaim(claimsMap);
                    claimsToCopyMap.putAll(claimsMap);
                }
            }
        }
        return claimsToCopyMap;
    }

    /**
     * @param claimsMap
     * @return
     */
    private Map handleIssuerClaim(Map claimsMap) {
        if (claimsMap != null && !claimsMap.isEmpty() && claimsMap.containsKey("iss")) {
            claimsMap.remove("iss");
        }
        return claimsMap;
    }

}
