/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.srt.ISRTServletRequest;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

public class AuthUtils {

    public static final TraceComponent tc = Tr.register(AuthUtils.class);

    @Sensitive
    public String getBearerTokenFromHeader(HttpServletRequest req) {
        String hdrValue = ISRTServletRequest.getHeader(req, HttpHeaderKeys.HDR_AUTHORIZATION);
        return getBearerTokenFromHeader(hdrValue, "Bearer ");
    }

    @Sensitive
    public String getBearerTokenFromHeader(HttpServletRequest req, String headerName) {
        if (headerName == null) {
            return null;
        }

        //if we are looking at custom header, then just return the value
        if (!isAuthorizationHeader(headerName)) {
            return req.getHeader(headerName);
        }

        return getBearerTokenFromHeader(req);
    }

    @Sensitive
    public String getBearerTokenFromHeader(@Sensitive String rawHeaderValue, String scheme) {
        if (rawHeaderValue == null) {
            return rawHeaderValue;
        }
        if (scheme != null) {
            scheme = scheme.trim();
            if (rawHeaderValue.startsWith(scheme)) {
                return rawHeaderValue.substring(scheme.length()).trim();
            }
        }
        return rawHeaderValue;
    }

    private boolean isAuthorizationHeader(String headerName) {
        return ("Authorization".equals(headerName));
    }

}
