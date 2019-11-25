/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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

@Component(name = "com.ibm.ws.security.social.twitter", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = SocialLoginConfig.class, property = { "service.vendor=IBM", "type=twitterLogin" })
public class TwitterLoginConfigImpl extends Oauth2LoginConfigImpl {
    public static final TraceComponent tc = Tr.register(TwitterLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_consumerKey = "consumerKey";
    public static final String KEY_consumerSecret = "consumerSecret";
    public static final String KEY_requestTokenUrl = "requestTokenUrl";
    public static final String KEY_userAuthorizationUrl = "userAuthorizationUrl";
    public static final String KEY_accessTokenUrl = "accessTokenUrl";

    @Override
    protected void checkForRequiredConfigAttributes(Map<String, Object> props) {
        getRequiredConfigAttribute(props, KEY_consumerKey);
        getRequiredSerializableProtectedStringConfigAttribute(props, KEY_consumerSecret);
    }

    @Override
    protected void setAllConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.clientId = configUtils.getConfigAttribute(props, KEY_consumerKey);
        this.clientSecret = configUtils.processProtectedString(props, KEY_consumerSecret);
        this.useSystemPropertiesForHttpClientConnections = configUtils.getBooleanConfigAttribute(props, KEY_USE_SYSPROPS_FOR_HTTPCLIENT_CONNECTONS, false);
        this.requestTokenUrl = configUtils.getConfigAttribute(props, KEY_requestTokenUrl);
        this.authorizationEndpoint = configUtils.getConfigAttribute(props, KEY_userAuthorizationUrl);
        this.tokenEndpoint = configUtils.getConfigAttribute(props, KEY_accessTokenUrl);
        this.userApi = configUtils.getConfigAttribute(props, KEY_userApi);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.mapToUserRegistry = configUtils.getBooleanConfigAttribute(props, KEY_mapToUserRegistry, this.mapToUserRegistry);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.isClientSideRedirectSupported = configUtils.getBooleanConfigAttribute(props, KEY_isClientSideRedirectSupported, this.isClientSideRedirectSupported);
        this.displayName = configUtils.getConfigAttribute(props, KEY_displayName);
        this.website = configUtils.getConfigAttribute(props, KEY_website);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
    }

    @Override
    protected void debug() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "" + this);
            Tr.debug(tc, KEY_consumerKey + " = " + clientId);
            Tr.debug(tc, KEY_consumerSecret + " is null = " + (clientSecret == null));
            Tr.debug(tc, KEY_requestTokenUrl + " = " + requestTokenUrl);
            Tr.debug(tc, KEY_userAuthorizationUrl + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_accessTokenUrl + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_userApi + " = " + userApi);
            Tr.debug(tc, "userApiConfigs = " + (userApiConfigs == null ? "null" : userApiConfigs.length));
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_mapToUserRegistry + " = " + mapToUserRegistry);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, CFG_KEY_jwtRef + " = " + jwtRef);
            Tr.debug(tc, CFG_KEY_jwtClaims + " = " + ((jwtClaims == null) ? null : Arrays.toString(jwtClaims)));
            Tr.debug(tc, KEY_isClientSideRedirectSupported + " = " + isClientSideRedirectSupported);
            Tr.debug(tc, KEY_displayName + " = " + displayName);
            Tr.debug(tc, KEY_website + " = " + website);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
        }
    }

}
