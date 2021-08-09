/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openid20.tai;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

public class ClientAuthnData {
    private static TraceComponent tc = Tr.register(ClientAuthnData.class);

    public static final String Authorization_Header = "Authorization";
    public static final String AUTHORIZATION_ENCODING = "Authorization-Encoding";
    public static final String BasicAuthEncoding = System.getProperty("com.ibm.websphere.security.BasicAuthEncoding", "UTF-8");
    String userName = null;
    String passWord = null;
    boolean authnData = false;

    public ClientAuthnData(HttpServletRequest req, HttpServletResponse res) {
        String hdrValue = decodeAuthorizationHeader(req);
        if (hdrValue != null) {
            int idx = hdrValue.indexOf(':');
            if (idx >= 0) {
                this.userName = hdrValue.substring(0, idx);
                this.passWord = hdrValue.substring(idx + 1);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Use authentication data from Authentication head for client:" + this.userName);
        }
        if (userName != null && userName.length() > 0)
            authnData = true;
    }

    @Sensitive
    public static String decodeAuthorizationHeader(HttpServletRequest req) {
        String hdrValue = req.getHeader(Authorization_Header);
        String encoding = req.getHeader(AUTHORIZATION_ENCODING);
        if (hdrValue == null || !hdrValue.startsWith("Basic ")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "decodeAuthorizationHeader null");
            }
            return null;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "decodeAuthorizationHeader Basic");
        }

        return decodeAuthorizationHeader(hdrValue, encoding);
    }

    @Sensitive
    public static String decodeAuthorizationHeader(@Sensitive String hdrValue, String encoding) {
        if (hdrValue == null) {
            return null;
        }
        if (encoding == null) {
            encoding = BasicAuthEncoding;
        }
        try {
            hdrValue = Base64Coder.base64Decode(hdrValue.substring(6), encoding);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Decoding fails with encoding:" + encoding, e.getMessage());
            }
        }

        return hdrValue;
    }

    public String getUserName() {
        return this.userName;
    }

    @Sensitive
    public String getPassWord() {
        return this.passWord;
    }

    public boolean hasAuthnData() {
        return authnData;
    }
}
