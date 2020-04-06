/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.krb5.SpnegoUtil;
import com.ibm.ws.security.spnego.internal.Krb5Util;
import com.ibm.ws.security.spnego.internal.SpnegoConfigImpl;
import com.ibm.ws.webcontainer.security.AuthResult;
import com.ibm.ws.webcontainer.security.AuthenticationResult;

/**
 * This object takes requests from the TrustAssociationInterceptorImpl classes and sends them to the
 * corresponding objects. Acts as a controller object.
 */
public class SpnegoAuthenticator {
    public static final TraceComponent tc = Tr.register(SpnegoAuthenticator.class);

    private HashMap<String, String> hostMap = new HashMap<String, String>();
    private final Krb5Util krb5Util = new Krb5Util();
    private final AuthenticationResult CONTINUE = new AuthenticationResult(AuthResult.CONTINUE, "SPNEGO authenticator said continue...");
    private final SpnegoUtil spnegoUtil = new SpnegoUtil();

    public AuthenticationResult authenticate(HttpServletRequest req, HttpServletResponse resp, String authzHeader, SpnegoConfig spnegoConfig) {
        AuthenticationResult result = CONTINUE;
        try {
            byte[] tokenByte = Base64Coder.base64Decode(Base64Coder.getBytes(spnegoUtil.extractAuthzTokenString(authzHeader)));
            if (!spnegoUtil.isSpnegoOrKrb5Token(tokenByte)) {
                return notSpnegoAndKerberosTokenError(resp, spnegoConfig);
            }

            String reqHostName = getReqHostName(req, spnegoConfig);
            result = krb5Util.processSpnegoToken(resp, tokenByte, reqHostName, spnegoConfig);
            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            if (spnegoConfig.isDisableLtpaCookie()) {
                hashtable.put(AuthenticationConstants.INTERNAL_DISABLE_LTPA_SSO_CACHE, Boolean.TRUE);
            }
        } catch (AuthenticationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unexpected exception:", new Object[] { e });
            }

            result = new AuthenticationResult(AuthResult.FAILURE, "SPNEGO authentication failure");
        }

        return result;
    }

    /**
     * @param resp
     * @param spnegoConfig
     * @return
     * @throws AuthenticationException
     */
    protected AuthenticationResult notSpnegoAndKerberosTokenError(HttpServletResponse resp, SpnegoConfig spnegoConfig) {
        if (!spnegoConfig.getDisableFailOverToAppAuthType()) { // disableFailOVerToAppAuthType is false, so continue ...
            return CONTINUE;
        }

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(spnegoConfig.getErrorPageConfig().getNtlmTokenReceivedPageContentType());
        resp.setCharacterEncoding(spnegoConfig.getErrorPageConfig().getNtlmTokenReceivedPageCharset());

        try {
            resp.getWriter().println(spnegoConfig.getErrorPageConfig().getNTLMTokenReceivedPage());
            com.ibm.wsspi.webcontainer.WebContainerRequestState.getInstance(true).setAttribute("spnego.error.page", "true");
        } catch (IOException ex) {
            Tr.error(tc, "SPNEGO_FAIL_TO_GET_WRITER", "NTLMTokenReceivedPage", ex.getMessage());
        }

        return new AuthenticationResult(AuthResult.SEND_401, "The token included in the HttpServletRequest is not a valid SPNEGO token");
    }

    /**
     * @param resp
     * @return
     * @throws AuthenticationException
     */
    public AuthenticationResult createNegotiateHeader(HttpServletResponse resp, SpnegoConfig spnegoConfig) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setHeader("WWW-Authenticate", "Negotiate");
        resp.setContentType(spnegoConfig.getErrorPageConfig().getSpnegoNotSupportedPageContentType());
        String charset = spnegoConfig.getErrorPageConfig().getSpnegoNotSupportedPageCharset();
        if (charset != null) {
            resp.setCharacterEncoding(charset);
        }

        try {
            // Add an HTML response indicating SPNEGO is not supported
            // in case request has come from a Client which does not support SPNEGO.
            resp.getWriter().println(spnegoConfig.getErrorPageConfig().getSpnegoNotSupportedPage());
            com.ibm.wsspi.webcontainer.WebContainerRequestState.getInstance(true).setAttribute("spnego.error.page", "true");
        } catch (IOException ex) {
            Tr.error(tc, "SPNEGO_FAIL_TO_GET_WRITER", "SpnegoNotSupportedPage", ex.getMessage());
        }
        //When we consolidate ChallengeReply and TAIChallengeRely, we can remove the TAI_CHALLENGE
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.TAI_CHALLENGE, "Create negotiation Http header");

        return authResult;
    }

    protected String getReqHostName(HttpServletRequest req, SpnegoConfig spnegoConfig) {
        String hostName = req.getServerName();

        if (spnegoConfig.getAllowLocalHost() && SpnegoConfigImpl.LOCAL_HOST.equalsIgnoreCase(hostName)) {
            return hostName;
        }

        if (spnegoConfig.isCanonicalHostName()) {
            return getCanonicalHostname(spnegoConfig, hostName);
        }

        return hostName;
    }

    /**
     * This method converts a hostName to its canonical value.
     * if it is not set, just return the same hostName.
     * if it is set, looks up hostMap and return canonicalHostname.
     *
     * @param hostName
     * @return canonicalName if canonical support is on
     * @throws UnknownHostException if specific hostName is not known
     */
    protected String getCanonicalHostname(SpnegoConfig spnegoConfig, String hostName) {

        String mappedHost = hostMap.get(hostName);
        if (mappedHost != null) {
            hostName = mappedHost;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CanonicalSupport has converted " + hostName + " to " + mappedHost);
            }
            return hostName;
        }
        try {
            InetAddress inet = InetAddress.getByName(hostName);
            String canonicalHostName = inet.getCanonicalHostName();
            hostName = cacheHostName(hostName, canonicalHostName);
        } catch (UnknownHostException ue) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Canonical support specified but error when looking up - " +
                             hostName + "failed. Aliases will not work.");
                Tr.debug(tc, "getCanonicalHostname got unexpected exception: " + ue);
            }
        }

        return hostName;
    }

    /**
     * @param hostName
     * @param canonicalHostName
     * @return
     */
    protected String cacheHostName(String hostName, String canonicalHostName) {
        hostMap.put(hostName, canonicalHostName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Canonicalization support will map " + hostName + " to " + canonicalHostName);
        }
        return canonicalHostName;
    }

    //This method for unit test
    protected void setHostMap(HashMap<String, String> hostMap) {
        this.hostMap = hostMap;
    }
}