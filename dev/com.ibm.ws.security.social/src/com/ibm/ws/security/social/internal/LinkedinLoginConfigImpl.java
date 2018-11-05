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
package com.ibm.ws.security.social.internal;

import java.util.Arrays;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;

@Component(name = "com.ibm.ws.security.social.linkedin", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = SocialLoginConfig.class, property = { "service.vendor=IBM", "type=linkedinLogin" })
public class LinkedinLoginConfigImpl extends Oauth2LoginConfigImpl {
    public static final TraceComponent tc = Tr.register(LinkedinLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.useSystemPropertiesForHttpClientConnections = configUtils.getBooleanConfigAttribute(props, KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, false);
        this.authorizationEndpoint = configUtils.getConfigAttribute(props, KEY_authorizationEndpoint);
        this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_tokenEndpoint);
        this.userApi = configUtils.getConfigAttribute(props, KEY_userApi);
        this.scope = configUtils.getConfigAttribute(props, KEY_scope);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttribute(props, KEY_tokenEndpointAuthMethod);
        this.userApiNeedsSpecialHeader = configUtils.getBooleanConfigAttribute(props, KEY_userApiNeedsSpecialHeader, this.userApiNeedsSpecialHeader);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
    }

    @Override
    protected void resetLazyInitializedMembers() {
        super.resetLazyInitializedMembers();
        this.responseType = ClientConstants.CODE;
    }

    @Override
    protected void debug() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "" + this);
            Tr.debug(tc, KEY_clientId + " = " + clientId);
            Tr.debug(tc, KEY_clientSecret + " is null = " + (clientSecret == null));
            Tr.debug(tc, KEY_authorizationEndpoint + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_tokenEndpoint + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_userApi + " = " + userApi);
            Tr.debug(tc, "userApiConfigs = " + (userApiConfigs == null ? "null" : userApiConfigs.length));
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_mapToUserRegistry + " = " + mapToUserRegistry);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, CFG_KEY_jwtRef + " = " + jwtRef);
            Tr.debug(tc, CFG_KEY_jwtClaims + " = " + ((jwtClaims == null) ? null : Arrays.toString(jwtClaims)));
            Tr.debug(tc, KEY_isClientSideRedirectSupported + " = " + isClientSideRedirectSupported);
            Tr.debug(tc, KEY_displayName + " = " + displayName);
            Tr.debug(tc, KEY_website + " = " + website);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
            Tr.debug(tc, KEY_userApiNeedsSpecialHeader + " = " + userApiNeedsSpecialHeader);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
        }
    }

}
