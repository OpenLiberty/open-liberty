/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.userinfo;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.config.MetadataUtils;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import io.openliberty.security.oidcclientcore.exceptions.OidcDiscoveryException;
import io.openliberty.security.oidcclientcore.exceptions.UserInfoResponseException;
import io.openliberty.security.oidcclientcore.userinfo.UserInfoRequestor.Builder;

public class UserInfoHandler {

    public static final TraceComponent tc = Tr.register(UserInfoHandler.class);

    public Map<String, Object> getUserInfoClaims(OidcClientConfig oidcClientConfig,
                                                 String accessToken) throws UserInfoResponseException, OidcDiscoveryException, OidcClientConfigurationException {
        String userInfoEndpoint = MetadataUtils.getUserInfoEndpoint(oidcClientConfig);
        UserInfoRequestor userInfoRequester = createUserInfoRequestor(userInfoEndpoint, oidcClientConfig, accessToken);
        UserInfoResponse userInfoResponse = userInfoRequester.requestUserInfo();
        if (userInfoResponse != null) {
            return userInfoResponse.asMap();
        }
        return null;
    }

    UserInfoRequestor createUserInfoRequestor(String userInfoEndpoint, OidcClientConfig oidcClientConfig, String accessToken) throws OidcDiscoveryException {
        Builder builder = new UserInfoRequestor.Builder(oidcClientConfig, userInfoEndpoint, accessToken);
        return builder.build();
    }

}
