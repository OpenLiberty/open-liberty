/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.common.OidcCommonClientRequest;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

/*
 * Store the data for a httpServletRequest session
 *
 * Initialize when a session starts and
 * discard after it ends
 */
public class OidcClientRequest extends OidcCommonClientRequest {
    // these constants are duplicate of OidcClient in CL

    public static final String PROPAGATION_TOKEN_AUTHENTICATED = "com.ibm.ws.webcontainer.security.openidconnect.propagation.token.authenticated";
    // In case it's authenticated by the oidc propagation token, do not create a cookie
    public static final String INBOUND_PROPAGATION_VALUE = "com.ibm.ws.webcontainer.security.openidconnect.inbound.propagation.value";
    public static final String AUTHN_SESSION_DISABLED = "com.ibm.ws.webcontainer.security.openidconnect.authn.session.disabled";
    public static final String ACCESS_TOKEN_IN_LTPA_TOKEN = "com.ibm.ws.webcontainer.security.oidc.accesstoken.in.ltpa";
    public static final String OIDC_ACCESS_TOKEN = "oidc_access_token";

    public static final String inboundNone = "none";
    public static final String inboundRequired = "required";
    public static final String inboundSupported = "supported";

    private static final TraceComponent tcClient = Tr.register(OidcClientRequest.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected ConvergedClientConfig oidcClientConfig;

    protected String clientConfigId; // providerId

    protected String preCookieValue = null; // The base of customCacheCookie before create

    protected boolean authnSessionDisabled = true;
    protected String inboundValue = "none";

    protected String tokenType = TYPE_ID_TOKEN;
    protected String tokenTypeNoSpace = Constants.TOKEN_TYPE_ID_TOKEN;

    OidcClientRequest() {
        // for FAT, make its scope package only
    };

    // Called by the OidcClientImpl authenticate and logout
    public OidcClientRequest(HttpServletRequest request,
            HttpServletResponse response,
            ConvergedClientConfig convergedClientConfig,
            ReferrerURLCookieHandler referrerURLCookieHandler) {
        // referrerURLCookieHandler is null when logout
        this.oidcClientConfig = convergedClientConfig;
        this.request = request;
        this.response = response;
        OidcClientUtil.setReferrerURLCookieHandler(referrerURLCookieHandler);

        clientConfigId = convergedClientConfig.getId();
        authnSessionDisabled = convergedClientConfig.isAuthnSessionDisabled_propagation();
        inboundValue = convergedClientConfig.getInboundPropagation();

        request.setAttribute(AUTHN_SESSION_DISABLED, Boolean.valueOf(authnSessionDisabled));
        request.setAttribute(INBOUND_PROPAGATION_VALUE, inboundValue);
        request.setAttribute(ACCESS_TOKEN_IN_LTPA_TOKEN, Boolean.valueOf(convergedClientConfig.getAccessTokenInLtpaCookie()));
    }

    /**
     * @param resp
     */
    public void createOidcClientCookieIfAnyAndDisableLtpa() {
        if (oidcClientConfig.isDisableLtpaCookie()) {
            Boolean booleanAuthenticatedByPropagationToken = (Boolean) request.getAttribute(PROPAGATION_TOKEN_AUTHENTICATED);
            boolean bAuthenticatedByPropagationToken = booleanAuthenticatedByPropagationToken == null ? false : booleanAuthenticatedByPropagationToken.booleanValue();
            if (!bAuthenticatedByPropagationToken) {
                // If bAuthenticatedByPropagationToken is true, it's RS.
                // Otherwise, it's RP
                // For RS, no local cookies(task 211020)
                //
                // This is RP. For RP, create the local cookie when disableLtpsCookie(true).
                String oidcClientCookieName = getOidcClientCookieName();
                String oidcClientPreCookieValue = preCookieValue;
                if (oidcClientCookieName != null && oidcClientPreCookieValue != null) {
                    createOidcClientCookie(request,
                            response,
                            oidcClientCookieName,
                            oidcClientPreCookieValue);
                }
            }
        }
    }

    public String getOidcClientCookieName() {
        return oidcClientConfig.getOidcClientCookieName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OidcClientRequest [clientId:").append(this.clientConfigId).append(" request:").append(this.request).append("]");
        return sb.toString();
    }

    public static void createOidcClientCookie(HttpServletRequest req,
            HttpServletResponse resp,
            String cookieName,
            String cookieValue) {
        // TODO this does not setDomain since no other server can use this cookie for now
        Cookie c = OidcClientUtil.getReferrerURLCookieHandler().createCookie(cookieName, cookieValue, req);
        resp.addCookie(c);
    }

    @Trivial
    public String generatePreCookieValue() {
        if (preCookieValue != null) {
            // something must be wrong
            if (tcClient.isDebugEnabled()) {
                Tr.debug(tcClient, "preCookieValue exists:" + preCookieValue);
            }
            ;
            return null;
        }
        this.preCookieValue = OidcUtil.generateRandom();
        return preCookieValue;
    }

    /**
     * @return
     */
    @Trivial
    public String getAndSetCustomCacheKeyValue() {
        return getCustomCookieValue(generatePreCookieValue());
    }

    /**
     * @param generatePreCookieValue
     * @return
     */
    @Sensitive
    @Trivial
    public String getCustomCookieValue(String preCookieValue) {
        // This can be called
        // either from the cookie value retrived from the http serverlet request
        // or from a newly generated preKeyValue
        if (preCookieValue == null || preCookieValue.isEmpty())
            return null; // something must be wrong
        String preDigest = clientConfigId + "_" + preCookieValue + "_ibm";
        return HashUtils.digest(preDigest);
    }

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @return the oidcClientConfig
     */
    public OidcClientConfig getOidcClientConfig() {
        return oidcClientConfig.getOidcClientConfig();
    }

    /** {@inheritDoc} */
    @Override
    public String getTokenType() {
        return tokenType;
    }

    /** {@inheritDoc} */
    public String getTokenTypeNoSpace() {
        return tokenTypeNoSpace;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
        // the bInboundSupported and bInboundRequired are all false, since it's ID Token
        if (tokenType.equalsIgnoreCase(TYPE_ID_TOKEN)) {
            bInboundSupported = false;
            bInboundRequired = false;
            tokenTypeNoSpace = Constants.TOKEN_TYPE_ID_TOKEN;
        } else if (tokenType.equalsIgnoreCase(TYPE_JWT_TOKEN)) {
            // reset the inboundPropagation
            bInboundRequired = inboundRequired.equalsIgnoreCase(inboundValue);
            bInboundSupported = inboundSupported.equalsIgnoreCase(inboundValue);
            tokenTypeNoSpace = Constants.TOKEN_TYPE_JWT;
        } else {
            // reset the inboundPropagation
            bInboundRequired = inboundRequired.equalsIgnoreCase(inboundValue);
            bInboundSupported = inboundSupported.equalsIgnoreCase(inboundValue);
            tokenTypeNoSpace = Constants.TOKEN_TYPE_ACCESS_TOKEN;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getInboundPropagation() {
        return oidcClientConfig.getInboundPropagation();
    }

    /**
     * @return the bInboundRequired
     */
    public boolean isInboundRequired() {
        return bInboundRequired;
    }

    // do not override
    public JWTTokenValidationFailedException error(boolean bTrError, TraceComponent tc, String[] msgCodes, Object[] objects) throws JWTTokenValidationFailedException {
        int msgIndex = 0;
        if (!TYPE_ID_TOKEN.equals(this.getTokenType())) {
            msgIndex = 1;
        }
        return error(bTrError, tc, msgCodes[msgIndex], objects);
    }

    /**
     * @param string
     * @param b
     * @param aud
     * @param clientId
     * @return
     * @throws JWTTokenValidationFailedException
     */
    public JWTTokenValidationFailedException error(boolean bTrError, TraceComponent tc, String msgCode, Object[] objects) throws JWTTokenValidationFailedException {
        if (bTrError && !bInboundSupported) {
            Tr.error(tcClient, msgCode, objects);
        }
        if (TYPE_ID_TOKEN.equals(this.getTokenType())) {
            return IDTokenValidationFailedException.format(tc, msgCode, objects);
        } else {
            return JWTTokenValidationFailedException.format(tc, msgCode, objects);
        }
    }

    public void error(String[] msgCodes, Object[] objects) {
        if (!bInboundSupported) {
            int msgIndex = 0;
            if (!TYPE_ID_TOKEN.equals(this.getTokenType())) {
                msgIndex = 1;
            }
            Tr.error(tcClient, msgCodes[msgIndex], objects);
        }
    }

    String getErrorMessage() {
        String message = getRealmMessage();
        message += ", error=\"invalid_token\",";
        message += " error_description=\"";
        if (tokenType.equals(TYPE_JWT_TOKEN)) {
            message += "Check JWT token";
        } else {
            // TODO currently, we only handle JWT and access token
            message += "Check access token";
        }
        message += "\"";
        return message;
    }

    String getRealmMessage() {
        String realm = null;
        if (tokenType.equals(TYPE_JWT_TOKEN)) {
            realm = "jwt";
        } else {
            // TODO we may want to distinguish other token
            //      currently, we only handle JWT and access token
            realm = "oauth";
        }
        return "Bearer realm=\"" + realm + "\"";
    }

    /**
     *
     */
    public void setWWWAuthenticate() {
        String errorMsg = null;
        if (NO_TOKEN.equals(getHeaderFailMsg())) {
            errorMsg = getRealmMessage();
        } else {
            errorMsg = getErrorMessage();
        }
        response.setHeader("WWW-Authenticate", errorMsg);
    }

    @Override
    public List<String> getAudiences() {
        return this.oidcClientConfig.getAudiences();
    }

    boolean bSecuredHttp = false;
    String httpHostStr = null;
    int serverPort = -1;

    /** {@inheritDoc} */
    @Override
    public boolean isPreServiceUrl(String audience) {
        // check the https or https first
        if (this.httpHostStr == null) {
            String httpStr = "http://";
            this.bSecuredHttp = request.isSecure();
            if (this.bSecuredHttp) {
                httpStr = "https://";
            }
            httpHostStr = httpStr + request.getServerName();
            this.serverPort = request.getServerPort();
        }
        if (!audience.startsWith(this.httpHostStr))
            return false;

        String portAudience = audience.substring(this.httpHostStr.length());
        // let's check the port
        if (portAudience.isEmpty() || portAudience.startsWith("/")) {
            // no port defined in audience
            if (this.serverPort == -1)
                return true; // TODO somehow the httpServerRequest does not know the port number...
            if (this.bSecuredHttp) {
                if (this.serverPort == 443)
                    return true;
                // else false
            } else {
                if (this.serverPort == 80)
                    return true;
                // else false;
            }
        } else if (portAudience.startsWith(":")) {
            // has a port
            String portStr = ":" + this.serverPort; // such as: ":8020"
            if (portAudience.startsWith(portStr)) {
                if (portAudience.length() > portStr.length()) {
                    char slash = portAudience.charAt(portStr.length());
                    return slash == '/'; // return ending with '/'
                } else { // exact match
                    return true;
                }
            } // else false
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean allowedAllAudiences() {
        return oidcClientConfig.allowedAllAudiences();
    }

    /** {@inheritDoc} */
    @Override
    public boolean disableIssChecking() {
        return oidcClientConfig.disableIssChecking();
    }

}
