/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml.saml2.core.Assertion;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.RequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;

import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UnsolicitedResponseCache;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class UnsolicitedHandler {
    private static TraceComponent tc = Tr.register(UnsolicitedHandler.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    HttpServletRequest request;
    HttpServletResponse response;
    SsoRequest samlRequest;
    Map<String, Object> parameters;
    SsoSamlService ssoService;
    
    InitialRequestUtil irUtil= new InitialRequestUtil();

    public UnsolicitedHandler(HttpServletRequest request,
                              HttpServletResponse response,
                              SsoRequest samlRequest,
                              Map<String, Object> parameters) {
        this.request = request;
        this.response = response;
        this.samlRequest = samlRequest;
        this.parameters = parameters;
        this.ssoService = (SsoSamlService) parameters.get(Constants.KEY_SAML_SERVICE);
    }

    /**
     * This is idp initiated
     *
     * @param relayState
     * @throws SamlException
     */
    public void handleRequest(String externalRelayState) throws SamlException {
        // 1) get the request from cookie/cache first
        // IF useRelayStateForTarget is FALSE:
        //    * use targetPageUrl
        // IF useRelayStateForTarget is TRUE (default):
        // 2) If no valid cached RequestInfo, then use the relay state from the acs request
        // 3) If no relayState, then try the configured targetPageUrl
        String relayState = RequestUtil.getCookieId((IExtendedRequest) request, response, Constants.COOKIE_WAS_REQUEST);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "RelayState from cookie is [" + relayState + "]");
        }
        RequestUtil.removeCookie(request, response, Constants.COOKIE_WAS_REQUEST); // removing unsolicited state cookie
        // check if sp_unsolicited or idp_init
        if (relayState == null ||
            relayState.isEmpty() ||
            !relayState.startsWith(Constants.IDP_INITAL)) {
            if (samlRequest.getSsoConfig().getUseRelayStateForTarget() == false) {
                relayState = samlRequest.getSsoConfig().getTargetPageUrl(); // TODO checking the URL
            } else {
                // Let's try to get sp unsolicited state from cookie
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "RelayState from SAMLResponse is [" + externalRelayState + "]");
                }
                relayState = externalRelayState;
                if (relayState == null || relayState.isEmpty()) {
                    // no relayState found. try the targetPageUrl (default landing url 211770)
                    relayState = samlRequest.getSsoConfig().getTargetPageUrl(); // TODO checking the URL
                }
            }
        }
        if (relayState == null || relayState.isEmpty()) {
            // Error: Can not find the relayState to redirected to.
            throw new SamlException("SAML20_NO_PROTECTED_RESOURCE_ENDPOINT_ERR",
                            //"SAML20_NO_PROTECTED_RESOURCE_ENDPOINT_ERR=CWWKS5041E: The SAML Request failed due to no protected resource endpoint is found.",
                            null, new Object[] {});
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Final target [" + relayState + "]");
        }

        String rawSamlResponse = request.getParameter(Constants.SAMLResponse);
        if (rawSamlResponse != null) {
            try {
                relayState = URLDecoder.decode(relayState,
                                               Constants.UTF8);
            } catch (UnsupportedEncodingException e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Internal error process SAML Web SSO Version 2.0 request", e);
                }
                throw new SamlException(e); // let the SamlException handle the Exception
            }
            BasicMessageContext<?, ?> msgCtx = WebSSOConsumer.getInstance().handleSAMLResponse(request,
                                                                                                  response,
                                                                                                  ssoService,
                                                                                                  null, // do not check inResponse in idp_init (SP Unsolicited)
                                                                                                  samlRequest);
            //Prevent replay
            UnsolicitedResponseCache resCache = ssoService.getUnsolicitedResponseCache(samlRequest.getProviderName());
            Assertion assertion = msgCtx.getValidatedAssertion();
            if (resCache.isValid(assertion.getID())) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The SAML Assertion with ID " + assertion.getID() + " can not be processed twice.");
                }
                throw new SamlException("SAML20_RESPONSE_REPLAY",
                                //SAML20_RESPONSE_REPLAY=CWWKS5082E: The SAML assertion with ID [{0}] has already been received, and cannot be accepted.,
                                null, new Object[] { assertion.getID() });
            } else {
                long exp = assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter().getMillis();
                resCache.put(assertion.getID(), Long.valueOf(exp));
            }

            // The msgCtx won't be null, otherwise, it throws Exception already
            Cache cache = ssoService.getAcsCookieCache(samlRequest.getProviderName());
            // Cache won't be null, since getAcsCookieCache does not return null
            HttpRequestInfo requestInfo = getUnsolicitedRequestInfo(msgCtx, relayState, cache);
            requestInfo.setWithFragmentUrl(request, response);
            redirectToRelayState(msgCtx,
                                 samlRequest.getProviderName(),
                                 cache,
                                 requestInfo);
        } else {
            throw new SamlException(
                            //"SAML20_NO_SAML_RESPONSE",
                            "Cannot process the request because SAML Response from the IdP is missing", null, // cause
                            new Object[] {});
        }
    }

    /**
     * @param msgCtx
     * @param relayState
     * @return
     * @throws SamlException
     */
    HttpRequestInfo getUnsolicitedRequestInfo(BasicMessageContext<?, ?> msgCtx, String relayState, Cache cache) throws SamlException {
        HttpRequestInfo requestInfo = getCachedRequestInfo(relayState, cache);
        // This got to be the idp_initiated. If not, it will fail when  we redirect the request
        if (requestInfo == null) {
            requestInfo = new HttpRequestInfo(relayState, "");
        }
        return requestInfo;
    }

    /**
     * @throws Exception
     */
    protected void redirectToRelayState(BasicMessageContext<?, ?> msgCtx,
                                        String providerName,
                                        Cache cache,
                                        HttpRequestInfo requestInfo) throws SamlException {
        String cacheId = SamlUtil.generateRandom(); // no need to Base64 encode
        UserData data = msgCtx.getUserDataIfReady();
        cache.put(cacheId, data);
        requestInfo.redirectCachedHttpRequest(request,
                                              response,
                                              Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(providerName),
                                              cacheId);
    }

    /**
     * @param relayState
     * @return
     * @throws SamlException
     */
    protected HttpRequestInfo getCachedRequestInfo(String relayState, Cache cache) throws SamlException {
        if (relayState == null)
            return null;
        HttpRequestInfo requestInfo = null;
        if (relayState.startsWith(Constants.IDP_INITAL)) {
            String cacheKey = relayState.substring(Constants.IDP_INITAL.length());
            requestInfo = (HttpRequestInfo) cache.get(cacheKey);
            if (requestInfo != null) {
                cache.remove(cacheKey); // the cache can only be used once
                irUtil.removeCookie(relayState, request, response);
            } else { // since there is a cookie value with idp_initial exists, it does mean that the request was originated at our saml sp side
                requestInfo = irUtil.recreateHttpRequestInfo(relayState, this.request, this.response, this.ssoService);
            }
        }
        return requestInfo;
    }

}
