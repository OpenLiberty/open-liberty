/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ParameterList;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.openid20.OpenidClientAuthenticator;
import com.ibm.ws.security.openid20.OpenidConstants;
import com.ibm.ws.security.openid20.OpenidClientConfig;
import com.ibm.ws.security.openid20.TraceConstants;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This is the main class for the OpenId Relying Party (RP) to communicate with OpenID provider (OP).
 */
public class OpenidClientAuthenticatorImpl implements OpenidClientAuthenticator {

    static final TraceComponent tc = Tr.register(OpenidClientAuthenticatorImpl.class);
    OpenidClientConfig openidClientConfig;
    static Map<String, Object> requestCache = null; // this is supposed to be a singleton
    ConsumerManagerFactory consumerManagerFactory = new ConsumerManagerFactory(null);
    Utils utils;
    ConsumerManager consumerManager;

    public void initialize(OpenidClientConfig openidClientConfig, SSLContext sslContext) {
        this.openidClientConfig = openidClientConfig;
        consumerManager = consumerManagerFactory.getConsumerManager(openidClientConfig, sslContext);
        requestCache = Collections.synchronizedMap(new BoundedHashMap(openidClientConfig.getMaxDiscoveryCacheSize()));
        utils = new Utils(openidClientConfig);
    }

    /*
     * Create an authentication request that will send to openID provider for authentication
     */
    public void createAuthRequest(ServletRequest request, ServletResponse response) throws ServletException, Exception {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        //1. get openID Identifier
        String identifier = (String) req.getAttribute(OpenidConstants.OPENID_IDENTIFIER);
        if (identifier == null) {
            identifier = req.getParameter(OpenidConstants.OPENID_IDENTIFIER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openID identifier from request parameter:" + identifier);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openID identifier from request attribute(TAI):" + identifier);
            }
        }

        //2. do OP discovery
        DiscoveryInformation discoveryInformation = utils.discoverOpenID(consumerManager, identifier);

        //3. create a uniqueKey using digest for looping back after authentication
        String uniqueKey = MessageDigestUtil.getDigest();

        //4. create return URL. 
        String return_to_url = utils.createReturnToUrl(req, uniqueKey);

        //5. get RP realm
        String rpRealm = utils.getRpRealm(req);

        //6. build an authentication request message for the user specified in the discovery information provided as a parameter.
        AuthRequest authRequest = null;
        try {
            authRequest = consumerManager.authenticate(discoveryInformation, return_to_url, rpRealm);
        } catch (Exception e) {
            Tr.error(tc, "OPENID_AUTHENTICATE_FAILED", new Object[] { identifier });
            throw new IOException(e);
        }

        //7. add additional UserInfo attributes to authRequest 
        utils.addUserInfoAttributes(authRequest);

        //8. redirect to OP for authentication
        resp.setStatus(javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
        resp.sendRedirect(authRequest.getDestinationUrl(true));

        //9. cached the discoveryInformation
        requestCache.put(uniqueKey, discoveryInformation);
    }

    /*
     * Verify the response which generate by the OP
     * get back the discover info from the requestCache and validate the response information
     */
    public ProviderAuthenticationResult verifyResponse(ServletRequest request) throws ServletException, IOException {
        ProviderAuthenticationResult result = null;

        HttpServletRequest req = (HttpServletRequest) request;

        String receivingUrl = utils.getReceivingUrl(req);

        ParameterList response = new ParameterList(req.getParameterMap());

        // Verify the response
        VerificationResult verificationResult = null;
        DiscoveryInformation discoveryInfo = getDiscoveryInfoFromCache(req);

        try {
            verificationResult = consumerManager.verify(receivingUrl, response, discoveryInfo);
        } catch (Exception e) {
            Tr.error(tc, "OPENID_VERIFY_RESPONSE_FAILED", new Object[] { discoveryInfo.getClaimedIdentifier() });
            throw new IOException(e.getLocalizedMessage());
        }

        Identifier verifiedIdentifier = verificationResult.getVerifiedId();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Verification identifier:" + verifiedIdentifier);
        }

        // if the verification failed, the verified identifier is null 
        if (verifiedIdentifier == null) {
//            if (!openidClientConfig.isCheckImmediate() && verificationResult.getOPSetupUrl()!= null){
//                //TODO: do something, probably send another authn request, and give user chance to interact with op
//            }
            utils.verificationFailed(verificationResult, discoveryInfo);
        }

        AuthSuccess authSuccess = (AuthSuccess) verificationResult.getAuthResponse();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "AuthSuccess:" + authSuccess);
        }
        String identifier = verifiedIdentifier.getIdentifier();
        if (authSuccess != null) {
            Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

            Map<String, Object> attributes = utils.receiveUserInfoAttributes(authSuccess);

            String opEndPoint = utils.getOpEndPoint(discoveryInfo, authSuccess, attributes);
            customProperties.put("openidProvider", opEndPoint);

            String mapUserName = utils.resolveMapUserName(authSuccess, attributes);
            if (openidClientConfig.isIncludeCustomCacheKeyInSubject()) {
                String customCacheKey = mapUserName + attributes.hashCode();
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
                customProperties.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
            }

            String realm = utils.getRealmName(openidClientConfig, attributes);
            if (realm != null && !realm.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
            }
            ArrayList<String> groupIds = utils.getGroups(openidClientConfig, attributes, realm);
            if (groupIds != null && !groupIds.isEmpty()) {
                customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupIds);
            }

            if (openidClientConfig.isIncludeUserInfoInSubject()) {
                customProperties.putAll(attributes);
            }
            result = new ProviderAuthenticationResult(AuthResult.SUCCESS, HttpServletResponse.SC_OK, mapUserName, (Subject) null, customProperties, null);
        } else {
            Tr.error(tc, "OPENID_AUTHENTICATE_FAILED", identifier);
            throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OPENID_AUTHENTICATE_FAILED",
                                                               new Object[] { identifier },
                                                               "CWWKS1513E: OpenID authentication failed for identifier {0}."));

        }

        return result;
    }

    /**
     * @param req
     * @return
     * @throws IOException
     */
    private DiscoveryInformation getDiscoveryInfoFromCache(HttpServletRequest req) throws IOException {
        String uniqueKey = req.getParameter(OpenidConstants.RP_REQUEST_IDENTIFIER);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "uniqueKey:" + uniqueKey);
        }
        if (uniqueKey == null || uniqueKey.trim().isEmpty()) {
            Tr.error(tc, "OPENID_RP_REQUEST_IDENTIFIER_NULL");
            throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OPENID_RP_REQUEST_IDENTIFIER_NULL",
                                                               null,
                                                               "CWWKS1512E: OpenID replying party request identifier is null."));

        }

        DiscoveryInformation discoveryInfo = (DiscoveryInformation) requestCache.get(uniqueKey);

        if (discoveryInfo == null) {
            Tr.error(tc, "OPENID_CACHE_MISS_FOR_UNIQUE_KEY", uniqueKey);
            throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OPENID_CACHE_MISS_FOR_UNIQUE_KEY",
                                                               null,
                                                               "CWWKS1514E: There is no cache entry found for unique key {0}."));
        }
        requestCache.remove(uniqueKey);

        return discoveryInfo;
    }
}
