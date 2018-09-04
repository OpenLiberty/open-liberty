/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.openidconnect.clients.common;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

    /**
     * get userinfo from provider's UserInfo Endpoint if configured and active.
     * If successful, update Subject or Properties in the ProviderAuthenticationResult
     *
     */
    public void getUserInfo(ProviderAuthenticationResult oidcResult, ConvergedClientConfig clientConfig,
            SSLSocketFactory sslSocketFactory, String accessToken) {

        if (clientConfig.getUserInfoEndpointUrl() == null || clientConfig.isUserInfoEnabled() == false || accessToken == null) {
            return;
        }

        String userInfoStr = getUserInfoFromURL(clientConfig, sslSocketFactory, accessToken);

        if (userInfoStr == null) {
            return;
        }

        if (clientConfig.isSocial()) {
            // social will finish building the subject
            updateAuthenticationResultPropertiesWithUserInfo(oidcResult, userInfoStr);
        } else {
            updateAuthenticationResultSubjectWithUserInfo(oidcResult, userInfoStr);
        }
    }

    protected void updateAuthenticationResultPropertiesWithUserInfo(ProviderAuthenticationResult oidcResult, String userInfoStr) {
        oidcResult.getCustomProperties().put(Constants.USERINFO_STR, userInfoStr);
    }

    protected void updateAuthenticationResultSubjectWithUserInfo(ProviderAuthenticationResult oidcResult, String userInfoStr) {
        oidcResult.getSubject().getPrivateCredentials().add(userInfoStr);
    }

    // per oidc-connect-core-1.0 sec 5.3.2, sub claim of userinfo response must match sub claim in id token,
    // i.e. the subject we authenticated.
    // At the time userinfo is retreived, subject isn't built, so this gets called later.
    public boolean isUserInfoValid(String userInfoStr, String subClaim) {
        String userInfoSubClaim = getUserInfoSubClaim(userInfoStr);
        if (userInfoSubClaim == null || userInfoSubClaim.compareTo(subClaim) != 0) {
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
     * @return
     */
    protected String getUserInfoFromURL(ConvergedClientConfig config, SSLSocketFactory sslsf, String accessToken) {
        String url = config.getUserInfoEndpointUrl();
        boolean hostnameVerification = config.isHostNameVerificationEnabled();

        // https required by spec, use of http is not spec compliant
        if (!url.toLowerCase().startsWith("https:") && config.isHttpsRequired()) {
            Tr.error(tc, "OIDC_CLIENT_URL_PROTOCOL_NOT_HTTPS", new Object[] { url });
            return null;
        }

        HttpClient hc = (OidcClientHttpUtil.getInstance()).createHTTPClient(sslsf, url, hostnameVerification);
        HttpGet getMethod = new HttpGet(url);
        if (accessToken != null) {
            getMethod.setHeader(ClientConstants.AUTHORIZATION, ClientConstants.BEARER + accessToken);
        }
        HttpResponse response = null;
        int statusCode = 0;
        String responseStr = null;
        try {
            response = hc.execute(getMethod);
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
