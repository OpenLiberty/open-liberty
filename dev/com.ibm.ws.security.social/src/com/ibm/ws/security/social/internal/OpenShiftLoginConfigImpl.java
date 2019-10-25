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

    public static final String AUTHORIZATION_ENDPOINT_PATH = "/oauth/authorize";
    public static final String TOKEN_ENDPOINT_PATH = "/oauth/token";
    public static final String DISCOVERY_ENDPOINT_PATH = "/.well-known/oauth-autorization-server";
    public static final String USER_API_ENDPOINT_PATH = "/apis/authentication.k8s.io/v1/tokenreviews";

    public static final String KEY_SERVICE_ACCOUNT_TOKEN = "serviceAccountToken";
    private String serviceAccountToken = null;

    public static final String KEY_OAUTH_SERVER = "oauthServer";
    private String oauthServer = null;

    public static final String KEY_USE_ACCESS_TOKEN_FROM_REQUEST = "useAccessTokenFromRequest";
    private String useAccessTokenFromRequest = null;

    public static final String KEY_TOKEN_HEADER_NAME = "tokenHeaderName";
    private String tokenHeaderName = null;

    private String discoveryEndpointUrl = null;

    DiscoveryConfigUtils discoveryUtils = new DiscoveryConfigUtils();

    @Override
    protected void setRequiredConfigAttributes(Map<String, Object> props) {
        this.clientId = getRequiredConfigAttribute(props, KEY_clientId);
        this.clientSecret = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_clientSecret);
        this.serviceAccountToken = getRequiredSerializableProtectedStringConfigAttribute(props, KEY_SERVICE_ACCOUNT_TOKEN);
    }

    @Override
    protected void setOptionalConfigAttributes(Map<String, Object> props) throws SocialLoginException {
        this.oauthServer = configUtils.getConfigAttribute(props, KEY_OAUTH_SERVER);
        this.useAccessTokenFromRequest = configUtils.getConfigAttribute(props, KEY_USE_ACCESS_TOKEN_FROM_REQUEST);
        this.tokenHeaderName = configUtils.getConfigAttribute(props, KEY_TOKEN_HEADER_NAME);
        this.authFilterRef = configUtils.getConfigAttribute(props, KEY_authFilterRef);
        this.redirectToRPHostAndPort = configUtils.getConfigAttribute(props, KEY_redirectToRPHostAndPort);
        hardCodeAttributeValues();
        setEndpointUrls(props);
    }

    /**
     * Hard-codes some values that don't have an associated configuration attribute in the openshiftLogin element.
     */
    protected void hardCodeAttributeValues() {
        this.scope = "user:full";
        this.tokenEndpointAuthMethod = ClientConstants.METHOD_client_secret_post;
        this.userNameAttribute = "username";
        this.groupNameAttribute = "groups";
        //this.userApiResponseIdentifier = "status";
    }

    private void setEndpointUrls(Map<String, Object> props) {
        this.authorizationEndpoint = buildEndpointUrl(AUTHORIZATION_ENDPOINT_PATH);
        this.tokenEndpoint = buildEndpointUrl(TOKEN_ENDPOINT_PATH);
        this.discoveryEndpointUrl = buildEndpointUrl(DISCOVERY_ENDPOINT_PATH);
        this.userApi = buildEndpointUrl(USER_API_ENDPOINT_PATH);
        // TODO
        if (discoveryEndpointUrl != null && !discoveryEndpointUrl.isEmpty()) {
            performDiscovery();
        } else {
        }
    }

    private String buildEndpointUrl(String endpointPath) {
        if (oauthServer == null && oauthServer.isEmpty()) {
            // TODO
            System.out.println("Gotta fix the hostnameAndPort value");
            return null;
        }
        // TODO
        return oauthServer + endpointPath;
        //        if (hostnameAndPort.endsWith("/")) {
        //        }
    }

    private void performDiscovery() {
        if (!discoveryEndpointUrl.startsWith("http")) {
            // TODO
        }
        // TODO
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
            Tr.debug(tc, KEY_OAUTH_SERVER + " = " + oauthServer);
            Tr.debug(tc, KEY_USE_ACCESS_TOKEN_FROM_REQUEST + " = " + useAccessTokenFromRequest);
            Tr.debug(tc, KEY_TOKEN_HEADER_NAME + " = " + tokenHeaderName);
            Tr.debug(tc, KEY_authFilterRef + " = " + authFilterRef);
            Tr.debug(tc, KEY_scope + " = " + scope);
            Tr.debug(tc, KEY_tokenEndpointAuthMethod + " = " + tokenEndpointAuthMethod);
            Tr.debug(tc, KEY_userNameAttribute + " = " + userNameAttribute);
            Tr.debug(tc, KEY_redirectToRPHostAndPort + " = " + redirectToRPHostAndPort);
        }
    }

    @Sensitive
    public String getServiceAccountToken() {
        return serviceAccountToken;
    }

    public String getOAuthServer() {
        return oauthServer;
    }

    public String getUseAccessTokenFromRequest() {
        return useAccessTokenFromRequest;
    }

    public String getTokenHeaderName() {
        return tokenHeaderName;
    }

}
