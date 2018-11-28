/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

/**
 * Utility methods to retrieve UserInfo data, validate it, and update the Subject with it.
 */
public class UserInfoHelper {
    private static final TraceComponent tc = Tr.register(UserInfoHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private ConvergedClientConfig clientConfig = null;

    public UserInfoHelper(ConvergedClientConfig config) {
        this.clientConfig = config;
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
    public boolean getUserInfo(ProviderAuthenticationResult oidcResult,
            SSLSocketFactory sslSocketFactory, String accessToken, String subjectFromIdToken) {

        if (!willRetrieveUserInfo() || accessToken == null) {
            return false;
        }
        String userInfoStr = getUserInfoFromURL(clientConfig, sslSocketFactory, accessToken);

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
    protected boolean isUserInfoValid(String userInfoStr, String subClaim) {
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
        }
        return jobj == null ? null : (String) jobj.get("sub");
    }

    /**
     * Obtain userInfo from an OIDC provider
     *
     * @return the userInfo string, or null
     */
    protected String getUserInfoFromURL(ConvergedClientConfig config, SSLSocketFactory sslsf, String accessToken) {
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
            responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception ex) {
            //ffdc
        }
        if (statusCode != 200) {
            Tr.error(tc, "USERINFO_RETREIVE_FAILED", new Object[] { url, Integer.toString(statusCode), responseStr });
            return null;
        }
        return responseStr;
    }
}
