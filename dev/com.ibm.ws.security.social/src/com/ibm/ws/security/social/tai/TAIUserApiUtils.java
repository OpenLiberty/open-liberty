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

import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;

public class TAIUserApiUtils {

    public static final TraceComponent tc = Tr.register(TAIUserApiUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @FFDCIgnore(SocialLoginException.class)
    public static String getUserApiResponse(OAuthClientUtil clientUtil, SocialLoginConfig clientConfig, @Sensitive String accessToken, SSLSocketFactory sslSocketFactory) {
        UserApiConfig[] userinfoCfg = clientConfig.getUserApis();
        if (userinfoCfg == null || userinfoCfg.length == 0) {
            Tr.warning(tc, "NO_USER_API_CONFIGS_PRESENT", new Object[] { clientConfig.getUniqueId() });
            return null;
        }
        UserApiConfig userApiConfig = userinfoCfg[0];
        String userinfoApi = userApiConfig.getApi();
        try {
            String userApiResp = clientUtil.getUserApiResponse(userinfoApi,
                    accessToken, 
                    sslSocketFactory,
                    false,
                    clientConfig.getUserApiNeedsSpecialHeader(),
                    clientConfig.getUseSystemPropertiesForHttpClientConnections());
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

    /**
     * @param userApiResp
     * @param userinfoApi
     * @param usernameattr
     */
    public static String convertToJson(String userApiResp, String usernameattr) {
        //String key = userinfoApi.substring(userinfoApi.lastIndexOf("/") + 1);
        StringBuffer sb = new StringBuffer();
        sb.append("{\"").append(usernameattr).append("\":").append(userApiResp).append("}");
        return sb.toString();
    }

}
