/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.userinfo;

import java.util.Map;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.http.EndpointRequest;
import io.openliberty.security.oidcclientcore.userinfo.UserInfoRequestor.Builder;

public class UserInfoHandler extends EndpointRequest {

    public static final TraceComponent tc = Tr.register(UserInfoHandler.class);

    public Map<String, Object> getUserInfoClaims(OidcClientConfig oidcClientConfig, String accessToken) throws UserInfoResponseException {
        String userInfoEndpoint = getUserInfoEndpoint(oidcClientConfig);
        if (userInfoEndpoint == null || userInfoEndpoint.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find a UserInfo endpoint in the client configuration or discovery data");
            }
            return null;
        }
        UserInfoRequestor userInfoRequester = createUserInfoRequestor(userInfoEndpoint, accessToken);
        UserInfoResponse userInfoResponse = userInfoRequester.requestUserInfo();
        if (userInfoResponse != null) {
            return userInfoResponse.asMap();
        }
        return null;
    }

    String getUserInfoEndpoint(OidcClientConfig oidcClientConfig) {
        String userInfoEndpoint = getUserInfoEndpointFromProviderMetadata(oidcClientConfig);
        if (userInfoEndpoint == null) {
            userInfoEndpoint = getUserInfoEndpointFromDiscoveryMetadata(oidcClientConfig);
        }
        return userInfoEndpoint;
    }

    String getUserInfoEndpointFromProviderMetadata(OidcClientConfig oidcClientConfig) {
        OidcProviderMetadata providerMetadata = oidcClientConfig.getProviderMetadata();
        if (providerMetadata != null) {
            // Provider metadata overrides properties discovered via providerUri
            String userInfoEndpoint = providerMetadata.getUserinfoEndpoint();
            if (userInfoEndpoint != null && !userInfoEndpoint.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "UserInfo endpoint found in the provider metadata: [" + userInfoEndpoint + "]");
                }
                return userInfoEndpoint;
            }
        }
        return null;
    }

    String getUserInfoEndpointFromDiscoveryMetadata(OidcClientConfig oidcClientConfig) {
        String userInfoEndpoint = null;
        JSONObject discoveryData = getProviderDiscoveryMetadata(oidcClientConfig);
        if (discoveryData != null) {
            userInfoEndpoint = (String) discoveryData.get(OidcDiscoveryConstants.METADATA_KEY_USERINFO_ENDPOINT);
        }
        return userInfoEndpoint;
    }

    UserInfoRequestor createUserInfoRequestor(String userInfoEndpoint, String accessToken) {
        Builder builder = new UserInfoRequestor.Builder(userInfoEndpoint, accessToken, getSSLSocketFactory());
        return builder.build();
    }

}
