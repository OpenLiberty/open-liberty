/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

public class AuthUtils {

    public static final TraceComponent tc = Tr.register(AuthUtils.class);

    @Sensitive
    public String getBearerTokenFromHeader(HttpServletRequest req) {
        return getBearerTokenFromHeader(req, "Authorization");
    }

    @Sensitive
    public String getBearerTokenFromHeader(HttpServletRequest req, String... headersToCheck) {
        if (headersToCheck == null) {
            return null;
        }
        for (String headerName : headersToCheck) {
            String hdrValue = req.getHeader(headerName);
            //if we are looking at custom header, then just return the value
            if (!isAuthorizationHeader(headerName)) {
                return hdrValue;
            } else {
                return getBearerTokenFromHeader(hdrValue, "Bearer ");
            }
        }
        return null;
    }

    @Sensitive
    public String getBearerTokenFromHeader(@Sensitive String rawHeaderValue, String scheme) {
        if (rawHeaderValue == null) {
            return rawHeaderValue;
        }
        if (scheme != null) {
            if (rawHeaderValue.startsWith(scheme)) {
                return rawHeaderValue.substring(scheme.length());
            }
        }
        return rawHeaderValue;
    }

    private boolean isAuthorizationHeader(String headerName) {
        return ("Authorization".equals(headerName));
    }

}
