/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

public class AuthUtils {

    public static final TraceComponent tc = Tr.register(AuthUtils.class);

    public String getBearerTokenFromHeader(HttpServletRequest req) {
        return getBearerTokenFromHeader(req, "Authorization");
    }

    public String getBearerTokenFromHeader(HttpServletRequest req, String... headersToCheck) {
        if (headersToCheck == null) {
            return null;
        }
        for (String headerName : headersToCheck) {
            String hdrValue = req.getHeader(headerName);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, headerName + " header=", hdrValue);
            }
            String bearerAuthzMethod = "Bearer ";
            if (hdrValue != null && hdrValue.startsWith(bearerAuthzMethod)) {
                hdrValue = hdrValue.substring(bearerAuthzMethod.length());
            }
            return hdrValue;
        }
        return null;
    }

}
