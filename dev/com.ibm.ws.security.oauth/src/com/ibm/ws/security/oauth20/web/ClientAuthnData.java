/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.oauth.core.api.error.oauth20.OAuth20DuplicateParameterException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.oauth20.util.Base64;

public class ClientAuthnData {

    private static TraceComponent tc = Tr.register(ClientAuthnData.class,
            "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    public static final String Authorization_Header = "Authorization";
    public static final String AUTHORIZATION_ENCODING = "Authorization-Encoding";
    public static final String BasicAuthEncoding = System.getProperty("com.ibm.websphere.security.BasicAuthEncoding", "UTF-8");
    String userName = null;
    String passWord = null;
    boolean authnData = false;
    boolean isBasicAuth = false;

    public ClientAuthnData(HttpServletRequest req, HttpServletResponse res) throws OAuth20DuplicateParameterException {
        String hdrValue = req.getHeader(Authorization_Header);
        if (hdrValue == null || !hdrValue.startsWith("Basic ")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization header was null or wasn't basic auth; looking for client_id and client_secret parameters");
            }
            this.passWord = checkForRepeatedOrEmptyParameter(req, OAuth20Constants.CLIENT_SECRET);
            this.userName = checkForRepeatedOrEmptyParameter(req, OAuth20Constants.CLIENT_ID);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization header set to basic auth; decoding header and extracting user name and password");
            }
            isBasicAuth = true;
            String encoding = req.getHeader(AUTHORIZATION_ENCODING);
            hdrValue = decodeAuthorizationHeader(hdrValue, encoding);
            int idx = hdrValue.indexOf(':');
            if (idx < 0) {
                this.userName = hdrValue;
            } else {
                this.userName = hdrValue.substring(0, idx);
                this.passWord = hdrValue.substring(idx + 1);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Use authentication data from Authentication head for client: " + this.userName);
        }
        if (userName != null && userName.length() > 0)
            authnData = true;
    }

    @Sensitive
    public static String decodeAuthorizationHeader(HttpServletRequest req) {
        String hdrValue = req.getHeader(Authorization_Header);
        String encoding = req.getHeader(AUTHORIZATION_ENCODING);
        if (hdrValue == null || !hdrValue.startsWith("Basic ")) {
            return null;
        }
        return decodeAuthorizationHeader(hdrValue, encoding);
    }

    @Sensitive
    public static String decodeAuthorizationHeader(@Sensitive String hdrValue, String encoding) {
        if (hdrValue == null) {
            return null;
        }
        boolean useDefault = false;
        if (encoding == null) {
            encoding = BasicAuthEncoding;
            useDefault = true;
        }

        // TODO: Why do we chop off the first 6 characters? Assuming hdrValue is "Basic ..." ?
        byte[] headerBytes = Base64.decode(hdrValue.substring(6));
        boolean decoded = false;
        if (encoding != null && encoding.length() > 0) {
            try {
                hdrValue = new String(headerBytes, encoding);
                decoded = true;
            } catch (Exception e) {
                decoded = false;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Decoding fails with encoding:" + encoding, e);
                }
            }
        }
        if (!decoded && !useDefault) {
            try {
                hdrValue = new String(headerBytes, BasicAuthEncoding);
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Decoding fails with default encoding:" + BasicAuthEncoding, e);
                }
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

    public boolean isBasicAuth() {
        return isBasicAuth;
    }

    /**
     * Throws an exception if multiple values exist for the given parameter name. Otherwise returns the value of the
     * parameter (null if the parameter doesn't exist or has an empty value). These cases are covered by sections 3.1
     * and 3.2 of the OAuth 2.0 spec (RFC6749).
     *
     * Sections 3.1 and 3.2 [RFC6749]:
     * "Parameters sent without a value MUST be treated as if they were omitted from the request... Request and response
     * parameters MUST NOT be included more than once."
     *
     * @param request
     * @param parameter
     * @return
     * @throws OAuth20DuplicateParameterException
     */
    @Sensitive
    private String checkForRepeatedOrEmptyParameter(HttpServletRequest request, String parameter) throws OAuth20DuplicateParameterException {
        String[] paramArray = request.getParameterValues(parameter);
        if (paramArray != null && paramArray.length > 1) {
            throw new OAuth20DuplicateParameterException("security.oauth20.error.duplicate.parameter", parameter);
        }
        // TODO
        if (paramArray == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No values found for parameter: " + parameter);
            }
            return null;
        }
        String paramValue = paramArray[0];
        if (paramValue.isEmpty()) {
            return null;
        }
        return paramValue;
    }

}
