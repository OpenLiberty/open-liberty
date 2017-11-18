/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.security.oauth;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

/**
 * If need more detail, can learn from org.apache.wink.client.handlers LtpaAuthSecurityHandler
 */
public class LibertyJaxRsClientOAuthInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientOAuthInterceptor.class, JAXRSClientConstants.TR_GROUP, JAXRSClientConstants.TR_RESOURCE_BUNDLE);
    private static final String AUTHN_TOKEN = "authnToken";
    private static final String WEB_TARGET = "webTarget";

    public LibertyJaxRsClientOAuthInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        //see if oauth, jwt or mpjwt handlers are used
        Object handleOauth = message.get(JAXRSClientConstants.OAUTH_HANDLER);
        Object handleJwt = message.get(JAXRSClientConstants.JWT_HANDLER);
        Object handleMpJwt = message.get(JAXRSClientConstants.MPJWT_HANDLER);

        if (handleMpJwt != null && "true".equals(handleMpJwt.toString().toLowerCase())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client configuration " + JAXRSClientConstants.MPJWT_HANDLER + " property is set to true ");
            }
            handleMpJwtToken(message);
        }
        else if (handleOauth != null || handleJwt != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client configuration property " + JAXRSClientConstants.OAUTH_HANDLER + " value is " + handleOauth);
            }
            String handler = handleOauth != null ? handleOauth.toString().toLowerCase() : "false";
            String jwtHandler = handleJwt != null ? handleJwt.toString().toLowerCase() : "false";
            handleOAuthTokens(message, handler, jwtHandler);
        }

    }

    /**
     * @param message
     */
    private void handleMpJwtToken(Message message) {
        String mpJwt = OAuthPropagationHelper.getMpJsonWebToken();
        if (mpJwt != null && !mpJwt.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Got MpJwt token from the subject. Set it on the request" + mpJwt);

                addAuthnHeader(mpJwt, message);
            }
        } else {
            String msg = Tr.formatMessage(tc, "warn_missing_mpjwt_token", new Object[] { AUTHN_TOKEN, WEB_TARGET, "mpjwt" });
            Tr.warning(tc, msg);
        }

    }

    private void handleOAuthTokens(Message message, String handleOauth, String jwtHandler) {
        boolean bOauth = handleOauth.equals("true");
        boolean bJwt = jwtHandler.equals("true");
        if (bOauth || bJwt) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, bOauth ? "About to get a OAuth access token" : "About to get a jwt token");
            }

            // retrieve the token from the Subject in current thread
            try {
                // this interceptor must depend on the appSecurity feature to use WebSecurityHelper.getSSOCookieFromSSOToken()
                String accessToken = bOauth ? OAuthPropagationHelper.getAccessToken() : OAuthPropagationHelper.getJwtToken();

                if (accessToken != null && !accessToken.isEmpty()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Retrieved an OAuth access/jwt token. About to set a request cookie: " + accessToken);
                    }
//                    //Authorization=[Bearer="<accessToken>"]
//                    @SuppressWarnings("unchecked")
//                    Map<String, List<String>> headers = (Map<String, List<String>>) message
//                                    .get(Message.PROTOCOL_HEADERS);
//                    headers.put("Authorization", Arrays.asList("Bearer " + accessToken));
//                    message.put(Message.PROTOCOL_HEADERS, headers);
                    addAuthnHeader(accessToken, message);
                } else { // no user credential available
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot find an OAuth access token out of the WSSubject");
                    }
                    //Because this is a client configuration property, we won't throws exception if it doesn't work, please analyze trace for detail
                    //throw new ProcessingException("Cannot find a ltpa authentication token off of the thread");
                }
            } catch (Exception e) {
                throw new ProcessingException(e);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No client OAuth handler configuration, skip");
            }
        }
    }

    private void addAuthnHeader(String token, Message message) {
        //Authorization=[Bearer="<accessToken>"]
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) message.get(Message.PROTOCOL_HEADERS);
        headers.put("Authorization", Arrays.asList("Bearer " + token));
        message.put(Message.PROTOCOL_HEADERS, headers);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Authorization header with Bearer token is added successfully!!!"); //TODO
        }
    }
}