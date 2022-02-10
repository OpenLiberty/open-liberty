/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

public class BackchannelLogoutHelper {

    private static TraceComponent tc = Tr.register(BackchannelLogoutHelper.class);

    public static final String LOGOUT_TOKEN_PARAM_NAME = "logout_token";

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public BackchannelLogoutHelper(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void handleBackchannelLogoutRequest() {
        if (!ProductInfo.getBetaEdition()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String httpMethod = request.getMethod();
        if (!"POST".equalsIgnoreCase(httpMethod)) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        String logoutTokenParameter = request.getParameter(LOGOUT_TOKEN_PARAM_NAME);
        if (logoutTokenParameter == null || logoutTokenParameter.isEmpty()) {
            // TODO
            Tr.formatMessage(tc, "", new Object[] { request.getPathInfo() });
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // TODO

        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache=");
    }

}
