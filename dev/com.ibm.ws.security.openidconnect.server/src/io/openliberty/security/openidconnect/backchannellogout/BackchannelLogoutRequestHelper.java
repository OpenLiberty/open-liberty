/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.oauth20.ProvidersService;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.webcontainer.security.openidconnect.OidcServerConfig;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.osgi.SecurityOSGiUtils;

public class BackchannelLogoutRequestHelper {

    private static TraceComponent tc = Tr.register(BackchannelLogoutRequestHelper.class);

    private final PrivilegedAction<ExecutorService> getExecutorServiceAction = new GetExecutorServiceAction();

    private final HttpServletRequest request;
    private final OidcServerConfig oidcServerConfig;

    public BackchannelLogoutRequestHelper(HttpServletRequest request, OidcServerConfig oidcServerConfig) {
        this.request = request;
        this.oidcServerConfig = oidcServerConfig;
    }

    /**
     * Uses the provided ID token string to build logout tokens and sends back-channel logout requests to all of the necessary
     * RPs. If the ID token contains multiple audiences, logout tokens are created for each client audience. Logout tokens are
     * also created for all RPs that the OP is aware of having active or recently valid sessions.
     *
     */
    public void sendBackchannelLogoutRequests(String user, String idTokenString) {
        if (!shouldSendLogoutRequests(user, idTokenString)) {
            return;
        }
        Map<OidcBaseClient, Set<String>> logoutTokens = null;
        try {
            LogoutTokenBuilder tokenBuilder = new LogoutTokenBuilder(request, oidcServerConfig);
            if (idTokenString == null || idTokenString.isEmpty()) {
                logoutTokens = tokenBuilder.buildLogoutTokensFromUserName(user);
            } else {
                logoutTokens = tokenBuilder.buildLogoutTokensFromIdTokenString(idTokenString);
            }
        } catch (LogoutTokenBuilderException e) {
            Tr.error(tc, "OIDC_SERVER_BACKCHANNEL_LOGOUT_REQUEST_ERROR", oidcServerConfig.getProviderId(), e.getMessage());
            return;
        }
        sendBackchannelLogoutRequestsToClients(logoutTokens);
    }

    boolean shouldSendLogoutRequests(String user, String idTokenString) {
        if (!ProductInfo.getBetaEdition()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Beta mode is not enabled; back-channel logout will not be performed");
            }
            return false;
        }
        if ((user == null || user.isEmpty()) && (idTokenString == null || idTokenString.isEmpty())) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Neither a user name nor an ID token string was provided, so back-channel logout will not be performed.");
            }
            return false;
        }
        if (!hasClientWithBackchannelLogoutUri(oidcServerConfig)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No client has a back-channel logout uri set up, so back-channel logout will not be performed.");
            }
            return false;
        }
        return true;
    }

    public static boolean hasClientWithBackchannelLogoutUri(OidcServerConfig oidcServerConfig) {
        String oauthProviderName = oidcServerConfig.getOauthProviderName();
        OAuth20Provider provider = ProvidersService.getOAuth20Provider(oauthProviderName);
        if (provider == null) {
            return false;
        }
        return hasClientWithBackchannelLogoutUri(provider);
    }

    @FFDCIgnore(OidcServerException.class)
    static boolean hasClientWithBackchannelLogoutUri(OAuth20Provider provider) {
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        if (clientProvider == null) {
            return false;
        }
        try {
            for (OidcBaseClient client : clientProvider.getAll()) {
                String backchannelLogoutUri = client.getBackchannelLogoutUri();
                if (backchannelLogoutUri != null && !backchannelLogoutUri.isEmpty()) {
                    return true;
                }
            }
        } catch (OidcServerException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "There was an issue getting all the OIDC OAuth20 clients.");
            }
            return false;
        }
        return false;
    }

    void sendBackchannelLogoutRequestsToClients(Map<OidcBaseClient, Set<String>> clientsAndLogoutTokens) {
        if (clientsAndLogoutTokens == null || clientsAndLogoutTokens.isEmpty()) {
            return;
        }
        List<BackchannelLogoutRequest> requests = createLogoutRequests(clientsAndLogoutTokens);
        sendAllBackchannelLogoutRequests(requests);
    }

    List<BackchannelLogoutRequest> createLogoutRequests(Map<OidcBaseClient, Set<String>> clientsAndLogoutTokens) {
        List<BackchannelLogoutRequest> requests = new ArrayList<>();
        for (Entry<OidcBaseClient, Set<String>> entry : clientsAndLogoutTokens.entrySet()) {
            OidcBaseClient client = entry.getKey();
            for (String logoutToken : entry.getValue()) {
                BackchannelLogoutRequest request = new BackchannelLogoutRequest(oidcServerConfig, client.getBackchannelLogoutUri(), logoutToken);
                requests.add(request);
            }
        }
        return requests;
    }

    void sendAllBackchannelLogoutRequests(List<BackchannelLogoutRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<Future<BackchannelLogoutRequest>> futures;
        ExecutorService executorService = getExecutorService();
        try {
            futures = executorService.invokeAll(requests, oidcServerConfig.getBackchannelLogoutRequestTimeout(), TimeUnit.SECONDS);
        } catch (Exception e) {
            Tr.error(tc, "BACKCHANNEL_LOGOUT_REQUESTS_INVOKE_ALL_FAILED", oidcServerConfig.getProviderId(), e);
            return;
        }
        processLogoutRequestFutures(requests, futures);
    }

    void processLogoutRequestFutures(List<BackchannelLogoutRequest> requests, List<Future<BackchannelLogoutRequest>> futures) {
        if (requests.size() != futures.size()) {
            // This should never happen
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Original task list (" + requests.size() + ") and futures list (" + futures.size() + ") did not have the same size");
            }
            return;
        }
        for (int i = 0; i < futures.size(); i++) {
            processLogoutRequestFuture(requests.get(i), futures.get(i));
        }
    }

    void processLogoutRequestFuture(BackchannelLogoutRequest originalRequest, Future<BackchannelLogoutRequest> future) {
        try {
            BackchannelLogoutRequest result = future.get();
            HttpResponse response = result.getResponse();
            if (response == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to obtain a response from request to " + originalRequest.getUrl());
                }
                return;
            }
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                String responseBody = readResponseBody(response);
                JwtClaims logoutTokenClaims = getLogoutTokenClaims(result);
                Tr.error(tc, "BACKCHANNEL_LOGOUT_RESPONSE_NOT_SUCCESSFUL", oidcServerConfig.getProviderId(), result.getUrl(), logoutTokenClaims, statusCode, responseBody);
            }
        } catch (Exception e) {
            JwtClaims logoutTokenClaims = getLogoutTokenClaims(originalRequest);
            Tr.error(tc, "BACKCHANNEL_LOGOUT_REQUEST_EXCEPTION", oidcServerConfig.getProviderId(), originalRequest.getUrl(), logoutTokenClaims, e);
        }
    }

    @FFDCIgnore(Exception.class)
    JwtClaims getLogoutTokenClaims(BackchannelLogoutRequest bclRequest) {
        String logoutToken = bclRequest.getLogoutToken();
        try {
            JwtContext jwtContext = JwtParsingUtils.parseJwtWithoutValidation(logoutToken);
            return jwtContext.getJwtClaims();
        } catch (Exception e) {
            // We built the logout token, so this should never happen
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to parse logout token to obtain claims: " + e);
            }
            return null;
        }
    }

    @FFDCIgnore(Exception.class)
    private String readResponseBody(HttpResponse response) {
        String responseBody = null;
        try {
            HttpEntity responseEntity = response.getEntity();
            responseBody = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to read response body: " + e);
            }
        }
        return responseBody;
    }

    ExecutorService getExecutorService() {
        ExecutorService executorService;
        if (System.getSecurityManager() == null) {
            executorService = getExecutorServiceFromServiceRegistry();
        } else {
            executorService = AccessController.doPrivileged(getExecutorServiceAction);
        }
        return executorService;
    }

    private ExecutorService getExecutorServiceFromServiceRegistry() {
        return SecurityOSGiUtils.getService(getClass(), ExecutorService.class);
    }

    private class GetExecutorServiceAction implements PrivilegedAction<ExecutorService> {
        @Override
        public ExecutorService run() {
            return getExecutorServiceFromServiceRegistry();
        }
    }

}
