/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.web.WebUtils;

import io.openliberty.security.oidcclientcore.utils.Utils;

public class AuthorizationRequestUtils {

    public static final TraceComponent tc = Tr.register(AuthorizationRequestUtils.class);

    public static final int STATE_LENGTH = 9;
    public static final int NONCE_LENGTH = 30;

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
        Integer realPort = getRealPort(request);
        int serverPort = request.getServerPort();

        StringBuffer requestURL = request.getRequestURL();
        if (shouldRewritePort(realPort, serverPort)) {
            requestURL = rewritePortInRequestOrigin(request, realPort);
        }
        requestURL = appendQueryString(request, requestURL);
        return requestURL.toString();
    }

    public String getBaseURL(HttpServletRequest request) {
        // due to some longstanding webcontainer strangeness, we have to do some extra things for certain behind-proxy cases to get the right port.
        Integer realPort = getRealPort(request);
        int serverPort = request.getServerPort();

        StringBuffer baseURL = rewritePortInBaseURL(request, shouldRewritePort(realPort, serverPort) ? realPort : serverPort);
        return baseURL.toString();
    }

    Integer getRealPort(HttpServletRequest request) {
        Integer realPort = null;
        if (request.getScheme().toLowerCase().contains("https")) {
            realPort = getWebUtils().getRedirectPortFromRequest(request);
        }
        return realPort;
    }

    WebUtils getWebUtils() {
        return new WebUtils();
    }

    boolean shouldRewritePort(Integer realPort, int serverPort) {
        if (realPort != null && realPort.intValue() != serverPort) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "serverport = " + serverPort + "real port is " + realPort.toString() + ", url will be rewritten to use real port");
            }
            return true;
        }
        return false;
    }

    StringBuffer rewritePortInRequestURL(HttpServletRequest request, int realPort) {
        StringBuffer requestURL = rewritePortInRequestOrigin(request, realPort);
        requestURL.append(request.getRequestURI());
        return requestURL;
    }

    StringBuffer rewritePortInBaseURL(HttpServletRequest request, int realPort) {
        StringBuffer baseURL = rewritePortInRequestOrigin(request, realPort);
        baseURL.append(request.getContextPath());
        return baseURL;
    }

    StringBuffer rewritePortInRequestOrigin(HttpServletRequest request, int realPort) {
        StringBuffer requestOrigin = new StringBuffer();
        requestOrigin.append(request.getScheme());
        requestOrigin.append("://");
        requestOrigin.append(request.getServerName());
        requestOrigin.append(":");
        requestOrigin.append(realPort);
        return requestOrigin;
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
