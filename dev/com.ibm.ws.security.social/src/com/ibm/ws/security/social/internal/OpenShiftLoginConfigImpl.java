/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.config.DiscoveryConfigUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;

@Component(name = "com.ibm.ws.security.social.openshift", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true, service = SocialLoginConfig.class, property = { "service.vendor=IBM", "type=openshiftLogin" })
public class OpenShiftLoginConfigImpl extends Oauth2LoginConfigImpl {
    public static final TraceComponent tc = Tr.register(OpenShiftLoginConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final String KEY_SERVICE_ACCOUNT_TOKEN = "serviceAccountToken";
    private String serviceAccountToken = null;

    public static final String KEY_DISCOVERY_ENDPOINT = "discoveryEndpoint";
    private String discoveryEndpointUrl = null;

    public static final String KEY_TOKEN_HEADER_NAME = "tokenHeaderName";
    private String tokenHeaderName = null;

    public static final String KEY_HOSTNAME_AND_PORT = "hostnameAndPort";
    private String hostnameAndPort = null;

    DiscoveryConfigUtils discoveryUtils = new DiscoveryConfigUtils();

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
        this.serviceAccountToken = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_SERVICE_ACCOUNT_TOKEN);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.scope = configUtils.getConfigAttribute(props, KEY_scope);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        this.sslRef = configUtils.getConfigAttribute(props, KEY_sslRef);
        this.tokenHeaderName = configUtils.getConfigAttribute(props, KEY_TOKEN_HEADER_NAME);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        this.hostnameAndPort = configUtils.getConfigAttribute(props, KEY_HOSTNAME_AND_PORT);
        this.tokenEndpointAuthMethod = configUtils.getConfigAttributeWithDefaultValue(props, KEY_tokenEndpointAuthMethod, ClientConstants.METHOD_client_secret_post);
        this.userNameAttribute = configUtils.getConfigAttribute(props, KEY_userNameAttribute);
        setEndpointUrls(props);
    }

    private void setEndpointUrls(Map<String, Object> props) {
        setUserApiUrl(props);
        this.discoveryEndpointUrl = configUtils.getConfigAttribute(props, KEY_DISCOVERY_ENDPOINT);
        // TODO
        if (discoveryEndpointUrl != null && !discoveryEndpointUrl.isEmpty()) {
            performDiscovery();
        } else {
            this.authorizationEndpoint = buildEndpointUrl(props, KEY_authorizationEndpoint);
            this.tokenEndpoint = buildEndpointUrl(props, KEY_tokenEndpoint);
        }
    }

    private void setUserApiUrl(Map<String, Object> props) {
        // TODO
        this.userApi = hostnameAndPort + configUtils.getConfigAttribute(props, KEY_userApi);
    }

    private void performDiscovery() {
        if (!discoveryEndpointUrl.startsWith("http")) {
            // TODO
        }
        // TODO
    }

    private String buildEndpointUrl(Map<String, Object> props, String configKey) {
        if (hostnameAndPort == null && hostnameAndPort.isEmpty()) {
            // TODO
            System.out.println("Gotta fix the hostnameAndPort value");
            return null;
        }
        // TODO
        return hostnameAndPort + configUtils.getConfigAttribute(props, configKey);
        //        if (hostnameAndPort.endsWith("/")) {
        //        }
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
            Tr.debug(tc, KEY_SERVICE_ACCOUNT_TOKEN + " is null = " + (serviceAccountToken == null));
            Tr.debug(tc, KEY_DISCOVERY_ENDPOINT + " = " + discoveryEndpointUrl);
            Tr.debug(tc, KEY_authorizationEndpoint + " = " + authorizationEndpoint);
            Tr.debug(tc, KEY_tokenEndpoint + " = " + tokenEndpoint);
            Tr.debug(tc, KEY_userApi + " = " + userApi);
            Tr.debug(tc, "userApiConfigs = " + (userApiConfigs == null ? "null" : userApiConfigs.length));
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_sslRef + " = " + sslRef);
            Tr.debug(tc, KEY_TOKEN_HEADER_NAME + " = " + tokenHeaderName);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
            Tr.debug(tc, KEY_HOSTNAME_AND_PORT + " = " + hostnameAndPort);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
        }
    }

    @Sensitive
    public String getServiceAccountToken() {
        return serviceAccountToken;
    }

    public String getDiscoveryEndpoint() {
        return discoveryEndpointUrl;
    }

    public String getTokenHeaderName() {
        return tokenHeaderName;
    }

    public String getGostnameAndPort() {
        return hostnameAndPort;
    }

}
