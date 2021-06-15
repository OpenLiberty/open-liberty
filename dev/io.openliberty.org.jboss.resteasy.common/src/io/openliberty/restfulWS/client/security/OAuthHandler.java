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
package io.openliberty.restfulWS.client.security;

import java.util.Arrays;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class OAuthHandler {
    private static final TraceComponent tc = Tr.register(OAuthHandler.class);

    private static final String AUTHN_TOKEN = "authnToken";
    private static final String WEB_TARGET = "webTarget";

    public static void handleMpJwtToken(ClientRequestContext crc) {
        String mpJwt = OAuthPropagationHelper.getMpJsonWebToken();
        if (mpJwt != null && !mpJwt.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Got MpJwt token from the subject. Set it on the request" + mpJwt);
            }
            addAuthnHeader(mpJwt, crc);
        } else {
            // CWWKW0707W: The [{0}] attribute in the [{1}] configuration is set to [{2}], but the MicroProfile JSON Web Token (JWT) is not available. The request does not contain an Authorization header with the token.
            Tr.warning(tc, "warn_missing_mpjwt_token", new Object[] { AUTHN_TOKEN, WEB_TARGET, "mpjwt" });
        }
    }

    public static void handleJwtToken(ClientRequestContext crc) {
        try {
            String token = OAuthPropagationHelper.getJwtToken();
            if (token != null && !token.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Retrieved a JWT token. About to set a request cookie: " + token);
                }
                addAuthnHeader(token, crc);
            } else { // no user credential available
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot find a JWT token out of the WSSubject");
                }
            }
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }
 
    public static void handleOAuthToken(ClientRequestContext crc) {
        try {
            String token = OAuthPropagationHelper.getAccessToken();
            if (token != null && !token.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Retrieved an OAuth access token. About to set a request cookie: " + token);
                }
                addAuthnHeader(token, crc);
            } else { // no user credential available
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot find an OAuth access token out of the WSSubject");
                }
            }
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private static void addAuthnHeader(String token, ClientRequestContext crc) {
        //Authorization=[Bearer="<accessToken>"]
        crc.getHeaders().put("Authorization", Arrays.asList("Bearer " + token));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Authorization header with Bearer token is added successfully!!!");
        }
    }
}
