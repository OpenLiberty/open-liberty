/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.LinkedinLoginConfigImpl;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.IntrospectUserApiUtils;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;
import com.ibm.ws.security.social.internal.utils.OpenShiftUserApiUtils;
import com.ibm.ws.security.social.internal.utils.SocialUtil;

public class TAIUserApiUtils {

    public static final TraceComponent tc = Tr.register(TAIUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    static SingleTableCache userApiCache = null;

    @FFDCIgnore(SocialLoginException.class)
    public String getUserApiResponse(OAuthClientUtil clientUtil, SocialLoginConfig clientConfig, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) {
        UserApiConfig[] userinfoCfg = clientConfig.getUserApis();
        if (userinfoCfg == null || userinfoCfg.length == 0) {
            Tr.warning(tc, "NO_USER_API_CONFIGS_PRESENT", new Object[] { clientConfig.getUniqueId() });
            return null;
        }
        UserApiConfig userApiConfig = userinfoCfg[0];
        String userinfoApi = userApiConfig.getApi();
        try {
            if (SocialUtil.isKubeConfig(clientConfig)) {
                return getUserApiResponseFromOpenShift(clientConfig, accessToken, sslSocketFactory);
            }

            if ("introspect".equals(clientConfig.getUserApiType())) {
                return getUserApiResponseFromIntrospectEndpoint((Oauth2LoginConfigImpl) clientConfig, accessToken, sslSocketFactory);
            }
            if (isTokenExpectedToBeServiceAccountToken(clientConfig)) {
                return getUserApiResponseForServiceAccountToken(clientConfig, accessToken, sslSocketFactory);
            }
            return getUserApiResponseFromGenericThirdParty(clientUtil, clientConfig, accessToken, sslSocketFactory, userinfoApi);
        } catch (SocialLoginException e) {
            Tr.error(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        } catch (Exception e) {
            Tr.error(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        }
    }

    private String getUserApiResponseFromIntrospectEndpoint(Oauth2LoginConfigImpl config, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        IntrospectUserApiUtils introspectUtils = new IntrospectUserApiUtils(config);
        return introspectUtils.getUserApiResponse(accessToken, sslSocketFactory);
    }

    private String getUserApiResponseFromOpenShift(SocialLoginConfig config, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        OpenShiftUserApiUtils openShiftUtils = new OpenShiftUserApiUtils(config);
        return openShiftUtils.getUserApiResponse(accessToken, sslSocketFactory);
    }

    private boolean isTokenExpectedToBeServiceAccountToken(SocialLoginConfig clientConfig) {
        return SocialUtil.isOkdConfig(clientConfig);
    }

    private String getUserApiResponseForServiceAccountToken(SocialLoginConfig config, @Sensitive String serviceAccountToken, SSLSocketFactory sslSocketFactory) throws SocialLoginException {
        String userApiResponse = getUserApiResponseFromCache(config, serviceAccountToken);
        if (userApiResponse == null) {
            OpenShiftUserApiUtils openShiftUtils = new OpenShiftUserApiUtils(config);
            userApiResponse = openShiftUtils.getUserApiResponseForServiceAccountToken(serviceAccountToken, sslSocketFactory);
            cacheUserApiResponse(serviceAccountToken + config.getUniqueId(), userApiResponse);
        }
        return userApiResponse;
    }

    private String getUserApiResponseFromCache(SocialLoginConfig config, @Sensitive String serviceAccountToken) {
        initializeCache(config);
        return (String) userApiCache.get(serviceAccountToken + config.getUniqueId());
    }

    private synchronized void initializeCache(SocialLoginConfig config) {
        long cacheTime = config.getApiResponseCacheTime();
        if (userApiCache == null) {
            userApiCache = new SingleTableCache(cacheTime);
        } else {
            if (cacheTime != userApiCache.getTimeoutInMilliseconds()) {
                userApiCache.rescheduleCleanup(cacheTime);
            }
        }
    }

    private void cacheUserApiResponse(@Sensitive String serviceAccountToken, String userApiResponse) {
        userApiCache.put(serviceAccountToken, userApiResponse);
    }

    private String getUserApiResponseFromGenericThirdParty(OAuthClientUtil clientUtil, SocialLoginConfig clientConfig, String accessToken, SSLSocketFactory sslSocketFactory, String userinfoApi) throws Exception {
        String userApiResp = clientUtil.getUserApiResponse(userinfoApi,
                accessToken,
                sslSocketFactory,
                false,
                clientConfig.getUserApiNeedsSpecialHeader(),
                clientConfig.getUseSystemPropertiesForHttpClientConnections());
        if (clientConfig instanceof LinkedinLoginConfigImpl) {
            return convertLinkedinToJson(userApiResp, clientConfig.getUserNameAttribute());
        }
        if (userApiResp != null && userApiResp.startsWith("[") && userApiResp.endsWith("]")) {
            return convertToJson(userApiResp, clientConfig.getUserNameAttribute());
        } else {
            return userApiResp;
        }
    }

    // flatten linkedin's json
    // in: {"elements":[{"handle~":{"emailAddress":"abcde@gmail.com"},"handle":"urn:li:emailAddress:688645328"}]}
    // out: {"emailAddress":"abcde@gmail.com"};
    private String convertLinkedinToJson(String resp, String usernameattr) {
        int end = 0;
        int begin = resp.indexOf(usernameattr) - 1;
        if (begin > 0) {
            end = resp.indexOf("}", begin);
            return resp.substring(begin - 1, end + 1);
        }
        return null;
    }

    private String convertToJson(String userApiResp, String usernameattr) {
        //String key = userinfoApi.substring(userinfoApi.lastIndexOf("/") + 1);
        StringBuffer sb = new StringBuffer();
        sb.append("{\"").append(usernameattr).append("\":").append(userApiResp).append("}");
        return sb.toString();
    }

}
