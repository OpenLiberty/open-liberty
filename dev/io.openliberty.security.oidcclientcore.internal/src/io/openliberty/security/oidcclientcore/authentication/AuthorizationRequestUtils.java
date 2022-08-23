/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.random.RandomUtils;

import io.openliberty.security.oidcclientcore.utils.Utils;

public class AuthorizationRequestUtils {

    public static final TraceComponent tc = Tr.register(AuthorizationRequestUtils.class);

    public static final int STATE_LENGTH = 9;
    public static final int NONCE_LENGTH = 20;

    public String generateStateValue(HttpServletRequest request) {
        String strRandom = RandomUtils.getRandomAlphaNumeric(STATE_LENGTH);
        String timestamp = Utils.getTimeStamp();
        String state = timestamp + strRandom;
        if (request != null && !request.getMethod().equalsIgnoreCase("GET") && request.getParameter("oidc_client") != null) {
            state = state + request.getParameter("oidc_client");
        }
        return state;
    }

    @Sensitive
    @Trivial
    public static String createStateValueForStorage(String clientSecret, String state) {
        String timestamp = state.substring(0, Utils.TIMESTAMP_LENGTH);
        String newValue = state + clientSecret; // state already has a timestamp in it
        String value = HashUtils.digest(newValue);
        return timestamp + value;
    }

    public String generateNonceValue() {
        return RandomUtils.getRandomAlphaNumeric(NONCE_LENGTH);
    }

    public String getRequestUrl(HttpServletRequest request) {
        // due to some longstanding webcontainer strangeness, we have to do some extra things for certain behind-proxy cases to get the right port.
        boolean rewritePort = false;
        Integer realPort = null;
        if (request.getScheme().toLowerCase().contains("https")) {
            realPort = new com.ibm.ws.security.common.web.WebUtils().getRedirectPortFromRequest(request);
        }
        int port = request.getServerPort();
        if (realPort != null && realPort.intValue() != port) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "serverport = " + port + "real port is " + realPort.toString() + ", url will be rewritten to use real port");
            }
            rewritePort = true;
        }

        StringBuffer requestURL = request.getRequestURL();
        if (rewritePort) {
            requestURL = rewritePortInRequestUrl(request, realPort);
        }
        requestURL = appendQueryString(request, requestURL);
        return requestURL.toString();
    }

    StringBuffer rewritePortInRequestUrl(HttpServletRequest request, int realPort) {
        StringBuffer requestURL = new StringBuffer();
        requestURL.append(request.getScheme());
        requestURL.append("://");
        requestURL.append(request.getServerName());
        requestURL.append(":");
        requestURL.append(realPort);
        requestURL.append(request.getRequestURI());
        return requestURL;
    }

    StringBuffer appendQueryString(HttpServletRequest request, StringBuffer requestURL) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestURL.append("?");
            requestURL.append(queryString);
        }
        return requestURL;
    }

}
