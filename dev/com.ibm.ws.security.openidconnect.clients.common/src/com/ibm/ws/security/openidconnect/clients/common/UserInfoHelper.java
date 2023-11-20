/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.JweHelper;
import com.ibm.ws.security.openidconnect.client.jose4j.util.Jose4jUtil;
import com.ibm.ws.security.openidconnect.client.jose4j.util.OidcTokenImplBase;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.common.jwt.JwtParsingUtils;

/**
 * Utility methods to retrieve UserInfo data, validate it, and update the Subject with it.
 */
public class UserInfoHelper {
    private static final TraceComponent tc = Tr.register(UserInfoHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private ConvergedClientConfig clientConfig = null;
    private Jose4jUtil jose4jUtil = null;

    public UserInfoHelper(ConvergedClientConfig config, SSLSupport sslSupport) {
        this.clientConfig = config;
        this.jose4jUtil = new Jose4jUtil(sslSupport);
    }

    public boolean willRetrieveUserInfo() {
        return (clientConfig.getUserInfoEndpointUrl() != null && clientConfig.isUserInfoEnabled() == true);
    }

    /**
     * get userinfo from provider's UserInfo Endpoint if configured and active.
     * If successful, update properties in the ProviderAuthenticationResult
     *
     * @return true if PAR was updated with userInfo
     *
     */
    public boolean getUserInfoIfPossible(ProviderAuthenticationResult oidcResult, Map<String, String> tokens, SSLSocketFactory sslsf, OidcClientRequest oidcClientRequest) {
        if (!willRetrieveUserInfo()) {
            return false;
        }
        OidcTokenImplBase idToken = null;
        if (oidcResult.getCustomProperties() != null) {
            idToken = (OidcTokenImplBase) oidcResult.getCustomProperties().get(Constants.ID_TOKEN_OBJECT);
        }
        String subjFromIdToken = null;
        if (idToken != null) {
            subjFromIdToken = idToken.getSubject();
        }
        if (subjFromIdToken != null) {
            return getUserInfoIfPossible(oidcResult, tokens.get(Constants.ACCESS_TOKEN), subjFromIdToken, sslsf, oidcClientRequest);
        }
        return false;
    }

    /**
     * get userinfo from provider's UserInfo Endpoint if configured and active.
     * If successful, update properties in the ProviderAuthenticationResult
     *
     * @return true if PAR was updated with userInfo
     *
     */
    public boolean getUserInfoIfPossible(ProviderAuthenticationResult oidcResult, String accessToken, String subject, SSLSocketFactory sslsf, OidcClientRequest oidcClientRequest) {
        if (!willRetrieveUserInfo()) {
            return false;
        }
        if (subject != null && accessToken != null) {
            return getUserInfo(oidcResult, sslsf, accessToken, subject, oidcClientRequest);
        }
        return false;
    }

    /**
     * get userinfo from provider's UserInfo Endpoint if configured and active.
     * If successful, update properties in the ProviderAuthenticationResult
     *
     * @return true if PAR was updated with userInfo
     *
     */
    public boolean getUserInfo(ProviderAuthenticationResult oidcResult,
            SSLSocketFactory sslSocketFactory, String accessToken, String subjectFromIdToken, OidcClientRequest oidcClientRequest) {

        if (!willRetrieveUserInfo() || accessToken == null) {
            return false;
        }
        String userInfoStr = getUserInfoFromURL(clientConfig, sslSocketFactory, accessToken, oidcClientRequest);

        if (userInfoStr == null) {
            return false;
        }

        if (!isUserInfoValid(userInfoStr, subjectFromIdToken)) {
            return false;
        }

        updateAuthenticationResultPropertiesWithUserInfo(oidcResult, userInfoStr);
        return true;
    }

    protected void updateAuthenticationResultPropertiesWithUserInfo(ProviderAuthenticationResult oidcResult, String userInfoStr) {
        oidcResult.getCustomProperties().put(Constants.USERINFO_STR, userInfoStr);
    }

    // per oidc-connect-core-1.0 sec 5.3.2, sub claim of userinfo response must match sub claim in id token.
    public boolean isUserInfoValid(String userInfoStr, String subClaim) {
        String userInfoSubClaim = getUserInfoSubClaim(userInfoStr);
        if (userInfoSubClaim == null || subClaim == null || userInfoSubClaim.compareTo(subClaim) != 0) {
            Tr.error(tc, "USERINFO_INVALID", new Object[] { userInfoStr, subClaim });
            return false;
        }
        return true;
    }

    protected String getUserInfoSubClaim(String userInfo) {
        JSONObject jobj = null;
        try {
            jobj = JSONObject.parse(userInfo);
        } catch (Exception e) { // ffdc
            Tr.error(tc, "USERINFO_CLAIMS_FORMAT_NOT_VALID", new Object[] { userInfo, e.getMessage() });
        }
        return jobj == null ? null : (String) jobj.get("sub");
    }

    /**
     * Obtain userInfo from an OIDC provider
     *
     * @return the userInfo string, or null
     */
    protected String getUserInfoFromURL(ConvergedClientConfig config, SSLSocketFactory sslsf, String accessToken, OidcClientRequest oidcClientRequest) {
        String url = config.getUserInfoEndpointUrl();
        boolean hostnameVerification = config.isHostNameVerificationEnabled();

        // https required by spec, use of http is not spec compliant
        if (!url.toLowerCase().startsWith("https:") && config.isHttpsRequired()) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", new Object[] { url });
            return null;
        }

        OidcClientUtil oidccu = new OidcClientUtil();
        int statusCode = 0;
        String responseStr = null;
        try {
            boolean useSysProps = config.getUseSystemPropertiesForHttpClientConnections();
            Map<String, Object> resultMap = oidccu.getUserinfo(url, accessToken, sslsf, hostnameVerification, useSysProps);
            if (resultMap == null) {
                throw new Exception("result map from getUserinfo is null");
            }
            HttpResponse response = (HttpResponse) resultMap.get(ClientConstants.RESPONSEMAP_CODE);
            if (response == null) {
                throw new Exception("HttpResponse from getUserinfo is null");
            }
            statusCode = response.getStatusLine().getStatusCode();
            responseStr = extractClaimsFromResponse(response, config.getOidcClientConfig(), oidcClientRequest);
        } catch (Exception ex) {
            Tr.error(tc, "ERROR_GETTING_USERINFO_OR_EXTRACTING_CLAIMS", new Object[] { config.getId(), ex.getMessage() });
        }
        if (statusCode != 200) {
            Tr.error(tc, "USERINFO_RETREIVE_FAILED", new Object[] { url, Integer.toString(statusCode), responseStr });
            return null;
        }
        return responseStr;
    }

    String extractClaimsFromResponse(HttpResponse response, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws Exception {
        HttpEntity entity = response.getEntity();
        String jresponse = null;
        if (entity != null) {
            jresponse = EntityUtils.toString(entity);
        }
        if (jresponse == null || jresponse.isEmpty()) {
            return null;
        }
        String contentType = getContentType(entity);
        if (contentType == null) {
            return null;
        }
        String claimsStr = null;
        if (contentType.contains("application/json")) {
            claimsStr = jresponse;
        } else if (contentType.contains("application/jwt")) {
            claimsStr = extractClaimsFromJwtResponse(jresponse, clientConfig, oidcClientRequest);
        }
        return claimsStr;
    }

    String getContentType(HttpEntity entity) {
        Header contentTypeHeader = entity.getContentType();
        if (contentTypeHeader != null) {
            return contentTypeHeader.getValue();
        }
        return null;
    }

    @FFDCIgnore({ Exception.class })
    public String extractClaimsFromJwtResponse(String responseString, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws Exception {
        if (responseString == null || responseString.isEmpty()) {
            return null;
        }
        boolean isJwe = false;
        try {
            if (JweHelper.isJwe(responseString)) {
                responseString = JweHelper.extractPayloadFromJweToken(responseString, clientConfig, null);
                isJwe = true;
            }
            if (JweHelper.isJws(responseString)) {
                return extractClaimsFromJwsResponse(responseString, clientConfig, oidcClientRequest);
            } else if (isJwe) {
                // JWE payloads can be either JWS or JSON, so allow falling back to returning JSON in the case of a JWE response
                return responseString;
            } else {
                // We expect to be extracting claims from a JWT, but the response string isn't a JWS or a JWE
                String msg = Tr.formatMessage(tc, "JWT_RESPONSE_STRING_NOT_IN_JWT_FORMAT", new Object[] { responseString });
                throw new UserInfoException(msg);
            }
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "OIDC_CLIENT_ERROR_EXTRACTING_JWT_CLAIMS_FROM_WEB_RESPONSE", new Object[] { clientConfig.getId(), e.getMessage() });
            throw new UserInfoException(msg, e);
        }
    }

    String extractClaimsFromJwsResponse(String responseString, OidcClientConfig clientConfig, OidcClientRequest oidcClientRequest) throws Exception {
        JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(responseString);
        if (jwtContext != null) {
            // Validate the JWS signature only; extract the claims so they can be verified elsewhere
            JwtClaims claims = jose4jUtil.validateJwsSignature(jwtContext, clientConfig, oidcClientRequest);
            if (claims != null) {
                return claims.toJson();
            }
        }
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * get userinfo from provider's UserInfo Endpoint if configured and active.
     *
     * @return the user info
     *
     */
    public String getUserInfoIfPossible(String sub, String accessToken, SSLSocketFactory sslsf, OidcClientRequest oidcClientRequest) {
        if (!willRetrieveUserInfo() || accessToken == null) {
            return null;
        }

        if (sub != null) {
            String userInfoStr = getUserInfoFromURL(clientConfig, sslsf, accessToken, oidcClientRequest);
            if (userInfoStr != null) {
                if (isUserInfoValid(userInfoStr, sub))
                    return userInfoStr;
            }
        }
        return null;
    }
}
