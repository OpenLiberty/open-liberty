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
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;

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
import com.ibm.ws.security.saml.sso20.internal.utils.InitialRequestUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;

public class SolicitedHandler {
    private static TraceComponent tc = Tr.register(SolicitedHandler.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    HttpServletRequest request;
    HttpServletResponse response;
    SsoRequest samlRequest;
    Map<String, Object> parameters;
    SsoSamlService ssoService;
    InitialRequestUtil irUtil = new InitialRequestUtil();

    public SolicitedHandler(HttpServletRequest request,
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
     * This is sp initiated
     * Need to make sure the inResponse is in the SAMLAssertion
     *
     * @param relayState
     * @throws SamlException
     */
    public void handleRequest(String externalRelayState) throws SamlException {
        String rawSamlResponse = request.getParameter(Constants.SAMLResponse);
        if (rawSamlResponse != null) {
            String relayState = null;
            try {
                relayState = URLDecoder.decode(externalRelayState,
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
                                                                                                  relayState, // make sure the inResponse is in the SAMLAssertion
                                                                                                  samlRequest);
            // The msgCtx won't be null, otherwise, it throws Exception already
            Cache cache = ssoService.getAcsCookieCache(samlRequest.getProviderName());
            // Cache won't be null, since getAcsCookieCache does not return null
            HttpRequestInfo requestInfo = msgCtx.getCachedRequestInfo();
            DateTime authnRequestExpiredTime = requestInfo.getBirthTime().plus(ssoService.getConfig().getAuthnRequestTime());
            if (authnRequestExpiredTime.isBeforeNow()) {
                // authnRequest expires
                // compare with local time, no time to handle clockSkew
                // TODO need to add clock skew in the cluster environment

                throw new SamlException("SAML20_AUTHN_REQUEST_EXPIRED",
                                //SAML20_AUTHN_REQUEST_EXPIRED=CWWKS5081E: The authentication request sent from the service provider to the IdP takes too long.
                                null, new Object[] { new Date(requestInfo.getBirthTime().getMillis()),
                                                     (ssoService.getConfig().getAuthnRequestTime()) / 60000, new Date(authnRequestExpiredTime.getMillis()), new Date() });
            }
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

}
