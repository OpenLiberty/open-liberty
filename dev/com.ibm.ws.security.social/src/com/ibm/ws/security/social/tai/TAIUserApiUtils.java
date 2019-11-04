/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.io.IOException;

import javax.net.ssl.SSLSocketFactory;

import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.LinkedinLoginConfigImpl;
import com.ibm.ws.security.social.internal.OpenShiftLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;
import com.ibm.ws.security.social.internal.utils.OpenShiftUserApiUtils;

public class TAIUserApiUtils {

    public static final TraceComponent tc = Tr.register(TAIUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

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
            if (clientConfig instanceof OpenShiftLoginConfigImpl) {
                //return convertLinkedinToJson(getUserApiResponseFromOpenShift((OpenShiftLoginConfigImpl) clientConfig, accessToken, sslSocketFactory), clientConfig.getUserNameAttribute());
                return getUserApiResponseFromOpenShift((OpenShiftLoginConfigImpl) clientConfig, accessToken, sslSocketFactory);
            }
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
        } catch (SocialLoginException e) {
            Tr.warning(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        } catch (Exception e) {
            Tr.warning(tc, "ERROR_GETTING_USER_API_RESPONSE", new Object[] { userinfoApi, clientConfig.getUniqueId(), e.getLocalizedMessage() });
            return null;
        }
    }

    private String getUserApiResponseFromOpenShift(OpenShiftLoginConfigImpl config, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) throws IOException, JoseException{
        OpenShiftUserApiUtils openShiftUtils = new OpenShiftUserApiUtils(config);
        return openShiftUtils.getUserApiResponse(accessToken, sslSocketFactory);
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
